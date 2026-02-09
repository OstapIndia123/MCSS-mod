package com.example.hubmod.net;

import com.example.hubmod.blockentity.HubBlockEntity;
import com.example.hubmod.blockentity.ReaderBlockEntity;
import com.example.hubmod.config.HubModConfig;
import com.example.hubmod.server.ServerRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HubWsClient {

    // это ID клиента (не хаба). хабов может быть несколько.
    private static final String CLIENT_ID = "client-" + UUID.randomUUID();
    private static final int HEARTBEAT_SEC = 10;
    private static final int RECONNECT_MIN_SEC = 2;
    private static final int RECONNECT_MAX_SEC = 30;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ScheduledExecutorService SCHED = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "hub-ws");
        t.setDaemon(true);
        return t;
    });
    private static final Queue<String> OUTBOX = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean connecting = new AtomicBoolean(false);
    // защита от конфликтов таймера durationMs: ключ = hubId|side, значение = token
    private static final ConcurrentHashMap<String, Long> OUTPUT_TOKENS = new ConcurrentHashMap<>();
    // --- regexes (ленивый парсер, НЕ трогаем ts вообще) ---
    private static final Pattern P_TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern P_HUBID = Pattern.compile("\"hubId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern P_READER = Pattern.compile("\"readerId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern P_SIDE = Pattern.compile("\"side\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern P_LEVEL = Pattern.compile("\"level\"\\s*:\\s*(\\d+)");
    private static final Pattern P_DUR = Pattern.compile("\"durationMs\"\\s*:\\s*(\\d+)");
    private static final Pattern P_TP = Pattern.compile("\"testPeriodMs\"\\s*:\\s*(\\d+)");
    private static final Pattern P_TF = Pattern.compile("\"testFailAfterMs\"\\s*:\\s*(\\d+)");
    private static volatile WebSocket ws;
    private static volatile int reconnectDelaySec = RECONNECT_MIN_SEC;
    private HubWsClient() {
    }

    private static String wsUrl() {
        return HubModConfig.get().wsUrl;
    }

    public static void start() {
        connect();

        // JVM heartbeat (не хаб).
        SCHED.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                sendJson(Map.of(
                        "type", "HEARTBEAT",
                        "clientId", CLIENT_ID,
                        "ts", System.currentTimeMillis()
                ));
            }
        }, HEARTBEAT_SEC, HEARTBEAT_SEC, TimeUnit.SECONDS);

        SCHED.scheduleAtFixedRate(HubWsClient::flushOutbox, 200, 200, TimeUnit.MILLISECONDS);
    }

    private static void connect() {
        if (!connecting.compareAndSet(false, true)) return;

        try {
            URI uri = URI.create(wsUrl());

            HTTP.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(uri, new WebSocket.Listener() {

                        // на всякий случай: поддержка фрагментированных кадров эщь эщь эщь
                        private final StringBuilder partial = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            ws = webSocket;
                            reconnectDelaySec = RECONNECT_MIN_SEC;
                            connecting.set(false);

                            // ВАЖНО: просим первый кадр
                            webSocket.request(1);

                            // HELLO = только факт подключения клиента
                            sendJson(Map.of(
                                    "type", "HELLO",
                                    "client", "hubmod",
                                    "clientId", CLIENT_ID,
                                    "ts", System.currentTimeMillis()
                            ));

                            // CLIENT_CONFIG = делаем testPeriodMs/testFailAfterMs из hubmod.yml "рабочими":
                            // backend применит runtimeCfg и перезапустит таймер тестов.
                            HubModConfig cfg = HubModConfig.get();
                            sendJson(Map.of(
                                    "type", "CLIENT_CONFIG",
                                    "clientId", CLIENT_ID,
                                    "testPeriodMs", cfg.testPeriodMs,
                                    "testFailAfterMs", cfg.testFailAfterMs,
                                    "ts", System.currentTimeMillis()
                            ));
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            String part = data.toString();

                            if (!last) {
                                partial.append(part);
                                webSocket.request(1);
                                return CompletableFuture.completedFuture(null);
                            }

                            String text;
                            if (!partial.isEmpty()) {
                                partial.append(part);
                                text = partial.toString();
                                partial.setLength(0);
                            } else {
                                text = part;
                            }

                            try {
                                handleIncoming(text);
                            } catch (Throwable t) {
                                com.example.hubmod.util.HubLog.d("[HUB][WS][IN] handle failed: " + t.getMessage());
                            }

                            // просим следующий кадр
                            webSocket.request(1);
                            return CompletableFuture.completedFuture(null);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            scheduleReconnect();
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            scheduleReconnect();
                            return CompletableFuture.completedFuture(null);
                        }
                    })
                    .exceptionally(ex -> {
                        connecting.set(false);
                        scheduleReconnect();
                        return null;
                    });

        } catch (Throwable t) {
            connecting.set(false);
            scheduleReconnect();
        }
    }

    private static void handleIncoming(String text) {
        if (text == null) return;
        String s = text.trim();
        if (s.isEmpty()) return;
        if (!s.startsWith("{")) return;

        String type = extractString(s, P_TYPE);
        if (type == null || type.isBlank()) return;

        if ("SET_OUTPUT".equalsIgnoreCase(type)) {
            handleSetOutput(s);
            return;
        }

        if ("SET_READER_OUTPUT".equalsIgnoreCase(type)) {
            handleSetReaderOutput(s);
        }

        // CONFIG пока не используем, но оставим хук на будущее
    }

    private static void handleSetOutput(String rawJson) {
        String hubId = extractString(rawJson, P_HUBID);

        // пытаемся взять поля из корня
        String side = extractString(rawJson, P_SIDE);
        Integer level = extractInt(rawJson, P_LEVEL);
        Integer durationMs = extractInt(rawJson, P_DUR);

        // если не нашли — попробуем payload:{...}
        if (side == null || level == null) {
            String payload = extractObjectSection(rawJson, "\"payload\"");
            if (payload != null) {
                if (side == null) side = extractString(payload, P_SIDE);
                if (level == null) level = extractInt(payload, P_LEVEL);
                if (durationMs == null) durationMs = extractInt(payload, P_DUR);
            }
        }

        if (hubId == null || hubId.isBlank()) {
            com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] missing hubId raw=" + rawJson);
            return;
        }

        if (side == null || side.isBlank() || level == null) {
            com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] invalid payload (side/level) raw=" + rawJson);
            return;
        }

        Direction dir = byNameSafe(side);
        if (dir == null) {
            com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] unknown side=" + side);
            return;
        }

        int lvl = level;
        if (lvl < 0 || lvl > 15) {
            com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] invalid level=" + lvl);
            return;
        }

        int dur = (durationMs == null) ? 0 : Math.max(0, durationMs);

        com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] hubId=" + hubId + " side=" + dir.getName()
                + " level=" + lvl + " durationMs=" + dur);

        // применяем строго на серверном треде
        final String hid = hubId;
        final Direction d = dir;
        final int outLevel = lvl;

        ServerRef.runOnServerThread(() -> {
            Object hub = findLoadedHub(hid);
            if (hub == null) {
                com.example.hubmod.util.HubLog.d("[HUB][WS][SET_OUTPUT] hub not found in LOADED: " + hid);
                return;
            }
            applyHubOutput(hub, d, outLevel);
        });

        // если нужно авто-выключение
        if (dur > 0 && lvl > 0) {
            String key = hubId + "|" + dir.getName();
            long token = System.nanoTime();
            OUTPUT_TOKENS.put(key, token);

            SCHED.schedule(() -> {
                Long cur = OUTPUT_TOKENS.get(key);
                if (cur == null || cur != token) return;

                final String hid2 = hubId;
                final Direction d2 = dir;

                ServerRef.runOnServerThread(() -> {
                    Object hub = findLoadedHub(hid2);
                    if (hub == null) return;
                    applyHubOutput(hub, d2, 0);
                });
            }, dur, TimeUnit.MILLISECONDS);
        }
    }

    // --- Reader output: {type:"SET_READER_OUTPUT", readerId:"...", level:0..15} ---
    private static void handleSetReaderOutput(String rawJson) {
        String readerId = extractString(rawJson, P_READER);
        Integer level = extractInt(rawJson, P_LEVEL);

        // если завернули в payload:{...}
        if ((readerId == null || level == null)) {
            String payload = extractObjectSection(rawJson, "\"payload\"");
            if (payload != null) {
                if (readerId == null) readerId = extractString(payload, P_READER);
                if (level == null) level = extractInt(payload, P_LEVEL);
            }
        }

        if (readerId == null || readerId.isBlank()) {
            com.example.hubmod.util.HubLog.d("[READER][WS][SET_READER_OUTPUT] missing readerId raw=" + rawJson);
            return;
        }

        if (level == null) {
            com.example.hubmod.util.HubLog.d("[READER][WS][SET_READER_OUTPUT] missing level raw=" + rawJson);
            return;
        }

        int lvl = level;
        if (lvl < 0 || lvl > 15) {
            com.example.hubmod.util.HubLog.d("[READER][WS][SET_READER_OUTPUT] invalid level=" + lvl);
            return;
        }

        boolean on = lvl > 0;
        com.example.hubmod.util.HubLog.d("[READER][WS][SET_READER_OUTPUT] readerId=" + readerId + " on=" + on + " (level=" + lvl + ")");

        final String rid = readerId;
        final int lvlFinal = lvl;

        ServerRef.runOnServerThread(() -> {
            ReaderBlockEntity r = findLoadedReader(rid);
            if (r == null) {
                com.example.hubmod.util.HubLog.d("[READER][WS][SET_READER_OUTPUT] reader not found in LOADED: " + rid);
                return;
            }
            r.setOutputLevel(lvlFinal);
        });

    }

    private static Object findLoadedHub(String hubId) {
        for (HubBlockEntity h : HubBlockEntity.loaded()) {
            if (h == null) continue;
            String id = h.getHubId();
            if (id == null || id.isBlank()) continue;
            if (id.equalsIgnoreCase(hubId)) return h;
        }
        for (com.example.hubmod.blockentity.HubExtensionBlockEntity h : com.example.hubmod.blockentity.HubExtensionBlockEntity.loaded()) {
            if (h == null) continue;
            String id = h.getHubId();
            if (id == null || id.isBlank()) continue;
            if (id.equalsIgnoreCase(hubId)) return h;
        }
        return null;
    }

    private static void applyHubOutput(Object hub, Direction dir, int level) {
        if (hub instanceof HubBlockEntity h) {
            h.setOutput(dir, level);
        } else if (hub instanceof com.example.hubmod.blockentity.HubExtensionBlockEntity h) {
            h.setOutput(dir, level);
        }
    }

    private static ReaderBlockEntity findLoadedReader(String readerId) {
        for (ReaderBlockEntity r : ReaderBlockEntity.loaded()) {
            if (r == null) continue;
            String id = r.getReaderId();
            if (id == null || id.isBlank()) continue;
            if (id.equalsIgnoreCase(readerId)) return r;
        }
        return null;
    }

    private static Direction byNameSafe(String side) {
        String x = side.trim().toLowerCase(Locale.ROOT);
        for (Direction d : Direction.values()) {
            if (d.getName().equals(x)) return d;
        }
        return null;
    }

    private static String extractString(String json, Pattern p) {
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        return m.group(1);
    }

    private static Integer extractInt(String json, Pattern p) {
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Вытаскивает JSON-объект после ключа, например для "payload":{...}
     * Возвращает строку вида {...} или null.
     */
    private static String extractObjectSection(String json, String keyLiteral) {
        int idx = json.indexOf(keyLiteral);
        if (idx < 0) return null;

        int brace = json.indexOf('{', idx);
        if (brace < 0) return null;

        int end = findMatchingBrace(json, brace);
        if (end < brace) return null;

        return json.substring(brace, end + 1);
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    private static void scheduleReconnect() {
        ws = null;

        int delay = reconnectDelaySec;
        reconnectDelaySec = Math.min(RECONNECT_MAX_SEC, reconnectDelaySec * 2);

        SCHED.schedule(HubWsClient::connect, delay, TimeUnit.SECONDS);
    }

    private static boolean isOpen() {
        return ws != null;
    }

    private static void flushOutbox() {
        WebSocket w = ws;
        if (w == null) return;

        for (int i = 0; i < 50; i++) {
            String msg = OUTBOX.poll();
            if (msg == null) return;

            w.sendText(msg, true).exceptionally(ex -> {
                OUTBOX.add(msg);
                scheduleReconnect();
                return null;
            });
        }
    }

    public static void sendJson(Map<String, Object> obj) {
        OUTBOX.add(Json.minify(obj));
    }

    private static String hubIdFallback() {
        return "unknown";
    }

    public static void sendHubPing(String hubId) {
        String effective = (hubId == null || hubId.isBlank()) ? hubIdFallback() : hubId;

        sendJson(Map.of(
                "type", "HUB_PING",
                "hubId", effective,
                "ts", System.currentTimeMillis()
        ));
    }

    // hubId теперь ПЕРЕДАЁТСЯ с блока (BlockEntity), а не глобальный
    public static void sendPortIn(String hubId, BlockPos pos, Direction side, int level) {
        String effective = (hubId == null || hubId.isBlank()) ? hubIdFallback() : hubId;

        sendJson(Map.of(
                "type", "PORT_IN",
                "hubId", effective,
                "pos", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()),
                "side", side.getName(),
                "level", level,
                "ts", System.currentTimeMillis()
        ));
    }

    public static void sendReaderScan(String readerId, String keyName, String playerName, BlockPos pos) {
        String rid = (readerId == null || readerId.isBlank()) ? "unknown" : readerId;
        String kn = (keyName == null) ? "" : keyName;

        sendJson(Map.of(
                "type", "READER_SCAN",
                "readerId", rid,
                "keyName", kn,
                "player", playerName == null ? "" : playerName,
                "pos", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()),
                "ts", System.currentTimeMillis()
        ));
    }
}

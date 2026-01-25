package com.example.hubmod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HubIdentity {

    private static final Path CONFIG_DIR = Paths.get("config");
    private static final Path FILE = CONFIG_DIR.resolve("hubmod.json");

    // {"hubId":"..."}
    private static final Pattern HUB_ID_PATTERN =
            Pattern.compile("\"hubId\"\\s*:\\s*\"([^\"]+)\"");

    private static volatile String hubId;

    private HubIdentity() {}

    public static String getHubId() {
        if (hubId != null) return hubId;

        synchronized (HubIdentity.class) {
            if (hubId != null) return hubId;

            String loaded = tryLoad();
            if (loaded != null && !loaded.isBlank()) {
                hubId = loaded.trim();
                return hubId;
            }

            String created = "hub-" + UUID.randomUUID();
            hubId = created;
            trySave(created);
            return hubId;
        }
    }

    private static String tryLoad() {
        try {
            if (!Files.exists(FILE)) return null;
            String s = Files.readString(FILE, StandardCharsets.UTF_8);
            Matcher m = HUB_ID_PATTERN.matcher(s);
            if (m.find()) return m.group(1);
            return null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void trySave(String id) {
        try {
            Files.createDirectories(CONFIG_DIR);
            String json = "{\n  \"hubId\": \"" + escape(id) + "\"\n}\n";
            Files.writeString(FILE, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
            // если не сохранили — в этом запуске будет работать, но ID не закрепится (чё блять)
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

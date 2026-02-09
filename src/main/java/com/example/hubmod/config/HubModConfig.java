package com.example.hubmod.config;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HubModConfig {

    private static final String FILE_NAME = "hubmod.yml";
    private static HubModConfig INSTANCE;

    public boolean debug = false;
    public String wsUrl = "ws://127.0.0.1:8080";

    // 1) Мод использует testFailAfterMs, чтобы вычислить безопасный интервал HUB_PING.
    // 2) Мод отправляет их в backend при подключении WS (CLIENT_CONFIG),
    //    backend применяет их как runtimeCfg и перезапускает таймер тестов.
    public long testPeriodMs = 300_000;
    public long testFailAfterMs = 300_000;

    public static HubModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = loadOrCreate();
        }
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = loadOrCreate();
    }

    private static HubModConfig loadOrCreate() {
        Path cfgDir = FabricLoader.getInstance().getConfigDir();
        Path file = cfgDir.resolve(FILE_NAME);

        HubModConfig cfg = new HubModConfig();

        if (!Files.exists(file)) {
            write(file, cfg);
            return cfg;
        }

        try (InputStream in = Files.newInputStream(file)) {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(in);
            if (!(obj instanceof Map<?, ?> m)) {
                write(file, cfg);
                return cfg;
            }

            cfg.debug = asBool(m.get("debug"), cfg.debug);
            cfg.wsUrl = asString(m.get("wsUrl"), cfg.wsUrl);

            cfg.testPeriodMs = asLong(m.get("testPeriodMs"), cfg.testPeriodMs);
            cfg.testFailAfterMs = asLong(m.get("testFailAfterMs"), cfg.testFailAfterMs);

            // clamp safety
            if (cfg.testPeriodMs < 5_000) cfg.testPeriodMs = 5_000;
            if (cfg.testFailAfterMs < 5_000) cfg.testFailAfterMs = 5_000;

            return cfg;

        } catch (Throwable t) {
            // если YAML сломан — перезапишем дефолтом
            write(file, cfg);
            return cfg;
        }
    }

    private static void write(Path file, HubModConfig cfg) {
        try {
            Files.createDirectories(file.getParent());

            DumperOptions opt = new DumperOptions();
            opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            opt.setPrettyFlow(true);
            opt.setIndent(2);

            Yaml yaml = new Yaml(opt);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("debug", cfg.debug);
            root.put("wsUrl", cfg.wsUrl);
            root.put("testPeriodMs", cfg.testPeriodMs);
            root.put("testFailAfterMs", cfg.testFailAfterMs);

            try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
                yaml.dump(root, w);
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean asBool(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return "true".equalsIgnoreCase(s.trim());
        return def;
    }

    private static String asString(Object o, String def) {
        if (o == null) return def;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? def : s;
    }

    private static long asLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (Throwable ignored) {
            }
        }
        return def;
    }
}

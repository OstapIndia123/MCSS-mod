package com.example.hubmod;

import java.util.UUID;

public final class HubBlockIds {
    private HubBlockIds() {}

    public static String newId() {
        return newId("HUB-");
    }

    public static String newId(String prefix) {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "HUB-" : prefix;
        return safePrefix + UUID.randomUUID();
    }

    public static String newExtensionId() {
        return "HUB_EXT-" + UUID.randomUUID();
    }
}

package com.example.hubmod;

import java.util.UUID;

public final class HubBlockIds {
    private HubBlockIds() {}

    public static String newId() {
        return "HUB-" + UUID.randomUUID();
    }

    public static String newExtensionId() {
        return "HUB_EXT-" + UUID.randomUUID();
    }
}

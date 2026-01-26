package com.example.hubmod;

import java.util.UUID;

public final class HubBlockIds {
    private HubBlockIds() {}

    public static String newId() {
        return "HUB-" + UUID.randomUUID();
    }
}

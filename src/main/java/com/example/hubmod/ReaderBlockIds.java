package com.example.hubmod;

import java.util.UUID;

public final class ReaderBlockIds {
    private ReaderBlockIds() {
    }

    public static String newId() {
        return "READER-" + UUID.randomUUID();
    }
}

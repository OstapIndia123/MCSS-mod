package com.example.hubmod.util;

import com.example.hubmod.config.HubModConfig;

public final class HubLog {
    private HubLog() {}

    public static void d(String msg) {
        if (HubModConfig.get().debug) {
            System.out.println(msg);
        }
    }

    public static void i(String msg) {
        System.out.println(msg);
    }
}

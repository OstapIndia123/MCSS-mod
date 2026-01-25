package com.example.hubmod.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public final class ServerRef {
    private static volatile MinecraftServer server;

    private ServerRef() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
    }

    public static MinecraftServer get() {
        return server;
    }

    public static void runOnServerThread(Runnable r) {
        MinecraftServer s = server;
        if (s == null) return;
        s.execute(r);
    }
}

package com.example.hubmod;

import com.example.hubmod.block.ModBlocks;
import com.example.hubmod.blockentity.ModBlockEntities;
import com.example.hubmod.item.ModItemGroups;
import com.example.hubmod.net.HubWsClient;
import com.example.hubmod.server.ServerRef;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubMod implements ModInitializer {
    public static final String MOD_ID = "hubmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItemGroups.register();
        ModBlockEntities.register();

        ServerRef.init();
        HubWsClient.start();

        LOGGER.info("[HUB] Mod loaded");
    }
}
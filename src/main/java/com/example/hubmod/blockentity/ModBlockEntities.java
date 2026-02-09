package com.example.hubmod.blockentity;

import com.example.hubmod.HubMod;
import com.example.hubmod.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static BlockEntityType<HubBlockEntity> HUB_BLOCK_ENTITY;
    public static BlockEntityType<HubExtensionBlockEntity> HUB_EXTENSION_BLOCK_ENTITY;
    public static BlockEntityType<ReaderBlockEntity> READER_BLOCK_ENTITY;
    private ModBlockEntities() {
    }

    public static void register() {
        HUB_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "hub_be"),
                FabricBlockEntityTypeBuilder.create(HubBlockEntity::new, ModBlocks.HUB_BLOCK).build()
        );

        HUB_EXTENSION_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "hub_extension_be"),
                FabricBlockEntityTypeBuilder.create(HubExtensionBlockEntity::new, ModBlocks.HUB_EXTENSION_BLOCK).build()
        );

        READER_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "reader_be"),
                FabricBlockEntityTypeBuilder.create(ReaderBlockEntity::new, ModBlocks.READER_BLOCK).build()
        );

        com.example.hubmod.util.HubLog.d("[HUB] BlockEntities registered");
    }
}

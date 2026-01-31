package com.example.hubmod.block;

import com.example.hubmod.HubMod;
import com.example.hubmod.item.HubBlockItem;
import com.example.hubmod.item.HubExtensionBlockItem;
import com.example.hubmod.item.ReaderBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    private ModBlocks() {}

    // --- HUB ---
    public static final Identifier HUB_ID = Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "hub");
    public static final ResourceKey<Block> HUB_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, HUB_ID);
    public static final ResourceKey<Item>  HUB_ITEM_KEY  = ResourceKey.create(Registries.ITEM, HUB_ID);

    public static final Block HUB_BLOCK = new HubBlock(
            BlockBehaviour.Properties.of()
                    .strength(10.0f, 15.0f)
                    .setId(HUB_BLOCK_KEY)
    );

    // --- HUB EXTENSION ---
    public static final Identifier HUB_EXTENSION_ID = Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "hub_extension");
    public static final ResourceKey<Block> HUB_EXTENSION_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, HUB_EXTENSION_ID);
    public static final ResourceKey<Item>  HUB_EXTENSION_ITEM_KEY  = ResourceKey.create(Registries.ITEM, HUB_EXTENSION_ID);

    public static final Block HUB_EXTENSION_BLOCK = new HubExtensionBlock(
            BlockBehaviour.Properties.of()
                    .strength(10.0f, 15.0f)
                    .setId(HUB_EXTENSION_BLOCK_KEY)
    );

    // --- READER ---
    public static final Identifier READER_ID = Identifier.fromNamespaceAndPath(HubMod.MOD_ID, "reader");
    public static final ResourceKey<Block> READER_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, READER_ID);
    public static final ResourceKey<Item>  READER_ITEM_KEY  = ResourceKey.create(Registries.ITEM, READER_ID);

    public static final Block READER_BLOCK = new ReaderBlock(
            BlockBehaviour.Properties.of()
                    .strength(3.0f, 13.0f)
                    .setId(READER_BLOCK_KEY)
    );

    public static void register() {
        // HUB
        Registry.register(BuiltInRegistries.BLOCK, HUB_ID, HUB_BLOCK);
        Registry.register(
                BuiltInRegistries.ITEM,
                HUB_ID,
                new HubBlockItem(HUB_BLOCK, new Item.Properties().setId(HUB_ITEM_KEY))
        );

        // HUB EXTENSION
        Registry.register(BuiltInRegistries.BLOCK, HUB_EXTENSION_ID, HUB_EXTENSION_BLOCK);
        Registry.register(
                BuiltInRegistries.ITEM,
                HUB_EXTENSION_ID,
                new HubExtensionBlockItem(HUB_EXTENSION_BLOCK, new Item.Properties().setId(HUB_EXTENSION_ITEM_KEY))
        );

        // READER
        Registry.register(BuiltInRegistries.BLOCK, READER_ID, READER_BLOCK);
        Registry.register(
                BuiltInRegistries.ITEM,
                READER_ID,
                new ReaderBlockItem(READER_BLOCK, new Item.Properties().setId(READER_ITEM_KEY))
        );

        com.example.hubmod.util.HubLog.d("[HUB] Blocks registered");
    }
}

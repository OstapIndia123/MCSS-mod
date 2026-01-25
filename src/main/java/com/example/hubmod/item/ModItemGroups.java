package com.example.hubmod.item;

import com.example.hubmod.HubMod;
import com.example.hubmod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {

    // Ключ хаба
    public static final ResourceKey<CreativeModeTab> HUB_TAB_KEY =
            ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), id("hub_tab"));

    // Сам хаб
    public static final CreativeModeTab HUB_TAB = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.hubmod"))
            .icon(() -> new ItemStack(ModBlocks.HUB_BLOCK))
            .build();

    public static void register() {
        // Регистрируем хаб
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, HUB_TAB_KEY, HUB_TAB);

        // Добавляем в него предметы
        ItemGroupEvents.modifyEntriesEvent(HUB_TAB_KEY).register(entries -> {
            entries.accept(ModBlocks.HUB_BLOCK);
            entries.accept(ModBlocks.READER_BLOCK);
        });

        com.example.hubmod.util.HubLog.d("[HUB] ItemGroups registered");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HubMod.MOD_ID, path);
    }

    private ModItemGroups() {}
}

package com.example.hubmod.item;

import com.example.hubmod.HubBlockIds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public class ReaderBlockItem extends BlockItem {
    private static final String KEY = "readerId";

    public ReaderBlockItem(Block block, Properties props) {
        super(block, props);
    }

    /** Вытащить readerId из предмета (если нет — вернёт null) */
    public static String getReaderId(ItemStack stack) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();
        String id = tag.getString(KEY).orElse("");
        return id.isEmpty() ? null : id;
    }

    /** Записать readerId в предмет */
    public static void setReaderId(ItemStack stack, String readerId) {
        if (readerId == null || readerId.isBlank()) return;

        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();

        tag.putString(KEY, readerId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Если readerId отсутствует — сгенерировать */
    public static String ensureReaderId(ItemStack stack) {
        String cur = getReaderId(stack);
        if (cur != null) return cur;

        String id = HubBlockIds.newId("READER-");
        setReaderId(stack, id);
        return id;
    }

    /** Генерация при крафте */
    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        ensureReaderId(stack);
    }

    /** Вывод ID прямо в имя предмета */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        String id = getReaderId(stack);
        if (id == null) return base;

        return base.copy().append(Component.literal(" (" + id + ")").withStyle(ChatFormatting.DARK_GRAY));
    }
}

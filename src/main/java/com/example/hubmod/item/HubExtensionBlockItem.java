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
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import static com.example.hubmod.util.HubComponents.idCopy;

public class HubExtensionBlockItem extends BlockItem {
    private static final String KEY = "hubId";

    public HubExtensionBlockItem(Block block, Properties props) {
        super(block, props);
    }

    /**
     * Вытащить hubExtensionId из предмета (если нет — вернёт null)
     */
    public static String getHubExtensionId(ItemStack stack) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();
        String id = tag.getString(KEY).orElse("");
        return id.isEmpty() ? null : id;
    }

    /**
     * Записать hubExtensionId в предмет
     */
    public static void setHubExtensionId(ItemStack stack, String hubId) {
        if (hubId == null || hubId.isBlank()) return;

        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = cd.copyTag();

        tag.putString(KEY, hubId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Если hubExtensionId отсутствует — сгенерировать
     */
    public static String ensureHubExtensionId(ItemStack stack) {
        String cur = getHubExtensionId(stack);
        if (cur != null) return cur;

        String id = HubBlockIds.newExtensionId();
        setHubExtensionId(stack, id);
        return id;
    }

    /**
     * В твоей версии: onCraftedBy(ItemStack, Player)
     */
    @Override
    public void onCraftedBy(@NonNull ItemStack stack, @NonNull Player player) {
        super.onCraftedBy(stack, player);
        ensureHubExtensionId(stack);
    }

    /**
     * Временный вывод ID прямо в имя предмета (не tooltip)
     */
    @Override
    public @NonNull Component getName(@NonNull ItemStack stack) {
        Component base = super.getName(stack);
        String id = getHubExtensionId(stack);
        if (id == null) return base;

        return base.copy().append(Component.literal(" (" + id + ")").withStyle(ChatFormatting.DARK_GRAY));
    }

    @SuppressWarnings("resource")
    @Override
    protected boolean placeBlock(@NotNull net.minecraft.world.item.context.BlockPlaceContext context, @NotNull net.minecraft.world.level.block.state.BlockState blockState) {
        if (!super.placeBlock(context, blockState)) return false;

        Player player = context.getPlayer();
        if (player != null && !player.level().isClientSide()) {
            String id = ensureHubExtensionId(context.getItemInHand());

            idCopy(player, id);
        }
        return true;
    }


}

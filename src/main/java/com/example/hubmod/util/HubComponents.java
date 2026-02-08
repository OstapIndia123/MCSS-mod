package com.example.hubmod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;

public class HubComponents {
    public static void idCopy(Player player, String id) {
        Component idComponent = Component.literal("[" + id + "]")
                .withStyle(s -> s
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent.CopyToClipboard(id))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy ID")
                                .withStyle(ChatFormatting.GRAY)
                        ))
                );

        player.displayClientMessage(
                Component.literal("ID: ").append(idComponent).withStyle(ChatFormatting.GREEN),
                false
        );
    }
}

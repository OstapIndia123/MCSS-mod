package com.example.hubmod.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.logging.LogUtils.getLogger;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"))
    private static void onClickEvent(ClickEvent clickEvent, Minecraft minecraft, Screen screen, CallbackInfo ci) {
        getLogger().info("ClickEvent detected: {}", clickEvent);
        if (clickEvent instanceof ClickEvent.CopyToClipboard) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("✓ Скопировано")
                                .withStyle(ChatFormatting.GREEN),
                        true  // показать над hotbar
                );
            }
        }
    }
}

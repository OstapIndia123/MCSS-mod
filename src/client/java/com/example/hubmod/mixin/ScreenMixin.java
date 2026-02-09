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
        if (clickEvent instanceof ClickEvent.CopyToClipboard(String value)) {
            getLogger().info("CopyToClipboard event detected with value: " + value);
            // Проверяем что это UUID (формат Hub ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
            if (value.matches("[A-Z]+-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.hub_id_copied")
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );
                }
            }
        }
    }
}

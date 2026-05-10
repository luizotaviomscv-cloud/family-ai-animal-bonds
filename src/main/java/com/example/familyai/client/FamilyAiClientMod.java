package com.example.familyai.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class FamilyAiClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> FamilyAiHudOverlay.render(guiGraphics));
    }
}

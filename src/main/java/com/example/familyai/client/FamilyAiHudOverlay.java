package com.example.familyai.client;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAiHudMode;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.ReputationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class FamilyAiHudOverlay {
    private FamilyAiHudOverlay() {
    }

    static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableHud) {
            return;
        }

        FamilyAiHudMode mode = config.resolvedHudMode();
        if (mode == FamilyAiHudMode.OFF) {
            return;
        }

        if (!(minecraft.hitResult instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof Animal animal) || !FamilyAi.isFamilyAnimal(animal)) {
            return;
        }

        FamilyAnimal data = (FamilyAnimal) animal;
        List<String> lines = new ArrayList<>();
        lines.add("Family AI");
        lines.add("Estado: " + data.family$getAiState());

        if (mode == FamilyAiHudMode.SIMPLE) {
            lines.add("Familia: " + (data.family$getMotherUuid().isPresent() || data.family$getFatherUuid().isPresent() ? "SIM" : "NAO"));
            lines.add("Alerta: " + (data.family$isAlert() ? "SIM" : "NAO"));
        } else {
            lines.add("Mae: " + shortId(data.family$getMotherUuid()));
            lines.add("Pai: " + shortId(data.family$getFatherUuid()));
            lines.add("Filhos: " + data.family$getChildUuids().size() + " | Irmaos: " + data.family$getSiblingUuids().size());
            lines.add("Alerta: " + data.family$getAlertTicks() + "t | Panic: " + data.family$getPanicCooldownTicks() + "t");
            lines.add("Lider: " + shortId(data.family$getLeaderUuid()));

            if (minecraft.player != null) {
                int rep = FamilyAi.getReputationFor(animal, minecraft.player.getUUID());
                ReputationState state = FamilyAi.getReputationState(animal, minecraft.player.getUUID());
                lines.add("Reputacao: " + rep + " (" + state + ")");
            }
        }

        if (mode == FamilyAiHudMode.DEBUG) {
            lines.add(String.format("Traits M/P/R: %.2f / %.2f / %.2f", data.family$getTraitFear(), data.family$getTraitProtective(), data.family$getTraitHerdAffinity()));
            lines.add("PathFail: " + data.family$getPathFailCount() + " | Stuck: " + data.family$getStuckTicks());
            lines.add("DataVersion: " + data.family$getDataVersion());
        }

        int x = 8;
        int y = 8;
        int color = 0xE8F0FF;
        for (String line : lines) {
            guiGraphics.drawString(minecraft.font, Component.literal(line), x, y, color, true);
            y += 11;
        }
    }

    private static String shortId(Optional<UUID> uuid) {
        if (uuid.isEmpty()) {
            return "-";
        }
        String full = uuid.get().toString();
        return full.substring(0, 8);
    }
}

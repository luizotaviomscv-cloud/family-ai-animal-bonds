package com.example.familyai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;

public final class FamilyAiMod implements ModInitializer {
    public static final String MOD_ID = "family_ai";

    @Override
    public void onInitialize() {
        FamilyAiConfig.load();
        FamilyAiCommands.register();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Animal animal) {
                FamilyAi.onAnimalLoaded(animal);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (entity instanceof Animal animal && animal.isBaby()) {
                FamilyAi.onChildThreatened(animal, source.getEntity());
            }
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level instanceof ServerLevel && entity instanceof Animal animal && animal.isBaby()) {
                FamilyAi.onChildThreatened(animal, player);
            }
            return InteractionResult.PASS;
        });
    }
}

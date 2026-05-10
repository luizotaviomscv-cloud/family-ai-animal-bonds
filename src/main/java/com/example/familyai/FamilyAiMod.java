package com.example.familyai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

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
            if (entity instanceof Animal animal && FamilyAi.isFamilyAnimal(animal)) {
                FamilyAi.onFamilyAnimalDamaged(animal, source.getEntity());
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof Player player) {
                FamilyAi.onLivingEntityKilledByPlayer(entity, player);
            }
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level instanceof ServerLevel && entity instanceof Animal animal) {
                FamilyAi.onAnimalHurtByPlayer(animal, player);
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level instanceof ServerLevel && entity instanceof Animal animal && FamilyAi.isFamilyAnimal(animal) && animal.isFood(player.getItemInHand(hand))) {
                return FamilyAi.onAnimalFed(animal, player) ? InteractionResult.PASS : InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FamilyAiChat.sendWelcome(handler.player);
            FamilyAiGuideBook.giveGuideOnFirstJoin(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                FamilyAiChat.clearPlayer(handler.player.getUUID()));
    }
}

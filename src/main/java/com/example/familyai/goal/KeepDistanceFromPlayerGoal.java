package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.ReputationState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

public final class KeepDistanceFromPlayerGoal extends Goal {
    private final Animal animal;
    private Player targetPlayer;
    private ReputationState state = ReputationState.NEUTRAL;
    private int recalcTicks;
    private int warningSoundCooldown;

    public KeepDistanceFromPlayerGoal(Animal animal) {
        this.animal = animal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (animal.isBaby()) {
            return false;
        }

        targetPlayer = findNearbyPlayer();
        if (targetPlayer == null) {
            return false;
        }

        state = FamilyAi.getReputationState(animal, targetPlayer.getUUID());
        if (state == ReputationState.NEUTRAL || state == ReputationState.TRUSTED) {
            return false;
        }

        if (state == ReputationState.HOSTILE) {
            findNearbyRegisteredChild().ifPresent(child -> ((FamilyAnimal) animal).family$alert(
                    targetPlayer.getUUID(),
                    child.getUUID(),
                    FamilyAiConfig.get().alertTicks,
                    FamilyAiConfig.get().alertCooldownTicks
            ));
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive() || targetPlayer.isSpectator() || targetPlayer.isCreative()) {
            return false;
        }

        state = FamilyAi.getReputationState(animal, targetPlayer.getUUID());
        if (state == ReputationState.NEUTRAL || state == ReputationState.TRUSTED) {
            return false;
        }

        double desiredDistance = state == ReputationState.HOSTILE ? 10.0D : 6.0D;
        return animal.distanceToSqr(targetPlayer) < (desiredDistance + 2.0D) * (desiredDistance + 2.0D);
    }

    @Override
    public void tick() {
        if (targetPlayer == null) {
            return;
        }

        if (state == ReputationState.HOSTILE && warningSoundCooldown-- <= 0) {
            warningSoundCooldown = FamilyAiConfig.get().warningSoundCooldownTicks;
            animal.playAmbientSound();
        }

        if (state == ReputationState.HOSTILE) {
            animal.getLookControl().setLookAt(targetPlayer, 35.0F, 35.0F);
        }

        if (--recalcTicks <= 0) {
            recalcTicks = 8;
            Vec3 away = animal.position().subtract(targetPlayer.position());
            if (away.lengthSqr() < 0.0001D) {
                away = new Vec3(animal.getRandom().nextDouble() - 0.5D, 0.0D, animal.getRandom().nextDouble() - 0.5D);
            }

            double retreatDistance = state == ReputationState.HOSTILE ? 5.5D : 3.0D;
            Vec3 target = animal.position().add(away.normalize().scale(retreatDistance));
            double speed = state == ReputationState.HOSTILE ? 1.18D : 0.98D;
            animal.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Override
    public void stop() {
        targetPlayer = null;
        state = ReputationState.NEUTRAL;
        recalcTicks = 0;
    }

    private Player findNearbyPlayer() {
        double radius = FamilyAiConfig.get().hostileScanRange + 3.0D;
        AABB box = animal.getBoundingBox().inflate(radius);
        return animal.level()
                .getEntitiesOfClass(Player.class, box, player -> !player.isSpectator() && !player.isCreative())
                .stream()
                .filter(player -> {
                    ReputationState rep = FamilyAi.getReputationState(animal, player.getUUID());
                    return rep == ReputationState.WARY || rep == ReputationState.HOSTILE;
                })
                .min(Comparator.comparingDouble(animal::distanceToSqr))
                .orElse(null);
    }

    private Optional<Animal> findNearbyRegisteredChild() {
        FamilyAnimal familyData = (FamilyAnimal) animal;
        if (familyData.family$getChildUuids().isEmpty()) {
            return Optional.empty();
        }

        AABB box = animal.getBoundingBox().inflate(14.0D);
        return animal.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> candidate.isBaby() && familyData.family$getChildUuids().contains(candidate.getUUID()))
                .stream()
                .min(Comparator.comparingDouble(animal::distanceToSqr));
    }
}

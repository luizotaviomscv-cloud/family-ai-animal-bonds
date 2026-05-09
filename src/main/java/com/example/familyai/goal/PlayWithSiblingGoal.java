package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class PlayWithSiblingGoal extends Goal {
    private final Animal child;
    private Animal sibling;
    private int recalcTicks;
    private int playTicks;

    public PlayWithSiblingGoal(Animal child, double speed) {
        this.child = child;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!child.isBaby() || ((FamilyAnimal) child).family$isAlert()) {
            return false;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        FamilyAnimal data = (FamilyAnimal) child;
        long elapsed = child.level().getGameTime() - data.family$getLastPlayTick();
        if (elapsed < config.siblingPlayCooldownTicks) {
            return false;
        }

        if (child.getRandom().nextInt(8) != 0) {
            return false;
        }

        sibling = FamilyAi.findPlaymate(child).orElse(null);
        return sibling != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!child.isBaby() || sibling == null || !sibling.isAlive() || !sibling.isBaby() || ((FamilyAnimal) child).family$isAlert()) {
            return false;
        }

        double maxRange = FamilyAiConfig.get().siblingPlayRange;
        return playTicks > 0 && child.distanceToSqr(sibling) <= maxRange * maxRange;
    }

    @Override
    public void start() {
        FamilyAiConfig config = FamilyAiConfig.get();
        playTicks = config.siblingPlayDurationTicks;
        ((FamilyAnimal) child).family$setLastPlayTick(child.level().getGameTime());
    }

    @Override
    public void tick() {
        if (sibling == null) {
            return;
        }

        playTicks--;
        child.getLookControl().setLookAt(sibling, 30.0F, 30.0F);

        if (child.distanceToSqr(sibling) < 1.8D * 1.8D && child.getRandom().nextInt(14) == 0) {
            child.getJumpControl().jump();
        }

        if (--recalcTicks <= 0) {
            recalcTicks = 10;
            Vec3 midpoint = child.position().add(sibling.position()).scale(0.5D);
            double offsetX = (child.getRandom().nextDouble() - 0.5D) * 2.6D;
            double offsetZ = (child.getRandom().nextDouble() - 0.5D) * 2.6D;
            Vec3 target = midpoint.add(offsetX, 0.0D, offsetZ);
            child.getNavigation().moveTo(target.x, target.y, target.z, FamilyAiConfig.get().siblingPlaySpeed);
        }
    }

    @Override
    public void stop() {
        sibling = null;
        recalcTicks = 0;
        playTicks = 0;
        child.getNavigation().stop();
    }
}

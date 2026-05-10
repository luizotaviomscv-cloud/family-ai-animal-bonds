package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.FamilyAiState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class ProtectChildGoal extends Goal {
    private final Animal parent;
    private Animal child;
    private Entity threat;
    private int recalcTicks;
    private int soundCooldown;

    public ProtectChildGoal(Animal parent, double speed) {
        this.parent = parent;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableChildProtection) {
            return false;
        }
        if (parent.isBaby() || !((FamilyAnimal) parent).family$isAlert()) {
            return false;
        }

        child = FamilyAi.findAlertChild(parent).orElse(null);
        threat = FamilyAi.findThreat(parent).orElse(null);
        return child != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !parent.isBaby()
                && child != null
                && child.isAlive()
                && ((FamilyAnimal) parent).family$isAlert();
    }

    @Override
    public void tick() {
        FamilyAi.findThreat(parent).ifPresent(entity -> threat = entity);

        if (soundCooldown-- <= 0) {
            soundCooldown = FamilyAiConfig.get().warningSoundCooldownTicks;
            parent.playAmbientSound();
        }

        if (threat != null) {
            parent.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        } else if (child != null) {
            parent.getLookControl().setLookAt(child, 30.0F, 30.0F);
        }

        if (--recalcTicks <= 0 && child != null) {
            recalcTicks = 8;
            Vec3 guardPoint = FamilyAi.guardPointBetweenThreatAndChild(child, threat);
            if (parent.position().distanceToSqr(guardPoint) > 1.0D) {
                parent.getNavigation().moveTo(guardPoint.x, guardPoint.y, guardPoint.z, FamilyAiConfig.get().parentProtectSpeed);
                FamilyAnimal data = (FamilyAnimal) parent;
                data.family$setAiState(FamilyAiState.PROTECT_CHILD);
                data.family$setPanicCooldownTicks(Math.max(data.family$getPanicCooldownTicks(), 20));
            }
        }
    }

    @Override
    public void stop() {
        child = null;
        threat = null;
        recalcTicks = 0;
    }
}

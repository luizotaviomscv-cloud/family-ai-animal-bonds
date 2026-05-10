package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.FamilyAiState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public final class RunToParentGoal extends Goal {
    private final Animal child;
    private final double speed;
    private final double stopDistance;
    private Animal parent;
    private Entity threat;
    private int recalcTicks;

    public RunToParentGoal(Animal child, double speed, double stopDistance) {
        this.child = child;
        this.speed = speed;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableFamilySystem) {
            return false;
        }
        if (!child.isBaby()) {
            return false;
        }

        Optional<Player> hostile = FamilyAi.findHostilePlayer(child, config.hostileScanRange);
        hostile.ifPresent(player -> FamilyAi.onChildThreatened(child, player));

        parent = FamilyAi.findPreferredParent(child).orElse(null);
        if (parent == null) {
            ((FamilyAnimal) child).family$setAiState(FamilyAiState.LOST_CHILD);
            return false;
        }

        FamilyAnimal data = (FamilyAnimal) child;
        double tooFarDistance = config.childTooFarDistance;
        boolean tooFarFromParent = child.distanceToSqr(parent) > tooFarDistance * tooFarDistance;
        return data.family$isAlert() || hostile.isPresent() || tooFarFromParent;
    }

    @Override
    public boolean canContinueToUse() {
        if (!child.isBaby() || parent == null || !parent.isAlive()) {
            return false;
        }

        double stopDistanceSquared = stopDistance * stopDistance;
        return child.distanceToSqr(parent) > stopDistanceSquared;
    }

    @Override
    public void tick() {
        if (parent == null || !parent.isAlive()) {
            parent = FamilyAi.findPreferredParent(child).orElse(null);
            if (parent == null) {
                return;
            }
        }

        FamilyAi.findThreat(child).ifPresent(entity -> threat = entity);
        child.getLookControl().setLookAt(parent, 30.0F, 30.0F);

        if (--recalcTicks <= 0) {
            recalcTicks = 6;
            Vec3 target = FamilyAi.refugePointBehindParent(parent, threat);
            boolean ok = child.getNavigation().moveTo(target.x, target.y, target.z, speed);
            FamilyAnimal data = (FamilyAnimal) child;
            data.family$setAiState(FamilyAiState.FOLLOW_PARENT);
            if (ok) {
                data.family$setPathFailCount(Math.max(0, data.family$getPathFailCount() - 1));
            } else {
                data.family$setPathFailCount(data.family$getPathFailCount() + 1);
            }
        }
    }

    @Override
    public void stop() {
        parent = null;
        threat = null;
        recalcTicks = 0;
        child.getNavigation().stop();
    }
}

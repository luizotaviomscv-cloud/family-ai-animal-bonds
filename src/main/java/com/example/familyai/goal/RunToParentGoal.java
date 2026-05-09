package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public final class RunToParentGoal extends Goal {
    private final Animal child;
    private Animal parent;
    private Entity threat;
    private int recalcTicks;

    public RunToParentGoal(Animal child, double speed, double stopDistance) {
        this.child = child;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!child.isBaby()) {
            return false;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        Optional<Player> hostile = FamilyAi.findHostilePlayer(child, config.hostileScanRange);
        hostile.ifPresent(player -> FamilyAi.onChildThreatened(child, player));

        parent = FamilyAi.findPreferredParent(child).orElse(null);
        if (parent == null) {
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

        double stopDistance = FamilyAiConfig.get().childStopDistance;
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
            child.getNavigation().moveTo(target.x, target.y, target.z, FamilyAiConfig.get().childFollowSpeed);
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

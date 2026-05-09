package com.example.familyai.goal;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class StayNearMateGoal extends Goal {
    private final Animal animal;
    private Animal mate;
    private int recalcTicks;

    public StayNearMateGoal(Animal animal, double speed, double maxRadius) {
        this.animal = animal;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (animal.isBaby() || ((FamilyAnimal) animal).family$isAlert()) {
            return false;
        }

        mate = FamilyAi.findMate(animal).orElse(null);
        double maxRadius = FamilyAiConfig.get().mateCohesionRadius;
        return mate != null && animal.distanceToSqr(mate) > maxRadius * maxRadius;
    }

    @Override
    public boolean canContinueToUse() {
        if (mate == null || !mate.isAlive() || ((FamilyAnimal) animal).family$isAlert()) {
            return false;
        }

        double maxRadius = FamilyAiConfig.get().mateCohesionRadius;
        double comfortableRadius = maxRadius - 2.0D;
        return animal.distanceToSqr(mate) > comfortableRadius * comfortableRadius;
    }

    @Override
    public void tick() {
        animal.getLookControl().setLookAt(mate, 20.0F, 20.0F);

        if (--recalcTicks <= 0) {
            recalcTicks = 20;
            FamilyAiConfig config = FamilyAiConfig.get();
            double maxRadius = config.mateCohesionRadius;
            Vec3 point = FamilyAi.mateCohesionPoint(animal, mate, maxRadius);
            animal.getNavigation().moveTo(point.x, point.y, point.z, config.mateCohesionSpeed);
        }
    }

    @Override
    public void stop() {
        mate = null;
        recalcTicks = 0;
    }
}

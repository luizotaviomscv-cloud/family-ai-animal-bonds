package com.example.familyai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class FamilyAiBrain {
    private FamilyAiBrain() {
    }

    static void tick(Animal animal, long gameTime) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableAdvancedAi || !(animal.level() instanceof ServerLevel level) || !animal.isAlive()) {
            return;
        }

        if (!isAiUpdateTurn(animal, gameTime, config.aiUpdateIntervalTicks)) {
            return;
        }

        FamilyAnimal data = (FamilyAnimal) animal;
        boolean threatDetected = detectThreat(animal, data, config).isPresent();
        if (threatDetected) {
            data.family$setLastThreatTick(gameTime);
            data.family$setPanicCooldownTicks(config.panicCooldownTicks);
            if (data.family$getAiState() != FamilyAiState.ALERT && data.family$getAiState() != FamilyAiState.FLEE) {
                setState(animal, FamilyAiState.ALERT);
            }
        }

        if (data.family$getPanicCooldownTicks() > 0 && !threatDetected && !data.family$isAlert()) {
            setState(animal, FamilyAiState.PANIC_COOLDOWN);
        }

        if (animal.isBaby()) {
            tickBaby(animal, level, config, threatDetected);
        } else {
            tickAdult(animal, level, config, threatDetected);
        }

        if (config.enableUnstuckSystem) {
            tickUnstuck(animal, config, gameTime);
        } else {
            data.family$setStuckTicks(0);
            data.family$setPathFailCount(0);
        }
    }

    private static boolean isAiUpdateTurn(Animal animal, long gameTime, int interval) {
        int safeInterval = Math.max(1, interval);
        return Math.floorMod(gameTime + animal.getId(), safeInterval) == 0;
    }

    private static void tickBaby(Animal child, ServerLevel level, FamilyAiConfig config, boolean threatDetected) {
        FamilyAnimal data = (FamilyAnimal) child;
        Optional<Animal> parent = FamilyAi.findPreferredParent(child);
        if (parent.isPresent()) {
            Animal targetParent = parent.get();
            data.family$setTempGuardianUuid(targetParent.getUUID());
            double distance = child.distanceTo(targetParent);
            if (distance > config.childTooFarDistance) {
                setState(child, FamilyAiState.FOLLOW_PARENT);
                double speed = threatDetected ? config.childRunSpeed : config.childFollowSpeed;
                Vec3 desired = threatDetected
                        ? FamilyAi.refugePointBehindParent(targetParent, FamilyAi.findThreat(child).orElse(null))
                        : targetParent.position();
                moveSafely(level, child, desired, speed);
            } else if (threatDetected) {
                setState(child, FamilyAiState.FLEE);
                moveSafely(level, child, FamilyAi.refugePointBehindParent(targetParent, FamilyAi.findThreat(child).orElse(null)), config.childRunSpeed);
            } else {
                setState(child, FamilyAiState.GRAZE);
            }
            return;
        }

        Optional<Animal> fallbackAdult = findTempGuardian(child, config.parentFallbackRange);
        if (fallbackAdult.isPresent()) {
            Animal guardian = fallbackAdult.get();
            data.family$setTempGuardianUuid(guardian.getUUID());
            setState(child, FamilyAiState.FOLLOW_TEMP_ADULT);
            moveSafely(level, child, guardian.position(), Math.max(config.childFollowSpeed * 0.92D, 0.25D));
            return;
        }

        data.family$setTempGuardianUuid(null);
        setState(child, FamilyAiState.SEARCH_PARENT);
        Optional<Vec3> center = herdCenter(child, config.regroupDistance);
        if (center.isPresent()) {
            moveSafely(level, child, center.get(), config.regroupSpeed);
        } else {
            setState(child, FamilyAiState.LOST_CHILD);
        }
    }

    private static void tickAdult(Animal adult, ServerLevel level, FamilyAiConfig config, boolean threatDetected) {
        FamilyAnimal data = (FamilyAnimal) adult;
        if (data.family$isAlert()) {
            setState(adult, FamilyAiState.PROTECT_CHILD);
            return;
        }

        List<Animal> herd = getNearbyHerd(adult, config.regroupDistance);
        if (herd.isEmpty()) {
            setState(adult, FamilyAiState.IDLE);
            return;
        }

        Optional<Animal> leader = resolveLeader(adult, herd, config);
        leader.ifPresent(animal -> data.family$setLeaderUuid(animal.getUUID()));

        if (threatDetected) {
            setState(adult, FamilyAiState.FLEE);
            Entity threat = detectThreat(adult, data, config).orElse(null);
            Vec3 away = threat == null ? randomNearby(adult) : adult.position().subtract(threat.position());
            if (away.lengthSqr() < 0.0001D) {
                away = randomNearby(adult);
            }
            moveSafely(level, adult, adult.position().add(away.normalize().scale(6.0D * config.panicIntensity)), config.fleeSpeed);
            return;
        }

        Vec3 boidTarget = computeHerdTarget(adult, herd, config, leader.orElse(null));
        if (boidTarget == null) {
            setState(adult, FamilyAiState.GRAZE);
            return;
        }

        double dist = adult.position().distanceTo(boidTarget);
        if (dist > config.herdMaxDistance) {
            setState(adult, FamilyAiState.REGROUP);
            moveSafely(level, adult, boidTarget, config.regroupSpeed);
        } else if (dist > 1.8D) {
            setState(adult, FamilyAiState.FOLLOW_HERD);
            moveSafely(level, adult, boidTarget, config.herdMoveSpeed);
        } else {
            setState(adult, FamilyAiState.GRAZE);
        }
    }

    private static Optional<Entity> detectThreat(Animal animal, FamilyAnimal data, FamilyAiConfig config) {
        if (data.family$isAlert()) {
            Optional<Entity> tracked = FamilyAi.findThreat(animal);
            if (tracked.isPresent()) {
                return tracked;
            }
        }

        Optional<Entity> hostileMob = findNearbyHostileMob(animal, config.dangerDetectionRadius);
        if (hostileMob.isPresent()) {
            return hostileMob;
        }

        Optional<Animal> hurtHerdMate = findHurtHerdMate(animal, config.dangerDetectionRadius);
        if (hurtHerdMate.isPresent()) {
            return Optional.of(hurtHerdMate.get());
        }

        return FamilyAi.findHostilePlayer(animal, config.dangerDetectionRadius).map(player -> player);
    }

    private static Optional<Animal> findTempGuardian(Animal child, double range) {
        AABB box = child.getBoundingBox().inflate(range);
        return child.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> candidate != child
                        && candidate.isAlive()
                        && !candidate.isBaby()
                        && candidate.getType() == child.getType()
                        && FamilyAi.isFamilyAnimal(candidate))
                .stream()
                .min(Comparator.comparingDouble(child::distanceToSqr));
    }

    private static List<Animal> getNearbyHerd(Animal animal, double radius) {
        AABB box = animal.getBoundingBox().inflate(radius);
        return animal.level().getEntitiesOfClass(Animal.class, box, candidate -> candidate != animal
                && candidate.isAlive()
                && !candidate.isBaby()
                && FamilyAi.isFamilyAnimal(candidate)
                && candidate.getType() == animal.getType());
    }

    private static Optional<Vec3> herdCenter(Animal animal, double radius) {
        List<Animal> herd = getNearbyHerd(animal, radius);
        if (herd.isEmpty()) {
            return Optional.empty();
        }
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Animal member : herd) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
        }
        int size = herd.size();
        return Optional.of(new Vec3(x / size, y / size, z / size));
    }

    private static Optional<Animal> resolveLeader(Animal self, List<Animal> herd, FamilyAiConfig config) {
        if (!config.enableHerdLeader) {
            return Optional.empty();
        }

        List<Animal> full = herd.stream().filter(animal -> !animal.isBaby()).toList();
        if (full.isEmpty()) {
            return Optional.of(self);
        }

        Vec3 center = average(full);
        Optional<Animal> leader = full.stream()
                .min(Comparator.comparingDouble(candidate -> candidate.position().distanceToSqr(center)));
        if (leader.isEmpty()) {
            return Optional.empty();
        }

        FamilyAnimal data = (FamilyAnimal) self;
        long age = self.level().getGameTime() - data.family$getStateChangedTick();
        if (data.family$getLeaderUuid().isPresent() && age < config.herdLeaderSwapCooldownTicks) {
            UUID current = data.family$getLeaderUuid().get();
            for (Animal candidate : full) {
                if (candidate.getUUID().equals(current) && candidate.isAlive()) {
                    return Optional.of(candidate);
                }
            }
        }

        return leader;
    }

    private static Vec3 computeHerdTarget(Animal self, List<Animal> herd, FamilyAiConfig config, Animal leader) {
        if (herd.isEmpty()) {
            return null;
        }

        Vec3 separation = Vec3.ZERO;
        Vec3 alignment = Vec3.ZERO;
        Vec3 cohesion = Vec3.ZERO;
        int alignCount = 0;
        int cohesionCount = 0;

        for (Animal neighbor : herd) {
            Vec3 offset = self.position().subtract(neighbor.position());
            double dist = offset.length();
            if (dist < 0.0001D) {
                continue;
            }

            if (dist < config.herdSeparationDistance) {
                separation = separation.add(offset.normalize().scale((config.herdSeparationDistance - dist) / config.herdSeparationDistance));
            }
            alignment = alignment.add(neighbor.getDeltaMovement());
            alignCount++;
            cohesion = cohesion.add(neighbor.position());
            cohesionCount++;
        }

        if (alignCount == 0 || cohesionCount == 0) {
            return null;
        }

        alignment = alignment.scale(1.0D / alignCount);
        cohesion = cohesion.scale(1.0D / cohesionCount).subtract(self.position());

        Vec3 target = self.position()
                .add(separation.scale(1.15D))
                .add(alignment.scale(config.herdAlignmentFactor))
                .add(cohesion.normalize().scale(config.herdCohesionFactor));

        if (leader != null && leader != self) {
            Vec3 toLeader = leader.position().subtract(self.position());
            if (toLeader.lengthSqr() > 0.0001D) {
                target = target.add(toLeader.normalize().scale(0.65D));
            }
        }
        return target;
    }

    private static Optional<Entity> findNearbyHostileMob(Animal animal, double radius) {
        AABB box = animal.getBoundingBox().inflate(radius);
        return animal.level()
                .getEntitiesOfClass(Monster.class, box, monster -> monster.isAlive())
                .stream()
                .min(Comparator.comparingDouble(animal::distanceToSqr))
                .map(monster -> (Entity) monster);
    }

    private static Optional<Animal> findHurtHerdMate(Animal animal, double radius) {
        AABB box = animal.getBoundingBox().inflate(radius);
        return animal.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> candidate != animal
                        && candidate.isAlive()
                        && candidate.getType() == animal.getType()
                        && FamilyAi.isFamilyAnimal(candidate))
                .stream()
                .filter(candidate -> candidate.hurtTime > 0)
                .min(Comparator.comparingDouble(animal::distanceToSqr));
    }

    private static void moveSafely(ServerLevel level, Animal animal, Vec3 target, double speed) {
        Vec3 safeTarget = findSafeTarget(level, animal, target);
        FamilyAnimal data = (FamilyAnimal) animal;

        long now = level.getGameTime();
        FamilyAiConfig config = FamilyAiConfig.get();
        if (now - data.family$getLastPathRecalcTick() < config.pathRecalcCooldownTicks) {
            return;
        }

        data.family$setLastPathRecalcTick(now);
        boolean started = animal.getNavigation().moveTo(safeTarget.x, safeTarget.y, safeTarget.z, speed);
        if (!started) {
            data.family$setPathFailCount(data.family$getPathFailCount() + 1);
            if (data.family$getPathFailCount() >= config.maxPathFailCount) {
                animal.getNavigation().stop();
                data.family$setPathFailCount(0);
            }
        } else if (data.family$getPathFailCount() > 0) {
            data.family$setPathFailCount(data.family$getPathFailCount() - 1);
        }
    }

    private static Vec3 findSafeTarget(ServerLevel level, Animal animal, Vec3 requested) {
        if (isSafe(level, requested)) {
            return requested;
        }

        Vec3 base = animal.position();
        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2.0D / 10.0D) * i;
            Vec3 test = requested.add(Math.cos(angle) * 2.2D, 0.0D, Math.sin(angle) * 2.2D);
            if (isSafe(level, test)) {
                return test;
            }
        }
        return base;
    }

    private static boolean isSafe(ServerLevel level, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockPos below = feet.below();
        BlockState feetState = level.getBlockState(feet);
        BlockState belowState = level.getBlockState(below);
        if (feetState.is(Blocks.LAVA) || belowState.is(Blocks.LAVA)) {
            return false;
        }
        if (feetState.is(Blocks.FIRE) || belowState.is(Blocks.FIRE)) {
            return false;
        }
        if (belowState.isAir()) {
            return false;
        }
        return true;
    }

    private static void tickUnstuck(Animal animal, FamilyAiConfig config, long gameTime) {
        FamilyAnimal data = (FamilyAnimal) animal;
        if (Math.floorMod(gameTime + animal.getId(), config.stuckCheckIntervalTicks) != 0) {
            return;
        }

        Vec3 nowPos = animal.position();
        Vec3 last = new Vec3(data.family$getLastKnownX(), data.family$getLastKnownY(), data.family$getLastKnownZ());
        double moved = nowPos.distanceTo(last);
        data.family$setLastKnownPos(nowPos.x, nowPos.y, nowPos.z);

        if (moved < 0.3D && animal.getNavigation().isInProgress()) {
            int stuck = data.family$getStuckTicks() + config.stuckCheckIntervalTicks;
            data.family$setStuckTicks(stuck);
            if (stuck >= config.stuckRecoveryTicks) {
                setState(animal, FamilyAiState.STUCK_RECOVERY);
                Vec3 recovery = randomNearby(animal).scale(3.0D).add(animal.position());
                moveSafely((ServerLevel) animal.level(), animal, recovery, Math.max(config.regroupSpeed, 0.6D));
                data.family$setStuckTicks(0);
            }
            return;
        }

        if (data.family$getStuckTicks() > 0) {
            data.family$setStuckTicks(Math.max(0, data.family$getStuckTicks() - config.stuckCheckIntervalTicks));
        }
    }

    private static Vec3 randomNearby(Animal animal) {
        double x = animal.getRandom().nextDouble() - 0.5D;
        double z = animal.getRandom().nextDouble() - 0.5D;
        Vec3 vec = new Vec3(x, 0.0D, z);
        if (vec.lengthSqr() < 0.0001D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        return vec.normalize();
    }

    private static Vec3 average(List<Animal> animals) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (Animal animal : animals) {
            x += animal.getX();
            y += animal.getY();
            z += animal.getZ();
        }
        return new Vec3(x / animals.size(), y / animals.size(), z / animals.size());
    }

    private static void setState(Animal animal, FamilyAiState next) {
        FamilyAnimal data = (FamilyAnimal) animal;
        FamilyAiState current = data.family$getAiState();
        if (current == next) {
            return;
        }
        data.family$setAiState(next);
        data.family$setStateChangedTick(animal.level().getGameTime());
    }
}

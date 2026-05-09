package com.example.familyai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class FamilyAi {
    private FamilyAi() {
    }

    public static void onAnimalLoaded(Animal animal) {
        if (!isFamilyAnimal(animal)) {
            return;
        }

        FamilyAnimal data = (FamilyAnimal) animal;
        if (data.family$getSex() == FamilySex.UNKNOWN) {
            data.family$setSex(FamilySex.random(animal.getRandom()));
        }
    }

    public static void linkBirth(Animal parentA, Animal parentB, Animal child) {
        if (!isFamilyAnimal(parentA) || !isFamilyAnimal(parentB) || !isFamilyAnimal(child)) {
            return;
        }

        FamilyAnimal a = (FamilyAnimal) parentA;
        FamilyAnimal b = (FamilyAnimal) parentB;
        FamilyAnimal c = (FamilyAnimal) child;

        if (a.family$getSex() == FamilySex.UNKNOWN) {
            a.family$setSex(FamilySex.random(parentA.getRandom()));
        }
        if (b.family$getSex() == FamilySex.UNKNOWN) {
            b.family$setSex(FamilySex.random(parentB.getRandom()));
        }

        Animal mother = b.family$getSex() == FamilySex.FEMALE && a.family$getSex() != FamilySex.FEMALE
                ? parentB
                : parentA;
        Animal father = mother == parentA ? parentB : parentA;

        c.family$setMotherUuid(mother.getUUID());
        c.family$setFatherUuid(father.getUUID());
        c.family$setSex(FamilySex.random(child.getRandom()));

        a.family$setPartnerUuid(parentB.getUUID());
        b.family$setPartnerUuid(parentA.getUUID());
        a.family$addChildUuid(child.getUUID());
        b.family$addChildUuid(child.getUUID());
    }

    public static void onChildThreatened(Animal child, Entity threat) {
        if (!(child.level() instanceof ServerLevel level) || !child.isBaby() || !isFamilyAnimal(child)) {
            return;
        }

        UUID threatUuid = threat == null ? null : threat.getUUID();
        FamilyAnimal childData = (FamilyAnimal) child;
        FamilyAiConfig config = FamilyAiConfig.get();

        if (childData.family$isAlert() && childData.family$getAlertCooldownTicks() > 0) {
            return;
        }

        childData.family$alert(threatUuid, child.getUUID(), config.alertTicks, config.alertCooldownTicks);

        alertParent(level, childData.family$getMotherUuid(), threatUuid, child.getUUID(), config);
        alertParent(level, childData.family$getFatherUuid(), threatUuid, child.getUUID(), config);
        alertFallbackParents(level, child, threatUuid, config);
    }

    public static Optional<Animal> findPreferredParent(Animal child) {
        if (!(child.level() instanceof ServerLevel level) || !isFamilyAnimal(child)) {
            return Optional.empty();
        }

        FamilyAnimal data = (FamilyAnimal) child;
        Optional<Animal> mother = findAnimal(level, data.family$getMotherUuid()).filter(Entity::isAlive);
        if (mother.isPresent()) {
            return mother;
        }

        Optional<Animal> fallbackMother = findFallbackParent(child, FamilySex.FEMALE);
        if (fallbackMother.isPresent()) {
            return fallbackMother;
        }

        Optional<Animal> father = findAnimal(level, data.family$getFatherUuid()).filter(Entity::isAlive);
        if (father.isPresent()) {
            return father;
        }

        return findFallbackParent(child, FamilySex.MALE);
    }

    public static Optional<Animal> findMate(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level) || !isFamilyAnimal(animal)) {
            return Optional.empty();
        }
        return findAnimal(level, ((FamilyAnimal) animal).family$getPartnerUuid()).filter(Entity::isAlive);
    }

    public static Optional<Animal> findAlertChild(Animal parent) {
        if (!(parent.level() instanceof ServerLevel level) || !isFamilyAnimal(parent)) {
            return Optional.empty();
        }
        return findAnimal(level, ((FamilyAnimal) parent).family$getAlertChildUuid()).filter(Entity::isAlive);
    }

    public static Optional<Entity> findThreat(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return ((FamilyAnimal) animal).family$getThreatUuid()
                .map(level::getEntityInAnyDimension)
                .filter(Entity::isAlive);
    }

    public static Optional<Player> findHostilePlayer(Animal child, double radius) {
        AABB box = child.getBoundingBox().inflate(radius);
        return child.level()
                .getEntitiesOfClass(Player.class, box, player -> !player.isSpectator() && !player.isCreative())
                .stream()
                .filter(FamilyAi::looksHostile)
                .findFirst();
    }

    public static boolean isFamilyAnimal(Animal animal) {
        return FamilyAiTags.isFamilyAnimal(animal);
    }

    public static Vec3 refugePointBehindParent(Animal parent, Entity threat) {
        Vec3 parentPos = parent.position();
        if (threat == null) {
            return parentPos;
        }

        Vec3 awayFromThreat = parentPos.subtract(threat.position());
        if (awayFromThreat.lengthSqr() < 0.0001D) {
            return parentPos;
        }

        return parentPos.add(awayFromThreat.normalize().scale(1.6D));
    }

    public static Vec3 guardPointBetweenThreatAndChild(Animal child, Entity threat) {
        if (threat == null) {
            return child.position();
        }

        Vec3 towardThreat = threat.position().subtract(child.position());
        if (towardThreat.lengthSqr() < 0.0001D) {
            return child.position();
        }

        return child.position().add(towardThreat.normalize().scale(1.7D));
    }

    public static Vec3 mateCohesionPoint(Animal animal, Animal mate, double radius) {
        Vec3 fromMate = animal.position().subtract(mate.position());
        if (fromMate.lengthSqr() < 0.0001D) {
            return mate.position();
        }
        return mate.position().add(fromMate.normalize().scale(radius * 0.65D));
    }

    private static void alertParent(ServerLevel level, Optional<UUID> parentUuid, UUID threatUuid, UUID childUuid, FamilyAiConfig config) {
        parentUuid
                .map(level::getEntityInAnyDimension)
                .filter(Animal.class::isInstance)
                .map(Animal.class::cast)
                .filter(Entity::isAlive)
                .filter(FamilyAi::isFamilyAnimal)
                .ifPresent(parent -> ((FamilyAnimal) parent).family$alert(threatUuid, childUuid, config.alertTicks, config.alertCooldownTicks));
    }

    private static void alertFallbackParents(ServerLevel level, Animal child, UUID threatUuid, FamilyAiConfig config) {
        AABB box = child.getBoundingBox().inflate(config.parentFallbackRange);
        level.getEntitiesOfClass(Animal.class, box, candidate -> isRecognizedFallbackParent(child, candidate))
                .forEach(parent -> ((FamilyAnimal) parent).family$alert(threatUuid, child.getUUID(), config.alertTicks, config.alertCooldownTicks));
    }

    private static Optional<Animal> findFallbackParent(Animal child, FamilySex preferredSex) {
        FamilyAiConfig config = FamilyAiConfig.get();
        AABB box = child.getBoundingBox().inflate(config.parentFallbackRange);

        return child.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> isRecognizedFallbackParent(child, candidate))
                .stream()
                .filter(candidate -> preferredSex == FamilySex.UNKNOWN || ((FamilyAnimal) candidate).family$getSex() == preferredSex)
                .min(Comparator.comparingDouble(child::distanceToSqr))
                .or(() -> child.level()
                        .getEntitiesOfClass(Animal.class, box, candidate -> isRecognizedFallbackParent(child, candidate))
                        .stream()
                        .min(Comparator.comparingDouble(child::distanceToSqr)));
    }

    private static boolean isRecognizedFallbackParent(Animal child, Animal candidate) {
        return candidate != child
                && !candidate.isBaby()
                && candidate.isAlive()
                && isFamilyAnimal(candidate)
                && candidate.getType() == child.getType()
                && ((FamilyAnimal) candidate).family$getChildUuids().contains(child.getUUID());
    }

    private static Optional<Animal> findAnimal(ServerLevel level, Optional<UUID> uuid) {
        return uuid
                .map(level::getEntityInAnyDimension)
                .filter(Animal.class::isInstance)
                .map(Animal.class::cast);
    }

    private static boolean looksHostile(Player player) {
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();
        return player.isSprinting()
                || stack.is(ItemTags.SWORDS)
                || item instanceof AxeItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem;
    }
}

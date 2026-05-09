package com.example.familyai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FamilyAi {
    private static final int DEFAULT_BABY_AGE_TICKS = -24000;
    private static final Map<UUID, Long> FEED_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> SPRINT_PENALTY_COOLDOWNS = new HashMap<>();
    private static final Map<Long, Long> CHUNK_FAMILY_SPAWN_COOLDOWNS = new HashMap<>();

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

        tryNaturalFamilySpawn(animal, data);
    }

    public static void linkBirth(Animal parentA, Animal parentB, Animal child) {
        if (!isFamilyAnimal(parentA) || !isFamilyAnimal(parentB) || !isFamilyAnimal(child)) {
            return;
        }

        FamilyAnimal a = (FamilyAnimal) parentA;
        FamilyAnimal b = (FamilyAnimal) parentB;
        FamilyAnimal c = (FamilyAnimal) child;

        ensureSex(parentA, a);
        ensureSex(parentB, b);

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

        linkSiblings(parentA, parentB, child);
        onBreedingSuccess(parentA, parentB);
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

    public static boolean onAnimalFed(Animal animal, Player player) {
        if (!(animal.level() instanceof ServerLevel level) || !isFamilyAnimal(animal) || player == null || player.isSpectator()) {
            return false;
        }

        ReputationState state = getReputationState(animal, player.getUUID());
        if (state == ReputationState.HOSTILE) {
            return false;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        if (!isCooldownReady(FEED_COOLDOWNS, player.getUUID(), level.getGameTime(), config.feedReputationCooldownTicks)) {
            return true;
        }

        if (config.reputationGainFeed > 0) {
            applyReputationChange(animal, player.getUUID(), config.reputationGainFeed, true);
        }
        return true;
    }

    public static void onAnimalHurtByPlayer(Animal animal, Player player) {
        if (!(animal.level() instanceof ServerLevel) || !isFamilyAnimal(animal) || player == null || player.isSpectator() || player.isCreative()) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        int loss = animal.isBaby() ? config.reputationLossBabyHit : config.reputationLossAdultHit;
        if (loss > 0) {
            applyReputationChange(animal, player.getUUID(), -loss, true);
        }

        if (animal.isBaby()) {
            onChildThreatened(animal, player);
        }
    }

    public static void onLivingEntityKilledByPlayer(LivingEntity victim, Player player) {
        if (!(victim.level() instanceof ServerLevel level) || player == null || player.isSpectator() || player.isCreative()) {
            return;
        }

        if (victim instanceof Animal animal && isFamilyAnimal(animal)) {
            int loss = FamilyAiConfig.get().reputationLossKill;
            if (loss > 0) {
                applyReputationChange(animal, player.getUUID(), -loss, true);
            }
            return;
        }

        rewardDefenderIfApplicable(level, victim, player);
    }

    public static void tickFamilyAi(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level) || !isFamilyAnimal(animal)) {
            return;
        }

        tickReputationDecay(animal, level.getGameTime());
        tickWeaponSprintPenalty(animal, level.getGameTime());
        cleanupCooldownMaps(level.getGameTime());
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

    public static Optional<Animal> findPlaymate(Animal child) {
        if (!child.isBaby() || !isFamilyAnimal(child) || !FamilyAiConfig.get().enableSiblingPlay) {
            return Optional.empty();
        }

        FamilyAnimal data = (FamilyAnimal) child;
        if (data.family$getSiblingUuids().isEmpty()
                && data.family$getMotherUuid().isEmpty()
                && data.family$getFatherUuid().isEmpty()) {
            return Optional.empty();
        }

        double range = FamilyAiConfig.get().siblingPlayRange;
        AABB box = child.getBoundingBox().inflate(range);
        return child.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> isRecognizedSibling(child, data, candidate))
                .stream()
                .min(Comparator.comparingDouble(child::distanceToSqr));
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
                .filter(player -> isPlayerThreateningBaby(child, player))
                .min(Comparator.comparingDouble(child::distanceToSqr));
    }

    public static boolean isHostileTo(Animal animal, Player player) {
        if (!isFamilyAnimal(animal) || player == null || player.isSpectator() || player.isCreative()) {
            return false;
        }
        return getReputationState(animal, player.getUUID()) == ReputationState.HOSTILE;
    }

    public static ReputationState getReputationState(Animal animal, UUID playerUuid) {
        if (!isFamilyAnimal(animal) || playerUuid == null) {
            return ReputationState.NEUTRAL;
        }
        return ReputationState.fromScore(((FamilyAnimal) animal).family$getReputation(playerUuid));
    }

    public static int getReputationFor(Animal animal, UUID playerUuid) {
        if (!isFamilyAnimal(animal) || playerUuid == null) {
            return 0;
        }
        return ((FamilyAnimal) animal).family$getReputation(playerUuid);
    }

    public static int clampReputation(int value) {
        FamilyAiConfig config = FamilyAiConfig.get();
        return Math.max(config.reputationMin, Math.min(config.reputationMax, value));
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

    private static void ensureSex(Animal animal, FamilyAnimal data) {
        if (data.family$getSex() == FamilySex.UNKNOWN) {
            data.family$setSex(FamilySex.random(animal.getRandom()));
        }
    }

    private static void linkSiblings(Animal parentA, Animal parentB, Animal child) {
        FamilyAnimal a = (FamilyAnimal) parentA;
        FamilyAnimal b = (FamilyAnimal) parentB;
        FamilyAnimal c = (FamilyAnimal) child;

        Set<UUID> knownSiblingUuids = new HashSet<>(a.family$getChildUuids());
        knownSiblingUuids.addAll(b.family$getChildUuids());
        knownSiblingUuids.remove(child.getUUID());

        for (UUID siblingUuid : knownSiblingUuids) {
            c.family$addSiblingUuid(siblingUuid);
        }

        if (!(child.level() instanceof ServerLevel level)) {
            return;
        }

        for (UUID siblingUuid : knownSiblingUuids) {
            Entity siblingEntity = level.getEntityInAnyDimension(siblingUuid);
            if (siblingEntity instanceof Animal siblingAnimal && siblingAnimal.isAlive() && isFamilyAnimal(siblingAnimal)) {
                ((FamilyAnimal) siblingAnimal).family$addSiblingUuid(child.getUUID());
            }
        }
    }

    private static void onBreedingSuccess(Animal parentA, Animal parentB) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (config.reputationGainBreedingAssist <= 0) {
            return;
        }

        Set<UUID> helpers = new HashSet<>();
        addBreedingHelper(helpers, parentA);
        addBreedingHelper(helpers, parentB);
        if (helpers.isEmpty()) {
            return;
        }

        for (UUID helper : helpers) {
            applyReputationChange(parentA, helper, config.reputationGainBreedingAssist, true);
            applyReputationChange(parentB, helper, config.reputationGainBreedingAssist, true);
        }
    }

    private static void addBreedingHelper(Set<UUID> helpers, Animal parent) {
        ServerPlayer loveCause = parent.getLoveCause();
        if (loveCause != null) {
            helpers.add(loveCause.getUUID());
        }
    }

    private static void rewardDefenderIfApplicable(ServerLevel level, LivingEntity victim, Player player) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (config.reputationGainDefendHerd <= 0) {
            return;
        }

        AABB box = victim.getBoundingBox().inflate(config.defendRewardRange);
        List<Animal> nearbyFamily = level.getEntitiesOfClass(Animal.class, box, candidate -> isFamilyAnimal(candidate) && !candidate.isBaby());
        if (nearbyFamily.isEmpty()) {
            return;
        }

        List<Animal> validWitnesses = new ArrayList<>();
        UUID victimUuid = victim.getUUID();
        for (Animal witness : nearbyFamily) {
            FamilyAnimal family = (FamilyAnimal) witness;
            if (family.family$isAlert() && family.family$getThreatUuid().filter(victimUuid::equals).isPresent()) {
                validWitnesses.add(witness);
            }
        }

        if (validWitnesses.isEmpty()) {
            return;
        }

        for (Animal witness : validWitnesses) {
            applyReputationChange(witness, player.getUUID(), config.reputationGainDefendHerd, true);
        }
    }

    private static void tickReputationDecay(Animal animal, long gameTime) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (config.reputationDecayStep <= 0 || config.reputationDecayIntervalTicks <= 0) {
            return;
        }

        int interval = config.reputationDecayIntervalTicks;
        int phase = Math.floorMod(animal.getId(), interval);
        if (Math.floorMod(gameTime, interval) != phase) {
            return;
        }

        FamilyAnimal family = (FamilyAnimal) animal;
        Map<UUID, Integer> snapshot = new HashMap<>(family.family$getReputationMap());
        for (Map.Entry<UUID, Integer> entry : snapshot.entrySet()) {
            int value = entry.getValue();
            if (value == 0) {
                continue;
            }

            int step = Math.min(config.reputationDecayStep, Math.abs(value));
            int next = value > 0 ? value - step : value + step;
            family.family$setReputation(entry.getKey(), next);
        }
    }

    private static void tickWeaponSprintPenalty(Animal animal, long gameTime) {
        if (!animal.isBaby()) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        if (config.reputationLossWeaponSprintNearBaby <= 0) {
            return;
        }

        AABB box = animal.getBoundingBox().inflate(config.hostileScanRange);
        Collection<Player> nearbyPlayers = animal.level().getEntitiesOfClass(Player.class, box, player -> !player.isSpectator() && !player.isCreative());
        for (Player player : nearbyPlayers) {
            if (!player.isSprinting() || !looksHostile(player)) {
                continue;
            }

            if (getReputationState(animal, player.getUUID()) == ReputationState.TRUSTED) {
                continue;
            }

            if (!isCooldownReady(SPRINT_PENALTY_COOLDOWNS, player.getUUID(), gameTime, config.weaponSprintPenaltyCooldownTicks)) {
                continue;
            }

            applyReputationChange(animal, player.getUUID(), -config.reputationLossWeaponSprintNearBaby, true);
            onChildThreatened(animal, player);
        }
    }

    private static void cleanupCooldownMaps(long gameTime) {
        if (gameTime % (20L * 90L) != 0L) {
            return;
        }

        long staleLimit = gameTime - 20L * 60L * 15L;
        FEED_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < staleLimit);
        SPRINT_PENALTY_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < staleLimit);
        CHUNK_FAMILY_SPAWN_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < staleLimit);
    }

    private static boolean isCooldownReady(Map<UUID, Long> cooldownMap, UUID playerUuid, long gameTime, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return true;
        }

        long lastTick = cooldownMap.getOrDefault(playerUuid, Long.MIN_VALUE / 4L);
        if (gameTime - lastTick < cooldownTicks) {
            return false;
        }

        cooldownMap.put(playerUuid, gameTime);
        return true;
    }

    private static boolean isPlayerThreateningBaby(Animal baby, Player player) {
        ReputationState state = getReputationState(baby, player.getUUID());
        if (state == ReputationState.TRUSTED) {
            return false;
        }
        if (state == ReputationState.HOSTILE) {
            return true;
        }
        return state == ReputationState.WARY || (player.isSprinting() && looksHostile(player));
    }

    private static void applyReputationChange(Animal source, UUID playerUuid, int baseDelta, boolean spreadToHerd) {
        if (playerUuid == null || baseDelta == 0 || !isFamilyAnimal(source)) {
            return;
        }

        ((FamilyAnimal) source).family$addReputation(playerUuid, baseDelta);
        if (!spreadToHerd) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        int herdDelta = scaledDelta(baseDelta, config.herdGossipFactor);
        if (herdDelta == 0) {
            return;
        }

        AABB box = source.getBoundingBox().inflate(config.herdGossipRange);
        source.level()
                .getEntitiesOfClass(Animal.class, box, candidate -> candidate != source
                        && candidate.isAlive()
                        && candidate.getType() == source.getType()
                        && isFamilyAnimal(candidate))
                .forEach(candidate -> ((FamilyAnimal) candidate).family$addReputation(playerUuid, herdDelta));
    }

    private static int scaledDelta(int delta, double factor) {
        if (delta == 0 || factor <= 0.0D) {
            return 0;
        }

        int scaled = (int) Math.round(delta * factor);
        if (scaled == 0) {
            return delta > 0 ? 1 : -1;
        }
        return scaled;
    }

    private static void tryNaturalFamilySpawn(Animal seed, FamilyAnimal seedData) {
        if (!(seed.level() instanceof ServerLevel level) || seed.isBaby()) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableNaturalFamilySpawns || config.naturalFamilySpawnChance <= 0.0D) {
            return;
        }
        if (!isSupportedNaturalFamilySpecies(seed.getType())) {
            return;
        }

        // Heuristic for fresh natural-like adults: first ticks, no known parents, no registered children.
        if (seed.tickCount > 2) {
            return;
        }
        if (seedData.family$getMotherUuid().isPresent() || seedData.family$getFatherUuid().isPresent()) {
            return;
        }
        if (!seedData.family$getChildUuids().isEmpty()) {
            return;
        }

        if (seed.getRandom().nextDouble() > config.naturalFamilySpawnChance) {
            return;
        }

        long chunkKey = seed.chunkPosition().toLong();
        long now = level.getGameTime();
        long last = CHUNK_FAMILY_SPAWN_COOLDOWNS.getOrDefault(chunkKey, Long.MIN_VALUE / 4L);
        if (now - last < config.naturalFamilyChunkCooldownTicks) {
            return;
        }

        AABB localBox = seed.getBoundingBox().inflate(config.naturalFamilySpawnRadius);
        long localCount = level.getEntitiesOfClass(Animal.class, localBox, candidate -> candidate.getType() == seed.getType()).size();
        if (localCount >= config.naturalFamilyLocalCap) {
            return;
        }

        spawnNaturalFamilyGroup(level, seed);
        CHUNK_FAMILY_SPAWN_COOLDOWNS.put(chunkKey, now);
    }

    private static void spawnNaturalFamilyGroup(ServerLevel level, Animal seed) {
        FamilyAiConfig config = FamilyAiConfig.get();
        boolean chicken = seed.getType() == EntityType.CHICKEN;

        int totalAdults = randomBetween(seed, chicken ? config.chickenAdultsMin : config.herdAdultsMin, chicken ? config.chickenAdultsMax : config.herdAdultsMax);
        int totalBabies = randomBetween(seed, chicken ? config.chickenBabiesMin : config.herdBabiesMin, chicken ? config.chickenBabiesMax : config.herdBabiesMax);
        int extraAdults = Math.max(0, totalAdults - 1);

        List<Animal> adults = new ArrayList<>();
        adults.add(seed);
        for (int i = 0; i < extraAdults; i++) {
            Animal spawnedAdult = spawnRelative(seed, false, level);
            if (spawnedAdult != null) {
                adults.add(spawnedAdult);
            }
        }

        if (adults.isEmpty()) {
            return;
        }

        Animal mother = adults.stream()
                .filter(animal -> ((FamilyAnimal) animal).family$getSex() == FamilySex.FEMALE)
                .findFirst()
                .orElse(adults.get(0));

        Animal father = adults.stream()
                .filter(animal -> animal != mother)
                .filter(animal -> ((FamilyAnimal) animal).family$getSex() != FamilySex.FEMALE)
                .findFirst()
                .orElseGet(() -> adults.size() > 1 ? adults.get(1) : adults.get(0));

        if (mother != father) {
            ((FamilyAnimal) mother).family$setPartnerUuid(father.getUUID());
            ((FamilyAnimal) father).family$setPartnerUuid(mother.getUUID());
        }

        List<Animal> babies = new ArrayList<>();
        for (int i = 0; i < totalBabies; i++) {
            Animal baby = spawnRelative(seed, true, level);
            if (baby == null) {
                continue;
            }

            FamilyAnimal babyData = (FamilyAnimal) baby;
            babyData.family$setMotherUuid(mother.getUUID());
            babyData.family$setFatherUuid(father.getUUID());
            babyData.family$setSex(FamilySex.random(baby.getRandom()));
            ((FamilyAnimal) mother).family$addChildUuid(baby.getUUID());
            ((FamilyAnimal) father).family$addChildUuid(baby.getUUID());
            babies.add(baby);
        }

        for (int i = 0; i < babies.size(); i++) {
            FamilyAnimal left = (FamilyAnimal) babies.get(i);
            for (int j = i + 1; j < babies.size(); j++) {
                FamilyAnimal right = (FamilyAnimal) babies.get(j);
                left.family$addSiblingUuid(babies.get(j).getUUID());
                right.family$addSiblingUuid(babies.get(i).getUUID());
            }
        }
    }

    private static Animal spawnRelative(Animal seed, boolean baby, ServerLevel level) {
        Entity maybeEntity = seed.getType().create(level, EntitySpawnReason.NATURAL);
        if (!(maybeEntity instanceof Animal spawned) || !isFamilyAnimal(spawned)) {
            return null;
        }

        double radius = FamilyAiConfig.get().naturalFamilySpawnRadius;
        double dx = (seed.getRandom().nextDouble() - 0.5D) * radius;
        double dz = (seed.getRandom().nextDouble() - 0.5D) * radius;
        spawned.setPos(seed.getX() + dx, seed.getY(), seed.getZ() + dz);

        if (baby) {
            spawned.setAge(DEFAULT_BABY_AGE_TICKS);
        }

        level.addFreshEntity(spawned);
        onAnimalLoaded(spawned);
        return spawned;
    }

    private static int randomBetween(Animal source, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + source.getRandom().nextInt(max - min + 1);
    }

    private static boolean isSupportedNaturalFamilySpecies(EntityType<?> type) {
        return type == EntityType.COW
                || type == EntityType.SHEEP
                || type == EntityType.GOAT
                || type == EntityType.CHICKEN;
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

    private static boolean isRecognizedSibling(Animal child, FamilyAnimal childData, Animal candidate) {
        if (candidate == child
                || !candidate.isAlive()
                || !candidate.isBaby()
                || candidate.getType() != child.getType()
                || !isFamilyAnimal(candidate)) {
            return false;
        }

        FamilyAnimal candidateData = (FamilyAnimal) candidate;
        return childData.family$getSiblingUuids().contains(candidate.getUUID())
                || candidateData.family$getSiblingUuids().contains(child.getUUID())
                || sharesParent(childData, candidateData);
    }

    private static boolean sharesParent(FamilyAnimal first, FamilyAnimal second) {
        return sameParent(first.family$getMotherUuid(), second.family$getMotherUuid())
                || sameParent(first.family$getFatherUuid(), second.family$getFatherUuid());
    }

    private static boolean sameParent(Optional<UUID> a, Optional<UUID> b) {
        return a.isPresent() && b.isPresent() && a.get().equals(b.get());
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
        return stack.is(ItemTags.SWORDS)
                || item instanceof AxeItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem;
    }

}

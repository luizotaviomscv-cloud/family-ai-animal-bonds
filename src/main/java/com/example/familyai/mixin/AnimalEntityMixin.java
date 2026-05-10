package com.example.familyai.mixin;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAiState;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.FamilySex;
import com.example.familyai.goal.KeepDistanceFromPlayerGoal;
import com.example.familyai.goal.PlayWithSiblingGoal;
import com.example.familyai.goal.ProtectChildGoal;
import com.example.familyai.goal.RunToParentGoal;
import com.example.familyai.goal.StayNearMateGoal;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(Animal.class)
public abstract class AnimalEntityMixin implements FamilyAnimal {
    private static final String ROOT_KEY = "FamilyAi";
    private static final String MOTHER_KEY = "Mother";
    private static final String FATHER_KEY = "Father";
    private static final String PARTNER_KEY = "Partner";
    private static final String CHILDREN_KEY = "Children";
    private static final String SIBLINGS_KEY = "Siblings";
    private static final String REPUTATION_KEY = "Reputation";
    private static final String SEX_KEY = "Sex";
    private static final String LAST_PLAY_TICK_KEY = "LastPlayTick";
    private static final String DATA_VERSION_KEY = "DataVersion";
    private static final String AI_STATE_KEY = "AiState";
    private static final String STATE_CHANGED_TICK_KEY = "StateChangedTick";
    private static final String LAST_THREAT_TICK_KEY = "LastThreatTick";
    private static final String PANIC_COOLDOWN_KEY = "PanicCooldown";
    private static final String LAST_PATH_RECALC_TICK_KEY = "LastPathRecalcTick";
    private static final String STUCK_TICKS_KEY = "StuckTicks";
    private static final String PATH_FAIL_COUNT_KEY = "PathFailCount";
    private static final String LAST_POS_X_KEY = "LastX";
    private static final String LAST_POS_Y_KEY = "LastY";
    private static final String LAST_POS_Z_KEY = "LastZ";
    private static final String LEADER_KEY = "Leader";
    private static final String TEMP_GUARDIAN_KEY = "TempGuardian";
    private static final String NATURAL_SPAWN_PROCESSED_KEY = "NaturalSpawnProcessed";
    private static final String TRAIT_FEAR_KEY = "TraitFear";
    private static final String TRAIT_PROTECTIVE_KEY = "TraitProtective";
    private static final String TRAIT_HERD_AFFINITY_KEY = "TraitHerdAffinity";
    private static final int CURRENT_DATA_VERSION = 3;

    private UUID familyAi$motherUuid;
    private UUID familyAi$fatherUuid;
    private UUID familyAi$partnerUuid;
    private UUID familyAi$threatUuid;
    private UUID familyAi$alertChildUuid;
    private UUID familyAi$leaderUuid;
    private UUID familyAi$tempGuardianUuid;
    private final Set<UUID> familyAi$childUuids = new HashSet<>();
    private final Set<UUID> familyAi$siblingUuids = new HashSet<>();
    private final Map<UUID, Integer> familyAi$reputation = new HashMap<>();
    private FamilySex familyAi$sex = FamilySex.UNKNOWN;
    private FamilyAiState familyAi$state = FamilyAiState.IDLE;
    private int familyAi$alertTicks;
    private int familyAi$alertCooldownTicks;
    private int familyAi$panicCooldownTicks;
    private int familyAi$stuckTicks;
    private int familyAi$pathFailCount;
    private long familyAi$lastPlayTick;
    private long familyAi$lastThreatTick;
    private long familyAi$stateChangedTick;
    private long familyAi$lastPathRecalcTick;
    private double familyAi$lastKnownX;
    private double familyAi$lastKnownY;
    private double familyAi$lastKnownZ;
    private double familyAi$traitFear = 0.5D;
    private double familyAi$traitProtective = 0.5D;
    private double familyAi$traitHerdAffinity = 0.5D;
    private boolean familyAi$naturalSpawnProcessed;
    private int familyAi$dataVersion = CURRENT_DATA_VERSION;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void familyAi$addGoals(EntityType<? extends Animal> type, Level level, CallbackInfo ci) {
        Animal animal = (Animal) (Object) this;
        if (!FamilyAi.isFamilyAnimal(animal)) {
            return;
        }

        FamilyAiConfig config = FamilyAiConfig.get();
        MobEntityAccessor accessor = (MobEntityAccessor) animal;
        accessor.familyAi$getGoalSelector().addGoal(0, new RunToParentGoal(animal, config.childFollowSpeed, config.childStopDistance));
        accessor.familyAi$getGoalSelector().addGoal(1, new ProtectChildGoal(animal, config.parentProtectSpeed));
        accessor.familyAi$getGoalSelector().addGoal(2, new KeepDistanceFromPlayerGoal(animal));
        accessor.familyAi$getGoalSelector().addGoal(5, new PlayWithSiblingGoal(animal, config.siblingPlaySpeed));
        accessor.familyAi$getGoalSelector().addGoal(6, new StayNearMateGoal(animal, config.mateCohesionSpeed, config.mateCohesionRadius));

        familyAi$ensureTraits(animal);
        family$setLastKnownPos(animal.getX(), animal.getY(), animal.getZ());
    }

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void familyAi$linkChildBeforeSpawn(ServerLevel level, Animal mate, AgeableMob baby, CallbackInfo ci) {
        if (baby instanceof Animal child) {
            FamilyAi.linkBirth((Animal) (Object) this, mate, child);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void familyAi$writeNbt(ValueOutput output, CallbackInfo ci) {
        ValueOutput family = output.child(ROOT_KEY);
        putUuid(family, MOTHER_KEY, familyAi$motherUuid);
        putUuid(family, FATHER_KEY, familyAi$fatherUuid);
        putUuid(family, PARTNER_KEY, familyAi$partnerUuid);
        putUuid(family, LEADER_KEY, familyAi$leaderUuid);
        putUuid(family, TEMP_GUARDIAN_KEY, familyAi$tempGuardianUuid);
        family.putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
        family.putString(SEX_KEY, familyAi$sex.name());
        family.putString(AI_STATE_KEY, familyAi$state.name());
        family.putLong(STATE_CHANGED_TICK_KEY, familyAi$stateChangedTick);
        family.putLong(LAST_THREAT_TICK_KEY, familyAi$lastThreatTick);
        family.putInt(PANIC_COOLDOWN_KEY, familyAi$panicCooldownTicks);
        family.putLong(LAST_PATH_RECALC_TICK_KEY, familyAi$lastPathRecalcTick);
        family.putInt(STUCK_TICKS_KEY, familyAi$stuckTicks);
        family.putInt(PATH_FAIL_COUNT_KEY, familyAi$pathFailCount);
        family.putDouble(LAST_POS_X_KEY, familyAi$lastKnownX);
        family.putDouble(LAST_POS_Y_KEY, familyAi$lastKnownY);
        family.putDouble(LAST_POS_Z_KEY, familyAi$lastKnownZ);
        family.putDouble(TRAIT_FEAR_KEY, familyAi$traitFear);
        family.putDouble(TRAIT_PROTECTIVE_KEY, familyAi$traitProtective);
        family.putDouble(TRAIT_HERD_AFFINITY_KEY, familyAi$traitHerdAffinity);
        family.putBoolean(NATURAL_SPAWN_PROCESSED_KEY, familyAi$naturalSpawnProcessed);

        ValueOutput.TypedOutputList<String> children = family.list(CHILDREN_KEY, Codec.STRING);
        for (UUID childUuid : familyAi$childUuids) {
            children.add(childUuid.toString());
        }

        ValueOutput.TypedOutputList<String> siblings = family.list(SIBLINGS_KEY, Codec.STRING);
        for (UUID siblingUuid : familyAi$siblingUuids) {
            siblings.add(siblingUuid.toString());
        }

        ValueOutput.TypedOutputList<String> reputation = family.list(REPUTATION_KEY, Codec.STRING);
        for (Map.Entry<UUID, Integer> entry : familyAi$reputation.entrySet()) {
            reputation.add(entry.getKey() + "=" + entry.getValue());
        }

        family.putLong(LAST_PLAY_TICK_KEY, familyAi$lastPlayTick);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void familyAi$readNbt(ValueInput input, CallbackInfo ci) {
        familyAi$motherUuid = null;
        familyAi$fatherUuid = null;
        familyAi$partnerUuid = null;
        familyAi$leaderUuid = null;
        familyAi$tempGuardianUuid = null;
        familyAi$childUuids.clear();
        familyAi$siblingUuids.clear();
        familyAi$reputation.clear();
        familyAi$sex = FamilySex.UNKNOWN;
        familyAi$state = FamilyAiState.IDLE;
        familyAi$lastPlayTick = 0L;
        familyAi$lastThreatTick = 0L;
        familyAi$stateChangedTick = 0L;
        familyAi$panicCooldownTicks = 0;
        familyAi$stuckTicks = 0;
        familyAi$pathFailCount = 0;
        familyAi$lastPathRecalcTick = 0L;
        familyAi$lastKnownX = 0.0D;
        familyAi$lastKnownY = 0.0D;
        familyAi$lastKnownZ = 0.0D;
        familyAi$naturalSpawnProcessed = false;
        familyAi$dataVersion = 0;
        familyAi$traitFear = 0.5D;
        familyAi$traitProtective = 0.5D;
        familyAi$traitHerdAffinity = 0.5D;

        ValueInput family = input.childOrEmpty(ROOT_KEY);
        familyAi$dataVersion = family.getIntOr(DATA_VERSION_KEY, 0);
        familyAi$motherUuid = getUuid(family, MOTHER_KEY);
        familyAi$fatherUuid = getUuid(family, FATHER_KEY);
        familyAi$partnerUuid = getUuid(family, PARTNER_KEY);
        familyAi$leaderUuid = getUuid(family, LEADER_KEY);
        familyAi$tempGuardianUuid = getUuid(family, TEMP_GUARDIAN_KEY);
        familyAi$sex = FamilySex.fromName(family.getStringOr(SEX_KEY, FamilySex.UNKNOWN.name()));
        familyAi$state = familyAi$readState(family.getStringOr(AI_STATE_KEY, FamilyAiState.IDLE.name()));
        familyAi$stateChangedTick = Math.max(0L, family.getLongOr(STATE_CHANGED_TICK_KEY, 0L));
        familyAi$lastThreatTick = Math.max(0L, family.getLongOr(LAST_THREAT_TICK_KEY, 0L));
        familyAi$panicCooldownTicks = Math.max(0, family.getIntOr(PANIC_COOLDOWN_KEY, 0));
        familyAi$lastPathRecalcTick = Math.max(0L, family.getLongOr(LAST_PATH_RECALC_TICK_KEY, 0L));
        familyAi$stuckTicks = Math.max(0, family.getIntOr(STUCK_TICKS_KEY, 0));
        familyAi$pathFailCount = Math.max(0, family.getIntOr(PATH_FAIL_COUNT_KEY, 0));
        familyAi$lastKnownX = family.getDoubleOr(LAST_POS_X_KEY, 0.0D);
        familyAi$lastKnownY = family.getDoubleOr(LAST_POS_Y_KEY, 0.0D);
        familyAi$lastKnownZ = family.getDoubleOr(LAST_POS_Z_KEY, 0.0D);
        familyAi$naturalSpawnProcessed = family.getBooleanOr(NATURAL_SPAWN_PROCESSED_KEY, false);
        familyAi$traitFear = clamp01(family.getDoubleOr(TRAIT_FEAR_KEY, 0.5D));
        familyAi$traitProtective = clamp01(family.getDoubleOr(TRAIT_PROTECTIVE_KEY, 0.5D));
        familyAi$traitHerdAffinity = clamp01(family.getDoubleOr(TRAIT_HERD_AFFINITY_KEY, 0.5D));

        for (String childUuid : family.listOrEmpty(CHILDREN_KEY, Codec.STRING)) {
            try {
                familyAi$childUuids.add(UUID.fromString(childUuid));
            } catch (IllegalArgumentException ignored) {
                // Ignore broken UUID values from older/corrupt saves.
            }
        }

        for (String siblingUuid : family.listOrEmpty(SIBLINGS_KEY, Codec.STRING)) {
            try {
                familyAi$siblingUuids.add(UUID.fromString(siblingUuid));
            } catch (IllegalArgumentException ignored) {
                // Ignore broken UUID values from older/corrupt saves.
            }
        }

        for (String reputationEntry : family.listOrEmpty(REPUTATION_KEY, Codec.STRING)) {
            String[] parts = reputationEntry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            try {
                UUID playerUuid = UUID.fromString(parts[0]);
                int reputation = Integer.parseInt(parts[1]);
                familyAi$reputation.put(playerUuid, FamilyAi.clampReputation(reputation));
            } catch (IllegalArgumentException ignored) {
                // Ignore broken UUID/int values from older/corrupt saves.
            }
        }

        familyAi$lastPlayTick = family.getLongOr(LAST_PLAY_TICK_KEY, 0L);
    }

    @Override
    public Optional<UUID> family$getMotherUuid() {
        return Optional.ofNullable(familyAi$motherUuid);
    }

    @Override
    public Optional<UUID> family$getFatherUuid() {
        return Optional.ofNullable(familyAi$fatherUuid);
    }

    @Override
    public Optional<UUID> family$getPartnerUuid() {
        return Optional.ofNullable(familyAi$partnerUuid);
    }

    @Override
    public Optional<UUID> family$getThreatUuid() {
        return Optional.ofNullable(familyAi$threatUuid);
    }

    @Override
    public Optional<UUID> family$getAlertChildUuid() {
        return Optional.ofNullable(familyAi$alertChildUuid);
    }

    @Override
    public Set<UUID> family$getChildUuids() {
        return Collections.unmodifiableSet(familyAi$childUuids);
    }

    @Override
    public Set<UUID> family$getSiblingUuids() {
        return Collections.unmodifiableSet(familyAi$siblingUuids);
    }

    @Override
    public Map<UUID, Integer> family$getReputationMap() {
        return Collections.unmodifiableMap(familyAi$reputation);
    }

    @Override
    public FamilySex family$getSex() {
        return familyAi$sex;
    }

    @Override
    public int family$getAlertTicks() {
        return familyAi$alertTicks;
    }

    @Override
    public int family$getAlertCooldownTicks() {
        return familyAi$alertCooldownTicks;
    }

    @Override
    public int family$getDataVersion() {
        return familyAi$dataVersion;
    }

    @Override
    public long family$getLastPlayTick() {
        return familyAi$lastPlayTick;
    }

    @Override
    public long family$getLastThreatTick() {
        return familyAi$lastThreatTick;
    }

    @Override
    public long family$getStateChangedTick() {
        return familyAi$stateChangedTick;
    }

    @Override
    public long family$getLastPathRecalcTick() {
        return familyAi$lastPathRecalcTick;
    }

    @Override
    public int family$getPanicCooldownTicks() {
        return familyAi$panicCooldownTicks;
    }

    @Override
    public int family$getStuckTicks() {
        return familyAi$stuckTicks;
    }

    @Override
    public int family$getPathFailCount() {
        return familyAi$pathFailCount;
    }

    @Override
    public double family$getLastKnownX() {
        return familyAi$lastKnownX;
    }

    @Override
    public double family$getLastKnownY() {
        return familyAi$lastKnownY;
    }

    @Override
    public double family$getLastKnownZ() {
        return familyAi$lastKnownZ;
    }

    @Override
    public double family$getTraitFear() {
        return familyAi$traitFear;
    }

    @Override
    public double family$getTraitProtective() {
        return familyAi$traitProtective;
    }

    @Override
    public double family$getTraitHerdAffinity() {
        return familyAi$traitHerdAffinity;
    }

    @Override
    public FamilyAiState family$getAiState() {
        return familyAi$state;
    }

    @Override
    public Optional<UUID> family$getLeaderUuid() {
        return Optional.ofNullable(familyAi$leaderUuid);
    }

    @Override
    public Optional<UUID> family$getTempGuardianUuid() {
        return Optional.ofNullable(familyAi$tempGuardianUuid);
    }

    @Override
    public boolean family$isNaturalSpawnProcessed() {
        return familyAi$naturalSpawnProcessed;
    }

    @Override
    public void family$setMotherUuid(UUID uuid) {
        familyAi$motherUuid = uuid;
    }

    @Override
    public void family$setFatherUuid(UUID uuid) {
        familyAi$fatherUuid = uuid;
    }

    @Override
    public void family$setPartnerUuid(UUID uuid) {
        familyAi$partnerUuid = uuid;
    }

    @Override
    public void family$setSex(FamilySex sex) {
        familyAi$sex = sex == null ? FamilySex.UNKNOWN : sex;
    }

    @Override
    public void family$addChildUuid(UUID uuid) {
        if (uuid != null) {
            familyAi$childUuids.add(uuid);
        }
    }

    @Override
    public void family$addSiblingUuid(UUID uuid) {
        if (uuid != null) {
            familyAi$siblingUuids.add(uuid);
        }
    }

    @Override
    public void family$setReputation(UUID playerUuid, int value) {
        if (playerUuid == null) {
            return;
        }
        int clamped = FamilyAi.clampReputation(value);
        if (clamped == 0) {
            familyAi$reputation.remove(playerUuid);
        } else {
            familyAi$reputation.put(playerUuid, clamped);
        }
    }

    @Override
    public void family$addReputation(UUID playerUuid, int delta) {
        if (playerUuid == null || delta == 0) {
            return;
        }
        int updated = FamilyAi.clampReputation(family$getReputation(playerUuid) + delta);
        family$setReputation(playerUuid, updated);
    }

    @Override
    public int family$getReputation(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        return familyAi$reputation.getOrDefault(playerUuid, 0);
    }

    @Override
    public void family$setLastPlayTick(long gameTick) {
        familyAi$lastPlayTick = Math.max(0L, gameTick);
    }

    @Override
    public void family$setLastThreatTick(long gameTick) {
        familyAi$lastThreatTick = Math.max(0L, gameTick);
    }

    @Override
    public void family$setStateChangedTick(long gameTick) {
        familyAi$stateChangedTick = Math.max(0L, gameTick);
    }

    @Override
    public void family$setLastPathRecalcTick(long gameTick) {
        familyAi$lastPathRecalcTick = Math.max(0L, gameTick);
    }

    @Override
    public void family$setPanicCooldownTicks(int ticks) {
        familyAi$panicCooldownTicks = Math.max(0, ticks);
    }

    @Override
    public void family$setStuckTicks(int ticks) {
        familyAi$stuckTicks = Math.max(0, ticks);
    }

    @Override
    public void family$setPathFailCount(int count) {
        familyAi$pathFailCount = Math.max(0, count);
    }

    @Override
    public void family$setLastKnownPos(double x, double y, double z) {
        familyAi$lastKnownX = x;
        familyAi$lastKnownY = y;
        familyAi$lastKnownZ = z;
    }

    @Override
    public void family$setTraits(double fear, double protective, double herdAffinity) {
        familyAi$traitFear = clamp01(fear);
        familyAi$traitProtective = clamp01(protective);
        familyAi$traitHerdAffinity = clamp01(herdAffinity);
    }

    @Override
    public void family$setAiState(FamilyAiState state) {
        familyAi$state = state == null ? FamilyAiState.IDLE : state;
    }

    @Override
    public void family$setLeaderUuid(UUID uuid) {
        familyAi$leaderUuid = uuid;
    }

    @Override
    public void family$setTempGuardianUuid(UUID uuid) {
        familyAi$tempGuardianUuid = uuid;
    }

    @Override
    public void family$setNaturalSpawnProcessed(boolean processed) {
        familyAi$naturalSpawnProcessed = processed;
    }

    @Override
    public void family$alert(UUID threatUuid, UUID childUuid, int ticks, int cooldownTicks) {
        familyAi$threatUuid = threatUuid;
        familyAi$alertChildUuid = childUuid;
        familyAi$alertTicks = Math.max(familyAi$alertTicks, ticks);
        familyAi$alertCooldownTicks = Math.max(familyAi$alertCooldownTicks, cooldownTicks);
    }

    @Override
    public void family$tickAlert() {
        if (familyAi$alertCooldownTicks > 0) {
            familyAi$alertCooldownTicks--;
        }
        if (familyAi$panicCooldownTicks > 0) {
            familyAi$panicCooldownTicks--;
        }
        if (familyAi$alertTicks > 0 && --familyAi$alertTicks == 0) {
            familyAi$threatUuid = null;
            familyAi$alertChildUuid = null;
        }
    }

    private void familyAi$ensureTraits(Animal animal) {
        if (familyAi$traitFear != 0.5D || familyAi$traitProtective != 0.5D || familyAi$traitHerdAffinity != 0.5D) {
            return;
        }

        family$setTraits(
                0.2D + animal.getRandom().nextDouble() * 0.6D,
                0.2D + animal.getRandom().nextDouble() * 0.6D,
                0.2D + animal.getRandom().nextDouble() * 0.6D
        );
    }

    private static FamilyAiState familyAi$readState(String value) {
        if (value == null || value.isBlank()) {
            return FamilyAiState.IDLE;
        }
        try {
            return FamilyAiState.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return FamilyAiState.IDLE;
        }
    }

    private static void putUuid(ValueOutput output, String key, UUID uuid) {
        if (uuid != null) {
            output.putString(key, uuid.toString());
        }
    }

    private static UUID getUuid(ValueInput input, String key) {
        return input.getString(key)
                .flatMap(value -> {
                    try {
                        return Optional.of(UUID.fromString(value));
                    } catch (IllegalArgumentException ignored) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

package com.example.familyai.mixin;

import com.example.familyai.FamilyAi;
import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAnimal;
import com.example.familyai.FamilySex;
import com.example.familyai.goal.ProtectChildGoal;
import com.example.familyai.goal.RunToParentGoal;
import com.example.familyai.goal.PlayWithSiblingGoal;
import com.example.familyai.goal.KeepDistanceFromPlayerGoal;
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
import java.util.HashSet;
import java.util.HashMap;
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
    private static final int CURRENT_DATA_VERSION = 2;

    private UUID familyAi$motherUuid;
    private UUID familyAi$fatherUuid;
    private UUID familyAi$partnerUuid;
    private UUID familyAi$threatUuid;
    private UUID familyAi$alertChildUuid;
    private final Set<UUID> familyAi$childUuids = new HashSet<>();
    private final Set<UUID> familyAi$siblingUuids = new HashSet<>();
    private final Map<UUID, Integer> familyAi$reputation = new HashMap<>();
    private FamilySex familyAi$sex = FamilySex.UNKNOWN;
    private int familyAi$alertTicks;
    private int familyAi$alertCooldownTicks;
    private long familyAi$lastPlayTick;
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
        family.putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
        family.putString(SEX_KEY, familyAi$sex.name());

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
        familyAi$childUuids.clear();
        familyAi$siblingUuids.clear();
        familyAi$reputation.clear();
        familyAi$sex = FamilySex.UNKNOWN;
        familyAi$lastPlayTick = 0L;
        familyAi$dataVersion = 0;

        ValueInput family = input.childOrEmpty(ROOT_KEY);
        familyAi$dataVersion = family.getIntOr(DATA_VERSION_KEY, 0);
        familyAi$motherUuid = getUuid(family, MOTHER_KEY);
        familyAi$fatherUuid = getUuid(family, FATHER_KEY);
        familyAi$partnerUuid = getUuid(family, PARTNER_KEY);
        familyAi$sex = FamilySex.fromName(family.getStringOr(SEX_KEY, FamilySex.UNKNOWN.name()));

        for (String childUuid : family.listOrEmpty(CHILDREN_KEY, Codec.STRING)) {
            try {
                familyAi$childUuids.add(UUID.fromString(childUuid));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted child entries instead of breaking entity loading.
            }
        }

        for (String siblingUuid : family.listOrEmpty(SIBLINGS_KEY, Codec.STRING)) {
            try {
                familyAi$siblingUuids.add(UUID.fromString(siblingUuid));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted sibling entries instead of breaking entity loading.
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
                // Ignore corrupted reputation entries instead of breaking entity loading.
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

        if (familyAi$alertTicks > 0 && --familyAi$alertTicks == 0) {
            familyAi$threatUuid = null;
            familyAi$alertChildUuid = null;
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
}

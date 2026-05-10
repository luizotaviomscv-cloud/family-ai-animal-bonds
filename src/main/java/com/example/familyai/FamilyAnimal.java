package com.example.familyai;

import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface FamilyAnimal {
    Optional<UUID> family$getMotherUuid();

    Optional<UUID> family$getFatherUuid();

    Optional<UUID> family$getPartnerUuid();

    Optional<UUID> family$getThreatUuid();

    Optional<UUID> family$getAlertChildUuid();

    Set<UUID> family$getChildUuids();

    Set<UUID> family$getSiblingUuids();

    Map<UUID, Integer> family$getReputationMap();

    FamilySex family$getSex();

    int family$getAlertTicks();

    int family$getAlertCooldownTicks();

    int family$getDataVersion();

    long family$getLastPlayTick();

    long family$getLastThreatTick();

    long family$getStateChangedTick();

    long family$getLastPathRecalcTick();

    int family$getPanicCooldownTicks();

    int family$getStuckTicks();

    int family$getPathFailCount();

    double family$getLastKnownX();

    double family$getLastKnownY();

    double family$getLastKnownZ();

    double family$getTraitFear();

    double family$getTraitProtective();

    double family$getTraitHerdAffinity();

    FamilyAiState family$getAiState();

    Optional<UUID> family$getLeaderUuid();

    Optional<UUID> family$getTempGuardianUuid();

    boolean family$isNaturalSpawnProcessed();

    void family$setMotherUuid(UUID uuid);

    void family$setFatherUuid(UUID uuid);

    void family$setPartnerUuid(UUID uuid);

    void family$setSex(FamilySex sex);

    void family$addChildUuid(UUID uuid);

    void family$addSiblingUuid(UUID uuid);

    void family$setReputation(UUID playerUuid, int value);

    void family$addReputation(UUID playerUuid, int delta);

    int family$getReputation(UUID playerUuid);

    void family$setLastPlayTick(long gameTick);

    void family$setLastThreatTick(long gameTick);

    void family$setStateChangedTick(long gameTick);

    void family$setLastPathRecalcTick(long gameTick);

    void family$setPanicCooldownTicks(int ticks);

    void family$setStuckTicks(int ticks);

    void family$setPathFailCount(int count);

    void family$setLastKnownPos(double x, double y, double z);

    void family$setTraits(double fear, double protective, double herdAffinity);

    void family$setAiState(FamilyAiState state);

    void family$setLeaderUuid(UUID uuid);

    void family$setTempGuardianUuid(UUID uuid);

    void family$setNaturalSpawnProcessed(boolean processed);

    void family$alert(UUID threatUuid, UUID childUuid, int ticks, int cooldownTicks);

    void family$tickAlert();

    default boolean family$isAlert() {
        return family$getAlertTicks() > 0;
    }
}

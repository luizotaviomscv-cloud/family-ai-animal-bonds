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

    void family$alert(UUID threatUuid, UUID childUuid, int ticks, int cooldownTicks);

    void family$tickAlert();

    default boolean family$isAlert() {
        return family$getAlertTicks() > 0;
    }
}

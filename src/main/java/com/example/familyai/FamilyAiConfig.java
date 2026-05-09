package com.example.familyai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FamilyAiConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("family-ai.json");
    private static FamilyAiConfig instance = new FamilyAiConfig();

    public double childFollowSpeed = 1.35D;
    public double childStopDistance = 2.0D;
    public double childTooFarDistance = 3.0D;
    public double parentProtectSpeed = 1.15D;
    public double mateCohesionSpeed = 0.9D;
    public double mateCohesionRadius = 8.0D;
    public double hostileScanRange = 9.0D;
    public double parentFallbackRange = 24.0D;
    public int alertTicks = 20 * 12;
    public int alertCooldownTicks = 20 * 3;
    public int warningSoundCooldownTicks = 40;

    public int reputationMin = -100;
    public int reputationMax = 100;
    public int reputationTrustedThreshold = 40;
    public int reputationNeutralMin = -19;
    public int reputationWaryMin = -20;
    public int reputationHostileMax = -60;
    public int reputationGainFeed = 4;
    public int reputationGainBreedingAssist = 8;
    public int reputationGainDefendHerd = 10;
    public int reputationLossAdultHit = 15;
    public int reputationLossBabyHit = 40;
    public int reputationLossKill = 80;
    public int reputationLossWeaponSprintNearBaby = 5;
    public int feedReputationCooldownTicks = 20 * 8;
    public int weaponSprintPenaltyCooldownTicks = 20 * 5;
    public int reputationDecayIntervalTicks = 20 * 60;
    public int reputationDecayStep = 1;
    public double herdGossipFactor = 0.5D;
    public double herdGossipRange = 16.0D;
    public double defendRewardRange = 14.0D;

    public boolean enableNaturalFamilySpawns = true;
    public double naturalFamilySpawnChance = 0.22D;
    public double naturalFamilySpawnRadius = 9.0D;
    public int naturalFamilyLocalCap = 20;
    public int naturalFamilyChunkCooldownTicks = 20 * 70;
    public int herdAdultsMin = 3;
    public int herdAdultsMax = 6;
    public int herdBabiesMin = 0;
    public int herdBabiesMax = 2;
    public int chickenAdultsMin = 4;
    public int chickenAdultsMax = 8;
    public int chickenBabiesMin = 1;
    public int chickenBabiesMax = 3;

    public boolean enableSiblingPlay = true;
    public double siblingPlaySpeed = 1.08D;
    public double siblingPlayRange = 14.0D;
    public int siblingPlayDurationTicks = 20 * 6;
    public int siblingPlayCooldownTicks = 20 * 18;

    public static FamilyAiConfig get() {
        return instance;
    }

    public static void resetDefaults() {
        instance = new FamilyAiConfig();
        save();
    }

    public static void load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                FamilyAiConfig loaded = GSON.fromJson(reader, FamilyAiConfig.class);
                instance = loaded == null ? new FamilyAiConfig() : loaded.sanitized();
            } catch (IOException ignored) {
                instance = new FamilyAiConfig();
            }
        }

        save();
    }

    public static void save() {
        instance.sanitized();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException ignored) {
            // Keep defaults in memory if the config file cannot be written.
        }
    }

    private FamilyAiConfig sanitized() {
        childFollowSpeed = clamp(childFollowSpeed, 0.05D, 3.0D);
        childStopDistance = clamp(childStopDistance, 0.5D, 8.0D);
        childTooFarDistance = clamp(childTooFarDistance, childStopDistance, 16.0D);
        parentProtectSpeed = clamp(parentProtectSpeed, 0.05D, 3.0D);
        mateCohesionSpeed = clamp(mateCohesionSpeed, 0.05D, 3.0D);
        mateCohesionRadius = clamp(mateCohesionRadius, 2.0D, 32.0D);
        hostileScanRange = clamp(hostileScanRange, 1.0D, 32.0D);
        parentFallbackRange = clamp(parentFallbackRange, 4.0D, 64.0D);
        alertTicks = clamp(alertTicks, 20, 20 * 60);
        alertCooldownTicks = clamp(alertCooldownTicks, 0, 20 * 30);
        warningSoundCooldownTicks = clamp(warningSoundCooldownTicks, 5, 20 * 10);

        reputationMin = clamp(reputationMin, -500, 0);
        reputationMax = clamp(reputationMax, 0, 500);
        if (reputationMin >= reputationMax) {
            reputationMin = -100;
            reputationMax = 100;
        }

        reputationTrustedThreshold = clamp(reputationTrustedThreshold, reputationMin, reputationMax);
        reputationHostileMax = clamp(reputationHostileMax, reputationMin, reputationTrustedThreshold - 2);
        reputationWaryMin = clamp(reputationWaryMin, reputationHostileMax + 1, reputationTrustedThreshold - 1);
        reputationNeutralMin = clamp(reputationNeutralMin, reputationWaryMin + 1, reputationTrustedThreshold);

        reputationGainFeed = clamp(reputationGainFeed, 0, 100);
        reputationGainBreedingAssist = clamp(reputationGainBreedingAssist, 0, 100);
        reputationGainDefendHerd = clamp(reputationGainDefendHerd, 0, 100);
        reputationLossAdultHit = clamp(reputationLossAdultHit, 0, 200);
        reputationLossBabyHit = clamp(reputationLossBabyHit, 0, 200);
        reputationLossKill = clamp(reputationLossKill, 0, 300);
        reputationLossWeaponSprintNearBaby = clamp(reputationLossWeaponSprintNearBaby, 0, 100);
        feedReputationCooldownTicks = clamp(feedReputationCooldownTicks, 20, 20 * 60);
        weaponSprintPenaltyCooldownTicks = clamp(weaponSprintPenaltyCooldownTicks, 20, 20 * 30);
        reputationDecayIntervalTicks = clamp(reputationDecayIntervalTicks, 20 * 10, 20 * 60 * 30);
        reputationDecayStep = clamp(reputationDecayStep, 1, 25);
        herdGossipFactor = clamp(herdGossipFactor, 0.0D, 1.0D);
        herdGossipRange = clamp(herdGossipRange, 12.0D, 20.0D);
        defendRewardRange = clamp(defendRewardRange, 6.0D, 24.0D);

        naturalFamilySpawnChance = clamp(naturalFamilySpawnChance, 0.0D, 1.0D);
        naturalFamilySpawnRadius = clamp(naturalFamilySpawnRadius, 3.0D, 20.0D);
        naturalFamilyLocalCap = clamp(naturalFamilyLocalCap, 4, 80);
        naturalFamilyChunkCooldownTicks = clamp(naturalFamilyChunkCooldownTicks, 20, 20 * 60 * 20);
        herdAdultsMin = clamp(herdAdultsMin, 1, 32);
        herdAdultsMax = clamp(herdAdultsMax, herdAdultsMin, 32);
        herdBabiesMin = clamp(herdBabiesMin, 0, 16);
        herdBabiesMax = clamp(herdBabiesMax, herdBabiesMin, 16);
        chickenAdultsMin = clamp(chickenAdultsMin, 1, 32);
        chickenAdultsMax = clamp(chickenAdultsMax, chickenAdultsMin, 32);
        chickenBabiesMin = clamp(chickenBabiesMin, 0, 16);
        chickenBabiesMax = clamp(chickenBabiesMax, chickenBabiesMin, 16);

        siblingPlaySpeed = clamp(siblingPlaySpeed, 0.05D, 3.0D);
        siblingPlayRange = clamp(siblingPlayRange, 2.0D, 32.0D);
        siblingPlayDurationTicks = clamp(siblingPlayDurationTicks, 20, 20 * 60);
        siblingPlayCooldownTicks = clamp(siblingPlayCooldownTicks, 20, 20 * 180);
        return this;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

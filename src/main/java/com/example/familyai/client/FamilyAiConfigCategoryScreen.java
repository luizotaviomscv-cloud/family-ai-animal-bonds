package com.example.familyai.client;

import com.example.familyai.FamilyAiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class FamilyAiConfigCategoryScreen extends OptionsSubScreen {
    private final FamilyAiConfigCategory category;

    public FamilyAiConfigCategoryScreen(Screen parent, FamilyAiConfigCategory category) {
        super(parent, Minecraft.getInstance().options, Component.translatable(category.titleKey()));
        this.category = category;
    }

    @Override
    protected void addOptions() {
        FamilyAiConfig config = FamilyAiConfig.get();
        switch (category) {
            case FAMILY_CORE -> addFamilyCoreOptions(config);
            case PROTECTION -> addProtectionOptions(config);
            case REPUTATION_ACTIONS -> addReputationActionOptions(config);
            case REPUTATION_LIMITS -> addReputationLimitOptions(config);
            case NATURAL_SPAWNS -> addNaturalSpawnOptions(config);
            case SIBLING_PLAY -> addSiblingPlayOptions(config);
        }
    }

    @Override
    public void removed() {
        FamilyAiConfig.save();
        super.removed();
    }

    private void addFamilyCoreOptions(FamilyAiConfig config) {
        this.list.addSmall(
                doubleOption("family_ai.config.child_follow_speed", config.childFollowSpeed, 0.25D, 3.0D, value -> config.childFollowSpeed = value),
                doubleOption("family_ai.config.child_stop_distance", config.childStopDistance, 0.5D, 8.0D, value -> config.childStopDistance = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.child_too_far_distance", config.childTooFarDistance, 1.0D, 16.0D, value -> config.childTooFarDistance = value),
                doubleOption("family_ai.config.parent_fallback_range", config.parentFallbackRange, 4.0D, 64.0D, value -> config.parentFallbackRange = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.mate_cohesion_speed", config.mateCohesionSpeed, 0.25D, 3.0D, value -> config.mateCohesionSpeed = value),
                doubleOption("family_ai.config.mate_cohesion_radius", config.mateCohesionRadius, 2.0D, 32.0D, value -> config.mateCohesionRadius = value)
        );
    }

    private void addProtectionOptions(FamilyAiConfig config) {
        this.list.addSmall(
                doubleOption("family_ai.config.parent_protect_speed", config.parentProtectSpeed, 0.25D, 3.0D, value -> config.parentProtectSpeed = value),
                doubleOption("family_ai.config.hostile_scan_range", config.hostileScanRange, 1.0D, 32.0D, value -> config.hostileScanRange = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.alert_ticks", () -> config.alertTicks, 20, 20 * 60, value -> config.alertTicks = value),
                intOption("family_ai.config.alert_cooldown_ticks", () -> config.alertCooldownTicks, 0, 20 * 30, value -> config.alertCooldownTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.warning_sound_cooldown_ticks", () -> config.warningSoundCooldownTicks, 5, 20 * 10, value -> config.warningSoundCooldownTicks = value)
        );
    }

    private void addReputationActionOptions(FamilyAiConfig config) {
        this.list.addSmall(
                intOption("family_ai.config.reputation_gain_feed", () -> config.reputationGainFeed, 0, 100, value -> config.reputationGainFeed = value),
                intOption("family_ai.config.reputation_gain_breeding_assist", () -> config.reputationGainBreedingAssist, 0, 100, value -> config.reputationGainBreedingAssist = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_gain_defend_herd", () -> config.reputationGainDefendHerd, 0, 100, value -> config.reputationGainDefendHerd = value),
                intOption("family_ai.config.reputation_loss_adult_hit", () -> config.reputationLossAdultHit, 0, 200, value -> config.reputationLossAdultHit = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_loss_baby_hit", () -> config.reputationLossBabyHit, 0, 200, value -> config.reputationLossBabyHit = value),
                intOption("family_ai.config.reputation_loss_kill", () -> config.reputationLossKill, 0, 300, value -> config.reputationLossKill = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_loss_weapon_sprint", () -> config.reputationLossWeaponSprintNearBaby, 0, 100, value -> config.reputationLossWeaponSprintNearBaby = value),
                intOption("family_ai.config.feed_reputation_cooldown_ticks", () -> config.feedReputationCooldownTicks, 20, 20 * 60, value -> config.feedReputationCooldownTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.weapon_sprint_penalty_cooldown_ticks", () -> config.weaponSprintPenaltyCooldownTicks, 20, 20 * 30, value -> config.weaponSprintPenaltyCooldownTicks = value),
                doubleOption("family_ai.config.defend_reward_range", config.defendRewardRange, 6.0D, 24.0D, value -> config.defendRewardRange = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.herd_gossip_factor", config.herdGossipFactor, 0.0D, 1.0D, value -> config.herdGossipFactor = value),
                doubleOption("family_ai.config.herd_gossip_range", config.herdGossipRange, 12.0D, 20.0D, value -> config.herdGossipRange = value)
        );
    }

    private void addReputationLimitOptions(FamilyAiConfig config) {
        this.list.addSmall(
                intOption("family_ai.config.reputation_decay_interval_ticks", () -> config.reputationDecayIntervalTicks, 20 * 10, 20 * 60 * 30, value -> config.reputationDecayIntervalTicks = value),
                intOption("family_ai.config.reputation_decay_step", () -> config.reputationDecayStep, 1, 25, value -> config.reputationDecayStep = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_min", () -> config.reputationMin, -500, 0, value -> config.reputationMin = value),
                intOption("family_ai.config.reputation_max", () -> config.reputationMax, 0, 500, value -> config.reputationMax = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_trusted_threshold", () -> config.reputationTrustedThreshold, -100, 100, value -> config.reputationTrustedThreshold = value),
                intOption("family_ai.config.reputation_wary_min", () -> config.reputationWaryMin, -100, 100, value -> config.reputationWaryMin = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_hostile_max", () -> config.reputationHostileMax, -100, 100, value -> config.reputationHostileMax = value)
        );
    }

    private void addNaturalSpawnOptions(FamilyAiConfig config) {
        this.list.addSmall(
                doubleOption("family_ai.config.natural_family_spawn_chance", config.naturalFamilySpawnChance, 0.0D, 1.0D, value -> config.naturalFamilySpawnChance = value),
                doubleOption("family_ai.config.natural_family_spawn_radius", config.naturalFamilySpawnRadius, 3.0D, 20.0D, value -> config.naturalFamilySpawnRadius = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.natural_family_local_cap", () -> config.naturalFamilyLocalCap, 4, 80, value -> config.naturalFamilyLocalCap = value),
                intOption("family_ai.config.natural_family_chunk_cooldown_ticks", () -> config.naturalFamilyChunkCooldownTicks, 20, 20 * 60 * 20, value -> config.naturalFamilyChunkCooldownTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.herd_adults_min", () -> config.herdAdultsMin, 1, 32, value -> config.herdAdultsMin = value),
                intOption("family_ai.config.herd_adults_max", () -> config.herdAdultsMax, 1, 32, value -> config.herdAdultsMax = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.herd_babies_min", () -> config.herdBabiesMin, 0, 16, value -> config.herdBabiesMin = value),
                intOption("family_ai.config.herd_babies_max", () -> config.herdBabiesMax, 0, 16, value -> config.herdBabiesMax = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.chicken_adults_min", () -> config.chickenAdultsMin, 1, 32, value -> config.chickenAdultsMin = value),
                intOption("family_ai.config.chicken_adults_max", () -> config.chickenAdultsMax, 1, 32, value -> config.chickenAdultsMax = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.chicken_babies_min", () -> config.chickenBabiesMin, 0, 16, value -> config.chickenBabiesMin = value),
                intOption("family_ai.config.chicken_babies_max", () -> config.chickenBabiesMax, 0, 16, value -> config.chickenBabiesMax = value)
        );
    }

    private void addSiblingPlayOptions(FamilyAiConfig config) {
        this.list.addSmall(
                doubleOption("family_ai.config.sibling_play_speed", config.siblingPlaySpeed, 0.25D, 3.0D, value -> config.siblingPlaySpeed = value),
                doubleOption("family_ai.config.sibling_play_range", config.siblingPlayRange, 2.0D, 32.0D, value -> config.siblingPlayRange = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.sibling_play_duration_ticks", () -> config.siblingPlayDurationTicks, 20, 20 * 60, value -> config.siblingPlayDurationTicks = value),
                intOption("family_ai.config.sibling_play_cooldown_ticks", () -> config.siblingPlayCooldownTicks, 20, 20 * 180, value -> config.siblingPlayCooldownTicks = value)
        );
    }

    private static OptionInstance<Double> doubleOption(String key, double currentValue, double min, double max, DoubleConsumer setter) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.literal(caption.getString() + ": " + String.format(Locale.ROOT, "%.2f", value)),
                OptionInstance.UnitDouble.INSTANCE.xmap(
                        unit -> round(min + clamp01(unit) * (max - min)),
                        value -> clamp01((value - min) / (max - min))
                ),
                round(currentValue),
                value -> {
                    setter.accept(value);
                    FamilyAiConfig.save();
                }
        );
    }

    private static OptionInstance<Integer> intOption(String key, Supplier<Integer> currentValue, int min, int max, IntConsumer setter) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.literal(caption.getString() + ": " + value),
                new OptionInstance.IntRange(min, max),
                currentValue.get(),
                value -> {
                    setter.accept(value);
                    FamilyAiConfig.save();
                }
        );
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

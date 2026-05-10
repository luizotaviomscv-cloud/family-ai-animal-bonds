package com.example.familyai.client;

import com.example.familyai.FamilyAiConfig;
import com.example.familyai.FamilyAiHudMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
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
            case ANIMAL_AI -> addAnimalAiOptions(config);
            case FAMILY_SYSTEM -> addFamilyOptions(config);
            case HERD_SYSTEM -> addHerdOptions(config);
            case DANGER -> addDangerOptions(config);
            case PERSONALITY_MEMORY -> addPersonalityOptions(config);
            case PATHFINDING -> addPathOptions(config);
            case HUD_ALERTS -> addHudOptions(config);
            case DEBUG_LOGS -> addDebugOptions(config);
            case PERFORMANCE -> addPerformanceOptions(config);
        }
    }

    @Override
    public void removed() {
        FamilyAiConfig.save();
        super.removed();
    }

    private void addAnimalAiOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_advanced_ai", () -> config.enableAdvancedAi, value -> config.enableAdvancedAi = value),
                intOption("family_ai.config.ai_update_interval_ticks", () -> config.aiUpdateIntervalTicks, 2, 80, value -> config.aiUpdateIntervalTicks = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.child_follow_speed", config.childFollowSpeed, 0.25D, 3.0D, value -> config.childFollowSpeed = value),
                doubleOption("family_ai.config.child_run_speed", config.childRunSpeed, 0.25D, 3.0D, value -> config.childRunSpeed = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.child_stop_distance", config.childStopDistance, 0.5D, 8.0D, value -> config.childStopDistance = value),
                doubleOption("family_ai.config.child_too_far_distance", config.childTooFarDistance, 1.0D, 24.0D, value -> config.childTooFarDistance = value)
        );
    }

    private void addFamilyOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_family_system", () -> config.enableFamilySystem, value -> config.enableFamilySystem = value),
                boolOption("family_ai.config.enable_child_protection", () -> config.enableChildProtection, value -> config.enableChildProtection = value)
        );
        this.list.addSmall(
                boolOption("family_ai.config.enable_sibling_play", () -> config.enableSiblingPlay, value -> config.enableSiblingPlay = value),
                doubleOption("family_ai.config.parent_fallback_range", config.parentFallbackRange, 4.0D, 64.0D, value -> config.parentFallbackRange = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.parent_protect_speed", config.parentProtectSpeed, 0.25D, 3.0D, value -> config.parentProtectSpeed = value),
                doubleOption("family_ai.config.mate_cohesion_radius", config.mateCohesionRadius, 2.0D, 32.0D, value -> config.mateCohesionRadius = value)
        );
    }

    private void addHerdOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_herd_system", () -> config.enableHerdSystem, value -> config.enableHerdSystem = value),
                boolOption("family_ai.config.enable_herd_leader", () -> config.enableHerdLeader, value -> config.enableHerdLeader = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.herd_max_distance", config.herdMaxDistance, 4.0D, 48.0D, value -> config.herdMaxDistance = value),
                doubleOption("family_ai.config.herd_separation_distance", config.herdSeparationDistance, 0.8D, 5.0D, value -> config.herdSeparationDistance = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.herd_alignment_factor", config.herdAlignmentFactor, 0.0D, 2.0D, value -> config.herdAlignmentFactor = value),
                doubleOption("family_ai.config.herd_cohesion_factor", config.herdCohesionFactor, 0.0D, 2.0D, value -> config.herdCohesionFactor = value)
        );
        this.list.addSmall(
                boolOption("family_ai.config.enable_natural_family_spawns", () -> config.enableNaturalFamilySpawns, value -> config.enableNaturalFamilySpawns = value),
                doubleOption("family_ai.config.natural_family_spawn_chance", config.naturalFamilySpawnChance, 0.0D, 1.0D, value -> config.naturalFamilySpawnChance = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.natural_family_local_cap", () -> config.naturalFamilyLocalCap, 2, 80, value -> config.naturalFamilyLocalCap = value),
                intOption("family_ai.config.natural_family_chunk_cooldown_ticks", () -> config.naturalFamilyChunkCooldownTicks, 20, 20 * 60 * 20, value -> config.naturalFamilyChunkCooldownTicks = value)
        );
    }

    private void addDangerOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_advanced_danger_reaction", () -> config.enableAdvancedDangerReaction, value -> config.enableAdvancedDangerReaction = value),
                doubleOption("family_ai.config.danger_detection_radius", config.dangerDetectionRadius, 2.0D, 40.0D, value -> config.dangerDetectionRadius = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.alert_ticks", () -> config.alertTicks, 20, 20 * 60, value -> config.alertTicks = value),
                intOption("family_ai.config.alert_cooldown_ticks", () -> config.alertCooldownTicks, 0, 20 * 30, value -> config.alertCooldownTicks = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.panic_intensity", config.panicIntensity, 0.2D, 2.5D, value -> config.panicIntensity = value),
                intOption("family_ai.config.panic_cooldown_ticks", () -> config.panicCooldownTicks, 20, 20 * 120, value -> config.panicCooldownTicks = value)
        );
    }

    private void addPersonalityOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_personality", () -> config.enablePersonality, value -> config.enablePersonality = value),
                boolOption("family_ai.config.enable_memory", () -> config.enableMemory, value -> config.enableMemory = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.threat_memory_ticks", () -> config.threatMemoryTicks, 20, 20 * 60 * 10, value -> config.threatMemoryTicks = value),
                doubleOption("family_ai.config.temp_guardian_follow_distance", config.tempGuardianFollowDistance, 1.0D, 12.0D, value -> config.tempGuardianFollowDistance = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.reputation_decay_interval_ticks", () -> config.reputationDecayIntervalTicks, 20 * 10, 20 * 60 * 30, value -> config.reputationDecayIntervalTicks = value),
                intOption("family_ai.config.reputation_decay_step", () -> config.reputationDecayStep, 1, 25, value -> config.reputationDecayStep = value)
        );
    }

    private void addPathOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_unstuck_system", () -> config.enableUnstuckSystem, value -> config.enableUnstuckSystem = value),
                intOption("family_ai.config.stuck_check_interval_ticks", () -> config.stuckCheckIntervalTicks, 5, 80, value -> config.stuckCheckIntervalTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.stuck_recovery_ticks", () -> config.stuckRecoveryTicks, 20, 20 * 60, value -> config.stuckRecoveryTicks = value),
                intOption("family_ai.config.path_recalc_cooldown_ticks", () -> config.pathRecalcCooldownTicks, 2, 80, value -> config.pathRecalcCooldownTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.max_path_fail_count", () -> config.maxPathFailCount, 1, 20, value -> config.maxPathFailCount = value),
                doubleOption("family_ai.config.regroup_distance", config.regroupDistance, config.herdMaxDistance, 64.0D, value -> config.regroupDistance = value)
        );
    }

    private void addHudOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_hud", () -> config.enableHud, value -> config.enableHud = value),
                hudModeOption(config)
        );
        this.list.addSmall(
                boolOption("family_ai.config.enable_chat_messages", () -> config.enableChatMessages, value -> config.enableChatMessages = value),
                intOption("family_ai.config.chat_message_cooldown_ticks", () -> config.chatMessageCooldownTicks, 10, 20 * 60, value -> config.chatMessageCooldownTicks = value)
        );
        this.list.addSmall(
                boolOption("family_ai.config.enable_welcome_message", () -> config.enableWelcomeMessage, value -> config.enableWelcomeMessage = value),
                boolOption("family_ai.config.enable_guide_book", () -> config.enableGuideBook, value -> config.enableGuideBook = value)
        );
    }

    private void addDebugOptions(FamilyAiConfig config) {
        this.list.addSmall(
                boolOption("family_ai.config.enable_debug", () -> config.enableDebug, value -> config.enableDebug = value),
                intOption("family_ai.config.debug_detail_level", () -> config.debugDetailLevel, 0, 3, value -> config.debugDetailLevel = value)
        );
        this.list.addSmall(
                boolOption("family_ai.config.allow_guide_command", () -> config.allowGuideCommand, value -> config.allowGuideCommand = value),
                intOption("family_ai.config.warning_sound_cooldown_ticks", () -> config.warningSoundCooldownTicks, 5, 20 * 10, value -> config.warningSoundCooldownTicks = value)
        );
    }

    private void addPerformanceOptions(FamilyAiConfig config) {
        this.list.addSmall(
                intOption("family_ai.config.ai_update_interval_ticks", () -> config.aiUpdateIntervalTicks, 2, 80, value -> config.aiUpdateIntervalTicks = value),
                intOption("family_ai.config.herd_leader_swap_cooldown_ticks", () -> config.herdLeaderSwapCooldownTicks, 20, 20 * 120, value -> config.herdLeaderSwapCooldownTicks = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.hostile_scan_range", config.hostileScanRange, 1.0D, 32.0D, value -> config.hostileScanRange = value),
                doubleOption("family_ai.config.herd_gossip_range", config.herdGossipRange, 8.0D, 24.0D, value -> config.herdGossipRange = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.natural_family_chunk_cooldown_ticks", () -> config.naturalFamilyChunkCooldownTicks, 20, 20 * 60 * 20, value -> config.naturalFamilyChunkCooldownTicks = value),
                intOption("family_ai.config.natural_family_local_cap", () -> config.naturalFamilyLocalCap, 2, 80, value -> config.naturalFamilyLocalCap = value)
        );
    }

    private static OptionInstance<Boolean> boolOption(String key, Supplier<Boolean> currentValue, Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(
                key,
                currentValue.get(),
                value -> {
                    setter.accept(value);
                    FamilyAiConfig.save();
                }
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

    private static OptionInstance<Integer> hudModeOption(FamilyAiConfig config) {
        return new OptionInstance<>(
                "family_ai.config.hud_mode",
                OptionInstance.noTooltip(),
                (caption, value) -> Component.literal(caption.getString() + ": " + modeLabel(value)),
                new OptionInstance.IntRange(0, 3),
                modeToInt(config.resolvedHudMode()),
                value -> {
                    config.setHudMode(intToMode(value));
                    FamilyAiConfig.save();
                }
        );
    }

    private static String modeLabel(int value) {
        return switch (value) {
            case 1 -> "SIMPLE";
            case 2 -> "DETAILED";
            case 3 -> "DEBUG";
            default -> "OFF";
        };
    }

    private static int modeToInt(FamilyAiHudMode mode) {
        return switch (mode) {
            case SIMPLE -> 1;
            case DETAILED -> 2;
            case DEBUG -> 3;
            default -> 0;
        };
    }

    private static FamilyAiHudMode intToMode(int value) {
        return switch (value) {
            case 1 -> FamilyAiHudMode.SIMPLE;
            case 2 -> FamilyAiHudMode.DETAILED;
            case 3 -> FamilyAiHudMode.DEBUG;
            default -> FamilyAiHudMode.OFF;
        };
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}

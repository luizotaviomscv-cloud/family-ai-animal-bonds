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

public final class FamilyAiConfigScreen extends OptionsSubScreen {
    public FamilyAiConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("family_ai.config.title"));
    }

    @Override
    protected void addOptions() {
        FamilyAiConfig config = FamilyAiConfig.get();

        this.list.addSmall(
                doubleOption("family_ai.config.child_follow_speed", config.childFollowSpeed, 0.25D, 3.0D, value -> config.childFollowSpeed = value),
                doubleOption("family_ai.config.child_stop_distance", config.childStopDistance, 0.5D, 8.0D, value -> config.childStopDistance = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.child_too_far_distance", config.childTooFarDistance, 1.0D, 16.0D, value -> config.childTooFarDistance = value),
                doubleOption("family_ai.config.parent_protect_speed", config.parentProtectSpeed, 0.25D, 3.0D, value -> config.parentProtectSpeed = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.mate_cohesion_speed", config.mateCohesionSpeed, 0.25D, 3.0D, value -> config.mateCohesionSpeed = value),
                doubleOption("family_ai.config.mate_cohesion_radius", config.mateCohesionRadius, 2.0D, 32.0D, value -> config.mateCohesionRadius = value)
        );
        this.list.addSmall(
                doubleOption("family_ai.config.hostile_scan_range", config.hostileScanRange, 1.0D, 32.0D, value -> config.hostileScanRange = value),
                doubleOption("family_ai.config.parent_fallback_range", config.parentFallbackRange, 4.0D, 64.0D, value -> config.parentFallbackRange = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.alert_ticks", () -> config.alertTicks, 20, 20 * 60, value -> config.alertTicks = value),
                intOption("family_ai.config.alert_cooldown_ticks", () -> config.alertCooldownTicks, 0, 20 * 30, value -> config.alertCooldownTicks = value)
        );
        this.list.addSmall(
                intOption("family_ai.config.warning_sound_cooldown_ticks", () -> config.warningSoundCooldownTicks, 5, 20 * 10, value -> config.warningSoundCooldownTicks = value)
        );
    }

    @Override
    public void removed() {
        FamilyAiConfig.save();
        super.removed();
    }

    private static OptionInstance<Double> doubleOption(String key, double currentValue, double min, double max, DoubleConsumer setter) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, value) -> Component.literal(caption.getString() + ": " + String.format(Locale.ROOT, "%.2f", value)),
                OptionInstance.UnitDouble.INSTANCE.xmap(
                        unit -> round(min + unit * (max - min)),
                        value -> (value - min) / (max - min)
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
}

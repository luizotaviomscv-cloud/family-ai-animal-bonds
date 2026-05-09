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
        return this;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

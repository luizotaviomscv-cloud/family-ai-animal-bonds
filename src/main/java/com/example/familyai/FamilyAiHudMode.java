package com.example.familyai;

public enum FamilyAiHudMode {
    OFF,
    SIMPLE,
    DETAILED,
    DEBUG;

    public static FamilyAiHudMode fromName(String value) {
        if (value == null || value.isBlank()) {
            return OFF;
        }

        try {
            return FamilyAiHudMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return OFF;
        }
    }
}

package com.example.familyai.client;

public enum FamilyAiConfigCategory {
    ANIMAL_AI("family_ai.config.category.animal_ai"),
    FAMILY_SYSTEM("family_ai.config.category.family_system"),
    HERD_SYSTEM("family_ai.config.category.herd_system"),
    DANGER("family_ai.config.category.danger"),
    PERSONALITY_MEMORY("family_ai.config.category.personality_memory"),
    PATHFINDING("family_ai.config.category.pathfinding"),
    HUD_ALERTS("family_ai.config.category.hud_alerts"),
    DEBUG_LOGS("family_ai.config.category.debug_logs"),
    PERFORMANCE("family_ai.config.category.performance");

    private final String translationKey;

    FamilyAiConfigCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String labelKey() {
        return translationKey;
    }

    public String titleKey() {
        return translationKey + ".title";
    }
}

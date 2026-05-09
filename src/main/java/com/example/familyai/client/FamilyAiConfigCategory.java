package com.example.familyai.client;

public enum FamilyAiConfigCategory {
    FAMILY_CORE("family_ai.config.category.family_core"),
    PROTECTION("family_ai.config.category.protection"),
    REPUTATION_ACTIONS("family_ai.config.category.reputation_actions"),
    REPUTATION_LIMITS("family_ai.config.category.reputation_limits"),
    NATURAL_SPAWNS("family_ai.config.category.natural_spawns"),
    SIBLING_PLAY("family_ai.config.category.sibling_play");

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

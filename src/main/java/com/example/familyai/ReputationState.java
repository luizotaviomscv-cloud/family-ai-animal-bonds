package com.example.familyai;

public enum ReputationState {
    TRUSTED,
    NEUTRAL,
    WARY,
    HOSTILE;

    public static ReputationState fromScore(int score) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (score >= config.reputationTrustedThreshold) {
            return TRUSTED;
        }
        if (score <= config.reputationHostileMax) {
            return HOSTILE;
        }
        if (score <= config.reputationWaryMin) {
            return WARY;
        }
        return NEUTRAL;
    }
}

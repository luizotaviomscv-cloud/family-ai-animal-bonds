package com.example.familyai;

import net.minecraft.util.RandomSource;

public enum FamilySex {
    UNKNOWN,
    FEMALE,
    MALE;

    public static FamilySex random(RandomSource random) {
        return random.nextBoolean() ? FEMALE : MALE;
    }

    public static FamilySex fromName(String name) {
        try {
            return FamilySex.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}

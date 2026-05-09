package com.example.familyai;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;

public final class FamilyAiTags {
    public static final TagKey<EntityType<?>> FAMILY_ANIMALS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(FamilyAiMod.MOD_ID, "family_animals")
    );

    private FamilyAiTags() {
    }

    public static boolean isFamilyAnimal(Animal animal) {
        return animal.getType().is(FAMILY_ANIMALS);
    }
}

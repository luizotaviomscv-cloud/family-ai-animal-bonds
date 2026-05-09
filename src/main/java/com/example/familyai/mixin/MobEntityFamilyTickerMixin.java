package com.example.familyai.mixin;

import com.example.familyai.FamilyAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobEntityFamilyTickerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void familyAi$tickAlertState(CallbackInfo ci) {
        if ((Object) this instanceof Animal animal && animal instanceof FamilyAnimal familyAnimal) {
            familyAnimal.family$tickAlert();
        }
    }
}

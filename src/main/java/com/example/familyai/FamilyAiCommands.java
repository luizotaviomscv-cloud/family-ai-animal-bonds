package com.example.familyai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class FamilyAiCommands {
    private static final double INSPECT_RANGE = 16.0D;
    private static final double MAX_AIM_OFFSET_SQUARED = 2.25D;

    private FamilyAiCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("familyai")
                .then(Commands.literal("inspect")
                        .executes(context -> inspect(context.getSource()))));
    }

    private static int inspect(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Animal> target = findLookedAtAnimal(player);

        if (target.isEmpty()) {
            source.sendFailure(Component.literal("Nenhum animal do Family AI na sua mira."));
            return 0;
        }

        Animal animal = target.get();
        FamilyAnimal data = (FamilyAnimal) animal;

        send(source, "Family AI: " + animal.getType().toShortString() + " " + shortUuid(animal.getUUID()));
        send(source, "Sexo: " + data.family$getSex());
        send(source, "Mae: " + format(data.family$getMotherUuid()));
        send(source, "Pai: " + format(data.family$getFatherUuid()));
        send(source, "Parceiro: " + format(data.family$getPartnerUuid()));
        send(source, "Filhos registrados: " + data.family$getChildUuids().size());
        send(source, "Alerta: " + data.family$getAlertTicks() + " ticks, cooldown: " + data.family$getAlertCooldownTicks() + " ticks");
        send(source, "Ameaca: " + format(data.family$getThreatUuid()));
        send(source, "Filhote em alerta: " + format(data.family$getAlertChildUuid()));
        send(source, "Versao dos dados: " + data.family$getDataVersion());
        return 1;
    }

    private static Optional<Animal> findLookedAtAnimal(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(INSPECT_RANGE)).inflate(2.0D);

        return player.level()
                .getEntitiesOfClass(Animal.class, searchBox, FamilyAi::isFamilyAnimal)
                .stream()
                .filter(animal -> isInAimCone(eye, look, animal))
                .min(Comparator.comparingDouble(animal -> projectedDistance(eye, look, animal)));
    }

    private static boolean isInAimCone(Vec3 eye, Vec3 look, Animal animal) {
        Vec3 toAnimal = animal.getBoundingBox().getCenter().subtract(eye);
        double projected = toAnimal.dot(look);
        if (projected < 0.0D || projected > INSPECT_RANGE) {
            return false;
        }

        double perpendicularSquared = toAnimal.lengthSqr() - projected * projected;
        return perpendicularSquared <= MAX_AIM_OFFSET_SQUARED;
    }

    private static double projectedDistance(Vec3 eye, Vec3 look, Animal animal) {
        return animal.getBoundingBox().getCenter().subtract(eye).dot(look);
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static String format(Optional<UUID> uuid) {
        return uuid.map(FamilyAiCommands::shortUuid).orElse("-");
    }

    private static String shortUuid(UUID uuid) {
        String value = uuid.toString();
        return value.substring(0, 8);
    }
}

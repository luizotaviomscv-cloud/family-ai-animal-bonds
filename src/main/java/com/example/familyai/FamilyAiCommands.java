package com.example.familyai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
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
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("inspect").executes(context -> inspect(context.getSource())))
                .then(Commands.literal("animalinfo").executes(context -> inspect(context.getSource())))
                .then(Commands.literal("herdinfo").executes(context -> herdInfo(context.getSource())))
                .then(Commands.literal("reload")
                        .requires(FamilyAiCommands::hasManagePermission)
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("guide").executes(context -> guide(context.getSource())))
                .then(Commands.literal("debug")
                        .requires(FamilyAiCommands::hasManagePermission)
                        .then(Commands.literal("on").executes(context -> setDebug(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setDebug(context.getSource(), false))))
                .then(Commands.literal("hud")
                        .requires(FamilyAiCommands::hasManagePermission)
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("off");
                                    builder.suggest("simple");
                                    builder.suggest("detailed");
                                    builder.suggest("debug");
                                    return builder.buildFuture();
                                })
                                .executes(context -> setHudMode(context.getSource(), StringArgumentType.getString(context, "mode"))))));
    }

    private static boolean hasManagePermission(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int status(CommandSourceStack source) {
        FamilyAiConfig config = FamilyAiConfig.get();
        send(source, "Family AI 1.2.0");
        send(source, "IA avancada: " + onOff(config.enableAdvancedAi));
        send(source, "Sistema familiar: " + onOff(config.enableFamilySystem));
        send(source, "Sistema de rebanho: " + onOff(config.enableHerdSystem));
        send(source, "Protecao de filhotes: " + onOff(config.enableChildProtection));
        send(source, "Reacao a perigo: " + onOff(config.enableAdvancedDangerReaction));
        send(source, "Antitravamento: " + onOff(config.enableUnstuckSystem));
        send(source, "HUD: " + onOff(config.enableHud) + " (" + config.resolvedHudMode() + ")");
        send(source, "Chat do mod: " + onOff(config.enableChatMessages));
        send(source, "Livro guia: " + onOff(config.enableGuideBook));
        send(source, "Debug: " + onOff(config.enableDebug) + " (nivel " + config.debugDetailLevel + ")");
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        FamilyAiConfig.load();
        send(source, "[Family AI] Configuracoes recarregadas com sucesso.");
        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        FamilyAiConfig config = FamilyAiConfig.get();
        config.enableDebug = enabled;
        FamilyAiConfig.save();
        send(source, enabled ? "[Family AI] Modo debug ativado." : "[Family AI] Modo debug desativado.");
        return 1;
    }

    private static int setHudMode(CommandSourceStack source, String modeRaw) {
        FamilyAiHudMode mode = FamilyAiHudMode.fromName(modeRaw);
        FamilyAiConfig config = FamilyAiConfig.get();
        config.enableHud = mode != FamilyAiHudMode.OFF;
        config.setHudMode(mode);
        FamilyAiConfig.save();
        send(source, "[Family AI] HUD atualizado para: " + mode);
        return 1;
    }

    private static int guide(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!FamilyAiGuideBook.giveGuideBook(player, true)) {
            send(source, "[Family AI] O guia esta desativado na configuracao.");
            return 0;
        }
        send(source, "[Family AI] Guia entregue no inventario.");
        return 1;
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
        send(source, "Estado IA: " + data.family$getAiState());
        send(source, "Sexo: " + data.family$getSex());
        send(source, "Mae: " + format(data.family$getMotherUuid()));
        send(source, "Pai: " + format(data.family$getFatherUuid()));
        send(source, "Parceiro: " + format(data.family$getPartnerUuid()));
        send(source, "Filhos: " + data.family$getChildUuids().size() + " | Irmaos: " + data.family$getSiblingUuids().size());
        send(source, "Ameaca: " + format(data.family$getThreatUuid()) + " | Alerta: " + data.family$getAlertTicks() + "t");
        send(source, "Lider: " + format(data.family$getLeaderUuid()) + " | Guardiao temp: " + format(data.family$getTempGuardianUuid()));
        send(source, "Panic cooldown: " + data.family$getPanicCooldownTicks() + " | Stuck: " + data.family$getStuckTicks());
        send(source, "Path fail: " + data.family$getPathFailCount() + " | Ultimo recalc: " + data.family$getLastPathRecalcTick());
        send(source, String.format("Traços [medo/protecao/rebanho]: %.2f / %.2f / %.2f",
                data.family$getTraitFear(),
                data.family$getTraitProtective(),
                data.family$getTraitHerdAffinity()));
        int reputation = FamilyAi.getReputationFor(animal, player.getUUID());
        ReputationState state = FamilyAi.getReputationState(animal, player.getUUID());
        send(source, "Reputacao com voce: " + reputation + " (" + state + ")");
        send(source, "Entradas de reputacao: " + data.family$getReputationMap().size() + " | DataVersion: " + data.family$getDataVersion());
        return 1;
    }

    private static int herdInfo(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        double range = FamilyAiConfig.get().regroupDistance;
        Optional<Animal> target = findLookedAtAnimal(player)
                .or(() -> findNearestFamilyAnimal(player, range));

        if (target.isEmpty()) {
            source.sendFailure(Component.literal("Nenhum rebanho proximo encontrado."));
            return 0;
        }

        Animal anchor = target.get();
        AABB box = anchor.getBoundingBox().inflate(range);
        List<Animal> herd = player.level().getEntitiesOfClass(Animal.class, box, candidate -> FamilyAi.isFamilyAnimal(candidate) && candidate.getType() == anchor.getType());
        long babies = herd.stream().filter(Animal::isBaby).count();
        long adults = herd.size() - babies;
        long alert = herd.stream().filter(candidate -> ((FamilyAnimal) candidate).family$isAlert()).count();

        send(source, "Rebanho: " + anchor.getType().toShortString());
        send(source, "Total: " + herd.size() + " | Adultos: " + adults + " | Filhotes: " + babies);
        send(source, "Em alerta: " + alert + " | Raio: " + String.format("%.1f", range));
        return 1;
    }

    private static Optional<Animal> findNearestFamilyAnimal(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        return player.level()
                .getEntitiesOfClass(Animal.class, box, FamilyAi::isFamilyAnimal)
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr));
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

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}

package com.example.familyai;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FamilyAiChat {
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private FamilyAiChat() {
    }

    public static void sendIfEnabled(ServerPlayer player, String message) {
        if (!FamilyAiConfig.get().enableChatMessages) {
            return;
        }
        player.sendSystemMessage(Component.literal("[Family AI] " + message));
    }

    public static void sendWithCooldown(ServerPlayer player, String key, String message) {
        if (!FamilyAiConfig.get().enableChatMessages) {
            return;
        }
        long now = player.level().getGameTime();
        int cooldown = FamilyAiConfig.get().chatMessageCooldownTicks;
        String fullKey = player.getUUID() + ":" + key;
        long last = COOLDOWNS.getOrDefault(fullKey, Long.MIN_VALUE / 4L);
        if (now - last < cooldown) {
            return;
        }
        COOLDOWNS.put(fullKey, now);
        player.sendSystemMessage(Component.literal("[Family AI] " + message));
    }

    public static void sendWelcome(ServerPlayer player) {
        if (!FamilyAiConfig.get().enableWelcomeMessage) {
            return;
        }
        sendIfEnabled(player, "Obrigado por instalar o Family AI 1.2.0! \uD83D\uDC3E");
        sendIfEnabled(player, "A IA avancada dos animais esta ativa.");
        sendIfEnabled(player, "Use o Mod Menu para personalizar o comportamento.");
    }

    public static void clearPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        COOLDOWNS.keySet().removeIf(key -> key.startsWith(playerUuid.toString() + ":"));
    }
}

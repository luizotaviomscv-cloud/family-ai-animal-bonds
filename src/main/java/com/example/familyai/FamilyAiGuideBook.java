package com.example.familyai;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

public final class FamilyAiGuideBook {
    private static final String GUIDE_TAG = "family_ai_guide_given_1_2_0";

    private FamilyAiGuideBook() {
    }

    public static boolean giveGuideOnFirstJoin(ServerPlayer player) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableGuideBook) {
            return false;
        }
        if (player.getTags().contains(GUIDE_TAG)) {
            return false;
        }
        boolean given = giveGuideBook(player, false);
        if (given) {
            player.addTag(GUIDE_TAG);
        }
        return given;
    }

    public static boolean giveGuideBook(ServerPlayer player, boolean fromCommand) {
        FamilyAiConfig config = FamilyAiConfig.get();
        if (!config.enableGuideBook || (!config.allowGuideCommand && fromCommand)) {
            return false;
        }

        ItemStack guide = new ItemStack(Items.WRITTEN_BOOK);
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("Guia do Family AI"),
                "Luiz Otavio Marques",
                0,
                List.of(
                        Filterable.passThrough(Component.literal("Obrigado por instalar o Family AI 1.2.0! \uD83D\uDC3E\n\nAgora os animais ficam mais vivos, com familia, rebanho, memoria e reacoes mais naturais ao perigo.")),
                        Filterable.passThrough(Component.literal("O mod melhora a IA dos animais com: parentesco, protecao de filhotes, coesao de rebanho, lider temporario, personalidade e memoria simples.")),
                        Filterable.passThrough(Component.literal("Abra o Mod Menu para configurar: IA, rebanho, HUD, mensagens no chat, debug e performance.")),
                        Filterable.passThrough(Component.literal("Use os comandos:\n/familyai status\n/familyai animalinfo\n/familyai herdinfo\n/familyai debug on|off\n/familyai hud <modo>")),
                        Filterable.passThrough(Component.literal("Obrigado por usar o Family AI.\n\nBoa aventura!"))
                ),
                true
        );
        guide.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        boolean inserted = player.getInventory().add(guide);
        if (!inserted) {
            player.drop(guide, false);
        }

        player.sendSystemMessage(Component.literal("[Family AI] Obrigado por instalar o Family AI 1.2.0!"));
        player.sendSystemMessage(Component.literal("[Family AI] Abra o Mod Menu para configurar IA, rebanho, HUD e debug."));
        player.sendSystemMessage(Component.literal("[Family AI] Dica: use /familyai status e /familyai animalinfo para diagnostico."));
        return true;
    }
}

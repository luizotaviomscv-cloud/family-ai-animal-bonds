package com.example.familyai.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class FamilyAiConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 154;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private final Screen parent;

    public FamilyAiConfigScreen(Screen parent) {
        super(Component.translatable("family_ai.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FamilyAiConfigCategory[] categories = FamilyAiConfigCategory.values();
        int columns = 3;
        int totalWidth = columns * BUTTON_WIDTH + (columns - 1) * BUTTON_GAP;
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 4;

        for (int i = 0; i < categories.length; i++) {
            FamilyAiConfigCategory category = categories[i];
            int row = i / columns;
            int column = i % columns;
            int x = startX + column * (BUTTON_WIDTH + BUTTON_GAP);
            int y = startY + row * (BUTTON_HEIGHT + BUTTON_GAP);
            this.addRenderableWidget(Button.builder(
                    Component.translatable(category.labelKey()),
                    button -> this.minecraft.setScreen(new FamilyAiConfigCategoryScreen(this, category))
            ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        int rows = (categories.length + columns - 1) / columns;
        int doneButtonY = startY + rows * (BUTTON_HEIGHT + BUTTON_GAP) + 14;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds((this.width - 150) / 2, doneButtonY, 150, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        guiGraphics.drawCenteredString(this.font, Component.translatable("family_ai.config.hub.subtitle"), this.width / 2, 36, 10526880);
    }
}

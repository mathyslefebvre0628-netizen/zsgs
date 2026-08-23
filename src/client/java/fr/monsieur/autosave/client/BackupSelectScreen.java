package fr.monsieur.autosave.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BackupSelectScreen extends Screen {
    private final Screen parent;
    private String[] backups = new String[0];
    private int selected = -1;
    private String status = "";

    public BackupSelectScreen(Screen parent) {
        super(Component.literal("Choisir une sauvegarde"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        backups = AutoSaveService.listBackups();
        int panelWidth = 460;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(20, (this.height - 380) / 2);

        int visible = Math.min(backups.length, 8);
        for (int i = 0; i < visible; i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal(backups[i]), button -> {
                selected = index;
                status = "Sélectionné : " + backups[index];
            }).bounds(left + 20, top + 55 + i * 30, 420, 24).build());
        }

        addRenderableWidget(Button.builder(Component.literal("Restaurer"), button -> restoreSelected())
                .bounds(left + 20, top + 315, 200, 24).build());
        addRenderableWidget(Button.builder(Component.literal("Retour"), button -> this.minecraft.gui.setScreen(parent))
                .bounds(left + 240, top + 315, 200, 24).build());
    }

    private void restoreSelected() {
        if (selected < 0 || selected >= backups.length) {
            status = "Sélectionne une sauvegarde.";
            return;
        }
        this.minecraft.gui.setScreen(new RestoreConfirmScreen(this, parent, backups[selected]));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, 0xFF050505);
        int panelWidth = 460;
        int panelHeight = 380;
        int left = (this.width - panelWidth) / 2;
        int top = Math.max(20, (this.height - panelHeight) / 2);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF101010);
        graphics.text(this.font, Component.literal("CHOISIR UNE SAUVEGARDE"), left + 20, top + 22, 0xFFFFFFFF, false);
        if (backups.length == 0) graphics.text(this.font, Component.literal("Aucune sauvegarde trouvée."), left + 20, top + 85, 0xFFAAAAAA, false);
        graphics.text(this.font, status, left + 20, top + 292, 0xFFAAAAAA, false);
    }
}

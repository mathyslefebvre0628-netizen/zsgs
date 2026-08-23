package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RestoreConfirmScreen extends Screen {

    private final Screen backupList;
    private final Screen parent;
    private final String backupName;

    private String status = "";

    public RestoreConfirmScreen(
            Screen backupList,
            Screen parent,
            String backupName
    ) {
        super(Component.literal("Confirmer la restauration"));
        this.backupList = backupList;
        this.parent = parent;
        this.backupName = backupName;
    }

    @Override
    protected void init() {
        int left = (this.width - 360) / 2;
        int top = Math.max(30, (this.height - 220) / 2);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Annuler"),
                        button -> this.minecraft.gui.setScreen(backupList)
                ).bounds(
                        left,
                        top + 145,
                        170,
                        24
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("Restaurer"),
                        button -> restore()
                ).bounds(
                        left + 190,
                        top + 145,
                        170,
                        24
                ).build()
        );
    }

    private void restore() {
        try {
            AutoSaveService.restore(
                    Minecraft.getInstance(),
                    backupName
            );

            status = "Restauration terminée.";

            this.minecraft.gui.setScreen(parent);

        } catch (Exception e) {
            status = "Échec : " + e.getMessage();
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0xFF050505
        );

        int left = (this.width - 400) / 2;
        int top = Math.max(
                40,
                (this.height - 210) / 2
        );

        graphics.fill(
                left,
                top,
                left + 400,
                top + 210,
                0xFF101010
        );

        graphics.text(
                this.font,
                Component.literal("RESTAURER CETTE SAUVEGARDE ?"),
                left + 20,
                top + 25,
                0xFFFFFFFF,
                false
        );

        graphics.text(
                this.font,
                Component.literal(backupName),
                left + 20,
                top + 60,
                0xFFCCCCCC,
                false
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Une sauvegarde de sécurité sera créée."
                ),
                left + 20,
                top + 90,
                0xFFAAAAAA,
                false
        );

        graphics.text(
                this.font,
                Component.literal(
                        "Le monde actuel sera remplacé."
                ),
                left + 20,
                top + 110,
                0xFFAAAAAA,
                false
        );

        graphics.text(
                this.font,
                Component.literal(status),
                left + 20,
                top + 190,
                0xFFAAAAAA,
                false
        );
    }
}

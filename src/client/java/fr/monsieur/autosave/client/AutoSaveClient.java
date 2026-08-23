package fr.monsieur.autosave.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class AutoSaveClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autosave", "main"));

    private static KeyMapping openGuiKey;
    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.autosave.open_gui",
                        InputConstants.Type.KEYSYM,
                        InputConstants.KEY_4,
                        CATEGORY
                )
        );

        AutoSaveConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                client.gui.setScreen(new AutoSaveScreen(client.gui.screen()));
            }

            if (!AutoSaveConfig.enabled || AutoSaveConfig.folder.isBlank()) return;

            if (++tickCounter >= AutoSaveConfig.intervalTicks) {
                tickCounter = 0;
                AutoSaveService.createBackup(client);
            }
        });
    }
}

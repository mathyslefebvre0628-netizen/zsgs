package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class AutoSaveConfig {

    static boolean enabled = true;

    static String folder = "";

    // 5 minutes par défaut.
    static int intervalTicks = 20 * 60 * 5;

    private AutoSaveConfig() {
    }

    private static Path file() {
        return Minecraft.getInstance()
                .gameDirectory
                .toPath()
                .resolve("config")
                .resolve("autosave.properties");
    }

    static void load() {
        Path config = file();

        if (!Files.isRegularFile(config)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(
                    config,
                    StandardCharsets.UTF_8
            )) {
                int separator = line.indexOf('=');

                if (separator <= 0) {
                    continue;
                }

                String key = line
                        .substring(0, separator)
                        .trim();

                String value = line
                        .substring(separator + 1)
                        .trim();

                switch (key) {
                    case "enabled" ->
                            enabled =
                                    Boolean.parseBoolean(value);

                    case "folder" ->
                            folder = value;

                    case "intervalTicks" -> {
                        try {
                            intervalTicks = Math.max(
                                    20,
                                    Integer.parseInt(value)
                            );
                        } catch (NumberFormatException ignored) {
                            intervalTicks = 20 * 60 * 5;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "[AutoSave] Impossible de lire la configuration."
            );
            e.printStackTrace();
        }
    }

    static void save() {
        Path config = file();

        try {
            Files.createDirectories(
                    config.getParent()
            );

            String safeFolder =
                    folder == null
                            ? ""
                            : folder
                                    .replace("\r", "")
                                    .replace("\n", "");

            String content =
                    "enabled=" + enabled + "\n"
                            + "folder=" + safeFolder + "\n"
                            + "intervalTicks="
                            + Math.max(
                                    20,
                                    intervalTicks
                            )
                            + "\n";

            Files.writeString(
                    config,
                    content,
                    StandardCharsets.UTF_8
            );

            System.out.println(
                    "[AutoSave] Configuration sauvegardée."
            );

        } catch (IOException e) {
            System.err.println(
                    "[AutoSave] Impossible d'écrire la configuration."
            );
            e.printStackTrace();
        }
    }
}

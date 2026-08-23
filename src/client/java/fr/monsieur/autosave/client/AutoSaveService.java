package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

final class AutoSaveService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd_HH-mm-ss"
            );

    private AutoSaveService() {
    }

    /*
     * =========================================================
     * CHOIX DU DOSSIER
     * =========================================================
     */

    static void chooseFolder(
            AutoSaveScreen screen
    ) {
        CompletableFuture
                .supplyAsync(
                        AutoSaveService::openWindowsFolderPicker
                )
                .thenAccept(path -> {

                    if (path == null || path.isBlank()) {
                        return;
                    }

                    Minecraft.getInstance().execute(() -> {

                        try {
                            Path folder = Path.of(path)
                                    .toAbsolutePath()
                                    .normalize();

                            /*
                             * Création immédiate du dossier.
                             */
                            Files.createDirectories(folder);

                            /*
                             * Vérification.
                             */
                            if (!Files.isDirectory(folder)) {
                                throw new IOException(
                                        "Le dossier n'existe pas."
                                );
                            }

                            AutoSaveConfig.folder =
                                    folder.toString();

                            AutoSaveConfig.save();

                            screen.updateFolder(
                                    folder.toString()
                            );

                            System.out.println(
                                    "[AutoSave] Dossier sélectionné : "
                                            + folder
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "[AutoSave] Impossible de créer le dossier."
                            );

                            e.printStackTrace();
                        }
                    });
                });
    }

    private static String openWindowsFolderPicker() {

        String script = """
                Add-Type -AssemblyName System.Windows.Forms

                $dialog = New-Object System.Windows.Forms.FolderBrowserDialog

                $dialog.Description = 'Choisir le dossier des sauvegardes'

                $dialog.UseDescriptionForTitle = $true

                $dialog.ShowNewFolderButton = $true

                $result = $dialog.ShowDialog()

                if ($result -eq [System.Windows.Forms.DialogResult]::OK) {
                    [Console]::WriteLine($dialog.SelectedPath)
                }
                """;

        try {
            Process process =
                    new ProcessBuilder(
                            "powershell.exe",
                            "-NoProfile",
                            "-STA",
                            "-ExecutionPolicy",
                            "Bypass",
                            "-Command",
                            script
                    )
                            .redirectErrorStream(true)
                            .start();

            String output =
                    new String(
                            process.getInputStream()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    ).trim();

            int exitCode =
                    process.waitFor();

            if (exitCode != 0
                    || output.isBlank()) {
                return null;
            }

            return output;

        } catch (Exception e) {

            System.err.println(
                    "[AutoSave] Sélecteur de dossier impossible."
            );

            e.printStackTrace();

            return null;
        }
    }

    /*
     * =========================================================
     * SAUVEGARDE IMMÉDIATE
     * =========================================================
     */

    static void createBackup(
            Minecraft client
    ) {
        MinecraftServer server =
                client.getSingleplayerServer();

        if (server == null) {
            System.err.println(
                    "[AutoSave] Aucun monde solo ouvert."
            );
            return;
        }

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {

            System.err.println(
                    "[AutoSave] Aucun dossier configuré."
            );

            return;
        }

        server.execute(() -> {

            try {

                /*
                 * Sauvegarde interne de Minecraft.
                 */
                if (!server.saveEverything(
                        false,
                        true,
                        true
                )) {
                    throw new IOException(
                            "Minecraft n'a pas pu sauvegarder le monde."
                    );
                }

                Path world =
                        server.getWorldPath(
                                LevelResource.ROOT
                        )
                                .toAbsolutePath()
                                .normalize();

                if (!Files.isDirectory(world)) {
                    throw new IOException(
                            "Monde introuvable : " + world
                    );
                }

                Path root =
                        Path.of(
                                AutoSaveConfig.folder
                        )
                                .toAbsolutePath()
                                .normalize();

                /*
                 * Création du dossier principal.
                 */
                Files.createDirectories(root);

                Path target =
                        root.resolve(
                                "autosave_"
                                        + STAMP.format(
                                        LocalDateTime.now()
                                )
                        );

                System.out.println(
                        "[AutoSave] Création de : "
                                + target
                );

                copyDirectory(
                        world,
                        target
                );

                System.out.println(
                        "[AutoSave] Sauvegarde créée : "
                                + target
                );

            } catch (Exception e) {

                System.err.println(
                        "[AutoSave] ERREUR DE SAUVEGARDE"
                );

                e.printStackTrace();
            }
        });
    }

    /*
     * =========================================================
     * LISTE DES SAUVEGARDES
     * =========================================================
     */

    static String[] listBackups() {

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {

            return new String[0];
        }

        try {

            Path root =
                    Path.of(
                            AutoSaveConfig.folder
                    )
                            .toAbsolutePath()
                            .normalize();

            if (!Files.isDirectory(root)) {
                return new String[0];
            }

            try (var stream =
                         Files.list(root)) {

                return stream
                        .filter(Files::isDirectory)
                        .map(path ->
                                path.getFileName()
                                        .toString()
                        )
                        .filter(name ->
                                name.startsWith(
                                        "autosave_"
                                )
                                        || name.startsWith(
                                        "backup_before_restore_"
                                )
                        )
                        .sorted(
                                Comparator.reverseOrder()
                        )
                        .toArray(String[]::new);
            }

        } catch (Exception e) {

            System.err.println(
                    "[AutoSave] Impossible de lire les sauvegardes."
            );

            e.printStackTrace();

            return new String[0];
        }
    }

    /*
     * =========================================================
     * RESTAURATION
     * =========================================================
     */

    static void restore(
            Minecraft client,
            String backupName
    ) throws IOException {

        MinecraftServer server =
                client.getSingleplayerServer();

        if (server == null) {
            throw new IOException(
                    "Aucun monde solo ouvert."
            );
        }

        if (AutoSaveConfig.folder == null
                || AutoSaveConfig.folder.isBlank()) {

            throw new IOException(
                    "Aucun dossier configuré."
            );
        }

        Path root =
                Path.of(
                        AutoSaveConfig.folder
                )
                        .toAbsolutePath()
                        .normalize();

        Path backup =
                root.resolve(
                                backupName
                        )
                        .toAbsolutePath()
                        .normalize();

        /*
         * Protection contre un chemin extérieur
         * au dossier configuré.
         */
        if (!backup.startsWith(root)
                || !Files.isDirectory(backup)) {

            throw new IOException(
                    "Sauvegarde invalide."
            );
        }

        /*
         * Sauvegarde du monde actuel.
         */
        if (!server.saveEverything(
                false,
                true,
                true
        )) {

            throw new IOException(
                    "Impossible de sauvegarder le monde actuel."
            );
        }

        Path world =
                server.getWorldPath(
                        LevelResource.ROOT
                )
                        .toAbsolutePath()
                        .normalize();

        /*
         * Backup de sécurité.
         */
        Path safety =
                root.resolve(
                        "backup_before_restore_"
                                + STAMP.format(
                                LocalDateTime.now()
                        )
                );

        System.out.println(
                "[AutoSave] Backup de sécurité : "
                        + safety
        );

        copyDirectory(
                world,
                safety
        );

        /*
         * Remplacement.
         */
        deleteDirectory(world);

        copyDirectory(
                backup,
                world
        );

        System.out.println(
                "[AutoSave] Restauration terminée : "
                        + backupName
        );
    }

    /*
     * =========================================================
     * COPIE
     * =========================================================
     */

    private static void copyDirectory(
            Path source,
            Path target
    ) throws IOException {

        if (!Files.exists(source)) {
            throw new IOException(
                    "Source inexistante : "
                            + source
            );
        }

        Files.createDirectories(target);

        try (var stream =
                     Files.walk(source)) {

            stream.forEach(path -> {

                try {

                    Path relative =
                            source.relativize(path);

                    Path destination =
                            target.resolve(relative);

                    if (Files.isDirectory(path)) {

                        Files.createDirectories(
                                destination
                        );

                    } else {

                        Files.copy(
                                path,
                                destination,
                                StandardCopyOption
                                        .REPLACE_EXISTING,
                                StandardCopyOption
                                        .COPY_ATTRIBUTES
                        );
                    }

                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            });
        }
    }

    /*
     * =========================================================
     * SUPPRESSION
     * =========================================================
     */

    private static void deleteDirectory(
            Path root
    ) throws IOException {

        if (!Files.exists(root)) {
            return;
        }

        try (var stream =
                     Files.walk(root)) {

            stream
                    .sorted(
                            Comparator.reverseOrder()
                    )
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}

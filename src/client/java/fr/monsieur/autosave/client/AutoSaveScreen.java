package fr.monsieur.autosave.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AutoSaveScreen extends Screen {

    private static final int MAX_WINDOW_WIDTH = 520;
    private static final int MAX_WINDOW_HEIGHT = 390;

    private final Screen parent;

    private Button toggleButton;
    private Button intervalButton;

    private boolean intervalMenuOpen = false;
    private int intervalScroll = 0;

    private double contentScroll = 0.0;

    private String status = "Prêt";

    private static final IntervalOption[] INTERVALS = {
            new IntervalOption("1 MIN", 20 * 60),
            new IntervalOption("5 MIN", 20 * 60 * 5),
            new IntervalOption("10 MIN", 20 * 60 * 10),
            new IntervalOption("15 MIN", 20 * 60 * 15),
            new IntervalOption("30 MIN", 20 * 60 * 30),
            new IntervalOption("1 H", 20 * 60 * 60)
    };

    public AutoSaveScreen(Screen parent) {
        super(Component.literal("Auto Save"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();

        Layout l = calculateLayout();

        toggleButton = Button.builder(
                Component.literal(
                        AutoSaveConfig.enabled ? "ON" : "OFF"
                ),
                button -> {
                    AutoSaveConfig.enabled = !AutoSaveConfig.enabled;
                    AutoSaveConfig.save();

                    button.setMessage(
                            Component.literal(
                                    AutoSaveConfig.enabled ? "ON" : "OFF"
                            )
                    );

                    status = AutoSaveConfig.enabled
                            ? "Sauvegarde activée"
                            : "Sauvegarde désactivée";
                }
        ).bounds(
                l.controlX,
                l.toggleY,
                l.controlWidth,
                l.buttonHeight
        ).build();

        addRenderableWidget(toggleButton);

        intervalButton = Button.builder(
                Component.literal(
                        getCurrentIntervalName()
                ),
                button -> toggleIntervalMenu()
        ).bounds(
                l.controlX,
                l.intervalY,
                l.controlWidth,
                l.buttonHeight
        ).build();

        addRenderableWidget(intervalButton);

        addRenderableWidget(
                Button.builder(
                        Component.literal("SELECT"),
                        button -> AutoSaveService.chooseFolder(this)
                ).bounds(
                        l.selectX,
                        l.folderY,
                        l.halfWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("SAVE NOW"),
                        button -> {
                            if (AutoSaveConfig.folder == null
                                    || AutoSaveConfig.folder.isBlank()) {
                                status = "Sélectionne un dossier d'abord";
                                return;
                            }

                            AutoSaveService.createBackup(
                                    Minecraft.getInstance()
                            );

                            status = "Sauvegarde en cours...";
                        }
                ).bounds(
                        l.restoreX,
                        l.folderY,
                        l.halfWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("RESTORE"),
                        button -> openBackupScreen()
                ).bounds(
                        l.restoreX,
                        l.restoreY,
                        l.halfWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("SAVE CONFIG"),
                        button -> {
                            AutoSaveConfig.save();
                            status = "Configuration enregistrée";
                        }
                ).bounds(
                        l.saveX,
                        l.footerY,
                        l.footerButtonWidth,
                        l.buttonHeight
                ).build()
        );

        addRenderableWidget(
                Button.builder(
                        Component.literal("CLOSE"),
                        button -> onClose()
                ).bounds(
                        l.closeX,
                        l.footerY,
                        l.footerButtonWidth,
                        l.buttonHeight
                ).build()
        );

        if (intervalMenuOpen) {
            for (int i = 0; i < l.visibleIntervalCount; i++) {
                int actualIndex = intervalScroll + i;

                if (actualIndex >= INTERVALS.length) {
                    break;
                }

                final int index = actualIndex;

                addRenderableWidget(
                        Button.builder(
                                Component.literal(
                                        INTERVALS[index].name
                                ),
                                button -> selectInterval(
                                        INTERVALS[index]
                                )
                        ).bounds(
                                l.menuX,
                                l.menuY
                                        + i * l.menuOptionHeight,
                                l.menuWidth,
                                l.menuOptionHeight
                        ).build()
                );
            }
        }
    }

    private void toggleIntervalMenu() {
        intervalMenuOpen = !intervalMenuOpen;
        intervalScroll = 0;

        status = intervalMenuOpen
                ? "Choisis un intervalle"
                : "Intervalle : " + getCurrentIntervalName();

        refreshWidgets();
    }

    private void selectInterval(IntervalOption option) {
        AutoSaveConfig.intervalTicks = option.ticks;
        AutoSaveConfig.save();

        intervalMenuOpen = false;
        intervalScroll = 0;

        status = "Intervalle : " + option.name;

        refreshWidgets();
    }

    private String getCurrentIntervalName() {
        for (IntervalOption option : INTERVALS) {
            if (option.ticks == AutoSaveConfig.intervalTicks) {
                return option.name;
            }
        }

        return "5 MIN";
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        Layout l = calculateLayout();

        if (intervalMenuOpen
                && mouseX >= l.menuX
                && mouseX <= l.menuX + l.menuWidth
                && mouseY >= l.menuY
                && mouseY <= l.menuY + l.menuHeight) {

            int maxScroll = Math.max(
                    0,
                    INTERVALS.length - l.visibleIntervalCount
            );

            if (verticalAmount < 0) {
                intervalScroll = Math.min(
                        maxScroll,
                        intervalScroll + 1
                );
            } else if (verticalAmount > 0) {
                intervalScroll = Math.max(
                        0,
                        intervalScroll - 1
                );
            }

            refreshWidgets();
            return true;
        }

        if (verticalAmount < 0) {
            contentScroll = Math.min(
                    getMaxContentScroll(),
                    contentScroll + 20
            );
        } else if (verticalAmount > 0) {
            contentScroll = Math.max(
                    0,
                    contentScroll - 20
            );
        }

        refreshWidgets();
        return true;
    }

    private double getMaxContentScroll() {
        Layout l = calculateLayout();

        return Math.max(
                0,
                l.contentHeight - l.visibleHeight
        );
    }

    void updateFolder(String path) {
        AutoSaveConfig.folder = path;
        AutoSaveConfig.save();
        status = "Dossier sélectionné";
    }

    private void openBackupScreen() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new BackupSelectScreen(this)
            );
        }
    }

    @Override
    public void onClose() {
        AutoSaveConfig.save();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        Layout l = calculateLayout();

        graphics.fill(
                0,
                0,
                this.width,
                this.height,
                0x76000000
        );

        graphics.fill(
                l.left,
                l.top,
                l.right,
                l.bottom,
                0xFF090909
        );

        graphics.text(
                this.font,
                Component.literal("AS"),
                l.left + l.padding,
                l.top + 14,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("AUTO SAVE"),
                l.left + l.padding + 24,
                l.top + 10,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                this.font,
                Component.literal("Automatic World Backup"),
                l.left + l.padding + 24,
                l.top + 26,
                0xFF666666,
                false
        );

        graphics.fill(
                l.left + l.padding,
                l.headerBottom,
                l.right - l.padding,
                l.headerBottom + 1,
                0xFF202020
        );

        int scroll = (int) contentScroll;

        graphics.enableScissor(
                l.left,
                l.contentTop,
                l.right,
                l.contentBottom
        );

        drawText(
                graphics,
                "AUTOMATIC BACKUP",
                l.left + l.padding,
                l.autoTitleY - scroll,
                0xFF666666
        );

        drawText(
                graphics,
                "Automatic backup",
                l.left + l.padding,
                l.autoNameY - scroll,
                0xFFFFFFFF
        );

        drawText(
                graphics,
                "Backup while playing.",
                l.left + l.padding,
                l.autoDescriptionY - scroll,
                0xFF666666
        );

        drawText(
                graphics,
                "BACKUP INTERVAL",
                l.left + l.padding,
                l.intervalNameY - scroll,
                0xFF666666
        );

        drawText(
                graphics,
                "Backup frequency.",
                l.left + l.padding,
                l.intervalDescriptionY - scroll,
                0xFF666666
        );

        drawText(
                graphics,
                "BACKUP LOCATION",
                l.left + l.padding,
                l.locationTitleY - scroll,
                0xFF666666
        );

        String folder =
                AutoSaveConfig.folder == null
                        || AutoSaveConfig.folder.isBlank()
                        ? "No folder selected"
                        : AutoSaveConfig.folder;

        if (folder.length() > 55) {
            folder = "..." + folder.substring(
                    folder.length() - 52
            );
        }

        graphics.fill(
                l.pathX,
                l.pathY - scroll,
                l.pathRight,
                l.pathBottom - scroll,
                0xFF050505
        );

        drawText(
                graphics,
                folder,
                l.pathTextX,
                l.pathTextY - scroll,
                0xFF888888
        );

        drawText(
                graphics,
                "SAVED BACKUPS",
                l.left + l.padding,
                l.backupsTitleY - scroll,
                0xFF666666
        );

        int count = AutoSaveService.listBackups().length;

        drawText(
                graphics,
                count + (
                        count == 1
                                ? " backup"
                                : " backups"
                ),
                l.left + l.padding,
                l.backupsCountY - scroll,
                0xFF666666
        );

        graphics.disableScissor();

        graphics.text(
                this.font,
                Component.literal(status),
                l.left + l.padding,
                l.statusY,
                0xFF555555,
                false
        );

        if (intervalMenuOpen) {
            graphics.fill(
                    l.menuX - 1,
                    l.menuY - 1,
                    l.menuX + l.menuWidth + 1,
                    l.menuY + l.menuHeight + 1,
                    0xFF333333
            );

            graphics.fill(
                    l.menuX,
                    l.menuY,
                    l.menuX + l.menuWidth,
                    l.menuY + l.menuHeight,
                    0xFF050505
            );
        }

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );
    }

    private void drawText(
            GuiGraphicsExtractor graphics,
            String text,
            int x,
            int y,
            int color
    ) {
        graphics.text(
                this.font,
                Component.literal(text),
                x,
                y,
                color,
                false
        );
    }

    private Layout calculateLayout() {
        int w = this.width;
        int h = this.height;

        int windowWidth = Math.min(
                MAX_WINDOW_WIDTH,
                Math.max(300, w - 10)
        );

        int windowHeight = Math.min(
                MAX_WINDOW_HEIGHT,
                Math.max(250, h - 10)
        );

        int left = (w - windowWidth) / 2;
        int top = (h - windowHeight) / 2;

        int padding = Math.max(
                12,
                Math.min(20, windowWidth / 18)
        );

        int buttonHeight = 22;

        int controlWidth = Math.min(
                95,
                Math.max(70, windowWidth / 5)
        );

        int halfWidth = Math.max(
                100,
                (windowWidth - padding * 2 - 6) / 2
        );

        int headerBottom = top + 48;

        int contentTop = top + 48;
        int contentBottom = top + windowHeight - 35;
        int visibleHeight = contentBottom - contentTop;

        int autoTitleY = contentTop + 18;
        int autoNameY = autoTitleY + 19;
        int autoDescriptionY = autoNameY + 15;

        int intervalNameY = autoDescriptionY + 32;
        int intervalDescriptionY = intervalNameY + 15;

        int locationTitleY = intervalDescriptionY + 30;

        int pathY = locationTitleY + 13;
        int pathBottom = pathY + 27;

        int folderY = pathBottom + 8;
        int restoreY = folderY + buttonHeight + 8;

        int backupsTitleY = restoreY + buttonHeight + 18;
        int backupsCountY = backupsTitleY + 15;

        int footerY =
                top + windowHeight - buttonHeight - 8;

        int statusY = footerY - 15;

        int footerButtonWidth = 75;

        int closeX =
                left + windowWidth
                        - padding
                        - footerButtonWidth;

        int saveX =
                closeX - 5 - footerButtonWidth;

        int controlX =
                left + windowWidth
                        - padding
                        - controlWidth;

        int selectX = left + padding;

        int menuWidth = controlWidth + 8;
        int menuX = controlX - 8;
        int menuY = top + 180;

        int menuOptionHeight = 22;

        int visibleIntervalCount = Math.max(
                2,
                Math.min(
                        4,
                        (h - 150) / menuOptionHeight
                )
        );

        int menuHeight =
                visibleIntervalCount
                        * menuOptionHeight;

        int pathX = left + padding;
        int pathRight =
                left + windowWidth - padding;

        int pathTextX = pathX + 8;
        int pathTextY = pathY + 8;

        int contentHeight =
                backupsCountY + 30 - contentTop;

        return new Layout(
                left,
                top,
                left + windowWidth,
                top + windowHeight,
                padding,
                buttonHeight,
                controlX,
                controlWidth,
                top + 65,
                top + 108,
                headerBottom,
                contentTop,
                contentBottom,
                visibleHeight,
                autoTitleY,
                autoNameY,
                autoDescriptionY,
                intervalNameY,
                intervalDescriptionY,
                locationTitleY,
                pathY,
                pathBottom,
                pathX,
                pathRight,
                pathTextX,
                pathTextY,
                selectX,
                selectX + halfWidth + 6,
                halfWidth,
                folderY,
                restoreY,
                backupsTitleY,
                backupsCountY,
                statusY,
                saveX,
                closeX,
                footerButtonWidth,
                footerY,
                menuX,
                menuY,
                menuWidth,
                menuOptionHeight,
                menuHeight,
                visibleIntervalCount,
                55,
                contentHeight
        );
    }

    private record IntervalOption(
            String name,
            int ticks
    ) {
    }

    private record Layout(
            int left,
            int top,
            int right,
            int bottom,
            int padding,
            int buttonHeight,
            int controlX,
            int controlWidth,
            int toggleY,
            int intervalY,
            int headerBottom,
            int contentTop,
            int contentBottom,
            int visibleHeight,
            int autoTitleY,
            int autoNameY,
            int autoDescriptionY,
            int intervalNameY,
            int intervalDescriptionY,
            int locationTitleY,
            int pathY,
            int pathBottom,
            int pathX,
            int pathRight,
            int pathTextX,
            int pathTextY,
            int selectX,
            int restoreX,
            int halfWidth,
            int folderY,
            int restoreY,
            int backupsTitleY,
            int backupsCountY,
            int statusY,
            int saveX,
            int closeX,
            int footerButtonWidth,
            int footerY,
            int menuX,
            int menuY,
            int menuWidth,
            int menuOptionHeight,
            int menuHeight,
            int visibleIntervalCount,
            int maxPathLength,
            int contentHeight
    ) {
    }
}

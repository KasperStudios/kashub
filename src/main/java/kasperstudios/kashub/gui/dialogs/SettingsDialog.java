package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import kasperstudios.kashub.util.ScriptFileWatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingsDialog extends Screen {
    private final Screen parent;
    private EditorTheme theme;

    private List<SettingEntry> settings = new ArrayList<>();
    private int scrollY = 0;

    private int getDialogWidth() { return this.width; }
    private int getDialogHeight() { return this.height; }
    private static final int HEADER_HEIGHT = 40;
    private static final int ROW_HEIGHT = 40;
    private static final int TOGGLE_WIDTH = 44;
    private static final int TOGGLE_HEIGHT = 22;
    private static final int FOOTER_HEIGHT = 50;

    private Runnable onThemeChange;
    private Runnable onClose;

    public SettingsDialog(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
        this.theme = ThemeManager.getCurrentTheme();
        this.font = Minecraft.getInstance().font;
        initializeSettings();
    }

    public void setOnThemeChange(Runnable onThemeChange) {
        this.onThemeChange = onThemeChange;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void initializeSettings() {
        settings.clear();
        KashubConfig config = KashubConfig.getInstance();

        settings.add(new SettingEntry("EDITOR", null, null, true));
        settings.add(new SettingEntry("Line Numbers", "Show line numbers in editor",
            () -> config.editorLineNumbers, v -> config.editorLineNumbers = v));
        settings.add(new SettingEntry("Auto Complete", "Enable code auto-completion",
            () -> config.editorAutoComplete, v -> config.editorAutoComplete = v));
        settings.add(new SettingEntry("Syntax Highlight", "Enable syntax highlighting",
            () -> config.editorSyntaxHighlight, v -> config.editorSyntaxHighlight = v));

        settings.add(new SettingEntry("LOGGING", null, null, true));
        settings.add(new SettingEntry("Enable Logging", "Log script execution",
            () -> config.enableLogging, v -> config.enableLogging = v));
        settings.add(new SettingEntry("Log to File", "Save logs to file",
            () -> config.logToFile, v -> config.logToFile = v));
        settings.add(new SettingEntry("Log to Chat", "Show logs in game chat",
            () -> config.logToChat, v -> config.logToChat = v));

        settings.add(new SettingEntry("SECURITY", null, null, true));
        settings.add(new SettingEntry("Sandbox Mode", "Restrict dangerous commands",
            () -> config.sandboxMode, v -> config.sandboxMode = v));
        settings.add(new SettingEntry("Allow Cheats", "Enable cheat commands",
            () -> config.allowCheats, v -> config.allowCheats = v));
        settings.add(new SettingEntry("Allow Eval ⚠", "Execute Java code (DANGEROUS!)",
            () -> config.allowEval, v -> config.allowEval = v));
        settings.add(new SettingEntry("Dangerous Commands", "Allow high-risk terminal commands",
            () -> config.allowDangerousCommands, v -> config.allowDangerousCommands = v));

        settings.add(new SettingEntry("PERMISSIONS", null, null, true));
        settings.add(new SettingEntry("World Mod", "Allow scripts to modify blocks/recipes",
            () -> config.allowWorldModification, v -> config.allowWorldModification = v));
        settings.add(new SettingEntry("Inventory Mod", "Allow scripts to move items",
            () -> config.allowInventoryControl, v -> config.allowInventoryControl = v));
        settings.add(new SettingEntry("Player Control", "Allow scripts to move/look",
            () -> config.allowPlayerControl, v -> config.allowPlayerControl = v));

        settings.add(new SettingEntry("SCRIPTS", null, null, true));
        settings.add(new SettingEntry("Hide System Scripts", "Hide example/system scripts in file panel",
            () -> config.hideSystemScripts, v -> {
                config.hideSystemScripts = v;
                config.save();
            }));
        settings.add(new SettingEntry("Hot Reload", "Automatically reload scripts when files change",
            () -> config.hotReload, v -> {
                config.hotReload = v;
                config.save();
                if (v) {
                    ScriptFileWatcher.getInstance().start();
                } else {
                    ScriptFileWatcher.getInstance().stop();
                }
            }));
        settings.add(new SettingEntry("Autorun Enabled", "Auto-run scripts on startup",
            () -> config.autorunEnabled, v -> {
                config.autorunEnabled = v;
                config.save();
            }));
        
        settings.add(new SettingEntry("DISCORD", null, null, true));
        settings.add(new SettingEntry("Discord Rich Presence", "Show game status in Discord profile",
            () -> config.discordRpcEnabled, v -> {
                config.discordRpcEnabled = v;
                config.save();
                kasperstudios.kashub.services.discord.DiscordRichPresence discord = 
                    kasperstudios.kashub.services.discord.DiscordRichPresence.getInstance();
                if (v) {
                    discord.reconnect();
                } else {
                    discord.clearPresence();
                }
            }));
        settings.add(new SettingEntry("Show Dimension", "Display current dimension in Discord",
            () -> config.discordShowDimension, v -> {
                config.discordShowDimension = v;
                config.save();
            }));
        settings.add(new SettingEntry("Show Server Name", "Display server name in Discord",
            () -> config.discordShowServerName, v -> {
                config.discordShowServerName = v;
                config.save();
            }));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {

    }

    private int getDialogX() {
        return 0;
    }

    private int getDialogY() {
        return 0;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {

        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int dx = getDialogX();
        int dy = getDialogY();

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.sidebarColor);

        // Borders removed for fullscreen mode

        renderHeader(context, dx, dy, mouseX, mouseY);

        renderSettingsList(context, dx, dy + HEADER_HEIGHT, mouseX, mouseY);

        renderFooter(context, dx, dy + getDialogHeight() - FOOTER_HEIGHT, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + HEADER_HEIGHT, adjustBrightness(theme.sidebarColor, 10));

        context.drawString(font, Component.literal("⚙ SETTINGS"), dx + 16, dy + 14, theme.textColor, true);

        String themeText = "Theme: " + theme.name;
        int themeWidth = font.width(themeText);
        context.drawString(font, Component.literal(themeText), dx + getDialogWidth() - themeWidth - 60, dy + 14, theme.accentColor, false);

        int closeX = dx + getDialogWidth() - 30;
        int closeY = dy + 10;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20;
        context.fill(closeX, closeY, closeX + 20, closeY + 20, closeHovered ? theme.consoleErrorColor : theme.buttonColor);
        context.drawString(font, Component.literal("✕"), closeX + 7, closeY + 6, 0xFFFFFFFF, false);

        context.fill(dx, dy + HEADER_HEIGHT - 1, dx + getDialogWidth(), dy + HEADER_HEIGHT, theme.accentColor & 0x66FFFFFF);
    }

    private void renderSettingsList(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        int listHeight = getDialogHeight() - HEADER_HEIGHT - FOOTER_HEIGHT;
        int visibleRows = listHeight / ROW_HEIGHT;

        int startIndex = scrollY / ROW_HEIGHT;
        int endIndex = Math.min(startIndex + visibleRows + 2, settings.size());

        for (int i = startIndex; i < endIndex; i++) {
            SettingEntry entry = settings.get(i);
            int rowY = dy + (i * ROW_HEIGHT) - scrollY;

            if (rowY + ROW_HEIGHT < dy || rowY > dy + listHeight) continue;

            if (entry.isSection) {
                renderSectionHeader(context, entry, dx, rowY);
            } else {
                renderSettingRow(context, entry, dx, rowY, mouseX, mouseY);
            }
        }

        if (settings.size() * ROW_HEIGHT > listHeight) {
            int totalHeight = settings.size() * ROW_HEIGHT;
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = dy + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);

            context.fill(dx + getDialogWidth() - 6, dy, dx + getDialogWidth() - 2, dy + listHeight, 0x22FFFFFF);
            context.fill(dx + getDialogWidth() - 6, scrollbarY, dx + getDialogWidth() - 2, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }

    private void renderSectionHeader(GuiGraphics context, SettingEntry entry, int dx, int rowY) {
        context.fill(dx + 8, rowY + 4, dx + getDialogWidth() - 8, rowY + ROW_HEIGHT - 4, adjustBrightness(theme.sidebarColor, 8));
        context.drawString(font, Component.literal("▸ " + entry.name), dx + 16, rowY + 14, theme.accentColor, true);
    }

    private void renderSettingRow(GuiGraphics context, SettingEntry entry, int dx, int rowY, int mouseX, int mouseY) {

        context.drawString(font, Component.literal(entry.name), dx + 24, rowY + 8, theme.textColor, false);

        if (entry.description != null) {
            context.drawString(font, Component.literal(entry.description), dx + 24, rowY + 22, theme.textDimColor, false);
        }

        int toggleX = dx + getDialogWidth() - TOGGLE_WIDTH - 24;
        int toggleY = rowY + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;

        boolean value = entry.getter != null && entry.getter.get();
        boolean hovered = mouseX >= toggleX && mouseX < toggleX + TOGGLE_WIDTH &&
                         mouseY >= toggleY && mouseY < toggleY + TOGGLE_HEIGHT;

        int bgColor = value ? theme.consoleSuccessColor : theme.buttonColor;
        if (hovered) bgColor = brighten(bgColor, 20);
        context.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, bgColor);

        int knobX = value ? toggleX + TOGGLE_WIDTH - TOGGLE_HEIGHT + 2 : toggleX + 2;
        context.fill(knobX, toggleY + 2, knobX + TOGGLE_HEIGHT - 4, toggleY + TOGGLE_HEIGHT - 2, 0xFFFFFFFF);
    }

    private void renderFooter(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + FOOTER_HEIGHT, adjustBrightness(theme.sidebarColor, 5));
        context.fill(dx, dy, dx + getDialogWidth(), dy + 1, theme.accentColor & 0x44FFFFFF);

        int buttonY = dy + 12;
        int buttonWidth = 100;
        int buttonHeight = 28;
        int spacing = 12;

        int themeX = dx + 16;
        renderButton(context, "🎨 Theme", themeX, buttonY, buttonWidth, buttonHeight,
            theme.accentColor, mouseX, mouseY);

        int saveX = dx + getDialogWidth() - buttonWidth * 2 - spacing - 16;
        renderButton(context, "💾 Save", saveX, buttonY, buttonWidth, buttonHeight,
            theme.consoleSuccessColor, mouseX, mouseY);

        int closeX = dx + getDialogWidth() - buttonWidth - 16;
        renderButton(context, "Close", closeX, buttonY, buttonWidth, buttonHeight,
            theme.buttonColor, mouseX, mouseY);
    }

    private void renderButton(GuiGraphics context, String text, int x, int y, int w, int h, int color, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bgColor = hovered ? brighten(color, 20) : color;

        context.fill(x, y, x + w, y + h, bgColor);

        if (hovered) {
            context.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
            context.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
            context.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
            context.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);
        }

        int textWidth = font.width(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        context.drawString(font, Component.literal(text), textX, textY, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int dx = getDialogX();
        int dy = getDialogY();

        int closeX = dx + getDialogWidth() - 30;
        int closeY = dy + 10;
        if (mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20) {
            this.onClose();
            return true;
        }

        int listY = dy + HEADER_HEIGHT;
        int listHeight = getDialogHeight() - HEADER_HEIGHT - FOOTER_HEIGHT;

        if (mouseY >= listY && mouseY < listY + listHeight) {
            int relativeY = (int) mouseY - listY + scrollY;
            int index = relativeY / ROW_HEIGHT;

            if (index >= 0 && index < settings.size()) {
                SettingEntry entry = settings.get(index);
                if (!entry.isSection && entry.getter != null && entry.setter != null) {
                    int toggleX = dx + getDialogWidth() - TOGGLE_WIDTH - 24;
                    int rowY = listY + (index * ROW_HEIGHT) - scrollY;
                    int toggleY = rowY + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;

                    if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_WIDTH &&
                        mouseY >= toggleY && mouseY < toggleY + TOGGLE_HEIGHT) {
                        entry.setter.accept(!entry.getter.get());
                        return true;
                    }
                }
            }
        }

        int buttonY = dy + getDialogHeight() - FOOTER_HEIGHT + 12;
        int buttonWidth = 100;
        int buttonHeight = 28;
        int spacing = 12;

        int themeX = dx + 16;
        if (isButtonClicked(themeX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)) {
            theme = ThemeManager.nextTheme();
            KashubConfig.getInstance().editorTheme = theme.id;
            if (onThemeChange != null) {
                onThemeChange.run();
            }
            return true;
        }

        int saveX = dx + getDialogWidth() - buttonWidth * 2 - spacing - 16;
        if (isButtonClicked(saveX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)) {
            KashubConfig.getInstance().save();
            return true;
        }

        int closeButtonX = dx + getDialogWidth() - buttonWidth - 16;
        if (isButtonClicked(closeButtonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY)) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isButtonClicked(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = getDialogHeight() - HEADER_HEIGHT - FOOTER_HEIGHT;
        int maxScroll = Math.max(0, settings.size() * ROW_HEIGHT - listHeight);
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (verticalAmount * ROW_HEIGHT)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        KashubConfig.getInstance().save();
        if (onClose != null) {
            onClose.run();
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int brighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static class SettingEntry {
        final String name;
        final String description;
        final Supplier<Boolean> getter;
        final Consumer<Boolean> setter;
        final boolean isSection;

        SettingEntry(String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.isSection = false;
        }

        SettingEntry(String name, String description, Supplier<Boolean> getter, boolean isSection) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = null;
            this.isSection = isSection;
        }
    }
}

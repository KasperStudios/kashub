package kasperstudios.kashub.gui.editor;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.dialogs.DocsDialog;
import kasperstudios.kashub.gui.dialogs.KeybindDialog;
import kasperstudios.kashub.gui.dialogs.SettingsDialog;
import kasperstudios.kashub.gui.dialogs.TaskManagerDialog;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import kasperstudios.kashub.gui.widgets.ModernButton;
import kasperstudios.kashub.gui.widgets.ModernTextArea;
import kasperstudios.kashub.gui.widgets.FilePanel;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import kasperstudios.kashub.runtime.ScriptType;
import kasperstudios.kashub.util.ScriptManager;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Modern fullscreen code editor for Kashub scripts
 * Simplified UI: No console panel, no right tabs
 * Editor takes 100% of available space (minus file panel)
 */
public class ModernEditorScreen extends Screen {
    // Layout constants
    private static final int LEFT_PANEL_WIDTH = 200;
    private static final int TOOLBAR_HEIGHT = 44;
    private static final int STATUS_BAR_HEIGHT = 26;
    
    // Widgets
    private ModernTextArea codeArea;
    private FilePanel filePanel;
    
    // State
    private String currentFile = null;
    private boolean hasUnsavedChanges = false;
    private EditorTheme theme;
    private int animationTick = 0;
    
    public ModernEditorScreen() {
        super(Text.literal("Kashub Editor"));
        this.theme = ThemeManager.getCurrentTheme();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Calculate layout - editor takes ALL space except left panel
        int editorX = LEFT_PANEL_WIDTH;
        int editorY = TOOLBAR_HEIGHT;
        int editorWidth = this.width - LEFT_PANEL_WIDTH;
        int editorHeight = this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT;
        
        // Initialize file panel (left)
        filePanel = new FilePanel(0, TOOLBAR_HEIGHT, LEFT_PANEL_WIDTH, 
            this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT, this::loadFile);
        
        // Initialize code area (takes full remaining space)
        codeArea = new ModernTextArea(
            this.textRenderer,
            editorX, editorY,
            editorWidth, editorHeight,
            theme
        );
        
        // Add toolbar buttons - icon-only layout for better fit
        int buttonY = 10;
        int buttonSize = 28; // Square icon buttons
        int buttonSpacing = 4;
        int buttonX = LEFT_PANEL_WIDTH + 12;
        
        // Run button (green accent)
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("▶"), 
            button -> runScript(), theme.accentColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Stop button (red)
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("⏹"),
            button -> stopScript(), 0xFFE74C3C));
        buttonX += buttonSize + buttonSpacing;
        
        // Save button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("💾"),
            button -> saveScript(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Keybind button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("⌨"),
            button -> openKeybindDialog(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Docs button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("📚"),
            button -> openDocs(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Tasks button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("📊"),
            button -> openTaskManager(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Settings button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("⚙"),
            button -> openSettings(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Theme button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("🎨"),
            button -> cycleTheme(), theme.buttonColor));
        buttonX += buttonSize + buttonSpacing;
        
        // Close button
        addDrawableChild(new ModernButton(buttonX, buttonY, buttonSize, 24, Text.literal("✕"),
            button -> this.close(), 0xFF666666));
        
        // Load file list
        filePanel.refreshFiles();
        
        // Load last opened file
        String lastFile = KashubConfig.getInstance().lastOpenedScript;
        if (lastFile != null && !lastFile.isEmpty()) {
            loadFile(lastFile);
        }
        
        // Log startup
        ScriptLogger.getInstance().info("Kashub Editor v" + kasperstudios.kashub.Kashub.VERSION + " ready");
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animationTick++;
        
        // Draw solid background
        context.fill(0, 0, this.width, this.height, theme.backgroundColor);
        
        // Draw left panel (file browser)
        renderLeftPanel(context, mouseX, mouseY, delta);
        
        // Draw toolbar
        renderToolbar(context, mouseX, mouseY);
        
        // Draw code area
        codeArea.render(context, mouseX, mouseY, delta);
        
        // Draw status bar
        renderStatusBar(context);
        
        // Draw separator
        renderSeparator(context);
        
        // Render buttons
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
        
        // Render file panel dialogs LAST (on top of everything)
        filePanel.renderCreateDialogOverlay(context, mouseX, mouseY);
    }
    
    private void renderLeftPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, LEFT_PANEL_WIDTH, this.height, theme.sidebarColor);
        
        // Logo header
        renderLogo(context);
        
        // File panel
        filePanel.render(context, mouseX, mouseY, delta);
    }
    
    private void renderLogo(DrawContext context) {
        int logoY = 12;
        float pulse = (float) (Math.sin(animationTick * 0.08) * 0.4 + 0.6);
        int glowAlpha = (int) (pulse * 80);
        
        // Glow effect
        int glowColor = (glowAlpha << 24) | (theme.accentColor & 0x00FFFFFF);
        context.fill(10, logoY - 4, LEFT_PANEL_WIDTH - 10, logoY + 22, glowColor);
        
        // Logo text
        context.drawText(this.textRenderer, "⚡ KASHUB", 16, logoY, theme.accentColor, true);
        context.drawText(this.textRenderer, "v" + kasperstudios.kashub.Kashub.VERSION, LEFT_PANEL_WIDTH - 40, logoY + 4, theme.textDimColor, false);
        
        // Separator
        context.fill(10, TOOLBAR_HEIGHT - 2, LEFT_PANEL_WIDTH - 10, TOOLBAR_HEIGHT - 1, theme.accentColor & 0x44FFFFFF);
    }
    
    private void renderToolbar(DrawContext context, int mouseX, int mouseY) {
        // Background
        context.fill(LEFT_PANEL_WIDTH, 0, this.width, TOOLBAR_HEIGHT, theme.toolbarColor);
    }
    
    private void renderStatusBar(DrawContext context) {
        int y = this.height - STATUS_BAR_HEIGHT;
        context.fill(0, y, this.width, this.height, theme.statusBarColor);
        
        // File info
        String fileInfo = currentFile != null ? "📄 " + currentFile : "📄 No file";
        if (hasUnsavedChanges) fileInfo += " ●";
        context.drawText(this.textRenderer, fileInfo, 12, y + 8, hasUnsavedChanges ? theme.consoleWarnColor : theme.textDimColor, false);
        
        // Cursor position
        String cursorInfo = String.format("Ln %d, Col %d", codeArea.getCurrentLine(), codeArea.getCurrentColumn());
        int cursorInfoWidth = this.textRenderer.getWidth(cursorInfo);
        context.drawText(this.textRenderer, cursorInfo, this.width - cursorInfoWidth - 12, y + 8, theme.textDimColor, false);
        
        // Error info or theme name (center)
        int errorCount = codeArea.getErrorCount();
        String currentError = codeArea.getCurrentLineError();
        
        if (currentError != null) {
            // Show current line error
            String errorInfo = "⚠ " + currentError;
            int errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            int maxWidth = this.width - 400;
            if (errorInfoWidth > maxWidth) {
                errorInfo = errorInfo.substring(0, Math.min(errorInfo.length(), 50)) + "...";
                errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            }
            context.drawText(this.textRenderer, errorInfo, (this.width - errorInfoWidth) / 2, y + 8, theme.consoleErrorColor, false);
        } else if (errorCount > 0) {
            // Show error count
            String errorInfo = "⚠ " + errorCount + " error" + (errorCount > 1 ? "s" : "");
            int errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            context.drawText(this.textRenderer, errorInfo, (this.width - errorInfoWidth) / 2, y + 8, theme.consoleWarnColor, false);
        } else {
            // Show theme name
            String themeInfo = "✓ No errors | 🎨 " + theme.name;
            int themeInfoWidth = this.textRenderer.getWidth(themeInfo);
            context.drawText(this.textRenderer, themeInfo, (this.width - themeInfoWidth) / 2, y + 8, theme.consoleSuccessColor, false);
        }
        
        // Running scripts count
        int runningCount = ScriptTaskManager.getInstance().getActiveCount();
        if (runningCount > 0) {
            String runningInfo = "▶ " + runningCount + " running";
            context.drawText(this.textRenderer, runningInfo, LEFT_PANEL_WIDTH + 12, y + 8, theme.consoleSuccessColor, false);
        }
    }
    
    private void renderSeparator(DrawContext context) {
        int sepX = LEFT_PANEL_WIDTH;
        context.fill(sepX - 1, 0, sepX, this.height, theme.accentColor & 0x33FFFFFF);
        context.fill(sepX, 0, sepX + 1, this.height, theme.accentColor & 0x66FFFFFF);
    }
    
    private void loadFile(String filename) {
        if (hasUnsavedChanges) {
            // TODO: Show save dialog
        }
        
        String content = ScriptManager.loadScript(filename);
        if (content != null) {
            currentFile = filename;
            codeArea.setText(content);
            hasUnsavedChanges = false;
            sendChatMessage("§a[KH] Loaded: " + filename);
        } else {
            sendChatMessage("§c[KH] Failed to load: " + filename);
        }
    }
    
    private void saveScript() {
        if (currentFile == null) {
            currentFile = "untitled.kh";
        }
        
        String content = codeArea.getText();
        if (ScriptManager.saveScript(currentFile, content)) {
            hasUnsavedChanges = false;
            sendChatMessage("§a[KH] Saved: " + currentFile);
            filePanel.refreshFiles();
        } else {
            sendChatMessage("§c[KH] Failed to save: " + currentFile);
        }
    }
    
    private void runScript() {
        String code = codeArea.getText();
        if (code.isEmpty()) {
            sendChatMessage("§e[KH] No code to run");
            return;
        }
        
        try {
            String name = currentFile != null ? currentFile : "untitled";
            ScriptTaskManager.getInstance().startScript(name, code, null, ScriptType.USER);
            sendChatMessage("§a[KH] Script started: " + name);
        } catch (Exception e) {
            sendChatMessage("§c[KH] Error: " + e.getMessage());
        }
    }
    
    private void stopScript() {
        ScriptTaskManager.getInstance().stopAll();
        sendChatMessage("§e[KH] All scripts stopped");
    }
    
    private void openTaskManager() {
        MinecraftClient.getInstance().setScreen(new TaskManagerDialog(this));
    }
    
    private void openDocs() {
        MinecraftClient.getInstance().setScreen(new DocsDialog(this));
    }
    
    private void openKeybindDialog() {
        if (currentFile != null && !currentFile.isEmpty()) {
            MinecraftClient.getInstance().setScreen(new KeybindDialog(this, currentFile));
        } else {
            sendChatMessage("§e[KH] Save the script first to set a keybind");
        }
    }
    
    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setOnThemeChange(this::applyThemeChange);
        dialog.setOnClose(() -> {
            // Refresh file panel when settings dialog closes (in case hideSystemScripts changed)
            if (filePanel != null) {
                filePanel.refreshFiles();
            }
        });
        MinecraftClient.getInstance().setScreen(dialog);
    }
    
    private void cycleTheme() {
        theme = ThemeManager.nextTheme();
        applyThemeChange();
    }
    
    private void applyThemeChange() {
        theme = ThemeManager.getCurrentTheme();
        codeArea.setTheme(theme);
        KashubConfig.getInstance().editorTheme = theme.id;
        KashubConfig.getInstance().save();
        
        // Reinitialize to apply new button colors
        this.clearChildren();
        this.init();
        
        sendChatMessage("§d[KH] Theme: " + theme.name);
    }
    
    private void sendChatMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Pass to file panel first (for search)
        if (filePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        // Ctrl+S to save
        if (keyCode == 83 && (modifiers & 2) != 0) {
            saveScript();
            return true;
        }
        
        // F5 to run
        if (keyCode == 294) {
            runScript();
            return true;
        }
        
        // Ctrl+N for new file
        if (keyCode == 78 && (modifiers & 2) != 0) {
            newScript();
            return true;
        }
        
        // Escape to close
        if (keyCode == 256) {
            this.close();
            return true;
        }
        
        // Pass to code area
        if (codeArea.keyPressed(keyCode, scanCode, modifiers)) {
            hasUnsavedChanges = true;
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void newScript() {
        if (hasUnsavedChanges) {
            // TODO: Show save dialog
        }
        
        currentFile = null;
        codeArea.setText("// New Kashub Script\n// Press F5 or click Run to execute\n\nprint \"Hello, World!\"\nwait 1000\nprint \"Script finished!\"\n");
        hasUnsavedChanges = false;
        sendChatMessage("§a[KH] New script created");
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        // Pass to file panel first (for search)
        if (filePanel.charTyped(chr, modifiers)) {
            return true;
        }
        
        if (codeArea.charTyped(chr, modifiers)) {
            hasUnsavedChanges = true;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check file panel
        if (filePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check code area
        if (codeArea.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Left panel
        if (mouseX < LEFT_PANEL_WIDTH) {
            return filePanel.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        
        // Shift+Scroll = horizontal scroll
        boolean shiftHeld = hasShiftDown();
        if (shiftHeld && verticalAmount != 0) {
            // Convert vertical scroll to horizontal when Shift is held
            return codeArea.mouseScrolled(mouseX, mouseY, verticalAmount, 0);
        }
        
        // Code area - pass both horizontal and vertical amounts
        return codeArea.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public void close() {
        if (currentFile != null) {
            KashubConfig.getInstance().lastOpenedScript = currentFile;
            KashubConfig.getInstance().save();
        }
        super.close();
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
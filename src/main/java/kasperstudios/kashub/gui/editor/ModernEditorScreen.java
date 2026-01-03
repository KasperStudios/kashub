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
import kasperstudios.kashub.services.runtime.ScriptTask;
import kasperstudios.kashub.services.runtime.ScriptTaskManager;
import kasperstudios.kashub.services.runtime.ScriptType;
import kasperstudios.kashub.util.ScriptManager;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.debug.*;
import net.minecraft.client.MinecraftClient;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModernEditorScreen extends Screen {

    private static final int TOOLBAR_HEIGHT = 44;
    private static final int STATUS_BAR_HEIGHT = 26;
    private static final int MIN_PANEL_WIDTH = 100;

    private int leftPanelWidth = 200;
    private int rightPanelWidth = 250;

    private boolean isResizingLeft = false;
    private boolean isResizingRight = false;
    private static final int RESIZE_HANDLE_WIDTH = 4;

    private ModernTextArea codeArea;
    private FilePanel filePanel;

    private String currentFile = null;
    private boolean hasUnsavedChanges = false;
    private EditorTheme theme;
    private int animationTick = 0;

    private boolean showDebugPanel = true;
    private int debugPollTimer = 0;
    private final List<ModernButton> debugButtons = new ArrayList<>();

    public ModernEditorScreen() {
        super(Text.literal("Kashub Editor"));
        this.theme = ThemeManager.getCurrentTheme();

        KashubConfig config = KashubConfig.getInstance();
        this.leftPanelWidth = config.editorLeftPanelWidth;
        this.rightPanelWidth = config.editorRightPanelWidth;
        this.showDebugPanel = config.editorShowDebugPanel;
    }

    @Override
    protected void init() {
        super.init();

        int editorX = leftPanelWidth;
        int editorY = TOOLBAR_HEIGHT;
        int editorWidth = this.width - leftPanelWidth - (showDebugPanel ? rightPanelWidth : 0);
        int editorHeight = this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT;

        filePanel = new FilePanel(0, TOOLBAR_HEIGHT, leftPanelWidth,
                this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT, this::loadFile);

        if (codeArea == null) {
            codeArea = new ModernTextArea(
                    this.textRenderer,
                    editorX, editorY,
                    editorWidth, editorHeight,
                    theme);
        } else {
            codeArea.setBounds(editorX, editorY, editorWidth, editorHeight);
        }

        int buttonY = 10;
        int buttonSize = 28;
        int buttonSpacing = 4;

        int buttonX = Math.max(220, leftPanelWidth + 20);

        addToolbarButton(buttonX, buttonY, buttonSize, 24, Text.literal("▶"),
                button -> runScript(), theme.accentColor, "Run Script (F5)");
        buttonX += buttonSize + buttonSpacing;

        addToolbarButton(buttonX, buttonY, buttonSize, 24, Text.literal("⏹"),
                button -> stopScript(), 0xFFE74C3C, "Stop Script (F6)");
        buttonX += buttonSize + buttonSpacing;

        buttonX += 10;

        addToolbarButton(buttonX, buttonY, buttonSize, 24, Text.literal("💾"),
                button -> saveScript(), theme.buttonColor, "Save (Ctrl+S)");
        buttonX += buttonSize + buttonSpacing;

        addToolbarButton(buttonX, buttonY, buttonSize, 24, Text.literal("⌨"),
                button -> openKeybindDialog(), theme.buttonColor, "Keybindings");
        buttonX += buttonSize + buttonSpacing;

        buttonX += 10;

        addToolbarButton(buttonX, buttonY, buttonSize, 24, Text.literal("🐞"),
                button -> toggleDebugPanel(), showDebugPanel ? theme.accentColor : theme.buttonColor,
                "Toggle Debug Panel");
        buttonX += buttonSize + buttonSpacing;

        int maxLeftButtonX = buttonX;

        int rightButtonX = this.width - 20;

        rightButtonX -= buttonSize;
        addToolbarButton(rightButtonX, buttonY, buttonSize, 24, Text.literal("✕"),
                button -> this.close(), 0xFF666666, "Close Editor");

        rightButtonX -= (buttonSize + buttonSpacing);
        addToolbarButton(rightButtonX, buttonY, buttonSize, 24, Text.literal("🎨"),
                button -> cycleTheme(), theme.buttonColor, "Cycle Theme");

        rightButtonX -= (buttonSize + buttonSpacing);
        addToolbarButton(rightButtonX, buttonY, buttonSize, 24, Text.literal("⚙"),
                button -> openSettings(), theme.buttonColor, "Settings");

        rightButtonX -= 10;

        if (rightButtonX - (buttonSize + buttonSpacing) > maxLeftButtonX + 20) {
            rightButtonX -= (buttonSize + buttonSpacing);
            addToolbarButton(rightButtonX, buttonY, buttonSize, 24, Text.literal("📊"),
                    button -> openTaskManager(), theme.buttonColor, "Task Manager");
        }

        if (rightButtonX - (buttonSize + buttonSpacing) > maxLeftButtonX + 20) {
            rightButtonX -= (buttonSize + buttonSpacing);
            addToolbarButton(rightButtonX, buttonY, buttonSize, 24, Text.literal("📚"),
                    button -> openDocs(), theme.buttonColor, "Documentation");
        }

        debugButtons.clear();
        int btnSize = 20;
        int dBtnX = this.width - 6 - btnSize;
        int dBtnY = TOOLBAR_HEIGHT + 2;

        addToolbarButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⤴"),
                button -> debugStepOut(), 0xFFE67E22, "Step Out (Shift+F11)");
        dBtnX -= (btnSize + 2);

        addToolbarButton(dBtnX, dBtnY, btnSize, 20, Text.literal("↘"),
                button -> debugStepInto(), 0xFF9B59B6, "Step Into (F11)");
        dBtnX -= (btnSize + 2);

        addToolbarButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⤵"),
                button -> debugStepOver(), 0xFF3498DB, "Step Over (F10)");
        dBtnX -= (btnSize + 2);

        addToolbarButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⏯"),
                button -> debugResume(), 0xFF2ECC71, "Resume (F8)");

        updateDebugButtonsVisibility();
    }

    private ModernButton addToolbarButton(int x, int y, int w, int h, Text message, ButtonWidget.PressAction action,
            int color, String tooltip) {
        ModernButton btn = new ModernButton(x, y, w, h, message, action, color);
        if (tooltip != null) {
            btn.setTooltip(Tooltip.of(Text.of(tooltip)));
        }
        addDrawableChild(btn);
        return btn;
    }

    private void updateDebugButtonsVisibility() {
        for (ModernButton btn : debugButtons) {
            btn.visible = showDebugPanel;
        }

        if (showDebugPanel) {
            int btnSize = 20;
            int pad = 2;

            int dBtnX = this.width - 6 - btnSize;
            int dBtnY = TOOLBAR_HEIGHT + 2;

            addDrawableChild(new ModernButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⤴"),
                    button -> debugStepOut(), 0xFFE67E22));
            dBtnX -= (btnSize + pad);

            addDrawableChild(new ModernButton(dBtnX, dBtnY, btnSize, 20, Text.literal("↘"),
                    button -> debugStepInto(), 0xFF9B59B6));
            dBtnX -= (btnSize + pad);

            addDrawableChild(new ModernButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⤵"),
                    button -> debugStepOver(), 0xFF3498DB));
            dBtnX -= (btnSize + pad);

            addDrawableChild(new ModernButton(dBtnX, dBtnY, btnSize, 20, Text.literal("⏯"),
                    button -> debugResume(), 0xFF2ECC71));
        }

        filePanel.refreshFiles();

        if (codeArea.getText().isEmpty() && currentFile == null) {
            String lastFile = KashubConfig.getInstance().lastOpenedScript;
            if (lastFile != null && !lastFile.isEmpty()) {
                loadFile(lastFile);
            }
        }

        ScriptLogger.getInstance().info("Kashub Editor " + kasperstudios.kashub.Kashub.VERSION + " ready");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animationTick++;

        int editorX = leftPanelWidth;
        int editorWidth = this.width - leftPanelWidth - (showDebugPanel ? rightPanelWidth : 0);
        int editorHeight = this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT;

        codeArea.setBounds(editorX, TOOLBAR_HEIGHT, editorWidth, editorHeight);
        filePanel.setBounds(0, TOOLBAR_HEIGHT, leftPanelWidth, editorHeight);

        context.fill(0, 0, this.width, this.height, theme.backgroundColor);

        renderLeftPanel(context, mouseX, mouseY, delta);

        renderToolbar(context, mouseX, mouseY);

        if (showDebugPanel) {
            renderDebugPanel(context, mouseX, mouseY);
        }

        codeArea.render(context, mouseX, mouseY, delta);

        renderStatusBar(context);

        renderSeparator(context);

        renderResizeHandles(context, mouseX, mouseY);

        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }

        filePanel.renderCreateDialogOverlay(context, mouseX, mouseY);
    }

    private void renderResizeHandles(DrawContext context, int mouseX, int mouseY) {

        boolean hoverLeft = Math.abs(mouseX - leftPanelWidth) <= RESIZE_HANDLE_WIDTH;
        if (hoverLeft || isResizingLeft) {
            context.fill(leftPanelWidth - 2, TOOLBAR_HEIGHT, leftPanelWidth + 2, this.height - STATUS_BAR_HEIGHT,
                    theme.accentColor & 0x88FFFFFF);
        }

        if (showDebugPanel) {
            int rightX = this.width - rightPanelWidth;
            boolean hoverRight = Math.abs(mouseX - rightX) <= RESIZE_HANDLE_WIDTH;
            if (hoverRight || isResizingRight) {
                context.fill(rightX - 2, TOOLBAR_HEIGHT, rightX + 2, this.height - STATUS_BAR_HEIGHT,
                        theme.accentColor & 0x88FFFFFF);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        debugPollTimer++;
        if (debugPollTimer >= 2) {
            debugPollTimer = 0;
            pollDebugState();
        }
    }

    private void pollDebugState() {

    }

    private void renderDebugPanel(DrawContext context, int mouseX, int mouseY) {
        int x = this.width - rightPanelWidth;
        int y = TOOLBAR_HEIGHT;
        int h = this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT;

        context.fill(x, y, this.width, y + h, theme.sidebarColor);

        context.fill(x, y, x + 1, y + h, theme.accentColor & 0x44FFFFFF);

        context.fill(x, y, this.width, y + 24, theme.toolbarColor);
        context.drawText(this.textRenderer, "VARIABLES", x + 8, y + 8, theme.textColor, false);

        int varY = y + 30;
        int lineHeight = 12;
        Map<String, String> vars = DebugManager.getInstance().getAggregateVariables();

        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (varY > y + h - 10)
                break;

            String key = entry.getKey();
            String val = entry.getValue();

            if (val != null && val.length() > 20)
                val = val.substring(0, 20) + "...";

            context.drawText(this.textRenderer, key, x + 8, varY, 0xFF99CCFF, false);
            context.drawText(this.textRenderer, "= " + val, x + 100, varY, 0xFFCCCCCC, false);

            varY += lineHeight;
        }
    }

    private void renderLeftPanel(DrawContext context, int mouseX, int mouseY, float delta) {

        context.fill(0, 0, leftPanelWidth, this.height, theme.sidebarColor);

        renderLogo(context);

        filePanel.render(context, mouseX, mouseY, delta);
    }

    private void renderLogo(DrawContext context) {
        int logoY = 12;
        float pulse = (float) (Math.sin(animationTick * 0.08) * 0.4 + 0.6);
        int glowAlpha = (int) (pulse * 80);

        int glowColor = (glowAlpha << 24) | (theme.accentColor & 0x00FFFFFF);
        context.fill(10, logoY - 4, leftPanelWidth - 10, logoY + 22, glowColor);

        context.drawText(this.textRenderer, "⚡ KASHUB", 16, logoY, theme.accentColor, true);

        String ver = kasperstudios.kashub.Kashub.VERSION;

        String verDisplay = ver.startsWith("v") ? ver : "v" + ver;
        int verWidth = this.textRenderer.getWidth(verDisplay);
        context.drawText(this.textRenderer, verDisplay, leftPanelWidth - verWidth - 10, logoY + 4,
                theme.textDimColor, false);

        context.fill(10, TOOLBAR_HEIGHT - 2, leftPanelWidth - 10, TOOLBAR_HEIGHT - 1, theme.accentColor & 0x44FFFFFF);
    }

    private void renderToolbar(DrawContext context, int mouseX, int mouseY) {

        context.fill(leftPanelWidth, 0, this.width, TOOLBAR_HEIGHT, theme.toolbarColor);

        int sepY = 6;
        int sepH = 12;
        int sepColor = theme.textDimColor & 0x44FFFFFF;

        int startX = 220;
        int btnW = 24 + 4;

        int x1 = startX + btnW * 2 + 5;
        context.fill(x1, sepY, x1 + 1, sepY + sepH, sepColor);

        int x2 = startX + btnW * 2 + 10 + btnW * 2 + 5;
        context.fill(x2, sepY, x2 + 1, sepY + sepH, sepColor);

        int x3 = this.width - 20 - btnW * 3 - 5;
        context.fill(x3, sepY, x3 + 1, sepY + sepH, sepColor);
    }

    private void renderStatusBar(DrawContext context) {
        int y = this.height - STATUS_BAR_HEIGHT;
        context.fill(0, y, this.width, this.height, theme.statusBarColor);

        String fileInfo = currentFile != null ? "📄 " + currentFile : "📄 No file";
        if (hasUnsavedChanges)
            fileInfo += " ●";
        context.drawText(this.textRenderer, fileInfo, 12, y + 8,
                hasUnsavedChanges ? theme.consoleWarnColor : theme.textDimColor, false);

        String cursorInfo = String.format("Ln %d, Col %d", codeArea.getCurrentLine(), codeArea.getCurrentColumn());
        int cursorInfoWidth = this.textRenderer.getWidth(cursorInfo);
        context.drawText(this.textRenderer, cursorInfo, this.width - cursorInfoWidth - 12, y + 8, theme.textDimColor,
                false);

        int errorCount = codeArea.getErrorCount();
        String currentError = codeArea.getCurrentLineError();

        if (currentError != null) {

            String errorInfo = "⚠ " + currentError;
            int errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            int maxWidth = this.width - 400;
            if (errorInfoWidth > maxWidth) {
                errorInfo = errorInfo.substring(0, Math.min(errorInfo.length(), 50)) + "...";
                errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            }
            context.drawText(this.textRenderer, errorInfo, (this.width - errorInfoWidth) / 2, y + 8,
                    theme.consoleErrorColor, false);
        } else if (errorCount > 0) {

            String errorInfo = "⚠ " + errorCount + " error" + (errorCount > 1 ? "s" : "");
            int errorInfoWidth = this.textRenderer.getWidth(errorInfo);
            context.drawText(this.textRenderer, errorInfo, (this.width - errorInfoWidth) / 2, y + 8,
                    theme.consoleWarnColor, false);
        } else {

            String themeInfo = "✓ No errors | 🎨 " + theme.name;
            int themeInfoWidth = this.textRenderer.getWidth(themeInfo);
            context.drawText(this.textRenderer, themeInfo, (this.width - themeInfoWidth) / 2, y + 8,
                    theme.consoleSuccessColor, false);
        }

        int runningCount = ScriptTaskManager.getInstance().getActiveCount();
        if (runningCount > 0) {
            String runningInfo = "▶ " + runningCount + " running";
            context.drawText(this.textRenderer, runningInfo, leftPanelWidth + 12, y + 8, theme.consoleSuccessColor,
                    false);
        }

    }

    private void renderSeparator(DrawContext context) {
        int sepX = leftPanelWidth;
        context.fill(sepX - 1, 0, sepX, this.height, theme.accentColor & 0x33FFFFFF);
        context.fill(sepX, 0, sepX + 1, this.height, theme.accentColor & 0x66FFFFFF);
    }

    private void loadFile(String filename) {
        if (hasUnsavedChanges) {
            sendChatMessage("§e[KH] Warning: Discarding unsaved changes");
        }

        String content = ScriptManager.loadScript(filename);
        if (content != null) {
            currentFile = filename;
            codeArea.setText(content);
            codeArea.setCurrentFile(filename);
            hasUnsavedChanges = false;
            sendChatMessage("§a[KH] Loaded: " + filename);
        } else {
            sendChatMessage("§c[KH] Failed to load: " + filename);
        }
    }

    private void saveScript() {
        if (currentFile == null) {
            currentFile = "untitled.kh";
            codeArea.setCurrentFile(currentFile);
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

        if (filePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == 83 && (modifiers & 2) != 0) {
            saveScript();
            return true;
        }

        if (keyCode == 294) {
            runScript();
            return true;
        }

        if (keyCode == 78 && (modifiers & 2) != 0) {
            newScript();
            return true;
        }

        if (keyCode == 256) {
            this.close();
            return true;
        }

        if (codeArea.keyPressed(keyCode, scanCode, modifiers)) {
            hasUnsavedChanges = true;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void newScript() {
        if (hasUnsavedChanges) {
            sendChatMessage("§e[KH] Warning: Discarding unsaved changes");
        }

        currentFile = null;
        codeArea.setCurrentFile(null);
        codeArea.setText(
                "// New Kashub Script\n// Press F5 or click Run to execute\n\nprint \"Hello, World!\"\nwait 1000\nprint \"Script finished!\"\n");
        hasUnsavedChanges = false;
        sendChatMessage("§a[KH] New script created");
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {

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
        if (button == 0) {

            if (Math.abs(mouseX - leftPanelWidth) <= RESIZE_HANDLE_WIDTH && mouseY > TOOLBAR_HEIGHT
                    && mouseY < height - STATUS_BAR_HEIGHT) {
                isResizingLeft = true;
                return true;
            }
            if (showDebugPanel) {
                int rightX = this.width - rightPanelWidth;
                if (Math.abs(mouseX - rightX) <= RESIZE_HANDLE_WIDTH && mouseY > TOOLBAR_HEIGHT
                        && mouseY < height - STATUS_BAR_HEIGHT) {
                    isResizingRight = true;
                    return true;
                }
            }
        }

        if (filePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (codeArea.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isResizingLeft = false;
            isResizingRight = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isResizingLeft) {
            leftPanelWidth = (int) Math.max(MIN_PANEL_WIDTH, Math.min(mouseX, width - 100));
        } else if (isResizingRight) {
            rightPanelWidth = (int) Math.max(MIN_PANEL_WIDTH, Math.min(width - mouseX, width - 100));
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isResizingLeft) {
            leftPanelWidth = (int) Math.max(MIN_PANEL_WIDTH, Math.min(mouseX, width - 100));
            return true;
        }
        if (isResizingRight) {
            rightPanelWidth = (int) Math.max(MIN_PANEL_WIDTH, Math.min(width - mouseX, width - 100));
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

        if (mouseX < leftPanelWidth) {
            return filePanel.mouseScrolled(mouseX, mouseY, verticalAmount);
        }

        boolean shiftHeld = hasShiftDown();
        if (shiftHeld && verticalAmount != 0) {

            return codeArea.mouseScrolled(mouseX, mouseY, verticalAmount, 0);
        }

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

    private void toggleDebugPanel() {
        showDebugPanel = !showDebugPanel;

        this.clearChildren();
        this.init();
    }

    private void debugResume() {
        System.err.println("DEBUG SCREEN: debugResume() called");
        DebugManager.getInstance().resumeAll();
        codeArea.setExecutionLine(-1);
    }

    private void debugStepOver() {
        System.err.println("DEBUG SCREEN: debugStepOver() called");
        ScriptTaskManager manager = ScriptTaskManager.getInstance();
        for (ScriptTask task : manager.getAllTasks()) {
            if (DebugManager.getInstance().isPaused(task.getId())) {
                System.err.println("DEBUG SCREEN: Found paused task " + task.getId() + ", calling stepOver");
                DebugManager.getInstance().stepOver(task.getId());
                return;
            }
        }
        System.err.println("DEBUG SCREEN: No paused tasks found for stepOver");
    }

    private void debugStepInto() {
        System.err.println("DEBUG SCREEN: debugStepInto() called");
        ScriptTaskManager manager = ScriptTaskManager.getInstance();
        for (ScriptTask task : manager.getAllTasks()) {
            if (DebugManager.getInstance().isPaused(task.getId())) {
                System.err.println("DEBUG SCREEN: Found paused task " + task.getId() + ", calling stepInto");
                DebugManager.getInstance().stepInto(task.getId());
                return;
            }
        }
        System.err.println("DEBUG SCREEN: No paused tasks found for stepInto");
    }

    private void debugStepOut() {
        System.err.println("DEBUG SCREEN: debugStepOut() called");

        debugResume();
    }

}
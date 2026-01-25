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
import kasperstudios.kashub.gui.widgets.TabMenuWidget;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Type;
import kasperstudios.kashub.core.State;
import kasperstudios.kashub.util.ScriptManager;
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

    // Static tracking for Discord RPC
    private static String activeEditorFile = null;
    private static boolean editorOpen = false;

    private ModernTextArea codeArea;
    private FilePanel filePanel;
    private TabMenuWidget tabMenu;

    private String currentFile = null;
    private boolean hasUnsavedChanges = false;
    private EditorTheme theme;
    private int animationTick = 0;
    private int debugPollTimer = 0;

    private boolean showDebugPanel = true;
    private final List<ModernButton> leftButtons = new ArrayList<>();
    private final List<ModernButton> rightButtons = new ArrayList<>();
    private final List<ModernButton> debugControlButtons = new ArrayList<>();

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
        leftButtons.clear();
        rightButtons.clear();
        debugControlButtons.clear();

        // Refresh completions to ensure new objects are detected
        kasperstudios.kashub.gui.CodeCompletionManager.refreshCompletions();
        kasperstudios.kashub.gui.widgets.ModernTextArea.refreshKnownObjects();

        editorOpen = true;
        activeEditorFile = currentFile;

        // Tab menu above file panel
        int tabMenuY = TOOLBAR_HEIGHT;
        int tabMenuHeight = 28;

        List<TabMenuWidget.Tab> tabs = new java.util.ArrayList<>();
        tabs.add(new TabMenuWidget.Tab(new net.minecraft.item.ItemStack(net.minecraft.item.Items.FILLED_MAP),
                Text.literal("Files")));
        tabs.add(new TabMenuWidget.Tab(new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD),
                Text.literal("Marketplace 😈")));

        tabMenu = new TabMenuWidget(
                0, tabMenuY, leftPanelWidth, tabMenuHeight,
                tabs,
                (newTab) -> {
                    // Future: switch between Files and Marketplace views
                });
        tabMenu.setActiveTab(0); // Files active
        tabMenu.setTabEnabled(1, false); // Marketplace disabled
        tabMenu.setTabTooltip(1, Text.literal("Coming soon...😈"));
        addDrawableChild(tabMenu);

        int editorX = leftPanelWidth;
        int editorY = TOOLBAR_HEIGHT;
        int editorWidth = this.width - leftPanelWidth - (showDebugPanel ? rightPanelWidth : 0);
        int editorHeight = this.height - TOOLBAR_HEIGHT - STATUS_BAR_HEIGHT;

        // File panel below tab menu
        int filePanelY = tabMenuY + tabMenuHeight;
        int filePanelHeight = this.height - filePanelY - STATUS_BAR_HEIGHT;
        filePanel = new FilePanel(0, filePanelY, leftPanelWidth, filePanelHeight, this::loadFile);

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

        // Left Buttons
        leftButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("▶"),
                button -> runScript(), theme.accentColor, "Run Script (F5)"));
        leftButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("⏹"),
                button -> stopScript(), 0xFFE74C3C, "Stop Script (F6)"));
        leftButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("💾"),
                button -> saveScript(), theme.buttonColor, "Save (Ctrl+S)"));
        leftButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("⌨"),
                button -> openKeybindDialog(), theme.buttonColor, "Keybindings"));
        leftButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("🐞"),
                button -> toggleDebugPanel(), showDebugPanel ? theme.accentColor : theme.buttonColor,
                "Toggle Debug Panel"));

        // Right Buttons
        rightButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("✕"),
                button -> this.close(), 0xFF666666, "Close Editor"));
        rightButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("🎨"),
                button -> cycleTheme(), theme.buttonColor, "Cycle Theme"));
        rightButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("⚙"),
                button -> openSettings(), theme.buttonColor, "Settings"));
        rightButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("📊"),
                button -> openTaskManager(), theme.buttonColor, "Task Manager"));
        rightButtons.add(addToolbarButton(0, buttonY, buttonSize, 24, Text.literal("📚"),
                button -> openDocs(), theme.buttonColor, "Documentation"));

        kasperstudios.kashub.util.RemoteScriptManager.getInstance().fetchMetadata(
                commands -> kasperstudios.kashub.gui.CodeCompletionManager.setServerCommands(commands));

        // Debug Control Buttons
        int btnSize = 20;
        debugControlButtons.add(addToolbarButton(0, 0, btnSize, 20, Text.literal("⤴"),
                button -> debugStepOut(), 0xFFE67E22, "Step Out (Shift+F11)"));
        debugControlButtons.add(addToolbarButton(0, 0, btnSize, 20, Text.literal("↘"),
                button -> debugStepInto(), 0xFF9B59B6, "Step Into (F11)"));
        debugControlButtons.add(addToolbarButton(0, 0, btnSize, 20, Text.literal("⤵"),
                button -> debugStepOver(), 0xFF3498DB, "Step Over (F10)"));
        debugControlButtons.add(addToolbarButton(0, 0, btnSize, 20, Text.literal("⏯"),
                button -> debugResume(), 0xFF2ECC71, "Resume (F8)"));

        updateLayout();
        updateDebugButtonsVisibility();

        filePanel.refreshFiles();

        if (codeArea.getText().isEmpty() && currentFile == null) {
            String lastFile = KashubConfig.getInstance().lastOpenedScript;
            if (lastFile != null && !lastFile.isEmpty()) {
                loadFile(lastFile);
            }
        }
    }

    private void updateLayout() {
        int buttonY = 10;
        int buttonSize = 28;
        int buttonSpacing = 4;

        // Left Buttons
        int leftX = Math.max(220, leftPanelWidth + 20);
        for (ModernButton btn : leftButtons) {
            btn.setX(leftX);
            leftX += buttonSize + buttonSpacing;
            if (leftButtons.indexOf(btn) == 2 || leftButtons.indexOf(btn) == 3)
                leftX += 10; // Extra spacing
        }

        // Right Buttons
        // Order: Close, Theme, Settings, Task, Docs
        int rightX = this.width - 20;

        // Close
        if (rightButtons.size() > 0) {
            rightX -= buttonSize;
            rightButtons.get(0).setX(rightX);
            rightX -= (buttonSize + buttonSpacing);
        }

        // Theme
        if (rightButtons.size() > 1) {
            rightButtons.get(1).setX(rightX);
            rightX -= (buttonSize + buttonSpacing);
        }

        // Settings
        if (rightButtons.size() > 2) {
            rightButtons.get(2).setX(rightX);
            rightX -= 10; // Extra spacer
        }

        // Task & Docs (Conditional fit)
        int maxLeftButtonX = leftX;

        if (rightButtons.size() > 3) { // Task
            if (rightX - (buttonSize + buttonSpacing) > maxLeftButtonX + 20) {
                rightX -= (buttonSize + buttonSpacing);
                rightButtons.get(3).visible = true;
                rightButtons.get(3).setX(rightX);
            } else {
                rightButtons.get(3).visible = false;
            }
        }

        if (rightButtons.size() > 4) { // Docs
            if (rightX - (buttonSize + buttonSpacing) > maxLeftButtonX + 20) {
                rightX -= (buttonSize + buttonSpacing);
                rightButtons.get(4).visible = true;
                rightButtons.get(4).setX(rightX);
            } else {
                rightButtons.get(4).visible = false;
            }
        }

        // Debug Controls
        if (showDebugPanel) {
            int dBtnSize = 20;
            int dBtnX = this.width - 6 - dBtnSize;
            int dBtnY = TOOLBAR_HEIGHT + 2;
            int pad = 2;

            // Resume (Last added, first from right)
            // List order: StepOut, StepInto, StepOver, Resume
            // Wanted visual order Right to Left: Resume, StepOver, StepInto, StepOut

            // Index 3: Resume
            if (debugControlButtons.size() > 3) {
                debugControlButtons.get(3).setX(dBtnX);
                dBtnX -= (dBtnSize + pad);
            }
            // Index 2: StepOver
            if (debugControlButtons.size() > 2) {
                debugControlButtons.get(2).setX(dBtnX);
                dBtnX -= (dBtnSize + pad);
            }
            // Index 1: StepInto
            if (debugControlButtons.size() > 1) {
                debugControlButtons.get(1).setX(dBtnX);
                dBtnX -= (dBtnSize + pad);
            }
            // Index 0: StepOut
            if (debugControlButtons.size() > 0) {
                debugControlButtons.get(0).setX(dBtnX);
            }
        }
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
        for (ModernButton btn : debugControlButtons) {
            btn.visible = showDebugPanel;
        }
        // No duplication logic anymore
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animationTick++;
        updateLayout(); // Dynamic layout update

        // Handle tab menu tooltip
        if (tabMenu != null) {
            Text tooltip = tabMenu.getHoveredTooltip(mouseX, mouseY);
            if (tooltip != null) {
                this.setTooltip(tooltip);
            }
        }

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

        // Server Mode Indicator
        if (kasperstudios.kashub.network.ServerModeManager.getInstance().isServerControlled()) {
            String modeInfo = "Mode: Server config (Managed by Server)";
            context.drawText(this.textRenderer, modeInfo, 12, y + 8, theme.consoleWarnColor, false);
        } else {
            String fileInfo = currentFile != null ? "📄 " + currentFile : "📄 No file";
            if (hasUnsavedChanges)
                fileInfo += " ●";
            context.drawText(this.textRenderer, fileInfo, 12, y + 8,
                    hasUnsavedChanges ? theme.consoleWarnColor : theme.textDimColor, false);
        }

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

        int runningCount = TaskManager.getInstance().getActiveCount();
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

        String content = null;
        boolean isRemoteScript = filename.startsWith("[REMOTE] ");
        boolean isModpackScript = filename.startsWith("[MODPACK] ") || filename.startsWith("[SERVER] "); // Backwards
                                                                                                         // compat

        if (isRemoteScript) {
            String scriptName = filename.substring(9);
            sendChatMessage("§e[KH] Loading remote script...");
            kasperstudios.kashub.util.RemoteScriptManager.getInstance().fetchContent(scriptName, (loadedContent) -> {
                if (loadedContent != null) {
                    currentFile = filename;
                    activeEditorFile = currentFile;
                    codeArea.setText(loadedContent);
                    codeArea.setCurrentFile(currentFile);
                    hasUnsavedChanges = false;

                    // Enforce server rules on editability
                    boolean isOperator = kasperstudios.kashub.util.ScriptManager.isOperator();
                    codeArea.setEditable(isOperator);

                    sendChatMessage("§a[KH] Loaded remote: " + scriptName);
                } else {
                    sendChatMessage("§c[KH] Failed to fetch remote script: " + scriptName);
                }
            });
            return; // Async return
        }

        if (isModpackScript) {
            String prefix = filename.startsWith("[MODPACK] ") ? "[MODPACK] " : "[SERVER] ";
            String scriptName = filename.substring(prefix.length());
            content = ScriptManager.loadServerScript(scriptName);
            currentFile = "[MODPACK] " + scriptName;
        } else {
            content = ScriptManager.loadScript(filename);
            currentFile = filename;
        }

        if (content != null) {
            activeEditorFile = currentFile;
            codeArea.setText(content);
            codeArea.setCurrentFile(currentFile);
            hasUnsavedChanges = false;

            // Modpack scripts are editable only by operators
            if (isModpackScript) {
                codeArea.setEditable(kasperstudios.kashub.util.ScriptManager.isOperator());
            } else {
                codeArea.setEditable(true); // User scripts always editable
            }

            sendChatMessage("§a[KH] Loaded: " + currentFile);
        } else {
            sendChatMessage("§c[KH] Failed to load: " + filename);
        }
    }

    private void saveScript() {
        if (!kasperstudios.kashub.network.ServerModeManager.getInstance().isEditorAllowed()) {
            sendChatMessage("§c[KH] Saving scripts is disabled by server");
            return;
        }

        if (currentFile == null) {
            currentFile = "untitled.kh";
            codeArea.setCurrentFile(currentFile);
        }

        String content = codeArea.getText();
        boolean success = false;

        boolean isRemoteScript = currentFile.startsWith("[REMOTE] ");
        boolean isModpackScript = currentFile.startsWith("[MODPACK] ") || currentFile.startsWith("[SERVER] ");

        if (isRemoteScript) {
            String scriptName = currentFile.substring(9);
            sendChatMessage("§e[KH] Saving remote script...");
            kasperstudios.kashub.util.RemoteScriptManager.getInstance().saveScript(scriptName, content, (response) -> {
                boolean ok = response != null && !response.startsWith("Error");
                if (ok) {
                    hasUnsavedChanges = false;
                    sendChatMessage("§a[KH] Saved remote: " + scriptName);
                    filePanel.refreshFiles();
                } else {
                    sendChatMessage("§c[KH] Failed to save remote script: " + response);
                }
            });
            return;
        }

        if (isModpackScript) {
            String prefix = currentFile.startsWith("[MODPACK] ") ? "[MODPACK] " : "[SERVER] ";
            String scriptName = currentFile.substring(prefix.length());
            if (!ScriptManager.isOperator()) {
                sendChatMessage("§c[KH] Only operators can save modpack scripts!");
                return;
            }
            success = ScriptManager.saveServerScript(scriptName, content);
        } else {
            success = ScriptManager.saveScript(currentFile, content);
        }

        if (success) {
            hasUnsavedChanges = false;
            sendChatMessage("§a[KH] Saved: " + currentFile);
            filePanel.refreshFiles();
        } else {
            sendChatMessage("§c[KH] Failed to save: " + currentFile);
        }
    }

    private void runScript() {
        if (!kasperstudios.kashub.network.ServerModeManager.getInstance().isEditorAllowed()) {
            sendChatMessage("§c[KH] Running scripts is disabled by server");
            return;
        }

        String code = codeArea.getText();
        if (code.isEmpty()) {
            sendChatMessage("§e[KH] No code to run");
            return;
        }

        try {
            String name = currentFile != null ? currentFile : "untitled";
            TaskManager.getInstance().startScript(name, code, null, Type.USER);
            sendChatMessage("§a[KH] Script started: " + name);
        } catch (Exception e) {
            sendChatMessage("§c[KH] Error: " + e.getMessage());
        }
    }

    private void stopScript() {
        TaskManager.getInstance().stopAll();
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
        if (kasperstudios.kashub.network.ServerModeManager.getInstance().isServerControlled()) {
            sendChatMessage("§c[KH] Settings are managed by server (Read-only)");
            return;
        }

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
        editorOpen = false;
        activeEditorFile = null;
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
        TaskManager manager = TaskManager.getInstance();
        for (Task task : manager.getAllTasks()) {
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
        TaskManager manager = TaskManager.getInstance();
        for (Task task : manager.getAllTasks()) {
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

    // Static getters for Discord RPC
    public static boolean isEditorOpen() {
        return editorOpen;
    }

    public static String getActiveFile() {
        return activeEditorFile;
    }

}
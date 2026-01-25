package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import kasperstudios.kashub.util.ScriptManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FilePanel {
    private int x, y, width, height;
    private EditorTheme theme;
    private final TextRenderer textRenderer;

    private List<String> systemScripts = new ArrayList<>();
    private List<String> userScripts = new ArrayList<>();
    private List<String> modpackScripts = new ArrayList<>();
    private List<String> remoteScripts = new ArrayList<>();
    private List<String> filteredScripts = new ArrayList<>();

    private String searchQuery = "";
    private String selectedFile = null;
    private int hoveredIndex = -1;
    private int scrollY = 0;

    private Consumer<String> onFileSelect;

    private static final int SEARCH_HEIGHT = 28;
    private static final int CREATE_BUTTON_HEIGHT = 32;
    private static final int HEADER_HEIGHT = 24;
    private static final int ITEM_HEIGHT = 26;

    private boolean showCreateDialog = false;
    private boolean isCreatingModpackScript = false;
    private boolean isCreatingRemoteScript = false;
    private String newFileName = "";
    private boolean createDialogFocused = false;

    private boolean searchFocused = false;

    public FilePanel(int x, int y, int width, int height, Consumer<String> onFileSelect) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = ThemeManager.getCurrentTheme();
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.onFileSelect = onFileSelect;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void refreshFiles() {
        kasperstudios.kashub.config.KashubConfig config = kasperstudios.kashub.config.KashubConfig.getInstance();
        if (config.hideSystemScripts) {
            systemScripts = new ArrayList<>();
        } else {
            systemScripts = ScriptManager.getSystemScripts();
        }
        userScripts = ScriptManager.getUserScripts();

        // Load modpack scripts (locally stored server scripts) if player is operator
        if (ScriptManager.isOperator()) {
            modpackScripts = ScriptManager.getServerScripts();
        } else {
            modpackScripts = new ArrayList<>();
        }

        // Fetch remote scripts (from network) if player is operator
        if (ScriptManager.isOperator()) {
            this.applyFilter(); // Show local scripts immediately
            kasperstudios.kashub.util.RemoteScriptManager.getInstance().fetchList(list -> {
                this.remoteScripts = list != null ? list : new ArrayList<>();
                this.applyFilter(); // Update with remote scripts when ready
            });
        } else {
            this.remoteScripts = new ArrayList<>();
            applyFilter();
        }
    }

    private void applyFilter() {
        filteredScripts.clear();
        String query = searchQuery.toLowerCase();

        // Remote scripts first
        for (String script : remoteScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add("[REMOTE] " + script);
            }
        }

        // Modpack scripts second
        for (String script : modpackScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add("[MODPACK] " + script);
            }
        }

        // Then system scripts
        for (String script : systemScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add("[SYS] " + script);
            }
        }

        // Then user scripts
        for (String script : userScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add(script);
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        theme = ThemeManager.getCurrentTheme();

        renderSearchBar(context, mouseX, mouseY);

        renderFileList(context, mouseX, mouseY, delta);

        renderCreateButton(context, mouseX, mouseY);

    }

    public void renderCreateDialogOverlay(DrawContext context, int mouseX, int mouseY) {
        if (showCreateDialog) {
            renderCreateDialog(context, mouseX, mouseY);
        }
    }

    public boolean isCreateDialogShowing() {
        return showCreateDialog;
    }

    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        int searchY = y;

        context.fill(x, searchY, x + width, searchY + SEARCH_HEIGHT, adjustBrightness(theme.sidebarColor, 5));

        context.drawText(textRenderer, "🔍", x + 8, searchY + 9, theme.textDimColor, false);

        int inputX = x + 24;
        int inputWidth = width - 32;
        int bgColor = searchFocused ? adjustBrightness(theme.backgroundColor, 10) : theme.backgroundColor;
        context.fill(inputX, searchY + 4, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4, bgColor);

        if (searchFocused) {
            context.fill(inputX, searchY + 4, inputX + inputWidth, searchY + 5, theme.accentColor);
            context.fill(inputX, searchY + SEARCH_HEIGHT - 5, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4,
                    theme.accentColor);
            context.fill(inputX, searchY + 4, inputX + 1, searchY + SEARCH_HEIGHT - 4, theme.accentColor);
            context.fill(inputX + inputWidth - 1, searchY + 4, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4,
                    theme.accentColor);
        }

        String displayText = searchQuery.isEmpty() ? "Search scripts..." : searchQuery;
        int textColor = searchQuery.isEmpty() ? theme.textDimColor : theme.textColor;
        context.drawText(textRenderer, displayText, inputX + 6, searchY + 9, textColor, false);

        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = inputX + 6 + textRenderer.getWidth(searchQuery);
            context.fill(cursorX, searchY + 7, cursorX + 1, searchY + SEARCH_HEIGHT - 7, theme.textColor);
        }

        if (!searchQuery.isEmpty()) {
            int clearX = inputX + inputWidth - 16;
            boolean clearHovered = mouseX >= clearX && mouseX < clearX + 12 && mouseY >= searchY + 4
                    && mouseY < searchY + SEARCH_HEIGHT - 4;
            context.drawText(textRenderer, "✕", clearX, searchY + 9,
                    clearHovered ? theme.consoleErrorColor : theme.textDimColor, false);
        }
    }

    private void renderFileList(DrawContext context, int mouseX, int mouseY, float delta) {
        int listY = y + SEARCH_HEIGHT;
        int listHeight = height - SEARCH_HEIGHT - CREATE_BUTTON_HEIGHT;

        hoveredIndex = -1;

        int currentY = listY - scrollY;

        // Remote scripts header
        if (!remoteScripts.isEmpty()
                && (searchQuery.isEmpty() || filteredScripts.stream().anyMatch(s -> s.startsWith("[REMOTE]")))) {
            if (currentY >= listY - HEADER_HEIGHT && currentY < listY + listHeight) {
                context.fill(x, Math.max(listY, currentY), x + width,
                        Math.min(listY + listHeight, currentY + HEADER_HEIGHT),
                        adjustBrightness(theme.sidebarColor, 8));
                context.drawText(textRenderer, "🌐 REMOTE", x + 10, currentY + 7, theme.accentColor, true);
            }
            currentY += HEADER_HEIGHT;
        }

        // Modpack scripts header
        if (!modpackScripts.isEmpty()
                && (searchQuery.isEmpty() || filteredScripts.stream().anyMatch(s -> s.startsWith("[MODPACK]")))) {
            if (currentY >= listY - HEADER_HEIGHT && currentY < listY + listHeight) {
                context.fill(x, Math.max(listY, currentY), x + width,
                        Math.min(listY + listHeight, currentY + HEADER_HEIGHT),
                        adjustBrightness(theme.sidebarColor, 8));
                context.drawText(textRenderer, "📦 MODPACK", x + 10, currentY + 7, theme.accentColor, true);
            }
            currentY += HEADER_HEIGHT;
        }

        if (!systemScripts.isEmpty()
                && (searchQuery.isEmpty() || filteredScripts.stream().anyMatch(s -> s.startsWith("[SYS]")))) {
            if (currentY >= listY - HEADER_HEIGHT && currentY < listY + listHeight) {
                context.fill(x, Math.max(listY, currentY), x + width,
                        Math.min(listY + listHeight, currentY + HEADER_HEIGHT),
                        adjustBrightness(theme.sidebarColor, 8));
                context.drawText(textRenderer, "📁 SYSTEM", x + 10, currentY + 7, theme.accentColor, true);
            }
            currentY += HEADER_HEIGHT;
        }

        int index = 0;
        for (String script : filteredScripts) {
            if (currentY >= listY - ITEM_HEIGHT && currentY < listY + listHeight) {
                boolean isRemote = script.startsWith("[REMOTE] ");
                boolean isModpack = script.startsWith("[MODPACK] ");
                boolean isSystem = script.startsWith("[SYS] ");

                String displayName;
                if (isRemote)
                    displayName = script.substring(9);
                else if (isModpack)
                    displayName = script.substring(10);
                else if (isSystem)
                    displayName = script.substring(6);
                else
                    displayName = script;

                String actualName = script;

                // Add USER SCRIPTS header when transitioning from system (or last active
                // category) to user
                if (!isSystem && !isRemote && !isModpack && index > 0) {
                    String prev = filteredScripts.get(index - 1);
                    if (prev.startsWith("[SYS]") || prev.startsWith("[MODPACK]") || prev.startsWith("[REMOTE]")) {
                        context.fill(x, Math.max(listY, currentY), x + width,
                                Math.min(listY + listHeight, currentY + HEADER_HEIGHT),
                                adjustBrightness(theme.sidebarColor, 8));
                        context.drawText(textRenderer, "📂 USER SCRIPTS", x + 10, currentY + 7, theme.accentColor,
                                true);
                        currentY += HEADER_HEIGHT;
                    }
                }

                if (mouseX >= x && mouseX < x + width && mouseY >= currentY && mouseY < currentY + ITEM_HEIGHT
                        && mouseY >= listY && mouseY < listY + listHeight) {
                    hoveredIndex = index;
                }

                boolean isSelected = actualName.equals(selectedFile);
                boolean isHovered = hoveredIndex == index;

                if (isSelected) {
                    context.fill(x + 4, currentY + 2, x + width - 4, currentY + ITEM_HEIGHT - 2,
                            theme.accentColor & 0x44FFFFFF);
                } else if (isHovered) {
                    context.fill(x + 4, currentY + 2, x + width - 4, currentY + ITEM_HEIGHT - 2,
                            theme.buttonHoverColor);
                }

                boolean isInFolder = displayName.contains("/");
                String icon = isRemote ? "🌐" : (isModpack ? "📦" : (isSystem ? "📜" : (isInFolder ? "📁" : "📄")));
                int iconColor = isRemote ? theme.accentColor : (isModpack ? theme.variableColor : theme.textDimColor);
                context.drawText(textRenderer, icon, x + 12, currentY + 8, iconColor, false);

                String name = displayName;
                if (isInFolder) {

                    int lastSlash = name.lastIndexOf('/');
                    String folder = name.substring(0, lastSlash);
                    String scriptName = name.substring(lastSlash + 1);
                    name = folder + "/" + scriptName;
                }
                if (name.length() > 20) {
                    name = name.substring(0, 17) + "...";
                }
                int nameColor = isSelected ? theme.accentColor : (isHovered ? theme.textColor : theme.textDimColor);
                context.drawText(textRenderer, name, x + 28, currentY + 8, nameColor, isSelected);
            }
            currentY += ITEM_HEIGHT;
            index++;
        }

        if (filteredScripts.isEmpty()) {
            String emptyText = searchQuery.isEmpty() ? "No scripts found" : "No matches for \"" + searchQuery + "\"";
            int textWidth = textRenderer.getWidth(emptyText);
            context.drawText(textRenderer, emptyText, x + (width - textWidth) / 2, listY + 40, theme.textDimColor,
                    false);
        }

        int totalHeight = filteredScripts.size() * ITEM_HEIGHT + HEADER_HEIGHT * 2;
        if (totalHeight > listHeight) {
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = listY + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);

            context.fill(x + width - 4, listY, x + width, listY + listHeight, 0x22FFFFFF);
            context.fill(x + width - 4, scrollbarY, x + width, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }

    private void renderCreateButton(DrawContext context, int mouseX, int mouseY) {
        int btnY = y + height - CREATE_BUTTON_HEIGHT;
        boolean isOperator = ScriptManager.isOperator();

        if (isOperator) {
            // Three buttons for operators: User, Modpack, Remote
            int btnWidth = (width - 20) / 3;

            // User
            renderButton(context, x + 5, btnY + 4, btnWidth - 2, CREATE_BUTTON_HEIGHT - 4, "User",
                    mouseX, mouseY, () -> {
                        showCreateDialog = true;
                        isCreatingModpackScript = false;
                        isCreatingRemoteScript = false;
                        createDialogFocused = true;
                    });

            // Modpack
            renderButton(context, x + 5 + btnWidth, btnY + 4, btnWidth - 2, CREATE_BUTTON_HEIGHT - 4, "Pack",
                    mouseX, mouseY, () -> {
                        showCreateDialog = true;
                        isCreatingModpackScript = true;
                        isCreatingRemoteScript = false;
                        createDialogFocused = true;
                    });

            // Remote
            renderButton(context, x + 5 + btnWidth * 2, btnY + 4, btnWidth - 2, CREATE_BUTTON_HEIGHT - 4, "Net",
                    mouseX, mouseY, () -> {
                        showCreateDialog = true;
                        isCreatingModpackScript = false;
                        isCreatingRemoteScript = true;
                        createDialogFocused = true;
                    });
        } else {
            // Single button
            renderButton(context, x + 10, btnY + 4, width - 20, CREATE_BUTTON_HEIGHT - 4, "+ New Script",
                    mouseX, mouseY, () -> {
                        showCreateDialog = true;
                        isCreatingModpackScript = false;
                        isCreatingRemoteScript = false;
                        createDialogFocused = true;
                    });
        }
    }

    // Helper for rendering small buttons
    private void renderButton(DrawContext context, int bx, int by, int bw, int bh, String text, int mx, int my,
            Runnable action) {
        boolean hovered = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        int color = hovered ? theme.accentColor : theme.buttonColor;
        context.fill(bx, by, bx + bw, by + bh, color);
        int tw = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, bx + (bw - tw) / 2, by + (bh - 8) / 2, 0xFFFFFFFF, true);

        if (hovered && net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            // Optional: add tooltips if needed
        }
    }

    private void renderCreateDialog(DrawContext context, int mouseX, int mouseY) {

        MinecraftClient client = MinecraftClient.getInstance();
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), 0xAA000000);

        int dialogWidth = 300;
        int dialogHeight = 120;
        int dialogX = (client.getWindow().getScaledWidth() - dialogWidth) / 2;
        int dialogY = (client.getWindow().getScaledHeight() - dialogHeight) / 2;

        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, theme.sidebarColor);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, theme.accentColor);
        context.fill(dialogX, dialogY + dialogHeight - 1, dialogX + dialogWidth, dialogY + dialogHeight,
                theme.accentColor);
        context.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogHeight, theme.accentColor);
        context.fill(dialogX + dialogWidth - 1, dialogY, dialogX + dialogWidth, dialogY + dialogHeight,
                theme.accentColor);

        String typeStr = isCreatingRemoteScript ? "Remote" : (isCreatingModpackScript ? "Modpack" : "User");
        String title = "Create New " + typeStr + " Script";
        context.drawText(textRenderer, title, dialogX + 15, dialogY + 15, theme.accentColor, true);

        int inputY = dialogY + 40;
        context.fill(dialogX + 15, inputY, dialogX + dialogWidth - 15, inputY + 24, theme.backgroundColor);

        String displayText = newFileName.isEmpty() ? "script_name.kh" : newFileName;
        int textColor = newFileName.isEmpty() ? theme.textDimColor : theme.textColor;
        context.drawText(textRenderer, displayText, dialogX + 20, inputY + 8, textColor, false);

        if (createDialogFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = dialogX + 20 + textRenderer.getWidth(newFileName);
            context.fill(cursorX, inputY + 6, cursorX + 1, inputY + 18, theme.textColor);
        }

        int btnY = dialogY + 80;
        int btnWidth = 80;

        boolean cancelHovered = mouseX >= dialogX + 15 && mouseX < dialogX + 15 + btnWidth && mouseY >= btnY
                && mouseY < btnY + 24;
        context.fill(dialogX + 15, btnY, dialogX + 15 + btnWidth, btnY + 24,
                cancelHovered ? theme.buttonHoverColor : theme.buttonColor);
        context.drawText(textRenderer, "Cancel", dialogX + 15 + (btnWidth - textRenderer.getWidth("Cancel")) / 2,
                btnY + 8, theme.textColor, false);

        boolean createHovered = mouseX >= dialogX + dialogWidth - 15 - btnWidth && mouseX < dialogX + dialogWidth - 15
                && mouseY >= btnY && mouseY < btnY + 24;
        context.fill(dialogX + dialogWidth - 15 - btnWidth, btnY, dialogX + dialogWidth - 15, btnY + 24,
                createHovered ? theme.accentColor : adjustBrightness(theme.accentColor, -30));
        context.drawText(textRenderer, "Create",
                dialogX + dialogWidth - 15 - btnWidth + (btnWidth - textRenderer.getWidth("Create")) / 2, btnY + 8,
                0xFFFFFFFF, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (showCreateDialog) {
            MinecraftClient client = MinecraftClient.getInstance();
            int dialogWidth = 300;
            int dialogHeight = 120;
            int dialogX = (client.getWindow().getScaledWidth() - dialogWidth) / 2;
            int dialogY = (client.getWindow().getScaledHeight() - dialogHeight) / 2;

            int btnY = dialogY + 80;
            int btnWidth = 80;

            if (mouseX >= dialogX + 15 && mouseX < dialogX + 15 + btnWidth && mouseY >= btnY && mouseY < btnY + 24) {
                showCreateDialog = false;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                newFileName = "";
                return true;
            }

            if (mouseX >= dialogX + dialogWidth - 15 - btnWidth && mouseX < dialogX + dialogWidth - 15 && mouseY >= btnY
                    && mouseY < btnY + 24) {
                if (!newFileName.isEmpty()) {
                    String fileName = newFileName.endsWith(".kh") ? newFileName : newFileName + ".kh";
                    String content = "// New Kashub Script\n// Created: " + java.time.LocalDateTime.now()
                            + "\n\nprint \"Hello, World!\"\n";

                    if (isCreatingRemoteScript) {
                        kasperstudios.kashub.util.RemoteScriptManager.getInstance().saveScript(fileName, content,
                                response -> {
                                    boolean success = response != null && !response.startsWith("Error");
                                    if (success)
                                        refreshFiles();
                                });
                        if (onFileSelect != null) {
                            onFileSelect.accept("[REMOTE] " + fileName);
                        }
                    } else if (isCreatingModpackScript) {
                        ScriptManager.saveServerScript(fileName, content);
                        refreshFiles();
                        if (onFileSelect != null) {
                            onFileSelect.accept("[MODPACK] " + fileName);
                        }
                    } else {
                        ScriptManager.saveScript(fileName, content);
                        refreshFiles();
                        if (onFileSelect != null) {
                            onFileSelect.accept(fileName);
                        }
                    }
                }
                showCreateDialog = false;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                newFileName = "";
                return true;
            }

            int inputY = dialogY + 40;
            if (mouseX >= dialogX + 15 && mouseX < dialogX + dialogWidth - 15 && mouseY >= inputY
                    && mouseY < inputY + 24) {
                createDialogFocused = true;
                return true;
            }

            return true;
        }

        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }

        int inputX = x + 24;
        int inputWidth = width - 32;
        if (mouseX >= inputX && mouseX < inputX + inputWidth && mouseY >= y + 4 && mouseY < y + SEARCH_HEIGHT - 4) {

            if (!searchQuery.isEmpty()) {
                int clearX = inputX + inputWidth - 16;
                if (mouseX >= clearX && mouseX < clearX + 12) {
                    searchQuery = "";
                    applyFilter();
                    return true;
                }
            }
            searchFocused = true;
            createDialogFocused = false;
            return true;
        } else {
            searchFocused = false;
        }

        int btnY = y + height - CREATE_BUTTON_HEIGHT;
        boolean isOperator = ScriptManager.isOperator();

        if (isOperator) {
            int btnWidth = (width - 20) / 3;

            // User
            if (mouseX >= x + 5 && mouseX < x + 5 + btnWidth - 2 && mouseY >= btnY + 4
                    && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4) {
                showCreateDialog = true;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                createDialogFocused = true;
                return true;
            }

            // Modpack
            if (mouseX >= x + 5 + btnWidth && mouseX < x + 5 + btnWidth + btnWidth - 2 && mouseY >= btnY + 4
                    && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4) {
                showCreateDialog = true;
                isCreatingModpackScript = true;
                isCreatingRemoteScript = false;
                createDialogFocused = true;
                return true;
            }

            // Remote
            if (mouseX >= x + 5 + btnWidth * 2 && mouseX < x + 5 + btnWidth * 2 + btnWidth - 2 && mouseY >= btnY + 4
                    && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4) {
                showCreateDialog = true;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = true;
                createDialogFocused = true;
                return true;
            }
        } else {
            // Single button for non-operators
            if (mouseX >= x + 10 && mouseX < x + width - 10 && mouseY >= btnY + 4
                    && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4) {
                showCreateDialog = true;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                createDialogFocused = true;
                return true;
            }
        }

        if (hoveredIndex >= 0 && hoveredIndex < filteredScripts.size()) {
            String script = filteredScripts.get(hoveredIndex);

            // Logic to determine actual name based on display list
            String actualName;
            if (script.startsWith("[REMOTE]"))
                actualName = script;
            else if (script.startsWith("[MODPACK]"))
                actualName = script;
            else if (script.startsWith("[SYS]"))
                actualName = script.substring(6); // System scripts often used without prefix for logic but displayed
                                                  // with one
            else
                actualName = script;

            // Re-evaluating actual logic since renderFileList constructs visual list
            // differently
            // filteredScripts contains keys like "[REMOTE] foo.kh", "[MODPACK] bar.kh",
            // "[SYS] baz.kh", "user.kh"
            // FilePanel needs to emit these keys so ModernEditorScreen can parse them.

            boolean isSystem = script.startsWith("[SYS] ");
            // ModernEditorScreen.loadScript expects:
            // [SERVER] for modpack (which we renamed to MODPACK)
            // [REMOTE] for remote
            // nothing for user
            // system scripts? loadScript handles them if check isSystemScript

            // Wait, ModernEditorScreen.java loads system scripts if loadScript() detects
            // it.
            // But FilePanel currently passes `actualName` which strips [SYS].

            if (isSystem) {
                actualName = script.substring(6);
            } else {
                actualName = script;
            }

            selectedFile = actualName;

            if (onFileSelect != null) {
                onFileSelect.accept(actualName);
            }
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (searchFocused) {

            if (keyCode == 259 && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                applyFilter();
                return true;
            }

            if (keyCode == 256) {
                searchFocused = false;
                return true;
            }

            if (keyCode == 257) {
                searchFocused = false;
                return true;
            }
        }

        if (showCreateDialog && createDialogFocused) {

            if (keyCode == 259 && !newFileName.isEmpty()) {
                newFileName = newFileName.substring(0, newFileName.length() - 1);
                return true;
            }

            if (keyCode == 257) {
                if (!newFileName.isEmpty()) {
                    String fileName = newFileName.endsWith(".kh") ? newFileName : newFileName + ".kh";
                    String content = "// New Kashub Script\n\nprint \"Hello, World!\"\n";

                    if (isCreatingRemoteScript) {
                        kasperstudios.kashub.util.RemoteScriptManager.getInstance().saveScript(fileName, content,
                                response -> {
                                    boolean success = response != null && !response.startsWith("Error");
                                    if (success)
                                        refreshFiles();
                                });
                        if (onFileSelect != null) {
                            onFileSelect.accept("[REMOTE] " + fileName);
                        }
                    } else if (isCreatingModpackScript) {
                        ScriptManager.saveServerScript(fileName, content);
                        refreshFiles();
                        if (onFileSelect != null) {
                            onFileSelect.accept("[MODPACK] " + fileName);
                        }
                    } else {
                        ScriptManager.saveScript(fileName, content);
                        refreshFiles();
                        if (onFileSelect != null) {
                            onFileSelect.accept(fileName);
                        }
                    }
                }
                showCreateDialog = false;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                newFileName = "";
                return true;
            }

            if (keyCode == 256) {
                showCreateDialog = false;
                isCreatingModpackScript = false;
                isCreatingRemoteScript = false;
                newFileName = "";
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {

        if (searchFocused) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.' || chr == ' ') {
                searchQuery += chr;
                applyFilter();
                return true;
            }
        }

        if (showCreateDialog && createDialogFocused) {
            if (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.') {
                newFileName += chr;
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }

        int listHeight = height - SEARCH_HEIGHT - CREATE_BUTTON_HEIGHT;
        int totalHeight = filteredScripts.size() * ITEM_HEIGHT + HEADER_HEIGHT * 2;
        int maxScroll = Math.max(0, totalHeight - listHeight);
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * ITEM_HEIGHT)));
        return true;
    }

    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }

    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

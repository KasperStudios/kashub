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
    
    // Create file dialog state
    private boolean showCreateDialog = false;
    private String newFileName = "";
    private boolean createDialogFocused = false;
    
    // Search field state
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

    public void refreshFiles() {
        kasperstudios.kashub.config.KashubConfig config = kasperstudios.kashub.config.KashubConfig.getInstance();
        if (config.hideSystemScripts) {
            systemScripts = new ArrayList<>();
        } else {
            systemScripts = ScriptManager.getSystemScripts();
        }
        userScripts = ScriptManager.getUserScripts();
        applyFilter();
    }
    
    private void applyFilter() {
        filteredScripts.clear();
        String query = searchQuery.toLowerCase();
        
        for (String script : systemScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add("[SYS] " + script);
            }
        }
        for (String script : userScripts) {
            if (query.isEmpty() || script.toLowerCase().contains(query)) {
                filteredScripts.add(script);
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        theme = ThemeManager.getCurrentTheme();
        
        // Search bar
        renderSearchBar(context, mouseX, mouseY);
        
        // File list
        renderFileList(context, mouseX, mouseY, delta);
        
        // Create button at bottom
        renderCreateButton(context, mouseX, mouseY);
        
        // NOTE: Create dialog is rendered separately via renderCreateDialogOverlay()
        // to ensure it appears on top of all other elements
    }
    
    /**
     * Renders the create dialog overlay. Should be called LAST in the render order
     * to ensure it appears on top of all other elements.
     */
    public void renderCreateDialogOverlay(DrawContext context, int mouseX, int mouseY) {
        if (showCreateDialog) {
            renderCreateDialog(context, mouseX, mouseY);
        }
    }
    
    /**
     * Returns true if the create dialog is currently showing
     */
    public boolean isCreateDialogShowing() {
        return showCreateDialog;
    }
    
    private void renderSearchBar(DrawContext context, int mouseX, int mouseY) {
        int searchY = y;
        
        // Background
        context.fill(x, searchY, x + width, searchY + SEARCH_HEIGHT, adjustBrightness(theme.sidebarColor, 5));
        
        // Search icon
        context.drawText(textRenderer, "🔍", x + 8, searchY + 9, theme.textDimColor, false);
        
        // Search input background
        int inputX = x + 24;
        int inputWidth = width - 32;
        int bgColor = searchFocused ? adjustBrightness(theme.backgroundColor, 10) : theme.backgroundColor;
        context.fill(inputX, searchY + 4, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4, bgColor);
        
        // Border when focused
        if (searchFocused) {
            context.fill(inputX, searchY + 4, inputX + inputWidth, searchY + 5, theme.accentColor);
            context.fill(inputX, searchY + SEARCH_HEIGHT - 5, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4, theme.accentColor);
            context.fill(inputX, searchY + 4, inputX + 1, searchY + SEARCH_HEIGHT - 4, theme.accentColor);
            context.fill(inputX + inputWidth - 1, searchY + 4, inputX + inputWidth, searchY + SEARCH_HEIGHT - 4, theme.accentColor);
        }
        
        // Search text or placeholder
        String displayText = searchQuery.isEmpty() ? "Search scripts..." : searchQuery;
        int textColor = searchQuery.isEmpty() ? theme.textDimColor : theme.textColor;
        context.drawText(textRenderer, displayText, inputX + 6, searchY + 9, textColor, false);
        
        // Cursor when focused
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = inputX + 6 + textRenderer.getWidth(searchQuery);
            context.fill(cursorX, searchY + 7, cursorX + 1, searchY + SEARCH_HEIGHT - 7, theme.textColor);
        }
        
        // Clear button if has text
        if (!searchQuery.isEmpty()) {
            int clearX = inputX + inputWidth - 16;
            boolean clearHovered = mouseX >= clearX && mouseX < clearX + 12 && mouseY >= searchY + 4 && mouseY < searchY + SEARCH_HEIGHT - 4;
            context.drawText(textRenderer, "✕", clearX, searchY + 9, clearHovered ? theme.consoleErrorColor : theme.textDimColor, false);
        }
    }
    
    private void renderFileList(DrawContext context, int mouseX, int mouseY, float delta) {
        int listY = y + SEARCH_HEIGHT;
        int listHeight = height - SEARCH_HEIGHT - CREATE_BUTTON_HEIGHT;
        
        // Update hovered index
        hoveredIndex = -1;
        
        // Headers and items
        int currentY = listY - scrollY;
        
        // System scripts header
        if (!systemScripts.isEmpty() && (searchQuery.isEmpty() || filteredScripts.stream().anyMatch(s -> s.startsWith("[SYS]")))) {
            if (currentY >= listY - HEADER_HEIGHT && currentY < listY + listHeight) {
                context.fill(x, Math.max(listY, currentY), x + width, Math.min(listY + listHeight, currentY + HEADER_HEIGHT), 
                    adjustBrightness(theme.sidebarColor, 8));
                context.drawText(textRenderer, "📁 SYSTEM", x + 10, currentY + 7, theme.accentColor, true);
            }
            currentY += HEADER_HEIGHT;
        }
        
        int index = 0;
        for (String script : filteredScripts) {
            if (currentY >= listY - ITEM_HEIGHT && currentY < listY + listHeight) {
                boolean isSystem = script.startsWith("[SYS] ");
                String displayName = isSystem ? script.substring(6) : script;
                String actualName = isSystem ? displayName : script;
                
                // Check if this is user scripts section header needed
                if (!isSystem && index > 0 && filteredScripts.get(index - 1).startsWith("[SYS]")) {
                    // Draw user header
                    context.fill(x, Math.max(listY, currentY), x + width, Math.min(listY + listHeight, currentY + HEADER_HEIGHT),
                        adjustBrightness(theme.sidebarColor, 8));
                    context.drawText(textRenderer, "📂 USER SCRIPTS", x + 10, currentY + 7, theme.accentColor, true);
                    currentY += HEADER_HEIGHT;
                }
                
                // Check hover
                if (mouseX >= x && mouseX < x + width && mouseY >= currentY && mouseY < currentY + ITEM_HEIGHT && mouseY >= listY && mouseY < listY + listHeight) {
                    hoveredIndex = index;
                }
                
                // Background
                boolean isSelected = actualName.equals(selectedFile);
                boolean isHovered = hoveredIndex == index;
                
                if (isSelected) {
                    context.fill(x + 4, currentY + 2, x + width - 4, currentY + ITEM_HEIGHT - 2, theme.accentColor & 0x44FFFFFF);
                } else if (isHovered) {
                    context.fill(x + 4, currentY + 2, x + width - 4, currentY + ITEM_HEIGHT - 2, theme.buttonHoverColor);
                }
                
                // Icon - show folder icon for scripts in subdirectories
                boolean isInFolder = displayName.contains("/");
                String icon = isSystem ? "📜" : (isInFolder ? "📁" : "📄");
                context.drawText(textRenderer, icon, x + 12, currentY + 8, theme.textDimColor, false);
                
                // File name - show folder path for scripts in subdirectories
                String name = displayName;
                if (isInFolder) {
                    // Show "folder/script" format
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
        
        // Empty state
        if (filteredScripts.isEmpty()) {
            String emptyText = searchQuery.isEmpty() ? "No scripts found" : "No matches for \"" + searchQuery + "\"";
            int textWidth = textRenderer.getWidth(emptyText);
            context.drawText(textRenderer, emptyText, x + (width - textWidth) / 2, listY + 40, theme.textDimColor, false);
        }
        
        // Scrollbar
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
        
        boolean isHovered = mouseX >= x + 10 && mouseX < x + width - 10 && mouseY >= btnY + 4 && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4;
        
        // Button background
        int bgColor = isHovered ? theme.accentColor : theme.buttonColor;
        context.fill(x + 10, btnY + 4, x + width - 10, btnY + CREATE_BUTTON_HEIGHT - 4, bgColor);
        
        // Button text
        String text = "+ New Script";
        int textWidth = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, x + (width - textWidth) / 2, btnY + 12, 0xFFFFFFFF, true);
    }
    
    private void renderCreateDialog(DrawContext context, int mouseX, int mouseY) {
        // Overlay
        MinecraftClient client = MinecraftClient.getInstance();
        context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(), 0xAA000000);
        
        // Dialog box
        int dialogWidth = 300;
        int dialogHeight = 120;
        int dialogX = (client.getWindow().getScaledWidth() - dialogWidth) / 2;
        int dialogY = (client.getWindow().getScaledHeight() - dialogHeight) / 2;
        
        // Background
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, theme.sidebarColor);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 1, theme.accentColor);
        context.fill(dialogX, dialogY + dialogHeight - 1, dialogX + dialogWidth, dialogY + dialogHeight, theme.accentColor);
        context.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogHeight, theme.accentColor);
        context.fill(dialogX + dialogWidth - 1, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, theme.accentColor);
        
        // Title
        context.drawText(textRenderer, "Create New Script", dialogX + 15, dialogY + 15, theme.accentColor, true);
        
        // Input field
        int inputY = dialogY + 40;
        context.fill(dialogX + 15, inputY, dialogX + dialogWidth - 15, inputY + 24, theme.backgroundColor);
        
        String displayText = newFileName.isEmpty() ? "script_name.kh" : newFileName;
        int textColor = newFileName.isEmpty() ? theme.textDimColor : theme.textColor;
        context.drawText(textRenderer, displayText, dialogX + 20, inputY + 8, textColor, false);
        
        // Cursor
        if (createDialogFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = dialogX + 20 + textRenderer.getWidth(newFileName);
            context.fill(cursorX, inputY + 6, cursorX + 1, inputY + 18, theme.textColor);
        }
        
        // Buttons
        int btnY = dialogY + 80;
        int btnWidth = 80;
        
        // Cancel button
        boolean cancelHovered = mouseX >= dialogX + 15 && mouseX < dialogX + 15 + btnWidth && mouseY >= btnY && mouseY < btnY + 24;
        context.fill(dialogX + 15, btnY, dialogX + 15 + btnWidth, btnY + 24, cancelHovered ? theme.buttonHoverColor : theme.buttonColor);
        context.drawText(textRenderer, "Cancel", dialogX + 15 + (btnWidth - textRenderer.getWidth("Cancel")) / 2, btnY + 8, theme.textColor, false);
        
        // Create button
        boolean createHovered = mouseX >= dialogX + dialogWidth - 15 - btnWidth && mouseX < dialogX + dialogWidth - 15 && mouseY >= btnY && mouseY < btnY + 24;
        context.fill(dialogX + dialogWidth - 15 - btnWidth, btnY, dialogX + dialogWidth - 15, btnY + 24, createHovered ? theme.accentColor : adjustBrightness(theme.accentColor, -30));
        context.drawText(textRenderer, "Create", dialogX + dialogWidth - 15 - btnWidth + (btnWidth - textRenderer.getWidth("Create")) / 2, btnY + 8, 0xFFFFFFFF, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle create dialog
        if (showCreateDialog) {
            MinecraftClient client = MinecraftClient.getInstance();
            int dialogWidth = 300;
            int dialogHeight = 120;
            int dialogX = (client.getWindow().getScaledWidth() - dialogWidth) / 2;
            int dialogY = (client.getWindow().getScaledHeight() - dialogHeight) / 2;
            
            int btnY = dialogY + 80;
            int btnWidth = 80;
            
            // Cancel button
            if (mouseX >= dialogX + 15 && mouseX < dialogX + 15 + btnWidth && mouseY >= btnY && mouseY < btnY + 24) {
                showCreateDialog = false;
                newFileName = "";
                return true;
            }
            
            // Create button
            if (mouseX >= dialogX + dialogWidth - 15 - btnWidth && mouseX < dialogX + dialogWidth - 15 && mouseY >= btnY && mouseY < btnY + 24) {
                if (!newFileName.isEmpty()) {
                    String fileName = newFileName.endsWith(".kh") ? newFileName : newFileName + ".kh";
                    ScriptManager.saveScript(fileName, "// New Kashub Script\n// Created: " + java.time.LocalDateTime.now() + "\n\nprint \"Hello, World!\"\n");
                    refreshFiles();
                    if (onFileSelect != null) {
                        onFileSelect.accept(fileName);
                    }
                }
                showCreateDialog = false;
                newFileName = "";
                return true;
            }
            
            // Input field focus
            int inputY = dialogY + 40;
            if (mouseX >= dialogX + 15 && mouseX < dialogX + dialogWidth - 15 && mouseY >= inputY && mouseY < inputY + 24) {
                createDialogFocused = true;
                return true;
            }
            
            return true; // Consume click when dialog is open
        }
        
        // Check if click is in panel bounds
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        // Search field click
        int inputX = x + 24;
        int inputWidth = width - 32;
        if (mouseX >= inputX && mouseX < inputX + inputWidth && mouseY >= y + 4 && mouseY < y + SEARCH_HEIGHT - 4) {
            // Check clear button first
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
        
        // Create button
        int btnY = y + height - CREATE_BUTTON_HEIGHT;
        if (mouseX >= x + 10 && mouseX < x + width - 10 && mouseY >= btnY + 4 && mouseY < btnY + CREATE_BUTTON_HEIGHT - 4) {
            showCreateDialog = true;
            createDialogFocused = true;
            return true;
        }
        
        // File selection
        if (hoveredIndex >= 0 && hoveredIndex < filteredScripts.size()) {
            String script = filteredScripts.get(hoveredIndex);
            boolean isSystem = script.startsWith("[SYS] ");
            String actualName = isSystem ? script.substring(6) : script;
            selectedFile = actualName;
            
            if (onFileSelect != null) {
                onFileSelect.accept(actualName);
            }
            return true;
        }
        
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Search field input
        if (searchFocused) {
            // Backspace
            if (keyCode == 259 && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                applyFilter();
                return true;
            }
            // Escape - unfocus
            if (keyCode == 256) {
                searchFocused = false;
                return true;
            }
            // Enter - unfocus
            if (keyCode == 257) {
                searchFocused = false;
                return true;
            }
        }
        
        if (showCreateDialog && createDialogFocused) {
            // Backspace
            if (keyCode == 259 && !newFileName.isEmpty()) {
                newFileName = newFileName.substring(0, newFileName.length() - 1);
                return true;
            }
            // Enter
            if (keyCode == 257) {
                if (!newFileName.isEmpty()) {
                    String fileName = newFileName.endsWith(".kh") ? newFileName : newFileName + ".kh";
                    ScriptManager.saveScript(fileName, "// New Kashub Script\n\nprint \"Hello, World!\"\n");
                    refreshFiles();
                    if (onFileSelect != null) {
                        onFileSelect.accept(fileName);
                    }
                }
                showCreateDialog = false;
                newFileName = "";
                return true;
            }
            // Escape
            if (keyCode == 256) {
                showCreateDialog = false;
                newFileName = "";
                return true;
            }
        }
        return false;
    }
    
    public boolean charTyped(char chr, int modifiers) {
        // Search field input
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

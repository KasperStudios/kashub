package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor settings panel
 */
public class SettingsPanel {
    private int x, y, width, height;
    private EditorTheme theme;
    private final TextRenderer textRenderer;
    
    private int scrollY = 0;
    private List<SettingEntry> settings = new ArrayList<>();
    
    private static final int HEADER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 40;
    private static final int TOGGLE_WIDTH = 44;
    private static final int TOGGLE_HEIGHT = 22;
    
    public SettingsPanel(int x, int y, int width, int height, EditorTheme theme) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = theme;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        
        initializeSettings();
    }
    
    private void initializeSettings() {
        settings.clear();
        KashubConfig config = KashubConfig.getInstance();
        
        // Section: Editor
        settings.add(new SettingEntry("EDITOR", null, null, true));
        settings.add(new SettingEntry("Line Numbers", "Show line numbers in editor", 
            () -> config.editorLineNumbers, v -> config.editorLineNumbers = v));
        settings.add(new SettingEntry("Auto Complete", "Enable code auto-completion",
            () -> config.editorAutoComplete, v -> config.editorAutoComplete = v));
        settings.add(new SettingEntry("Syntax Highlight", "Enable syntax highlighting",
            () -> config.editorSyntaxHighlight, v -> config.editorSyntaxHighlight = v));
        
        // Section: Logging
        settings.add(new SettingEntry("LOGGING", null, null, true));
        settings.add(new SettingEntry("Enable Logging", "Log script execution",
            () -> config.enableLogging, v -> config.enableLogging = v));
        settings.add(new SettingEntry("Log to File", "Save logs to file",
            () -> config.logToFile, v -> config.logToFile = v));
        settings.add(new SettingEntry("Log to Chat", "Show logs in game chat",
            () -> config.logToChat, v -> config.logToChat = v));
        
        // Section: Security
        settings.add(new SettingEntry("SECURITY", null, null, true));
        settings.add(new SettingEntry("Sandbox Mode", "Restrict dangerous commands",
            () -> config.sandboxMode, v -> config.sandboxMode = v));
        settings.add(new SettingEntry("Allow Cheats", "Enable cheat commands",
            () -> config.allowCheats, v -> config.allowCheats = v));
        settings.add(new SettingEntry("Allow HTTP", "Enable HTTP requests",
            () -> config.allowHttpRequests, v -> config.allowHttpRequests = v));
        settings.add(new SettingEntry("Allow AI", "Enable AI integration",
            () -> config.allowAiIntegration, v -> config.allowAiIntegration = v));
        
        // Section: Scripts
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
                    kasperstudios.kashub.util.ScriptFileWatcher.getInstance().start();
                } else {
                    kasperstudios.kashub.util.ScriptFileWatcher.getInstance().stop();
                }
            }));
        settings.add(new SettingEntry("Autorun Enabled", "Auto-run scripts on startup",
            () -> config.autorunEnabled, v -> {
                config.autorunEnabled = v;
                config.save();
            }));
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(x, y, x + width, y + height, theme.consoleBackground);
        
        // Header
        context.fill(x, y, x + width, y + HEADER_HEIGHT, adjustBrightness(theme.consoleBackground, 10));
        context.drawText(textRenderer, "⚙ SETTINGS", x + 10, y + 10, theme.textColor, true);
        
        // Separator
        context.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, theme.accentColor & 0x66FFFFFF);
        
        // Settings list
        int listY = y + HEADER_HEIGHT;
        int listHeight = height - HEADER_HEIGHT;
        int visibleRows = listHeight / ROW_HEIGHT;
        
        int startIndex = scrollY / ROW_HEIGHT;
        int endIndex = Math.min(startIndex + visibleRows + 2, settings.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            SettingEntry entry = settings.get(i);
            int rowY = listY + (i * ROW_HEIGHT) - scrollY;
            
            if (rowY + ROW_HEIGHT < listY || rowY > y + height) continue;
            
            if (entry.isSection) {
                renderSectionHeader(context, entry, rowY);
            } else {
                renderSettingRow(context, entry, rowY, mouseX, mouseY);
            }
        }
        
        // Scrollbar
        if (settings.size() * ROW_HEIGHT > listHeight) {
            int totalHeight = settings.size() * ROW_HEIGHT;
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = listY + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);
            
            context.fill(x + width - 4, listY, x + width, y + height, 0x22FFFFFF);
            context.fill(x + width - 4, scrollbarY, x + width, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }
    
    private void renderSectionHeader(DrawContext context, SettingEntry entry, int rowY) {
        context.fill(x, rowY, x + width, rowY + ROW_HEIGHT, adjustBrightness(theme.consoleBackground, 5));
        context.drawText(textRenderer, "▸ " + entry.name, x + 10, rowY + 14, theme.accentColor, true);
        context.fill(x + 10, rowY + ROW_HEIGHT - 2, x + width - 10, rowY + ROW_HEIGHT - 1, theme.accentColor & 0x44FFFFFF);
    }
    
    private void renderSettingRow(DrawContext context, SettingEntry entry, int rowY, int mouseX, int mouseY) {
        // Hover effect
        boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
        if (isHovered) {
            context.fill(x + 4, rowY + 2, x + width - 4, rowY + ROW_HEIGHT - 2, theme.buttonHoverColor);
        }
        
        // Name
        context.drawText(textRenderer, entry.name, x + 20, rowY + 8, theme.textColor, false);
        
        // Description
        if (entry.description != null) {
            context.drawText(textRenderer, entry.description, x + 20, rowY + 22, theme.textDimColor, false);
        }
        
        // Toggle switch
        if (entry.getter != null) {
            boolean value = entry.getter.get();
            int toggleX = x + width - TOGGLE_WIDTH - 20;
            int toggleY = rowY + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
            
            renderToggle(context, toggleX, toggleY, value, mouseX, mouseY);
        }
    }
    
    private void renderToggle(DrawContext context, int tx, int ty, boolean value, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= tx && mouseX < tx + TOGGLE_WIDTH && mouseY >= ty && mouseY < ty + TOGGLE_HEIGHT;
        
        // Toggle background
        int bgColor = value ? theme.consoleSuccessColor : theme.textDimColor;
        if (isHovered) {
            bgColor = brighten(bgColor, 20);
        }
        
        // Draw rounded background
        context.fill(tx + 2, ty, tx + TOGGLE_WIDTH - 2, ty + TOGGLE_HEIGHT, bgColor);
        context.fill(tx, ty + 2, tx + TOGGLE_WIDTH, ty + TOGGLE_HEIGHT - 2, bgColor);
        context.fill(tx + 1, ty + 1, tx + TOGGLE_WIDTH - 1, ty + TOGGLE_HEIGHT - 1, bgColor);
        
        // Circle
        int circleX = value ? tx + TOGGLE_WIDTH - TOGGLE_HEIGHT + 2 : tx + 2;
        int circleSize = TOGGLE_HEIGHT - 4;
        context.fill(circleX, ty + 2, circleX + circleSize, ty + 2 + circleSize, 0xFFFFFFFF);
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        int listY = y + HEADER_HEIGHT;
        if (mouseY < listY) return false;
        
        int relativeY = (int) mouseY - listY + scrollY;
        int index = relativeY / ROW_HEIGHT;
        
        if (index >= 0 && index < settings.size()) {
            SettingEntry entry = settings.get(index);
            
            if (!entry.isSection && entry.getter != null && entry.setter != null) {
                // Check click on toggle
                int rowY = listY + (index * ROW_HEIGHT) - scrollY;
                int toggleX = x + width - TOGGLE_WIDTH - 20;
                int toggleY = rowY + (ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
                
                if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_WIDTH &&
                    mouseY >= toggleY && mouseY < toggleY + TOGGLE_HEIGHT) {
                    
                    boolean newValue = !entry.getter.get();
                    entry.setter.accept(newValue);
                    KashubConfig.getInstance().save();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        int listHeight = height - HEADER_HEIGHT;
        int maxScroll = Math.max(0, settings.size() * ROW_HEIGHT - listHeight);
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * ROW_HEIGHT)));
        return true;
    }
    
    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }
    
    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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
    
    @FunctionalInterface
    interface BooleanGetter {
        boolean get();
    }
    
    @FunctionalInterface
    interface BooleanSetter {
        void accept(boolean value);
    }
    
    private static class SettingEntry {
        final String name;
        final String description;
        final BooleanGetter getter;
        final BooleanSetter setter;
        final boolean isSection;
        
        SettingEntry(String name, String description, BooleanGetter getter, BooleanSetter setter) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = setter;
            this.isSection = false;
        }
        
        SettingEntry(String name, String description, BooleanGetter getter, boolean isSection) {
            this.name = name;
            this.description = description;
            this.getter = getter;
            this.setter = null;
            this.isSection = isSection;
        }
    }
}

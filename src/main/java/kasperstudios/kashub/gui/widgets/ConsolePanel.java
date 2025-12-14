package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.gui.theme.EditorTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConsolePanel {
    private final int x, y, width, height;
    private EditorTheme theme;
    private final TextRenderer textRenderer;
    
    private final List<LogEntry> logs = new ArrayList<>();
    private int scrollY = 0;
    private int maxLogs = 1000;
    
    private static final int LINE_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 24;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public enum LogLevel {
        INFO, WARN, ERROR, SUCCESS, DEBUG
    }
    
    public ConsolePanel(int x, int y, int width, int height, EditorTheme theme) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = theme;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }
    
    public void log(String message, LogLevel level) {
        String time = LocalTime.now().format(TIME_FORMAT);
        logs.add(new LogEntry(time, message, level));
        
        while (logs.size() > maxLogs) {
            logs.remove(0);
        }
        
        // Auto-scroll to bottom
        int contentHeight = logs.size() * LINE_HEIGHT;
        int visibleHeight = height - HEADER_HEIGHT;
        if (contentHeight > visibleHeight) {
            scrollY = contentHeight - visibleHeight;
        }
    }
    
    public void clear() {
        logs.clear();
        scrollY = 0;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(x, y, x + width, y + height, theme.consoleBackground);
        
        // Header
        context.fill(x, y, x + width, y + HEADER_HEIGHT, adjustBrightness(theme.consoleBackground, 10));
        context.drawText(textRenderer, "⌨ CONSOLE", x + 10, y + 7, theme.textColor, true);
        
        // Clear button
        int clearX = x + width - 50;
        boolean clearHovered = mouseX >= clearX && mouseX < clearX + 40 && mouseY >= y + 4 && mouseY < y + 20;
        context.fill(clearX, y + 4, clearX + 40, y + 20, clearHovered ? theme.buttonHoverColor : theme.buttonColor);
        context.drawText(textRenderer, "Clear", clearX + 6, y + 7, theme.textColor, false);
        
        // Separator
        context.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, theme.accentColor & 0x66FFFFFF);
        
        // Logs
        int listY = y + HEADER_HEIGHT;
        int listHeight = height - HEADER_HEIGHT;
        int visibleLines = listHeight / LINE_HEIGHT;
        
        int startLine = scrollY / LINE_HEIGHT;
        int endLine = Math.min(startLine + visibleLines + 1, logs.size());
        
        for (int i = startLine; i < endLine; i++) {
            LogEntry entry = logs.get(i);
            int lineY = listY + (i * LINE_HEIGHT) - scrollY + 4;
            
            if (lineY < listY || lineY > y + height - LINE_HEIGHT) continue;
            
            // Time
            context.drawText(textRenderer, entry.time, x + 6, lineY, theme.textDimColor, false);
            
            // Level indicator
            String levelIcon = getLevelIcon(entry.level);
            int levelColor = getLevelColor(entry.level);
            context.drawText(textRenderer, levelIcon, x + 60, lineY, levelColor, false);
            
            // Message
            context.drawText(textRenderer, entry.message, x + 76, lineY, levelColor, false);
        }
        
        // Scrollbar
        if (logs.size() * LINE_HEIGHT > listHeight) {
            int totalHeight = logs.size() * LINE_HEIGHT;
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = listY + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);
            
            context.fill(x + width - 4, listY, x + width, y + height, 0x22FFFFFF);
            context.fill(x + width - 4, scrollbarY, x + width, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check clear button
        int clearX = x + width - 50;
        if (mouseX >= clearX && mouseX < clearX + 40 && mouseY >= y + 4 && mouseY < y + 20) {
            clear();
            return true;
        }
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int listHeight = height - HEADER_HEIGHT;
        int maxScroll = Math.max(0, logs.size() * LINE_HEIGHT - listHeight);
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * LINE_HEIGHT * 2)));
        return true;
    }
    
    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }
    
    private String getLevelIcon(LogLevel level) {
        return switch (level) {
            case INFO -> "ℹ";
            case WARN -> "⚠";
            case ERROR -> "✖";
            case SUCCESS -> "✔";
            case DEBUG -> "🔧";
        };
    }
    
    private int getLevelColor(LogLevel level) {
        return switch (level) {
            case INFO -> theme.consoleInfoColor;
            case WARN -> theme.consoleWarnColor;
            case ERROR -> theme.consoleErrorColor;
            case SUCCESS -> theme.consoleSuccessColor;
            case DEBUG -> theme.textDimColor;
        };
    }
    
    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private static class LogEntry {
        final String time;
        final String message;
        final LogLevel level;
        
        LogEntry(String time, String message, LogLevel level) {
            this.time = time;
            this.message = message;
            this.level = level;
        }
    }
}

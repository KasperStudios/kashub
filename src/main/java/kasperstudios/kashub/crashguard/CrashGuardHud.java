package kasperstudios.kashub.crashguard;

import kasperstudios.kashub.crashguard.CrashGuard.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/**
 * HUD рендер для отображения статуса CrashGuard
 */
public class CrashGuardHud {
    
    private static boolean enabled = true;
    private static boolean minimized = false;
    private static int posX = 5;
    private static int posY = 5;
    
    // Цвета
    private static final int COLOR_OK = 0xFF00FF00;      // Зелёный
    private static final int COLOR_WARNING = 0xFFFFFF00; // Жёлтый
    private static final int COLOR_ERROR = 0xFFFF0000;   // Красный
    private static final int COLOR_PAUSED = 0xFFFF8800;  // Оранжевый
    private static final int COLOR_TEXT = 0xFFFFFFFF;    // Белый
    private static final int COLOR_BG = 0x80000000;      // Полупрозрачный чёрный
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    public static void toggleMinimized() {
        minimized = !minimized;
    }
    
    public static void setPosition(int x, int y) {
        posX = x;
        posY = y;
    }
    
    /**
     * Рендерит HUD CrashGuard
     * Вызывается из HUD рендер хука
     */
    public static void render(DrawContext context, float tickDelta) {
        if (!enabled) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        CrashGuard guard = CrashGuard.getInstance();
        GlobalStats global = guard.getGlobalStats();
        Map<String, ScriptStats> scripts = guard.getStats();
        
        TextRenderer textRenderer = client.textRenderer;
        
        int x = posX;
        int y = posY;
        int lineHeight = 10;
        int padding = 3;
        
        if (minimized) {
            // Минимизированный вид - только иконка статуса
            String status = getGlobalStatusIcon(global, scripts);
            int width = textRenderer.getWidth(status) + padding * 2;
            
            context.fill(x, y, x + width, y + lineHeight + padding * 2, COLOR_BG);
            context.drawText(textRenderer, status, x + padding, y + padding, getGlobalStatusColor(global, scripts), true);
            return;
        }
        
        // Полный вид
        int width = 180;
        int height = 30 + (scripts.size() * lineHeight);
        
        // Фон
        context.fill(x, y, x + width, y + height, COLOR_BG);
        
        // Заголовок
        String title = "[CrashGuard] " + global.strictness.name();
        context.drawText(textRenderer, title, x + padding, y + padding, COLOR_TEXT, true);
        y += lineHeight;
        
        // Глобальная статистика
        String globalLine = String.format("CPU:%dms Act:%d/s Scripts:%d",
            global.totalCpuTimeMs, global.totalActionsPerSec, global.activeScripts);
        int globalColor = global.totalCpuTimeMs > 20 ? COLOR_WARNING : COLOR_TEXT;
        context.drawText(textRenderer, globalLine, x + padding, y + padding, globalColor, true);
        y += lineHeight;
        
        // Разделитель
        context.fill(x + padding, y + padding, x + width - padding, y + padding + 1, 0x40FFFFFF);
        y += 5;
        
        // Per-script статус
        for (Map.Entry<String, ScriptStats> entry : scripts.entrySet()) {
            ScriptStats stats = entry.getValue();
            String name = truncate(entry.getKey(), 12);
            String status = stats.getStatus();
            
            int color = getStatusColor(stats);
            String icon = getStatusIcon(stats);
            
            String line = String.format("%s %s CPU:%dms %d/s",
                icon, name, stats.cpuTimeMs, stats.actionsPerSec);
            
            context.drawText(textRenderer, line, x + padding, y + padding, color, true);
            y += lineHeight;
        }
        
        if (scripts.isEmpty()) {
            context.drawText(textRenderer, "  No active scripts", x + padding, y + padding, 0xFF888888, true);
        }
    }
    
    private static int getStatusColor(ScriptStats stats) {
        if (stats.isStopped) return COLOR_ERROR;
        if (stats.isPaused) return COLOR_PAUSED;
        if (stats.cpuTimeMs > 10 || stats.actionsPerSec > 30) return COLOR_WARNING;
        return COLOR_OK;
    }
    
    private static String getStatusIcon(ScriptStats stats) {
        if (stats.isStopped) return "✗";
        if (stats.isPaused) return "⏸";
        if (stats.cpuTimeMs > 10 || stats.actionsPerSec > 30) return "⚠";
        return "✓";
    }
    
    private static int getGlobalStatusColor(GlobalStats global, Map<String, ScriptStats> scripts) {
        for (ScriptStats stats : scripts.values()) {
            if (stats.isStopped) return COLOR_ERROR;
            if (stats.isPaused) return COLOR_PAUSED;
        }
        if (global.totalCpuTimeMs > 20) return COLOR_WARNING;
        return COLOR_OK;
    }
    
    private static String getGlobalStatusIcon(GlobalStats global, Map<String, ScriptStats> scripts) {
        for (ScriptStats stats : scripts.values()) {
            if (stats.isStopped) return "[CG] ✗";
            if (stats.isPaused) return "[CG] ⏸";
        }
        if (global.totalCpuTimeMs > 20) return "[CG] ⚠";
        return "[CG] ✓";
    }
    
    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 2) + "..";
    }
}

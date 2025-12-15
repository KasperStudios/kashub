package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import kasperstudios.kashub.runtime.ScriptState;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Task manager panel - displays active scripts with controls
 */
public class TaskManagerPanel {
    private int x, y, width, height;
    private EditorTheme theme;
    private final TextRenderer textRenderer;
    
    private List<ScriptTask> tasks = new ArrayList<>();
    private int scrollY = 0;
    private int selectedTaskId = -1;
    private int hoveredTaskId = -1;
    
    private Consumer<ScriptTask> onTaskSelect;
    
    private static final int HEADER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 36;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 8;
    
    // Pulse animation for RUNNING status
    private float pulseAnimation = 0f;
    
    public TaskManagerPanel(int x, int y, int width, int height, EditorTheme theme) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = theme;
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
    }
    
    public void setOnTaskSelect(Consumer<ScriptTask> onTaskSelect) {
        this.onTaskSelect = onTaskSelect;
    }
    
    public void refreshTasks() {
        Collection<ScriptTask> allTasks = ScriptTaskManager.getInstance().getAllTasks();
        tasks = new ArrayList<>(allTasks);
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update pulse animation
        pulseAnimation += delta * 0.1f;
        if (pulseAnimation > Math.PI * 2) {
            pulseAnimation -= Math.PI * 2;
        }
        
        // Update task list every frame
        refreshTasks();
        
        // Panel background
        context.fill(x, y, x + width, y + height, theme.consoleBackground);
        
        // Header
        renderHeader(context, mouseX, mouseY);
        
        // Separator
        context.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, theme.accentColor & 0x66FFFFFF);
        
        // Task list or placeholder
        if (tasks.isEmpty()) {
            renderEmptyState(context);
        } else {
            renderTaskList(context, mouseX, mouseY, delta);
        }
        
        // Control buttons at bottom
        renderControlButtons(context, mouseX, mouseY);
    }
    
    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + HEADER_HEIGHT, adjustBrightness(theme.consoleBackground, 10));
        context.drawText(textRenderer, "📋 TASK MANAGER", x + 10, y + 10, theme.textColor, true);
        
        // Active tasks counter
        int runningCount = (int) tasks.stream().filter(t -> t.getState() == ScriptState.RUNNING).count();
        String countText = runningCount + " running";
        int countColor = runningCount > 0 ? theme.consoleSuccessColor : theme.textDimColor;
        context.drawText(textRenderer, countText, x + width - textRenderer.getWidth(countText) - 10, y + 10, countColor, false);
    }
    
    private void renderEmptyState(DrawContext context) {
        String message = "No active scripts";
        int textWidth = textRenderer.getWidth(message);
        int centerX = x + (width - textWidth) / 2;
        int centerY = y + (height - HEADER_HEIGHT) / 2 + HEADER_HEIGHT;
        
        // Icon
        context.drawText(textRenderer, "📭", centerX - 10, centerY - 20, theme.textDimColor, false);
        context.drawText(textRenderer, message, centerX, centerY, theme.textDimColor, false);
        
        String hint = "Run a script to see it here";
        int hintWidth = textRenderer.getWidth(hint);
        context.drawText(textRenderer, hint, x + (width - hintWidth) / 2, centerY + 15, theme.textDimColor & 0x88FFFFFF, false);
    }
    
    private void renderTaskList(DrawContext context, int mouseX, int mouseY, float delta) {
        int listY = y + HEADER_HEIGHT;
        int listHeight = height - HEADER_HEIGHT - 50; // Leave space for buttons
        int visibleRows = listHeight / ROW_HEIGHT;
        
        // Update hovered
        hoveredTaskId = -1;
        
        int startIndex = scrollY / ROW_HEIGHT;
        int endIndex = Math.min(startIndex + visibleRows + 1, tasks.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ScriptTask task = tasks.get(i);
            int rowY = listY + (i * ROW_HEIGHT) - scrollY;
            
            if (rowY + ROW_HEIGHT < listY || rowY > y + height - 50) continue;
            
            // Check hover
            if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredTaskId = task.getId();
            }
            
            renderTaskRow(context, task, rowY, mouseX, mouseY, delta);
        }
        
        // Scrollbar
        if (tasks.size() * ROW_HEIGHT > listHeight) {
            int totalHeight = tasks.size() * ROW_HEIGHT;
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = listY + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);
            
            context.fill(x + width - 4, listY, x + width, listY + listHeight, 0x22FFFFFF);
            context.fill(x + width - 4, scrollbarY, x + width, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }
    
    private void renderTaskRow(DrawContext context, ScriptTask task, int rowY, int mouseX, int mouseY, float delta) {
        boolean isSelected = task.getId() == selectedTaskId;
        boolean isHovered = task.getId() == hoveredTaskId;
        
        // Row background
        int bgColor;
        if (isSelected) {
            bgColor = theme.accentColor & 0x44FFFFFF;
        } else if (isHovered) {
            bgColor = theme.buttonHoverColor;
        } else {
            bgColor = 0x00000000;
        }
        
        if (bgColor != 0) {
            context.fill(x + 4, rowY + 2, x + width - 4, rowY + ROW_HEIGHT - 2, bgColor);
        }
        
        // Status indicator (colored dot with animation)
        int statusColor = task.getState().getColor();
        int dotX = x + 12;
        int dotY = rowY + ROW_HEIGHT / 2;
        
        // Pulse for RUNNING
        if (task.getState() == ScriptState.RUNNING) {
            float pulse = (float) (Math.sin(pulseAnimation) * 0.3 + 0.7);
            int alpha = (int) (255 * pulse);
            int pulseColor = (alpha << 24) | (statusColor & 0x00FFFFFF);
            context.fill(dotX - 6, dotY - 6, dotX + 6, dotY + 6, pulseColor);
        }
        context.fill(dotX - 4, dotY - 4, dotX + 4, dotY + 4, statusColor);
        
        // ID and name
        String idText = "#" + task.getId();
        context.drawText(textRenderer, idText, x + 24, rowY + 6, theme.textDimColor, false);
        
        String name = task.getName();
        if (name.length() > 20) {
            name = name.substring(0, 17) + "...";
        }
        context.drawText(textRenderer, name, x + 24, rowY + 18, theme.textColor, false);
        
        // Status
        String stateText = task.getState().getDisplayName();
        int stateX = x + 140;
        context.drawText(textRenderer, stateText, stateX, rowY + 12, statusColor, false);
        
        // Uptime
        String uptime = task.getUptimeFormatted();
        int uptimeX = x + width - 80;
        context.drawText(textRenderer, uptime, uptimeX, rowY + 12, theme.textDimColor, false);
        
        // Error (if any)
        if (task.getLastError() != null && task.getState() == ScriptState.ERROR) {
            String error = task.getLastError();
            if (error.length() > 30) {
                error = error.substring(0, 27) + "...";
            }
            context.drawText(textRenderer, "⚠ " + error, x + 24, rowY + 28, theme.consoleErrorColor, false);
        }
    }
    
    private void renderControlButtons(DrawContext context, int mouseX, int mouseY) {
        int buttonY = y + height - 40;
        int buttonX = x + 10;
        
        // Pause All
        boolean pauseHovered = isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        renderButton(context, "⏸ Pause", buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 
            theme.consoleWarnColor, pauseHovered);
        
        // Resume All
        buttonX += BUTTON_WIDTH + BUTTON_SPACING;
        boolean resumeHovered = isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        renderButton(context, "▶ Resume", buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
            theme.consoleSuccessColor, resumeHovered);
        
        // Stop All
        buttonX += BUTTON_WIDTH + BUTTON_SPACING;
        boolean stopHovered = isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        renderButton(context, "⏹ Stop", buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
            theme.consoleErrorColor, stopHovered);
        
        // Buttons for selected task (if any)
        if (selectedTaskId >= 0) {
            buttonX = x + width - BUTTON_WIDTH - 10;
            boolean restartHovered = isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
            renderButton(context, "🔄 Restart", buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                theme.accentColor, restartHovered);
        }
    }
    
    private void renderButton(DrawContext context, String text, int bx, int by, int bw, int bh, int color, boolean hovered) {
        int bgColor = hovered ? brighten(color, 20) : color;
        
        // Button background
        context.fill(bx, by, bx + bw, by + bh, bgColor);
        
        // Border
        if (hovered) {
            context.fill(bx, by, bx + bw, by + 1, 0xFFFFFFFF);
            context.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFFFFFFFF);
            context.fill(bx, by, bx + 1, by + bh, 0xFFFFFFFF);
            context.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFFFFFFFF);
        }
        
        // Text
        int textWidth = textRenderer.getWidth(text);
        int textX = bx + (bw - textWidth) / 2;
        int textY = by + (bh - 8) / 2;
        context.drawText(textRenderer, text, textX, textY, 0xFFFFFFFF, true);
    }
    
    private boolean isButtonHovered(int bx, int by, int bw, int bh, int mouseX, int mouseY) {
        return mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        // Check click on control buttons
        int buttonY = y + height - 40;
        int buttonX = x + 10;
        
        // Pause All
        if (isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, (int) mouseX, (int) mouseY)) {
            ScriptTaskManager.getInstance().pauseAll();
            return true;
        }
        
        // Resume All
        buttonX += BUTTON_WIDTH + BUTTON_SPACING;
        if (isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, (int) mouseX, (int) mouseY)) {
            ScriptTaskManager.getInstance().resumeAll();
            return true;
        }
        
        // Stop All
        buttonX += BUTTON_WIDTH + BUTTON_SPACING;
        if (isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, (int) mouseX, (int) mouseY)) {
            ScriptTaskManager.getInstance().stopAll();
            return true;
        }
        
        // Restart selected
        if (selectedTaskId >= 0) {
            buttonX = x + width - BUTTON_WIDTH - 10;
            if (isButtonHovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, (int) mouseX, (int) mouseY)) {
                ScriptTaskManager.getInstance().restart(selectedTaskId);
                return true;
            }
        }
        
        // Check click on task row
        int listY = y + HEADER_HEIGHT;
        if (mouseY >= listY && mouseY < y + height - 50) {
            int relativeY = (int) mouseY - listY + scrollY;
            int index = relativeY / ROW_HEIGHT;
            
            if (index >= 0 && index < tasks.size()) {
                ScriptTask task = tasks.get(index);
                selectedTaskId = task.getId();
                
                if (onTaskSelect != null) {
                    onTaskSelect.accept(task);
                }
                return true;
            }
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        int listHeight = height - HEADER_HEIGHT - 50;
        int maxScroll = Math.max(0, tasks.size() * ROW_HEIGHT - listHeight);
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
}

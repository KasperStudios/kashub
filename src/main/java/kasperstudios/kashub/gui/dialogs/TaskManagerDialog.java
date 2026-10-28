package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.khgui.core.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import kasperstudios.khgui.api.ITheme;
import kasperstudios.kashub.core.State;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.util.ScriptManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Ported high-fidelity Task Manager from main project.
 * Adapted for KHGui and Fabric 1.21.1 (Yarn).
 * Emoji-free professional UI.
 */
public class TaskManagerDialog extends Screen {
    private final Screen parent;
    private final ITheme theme;

    private enum Tab {
        PROCESSES("Processes"),
        AUTORUN("Autorun");

        private final String displayName;

        Tab(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Tab currentTab = Tab.PROCESSES;

    private List<Task> tasks = new ArrayList<>();
    private int scrollY = 0;
    private int selectedTaskId = -1;
    private int hoveredTaskId = -1;

    private List<String> allScripts = new ArrayList<>();
    private List<String> autorunScripts = new ArrayList<>();
    private int autorunScrollY = 0;
    private int hoveredAutorunIndex = -1;
    private int hoveredAvailableIndex = -1;
    private int selectedAutorunIndex = -1;
    private int selectedAvailableIndex = -1;

    private static final int HEADER_HEIGHT = 40;
    private static final int TAB_HEIGHT = 32;
    private static final int ROW_HEIGHT = 36;
    private static final int BUTTON_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 50;

    private int getDialogWidth() { return this.width; }
    private int getDialogHeight() { return this.height; }

    private float pulseAnimation = 0f;
    private long lastClickTime = 0;
    private int lastClickedIndex = -1;
    private boolean lastClickWasAutorun = false;

    public TaskManagerDialog(Screen parent) {
        super(Component.literal("Task Manager"));
        this.parent = parent;
        this.theme = ThemeManager.getCurrentTheme();
        this.font = Minecraft.getInstance().font;
    }

    @Override
    protected void init() {
        super.init();
        refreshAutorunData();
    }

    private int getDialogX() {
        return 0;
    }

    private int getDialogY() {
        return 0;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Overridden to prevent default Minecraft background blur
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        pulseAnimation += delta * 0.1f;
        if (pulseAnimation > Math.PI * 2) {
            pulseAnimation -= Math.PI * 2;
        }

        refreshTasks();

        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int dx = getDialogX();
        int dy = getDialogY();

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.getSidebarBackground());

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.getSidebarBackground());

        // Borders removed for fullscreen mode

        renderHeader(context, dx, dy, mouseX, mouseY);

        renderTabs(context, dx, dy + HEADER_HEIGHT, mouseX, mouseY);

        int contentY = dy + HEADER_HEIGHT + TAB_HEIGHT;
        if (currentTab == Tab.PROCESSES) {
            renderTaskList(context, dx, contentY, mouseX, mouseY, delta);
        } else {
            renderAutorunPanel(context, dx, contentY, mouseX, mouseY, delta);
        }

        renderFooter(context, dx, dy + getDialogHeight() - FOOTER_HEIGHT, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + HEADER_HEIGHT, adjustBrightness(theme.getSidebarBackground(), 10));

        context.drawString(font, Component.literal("TASK MANAGER"), dx + 16, dy + 14, theme.getPrimaryText(), true);

        int runningCount = (int) tasks.stream().filter(t -> t.getState() == State.RUNNING).count();
        String countText = runningCount + " running / " + tasks.size() + " total";
        int countColor = runningCount > 0 ? 0xFF00FF00 : theme.getMutedText();
        int countWidth = font.width(countText);
        context.drawString(font, Component.literal(countText), dx + getDialogWidth() - countWidth - 60, dy + 14, countColor, false);

        int closeX = dx + getDialogWidth() - 30;
        int closeY = dy + 10;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20;
        context.fill(closeX, closeY, closeX + 20, closeY + 20,
                closeHovered ? 0xFFFF4444 : theme.getHover());
        context.drawString(font, Component.literal("X"), closeX + 7, closeY + 6, 0xFFFFFFFF, false);

        context.fill(dx, dy + HEADER_HEIGHT - 1, dx + getDialogWidth(), dy + HEADER_HEIGHT, theme.getPrimaryAccent() & 0x66FFFFFF);
    }

    private void renderTabs(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        int tabWidth = getDialogWidth() / Tab.values().length;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabX = dx + i * tabWidth;
            boolean isActive = tab == currentTab;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + tabWidth &&
                    mouseY >= dy && mouseY < dy + TAB_HEIGHT;

            int bgColor = isActive ? theme.getPrimaryAccent() : (isHovered ? theme.getHover() : theme.getSidebarBackground());
            context.fill(tabX, dy, tabX + tabWidth, dy + TAB_HEIGHT, bgColor);

            String tabText = tab.getDisplayName();
            int textWidth = font.width(tabText);
            int textX = tabX + (tabWidth - textWidth) / 2;
            int textY = dy + (TAB_HEIGHT - 9) / 2;
            int textColor = isActive ? 0xFFFFFFFF : theme.getPrimaryText();
            context.drawString(font, Component.literal(tabText), textX, textY, textColor, false);

            if (i < Tab.values().length - 1) {
                context.fill(tabX + tabWidth - 1, dy, tabX + tabWidth, dy + TAB_HEIGHT, theme.getPrimaryAccent() & 0x44FFFFFF);
            }
        }

        context.fill(dx, dy + TAB_HEIGHT - 1, dx + getDialogWidth(), dy + TAB_HEIGHT, theme.getPrimaryAccent() & 0x66FFFFFF);
    }

    private void renderTaskList(GuiGraphics context, int dx, int dy, int mouseX, int mouseY, float delta) {
        int listHeight = getDialogHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT;

        if (tasks.isEmpty()) {
            String message = "No active scripts";
            int textWidth = font.width(message);
            context.drawString(font, Component.literal(message), dx + (getDialogWidth() - textWidth) / 2, dy + listHeight / 2,
                    theme.getMutedText(), false);
            return;
        }

        hoveredTaskId = -1;

        int visibleRows = listHeight / ROW_HEIGHT;
        int startIndex = scrollY / ROW_HEIGHT;
        int endIndex = Math.min(startIndex + visibleRows + 1, tasks.size());

        for (int i = startIndex; i < endIndex; i++) {
            Task task = tasks.get(i);
            int rowY = dy + (i * ROW_HEIGHT) - scrollY;

            if (rowY + ROW_HEIGHT < dy || rowY > dy + listHeight)
                continue;

            if (mouseX >= dx && mouseX < dx + getDialogWidth() && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredTaskId = task.getId();
            }

            renderTaskRow(context, task, dx + 8, rowY, getDialogWidth() - 16, mouseX, mouseY);
        }

        if (tasks.size() * ROW_HEIGHT > listHeight) {
            int totalHeight = tasks.size() * ROW_HEIGHT;
            int scrollbarHeight = Math.max(20, (listHeight * listHeight) / totalHeight);
            int maxScroll = totalHeight - listHeight;
            int scrollbarY = dy + (scrollY * (listHeight - scrollbarHeight)) / Math.max(1, maxScroll);

            context.fill(dx + getDialogWidth() - 6, dy, dx + getDialogWidth() - 2, dy + listHeight, 0x22FFFFFF);
            context.fill(dx + getDialogWidth() - 6, scrollbarY, dx + getDialogWidth() - 2, scrollbarY + scrollbarHeight,
                    theme.getPrimaryAccent());
        }
    }

    private void renderTaskRow(GuiGraphics context, Task task, int x, int y, int width, int mouseX, int mouseY) {
        boolean isSelected = task.getId() == selectedTaskId;
        boolean isHovered = task.getId() == hoveredTaskId;

        int bgColor = isSelected ? (theme.getPrimaryAccent() & 0x44FFFFFF) : (isHovered ? theme.getHover() : 0x00000000);
        if (bgColor != 0) {
            context.fill(x, y + 2, x + width, y + ROW_HEIGHT - 2, bgColor);
        }

        int statusColor = task.getState().getColor();
        int dotX = x + 12;
        int dotY = y + ROW_HEIGHT / 2;

        if (task.getState() == State.RUNNING) {
            float pulse = (float) (Math.sin(pulseAnimation) * 0.3 + 0.7);
            int alpha = (int) (255 * pulse);
            int pulseColor = (alpha << 24) | (statusColor & 0x00FFFFFF);
            context.fill(dotX - 6, dotY - 6, dotX + 6, dotY + 6, pulseColor);
        }
        context.fill(dotX - 4, dotY - 4, dotX + 4, dotY + 4, statusColor);

        String idText = "#" + task.getId();
        context.drawString(font, Component.literal(idText), x + 28, y + 6, theme.getMutedText(), false);

        String name = task.getName();
        if (name.length() > 25) {
            name = name.substring(0, 22) + "...";
        }
        context.drawString(font, Component.literal(name), x + 28, y + 18, theme.getPrimaryText(), false);

        String stateText = task.getState().getDisplayName();
        context.drawString(font, Component.literal(stateText), x + 160, y + 12, statusColor, false);

        String uptime = task.getUptimeFormatted();
        context.drawString(font, Component.literal(uptime), x + 250, y + 12, theme.getMutedText(), false);

        String cmdInfo = String.valueOf(task.getExecutedCommands());
        context.drawString(font, Component.literal(cmdInfo), x + 340, y + 12, theme.getMutedText(), false);

        if (task.getLastError() != null && task.getState() == State.ERROR) {
            String error = task.getLastError();
            if (error.length() > 40) {
                error = error.substring(0, 37) + "...";
            }
            context.drawString(font, Component.literal("! " + error), x + 28, y + 28, 0xFFFF4444, false);
        }
    }

    private void renderFooter(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + FOOTER_HEIGHT, adjustBrightness(theme.getSidebarBackground(), 5));
        context.fill(dx, dy, dx + getDialogWidth(), dy + 1, theme.getPrimaryAccent() & 0x44FFFFFF);

        int buttonY = dy + 12;
        int buttonWidth = 80;
        int buttonX = dx + 16;
        int spacing = 10;

        renderButton(context, "Pause", buttonX, buttonY, buttonWidth, BUTTON_HEIGHT,
                0xFFFFAA00, mouseX, mouseY);

        buttonX += buttonWidth + spacing;
        renderButton(context, "Resume", buttonX, buttonY, buttonWidth, BUTTON_HEIGHT,
                0xFF55FF55, mouseX, mouseY);

        buttonX += buttonWidth + spacing;
        renderButton(context, "Stop All", buttonX, buttonY, buttonWidth, BUTTON_HEIGHT,
                0xFFFF4444, mouseX, mouseY);

        buttonX = dx + getDialogWidth() - buttonWidth - 16;
        renderButton(context, "Close", buttonX, buttonY, buttonWidth, BUTTON_HEIGHT,
                theme.getHover(), mouseX, mouseY);
    }

    private void renderButton(GuiGraphics context, String text, int x, int y, int w, int h, int color, int mouseX,
            int mouseY) {
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

        int closeX = dx + getDialogWidth() - 36;
        int closeY = dy + 10;
        if (mouseX >= closeX && mouseX < closeX + 24 && mouseY >= closeY && mouseY < closeY + 20) {
            this.onClose();
            return true;
        }

        int buttonY = dy + getDialogHeight() - FOOTER_HEIGHT + 12;
        int buttonWidth = 80;
        int buttonX = dx + 16;
        int spacing = 10;

        if (isButtonClicked(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY)) {
            TaskManager.getInstance().pauseAll();
            return true;
        }

        buttonX += buttonWidth + spacing;
        if (isButtonClicked(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY)) {
            TaskManager.getInstance().resumeAll();
            return true;
        }

        buttonX += buttonWidth + spacing;
        if (isButtonClicked(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY)) {
            TaskManager.getInstance().stopAll();
            return true;
        }

        buttonX = dx + getDialogWidth() - buttonWidth - 16;
        if (isButtonClicked(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, mouseX, mouseY)) {
            this.onClose();
            return true;
        }

        int tabY = dy + HEADER_HEIGHT;
        int tabWidth = getDialogWidth() / Tab.values().length;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabX = dx + i * tabWidth;
            if (mouseX >= tabX && mouseX < tabX + tabWidth &&
                    mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                currentTab = tab;
                refreshAutorunData();
                return true;
            }
        }

        int contentY = dy + HEADER_HEIGHT + TAB_HEIGHT;
        if (currentTab == Tab.PROCESSES) {
            int listY = contentY;
            int listHeight = getDialogHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT;
            if (mouseY >= listY && mouseY < listY + listHeight && mouseX >= dx && mouseX < dx + getDialogWidth()) {
                int relativeY = (int) mouseY - listY + scrollY;
                int index = relativeY / ROW_HEIGHT;

                if (index >= 0 && index < tasks.size()) {
                    selectedTaskId = tasks.get(index).getId();
                    return true;
                }
            }
        } else {
            return handleAutorunClick(mouseX, mouseY, dx, contentY);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isButtonClicked(int x, int y, int w, int h, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = getDialogHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT;

        if (currentTab == Tab.PROCESSES) {
            int maxScroll = Math.max(0, tasks.size() * ROW_HEIGHT - listHeight);
            scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int) (verticalAmount * ROW_HEIGHT)));
        } else {
            int maxScroll = Math.max(0, Math.max(autorunScripts.size(), allScripts.size()) * ROW_HEIGHT - listHeight);
            autorunScrollY = Math.max(0, Math.min(maxScroll, autorunScrollY - (int) (verticalAmount * ROW_HEIGHT)));
        }
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void refreshTasks() {
        Collection<Task> allTasks = TaskManager.getInstance().getAllTasks();
        tasks = new ArrayList<>(allTasks);
    }

    private void refreshAutorunData() {
        KashubConfig config = KashubConfig.getInstance();
        autorunScripts = new ArrayList<>(config.autorunScripts);
        allScripts = new ArrayList<>(ScriptManager.getAllScripts());
        allScripts.removeAll(autorunScripts);

        if (selectedAutorunIndex >= autorunScripts.size()) {
            selectedAutorunIndex = autorunScripts.isEmpty() ? -1 : autorunScripts.size() - 1;
        }
        if (selectedAvailableIndex >= allScripts.size()) {
            selectedAvailableIndex = allScripts.isEmpty() ? -1 : allScripts.size() - 1;
        }
    }

    private void renderAutorunPanel(GuiGraphics context, int dx, int dy, int mouseX, int mouseY, float delta) {
        int panelHeight = getDialogHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT;
        int panelWidth = getDialogWidth() - 16;
        int panelX = dx + 8;

        int leftWidth = (panelWidth - 20) / 2;
        int rightWidth = panelWidth - leftWidth - 20;
        int rightX = panelX + leftWidth + 20;

        renderAutorunList(context, panelX, dy, leftWidth, panelHeight, mouseX, mouseY, true);
        renderAutorunList(context, rightX, dy, rightWidth, panelHeight, mouseX, mouseY, false);

        int centerX = panelX + leftWidth + 4;
        int centerY = dy + panelHeight / 2 - 20;

        boolean addHovered = mouseX >= centerX && mouseX < centerX + 32 &&
                mouseY >= centerY && mouseY < centerY + 24;
        context.fill(centerX, centerY, centerX + 32, centerY + 24,
                addHovered ? 0xFF55FF55 : theme.getHover());
        context.drawString(font, Component.literal("->"), centerX + 10, centerY + 8, 0xFFFFFFFF, false);

        boolean removeHovered = mouseX >= centerX && mouseX < centerX + 32 &&
                mouseY >= centerY + 28 && mouseY < centerY + 52;
        context.fill(centerX, centerY + 28, centerX + 32, centerY + 52,
                removeHovered ? 0xFFFF4444 : theme.getHover());
        context.drawString(font, Component.literal("<-"), centerX + 10, centerY + 36, 0xFFFFFFFF, false);
    }

    private void renderAutorunList(GuiGraphics context, int x, int y, int width, int height,
            int mouseX, int mouseY, boolean isAutorunList) {
        List<String> scripts = isAutorunList ? autorunScripts : allScripts;
        String title = isAutorunList ? "Autorun" : "Available";

        context.fill(x, y, x + width, y + 24, adjustBrightness(theme.getSidebarBackground(), 5));
        context.drawString(font, Component.literal(title), x + 8, y + 8, theme.getPrimaryText(), false);
        context.fill(x, y + 23, x + width, y + 24, theme.getPrimaryAccent() & 0x44FFFFFF);

        int listY = y + 24;
        int listHeight = height - 24;

        if (scripts.isEmpty()) {
            String message = "Empty";
            int textWidth = font.width(message);
            context.drawString(font, Component.literal(message), x + (width - textWidth) / 2,
                    listY + listHeight / 2, theme.getMutedText(), false);
            return;
        }

        if (isAutorunList) {
            hoveredAutorunIndex = -1;
        } else {
            hoveredAvailableIndex = -1;
        }

        int visibleRows = listHeight / ROW_HEIGHT;
        int startIndex = autorunScrollY / ROW_HEIGHT;
        int endIndex = Math.min(startIndex + visibleRows + 1, scripts.size());

        for (int i = startIndex; i < endIndex; i++) {
            String script = scripts.get(i);
            int rowY = listY + (i * ROW_HEIGHT) - autorunScrollY;

            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight)
                continue;

            if (mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                if (isAutorunList) {
                    hoveredAutorunIndex = i;
                } else {
                    hoveredAvailableIndex = i;
                }
            }

            boolean isHovered = (isAutorunList && hoveredAutorunIndex == i) ||
                    (!isAutorunList && hoveredAvailableIndex == i);
            boolean isSelected = (isAutorunList && selectedAutorunIndex == i) ||
                    (!isAutorunList && selectedAvailableIndex == i);

            if (isSelected) {
                context.fill(x + 2, rowY + 2, x + width - 2, rowY + ROW_HEIGHT - 2, theme.getPrimaryAccent() & 0x66FFFFFF);
            } else if (isHovered) {
                context.fill(x + 2, rowY + 2, x + width - 2, rowY + ROW_HEIGHT - 2, theme.getHover());
            }

            String displayName = script;
            if (displayName.length() > 20) {
                displayName = displayName.substring(0, 17) + "...";
            }
            context.drawString(font, Component.literal(displayName), x + 8, rowY + 14, theme.getPrimaryText(), false);
        }
    }

    private boolean handleAutorunClick(double mouseX, double mouseY, int dx, int dy) {
        int panelHeight = getDialogHeight() - HEADER_HEIGHT - TAB_HEIGHT - FOOTER_HEIGHT;
        int panelWidth = getDialogWidth() - 16;
        int panelX = dx + 8;
        int leftWidth = (panelWidth - 20) / 2;
        int rightWidth = panelWidth - leftWidth - 20;
        int rightX = panelX + leftWidth + 20;
        int centerX = panelX + leftWidth + 4;
        int centerY = dy + panelHeight / 2 - 20;

        int listY = dy + 24;
        int listHeight = panelHeight - 24;

        long currentTime = System.currentTimeMillis();

        if (mouseX >= panelX && mouseX < panelX + leftWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {
            int relativeY = (int) mouseY - listY + autorunScrollY;
            int index = relativeY / ROW_HEIGHT;
            if (index >= 0 && index < autorunScripts.size()) {
                if (lastClickWasAutorun && lastClickedIndex == index &&
                        currentTime - lastClickTime < 400) {
                    String script = autorunScripts.get(index);
                    KashubConfig config = KashubConfig.getInstance();
                    config.autorunScripts.remove(script);
                    config.save();
                    refreshAutorunData();
                    lastClickTime = 0;
                } else {
                    selectedAutorunIndex = index;
                    lastClickTime = currentTime;
                    lastClickedIndex = index;
                    lastClickWasAutorun = true;
                }
                return true;
            }
        }

        if (mouseX >= rightX && mouseX < rightX + rightWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {
            int relativeY = (int) mouseY - listY + autorunScrollY;
            int index = relativeY / ROW_HEIGHT;
            if (index >= 0 && index < allScripts.size()) {
                if (!lastClickWasAutorun && lastClickedIndex == index &&
                        currentTime - lastClickTime < 400) {
                    String script = allScripts.get(index);
                    KashubConfig config = KashubConfig.getInstance();
                    if (!config.autorunScripts.contains(script)) {
                        config.autorunScripts.add(script);
                        config.save();
                        refreshAutorunData();
                    }
                    lastClickTime = 0;
                } else {
                    selectedAvailableIndex = index;
                    lastClickTime = currentTime;
                    lastClickedIndex = index;
                    lastClickWasAutorun = false;
                }
                return true;
            }
        }

        if (mouseX >= centerX && mouseX < centerX + 32 &&
                mouseY >= centerY && mouseY < centerY + 24) {
            int indexToAdd = selectedAvailableIndex >= 0 ? selectedAvailableIndex : hoveredAvailableIndex;
            if (indexToAdd >= 0 && indexToAdd < allScripts.size()) {
                String script = allScripts.get(indexToAdd);
                KashubConfig config = KashubConfig.getInstance();
                if (!config.autorunScripts.contains(script)) {
                    config.autorunScripts.add(script);
                    config.save();
                    refreshAutorunData();
                }
                return true;
            }
        }

        if (mouseX >= centerX && mouseX < centerX + 32 &&
                mouseY >= centerY + 28 && mouseY < centerY + 52) {
            int indexToRemove = selectedAutorunIndex >= 0 ? selectedAutorunIndex : hoveredAutorunIndex;
            if (indexToRemove >= 0 && indexToRemove < autorunScripts.size()) {
                String script = autorunScripts.get(indexToRemove);
                KashubConfig config = KashubConfig.getInstance();
                config.autorunScripts.remove(script);
                config.save();
                refreshAutorunData();
                return true;
            }
        }
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
}

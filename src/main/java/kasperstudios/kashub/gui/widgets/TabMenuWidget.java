package kasperstudios.kashub.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tab menu widget for switching between different views.
 * Renders ItemStack icons instead of text for compactness.
 */
public class TabMenuWidget extends ClickableWidget {

    public static class Tab {
        public final ItemStack icon;
        public final Text tooltip;

        public Tab(ItemStack icon, Text tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }

    public interface TabChangeListener {
        void onTabChanged(int newTab);
    }

    private final List<Tab> tabs;
    private final Map<Integer, Boolean> enabled = new HashMap<>();
    private final Map<Integer, Text> tooltips = new HashMap<>();
    private int activeTab = 0;
    private final TabChangeListener listener;

    public TabMenuWidget(int x, int y, int width, int height, List<Tab> tabs, TabChangeListener listener) {
        super(x, y, width, height, Text.empty());
        this.tabs = tabs;
        this.listener = listener;

        // All tabs enabled by default
        for (int i = 0; i < tabs.size(); i++) {
            enabled.put(i, true);
        }
    }

    public void setActiveTab(int index) {
        this.activeTab = index;
    }

    public void setTabEnabled(int index, boolean value) {
        enabled.put(index, value);
    }

    public void setTabTooltip(int index, Text tooltip) {
        tooltips.put(index, tooltip);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int tabWidth = this.width / this.tabs.size();

        for (int i = 0; i < tabs.size(); i++) {
            int tabX = this.getX() + i * tabWidth;
            boolean isActive = i == activeTab;
            boolean isEnabled = enabled.getOrDefault(i, true);
            boolean isHovered = mouseX >= tabX && mouseX < tabX + tabWidth
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;

            // Background color
            int bg;
            if (!isEnabled) {
                bg = 0xFF111827; // Dark gray for disabled
            } else if (isActive) {
                bg = 0xFF7c3aed; // Purple for active
            } else if (isHovered) {
                bg = 0xFF5b21b6; // Darker purple for hover
            } else {
                bg = 0xFF1e1b4b; // Very dark purple for inactive
            }

            context.fill(tabX, this.getY(), tabX + tabWidth, this.getY() + this.height, bg);

            // Draw Icon
            Tab tab = tabs.get(i);
            int iconX = tabX + (tabWidth - 16) / 2;
            int iconY = this.getY() + (this.height - 16) / 2;

            context.drawItem(tab.icon, iconX, iconY);

            // Draw overlay if disabled
            if (!isEnabled) {
                context.fill(tabX, this.getY(), tabX + tabWidth, this.getY() + this.height, 0xAA000000);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        int tabWidth = this.width / this.tabs.size();
        int clicked = (int) ((mouseX - this.getX()) / tabWidth);

        if (clicked < 0 || clicked >= tabs.size()) {
            return false;
        }

        if (!enabled.getOrDefault(clicked, true)) {
            return false; // Disabled tab - no action
        }

        if (activeTab != clicked) {
            activeTab = clicked;
            if (listener != null) {
                listener.onTabChanged(clicked);
            }
        }

        return true;
    }

    @Override
    protected void appendClickableNarrations(
            net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        if (activeTab >= 0 && activeTab < tabs.size()) {
            builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE,
                    Text.literal("Tab menu, active: ").append(tabs.get(activeTab).tooltip));
        }
    }

    @Nullable
    public Text getHoveredTooltip(int mouseX, int mouseY) {
        int tabWidth = this.width / this.tabs.size();

        for (int i = 0; i < tabs.size(); i++) {
            int tabX = this.getX() + i * tabWidth;
            boolean isHovered = mouseX >= tabX && mouseX < tabX + tabWidth
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;

            if (isHovered) {
                // Return dynamic tooltip if set, otherwise tab tooltip
                if (tooltips.containsKey(i)) {
                    return tooltips.get(i);
                }
                return tabs.get(i).tooltip;
            }
        }

        return null;
    }
}

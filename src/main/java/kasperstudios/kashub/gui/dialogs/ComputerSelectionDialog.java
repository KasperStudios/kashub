package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import kasperstudios.kashub.services.integration.CCTBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A selection dialog for Neural Link computer connections.
 */
public class ComputerSelectionDialog extends Screen {
    private final Screen parent;
    private final List<CCTBridge.DiscoveredComputer> computers;
    private final java.util.function.Consumer<CCTBridge.DiscoveredComputer> onSelect;
    private EditorTheme theme;
    
    private int hoveredIndex = -1;
    private static final int ITEM_HEIGHT = 28;

    public ComputerSelectionDialog(Screen parent, List<CCTBridge.DiscoveredComputer> computers, java.util.function.Consumer<CCTBridge.DiscoveredComputer> onSelect) {
        super(Component.literal("Neural Link Selection"));
        this.parent = parent;
        this.computers = computers;
        this.onSelect = onSelect;
        this.theme = ThemeManager.getCurrentTheme();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000); // Dark background

        int dialogWidth = this.width;
        int dialogHeight = this.height;
        int x = 0;
        int y = 0;

        // Dialog background
        context.fill(x, y, x + dialogWidth, y + dialogHeight, theme.sidebarColor);
        context.fill(x, y, x + dialogWidth, y + 2, theme.accentColor); // Top accent

        context.drawString(this.font, "SELECT COMPUTER", x + 16, y + 14, theme.accentColor, true);

        // Close button
        int closeX = this.width - 30;
        int closeY = 10;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20;
        context.fill(closeX, closeY, closeX + 20, closeY + 20, closeHovered ? 0xFFFF4444 : theme.buttonColor);
        context.drawString(this.font, "X", closeX + 7, closeY + 6, 0xFFFFFFFF, false);

        hoveredIndex = -1;
        int listStartY = y + 40;
        for (int i = 0; i < computers.size(); i++) {
            int itemY = listStartY + i * ITEM_HEIGHT;
            if (itemY > this.height - ITEM_HEIGHT) break;

            boolean hovered = mouseX >= x && mouseX < x + dialogWidth && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            if (hovered) hoveredIndex = i;

            if (hovered) {
                context.fill(x, itemY, x + dialogWidth, itemY + ITEM_HEIGHT, theme.buttonHoverColor);
                context.fill(x, itemY, x + 3, itemY + ITEM_HEIGHT, theme.accentColor);
            }

            var c = computers.get(i);
            String label = c.label != null ? c.label : "Computer #" + c.id;
            context.drawString(this.font, label, x + 16, itemY + 10, hovered ? theme.textColor : theme.textDimColor, false);
            
            // Dist indicator
            int dist = (int) Math.sqrt(net.minecraft.client.Minecraft.getInstance().player.blockPosition().distSqr(c.pos));
            String distStr = dist + "m";
            context.drawString(this.font, distStr, x + dialogWidth - 24 - this.font.width(distStr), itemY + 10, 0xFF666666, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int closeX = this.width - 30;
        int closeY = 10;
        if (mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20) {
            onClose();
            return true;
        }

        if (hoveredIndex != -1) {
            onSelect.accept(computers.get(hoveredIndex));
            onClose();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        net.minecraft.client.Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package kasperstudios.kashub.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import kasperstudios.kashub.services.integration.PeripheralDocsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PeripheralScanScreen extends Screen {

    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 270;
    private static final int LEFT_PANEL_WIDTH = 160;
    private static final int HEADER_HEIGHT = 28;
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_START_OFFSET_Y = 34;
    private static final int MAX_VISIBLE_METHODS = 11;

    private final String peripheralType;
    private final List<String> methods;
    
    private int selectedMethodIndex = -1;
    private int scrollOffset = 0;

    private final Screen parent;

    public PeripheralScanScreen(String type, List<String> methods, Screen parent) {
        super(Component.literal("Peripheral Scan: " + type));
        this.peripheralType = type != null ? type : "unknown";
        this.methods = methods != null ? methods : List.of();
        this.parent = parent;
        this.scrollOffset = 0;
        if (!this.methods.isEmpty()) {
            this.selectedMethodIndex = 0;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Overridden to prevent default Minecraft background blur/dirt
    }

    private final kasperstudios.khgui.api.ITheme theme = kasperstudios.khgui.core.ThemeManager.getCurrentTheme();

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Draw custom semi-transparent backdrop
        graphics.fill(0, 0, width, height, 0xAA000000);

        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;

        // Main Panel Background with theme support
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, theme.getSidebarBackground());
        graphics.renderOutline(x, y, PANEL_WIDTH, PANEL_HEIGHT, theme.getPrimaryAccent());

        // Header
        graphics.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, theme.getHover());
        graphics.drawString(font, "§bNEURAL ANALYSIS: §f" + peripheralType.toUpperCase(), x + 12, y + 10, 0xFFFFFF, true);
        
        // Close [X] button in header
        int closeX = x + PANEL_WIDTH - 22;
        int closeY = y + 7;
        boolean closeHovered = mouseX >= closeX - 2 && mouseX <= closeX + 14 && mouseY >= closeY - 2 && mouseY <= closeY + 14;
        graphics.drawString(font, "§c[X]", closeX, closeY, closeHovered ? 0xFF5555 : 0xAAAAAA, false);

        // Scan line animation (subtle)
        float scanAnim = (System.currentTimeMillis() % 2500) / 2500f;
        int scanY = y + HEADER_HEIGHT + (int) (scanAnim * (PANEL_HEIGHT - HEADER_HEIGHT));
        graphics.fill(x + 1, scanY, x + PANEL_WIDTH - 1, scanY + 1, theme.getPrimaryAccent() & 0x44FFFFFF);

        if (methods.isEmpty()) {
            graphics.drawCenteredString(font, "§cNo callable methods identified.", x + PANEL_WIDTH / 2, y + PANEL_HEIGHT / 2, 0xFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTicks);
            return;
        }

        // Clamp scrollOffset & selection
        int maxScroll = Math.max(0, methods.size() - MAX_VISIBLE_METHODS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;
        if (selectedMethodIndex >= methods.size()) {
            selectedMethodIndex = 0;
        }

        // Left Panel: Method List
        graphics.fill(x, y + HEADER_HEIGHT, x + LEFT_PANEL_WIDTH, y + PANEL_HEIGHT, theme.getHover() & 0x66FFFFFF);
        graphics.fill(x + LEFT_PANEL_WIDTH - 1, y + HEADER_HEIGHT, x + LEFT_PANEL_WIDTH, y + PANEL_HEIGHT, theme.getPrimaryAccent() & 0x33FFFFFF);

        for (int i = 0; i < MAX_VISIBLE_METHODS; i++) {
            int dataIndex = scrollOffset + i;
            if (dataIndex >= methods.size()) break;

            String method = methods.get(dataIndex);
            int itemY = y + ITEM_START_OFFSET_Y + (i * ITEM_HEIGHT);

            boolean isHovered = mouseX >= x + 5 && mouseX <= x + LEFT_PANEL_WIDTH - 5 && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean isSelected = (dataIndex == selectedMethodIndex);

            if (isSelected) {
                graphics.fill(x + 4, itemY, x + LEFT_PANEL_WIDTH - 4, itemY + ITEM_HEIGHT - 1, theme.getPrimaryAccent() & 0x66FFFFFF);
                graphics.fill(x + 4, itemY, x + 7, itemY + ITEM_HEIGHT - 1, theme.getPrimaryAccent());
            } else if (isHovered) {
                graphics.fill(x + 4, itemY, x + LEFT_PANEL_WIDTH - 4, itemY + ITEM_HEIGHT - 1, 0x22FFFFFF);
            }

            int color = isSelected ? 0xFFFFFF : (isHovered ? 0xDDDDDD : 0xAAAAAA);
            String displayMethod = font.plainSubstrByWidth(method, LEFT_PANEL_WIDTH - 18);
            graphics.drawString(font, displayMethod, x + 10, itemY + 5, color, false);
        }

        // Scroll indicator if scrollable
        if (methods.size() > MAX_VISIBLE_METHODS) {
            int scrollbarTrackH = PANEL_HEIGHT - HEADER_HEIGHT - 10;
            int thumbH = Math.max(16, (MAX_VISIBLE_METHODS * scrollbarTrackH) / methods.size());
            int thumbY = y + HEADER_HEIGHT + 5 + (scrollOffset * (scrollbarTrackH - thumbH)) / maxScroll;
            graphics.fill(x + LEFT_PANEL_WIDTH - 4, thumbY, x + LEFT_PANEL_WIDTH - 2, thumbY + thumbH, theme.getPrimaryAccent());
        }

        // Right Panel: Snippet & Description
        int rightX = x + LEFT_PANEL_WIDTH + 14;
        int rightWidth = PANEL_WIDTH - LEFT_PANEL_WIDTH - 24;

        if (selectedMethodIndex >= 0 && selectedMethodIndex < methods.size()) {
            String selectedMethod = methods.get(selectedMethodIndex);
            graphics.drawString(font, "§eMethod: §f" + selectedMethod, rightX, y + 36, 0xFFFFFF, false);

            PeripheralDocsManager.MethodDoc doc = PeripheralDocsManager.getDoc(peripheralType, selectedMethod);
            int curY = y + 52;
            if (doc != null) {
                // Description (wrapped)
                List<net.minecraft.util.FormattedCharSequence> wrapped = font.split(Component.literal(doc.description()), rightWidth);
                for (net.minecraft.util.FormattedCharSequence fcs : wrapped) {
                    if (curY + 12 > y + PANEL_HEIGHT - 80) break;
                    graphics.drawString(font, fcs, rightX, curY, 0xCCCCCC, false);
                    curY += 12;
                }

                // Snippet box
                int snippetY = curY + 6;
                int snippetHeight = Math.max(36, y + PANEL_HEIGHT - 32 - snippetY);
                graphics.fill(rightX, snippetY, rightX + rightWidth, snippetY + snippetHeight, 0xCC000000);
                graphics.renderOutline(rightX, snippetY, rightWidth, snippetHeight, theme.getPrimaryAccent() & 0x77FFFFFF);

                String[] snippetLines = doc.snippet().split("\n");
                for (int i = 0; i < snippetLines.length; i++) {
                    if (snippetY + 6 + (i * 11) > snippetY + snippetHeight - 12) break;
                    graphics.drawString(font, snippetLines[i], rightX + 6, snippetY + 6 + (i * 11), 0x55FF55, false);
                }
            } else {
                graphics.drawString(font, "§8No documentation found.", rightX, curY, 0x888888, false);
                int snippetY = curY + 16;
                int snippetHeight = y + PANEL_HEIGHT - 32 - snippetY;
                graphics.fill(rightX, snippetY, rightX + rightWidth, snippetY + snippetHeight, 0xCC000000);
                graphics.renderOutline(rightX, snippetY, rightWidth, snippetHeight, 0x44FFFFFF);

                String cleanMethod = selectedMethod.endsWith("()") ? selectedMethod.substring(0, selectedMethod.length() - 2) : selectedMethod;
                graphics.drawString(font, "peripheral.call(\"top\", \"" + cleanMethod + "\")", rightX + 6, snippetY + 8, 0x55FF55, false);
            }

            // Footer instructions
            String inst = "§b[L-Click]§7 Copy Call  §b[R-Click]§7 Copy Snippet";
            graphics.drawCenteredString(font, inst, x + LEFT_PANEL_WIDTH + (PANEL_WIDTH - LEFT_PANEL_WIDTH) / 2, y + PANEL_HEIGHT - 16, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (methods.size() > MAX_VISIBLE_METHODS) {
            int maxScroll = methods.size() - MAX_VISIBLE_METHODS;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;

        // Close button [X]
        int closeX = x + PANEL_WIDTH - 22;
        int closeY = y + 7;
        if (mouseX >= closeX - 4 && mouseX <= closeX + 16 && mouseY >= closeY - 2 && mouseY <= closeY + 16) {
            onClose();
            return true;
        }

        // Check clicking on method list in left panel
        if (mouseX >= x + 4 && mouseX <= x + LEFT_PANEL_WIDTH - 4) {
            for (int i = 0; i < MAX_VISIBLE_METHODS; i++) {
                int itemY = y + ITEM_START_OFFSET_Y + (i * ITEM_HEIGHT);
                if (mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    int dataIndex = scrollOffset + i;
                    if (dataIndex < methods.size()) {
                        selectedMethodIndex = dataIndex;
                        
                        String method = methods.get(selectedMethodIndex);
                        String copyText = "";
                        
                        if (button == 0) { // Left click = direct call
                            String cleanMethod = method.endsWith("()") ? method.substring(0, method.length() - 2) : method;
                            copyText = "peripheral.call(\"top\", \"" + cleanMethod + "\")";
                        } else if (button == 1) { // Right click = full snippet
                            PeripheralDocsManager.MethodDoc doc = PeripheralDocsManager.getDoc(peripheralType, method);
                            if (doc != null) {
                                copyText = doc.snippet();
                            } else {
                                String cleanMethod = method.endsWith("()") ? method.substring(0, method.length() - 2) : method;
                                copyText = "peripheral.call(\"top\", \"" + cleanMethod + "\")";
                            }
                        }

                        if (!copyText.isEmpty()) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(copyText);
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.displayClientMessage(Component.literal("§aCopied: " + copyText), true);
                            }
                        }
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}

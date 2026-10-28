package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.types.KHType;
import kasperstudios.khgui.core.ThemeManager;
import kasperstudios.kashub.util.DocumentationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import kasperstudios.khgui.api.ITheme;
import java.util.*;

/**
 * Ported high-fidelity Documentation Dialog.
 * Adapted for KHGui and Fabric 1.21.1 (Yarn).
 * Emoji-free professional UI.
 */
public class DocsDialog extends Screen {
    private final Screen parent;
    private final ITheme theme;

    private static List<DocSection> cachedSections = null;
    private static Map<String, List<Command>> cachedCategories = null;

    private List<DocSection> sections = new ArrayList<>();
    private List<DocSection> filteredSections = new ArrayList<>();
    private int selectedSection = 0;
    private int scrollY = 0;
    private int scrollX = 0;
    private int sidebarScrollY = 0;

    private String searchQuery = "";
    private EditBox searchField;

    private static final int SIDEBAR_WIDTH = 160;
    private static final int HEADER_HEIGHT = 70;
    private static final int SEARCH_HEIGHT = 24;

    private int getDialogWidth() {
        return this.width;
    }

    private int getDialogHeight() {
        return this.height;
    }

    public DocsDialog(Screen parent) {
        super(Component.literal("KHScript Documentation"));
        this.parent = parent;
        this.theme = ThemeManager.getCurrentTheme();
        this.font = Minecraft.getInstance().font;
        initializeDocs();
    }

    public static void clearCache() {
        cachedSections = null;
        cachedCategories = null;
    }

    private void initializeDocs() {
        DocumentationManager.initialize();
        sections.clear();
        for (DocumentationManager.DocSection ds : DocumentationManager.getAllSections()) {
            sections.add(new DocSection(ds.title, ds.id, ds.icon, ds.lines));
        }
        filteredSections = new ArrayList<>(sections);
    }

    private void filterSections(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredSections = new ArrayList<>(sections);
            selectedSection = 0;
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        filteredSections = new ArrayList<>();

        List<String> matchingLines = new ArrayList<>();
        matchingLines.add("# 🔍 Search Results for \"" + query + "\"");
        matchingLines.add("");

        int matchCount = 0;

        // 1. Search in all Objects and their methods
        for (DocSection ds : sections) {
            if (ds.id.startsWith("obj_")) {
                boolean objMatched = ds.title.toLowerCase().contains(lowerQuery);
                List<String> matchedMethods = new ArrayList<>();
                for (String line : ds.lines) {
                    if (line.toLowerCase().contains(lowerQuery)) {
                        matchedMethods.add(line);
                    }
                }
                if (objMatched || !matchedMethods.isEmpty()) {
                    matchCount++;
                    matchingLines.add("## 📦 Object: " + ds.title);
                    for (String m : matchedMethods) {
                        matchingLines.add(m);
                    }
                    matchingLines.add("");
                }
            }
        }

        // 2. Search in Commands registry
        for (Command cmd : Registry.getAllCommands()) {
            kasperstudios.kashub.core.Metadata meta = cmd.getMetadata();
            boolean matches = meta.name.toLowerCase().contains(lowerQuery) ||
                            meta.description.toLowerCase().contains(lowerQuery) ||
                            (meta.syntax != null && meta.syntax.toLowerCase().contains(lowerQuery));

            if (matches) {
                matchCount++;
                matchingLines.add("## ⚙ Command: " + meta.name);
                matchingLines.add(meta.description);
                if (meta.syntax != null && !meta.syntax.isEmpty()) {
                    matchingLines.add("Usage: " + meta.syntax);
                }
                matchingLines.add("");
            }
        }

        if (matchCount > 0) {
            matchingLines.add(1, "Found " + matchCount + " match(es):");
            filteredSections.add(new DocSection("Results (" + matchCount + ")", "Search", "🔍", matchingLines));
            selectedSection = 0;
        } else {
            matchingLines.add("No methods or commands found matching \"" + query + "\"");
            matchingLines.add("");
            matchingLines.add("Try searching for player, move, attack, block, health, gamma, scan...");
            filteredSections.add(new DocSection("No Results", "Search", "🔍", matchingLines));
            selectedSection = 0;
        }
        scrollY = 0;
    }

    @Override
    protected void init() {
        super.init();
        initializeDocs(); // Refresh docs when opened

        int dx = getDialogX();
        int dy = getDialogY();

        searchField = new EditBox(
            font,
            dx + SIDEBAR_WIDTH + 10,
            dy + HEADER_HEIGHT - SEARCH_HEIGHT - 8,
            getDialogWidth() - SIDEBAR_WIDTH - 20,
            SEARCH_HEIGHT - 4,
            Component.literal("Search...")
        );
        searchField.setResponder(query -> {
            searchQuery = query;
            filterSections(query);
        });
        addRenderableWidget(searchField);
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
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int dx = getDialogX();
        int dy = getDialogY();

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.getSidebarBackground());

        int accent = theme.getPrimaryAccent();
        // Remove borders as it is now full screen

        renderHeader(context, dx, dy, mouseX, mouseY);
        renderSidebar(context, dx, dy + HEADER_HEIGHT, mouseX, mouseY);
        renderContent(context, dx + SIDEBAR_WIDTH, dy + HEADER_HEIGHT, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + HEADER_HEIGHT, adjustBrightness(theme.getSidebarBackground(), 10));
        context.drawString(font, Component.literal("KHSCRIPT DOCUMENTATION"), dx + 16, dy + 14, theme.getPrimaryText(), true);
        
        long objCount = sections.stream().filter(s -> s.id.startsWith("obj_")).count();
        int cmdCount = Registry.getAllCommands().size();
        String stats = objCount + " Core Objects • " + cmdCount + " Commands • Full API";
        context.drawString(font, Component.literal(stats), dx + 16, dy + 28, theme.getMutedText(), false);

        int closeX = dx + getDialogWidth() - 30;
        int closeY = dy + 10;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 20 && mouseY >= closeY && mouseY < closeY + 20;
        context.fill(closeX, closeY, closeX + 20, closeY + 20, closeHovered ? 0xFFFF4444 : theme.getHover());
        context.drawString(font, Component.literal("X"), closeX + 7, closeY + 6, 0xFFFFFFFF, false);

        context.fill(dx, dy + HEADER_HEIGHT - 1, dx + getDialogWidth(), dy + HEADER_HEIGHT, theme.getPrimaryAccent() & 0x66FFFFFF);
    }

    private void renderSidebar(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        int contentHeight = getDialogHeight() - HEADER_HEIGHT;
        context.fill(dx, dy, dx + SIDEBAR_WIDTH, dy + contentHeight, adjustBrightness(theme.getSidebarBackground(), -5));
        context.fill(dx + SIDEBAR_WIDTH - 1, dy, dx + SIDEBAR_WIDTH, dy + contentHeight, theme.getPrimaryAccent() & 0x44FFFFFF);

        int itemHeight = 22;
        List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;

        for (int i = 0; i < displaySections.size(); i++) {
            int itemY = dy + i * itemHeight + 8 - sidebarScrollY;
            if (itemY < dy - itemHeight || itemY > dy + contentHeight) continue;

            boolean hovered = mouseX >= dx && mouseX < dx + SIDEBAR_WIDTH && mouseY >= itemY && mouseY < itemY + itemHeight;
            boolean selected = i == selectedSection;

            if (selected) {
                context.fill(dx, itemY, dx + SIDEBAR_WIDTH - 1, itemY + itemHeight, theme.getPrimaryAccent() & 0x44FFFFFF);
                context.fill(dx, itemY, dx + 3, itemY + itemHeight, theme.getPrimaryAccent());
            } else if (hovered) {
                context.fill(dx, itemY, dx + SIDEBAR_WIDTH - 1, itemY + itemHeight, theme.getHover());
            }

            int textColor = selected ? theme.getPrimaryAccent() : (hovered ? theme.getPrimaryText() : theme.getMutedText());
            DocSection sec = displaySections.get(i);
            String iconPrefix = (sec.icon != null && !sec.icon.isEmpty()) ? sec.icon + " " : "";
            String title = iconPrefix + sec.title;
            if (font.width(title) > SIDEBAR_WIDTH - 20) {
                title = font.plainSubstrByWidth(title, SIDEBAR_WIDTH - 25) + "...";
            }
            context.drawString(font, Component.literal(title), dx + 10, itemY + 6, textColor, false);
        }
    }

    private String cleanMarkdown(String line) {
        if (line == null || line.isEmpty()) return "";
        // Replace markdown links with bold: [**Text**](link) -> Text
        String s = line.replaceAll("\\[\\*\\*(.*?)\\*\\*\\]\\(.*?\\)", "$1");
        // Replace regular markdown links: [Text](link) -> Text
        s = s.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
        // Strip bold markers: **Text** -> Text
        s = s.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        // Strip inline code markers: `Text` -> Text
        s = s.replaceAll("`([^`]+)`", "$1");
        // Clean table separator rows like |:---|:---|
        if (s.matches("^\\|\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)+\\|$")) {
            return "";
        }
        // Clean table borders
        if (s.startsWith("|") && s.endsWith("|")) {
            s = s.substring(1, s.length() - 1).replace("|", " • ").trim();
        }
        return s;
    }

    private void renderContent(GuiGraphics context, int dx, int dy, int mouseX, int mouseY) {
        int contentWidth = getDialogWidth() - SIDEBAR_WIDTH;
        int contentHeight = getDialogHeight() - HEADER_HEIGHT;

        List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
        if (selectedSection >= displaySections.size()) selectedSection = 0;
        if (displaySections.isEmpty()) return;

        DocSection section = displaySections.get(selectedSection);
        int lineHeight = 12;
        int y = dy + 14 - scrollY;
        boolean inCodeBlock = false;

        for (String line : section.lines) {
            int color = theme.getPrimaryText();
            int xOffset = 14 - scrollX;
            String processedLine = cleanMarkdown(line);
            if (processedLine.isEmpty() && !line.isEmpty()) {
                continue; // Skip empty stripped table markers
            }
            boolean isBold = line.contains("**");
            boolean isScan = false;

            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                y += 4;
                continue;
            }

            if (inCodeBlock) {
                // Render code block line with dark background strip
                int codeBg = 0x55000000;
                int codeBorder = theme.getPrimaryAccent() & 0x88FFFFFF;
                if (y > dy - lineHeight && y < dy + contentHeight) {
                    context.fill(dx + xOffset - 4, y - 1, dx + contentWidth - 20, y + lineHeight - 1, codeBg);
                    context.fill(dx + xOffset - 4, y - 1, dx + xOffset - 2, y + lineHeight - 1, codeBorder);
                    context.drawString(font, Component.literal(line), dx + xOffset + 4, y, 0xFFE0E0E0, false);
                }
                y += lineHeight;
                continue;
            }

            if (line.startsWith("# ")) {
                color = theme.getPrimaryAccent();
                processedLine = line.substring(2);
                isBold = true;
                y += 4;
            } else if (line.startsWith("## ")) {
                color = 0xFF68D391; // Light green H2
                processedLine = line.substring(3);
                isBold = true;
                y += 6;
            } else if (line.startsWith("### ")) {
                color = 0xFFF6AD55; // Warm gold H3
                processedLine = line.substring(4);
                isBold = true;
                y += 4;
            } else if (line.startsWith("//") || line.startsWith("  ")) {
                color = theme.getMutedText();
            } else if (line.startsWith("*") || line.startsWith("- ") || line.startsWith("• ")) {
                color = 0xFFE2E8F0;
                xOffset += 8;
            } else if (line.startsWith("Usage:")) {
                color = theme.getPrimaryAccent();
                xOffset += 4;
            } else if (line.startsWith("[SCAN:")) {
                isScan = true;
            }

            if (isScan) {
                String type = line.substring(6, line.length() - 1);
                String btnText = " 🔍 VIEW SCAN: " + type.toUpperCase();
                int btnWidth = font.width(btnText) + 12;
                if (y > dy - 16 && y < dy + contentHeight) {
                    boolean isHovered = mouseX >= dx + xOffset && mouseX <= dx + xOffset + btnWidth && mouseY >= y && mouseY <= y + 16;
                    context.fill(dx + xOffset, y, dx + xOffset + btnWidth, y + 16, isHovered ? theme.getPrimaryAccent() : (theme.getPrimaryAccent() & 0x66FFFFFF));
                    context.renderOutline(dx + xOffset, y, btnWidth, 16, theme.getPrimaryAccent());
                    context.drawString(font, Component.literal(btnText), dx + xOffset + 6, y + 4, 0xFFFFFF, false);
                }
                y += 22;
            } else {
                List<net.minecraft.util.FormattedCharSequence> wrappedLines = font.split(Component.literal(processedLine), contentWidth - 36);
                for (net.minecraft.util.FormattedCharSequence wrapped : wrappedLines) {
                    if (y > dy - lineHeight && y < dy + contentHeight) {
                        context.drawString(font, wrapped, dx + xOffset, y, color, isBold);
                    }
                    y += lineHeight;
                }
                if (isBold) y += 3;
            }
        }

        int totalHeight = calculateSectionHeight(section) + 48;
        if (totalHeight > contentHeight) {
            int scrollbarHeight = Math.max(20, (contentHeight * contentHeight) / totalHeight);
            int maxScroll = totalHeight - contentHeight;
            int scrollbarY = dy + (scrollY * (contentHeight - scrollbarHeight)) / Math.max(1, maxScroll);
            context.fill(dx + contentWidth - 6, dy, dx + contentWidth - 2, dy + contentHeight, 0x22FFFFFF);
            context.fill(dx + contentWidth - 6, scrollbarY, dx + contentWidth - 2, scrollbarY + scrollbarHeight, theme.getPrimaryAccent());
        }
    }

    private int calculateSectionHeight(DocSection section) {
        int contentWidth = getDialogWidth() - SIDEBAR_WIDTH;
        int lineHeight = 12;
        int height = 16;
        boolean inCode = false;
        for (String line : section.lines) {
            if (line.trim().startsWith("```")) {
                inCode = !inCode;
                height += 4;
                continue;
            }
            if (inCode) {
                height += lineHeight;
                continue;
            }
            if (line.startsWith("[SCAN:")) {
                height += 22;
            } else {
                String processedLine = cleanMarkdown(line);
                if (processedLine.isEmpty() && !line.isEmpty()) continue;
                if (processedLine.startsWith("# ")) processedLine = processedLine.substring(2);
                else if (processedLine.startsWith("## ")) processedLine = processedLine.substring(3);
                else if (processedLine.startsWith("### ")) processedLine = processedLine.substring(4);
                
                int wrappedCount = font.split(Component.literal(processedLine), contentWidth - 36).size();
                height += wrappedCount * lineHeight;
                if (line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")) height += 4;
            }
        }
        return height;
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

        int sidebarY = dy + HEADER_HEIGHT;
        int itemHeight = 22;
        List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
        if (mouseX >= dx && mouseX < dx + SIDEBAR_WIDTH && mouseY >= sidebarY) {
            int index = (int) ((mouseY - sidebarY - 8 + sidebarScrollY) / itemHeight);
            if (index >= 0 && index < displaySections.size()) {
                selectedSection = index;
                scrollY = 0;
                scrollX = 0;
                return true;
            }
        }

        // Check for clicks on [SCAN:] buttons
        // NOTE: y calculation must exactly mirror renderContent() for hit detection to work.
        if (mouseX >= dx + SIDEBAR_WIDTH && mouseX < dx + getDialogWidth()) {
            if (selectedSection < displaySections.size()) {
                DocSection section = displaySections.get(selectedSection);
                int contentWidth = getDialogWidth() - SIDEBAR_WIDTH;
                // Start y matches renderContent: dy + HEADER_HEIGHT + 12 - scrollY
                int y = dy + HEADER_HEIGHT + 12 - scrollY;
                int lineHeight = 12;
                int contentAreaY = dy + HEADER_HEIGHT;
                int contentHeight = getDialogHeight() - HEADER_HEIGHT;

                for (String line : section.lines) {
                    int xOffset = 12 - scrollX;
                    if (line.startsWith("[SCAN:")) {
                        // SCAN button: height is 22 (matches renderContent y += 22)
                        if (y > contentAreaY - 22 && y < contentAreaY + contentHeight) {
                            String type = line.substring(6, line.length() - 1);
                            String btnText = " \uD83D\uDD0D VIEW SCAN: " + type.toUpperCase();
                            int btnWidth = font.width(btnText) + 12;
                            if (mouseX >= dx + SIDEBAR_WIDTH + xOffset
                                    && mouseX <= dx + SIDEBAR_WIDTH + xOffset + btnWidth
                                    && mouseY >= y && mouseY <= y + 16) {
                                java.util.List<String> methods = kasperstudios.kashub.services.integration.ScannedPeripheralManager.getInstance().getAllScanned().get(type);
                                if (methods != null) {
                                    Minecraft.getInstance().setScreen(new kasperstudios.kashub.gui.screen.PeripheralScanScreen(type, methods, this));
                                }
                                return true;
                            }
                        }
                        y += 22;
                    } else {
                        // Text lines: account for wrapping exactly like renderContent
                        String processedLine = line.startsWith("# ") ? line.substring(2)
                                : (line.startsWith("## ") ? line.substring(3) : line);
                        java.util.List<net.minecraft.util.FormattedCharSequence> wrappedLines =
                                font.split(net.minecraft.network.chat.Component.literal(processedLine), contentWidth - 30);
                        int wrappedCount = Math.max(1, wrappedLines.size());
                        y += wrappedCount * lineHeight;
                        if (line.startsWith("# ") || line.startsWith("## ")) y += 4;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int dx = getDialogX();
        int dy = getDialogY();
        if (mouseX >= dx && mouseX < dx + SIDEBAR_WIDTH) {
            List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
            int totalHeight = displaySections.size() * 22 + 16;
            int contentHeight = getDialogHeight() - HEADER_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - contentHeight);
            sidebarScrollY = Math.max(0, Math.min(maxScroll, sidebarScrollY - (int)(verticalAmount * 22)));
            return true;
        }

        if (mouseX >= dx + SIDEBAR_WIDTH && mouseX < dx + getDialogWidth()) {
            int contentHeight = getDialogHeight() - HEADER_HEIGHT;
            List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
            if (selectedSection < displaySections.size()) {
                DocSection section = displaySections.get(selectedSection);
                int totalHeight = calculateSectionHeight(section) + 48;
                int maxScroll = Math.max(0, totalHeight - contentHeight);
                scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int)(verticalAmount * 36)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static class DocSection {
        final String title;
        final String id;
        final String icon;
        final List<String> lines;
        DocSection(String title, String id, String icon, List<String> lines) {
            this.title = title;
            this.id = id;
            this.icon = icon;
            this.lines = new ArrayList<>(lines);
        }
    }
}

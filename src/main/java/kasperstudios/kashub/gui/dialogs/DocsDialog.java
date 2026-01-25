package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.types.KHType;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;

public class DocsDialog extends Screen {
    private final Screen parent;
    private EditorTheme theme;
    private final TextRenderer textRenderer;

    private static List<DocSection> cachedSections = null;
    private static Map<String, List<Command>> cachedCategories = null;

    private List<DocSection> sections = new ArrayList<>();
    private List<DocSection> filteredSections = new ArrayList<>();
    private int selectedSection = 0;
    private int scrollY = 0;
    private int scrollX = 0;
    private int sidebarScrollY = 0;

    private String searchQuery = "";
    private TextFieldWidget searchField;

    private static final int SIDEBAR_WIDTH = 160;
    private static final int HEADER_HEIGHT = 70;
    private static final int SEARCH_HEIGHT = 24;

    private int getDialogWidth() {
        return Math.min(700, this.width - 40);
    }

    private int getDialogHeight() {
        return Math.min(500, this.height - 40);
    }

    public DocsDialog(Screen parent) {
        super(Text.literal("KHScript Documentation"));
        this.parent = parent;
        this.theme = ThemeManager.getCurrentTheme();
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        initializeDocs();
    }

    public static void clearCache() {
        cachedSections = null;
        cachedCategories = null;
    }

    private void initializeDocs() {
        if (cachedSections != null) {
            sections = new ArrayList<>(cachedSections);
            filteredSections = new ArrayList<>(sections);
            return;
        }

        sections.clear();

        sections.add(new DocSection("📖 Overview", "Overview", Arrays.asList(
            "# KHScript Language",
            "",
            "Soon.."
        )));

        Map<String, List<Command>> categories = getCommandsByCategory();

        String[] categoryOrder = {"Movement", "Interaction", "Inventory", "Output", "Timing", "Vision", "Sound", "Scanner", "Network", "Other"};

        for (String category : categoryOrder) {
            List<Command> cmds = categories.get(category);
            if (cmds != null && !cmds.isEmpty()) {
                sections.add(createCategorySection(category, cmds));
            }
        }

        for (Map.Entry<String, List<Command>> entry : categories.entrySet()) {
            if (!Arrays.asList(categoryOrder).contains(entry.getKey())) {
                sections.add(createCategorySection(entry.getKey(), entry.getValue()));
            }
        }

        sections.add(createAllCommandsSection());

        sections.add(createObjectsSection());

        sections.add(createTypesSection());

        sections.add(new DocSection("⌨ Shortcuts", "Shortcuts", Arrays.asList(
            "# Editor Shortcuts",
            "",
            "## File",
            "Ctrl+S - Save",
            "Ctrl+N - New script",
            "F5 - Run script",
            "",
            "## Edit",
            "Ctrl+A - Select all",
            "Ctrl+C/X/V - Copy/Cut/Paste",
            "Ctrl+Z - Undo",
            "Ctrl+Y - Redo",
            "Ctrl+D - Duplicate line",
            "Ctrl+Shift+K - Delete line",
            "",
            "## Navigation",
            "Ctrl+Home/End - Start/End of file",
            "Alt+Up/Down - Move line"
        )));

        cachedSections = new ArrayList<>(sections);
        cachedCategories = categories;
        filteredSections = new ArrayList<>(sections);
    }

    private Map<String, List<Command>> getCommandsByCategory() {
        if (cachedCategories != null) {
            return cachedCategories;
        }

        Map<String, List<Command>> categories = new LinkedHashMap<>();

        for (Command cmd : Registry.getAllCommands()) {
            String category = cmd.getCategory();
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(cmd);
        }

        for (List<Command> cmds : categories.values()) {
            cmds.sort(Comparator.comparing(Command::getName));
        }

        return categories;
    }

    private DocSection createCategorySection(String category, List<Command> commands) {
        List<String> lines = new ArrayList<>();
        String icon = getCategoryIcon(category);

        lines.add("# " + icon + " " + category + " Commands");
        lines.add("");

        for (Command cmd : commands) {
            kasperstudios.kashub.core.Metadata meta = cmd.getMetadata();
            lines.add("## " + meta.name);
            lines.add(meta.description);

            if (meta.syntax != null && !meta.syntax.isEmpty()) {
                lines.add("Usage: " + meta.syntax);
            }

            if (meta.examples != null && !meta.examples.isEmpty()) {
                lines.add("");
                lines.add("Examples:");
                for (String example : meta.examples) {
                    lines.add("- " + example);
                }
            }

            lines.add("");
        }

        return new DocSection(icon + " " + category, category, lines);
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Movement": return "🚶";
            case "Interaction": return "⚔";
            case "Inventory": return "🎒";
            case "Output": return "💬";
            case "Timing": return "⏱";
            case "Vision": return "👁";
            case "Sound": return "🔊";
            case "Scanner": return "🔍";
            case "Network": return "🌐";
            case "Pathfinding": return "🧭";
            case "Trading": return "💰";
            case "Crafting": return "🔨";
            case "Protection": return "🛡";
            default: return "📋";
        }
    }

    private DocSection createAllCommandsSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 📚 All Commands");
        lines.add("");
        lines.add("Quick reference (use search to filter):");
        lines.add("");

        List<Command> allCommands = Registry.getAllCommands();
        allCommands.sort(Comparator.comparing(Command::getName));

        for (Command cmd : allCommands) {
            kasperstudios.kashub.core.Metadata meta = cmd.getMetadata();
            if (meta.syntax != null && !meta.syntax.isEmpty()) {
                lines.add("• " + meta.syntax);
            } else {
                lines.add("• " + meta.name);
            }
            lines.add("  " + meta.description);
        }

        return new DocSection("📚 All Commands", "All", lines);
    }

    private DocSection createObjectsSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 🎯 Built-in Objects");
        lines.add("");
        lines.add("KHScript provides built-in objects for common game interactions.");
        lines.add("");
        
        // Player Object
        lines.add("## player");
        lines.add("Player control and information");
        lines.add("");
        lines.add("Methods:");
        lines.add("• player.getHealth() - Get current health (0-20)");
        lines.add("• player.getPos() - Get position {x, y, z}");
        lines.add("• player.moveTo(x, y, z, [radius]) - Navigate to coordinates");
        lines.add("• player.attack([entity]) - Attack entity (crosshair or passed entity)");
        lines.add("• player.lookAt(x, y, z) - Look at coordinates");
        lines.add("• player.chat(message) - Send chat message or command");
        lines.add("• player.animation(action, name, [duration]) - Play animation");
        lines.add("• player.setFullbright(enabled) - Enable/disable fullbright (gamma 16)");
        lines.add("• player.getFullbright() - Check if fullbright is enabled");
        lines.add("");
        lines.add("Examples:");
        lines.add("  let health = player.getHealth()");
        lines.add("  if (health < 10) {");
        lines.add("      player.chat(\"/spawn\")");
        lines.add("  }");
        lines.add("  player.moveTo(100, 64, 200)");
        lines.add("  player.setFullbright(true)  // Enable fullbright");
        lines.add("");
        
        // Inventory Object
        lines.add("## inventory");
        lines.add("Inventory management");
        lines.add("");
        lines.add("Methods:");
        lines.add("• inventory.count(itemName) - Count items by name");
        lines.add("• inventory.getItems() - Get all items [{slot, id, count, name}]");
        lines.add("• inventory.getEmptySlots() - Count empty slots");
        lines.add("• inventory.drop(slot, [dropAll]) - Drop item from slot");
        lines.add("• inventory.swap(slot1, slot2) - Swap items between slots");
        lines.add("• inventory.equip(slot) - Equip item (armor/shield)");
        lines.add("• inventory.use(itemName) - Find and use item by name");
        lines.add("• inventory.craft(itemName, count) - Auto-craft item");
        lines.add("");
        lines.add("Examples:");
        lines.add("  let diamonds = inventory.count(\"diamond\")");
        lines.add("  print(\"Diamonds: \" + diamonds)");
        lines.add("  ");
        lines.add("  inventory.use(\"golden_apple\")");
        lines.add("  inventory.swap(0, 9)");
        lines.add("");
        
        // Vision Object
        lines.add("## vision");
        lines.add("Vision and entity detection");
        lines.add("");
        lines.add("Methods:");
        lines.add("• vision.getTarget() - Get crosshair target {type, x, y, z, blockId/entityId}");
        lines.add("• vision.nearest(type, [maxDist], [target]) - Find nearest entity");
        lines.add("  Types: \"hostile\", \"living\", \"all\", or entity name");
        lines.add("  Target: \"head\", \"body\", \"legs\" (default: \"body\")");
        lines.add("  Returns: {type, id, pos{x,y,z}, x, y, z, distance, health}");
        lines.add("• vision.isLookingAt(targetId, [maxDist]) - Check if looking at target");
        lines.add("");
        lines.add("Examples:");
        lines.add("  let enemy = vision.nearest(\"hostile\", 10, \"head\")");
        lines.add("  if (enemy != null) {");
        lines.add("      print(\"Found: \" + enemy.type)");
        lines.add("      player.lookAt(enemy.pos.x, enemy.pos.y, enemy.pos.z)");
        lines.add("      player.attack(enemy)");
        lines.add("  }");
        lines.add("");
        
        // Scanner Object
        lines.add("## scanner");
        lines.add("Advanced block and entity scanning");
        lines.add("");
        lines.add("Methods:");
        lines.add("• scanner.findBlocks(types, radius, [yMin], [yMax]) - Find blocks");
        lines.add("• scanner.findEntities(types, radius) - Find entities");
        lines.add("• scanner.clearCache() - Clear scan cache");
        lines.add("");
        lines.add("Examples:");
        lines.add("  let ores = scanner.findBlocks(\"*ore*\", 32, -64, 20)");
        lines.add("  let mobs = scanner.findEntities(\"hostile\", 50)");
        lines.add("");
        
        // World Object
        lines.add("## world");
        lines.add("World information");
        lines.add("");
        lines.add("Methods:");
        lines.add("• world.getTime() - Get world time in ticks");
        lines.add("• world.isDay() - Check if daytime");
        lines.add("• world.isNight() - Check if nighttime");
        lines.add("• world.getWeather() - Get weather (\"clear\", \"rain\", \"thunder\")");
        lines.add("");
        
        // Game Object
        lines.add("## game");
        lines.add("Game state information");
        lines.add("");
        lines.add("Methods:");
        lines.add("• game.getMode() - Get game mode (\"survival\", \"creative\", \"spectator\")");
        lines.add("• game.getDimension() - Get current dimension");
        lines.add("• game.isSingleplayer() - Check if singleplayer");
        lines.add("");

        return new DocSection("🎯 Objects", "Objects", lines);
    }

    private DocSection createTypesSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 📐 Type System");
        lines.add("");
        lines.add("KHScript supports optional type annotations.");
        lines.add("");
        lines.add("## Enabling Types");
        lines.add("@type strict  // strict, loose, or off");
        lines.add("");
        lines.add("## Syntax");
        lines.add("varName: type = value");
        lines.add("");
        lines.add("## Examples");
        lines.add("count: number = 10");
        lines.add("name: string = \"Player\"");
        lines.add("active: bool = true");
        lines.add("pos: position = \"100, 64, 200\"");
        lines.add("");
        lines.add("## Available Types");
        lines.add("");

        lines.add("### Primitive Types");
        for (KHType type : KHType.values()) {
            if (type == KHType.NUMBER || type == KHType.STRING || type == KHType.BOOL) {
                lines.add("• " + type.getName() + " - " + type.getDescription());
            }
        }
        lines.add("");

        lines.add("### Game Types");
        for (KHType type : KHType.values()) {
            if (type == KHType.POSITION || type == KHType.ITEM ||
                type == KHType.ENTITY || type == KHType.BLOCK) {
                lines.add("• " + type.getName() + " - " + type.getDescription());
            }
        }
        lines.add("");

        lines.add("### API Types");
        for (KHType type : KHType.values()) {
            if (type == KHType.TRADE_OFFER || type == KHType.RECIPE || type == KHType.PATH) {
                lines.add("• " + type.getName() + " - " + type.getDescription());
            }
        }
        lines.add("");

        lines.add("### Collections");
        for (KHType type : KHType.values()) {
            if (type == KHType.LIST || type == KHType.MAP) {
                lines.add("• " + type.getName() + " - " + type.getDescription());
            }
        }
        lines.add("");

        lines.add("### Special Types");
        for (KHType type : KHType.values()) {
            if (type == KHType.ANY || type == KHType.VOID || type == KHType.NULL) {
                lines.add("• " + type.getName() + " - " + type.getDescription());
            }
        }
        lines.add("");

        lines.add("## Type Modes");
        lines.add("• off - No type checking (default)");
        lines.add("• loose - Warnings only");
        lines.add("• strict - Errors on type mismatch");
        lines.add("");

        lines.add("## Type Compatibility");
        lines.add("• any is compatible with all types");
        lines.add("• number and string auto-convert");
        lines.add("• Same types are always compatible");

        return new DocSection("📐 Types", "Types", lines);
    }

    private void filterSections(String query) {
        if (query == null || query.isEmpty()) {
            filteredSections = new ArrayList<>(sections);
            return;
        }

        String lowerQuery = query.toLowerCase();
        filteredSections = new ArrayList<>();

        if (!sections.isEmpty()) {
            filteredSections.add(sections.get(0));
        }

        List<String> matchingLines = new ArrayList<>();
        matchingLines.add("# 🔍 Search Results: \"" + query + "\"");
        matchingLines.add("");

        int matchCount = 0;
        for (Command cmd : Registry.getAllCommands()) {
            kasperstudios.kashub.core.Metadata meta = cmd.getMetadata();
            boolean matches = meta.name.toLowerCase().contains(lowerQuery) ||
                            meta.description.toLowerCase().contains(lowerQuery) ||
                            (meta.syntax != null && meta.syntax.toLowerCase().contains(lowerQuery));

            if (matches) {
                matchCount++;
                matchingLines.add("## " + meta.name);
                matchingLines.add(meta.description);

                if (meta.syntax != null && !meta.syntax.isEmpty()) {
                    matchingLines.add("Usage: " + meta.syntax);
                }

                if (meta.examples != null && !meta.examples.isEmpty()) {
                    matchingLines.add("");
                    for (String example : meta.examples) {
                        matchingLines.add("- " + example);
                    }
                }
                matchingLines.add("");
            }
        }

        if (matchCount > 0) {
            matchingLines.add(1, "Found " + matchCount + " command(s)");
            filteredSections.add(new DocSection("🔍 Results (" + matchCount + ")", "Search", matchingLines));
            selectedSection = 1;
        } else {
            matchingLines.add("No commands found matching \"" + query + "\"");
            filteredSections.add(new DocSection("🔍 No Results", "Search", matchingLines));
            selectedSection = 1;
        }

        scrollY = 0;
    }

    @Override
    protected void init() {
        super.init();

        int dx = getDialogX();
        int dy = getDialogY();

        searchField = new TextFieldWidget(
            textRenderer,
            dx + SIDEBAR_WIDTH + 10,
            dy + HEADER_HEIGHT - SEARCH_HEIGHT - 8,
            getDialogWidth() - SIDEBAR_WIDTH - 20,
            SEARCH_HEIGHT - 4,
            Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("Search commands..."));
        searchField.setChangedListener(query -> {
            searchQuery = query;
            filterSections(query);
        });
        addDrawableChild(searchField);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    private int getDialogX() {
        return (this.width - getDialogWidth()) / 2;
    }

    private int getDialogY() {
        return (this.height - getDialogHeight()) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int dx = getDialogX();
        int dy = getDialogY();

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.sidebarColor);

        context.fill(dx, dy, dx + getDialogWidth(), dy + 2, theme.accentColor);
        context.fill(dx, dy + getDialogHeight() - 2, dx + getDialogWidth(), dy + getDialogHeight(), theme.accentColor);
        context.fill(dx, dy, dx + 2, dy + getDialogHeight(), theme.accentColor);
        context.fill(dx + getDialogWidth() - 2, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.accentColor);

        renderHeader(context, dx, dy, mouseX, mouseY);
        renderSidebar(context, dx, dy + HEADER_HEIGHT, mouseX, mouseY);
        renderContent(context, dx + SIDEBAR_WIDTH, dy + HEADER_HEIGHT, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int dx, int dy, int mouseX, int mouseY) {
        context.fill(dx, dy, dx + getDialogWidth(), dy + HEADER_HEIGHT, adjustBrightness(theme.sidebarColor, 10));

        context.drawText(textRenderer, "📚 KHSCRIPT DOCUMENTATION", dx + 16, dy + 14, theme.textColor, true);

        int cmdCount = Registry.getAllCommands().size();
        context.drawText(textRenderer, cmdCount + " commands", dx + 16, dy + 28, theme.textDimColor, false);

        int closeX = dx + getDialogWidth() - 36;
        int closeY = dy + 10;
        boolean closeHovered = mouseX >= closeX && mouseX < closeX + 24 && mouseY >= closeY && mouseY < closeY + 20;
        context.fill(closeX, closeY, closeX + 24, closeY + 20, closeHovered ? theme.consoleErrorColor : theme.buttonColor);
        context.drawText(textRenderer, "✕", closeX + 8, closeY + 6, 0xFFFFFFFF, false);

        context.fill(dx, dy + HEADER_HEIGHT - 1, dx + getDialogWidth(), dy + HEADER_HEIGHT, theme.accentColor & 0x66FFFFFF);
    }

    private void renderSidebar(DrawContext context, int dx, int dy, int mouseX, int mouseY) {
        int contentHeight = getDialogHeight() - HEADER_HEIGHT;
        context.fill(dx, dy, dx + SIDEBAR_WIDTH, dy + contentHeight, adjustBrightness(theme.sidebarColor, -5));
        context.fill(dx + SIDEBAR_WIDTH - 1, dy, dx + SIDEBAR_WIDTH, dy + contentHeight, theme.accentColor & 0x44FFFFFF);

        context.enableScissor(dx, dy, dx + SIDEBAR_WIDTH, dy + contentHeight);

        int itemHeight = 22;
        List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;

        for (int i = 0; i < displaySections.size(); i++) {
            int itemY = dy + i * itemHeight + 8 - sidebarScrollY;

            if (itemY < dy - itemHeight || itemY > dy + contentHeight) continue;

            boolean hovered = mouseX >= dx && mouseX < dx + SIDEBAR_WIDTH &&
                             mouseY >= itemY && mouseY < itemY + itemHeight;
            boolean selected = i == selectedSection;

            if (selected) {
                context.fill(dx, itemY, dx + SIDEBAR_WIDTH - 1, itemY + itemHeight, theme.accentColor & 0x44FFFFFF);
                context.fill(dx, itemY, dx + 3, itemY + itemHeight, theme.accentColor);
            } else if (hovered) {
                context.fill(dx, itemY, dx + SIDEBAR_WIDTH - 1, itemY + itemHeight, theme.buttonHoverColor);
            }

            int textColor = selected ? theme.accentColor : (hovered ? theme.textColor : theme.textDimColor);
            String title = displaySections.get(i).title;
            if (textRenderer.getWidth(title) > SIDEBAR_WIDTH - 20) {
                title = truncateText(title, SIDEBAR_WIDTH - 25) + "...";
            }
            context.drawText(textRenderer, title, dx + 10, itemY + 6, textColor, false);
        }

        context.disableScissor();
    }

    private void renderContent(DrawContext context, int dx, int dy, int mouseX, int mouseY) {
        int contentWidth = getDialogWidth() - SIDEBAR_WIDTH;
        int contentHeight = getDialogHeight() - HEADER_HEIGHT;

        List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
        if (selectedSection >= displaySections.size()) {
            selectedSection = 0;
        }
        if (displaySections.isEmpty()) return;

        DocSection section = displaySections.get(selectedSection);
        int lineHeight = 12;
        int y = dy + 12 - scrollY;

        context.enableScissor(dx, dy, dx + contentWidth, dy + contentHeight);

        for (String line : section.lines) {
            if (y > dy - lineHeight && y < dy + contentHeight) {
                int color = theme.textColor;
                int xOffset = 12 - scrollX;

                String displayLine = line;

                if (line.startsWith("# ")) {
                    color = theme.accentColor;
                    displayLine = line.substring(2);
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, color, true);
                    y += 4;
                } else if (line.startsWith("## ")) {
                    color = theme.functionColor;
                    displayLine = line.substring(3);
                    y += 4;
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, color, true);
                } else if (line.startsWith("//") || line.startsWith("  ")) {
                    color = theme.commentColor;
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, color, false);
                } else if (line.startsWith("•")) {
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, theme.stringColor, false);
                } else if (line.startsWith("Usage:")) {
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, theme.keywordColor, false);
                } else {
                    context.drawText(textRenderer, displayLine, dx + xOffset, y, color, false);
                }
            }
            y += lineHeight;
        }

        context.disableScissor();

        int totalHeight = calculateSectionHeight(section) + 48;
        if (totalHeight > contentHeight) {
            int scrollbarHeight = Math.max(20, (contentHeight * contentHeight) / totalHeight);
            int maxScroll = totalHeight - contentHeight;
            int scrollbarY = dy + (scrollY * (contentHeight - scrollbarHeight)) / Math.max(1, maxScroll);

            context.fill(dx + contentWidth - 6, dy, dx + contentWidth - 2, dy + contentHeight, 0x22FFFFFF);
            context.fill(dx + contentWidth - 6, scrollbarY, dx + contentWidth - 2, scrollbarY + scrollbarHeight, theme.accentColor);
        }
    }

    private String truncateText(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        int len = text.length();
        while (len > 0 && textRenderer.getWidth(text.substring(0, len)) > maxWidth) {
            len--;
        }
        return text.substring(0, Math.max(0, len));
    }

    private int calculateSectionHeight(DocSection section) {
        int lineHeight = 12;
        int height = 12;
        for (String line : section.lines) {
            if (line.startsWith("# ")) {
                height += lineHeight + 4;
            } else if (line.startsWith("## ")) {
                height += lineHeight + 4;
            } else {
                height += lineHeight;
            }
        }
        return height;
    }

    private int calculateMaxLineWidth(DocSection section) {
        int maxWidth = 0;
        for (String line : section.lines) {
            int width = textRenderer.getWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int dx = getDialogX();
        int dy = getDialogY();

        int closeX = dx + getDialogWidth() - 36;
        int closeY = dy + 10;
        if (mouseX >= closeX && mouseX < closeX + 24 && mouseY >= closeY && mouseY < closeY + 20) {
            this.close();
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
            int contentWidth = getDialogWidth() - SIDEBAR_WIDTH;
            List<DocSection> displaySections = filteredSections.isEmpty() ? sections : filteredSections;
            if (selectedSection < displaySections.size()) {
                DocSection section = displaySections.get(selectedSection);

                if (hasShiftDown()) {
                    int maxLineWidth = calculateMaxLineWidth(section);
                    int maxScrollX = Math.max(0, maxLineWidth - contentWidth + 40);
                    scrollX = Math.max(0, Math.min(maxScrollX, scrollX - (int)(verticalAmount * 36)));
                } else {

                    int totalHeight = calculateSectionHeight(section) + 48;
                    int maxScroll = Math.max(0, totalHeight - contentHeight);
                    scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int)(verticalAmount * 36)));
                }
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
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
        final String category;
        final List<String> lines;

        DocSection(String title, String category, List<String> lines) {
            this.title = title;
            this.category = category;
            this.lines = new ArrayList<>(lines);
        }
    }
}

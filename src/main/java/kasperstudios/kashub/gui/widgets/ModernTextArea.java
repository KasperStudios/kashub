package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.CodeCompletionManager;
import kasperstudios.kashub.debug.DebugManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.Command;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;

public class ModernTextArea {
    private final TextRenderer textRenderer;
    private int x, y, width, height;
    private EditorTheme theme;

    private List<String> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorColumn = 0;
    private int scrollY = 0;
    private int scrollX = 0;

    private int selectionStartLine = -1;
    private int selectionStartColumn = -1;
    private int selectionEndLine = -1;
    private int selectionEndColumn = -1;
    private boolean hasSelection = false;

    private final Deque<UndoState> undoStack = new ArrayDeque<>();
    private final Deque<UndoState> redoStack = new ArrayDeque<>();
    private long lastUndoSaveTime = 0;
    private static final long UNDO_SAVE_INTERVAL = 500;

    private boolean focused = false;
    private long cursorBlinkTime = 0;

    private Map<Integer, String> lineErrors = new HashMap<>();
    private long lastValidationTime = 0;
    private static final long VALIDATION_DELAY = 500;

    private final Set<Integer> breakpoints = new HashSet<>();
    private int executionLine = -1;

    private static final int LINE_HEIGHT = 12;
    private static final int LINE_NUMBER_WIDTH = 45;
    private static final int PADDING = 8;

    private static final Set<String> KEYWORDS = Set.of(
            "if", "else", "while", "for", "loop", "function", "fn", "return", "break", "continue",
            "true", "false", "null", "and", "or", "not", "in", "end", "let", "const");

    private static Set<String> COMMANDS = null;

    private static Set<String> getCommands() {
        if (COMMANDS == null) {
            refreshCommands();
        }
        return COMMANDS;
    }

    public static void refreshCommands() {
        COMMANDS = new HashSet<>();
        for (Command cmd : CommandRegistry.getAllCommands()) {
            COMMANDS.add(cmd.getName().toLowerCase());
        }
    }

    private boolean showAutocomplete = false;
    private List<String> autocompleteItems = new ArrayList<>();
    private int autocompleteSelectedIndex = 0;
    private String autocompletePrefix = "";
    private int autocompleteX = 0;
    private int autocompleteY = 0;
    private static final int AUTOCOMPLETE_MAX_ITEMS = 8;
    private static final int AUTOCOMPLETE_ITEM_HEIGHT = 14;

    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("//.*$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

    public ModernTextArea(TextRenderer textRenderer, int x, int y, int width, int height, EditorTheme theme) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.theme = theme;
        this.lines.add("");
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        long now = System.currentTimeMillis();
        if (now - lastValidationTime > VALIDATION_DELAY) {
            validateSyntax();
            lastValidationTime = now;
        }

        context.fill(x, y, x + width, y + height, theme.backgroundColor);

        context.fill(x, y, x + LINE_NUMBER_WIDTH, y + height, adjustBrightness(theme.backgroundColor, -10));

        int visibleLines = height / LINE_HEIGHT;
        int startLine = scrollY;
        int endLine = Math.min(startLine + visibleLines + 1, lines.size());

        for (int i = startLine; i < endLine; i++) {
            int lineY = y + (i - scrollY) * LINE_HEIGHT + PADDING;

            boolean hasError = lineErrors.containsKey(i);
            if (hasError) {
                context.fill(x + LINE_NUMBER_WIDTH, lineY - 2, x + width, lineY + LINE_HEIGHT - 2,
                        0x33FF0000);
            }

            if (i + 1 == executionLine) {
                context.fill(x + LINE_NUMBER_WIDTH, lineY - 2, x + width, lineY + LINE_HEIGHT - 2,
                        0x3300FF00);

                context.drawText(textRenderer, "➜", x + 2, lineY, 0xFF00FF00, false);
            }

            if (i == cursorLine && focused && !hasSelection) {
                context.fill(x + LINE_NUMBER_WIDTH, lineY - 2, x + width, lineY + LINE_HEIGHT - 2,
                        theme.selectionColor & 0x33FFFFFF);
            }

            if (hasSelection) {
                int[] selStart = getSelectionStart();
                int[] selEnd = getSelectionEnd();
                if (selStart != null && selEnd != null && i >= selStart[0] && i <= selEnd[0]) {
                    int startCol = (i == selStart[0]) ? selStart[1] : 0;
                    int endCol = (i == selEnd[0]) ? selEnd[1] : lines.get(i).length();

                    String lineText = lines.get(i);
                    int selStartX = x + LINE_NUMBER_WIDTH + PADDING
                            + getTextWidth(lineText.substring(0, Math.min(startCol, lineText.length()))) - scrollX;
                    int selEndX = x + LINE_NUMBER_WIDTH + PADDING
                            + getTextWidth(lineText.substring(0, Math.min(endCol, lineText.length()))) - scrollX;

                    context.fill(selStartX, lineY - 2, selEndX, lineY + LINE_HEIGHT - 2, theme.selectionColor);
                }
            }

            if (breakpoints.contains(i + 1)) {

                context.fill(x + 20, lineY + 2, x + 28, lineY + 10, 0xFFFF0000);
            }

            String lineNum = String.valueOf(i + 1);
            int lineNumX = x + LINE_NUMBER_WIDTH - textRenderer.getWidth(lineNum) - 8;
            int lineNumColor = hasError ? 0xFFFF6666
                    : (breakpoints.contains(i + 1) ? 0xFFFF0000 : theme.lineNumberColor);
            context.drawText(textRenderer, lineNum, lineNumX, lineY, lineNumColor, false);

            if (hasError) {
                context.drawText(textRenderer, "⚠", x + 4, lineY, 0xFFFF4444, false);
            }

            String line = lines.get(i);

            int codeAreaX = x + LINE_NUMBER_WIDTH;
            int codeAreaWidth = width - LINE_NUMBER_WIDTH - 8;
            MinecraftClient mc = MinecraftClient.getInstance();
            double scale = mc.getWindow().getScaleFactor();
            int scaledX = (int) (codeAreaX * scale);
            int scaledY = (int) (y * scale);
            int scaledW = (int) (codeAreaWidth * scale);
            int scaledH = (int) (height * scale);

            int windowHeight = mc.getWindow().getFramebufferHeight();
            RenderSystem.enableScissor(scaledX, windowHeight - scaledY - scaledH, scaledW, scaledH);
            renderHighlightedLine(context, line, x + LINE_NUMBER_WIDTH + PADDING - scrollX, lineY);
            RenderSystem.disableScissor();
        }

        if (focused && (System.currentTimeMillis() - cursorBlinkTime) % 1000 < 500) {
            int cursorX = x + LINE_NUMBER_WIDTH + PADDING + getTextWidth(getCurrentLineBeforeCursor()) - scrollX;
            int cursorY = y + (cursorLine - scrollY) * LINE_HEIGHT + PADDING;

            int codeAreaLeft = x + LINE_NUMBER_WIDTH;
            int codeAreaRight = x + width - 8;

            if (cursorY >= y && cursorY < y + height - LINE_HEIGHT &&
                    cursorX >= codeAreaLeft && cursorX < codeAreaRight) {
                context.fill(cursorX, cursorY - 1, cursorX + 2, cursorY + LINE_HEIGHT - 1, theme.cursorColor);
            }
        }

        if (lines.size() > visibleLines) {
            int scrollbarHeight = Math.max(20, (visibleLines * height) / lines.size());
            int scrollbarY = y + (scrollY * (height - scrollbarHeight)) / Math.max(1, lines.size() - visibleLines);
            context.fill(x + width - 6, y, x + width - 2, y + height, 0x22FFFFFF);
            context.fill(x + width - 6, scrollbarY, x + width - 2, scrollbarY + scrollbarHeight, theme.accentColor);
        }

        int maxLineWidth = getMaxLineWidth();
        int contentWidth = width - LINE_NUMBER_WIDTH - PADDING * 2 - 10;
        if (maxLineWidth > contentWidth) {
            int scrollbarWidth = Math.max(30, (contentWidth * contentWidth) / maxLineWidth);
            int maxScrollX = maxLineWidth - contentWidth;
            int scrollbarX = x + LINE_NUMBER_WIDTH
                    + (scrollX * (contentWidth - scrollbarWidth)) / Math.max(1, maxScrollX);
            context.fill(x + LINE_NUMBER_WIDTH, y + height - 6, x + width - 8, y + height - 2, 0x22FFFFFF);
            context.fill(scrollbarX, y + height - 6, scrollbarX + scrollbarWidth, y + height - 2, theme.accentColor);
        }

        if (showAutocomplete && !autocompleteItems.isEmpty()) {
            renderAutocomplete(context, mouseX, mouseY);
        }
    }

    private void renderAutocomplete(DrawContext context, int mouseX, int mouseY) {
        int itemsToShow = Math.min(autocompleteItems.size(), AUTOCOMPLETE_MAX_ITEMS);
        int popupWidth = 200;
        int popupHeight = itemsToShow * AUTOCOMPLETE_ITEM_HEIGHT + 4;

        int cursorX = x + LINE_NUMBER_WIDTH + PADDING + getTextWidth(getCurrentLineBeforeCursor()) - scrollX;
        int cursorY = y + (cursorLine - scrollY) * LINE_HEIGHT + PADDING + LINE_HEIGHT;

        if (cursorX + popupWidth > x + width) {
            cursorX = x + width - popupWidth - 10;
        }
        if (cursorY + popupHeight > y + height) {
            cursorY = y + (cursorLine - scrollY) * LINE_HEIGHT + PADDING - popupHeight;
        }

        autocompleteX = cursorX;
        autocompleteY = cursorY;

        context.fill(cursorX - 1, cursorY - 1, cursorX + popupWidth + 1, cursorY + popupHeight + 1, theme.accentColor);
        context.fill(cursorX, cursorY, cursorX + popupWidth, cursorY + popupHeight,
                adjustBrightness(theme.backgroundColor, 10));

        for (int i = 0; i < itemsToShow; i++) {
            int itemY = cursorY + 2 + i * AUTOCOMPLETE_ITEM_HEIGHT;
            String item = autocompleteItems.get(i);

            if (i == autocompleteSelectedIndex) {
                context.fill(cursorX + 1, itemY, cursorX + popupWidth - 1, itemY + AUTOCOMPLETE_ITEM_HEIGHT,
                        theme.selectionColor);
            }

            String icon = getAutocompleteIcon(item);
            int textColor = i == autocompleteSelectedIndex ? 0xFFFFFFFF : theme.textColor;
            context.drawText(textRenderer, icon + " " + item, cursorX + 4, itemY + 2, textColor, false);

            if (i == autocompleteSelectedIndex) {
                String desc;
                if (isArgumentMode && currentCommandContext != null) {

                    desc = CodeCompletionManager.getArgumentDescription(currentCommandContext, item);
                } else {
                    desc = CodeCompletionManager.getCommandDescription(item);
                }
                if (desc != null && !desc.isEmpty()) {

                    if (desc.length() > 30) {
                        desc = desc.substring(0, 27) + "...";
                    }
                    int descWidth = textRenderer.getWidth(desc);
                    context.drawText(textRenderer, desc, cursorX + popupWidth - descWidth - 4, itemY + 2,
                            theme.textDimColor, false);
                }
            }
        }

        if (autocompleteItems.size() > AUTOCOMPLETE_MAX_ITEMS) {
            String moreText = "+" + (autocompleteItems.size() - AUTOCOMPLETE_MAX_ITEMS) + " more";
            context.drawText(textRenderer, moreText, cursorX + 4, cursorY + popupHeight - 10, theme.textDimColor,
                    false);
        }
    }

    private String getAutocompleteIcon(String item) {
        if (isArgumentMode)
            return "▸";
        if (item.startsWith("$"))
            return "📦";
        if (KEYWORDS.contains(item.toLowerCase()))
            return "🔑";
        if (getCommands().contains(item.toLowerCase()))
            return "⚡";
        return "📝";
    }

    private int getMaxLineWidth() {
        int max = 0;
        for (String line : lines) {
            int w = getTextWidth(line);
            if (w > max)
                max = w;
        }
        return max + 50;
    }

    private void renderHighlightedLine(DrawContext context, String line, int startX, int y) {
        if (line.isEmpty())
            return;

        int[] colors = new int[line.length()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = theme.textColor;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        while (commentMatcher.find()) {
            for (int i = commentMatcher.start(); i < commentMatcher.end(); i++) {
                colors[i] = theme.commentColor;
            }
        }

        Matcher stringMatcher = STRING_PATTERN.matcher(line);
        while (stringMatcher.find()) {
            for (int i = stringMatcher.start(); i < stringMatcher.end(); i++) {
                if (colors[i] != theme.commentColor) {
                    colors[i] = theme.stringColor;
                }
            }
        }

        Matcher numberMatcher = NUMBER_PATTERN.matcher(line);
        while (numberMatcher.find()) {
            for (int i = numberMatcher.start(); i < numberMatcher.end(); i++) {
                if (colors[i] == theme.textColor) {
                    colors[i] = theme.numberColor;
                }
            }
        }

        Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
        while (varMatcher.find()) {
            for (int i = varMatcher.start(); i < varMatcher.end(); i++) {
                if (colors[i] == theme.textColor) {
                    colors[i] = theme.variableColor;
                }
            }
        }

        String[] words = line.split("(?<=\\s)|(?=\\s)|(?<=[^a-zA-Z0-9_])|(?=[^a-zA-Z0-9_])");
        int pos = 0;
        for (String word : words) {
            String trimmed = word.trim();
            if (KEYWORDS.contains(trimmed.toLowerCase())) {
                for (int i = pos; i < pos + word.length() && i < colors.length; i++) {
                    if (colors[i] == theme.textColor) {
                        colors[i] = theme.keywordColor;
                    }
                }
            } else if (getCommands().contains(trimmed.toLowerCase())) {
                for (int i = pos; i < pos + word.length() && i < colors.length; i++) {
                    if (colors[i] == theme.textColor) {
                        colors[i] = theme.functionColor;
                    }
                }
            }
            pos += word.length();
        }

        int currentX = startX;
        for (int i = 0; i < line.length(); i++) {
            String ch = String.valueOf(line.charAt(i));
            context.drawText(textRenderer, ch, currentX, y, colors[i], false);
            currentX += textRenderer.getWidth(ch);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused)
            return false;

        cursorBlinkTime = System.currentTimeMillis();
        boolean ctrlPressed = (modifiers & 2) != 0;
        boolean shiftPressed = (modifiers & 1) != 0;
        boolean altPressed = (modifiers & 4) != 0;

        if (showAutocomplete) {

            if (keyCode == 265) {
                autocompleteSelectedIndex = Math.max(0, autocompleteSelectedIndex - 1);
                return true;
            }

            if (keyCode == 264) {
                autocompleteSelectedIndex = Math.min(autocompleteItems.size() - 1, autocompleteSelectedIndex + 1);
                return true;
            }

            if (keyCode == 257 || keyCode == 335 || keyCode == 258) {
                acceptAutocomplete();
                return true;
            }

            if (keyCode == 256) {
                hideAutocomplete();
                return true;
            }
        }

        if (ctrlPressed && keyCode == 32) {
            triggerAutocomplete();
            return true;
        }

        if (ctrlPressed && keyCode == 65) {
            selectAll();
            return true;
        }

        if (ctrlPressed && keyCode == 67) {
            copy();
            return true;
        }

        if (ctrlPressed && keyCode == 88) {
            cut();
            return true;
        }

        if (ctrlPressed && keyCode == 86) {
            paste();
            return true;
        }

        if (ctrlPressed && !shiftPressed && keyCode == 90) {
            undo();
            return true;
        }

        if ((ctrlPressed && shiftPressed && keyCode == 90) || (ctrlPressed && keyCode == 89)) {
            redo();
            return true;
        }

        if (ctrlPressed && keyCode == 68) {
            duplicateLine();
            return true;
        }

        if (ctrlPressed && shiftPressed && keyCode == 75) {
            deleteLine();
            return true;
        }

        if (altPressed && keyCode == 265) {
            moveLineUp();
            return true;
        }

        if (altPressed && keyCode == 264) {
            moveLineDown();
            return true;
        }

        if (ctrlPressed && keyCode == 268) {
            cursorLine = 0;
            cursorColumn = 0;
            clearSelection();
            ensureCursorVisible();
            return true;
        }

        if (ctrlPressed && keyCode == 269) {
            cursorLine = lines.size() - 1;
            cursorColumn = lines.get(cursorLine).length();
            clearSelection();
            ensureCursorVisible();
            return true;
        }

        if (shiftPressed && (keyCode == 262 || keyCode == 263 || keyCode == 264 || keyCode == 265 ||
                keyCode == 268 || keyCode == 269)) {

            if (!hasSelection) {
                selectionStartLine = cursorLine;
                selectionStartColumn = cursorColumn;
                hasSelection = true;
            }
        }

        if (!shiftPressed && (keyCode == 262 || keyCode == 263 || keyCode == 264 || keyCode == 265 ||
                keyCode == 268 || keyCode == 269)) {
            clearSelection();
        }

        if (hasSelection && (keyCode == 259 || keyCode == 261)) {
            deleteSelection();
            return true;
        }

        switch (keyCode) {
            case 259:
                if (cursorColumn > 0) {
                    String line = lines.get(cursorLine);
                    lines.set(cursorLine, line.substring(0, cursorColumn - 1) + line.substring(cursorColumn));
                    cursorColumn--;
                } else if (cursorLine > 0) {
                    String currentLine = lines.remove(cursorLine);
                    cursorLine--;
                    cursorColumn = lines.get(cursorLine).length();
                    lines.set(cursorLine, lines.get(cursorLine) + currentLine);
                }
                return true;

            case 261:
                String line = lines.get(cursorLine);
                if (cursorColumn < line.length()) {
                    lines.set(cursorLine, line.substring(0, cursorColumn) + line.substring(cursorColumn + 1));
                } else if (cursorLine < lines.size() - 1) {
                    lines.set(cursorLine, line + lines.remove(cursorLine + 1));
                }
                return true;

            case 257:
            case 335:
                String currentLine = lines.get(cursorLine);
                String before = currentLine.substring(0, cursorColumn);
                String after = currentLine.substring(cursorColumn);

                StringBuilder indent = new StringBuilder();
                for (char c : before.toCharArray()) {
                    if (c == ' ' || c == '\t')
                        indent.append(c);
                    else
                        break;
                }

                lines.set(cursorLine, before);
                lines.add(cursorLine + 1, indent.toString() + after);
                cursorLine++;
                cursorColumn = indent.length();
                ensureCursorVisible();
                return true;

            case 258:
                insertText("    ");
                return true;

            case 262:
                if (cursorColumn < lines.get(cursorLine).length()) {
                    cursorColumn++;
                } else if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorColumn = 0;
                }
                if (shiftPressed)
                    updateSelection();
                ensureCursorVisible();
                return true;

            case 263:
                if (cursorColumn > 0) {
                    cursorColumn--;
                } else if (cursorLine > 0) {
                    cursorLine--;
                    cursorColumn = lines.get(cursorLine).length();
                }
                if (shiftPressed)
                    updateSelection();
                ensureCursorVisible();
                return true;

            case 264:
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length());
                }
                if (shiftPressed)
                    updateSelection();
                ensureCursorVisible();
                return true;

            case 265:
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length());
                }
                if (shiftPressed)
                    updateSelection();
                ensureCursorVisible();
                return true;

            case 268:
                cursorColumn = 0;
                if (shiftPressed)
                    updateSelection();
                return true;

            case 269:
                cursorColumn = lines.get(cursorLine).length();
                if (shiftPressed)
                    updateSelection();
                return true;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused)
            return false;
        if (chr < 32)
            return false;

        cursorBlinkTime = System.currentTimeMillis();

        if (showAutocomplete) {
            insertText(String.valueOf(chr));
            updateAutocomplete();
            return true;
        }

        insertText(String.valueOf(chr));

        if (Character.isLetterOrDigit(chr) || chr == '$' || chr == '_') {
            triggerAutocomplete();
        } else {
            hideAutocomplete();
        }

        return true;
    }

    private void insertText(String text) {
        String line = lines.get(cursorLine);
        lines.set(cursorLine, line.substring(0, cursorColumn) + text + line.substring(cursorColumn));
        cursorColumn += text.length();
    }

    private String currentCommandContext = null;
    private boolean isArgumentMode = false;

    private void triggerAutocomplete() {

        String[] lineContext = getLineContext();
        String currentWord = getCurrentWord();

        if (lineContext != null && lineContext.length >= 1) {
            String command = lineContext[0].toLowerCase();
            if (CodeCompletionManager.hasArgumentSuggestions(command)) {

                currentCommandContext = command;
                isArgumentMode = true;
                autocompletePrefix = currentWord;
                autocompleteItems = CodeCompletionManager.getArgumentCompletions(command, currentWord);

                if (!autocompleteItems.isEmpty()) {
                    autocompleteSelectedIndex = 0;
                    showAutocomplete = true;
                    return;
                }
            }
        }

        isArgumentMode = false;
        currentCommandContext = null;

        if (currentWord.isEmpty()) {
            hideAutocomplete();
            return;
        }

        autocompletePrefix = currentWord;
        autocompleteItems = CodeCompletionManager.getCompletions(currentWord);

        if (autocompleteItems.isEmpty()) {
            hideAutocomplete();
            return;
        }

        autocompleteSelectedIndex = 0;
        showAutocomplete = true;
    }

    private void updateAutocomplete() {
        String currentWord = getCurrentWord();

        String[] lineContext = getLineContext();

        if (lineContext != null && lineContext.length >= 1) {
            String command = lineContext[0].toLowerCase();
            if (CodeCompletionManager.hasArgumentSuggestions(command) && cursorColumn > command.length()) {

                currentCommandContext = command;
                isArgumentMode = true;
                autocompletePrefix = currentWord;
                autocompleteItems = CodeCompletionManager.getArgumentCompletions(command, currentWord);

                if (autocompleteItems.isEmpty()) {
                    hideAutocomplete();
                    return;
                }

                autocompleteSelectedIndex = Math.min(autocompleteSelectedIndex, autocompleteItems.size() - 1);
                showAutocomplete = true;
                return;
            }
        }

        isArgumentMode = false;
        currentCommandContext = null;

        if (currentWord.isEmpty()) {
            hideAutocomplete();
            return;
        }

        autocompletePrefix = currentWord;
        autocompleteItems = CodeCompletionManager.getCompletions(currentWord);

        if (autocompleteItems.isEmpty()) {
            hideAutocomplete();
            return;
        }

        autocompleteSelectedIndex = Math.min(autocompleteSelectedIndex, autocompleteItems.size() - 1);
    }

    private String[] getLineContext() {
        if (cursorLine >= lines.size())
            return null;
        String line = lines.get(cursorLine).trim();
        if (line.isEmpty())
            return null;

        String beforeCursor = line.substring(0, Math.min(cursorColumn, line.length())).trim();
        if (beforeCursor.isEmpty())
            return null;

        String[] parts = beforeCursor.split("\\s+");
        return parts;
    }

    private void acceptAutocomplete() {
        if (!showAutocomplete || autocompleteItems.isEmpty())
            return;

        String selected = autocompleteItems.get(autocompleteSelectedIndex);
        String prefix = autocompletePrefix;

        String line = lines.get(cursorLine);
        int prefixStart = cursorColumn - prefix.length();
        if (prefixStart < 0)
            prefixStart = 0;

        String snippet = CodeCompletionManager.getSnippet(selected);
        if (!snippet.isEmpty()) {

            lines.set(cursorLine, line.substring(0, prefixStart) + line.substring(cursorColumn));
            cursorColumn = prefixStart;

            String[] snippetLines = snippet.split("\n");
            for (int i = 0; i < snippetLines.length; i++) {
                String snippetLine = snippetLines[i].replace("$cursor", "").replace("$condition", "condition")
                        .replace("$name", "myFunction").replace("$start", "0").replace("$end", "10")
                        .replace("$step", "1").replace("$i", "i");
                if (i == 0) {
                    insertText(snippetLine);
                } else {

                    String currentLine = lines.get(cursorLine);
                    String before = currentLine.substring(0, cursorColumn);
                    String after = currentLine.substring(cursorColumn);
                    lines.set(cursorLine, before);
                    lines.add(cursorLine + 1, snippetLine + after);
                    cursorLine++;
                    cursorColumn = snippetLine.length();
                }
            }
        } else {

            lines.set(cursorLine, line.substring(0, prefixStart) + selected + line.substring(cursorColumn));
            cursorColumn = prefixStart + selected.length();

            if (getCommands().contains(selected.toLowerCase())) {
                insertText(" ");
            }
        }

        hideAutocomplete();
        ensureCursorVisible();
    }

    private void hideAutocomplete() {
        showAutocomplete = false;
        autocompleteItems.clear();
        autocompleteSelectedIndex = 0;
        autocompletePrefix = "";
    }

    private String getCurrentWord() {
        if (cursorLine >= lines.size())
            return "";
        String line = lines.get(cursorLine);
        if (cursorColumn == 0)
            return "";

        int start = cursorColumn - 1;
        while (start >= 0) {
            char c = line.charAt(start);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
                break;
            }
            start--;
        }
        start++;

        if (start >= cursorColumn)
            return "";
        return line.substring(start, cursorColumn);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (showAutocomplete && button == 0) {
            int itemsToShow = Math.min(autocompleteItems.size(), AUTOCOMPLETE_MAX_ITEMS);
            int popupWidth = 200;
            int popupHeight = itemsToShow * AUTOCOMPLETE_ITEM_HEIGHT + 4;

            if (mouseX >= autocompleteX && mouseX < autocompleteX + popupWidth &&
                    mouseY >= autocompleteY && mouseY < autocompleteY + popupHeight) {

                int clickedIndex = (int) ((mouseY - autocompleteY - 2) / AUTOCOMPLETE_ITEM_HEIGHT);
                if (clickedIndex >= 0 && clickedIndex < itemsToShow) {
                    autocompleteSelectedIndex = clickedIndex;
                    acceptAutocomplete();
                    return true;
                }
            } else {

                hideAutocomplete();
            }
        }

        focused = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        if (focused && button == 0) {

            int clickedLine = (int) ((mouseY - y - PADDING) / LINE_HEIGHT) + scrollY;
            clickedLine = Math.max(0, Math.min(lines.size() - 1, clickedLine));

            if (mouseX < x + LINE_NUMBER_WIDTH) {
                toggleBreakpoint(clickedLine + 1);
                return true;
            }

            int clickedColumn = 0;
            if (mouseX > x + LINE_NUMBER_WIDTH + PADDING) {
                String line = lines.get(clickedLine);
                int textX = x + LINE_NUMBER_WIDTH + PADDING - scrollX;
                for (int i = 0; i <= line.length(); i++) {
                    int charX = textX + getTextWidth(line.substring(0, i));
                    if (charX > mouseX) {
                        clickedColumn = Math.max(0, i - 1);
                        break;
                    }
                    clickedColumn = i;
                }
            }

            cursorLine = clickedLine;
            cursorColumn = Math.min(clickedColumn, lines.get(cursorLine).length());
            cursorBlinkTime = System.currentTimeMillis();
            hideAutocomplete();
            return true;
        }

        return focused;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

        if (horizontalAmount != 0) {
            int maxLineWidth = getMaxLineWidth();
            int contentWidth = width - LINE_NUMBER_WIDTH - PADDING * 2 - 10;
            int maxScrollX = Math.max(0, maxLineWidth - contentWidth);
            scrollX = Math.max(0, Math.min(maxScrollX, scrollX - (int) (horizontalAmount * 20)));
        }

        if (verticalAmount != 0) {
            int visibleLines = height / LINE_HEIGHT;
            scrollY = Math.max(0, Math.min(lines.size() - visibleLines, scrollY - (int) verticalAmount * 3));
        }
        return true;
    }

    private void ensureCursorVisible() {
        int visibleLines = height / LINE_HEIGHT - 1;
        if (cursorLine < scrollY) {
            scrollY = cursorLine;
        } else if (cursorLine >= scrollY + visibleLines) {
            scrollY = cursorLine - visibleLines + 1;
        }

        int cursorX = getTextWidth(getCurrentLineBeforeCursor());
        int contentWidth = width - LINE_NUMBER_WIDTH - PADDING * 2 - 10;

        if (cursorX - scrollX > contentWidth - 20) {
            scrollX = cursorX - contentWidth + 50;
        } else if (cursorX < scrollX) {
            scrollX = Math.max(0, cursorX - 20);
        }
    }

    private String getCurrentLineBeforeCursor() {
        if (cursorLine < lines.size()) {
            String line = lines.get(cursorLine);
            return line.substring(0, Math.min(cursorColumn, line.length()));
        }
        return "";
    }

    private int getTextWidth(String text) {
        return textRenderer.getWidth(text);
    }

    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, (color & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public String getText() {
        return String.join("\n", lines);
    }

    public void setText(String text) {
        lines.clear();
        if (text == null || text.isEmpty()) {
            lines.add("");
        } else {

            text = sanitizeText(text);
            for (String line : text.split("\n", -1)) {
                lines.add(line);
            }
        }
        cursorLine = 0;
        cursorColumn = 0;
        scrollY = 0;
    }

    private String sanitizeText(String text) {
        if (text == null)
            return "";

        text = text.replace("\r\n", "\n").replace("\r", "\n");

        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        text = text.replace("\u00A0", " ");
        text = text.replace("\u2028", "\n");
        text = text.replace("\u2029", "\n");
        text = text.replace("\u0000", "");
        text = text.replace("\u000B", "");
        text = text.replace("\u000C", "");
        text = text.replace("\u0085", "\n");

        return text;
    }

    public void setTheme(EditorTheme theme) {
        this.theme = theme;
    }

    public int getCurrentLine() {
        return cursorLine + 1;
    }

    public int getCurrentColumn() {
        return cursorColumn + 1;
    }

    private void validateSyntax() {
        lineErrors.clear();

        Set<String> userFunctions = new HashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            String rest = null;
            if (trimmed.startsWith("function ")) {
                rest = trimmed.substring(9).trim();
            } else if (trimmed.startsWith("fn ")) {
                rest = trimmed.substring(3).trim();
            }
            if (rest != null) {

                int parenIdx = rest.indexOf('(');
                int braceIdx = rest.indexOf('{');
                int endIdx = parenIdx > 0 ? parenIdx : (braceIdx > 0 ? braceIdx : rest.length());
                String funcName = rest.substring(0, endIdx).trim();
                if (!funcName.isEmpty()) {
                    userFunctions.add(funcName.toLowerCase());
                }
            }
        }

        int braceDepth = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }

            for (char c : line.toCharArray()) {
                if (c == '{')
                    braceDepth++;
                if (c == '}')
                    braceDepth--;
            }

            if (braceDepth < 0) {
                lineErrors.put(i, "Unexpected closing brace '}'");
                braceDepth = 0;
                continue;
            }

            if (line.equals("}") || line.startsWith("} else") || line.equals("else {") || line.equals("else")) {
                continue;
            }

            if (line.startsWith("if ") || line.startsWith("if(") ||
                    line.startsWith("while ") || line.startsWith("while(") ||
                    line.startsWith("for ") || line.startsWith("for(")) {

                if (!line.contains("{") && !line.endsWith("{")) {

                }
                continue;
            }

            if (line.startsWith("else if ") || line.startsWith("} else if ") ||
                    line.startsWith("else if(") || line.startsWith("} else if(")) {
                continue;
            }

            if (line.startsWith("loop")) {
                continue;
            }

            if (line.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*") ||
                    line.matches("^let\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*") ||
                    line.matches("^const\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*")) {
                continue;
            }

            if (line.startsWith("function ") || line.startsWith("fn ")) {
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();

            if (KEYWORDS.contains(commandName)) {
                continue;
            }

            if (userFunctions.contains(commandName)) {
                continue;
            }

            if (!getCommands().contains(commandName) && CommandRegistry.getCommand(commandName) == null) {
                lineErrors.put(i, "Unknown command: " + commandName);
            }
        }

        if (braceDepth > 0) {
            lineErrors.put(lines.size() - 1, "Unclosed brace - missing '}'");
        }
    }

    public String getCurrentLineError() {
        return lineErrors.get(cursorLine);
    }

    public int getErrorCount() {
        return lineErrors.size();
    }

    public Map<Integer, String> getErrors() {
        return Collections.unmodifiableMap(lineErrors);
    }

    private void clearSelection() {
        hasSelection = false;
        selectionStartLine = -1;
        selectionStartColumn = -1;
        selectionEndLine = -1;
        selectionEndColumn = -1;
    }

    private void startSelection() {
        if (!hasSelection) {
            selectionStartLine = cursorLine;
            selectionStartColumn = cursorColumn;
            selectionEndLine = cursorLine;
            selectionEndColumn = cursorColumn;
            hasSelection = true;
        }
    }

    private void updateSelection() {
        if (hasSelection) {
            selectionEndLine = cursorLine;
            selectionEndColumn = cursorColumn;
        }
    }

    public void selectAll() {
        selectionStartLine = 0;
        selectionStartColumn = 0;
        selectionEndLine = lines.size() - 1;
        selectionEndColumn = lines.get(lines.size() - 1).length();
        hasSelection = true;
        cursorLine = selectionEndLine;
        cursorColumn = selectionEndColumn;
    }

    private int[] getSelectionStart() {
        if (!hasSelection)
            return null;

        int startLine = Math.max(0, Math.min(selectionStartLine, lines.size() - 1));
        int endLine = Math.max(0, Math.min(selectionEndLine, lines.size() - 1));
        int startCol = Math.max(0, Math.min(selectionStartColumn, lines.get(startLine).length()));
        int endCol = Math.max(0, Math.min(selectionEndColumn, lines.get(endLine).length()));

        if (startLine < endLine || (startLine == endLine && startCol <= endCol)) {
            return new int[] { startLine, startCol };
        }
        return new int[] { endLine, endCol };
    }

    private int[] getSelectionEnd() {
        if (!hasSelection)
            return null;

        int startLine = Math.max(0, Math.min(selectionStartLine, lines.size() - 1));
        int endLine = Math.max(0, Math.min(selectionEndLine, lines.size() - 1));
        int startCol = Math.max(0, Math.min(selectionStartColumn, lines.get(startLine).length()));
        int endCol = Math.max(0, Math.min(selectionEndColumn, lines.get(endLine).length()));

        if (startLine < endLine || (startLine == endLine && startCol <= endCol)) {
            return new int[] { endLine, endCol };
        }
        return new int[] { startLine, startCol };
    }

    public String getSelectedText() {
        if (!hasSelection)
            return "";

        int[] start = getSelectionStart();
        int[] end = getSelectionEnd();

        if (start[0] == end[0]) {

            return lines.get(start[0]).substring(start[1], end[1]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(lines.get(start[0]).substring(start[1]));
        sb.append("\n");

        for (int i = start[0] + 1; i < end[0]; i++) {
            sb.append(lines.get(i));
            sb.append("\n");
        }

        sb.append(lines.get(end[0]).substring(0, end[1]));
        return sb.toString();
    }

    public void deleteSelection() {
        if (!hasSelection)
            return;

        saveUndoState();

        int[] start = getSelectionStart();
        int[] end = getSelectionEnd();

        if (start == null || end == null) {
            clearSelection();
            return;
        }

        if (start[0] < 0 || start[0] >= lines.size() || end[0] < 0 || end[0] >= lines.size()) {
            clearSelection();
            return;
        }

        String startLineText = lines.get(start[0]);
        String endLineText = lines.get(end[0]);

        start[1] = Math.max(0, Math.min(start[1], startLineText.length()));
        end[1] = Math.max(0, Math.min(end[1], endLineText.length()));

        if (start[0] == end[0]) {

            int minCol = Math.min(start[1], end[1]);
            int maxCol = Math.max(start[1], end[1]);
            String line = lines.get(start[0]);
            lines.set(start[0], line.substring(0, minCol) + line.substring(maxCol));
            cursorColumn = minCol;
        } else {

            String startLine = startLineText.substring(0, start[1]);
            String endLine = endLineText.substring(end[1]);

            for (int i = end[0]; i > start[0]; i--) {
                if (i < lines.size()) {
                    lines.remove(i);
                }
            }

            lines.set(start[0], startLine + endLine);
            cursorColumn = start[1];
        }

        cursorLine = start[0];
        clearSelection();
    }

    public void copy() {
        if (!hasSelection)
            return;
        String text = getSelectedText();
        MinecraftClient.getInstance().keyboard.setClipboard(text);
    }

    public void cut() {
        if (!hasSelection)
            return;
        copy();
        deleteSelection();
    }

    public void paste() {
        String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clipboard == null || clipboard.isEmpty())
            return;

        clipboard = sanitizeText(clipboard);

        saveUndoState();

        if (hasSelection) {
            deleteSelection();
        }

        String[] clipLines = clipboard.split("\n", -1);

        if (clipLines.length == 1) {

            insertText(clipLines[0]);
        } else {

            String currentLine = lines.get(cursorLine);
            String before = currentLine.substring(0, cursorColumn);
            String after = currentLine.substring(cursorColumn);

            lines.set(cursorLine, before + clipLines[0]);

            for (int i = 1; i < clipLines.length - 1; i++) {
                lines.add(cursorLine + i, clipLines[i]);
            }

            lines.add(cursorLine + clipLines.length - 1, clipLines[clipLines.length - 1] + after);

            cursorLine += clipLines.length - 1;
            cursorColumn = clipLines[clipLines.length - 1].length();
        }

        ensureCursorVisible();
    }

    private void saveUndoState() {
        long now = System.currentTimeMillis();
        if (now - lastUndoSaveTime < UNDO_SAVE_INTERVAL && !undoStack.isEmpty()) {
            return;
        }

        undoStack.push(new UndoState(new ArrayList<>(lines), cursorLine, cursorColumn));
        redoStack.clear();
        lastUndoSaveTime = now;

        while (undoStack.size() > 100) {
            undoStack.removeLast();
        }
    }

    public void undo() {
        if (undoStack.isEmpty())
            return;

        redoStack.push(new UndoState(new ArrayList<>(lines), cursorLine, cursorColumn));

        UndoState state = undoStack.pop();
        lines.clear();
        lines.addAll(state.lines);
        cursorLine = Math.min(state.cursorLine, lines.size() - 1);
        cursorColumn = Math.min(state.cursorColumn, lines.get(cursorLine).length());

        clearSelection();
        ensureCursorVisible();
    }

    public void redo() {
        if (redoStack.isEmpty())
            return;

        undoStack.push(new UndoState(new ArrayList<>(lines), cursorLine, cursorColumn));

        UndoState state = redoStack.pop();
        lines.clear();
        lines.addAll(state.lines);
        cursorLine = Math.min(state.cursorLine, lines.size() - 1);
        cursorColumn = Math.min(state.cursorColumn, lines.get(cursorLine).length());

        clearSelection();
        ensureCursorVisible();
    }

    public void duplicateLine() {
        saveUndoState();
        String currentLine = lines.get(cursorLine);
        lines.add(cursorLine + 1, currentLine);
        cursorLine++;
        ensureCursorVisible();
    }

    public void deleteLine() {
        if (lines.size() <= 1) {
            lines.set(0, "");
            cursorColumn = 0;
            return;
        }

        saveUndoState();
        lines.remove(cursorLine);
        if (cursorLine >= lines.size()) {
            cursorLine = lines.size() - 1;
        }
        cursorColumn = Math.min(cursorColumn, lines.get(cursorLine).length());
        ensureCursorVisible();
    }

    public void moveLineUp() {
        if (cursorLine <= 0)
            return;

        saveUndoState();
        String line = lines.remove(cursorLine);
        cursorLine--;
        lines.add(cursorLine, line);
        ensureCursorVisible();
    }

    public void moveLineDown() {
        if (cursorLine >= lines.size() - 1)
            return;

        saveUndoState();
        String line = lines.remove(cursorLine);
        cursorLine++;
        lines.add(cursorLine, line);
        ensureCursorVisible();
    }

    private static class UndoState {
        final List<String> lines;
        final int cursorLine;
        final int cursorColumn;

        UndoState(List<String> lines, int cursorLine, int cursorColumn) {
            this.lines = lines;
            this.cursorLine = cursorLine;
            this.cursorColumn = cursorColumn;
        }
    }

    private String currentFile = null;

    public void setCurrentFile(String file) {
        this.currentFile = file;

        this.breakpoints.clear();
        if (file != null) {
            List<Integer> fileBps = DebugManager.getInstance().getBreakpoints(file);
            this.breakpoints.addAll(fileBps);
        }
    }

    public void toggleBreakpoint(int line) {
        if (breakpoints.contains(line)) {
            breakpoints.remove(line);
            if (currentFile != null) {
                DebugManager.getInstance().toggleBreakpoint(currentFile, line);
            }
        } else {
            breakpoints.add(line);
            if (currentFile != null) {
                DebugManager.getInstance().toggleBreakpoint(currentFile, line);
            }
        }
    }

    public Set<Integer> getBreakpoints() {
        return Collections.unmodifiableSet(breakpoints);
    }

    public boolean hasBreakpoint(int line) {
        return breakpoints.contains(line);
    }

    public void setExecutionLine(int line) {
        this.executionLine = line;

        if (line > 0) {
            cursorLine = line - 1;
            ensureCursorVisible();
        }
    }

    public int getExecutionLine() {
        return executionLine;
    }
}

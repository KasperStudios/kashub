package kasperstudios.kashub.gui.widgets;

import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ConsolePanel — the KHinux terminal window.
 *
 * Supports two modes:
 *   KASHUB   — logs from KasHub scripts/system, input routed to TerminalManager.
 *   CC       — proxies input/output to a CC:T computer via network packets.
 */
public class ConsolePanel {

    public enum Mode { KASHUB, CC }

    // ─── Layout ────────────────────────────────────────────────────────────────
    private int x, y, width, height;
    private EditorTheme theme;
    private final Font textRenderer;

    private static final int LINE_HEIGHT     = 14;
    private static final int HEADER_HEIGHT   = 24;
    private static final int INPUT_HEIGHT    = 22;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─── Log levels ────────────────────────────────────────────────────────────
    public enum LogLevel { INFO, WARN, ERROR, SUCCESS, DEBUG, COMMAND }

    // ─── State ─────────────────────────────────────────────────────────────────
    private final List<LogEntry> logs       = new ArrayList<>();
    private final int maxLogs              = 1000;
    private int scrollY                    = 0;

    /** Current terminal mode. */
    private Mode mode                      = Mode.KASHUB;
    /** CC computer ID, only used in CC mode. */
    private int ccComputerId               = -1;

    /** Live input string. */
    private String currentInput            = "";
    /** History of submitted commands. */
    private final List<String> history     = new ArrayList<>();
    /** Navigation index in history (-1 = live input). */
    private int historyIndex               = -1;
    /** Stashed live input while browsing history. */
    private String savedInput              = "";

    /** Disconnect callback, only used in CC mode. May be null. */
    private Runnable onDisconnect = null;

    public void setOnDisconnect(Runnable callback) {
        this.onDisconnect = callback;
    }

    public ConsolePanel(int x, int y, int width, int height, EditorTheme theme) {
        this.x           = x;
        this.y           = y;
        this.width       = width;
        this.height      = height;
        this.theme       = theme;
        this.textRenderer = Minecraft.getInstance().font;
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /** Switch terminal mode. Clears the input line, keeps the log. */
    public void setMode(Mode mode) {
        this.mode         = mode;
        this.currentInput = "";
        this.historyIndex = -1;
        this.savedInput   = "";
    }

    /** Set the CC computer to route input/output to (only used in CC mode). */
    public void setComputerId(int id) {
        this.ccComputerId = id;
    }

    public Mode getMode() { return mode; }

    public void log(String message, LogLevel level) {
        String time = LocalTime.now().format(TIME_FORMAT);
        logs.add(new LogEntry(time, message, level));
        while (logs.size() > maxLogs) logs.remove(0);
        scrollToBottom();
    }

    public void clear() {
        logs.clear();
        scrollY = 0;
    }

    // ─── Rendering ─────────────────────────────────────────────────────────────

    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(x, y, x + width, y + height, theme.consoleBackground);

        renderHeader(ctx);

        int listY      = y + HEADER_HEIGHT;
        int listHeight = height - HEADER_HEIGHT - INPUT_HEIGHT;

        renderLogs(ctx, listY, listHeight);
        renderScrollbar(ctx, listY, listHeight);
        renderInputBar(ctx, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics ctx) {
        ctx.fill(x, y, x + width, y + HEADER_HEIGHT, adjustBrightness(theme.consoleBackground, 10));

        String title;
        if (mode == Mode.CC && ccComputerId >= 0) {
            title = "TERMINAL \u2500 CC#" + ccComputerId;
        } else {
            String perm = "";
            boolean sudo = kasperstudios.kashub.core.TerminalManager.getInstance().hasSudo();
            boolean su   = kasperstudios.kashub.core.TerminalManager.getInstance().hasSu();
            if (su)        perm = " [SU]";
            else if (sudo) perm = " [SUDO]";
            title = "TERMINAL" + perm;
        }
        ctx.drawString(textRenderer, title, x + 10, y + 7, theme.textColor, true);

        int buttonAreaRight = x + width - 10;

        if (mode == Mode.CC && ccComputerId >= 0 && onDisconnect != null) {
            int dcX = buttonAreaRight - 110;
            boolean dcHovered = mouseInside(dcX, y + 4, 50, 16, 0, 0);
            ctx.fill(dcX, y + 4, dcX + 50, y + 20,
                    dcHovered ? 0xFFE74C3C : 0x44E74C3C);
            ctx.drawString(textRenderer, "Discon", dcX + 5, y + 7, theme.textColor, false);
        }

        int clearX = buttonAreaRight - 50;
        boolean clearHovered = mouseInside(clearX, y + 4, 40, 16, 0, 0);
        ctx.fill(clearX, y + 4, clearX + 40, y + 20,
                clearHovered ? theme.buttonHoverColor : theme.buttonColor);
        ctx.drawString(textRenderer, "Clear", clearX + 6, y + 7, theme.textColor, false);

        ctx.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT,
                theme.accentColor & 0x66FFFFFF);
    }

    private void renderLogs(GuiGraphics ctx, int listY, int listHeight) {
        int visibleLines = listHeight / LINE_HEIGHT;
        int startLine    = scrollY / LINE_HEIGHT;
        int endLine      = Math.min(startLine + visibleLines + 1, logs.size());

        for (int i = startLine; i < endLine; i++) {
            LogEntry entry = logs.get(i);
            int lineY = listY + (i * LINE_HEIGHT) - scrollY + 4;

            if (lineY < listY || lineY > y + height - INPUT_HEIGHT - LINE_HEIGHT) continue;

            ctx.drawString(textRenderer, entry.time, x + 6, lineY, theme.textDimColor, false);

            String icon  = getLevelIcon(entry.level);
            int    color = getLevelColor(entry.level);
            ctx.drawString(textRenderer, icon, x + 60, lineY, color, false);
            // MC's drawString already handles §-codes, no extra parsing needed.
            ctx.drawString(textRenderer, Component.literal(entry.message), x + 76, lineY, color, false);
        }
    }

    private void renderScrollbar(GuiGraphics ctx, int listY, int listHeight) {
        int totalHeight = logs.size() * LINE_HEIGHT;
        if (totalHeight <= listHeight) return;

        int scrollbarH = Math.max(20, (listHeight * listHeight) / totalHeight);
        int maxScroll  = totalHeight - listHeight;
        int scrollbarY = listY + (scrollY * (listHeight - scrollbarH)) / Math.max(1, maxScroll);

        ctx.fill(x + width - 4, listY, x + width, y + height - INPUT_HEIGHT, 0x22FFFFFF);
        ctx.fill(x + width - 4, scrollbarY, x + width, scrollbarY + scrollbarH, theme.accentColor);
    }

    private void renderInputBar(GuiGraphics ctx, int mouseX, int mouseY) {
        int barY = y + height - INPUT_HEIGHT;

        // Separator line
        ctx.fill(x, barY, x + width, barY + 1, theme.accentColor & 0x44FFFFFF);
        ctx.fill(x, barY + 1, x + width, y + height, adjustBrightness(theme.consoleBackground, 6));

        // Prompt
        String prompt = buildPrompt();
        int promptW   = textRenderer.width(prompt);
        ctx.drawString(textRenderer, prompt, x + 6, barY + 6, theme.accentColor, false);

        // Input text
        String displayInput = currentInput;
        int inputX = x + 6 + promptW + 4;
        int maxInputW = width - promptW - 20;

        // Clip to visible tail if too long
        String visibleInput = displayInput;
        while (textRenderer.width(visibleInput) > maxInputW && !visibleInput.isEmpty()) {
            visibleInput = visibleInput.substring(1);
        }
        ctx.drawString(textRenderer, visibleInput, inputX, barY + 6, theme.textColor, false);

        // Blinking cursor
        if (System.currentTimeMillis() % 1000 < 500) {
            int cursorX = inputX + textRenderer.width(visibleInput);
            ctx.fill(cursorX, barY + 5, cursorX + 1, barY + INPUT_HEIGHT - 5, theme.textColor);
        }
    }

    // ─── Input handling ────────────────────────────────────────────────────────

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter → execute
        if (keyCode == 257 || keyCode == 335) {
            if (!currentInput.trim().isEmpty()) {
                submitInput();
            }
            return true;
        }

        // Backspace
        if (keyCode == 259) {
            if (!currentInput.isEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length() - 1);
                historyIndex = -1;
            }
            return true;
        }

        // Arrow Up — history back
        if (keyCode == 265) {
            if (history.isEmpty()) return true;
            if (historyIndex == -1) {
                savedInput   = currentInput;
                historyIndex = history.size() - 1;
            } else if (historyIndex > 0) {
                historyIndex--;
            }
            currentInput = history.get(historyIndex);
            return true;
        }

        // Arrow Down — history forward
        if (keyCode == 264) {
            if (historyIndex == -1) return true;
            historyIndex++;
            if (historyIndex >= history.size()) {
                historyIndex = -1;
                currentInput = savedInput;
            } else {
                currentInput = history.get(historyIndex);
            }
            return true;
        }

        // Ctrl+C — interrupt / terminate
        if (keyCode == 67 && (modifiers & 2) != 0) {
            if (mode == Mode.CC && ccComputerId >= 0) {
                sendCCInput("\u0003"); // CC:T terminate signal
            } else {
                kasperstudios.kashub.core.TaskManager.getInstance().stopAll();
                ScriptLogger.getInstance().warn("[KasHub] Ctrl+C — all tasks stopped.");
            }
            currentInput = "";
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32) {
            currentInput += chr;
            historyIndex  = -1;
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int buttonAreaRight = x + width - 10;

        if (mode == Mode.CC && ccComputerId >= 0 && onDisconnect != null) {
            int dcX = buttonAreaRight - 110;
            if (mouseX >= dcX && mouseX < dcX + 50 && mouseY >= y + 4 && mouseY < y + 20) {
                onDisconnect.run();
                return true;
            }
        }

        int clearX = buttonAreaRight - 50;
        if (mouseX >= clearX && mouseX < clearX + 40 && mouseY >= y + 4 && mouseY < y + 20) {
            clear();
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int listHeight = height - HEADER_HEIGHT - INPUT_HEIGHT;
        int maxScroll  = Math.max(0, logs.size() * LINE_HEIGHT - listHeight);
        scrollY        = Math.max(0, Math.min(maxScroll, scrollY - (int) (amount * LINE_HEIGHT * 2)));
        return true;
    }

    // ─── Misc public ───────────────────────────────────────────────────────────

    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    public void setTheme(EditorTheme theme) { this.theme = theme; }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private void submitInput() {
        String cmd = currentInput.trim();

        // Echo the command into the log
        log(buildPrompt() + cmd, LogLevel.COMMAND);

        // Push to history (avoid consecutive duplicates)
        if (history.isEmpty() || !history.get(history.size() - 1).equals(cmd)) {
            history.add(cmd);
            if (history.size() > 200) history.remove(0);
        }
        historyIndex = -1;
        currentInput = "";
        savedInput   = "";

        // Route to the right backend
        if (mode == Mode.CC && ccComputerId >= 0) {
            sendCCInput(cmd);
        } else {
            kasperstudios.kashub.core.TerminalManager.getInstance().execute(cmd);
        }
    }

    private void sendCCInput(String cmd) {
        kasperstudios.kashub.network.NetworkingManager.getInstance()
                .sendCCTerminalInput(ccComputerId, cmd);
    }

    private String buildPrompt() {
        if (mode == Mode.CC && ccComputerId >= 0) {
            return "cc#" + ccComputerId + "> ";
        }
        String dir = kasperstudios.kashub.core.TerminalManager.getInstance().getCurrentDir();
        // Shorten ~/
        if (dir.equals("~/")) dir = "~";
        boolean su   = kasperstudios.kashub.core.TerminalManager.getInstance().hasSu();
        boolean sudo = kasperstudios.kashub.core.TerminalManager.getInstance().hasSudo();
        String sigil  = su ? "#" : (sudo ? "$+" : "$ ");
        return "user@khinux:" + dir + sigil;
    }

    private void scrollToBottom() {
        int listHeight = height - HEADER_HEIGHT - INPUT_HEIGHT;
        int total      = logs.size() * LINE_HEIGHT;
        scrollY        = Math.max(0, total - listHeight);
    }

    private boolean mouseInside(int rx, int ry, int rw, int rh, int mx, int my) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    private String getLevelIcon(LogLevel level) {
        return switch (level) {
            case INFO    -> "i";
            case WARN    -> "!";
            case ERROR   -> "x";
            case SUCCESS -> "v";
            case DEBUG   -> "d";
            case COMMAND -> ">";
        };
    }

    private int getLevelColor(LogLevel level) {
        return switch (level) {
            case INFO    -> theme.consoleInfoColor;
            case WARN    -> theme.consoleWarnColor;
            case ERROR   -> theme.consoleErrorColor;
            case SUCCESS -> theme.consoleSuccessColor;
            case DEBUG   -> theme.textDimColor;
            case COMMAND -> theme.accentColor;
        };
    }

    private int adjustBrightness(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, Math.min(255, ((color >> 16) & 0xFF) + amount));
        int g = Math.max(0, Math.min(255, ((color >>  8) & 0xFF) + amount));
        int b = Math.max(0, Math.min(255, ( color        & 0xFF) + amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ─── Data class ────────────────────────────────────────────────────────────

    private static class LogEntry {
        final String   time;
        final String   message;
        final LogLevel level;
        LogEntry(String time, String message, LogLevel level) {
            this.time = time; this.message = message; this.level = level;
        }
    }
}

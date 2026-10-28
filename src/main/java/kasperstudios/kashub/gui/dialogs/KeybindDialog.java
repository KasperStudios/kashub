package kasperstudios.kashub.gui.dialogs;

import com.mojang.blaze3d.platform.InputConstants;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class KeybindDialog extends Screen {
    private final Screen parent;
    private final String scriptName;
    private EditorTheme theme;

    private boolean waitingForKey = false;
    private int currentKey = -1;

    private int getDialogWidth() { return this.width; }
    private int getDialogHeight() { return this.height; }

    public KeybindDialog(Screen parent, String scriptName) {
        super(Component.literal("Set Keybind"));
        this.parent = parent;
        this.scriptName = scriptName;
        this.theme = ThemeManager.getCurrentTheme();
        this.font = Minecraft.getInstance().font;

        this.currentKey = KashubConfig.getInstance().getKeyForScript(scriptName);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {

    }

    private int getDialogX() {
        return 0;
    }

    private int getDialogY() {
        return 0;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {

        context.fill(0, 0, this.width, this.height, 0xCC000000);

        int dx = getDialogX();
        int dy = getDialogY();

        context.fill(dx, dy, dx + getDialogWidth(), dy + getDialogHeight(), theme.sidebarColor);

        // Borders removed for fullscreen mode

        String title = "⌨ Set Keybind for: " + scriptName;
        context.drawString(font, title, dx + 12, dy + 12, theme.textColor, true);

        String keyText;
        if (waitingForKey) {
            keyText = "Press any key... (ESC to cancel)";
        } else if (currentKey > 0) {
            keyText = "Current: " + getKeyName(currentKey);
        } else {
            keyText = "No keybind set";
        }

        int keyBoxY = dy + 45;
        context.fill(dx + 12, keyBoxY, dx + getDialogWidth() - 12, keyBoxY + 30,
            waitingForKey ? (theme.accentColor & 0x44FFFFFF) : theme.buttonColor);

        int textWidth = font.width(keyText);
        context.drawString(font, keyText, dx + (getDialogWidth() - textWidth) / 2, keyBoxY + 10,
            waitingForKey ? theme.accentColor : theme.textColor, false);

        int buttonY = dy + getDialogHeight() - 40;
        int buttonWidth = 80;
        int spacing = 10;

        int setX = dx + 12;
        boolean setHovered = mouseX >= setX && mouseX < setX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(setX, buttonY, setX + buttonWidth, buttonY + 24,
            setHovered ? brighten(theme.consoleSuccessColor, 20) : theme.consoleSuccessColor);
        context.drawString(font, "Set Key", setX + 14, buttonY + 8, 0xFFFFFFFF, true);

        int clearX = setX + buttonWidth + spacing;
        boolean clearHovered = mouseX >= clearX && mouseX < clearX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(clearX, buttonY, clearX + buttonWidth, buttonY + 24,
            clearHovered ? brighten(theme.consoleWarnColor, 20) : theme.consoleWarnColor);
        context.drawString(font, "Clear", clearX + 22, buttonY + 8, 0xFFFFFFFF, true);

        int cancelX = dx + getDialogWidth() - buttonWidth - 12;
        boolean cancelHovered = mouseX >= cancelX && mouseX < cancelX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(cancelX, buttonY, cancelX + buttonWidth, buttonY + 24,
            cancelHovered ? brighten(theme.buttonColor, 20) : theme.buttonColor);
        context.drawString(font, "Cancel", cancelX + 18, buttonY + 8, 0xFFFFFFFF, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int dx = getDialogX();
        int dy = getDialogY();
        int buttonY = dy + getDialogHeight() - 40;
        int buttonWidth = 80;
        int spacing = 10;

        int keyBoxY = dy + 45;
        if (mouseX >= dx + 12 && mouseX < dx + getDialogWidth() - 12 &&
            mouseY >= keyBoxY && mouseY < keyBoxY + 30) {
            waitingForKey = true;
            return true;
        }

        int setX = dx + 12;
        if (mouseX >= setX && mouseX < setX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            waitingForKey = true;
            return true;
        }

        int clearX = setX + buttonWidth + spacing;
        if (mouseX >= clearX && mouseX < clearX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            KashubConfig.getInstance().removeScriptKeybind(scriptName);
            currentKey = -1;
            return true;
        }

        int cancelX = dx + getDialogWidth() - buttonWidth - 12;
        if (mouseX >= cancelX && mouseX < cancelX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
                return true;
            }

            currentKey = keyCode;
            KashubConfig.getInstance().setScriptKeybind(keyCode, scriptName);
            waitingForKey = false;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String getKeyName(int keyCode) {
        String name = InputConstants.getKey(keyCode, 0).getDisplayName().getString();
        return name.isEmpty() ? "Key " + keyCode : name;
    }

    private int brighten(int color, int amount) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

package kasperstudios.kashub.gui.dialogs;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.theme.EditorTheme;
import kasperstudios.kashub.gui.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Dialog for setting a keybind for a script
 */
public class KeybindDialog extends Screen {
    private final Screen parent;
    private final String scriptName;
    private EditorTheme theme;
    private final TextRenderer textRenderer;
    
    private boolean waitingForKey = false;
    private int currentKey = -1;
    
    private static final int DIALOG_WIDTH = 300;
    private static final int DIALOG_HEIGHT = 150;
    
    public KeybindDialog(Screen parent, String scriptName) {
        super(Text.literal("Set Keybind"));
        this.parent = parent;
        this.scriptName = scriptName;
        this.theme = ThemeManager.getCurrentTheme();
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        
        // Load current keybind
        this.currentKey = KashubConfig.getInstance().getKeyForScript(scriptName);
    }
    
    @Override
    protected void init() {
        super.init();
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't render default blurred background
    }
    
    private int getDialogX() {
        return (this.width - DIALOG_WIDTH) / 2;
    }
    
    private int getDialogY() {
        return (this.height - DIALOG_HEIGHT) / 2;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark background
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        
        int dx = getDialogX();
        int dy = getDialogY();
        
        // Dialog background
        context.fill(dx, dy, dx + DIALOG_WIDTH, dy + DIALOG_HEIGHT, theme.sidebarColor);
        
        // Border
        context.fill(dx, dy, dx + DIALOG_WIDTH, dy + 2, theme.accentColor);
        context.fill(dx, dy + DIALOG_HEIGHT - 2, dx + DIALOG_WIDTH, dy + DIALOG_HEIGHT, theme.accentColor);
        context.fill(dx, dy, dx + 2, dy + DIALOG_HEIGHT, theme.accentColor);
        context.fill(dx + DIALOG_WIDTH - 2, dy, dx + DIALOG_WIDTH, dy + DIALOG_HEIGHT, theme.accentColor);
        
        // Title
        String title = "⌨ Set Keybind for: " + scriptName;
        context.drawText(textRenderer, title, dx + 12, dy + 12, theme.textColor, true);
        
        // Current keybind display
        String keyText;
        if (waitingForKey) {
            keyText = "Press any key... (ESC to cancel)";
        } else if (currentKey > 0) {
            keyText = "Current: " + getKeyName(currentKey);
        } else {
            keyText = "No keybind set";
        }
        
        int keyBoxY = dy + 45;
        context.fill(dx + 12, keyBoxY, dx + DIALOG_WIDTH - 12, keyBoxY + 30, 
            waitingForKey ? (theme.accentColor & 0x44FFFFFF) : theme.buttonColor);
        
        int textWidth = textRenderer.getWidth(keyText);
        context.drawText(textRenderer, keyText, dx + (DIALOG_WIDTH - textWidth) / 2, keyBoxY + 10, 
            waitingForKey ? theme.accentColor : theme.textColor, false);
        
        // Buttons
        int buttonY = dy + DIALOG_HEIGHT - 40;
        int buttonWidth = 80;
        int spacing = 10;
        
        // Set button
        int setX = dx + 12;
        boolean setHovered = mouseX >= setX && mouseX < setX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(setX, buttonY, setX + buttonWidth, buttonY + 24, 
            setHovered ? brighten(theme.consoleSuccessColor, 20) : theme.consoleSuccessColor);
        context.drawText(textRenderer, "Set Key", setX + 14, buttonY + 8, 0xFFFFFFFF, true);
        
        // Clear button
        int clearX = setX + buttonWidth + spacing;
        boolean clearHovered = mouseX >= clearX && mouseX < clearX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(clearX, buttonY, clearX + buttonWidth, buttonY + 24,
            clearHovered ? brighten(theme.consoleWarnColor, 20) : theme.consoleWarnColor);
        context.drawText(textRenderer, "Clear", clearX + 22, buttonY + 8, 0xFFFFFFFF, true);
        
        // Cancel button
        int cancelX = dx + DIALOG_WIDTH - buttonWidth - 12;
        boolean cancelHovered = mouseX >= cancelX && mouseX < cancelX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24;
        context.fill(cancelX, buttonY, cancelX + buttonWidth, buttonY + 24,
            cancelHovered ? brighten(theme.buttonColor, 20) : theme.buttonColor);
        context.drawText(textRenderer, "Cancel", cancelX + 18, buttonY + 8, 0xFFFFFFFF, true);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int dx = getDialogX();
        int dy = getDialogY();
        int buttonY = dy + DIALOG_HEIGHT - 40;
        int buttonWidth = 80;
        int spacing = 10;
        
        // Key box click - start waiting for key
        int keyBoxY = dy + 45;
        if (mouseX >= dx + 12 && mouseX < dx + DIALOG_WIDTH - 12 && 
            mouseY >= keyBoxY && mouseY < keyBoxY + 30) {
            waitingForKey = true;
            return true;
        }
        
        // Set button
        int setX = dx + 12;
        if (mouseX >= setX && mouseX < setX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            waitingForKey = true;
            return true;
        }
        
        // Clear button
        int clearX = setX + buttonWidth + spacing;
        if (mouseX >= clearX && mouseX < clearX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            KashubConfig.getInstance().removeScriptKeybind(scriptName);
            currentKey = -1;
            return true;
        }
        
        // Cancel button
        int cancelX = dx + DIALOG_WIDTH - buttonWidth - 12;
        if (mouseX >= cancelX && mouseX < cancelX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + 24) {
            this.close();
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
            
            // Set the keybind
            currentKey = keyCode;
            KashubConfig.getInstance().setScriptKeybind(keyCode, scriptName);
            waitingForKey = false;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
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
    
    private String getKeyName(int keyCode) {
        String name = InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
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

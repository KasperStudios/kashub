package kasperstudios.kashub.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModernButton extends ButtonWidget {
    private final int baseColor;
    private boolean isHovered = false;
    private float hoverAnimation = 0f;
    
    public ModernButton(int x, int y, int width, int height, Text message, PressAction onPress, int color) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.baseColor = color;
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isMouseOver(mouseX, mouseY);
        
        // Smooth hover animation
        if (isHovered && hoverAnimation < 1f) {
            hoverAnimation = Math.min(1f, hoverAnimation + delta * 0.15f);
        } else if (!isHovered && hoverAnimation > 0f) {
            hoverAnimation = Math.max(0f, hoverAnimation - delta * 0.15f);
        }
        
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        
        // Background with hover effect
        int bgColor = interpolateColor(baseColor, brighten(baseColor, 30), hoverAnimation);
        
        // Draw rounded rectangle effect (corners)
        int cornerRadius = 4;
        
        // Main background
        context.fill(x + cornerRadius, y, x + w - cornerRadius, y + h, bgColor);
        context.fill(x, y + cornerRadius, x + w, y + h - cornerRadius, bgColor);
        
        // Corners (simplified rounded effect)
        context.fill(x + 1, y + 1, x + cornerRadius, y + cornerRadius, bgColor);
        context.fill(x + w - cornerRadius, y + 1, x + w - 1, y + cornerRadius, bgColor);
        context.fill(x + 1, y + h - cornerRadius, x + cornerRadius, y + h - 1, bgColor);
        context.fill(x + w - cornerRadius, y + h - cornerRadius, x + w - 1, y + h - 1, bgColor);
        
        // Glow effect on hover
        if (hoverAnimation > 0) {
            int glowAlpha = (int) (hoverAnimation * 40);
            int glowColor = (glowAlpha << 24) | (baseColor & 0x00FFFFFF);
            context.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);
        }
        
        // Border
        int borderColor = isHovered ? 0xFFFFFFFF : (baseColor | 0xFF000000);
        drawBorder(context, x, y, w, h, borderColor);
        
        // Text with shadow
        MinecraftClient client = MinecraftClient.getInstance();
        int textColor = this.active ? 0xFFFFFFFF : 0xFF888888;
        
        int textX = x + (w - client.textRenderer.getWidth(getMessage())) / 2;
        int textY = y + (h - 8) / 2;
        
        context.drawText(client.textRenderer, getMessage(), textX, textY, textColor, true);
    }
    
    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        // Top and bottom
        context.fill(x + 2, y, x + w - 2, y + 1, color);
        context.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        // Left and right
        context.fill(x, y + 2, x + 1, y + h - 2, color);
        context.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
    }
    
    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
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

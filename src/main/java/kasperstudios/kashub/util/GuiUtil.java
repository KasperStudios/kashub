package kasperstudios.kashub.util;

import net.minecraft.client.gui.DrawContext;

public class GuiUtil {
    public static void drawGradientRect(DrawContext context, int x1, int y1, int x2, int y2,
            int colorStart, int colorEnd, boolean vertical) {
        if (vertical) {
            for (int y = y1; y < y2; y++) {
                float progress = (float) (y - y1) / (y2 - y1);
                int color = interpolateColors(colorStart, colorEnd, progress);
                context.fill(x1, y, x2, y + 1, color);
            }
        } else {
            for (int x = x1; x < x2; x++) {
                float progress = (float) (x - x1) / (x2 - x1);
                int color = interpolateColors(colorStart, colorEnd, progress);
                context.fill(x, y1, x + 1, y2, color);
            }
        }
    }

    public static int interpolateColors(int color1, int color2, float progress) {
        float inv = 1.0f - progress;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int) (r1 * inv + r2 * progress);
        int g = (int) (g1 * inv + g2 * progress);
        int b = (int) (b1 * inv + b2 * progress);
        int a = (int) (a1 * inv + a2 * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawRect(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public static void drawRoundedRect(DrawContext context, int x1, int y1, int x2, int y2, int color, int radius) {
        drawRect(context, x1 + radius, y1, x2 - radius, y2, color);
        drawRect(context, x1, y1 + radius, x1 + radius, y2 - radius, color);
        drawRect(context, x2 - radius, y1 + radius, x2, y2 - radius, color);
        drawRect(context, x1, y1, x1 + radius, y1 + radius, color);
        drawRect(context, x2 - radius, y1, x2, y1 + radius, color);
        drawRect(context, x1, y2 - radius, x1 + radius, y2, color);
        drawRect(context, x2 - radius, y2 - radius, x2, y2, color);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

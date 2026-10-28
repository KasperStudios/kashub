package kasperstudios.kashub.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import com.kasperstudios.khwrapper.KHClientWrapper;

public class VisualEffectRenderer {
    private static final VisualEffectRenderer INSTANCE = new VisualEffectRenderer();
    private float flashIntensity = 0.0f;
    private int flashTicks = 0;
    private float shakeIntensity = 0.0f;
    private int shakeTicks = 0;

    public static VisualEffectRenderer getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        KHClientWrapper.registerGuiOverlay("visual_effects", this::render);
    }

    public void flash(float intensity, int ticks) {
        this.flashIntensity = intensity;
        this.flashTicks = ticks;
    }

    public void shake(float intensity, int ticks) {
        this.shakeIntensity = intensity;
        this.shakeTicks = ticks;
    }

    private void render(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Handle Flash
        if (flashTicks > 0) {
            float alpha = flashIntensity * ((float) flashTicks / 10.0f);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 
                ((int) (alpha * 255) << 24) | 0xFFFFFF);
            flashTicks--;
        }

        // Handle Shake (Note: This is a simple visual offset, 
        // real camera shake usually involves mixins into GameRenderer)
        if (shakeTicks > 0) {
            float offset = (client.level.random.nextFloat() - 0.5f) * shakeIntensity * 10.0f;
            // graphics.pose().translate(offset, offset, 0); // This only affects GUI
            shakeTicks--;
        }
    }
}

package kasperstudios.kashub.client;

import com.kasperstudios.khwrapper.KHClientWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

public class BlackoutRenderer {
    private static final BlackoutRenderer INSTANCE = new BlackoutRenderer();
    private long blackoutEndTime = 0;
    private String blackoutMessage = "";

    private BlackoutRenderer() {}

    public static BlackoutRenderer getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        KHClientWrapper.registerGuiOverlay("blackout", this::render);
    }

    public void triggerBlackout(int durationSeconds, String message) {
        this.blackoutEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        this.blackoutMessage = message;
    }

    private void render(GuiGraphics graphics, float partialTick) {
        long now = System.currentTimeMillis();
        if (now < blackoutEndTime) {
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();

            // Render Black Background
            graphics.fill(0, 0, width, height, 0xFF000000);

            // Render Message
            if (!blackoutMessage.isEmpty()) {
                graphics.drawCenteredString(Minecraft.getInstance().font, blackoutMessage, width / 2, height / 2, 0xFFFFFFFF);
            }
        }
    }
}

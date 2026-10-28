package kasperstudios.kashub.client;

import com.kasperstudios.khwrapper.KHClientWrapper;
import kasperstudios.kashub.services.AccessPolicy;
import kasperstudios.kashub.services.integration.CCTBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * ComputerInteractionOverlay - HUD element above the hotbar.
 *
 * Shows context-aware prompts:
 * - No access: nothing.
 * - Access but no computers nearby: nothing.
 * - 1 computer nearby: "[K] Connect to <name>"
 * - N computers nearby: "[K] N Computers Nearby — select"
 *
 * The scan is throttled to avoid running every tick.
 */
public class ComputerInteractionOverlay {

    private static List<CCTBridge.DiscoveredComputer> lastScan = List.of();
    private static long lastScanTime = 0L;
    private static final long SCAN_INTERVAL_MS = 500; // scan every 500ms

    public static void initialize() {
        KHClientWrapper.registerGuiOverlay("computer_link", (guiGraphics, partialTicks) -> {
            render(guiGraphics);
        });
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui || mc.player == null) return;

        // Only show if player has access (implant or casual/creative)
        if (!AccessPolicy.getInstance().hasAccess(mc.player)) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        renderScanProgress(graphics, width, height);

        // Throttle scan
        long now = System.currentTimeMillis();
        if (now - lastScanTime > SCAN_INTERVAL_MS) {
            lastScanTime = now;
            lastScan = kasperstudios.kashub.client.CCTClientBridge.getInstance().scan();
        }

        if (lastScan.isEmpty()) return;

        String text;
        if (lastScan.size() == 1) {
            CCTBridge.DiscoveredComputer comp = lastScan.get(0);
            String label = (comp.label != null && !comp.label.isBlank()) ? comp.label : "Computer #" + comp.id;
            text = "§b[K] §fConnect to §b" + label;
        } else {
            text = "§b[K] §f" + lastScan.size() + " §bComputers Nearby";
        }

        int textWidth = mc.font.width(text);
        int x = (width - textWidth) / 2;
        int y = height - 62; // Just above the hotbar

        // Pill-shaped background
        graphics.fill(x - 6, y - 3, x + textWidth + 6, y + 13, 0xAA000000);
        // Left accent bar
        graphics.fill(x - 6, y - 3, x - 4, y + 13, 0xFF007ACC);

        graphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, false);
    }

    private static void renderScanProgress(GuiGraphics graphics, int width, int height) {
        float progress = KashubClient.getScanProgress();
        if (progress <= 0f) return;

        Minecraft mc = Minecraft.getInstance();
        String text = "§bNeural Implant: §fDeep Scanning...";
        int textWidth = mc.font.width(text);
        
        int x = (width - textWidth) / 2;
        int y = height / 2 + 20; // Just below crosshair

        // Background pill
        graphics.fill(x - 10, y - 5, x + textWidth + 10, y + 20, 0xAA000000);
        
        // Text
        graphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, false);
        
        // Progress bar background
        int barY = y + 12;
        graphics.fill(x, barY, x + textWidth, barY + 3, 0x55FFFFFF);
        // Progress bar fill
        graphics.fill(x, barY, x + (int)(textWidth * progress), barY + 3, 0xFF00BFFF);
    }
}

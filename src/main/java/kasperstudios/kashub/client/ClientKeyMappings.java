package kasperstudios.kashub.client;

import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.IKeyBindingHandle;
import kasperstudios.kashub.services.AccessPolicy;
import kasperstudios.kashub.services.integration.CCTBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ClientKeyMappings {
    private static IKeyBindingHandle menuKey;

    // Edge-trigger state — prevents firing every tick while key is held down.
    private static boolean wasKeyDown = false;

    public static void initialize() {
        menuKey = KHWrapper.get().registerKeyBinding(
            "key.kashub.menu",
            GLFW.GLFW_KEY_K,
            "category.kashub.main"
        );

        KHWrapper.get().onClientTick(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                wasKeyDown = false;
                return;
            }

            boolean isDown = KHWrapper.get().isKeyPressed(GLFW.GLFW_KEY_K);

            // Fire only on key-down edge (pressed this tick, was not pressed last tick).
            if (isDown && !wasKeyDown) {
                handleMenuPress(mc);
            }

            wasKeyDown = isDown;
        });
    }

    private static void handleMenuPress(Minecraft mc) {
        if (!AccessPolicy.getInstance().hasAccess(mc.player)) {
            mc.player.displayClientMessage(
                Component.translatable("message.kashub.no_implant").withStyle(net.minecraft.ChatFormatting.RED),
                true
            );
            return;
        }

        var computers = CCTBridge.getInstance().scan();

        if (computers.size() == 1) {
            // Single computer — connect directly in the background
            var comp = computers.get(0);
            com.kasperstudios.khwrapper.KHWrapper.sendPacket(
                new kasperstudios.kashub.network.payload.NetworkingPayloads.ComputerLinkPayload(comp.id, comp.pos, true)
            );
        } else if (computers.size() > 1) {
            // If multiple, just pick the closest one (the first in the list since it's sorted by distance)
            var comp = computers.get(0);
            com.kasperstudios.khwrapper.KHWrapper.sendPacket(
                new kasperstudios.kashub.network.payload.NetworkingPayloads.ComputerLinkPayload(comp.id, comp.pos, true)
            );
        }

        // Always open the main KasHub editor!
        if (kasperstudios.kashub.network.ServerModeManager.getInstance().isEditorAllowed()) {
            mc.setScreen(kasperstudios.khgui.KHGuiScreens.createCodeEditorScreen(new kasperstudios.kashub.gui.KashubEditorContext(null)));
        } else {
            kasperstudios.kashub.util.ScriptLogger.getInstance().error("Editor is disabled by server.");
        }
    }
}


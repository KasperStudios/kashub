package kasperstudios.kashub.mixin;

import kasperstudios.kashub.algorithm.events.EventManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMessageMixin {

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onChatMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        try {
            String message = packet.content().getString();

            String sender = "System";
            if (message.startsWith("<") && message.contains(">")) {
                int endIndex = message.indexOf(">");
                sender = message.substring(1, endIndex);
                message = message.substring(endIndex + 1).trim();
            }

            Map<String, Object> chatData = new HashMap<>();
            chatData.put("message", message);
            chatData.put("sender", sender);
            chatData.put("full_message", packet.content().getString());

            EventManager.getInstance().fireEvent("onChat", chatData);
        } catch (Exception e) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                "Error in ChatMessageMixin: " + e.getMessage());
        }
    }
}

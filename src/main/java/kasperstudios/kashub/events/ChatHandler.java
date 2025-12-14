package kasperstudios.kashub.events;

import kasperstudios.kashub.algorithm.events.EventManager;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class ChatHandler {
    private static ChatHandler instance;

    private ChatHandler() {}

    public static ChatHandler getInstance() {
        if (instance == null) {
            instance = new ChatHandler();
        }
        return instance;
    }

    public void handleChatMessage(GameMessageS2CPacket packet) {
        Text content = packet.content();
        if (content == null) return;

        String message = content.getString();
        
        Map<String, Object> context = new HashMap<>();
        context.put("message", message);
        context.put("raw", content);
        
        EventManager.getInstance().fireEvent("chat", context);
    }

    public void handleChatMessage(String message) {
        Map<String, Object> context = new HashMap<>();
        context.put("message", message);
        
        EventManager.getInstance().fireEvent("chat", context);
    }
}

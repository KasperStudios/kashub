package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Команда для отправки сообщения в чат
 * Синтаксис: chat "сообщение"
 */
public class ChatCommand implements Command {

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public String getDescription() {
        return "Sends message to game chat (visible to all players)";
    }

    @Override
    public String getParameters() {
        return "<message> - message text to send";
    }
    
    @Override
    public String getCategory() {
        return "Output";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Sends a chat message visible to all players on the server.\n\n" +
               "Usage:\n" +
               "  chat <message>\n" +
               "  chat \"Hello World!\"\n\n" +
               "Details:\n" +
               "  - Message is sent as the player (not system)\n" +
               "  - Other players will see your username\n" +
               "  - Supports variables: chat \"Health: $PLAYER_HEALTH\"\n" +
               "  - Can execute commands: chat \"/gamemode creative\"\n" +
               "  - Use 'print' for local-only messages\n\n" +
               "Examples:\n" +
               "  chat \"Hello everyone!\"     // Send greeting\n" +
               "  chat \"My pos: $PLAYER_X $PLAYER_Y $PLAYER_Z\"\n" +
               "  chat \"/tp @p 0 100 0\"      // Execute command\n\n" +
               "Note: For local messages only you see, use 'print' instead.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Использование: chat <сообщение>");
        }

        String message = String.join(" ", args);
        
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            ClientPlayerEntity player = client.player;
            if (player != null) {
                player.networkHandler.sendChatMessage(message);
            }
        });
    }
}

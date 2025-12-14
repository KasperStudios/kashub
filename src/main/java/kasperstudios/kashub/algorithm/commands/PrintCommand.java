package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Command for displaying messages in chat
 * v3.1 - Simplified: outputs to chat only (no console panel)
 */
public class PrintCommand implements Command {
    @Override
    public String getName() {
        return "print";
    }

    @Override
    public String getDescription() {
        return "Outputs message to player chat";
    }

    @Override
    public String getParameters() {
        return "<message> - message text";
    }

    @Override
    public String getCategory() {
        return "Output";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Prints message to local chat (only you see it).\n\n" +
               "Examples:\n" +
               "  print Hello World!\n" +
               "  print \"Quoted text\"\n" +
               "  print Health: $PLAYER_HEALTH\n" +
               "  print Position: $PLAYER_X, $PLAYER_Y, $PLAYER_Z\n\n" +
               "Variables are automatically substituted.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        // Handle empty args gracefully - just print empty line
        String message = args.length > 0 ? String.join(" ", args) : "";
        
        // Safe execution on Minecraft main thread - send to player chat only
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                // Send as local message with purple prefix (not to server chat)
                client.player.sendMessage(Text.literal("§5[KH] §f" + message), false);
            }
        });
    }
}

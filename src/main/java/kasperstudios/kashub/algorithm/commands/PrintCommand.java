package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.api.server.events.ScriptOutputEvent;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

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

        String message = args.length > 0 ? String.join(" ", args) : "";

        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {

                client.player.sendMessage(Text.literal("§5[KH] §f" + message), false);
            }
        });

        KashubAPIServer.broadcast(new ScriptOutputEvent(
            0,
            message,
            "info",
            System.currentTimeMillis()
        ));
    }
}

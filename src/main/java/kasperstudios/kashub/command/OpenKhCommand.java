package kasperstudios.kashub.command;

import com.mojang.brigadier.CommandDispatcher;
import kasperstudios.kashub.gui.editor.ModernEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class OpenKhCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("openkh")
            .executes(context -> {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().setScreen(new ModernEditorScreen());
                });
                context.getSource().sendFeedback(() -> Text.literal("Opening Kashub Editor..."), false);
                return 1;
            }));
    }
    
    public static void openScreen() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().setScreen(new ModernEditorScreen());
        });
    }
}

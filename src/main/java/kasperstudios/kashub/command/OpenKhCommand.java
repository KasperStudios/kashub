package kasperstudios.kashub.command;

import com.mojang.brigadier.CommandDispatcher;
import kasperstudios.kashub.gui.KashubEditorContext;
import kasperstudios.khgui.KHGuiScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class OpenKhCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("openkh")
            .executes(context -> {
                openScreen();
                context.getSource().sendSuccess(() -> Component.literal("Opening Kashub Editor..."), false);
                return 1;
            }));
    }

    public static void openScreen() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(KHGuiScreens.createCodeEditorScreen(new KashubEditorContext(null)));
        });
    }
}

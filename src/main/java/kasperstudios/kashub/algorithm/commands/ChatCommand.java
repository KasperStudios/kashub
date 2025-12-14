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
        return "Отправляет сообщение в чат игры";
    }

    @Override
    public String getParameters() {
        return "<message> - текст сообщения для отправки в чат";
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

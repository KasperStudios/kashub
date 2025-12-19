package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JumpCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getDescription() {
        return "Makes player jump specified number of times";
    }

    @Override
    public String getParameters() {
        return "[count] - number of jumps (default: 1)";
    }
    
    @Override
    public String getCategory() {
        return "Movement";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Makes the player jump one or more times.\n\n" +
               "Usage:\n" +
               "  jump         - Single jump\n" +
               "  jump 3       - Jump 3 times\n" +
               "  jump 10      - Jump 10 times\n\n" +
               "Details:\n" +
               "  - Each jump has ~200ms delay between them\n" +
               "  - Player must be on ground to jump\n" +
               "  - Works in water (swimming up)\n" +
               "  - Async execution waits for all jumps to complete\n\n" +
               "Examples:\n" +
               "  jump              // Single jump\n" +
               "  jump 5            // Jump 5 times\n" +
               "  loop 3 { jump }   // Jump in a loop";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null) {
            int jumps = 1; // По умолчанию 1 прыжок

            if (args.length > 0) {
                try {
                    jumps = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Неверное количество прыжков: " + args[0]);
                }
            }

            // В синхронной версии просто выполняем один прыжок
            player.jump();

            // Примечание: синхронная версия не поддерживает многократные прыжки,
            // это обрабатывается только в асинхронной версии
        }
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null) {
            int jumps = 1; // По умолчанию 1 прыжок

            if (args.length > 0) {
                try {
                    jumps = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Неверное количество прыжков: " + args[0]);
                }
            }

            // Создаем счетчик для отслеживания оставшихся прыжков
            final int[] remainingJumps = { jumps };

            // Планируем выполнение прыжков с интервалом
            Runnable jumpTask = new Runnable() {
                @Override
                public void run() {
                    // Используем runTask для выполнения прыжка в основном потоке игры
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null) {
                            MinecraftClient.getInstance().player.jump();
                        }
                    });

                    remainingJumps[0]--;

                    if (remainingJumps[0] > 0) {
                        // Планируем следующий прыжок
                        scheduler.schedule(this, 200, TimeUnit.MILLISECONDS);
                    } else {
                        // Завершаем CompletableFuture, когда все прыжки выполнены
                        future.complete(null);
                    }
                }
            };

            // Запускаем первый прыжок
            scheduler.schedule(jumpTask, 0, TimeUnit.MILLISECONDS);
        } else {
            future.complete(null); // Завершаем сразу, если игрока нет
        }

        return future;
    }
}
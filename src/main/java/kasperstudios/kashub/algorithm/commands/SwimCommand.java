package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Команда для плавания
 * Синтаксис: swim [duration_ms] или swim up/down/forward
 */
public class SwimCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "swim";
    }

    @Override
    public String getDescription() {
        return "Заставляет игрока плыть";
    }

    @Override
    public String getParameters() {
        return "[duration_ms] или up/down/forward - направление плавания";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) return;

        String direction = args.length > 0 ? args[0].toLowerCase() : "forward";
        
        client.execute(() -> {
            switch (direction) {
                case "up":
                    client.options.jumpKey.setPressed(true);
                    break;
                case "down":
                    client.options.sneakKey.setPressed(true);
                    break;
                case "forward":
                default:
                    client.options.forwardKey.setPressed(true);
                    break;
            }
        });
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            future.complete(null);
            return future;
        }

        String direction = "forward";
        int duration = 1000;

        if (args.length > 0) {
            try {
                duration = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                direction = args[0].toLowerCase();
            }
        }
        if (args.length > 1) {
            try {
                duration = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        final String finalDirection = direction;
        final int finalDuration = duration;

        client.execute(() -> {
            switch (finalDirection) {
                case "up":
                    client.options.jumpKey.setPressed(true);
                    break;
                case "down":
                    client.options.sneakKey.setPressed(true);
                    break;
                case "forward":
                default:
                    client.options.forwardKey.setPressed(true);
                    break;
            }
        });

        scheduler.schedule(() -> {
            client.execute(() -> {
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);
                client.options.forwardKey.setPressed(false);
            });
            future.complete(null);
        }, finalDuration, TimeUnit.MILLISECONDS);

        return future;
    }
}

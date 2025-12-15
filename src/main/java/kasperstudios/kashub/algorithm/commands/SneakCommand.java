package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Команда для приседания
 * Синтаксис: sneak [duration_ms] или sneak toggle
 */
public class SneakCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean isSneaking = false;

    @Override
    public String getName() {
        return "sneak";
    }

    @Override
    public String getDescription() {
        return "Makes player sneak";
    }

    @Override
    public String getParameters() {
        return "[duration_ms] or toggle - duration in ms or toggle";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            isSneaking = !isSneaking;
            client.execute(() -> {
                client.options.sneakKey.setPressed(isSneaking);
            });
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            isSneaking = false;
            client.execute(() -> {
                client.options.sneakKey.setPressed(false);
            });
            return;
        }

        client.execute(() -> {
            client.options.sneakKey.setPressed(true);
        });
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();

        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            isSneaking = !isSneaking;
            client.execute(() -> {
                client.options.sneakKey.setPressed(isSneaking);
            });
            future.complete(null);
            return future;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            isSneaking = false;
            client.execute(() -> {
                client.options.sneakKey.setPressed(false);
            });
            future.complete(null);
            return future;
        }

        int duration = 0;
        if (args.length > 0) {
            try {
                duration = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        final int finalDuration = duration;

        client.execute(() -> {
            client.options.sneakKey.setPressed(true);
        });

        if (finalDuration > 0) {
            scheduler.schedule(() -> {
                client.execute(() -> {
                    client.options.sneakKey.setPressed(false);
                });
                future.complete(null);
            }, finalDuration, TimeUnit.MILLISECONDS);
        } else {
            future.complete(null);
        }

        return future;
    }
}

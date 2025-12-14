package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Команда для бега
 * Синтаксис: sprint [duration_ms] или sprint toggle/stop
 */
public class SprintCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean isSprinting = false;

    @Override
    public String getName() {
        return "sprint";
    }

    @Override
    public String getDescription() {
        return "Заставляет игрока бежать";
    }

    @Override
    public String getParameters() {
        return "[duration_ms] или toggle/stop - длительность в мс или переключение";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            isSprinting = !isSprinting;
            client.execute(() -> {
                client.options.sprintKey.setPressed(isSprinting);
                if (client.player != null) {
                    client.player.setSprinting(isSprinting);
                }
            });
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            isSprinting = false;
            client.execute(() -> {
                client.options.sprintKey.setPressed(false);
                if (client.player != null) {
                    client.player.setSprinting(false);
                }
            });
            return;
        }

        client.execute(() -> {
            client.options.sprintKey.setPressed(true);
            if (client.player != null) {
                client.player.setSprinting(true);
            }
        });
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();

        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            isSprinting = !isSprinting;
            client.execute(() -> {
                client.options.sprintKey.setPressed(isSprinting);
                if (client.player != null) {
                    client.player.setSprinting(isSprinting);
                }
            });
            future.complete(null);
            return future;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            isSprinting = false;
            client.execute(() -> {
                client.options.sprintKey.setPressed(false);
                if (client.player != null) {
                    client.player.setSprinting(false);
                }
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
            client.options.sprintKey.setPressed(true);
            if (client.player != null) {
                client.player.setSprinting(true);
            }
        });

        if (finalDuration > 0) {
            scheduler.schedule(() -> {
                client.execute(() -> {
                    client.options.sprintKey.setPressed(false);
                    if (client.player != null) {
                        client.player.setSprinting(false);
                    }
                });
                future.complete(null);
            }, finalDuration, TimeUnit.MILLISECONDS);
        } else {
            future.complete(null);
        }

        return future;
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Команда для плавного перемещения игрока к указанным координатам
 * Синтаксис: moveTo x y z [speed]
 */
public class MoveToCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean moving = new AtomicBoolean(false);
    private static Vec3d target;
    private static double moveSpeed = 0.2;
    private static CompletableFuture<Void> currentTask = null;

    @Override
    public String getName() {
        return "moveTo";
    }

    @Override
    public String getDescription() {
        return "Плавно перемещает игрока к указанным координатам";
    }

    @Override
    public String getParameters() {
        return "<x> <y> <z> [speed] - координаты и опциональная скорость (0.1-1.0)";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (args.length == 0) {
            System.out.println("Использование: moveTo x y z [speed] или moveTo stop");
            return;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            stopMoving();
            return;
        }

        try {
            if (player == null) return;

            Vec3d currentPos = player.getPos();
            double x = parseCoordinate(args[0], currentPos.x);
            double y = args.length > 1 ? parseCoordinate(args[1], currentPos.y) : currentPos.y;
            double z = args.length > 2 ? parseCoordinate(args[2], currentPos.z) : currentPos.z;
            
            if (args.length > 3) {
                moveSpeed = Math.max(0.1, Math.min(1.0, Double.parseDouble(args[3])));
            }

            if (moving.get()) {
                stopMoving();
            }

            target = new Vec3d(x, y, z);
            moving.set(true);

        } catch (NumberFormatException e) {
            System.out.println("Неверный формат координат");
        }
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        MinecraftClient.getInstance().execute(() -> {
            try {
                execute(args);

                if (args.length > 0 && !args[0].equalsIgnoreCase("stop") && moving.get()) {
                    currentTask = startMoving(target);
                    currentTask.thenRun(() -> future.complete(null));
                } else {
                    future.complete(null);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private double parseCoordinate(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    private CompletableFuture<Void> startMoving(Vec3d targetPos) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable moveTask = new Runnable() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    ClientPlayerEntity player = client.player;

                    if (player == null || !moving.get()) {
                        stopMoving();
                        future.complete(null);
                        return;
                    }

                    Vec3d pos = player.getPos();
                    Vec3d direction = target.subtract(pos);
                    double distance = direction.length();

                    if (distance < 0.5) {
                        stopMoving();
                        future.complete(null);
                        return;
                    }

                    Vec3d normalizedDir = direction.normalize();
                    double step = Math.min(moveSpeed, distance);
                    
                    Vec3d newPos = pos.add(normalizedDir.multiply(step));
                    player.setPos(newPos.x, newPos.y, newPos.z);

                    if (moving.get()) {
                        scheduler.schedule(this, 50, TimeUnit.MILLISECONDS);
                    } else {
                        future.complete(null);
                    }
                });
            }
        };

        scheduler.schedule(moveTask, 0, TimeUnit.MILLISECONDS);
        return future;
    }

    public static void stopMoving() {
        moving.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
    }
}

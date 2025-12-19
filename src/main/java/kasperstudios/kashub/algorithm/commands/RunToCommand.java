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

public class RunToCommand implements Command {
  private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private static final AtomicBoolean running = new AtomicBoolean(false);
  private static Vec3d target;
  private static CompletableFuture<Void> currentTask = null;

  @Override
  public String getName() {
    return "run";
  }

  @Override
  public String getDescription() {
    return "Makes player run to specified coordinates";
  }

  @Override
  public String getParameters() {
    return "<x> <y> <z> - destination coordinates";
  }

  @Override
  public String getCategory() {
    return "Movement";
  }

  @Override
  public String getDetailedHelp() {
    return "Makes player run to specified coordinates.\n\n" +
           "Usage:\n" +
           "  run <x> <y> <z>     - Run to absolute coords\n" +
           "  run ~<dx> ~<dy> ~<dz> - Run to relative coords\n" +
           "  run stop            - Stop running\n\n" +
           "Coordinate Types:\n" +
           "  100 64 200   - Absolute coordinates\n" +
           "  ~10 ~ ~-5    - Relative to player\n" +
           "  ~            - Current coordinate\n\n" +
           "Examples:\n" +
           "  run 100 64 200\n" +
           "  run ~10 ~ ~10\n" +
           "  run ~ ~5 ~\n" +
           "  run stop\n\n" +
           "Behavior:\n" +
           "  - Continuously moves toward target\n" +
           "  - Stops when within 1 block of target\n" +
           "  - Does not pathfind around obstacles\n" +
           "  - Y coordinate affects vertical velocity\n\n" +
           "Notes:\n" +
           "  - Use 'moveTo' for single-step movement\n" +
           "  - Combine with 'jump' for obstacles\n" +
           "  - Async version waits until arrival\n" +
           "  - Call 'run stop' to cancel";
  }

  @Override
  public void execute(String[] args) throws Exception {
    MinecraftClient client = MinecraftClient.getInstance();
    ClientPlayerEntity player = client.player;

    if (args.length == 0) {
      System.out.println("Использование: run x y z или run ~x ~y ~z или run stop");
      return;
    }

    if (args[0].equalsIgnoreCase("stop")) {
      stopRunning();
      return;
    }

    try {
      if (player == null) {
        return;
      }

      Vec3d currentPos = player.getPos();
      double x = parseCoordinate(args[0], currentPos.x);
      double y = args.length > 1 ? parseCoordinate(args[1], currentPos.y) : currentPos.y;
      double z = args.length > 2 ? parseCoordinate(args[2], currentPos.z) : currentPos.z;

      // В синхронном режиме просто устанавливаем направление и velocity один раз
      if (running.get()) {
        stopRunning();
      }

      target = new Vec3d(x, y, z);
      Vec3d direction = target.subtract(currentPos).normalize();
      player.setVelocity(direction.x, 0, direction.z);
      running.set(true);

      // Примечание: синхронная версия не поддерживает непрерывное движение к цели,
      // это обрабатывается только в асинхронной версии

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

        // Если не команда stop, запускаем движение к цели
        if (args.length > 0 && !args[0].equalsIgnoreCase("stop") && running.get()) {
          // Когда предыдущая задача завершится, завершаем и наш future
          currentTask = startRunning(target);
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
      if (arg.length() == 1)
        return current; // Просто "~" означает текущую координату
      return current + Double.parseDouble(arg.substring(1)); // "~10" означает текущая + 10
    }
    return Double.parseDouble(arg); // Обычная абсолютная координата
  }

  private CompletableFuture<Void> startRunning(Vec3d targetPos) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    Runnable runTask = new Runnable() {
      @Override
      public void run() {
        MinecraftClient.getInstance().execute(() -> {
          MinecraftClient client = MinecraftClient.getInstance();
          ClientPlayerEntity player = client.player;

          if (player == null || !running.get()) {
            stopRunning();
            future.complete(null);
            return;
          }

          Vec3d pos = player.getPos();
          Vec3d direction = target.subtract(pos).normalize();

          // Если мы близко к цели, останавливаемся
          if (pos.squaredDistanceTo(target) < 1) {
            stopRunning();
            future.complete(null);
            return;
          }

          // Устанавливаем направление движения
          player.setVelocity(direction.x, 0, direction.z);

          // Планируем следующую проверку, если всё ещё бежим
          if (running.get()) {
            scheduler.schedule(this, 50, TimeUnit.MILLISECONDS);
          } else {
            future.complete(null);
          }
        });
      }
    };

    // Запускаем первую итерацию
    scheduler.schedule(runTask, 0, TimeUnit.MILLISECONDS);

    return future;
  }

  public static void stopRunning() {
    if (running.compareAndSet(true, false)) {
      // Останавливаем движение игрока
      MinecraftClient.getInstance().execute(() -> {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
          player.setVelocity(0, player.getVelocity().y, 0);
        }
      });

      if (currentTask != null && !currentTask.isDone()) {
        currentTask.complete(null);
      }
    }
  }
}

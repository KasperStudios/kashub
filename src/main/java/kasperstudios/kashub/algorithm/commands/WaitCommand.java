package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WaitCommand implements Command {
  private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  @Override
  public String getName() {
    return "wait";
  }

  @Override
  public String getDescription() {
    return "Waits for specified number of milliseconds";
  }

  @Override
  public String getParameters() {
    return "<time_in_ms> - wait time in milliseconds";
  }

  @Override
  public String getCategory() {
    return "Timing";
  }
  
  @Override
  public String getDetailedHelp() {
    return "Pauses script execution for specified time.\n\n" +
           "Examples:\n" +
           "  wait 100   - 0.1 seconds\n" +
           "  wait 500   - 0.5 seconds\n" +
           "  wait 1000  - 1 second\n" +
           "  wait 5000  - 5 seconds\n\n" +
           "Tips:\n" +
           "  Always add wait in loops to prevent lag\n" +
           "  Minimum recommended: 50ms";
  }

  @Override
  public void execute(String[] args) throws Exception {
    if (args.length > 0) {
      try {
        int waitTime = Integer.parseInt(args[0]);
        System.out.println("Ожидание " + waitTime + " мс...");

        // Синхронное ожидание (блокирует текущий поток)
        Thread.sleep(waitTime);
        System.out.println("Ожидание завершено");

      } catch (NumberFormatException e) {
        System.out.println("Неверное время ожидания: " + args[0]);
      }
    } else {
      System.out.println("Использование: wait <время в мс>");
    }
  }

  @Override
  public CompletableFuture<Void> executeAsync(String[] args) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (args.length > 0) {
      try {
        int waitTime = Integer.parseInt(args[0]);
        System.out.println("Ожидание " + waitTime + " мс...");

        // Планируем задачу, которая завершит future через указанное время
        scheduler.schedule(() -> {
          System.out.println("Ожидание завершено");
          future.complete(null);
        }, waitTime, TimeUnit.MILLISECONDS);

      } catch (NumberFormatException e) {
        System.out.println("Неверное время ожидания: " + args[0]);
        future.complete(null); // Завершаем сразу в случае ошибки
      }
    } else {
      System.out.println("Использование: wait <время в мс>");
      future.complete(null); // Завершаем сразу, если нет аргументов
    }

    return future;
  }
}
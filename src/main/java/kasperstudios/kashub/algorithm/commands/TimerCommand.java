package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TimerCommand implements Command {

    private static final Map<String, Timer> timers = new HashMap<>();

    @Override
    public String getName() {
        return "timer";
    }

    @Override
    public String getDescription() {
        return "Set timers and delayed execution";
    }

    @Override
    public String getParameters() {
        return "set|check|cancel|list|clear <args>";
    }

    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public String getDetailedHelp() {
        return "Set timers and delayed execution.\n\n" +
               "Usage:\n" +
               "  timer set <name> <seconds> [message]\n" +
               "  timer check <name>\n" +
               "  timer cancel <name>\n" +
               "  timer list\n" +
               "  timer clear\n\n" +
               "Actions:\n" +
               "  set    - Create new timer\n" +
               "  check  - Check remaining time\n" +
               "  cancel - Cancel timer\n" +
               "  list   - List all active timers\n" +
               "  clear  - Clear all timers\n\n" +
               "Examples:\n" +
               "  timer set mining 300\n" +
               "  timer set farming 600 Time to harvest!\n" +
               "  timer check mining\n" +
               "  timer cancel mining\n" +
               "  timer list\n\n" +
               "Variables set:\n" +
               "  $timer_<name>_remaining - Seconds remaining\n" +
               "  $timer_<name>_expired   - Has timer expired\n" +
               "  $timer_count            - Number of active timers\n\n" +
               "Notes:\n" +
               "  - Timers persist until cancelled or expired\n" +
               "  - Optional message shown when timer expires\n" +
               "  - Useful for farming, crafting, cooldowns";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "set":
                handleSet(args, interpreter);
                break;
            case "check":
                handleCheck(args, interpreter);
                break;
            case "cancel":
                handleCancel(args, interpreter);
                break;
            case "list":
                handleList(interpreter);
                break;
            case "clear":
                handleClear(interpreter);
                break;
            default:
                printHelp();
        }
    }

    private void handleSet(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 3) {
            ScriptLogger.getInstance().error("Usage: timer set <name> <seconds> [message]");
            return;
        }

        String name = args[1];
        int seconds;

        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ScriptLogger.getInstance().error("Invalid seconds: " + args[2]);
            return;
        }

        String message = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)) : null;

        Timer timer = new Timer(name, seconds, message);
        timers.put(name, timer);

        interpreter.setVariable("timer_" + name + "_remaining", String.valueOf(seconds));
        interpreter.setVariable("timer_" + name + "_expired", "false");

        ScriptLogger.getInstance().info("Timer set: " + name + " for " + seconds + " seconds");

        CompletableFuture.delayedExecutor(seconds, java.util.concurrent.TimeUnit.SECONDS)
            .execute(() -> {
                Timer t = timers.get(name);
                if (t != null && !t.cancelled) {
                    t.expired = true;
                    interpreter.setVariable("timer_" + name + "_expired", "true");
                    interpreter.setVariable("timer_" + name + "_remaining", "0");

                    String msg = t.message != null ? t.message : "Timer '" + name + "' expired!";
                    ScriptLogger.getInstance().info("[TIMER] " + msg);

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§e[TIMER] §r" + msg), false);
                            }
                        });
                    }
                }
            });
    }

    private void handleCheck(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: timer check <name>");
            return;
        }

        String name = args[1];
        Timer timer = timers.get(name);

        if (timer == null) {
            ScriptLogger.getInstance().warn("Timer not found: " + name);
            interpreter.setVariable("timer_" + name + "_remaining", "-1");
            return;
        }

        if (timer.expired) {
            ScriptLogger.getInstance().info("Timer '" + name + "' has expired");
            interpreter.setVariable("timer_" + name + "_remaining", "0");
            interpreter.setVariable("timer_" + name + "_expired", "true");
        } else if (timer.cancelled) {
            ScriptLogger.getInstance().info("Timer '" + name + "' was cancelled");
            interpreter.setVariable("timer_" + name + "_remaining", "-1");
        } else {
            long elapsed = (System.currentTimeMillis() - timer.startTime) / 1000;
            long remaining = timer.duration - elapsed;

            if (remaining < 0) remaining = 0;

            ScriptLogger.getInstance().info("Timer '" + name + "': " + remaining + " seconds remaining");
            interpreter.setVariable("timer_" + name + "_remaining", String.valueOf(remaining));
        }
    }

    private void handleCancel(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: timer cancel <name>");
            return;
        }

        String name = args[1];
        Timer timer = timers.get(name);

        if (timer == null) {
            ScriptLogger.getInstance().warn("Timer not found: " + name);
            return;
        }

        timer.cancelled = true;
        timers.remove(name);

        interpreter.setVariable("timer_" + name + "_remaining", "-1");
        ScriptLogger.getInstance().info("Timer cancelled: " + name);
    }

    private void handleList(ScriptInterpreter interpreter) {
        if (timers.isEmpty()) {
            ScriptLogger.getInstance().info("No active timers");
            interpreter.setVariable("timer_count", "0");
            return;
        }

        ScriptLogger.getInstance().info("=== Active Timers ===");
        int activeCount = 0;

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            if (timer.cancelled || timer.expired) continue;

            long elapsed = (System.currentTimeMillis() - timer.startTime) / 1000;
            long remaining = timer.duration - elapsed;

            if (remaining < 0) remaining = 0;

            ScriptLogger.getInstance().info("  " + entry.getKey() + ": " + remaining + "s remaining");
            activeCount++;
        }

        interpreter.setVariable("timer_count", String.valueOf(activeCount));
    }

    private void handleClear(ScriptInterpreter interpreter) {
        int count = timers.size();
        timers.clear();

        interpreter.setVariable("timer_count", "0");
        ScriptLogger.getInstance().info("Cleared " + count + " timers");
    }

    public static void tick() {
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Timer timer = entry.getValue();
            if (timer.cancelled || timer.expired) continue;

            long elapsed = (System.currentTimeMillis() - timer.startTime) / 1000;
            long remaining = timer.duration - elapsed;

            if (remaining < 0) remaining = 0;

            interpreter.setVariable("timer_" + entry.getKey() + "_remaining", String.valueOf(remaining));
        }
    }

    private void printHelp() {
        ScriptLogger.getInstance().info("Timer Command:");
        ScriptLogger.getInstance().info("  timer set <name> <seconds> [message]");
        ScriptLogger.getInstance().info("    - Create new timer");
        ScriptLogger.getInstance().info("  timer check <name>");
        ScriptLogger.getInstance().info("    - Check remaining time");
        ScriptLogger.getInstance().info("  timer cancel <name>");
        ScriptLogger.getInstance().info("    - Cancel timer");
        ScriptLogger.getInstance().info("  timer list");
        ScriptLogger.getInstance().info("    - List active timers");
        ScriptLogger.getInstance().info("  timer clear");
        ScriptLogger.getInstance().info("    - Clear all timers");
    }

    public static void clearAll() {
        timers.clear();
    }

    private static class Timer {
        String name;
        int duration;
        String message;
        long startTime;
        boolean expired;
        boolean cancelled;

        Timer(String name, int duration, String message) {
            this.name = name;
            this.duration = duration;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.expired = false;
            this.cancelled = false;
        }
    }
}

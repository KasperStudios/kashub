package kasperstudios.kashub.core;

import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TimerManager {
    private static TimerManager instance;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    private TimerManager() {
    }

    public static synchronized TimerManager getInstance() {
        if (instance == null) {
            instance = new TimerManager();
        }
        return instance;
    }

    public void setTimer(String name, int seconds, String message) {
        Timer timer = new Timer(name, seconds, message);
        timers.put(name, timer);

        ScriptLogger.getInstance().info("Timer set: " + name + " for " + seconds + " seconds");

        CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS)
                .execute(() -> {
                    Timer t = timers.get(name);
                    // Ensure it's the same timer instance (not replaced)
                    if (t == timer && !t.cancelled) {
                        t.expired = true;

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

    public Timer getTimer(String name) {
        return timers.get(name);
    }

    public void cancelTimer(String name) {
        Timer timer = timers.get(name);
        if (timer != null) {
            timer.cancelled = true;
            timers.remove(name);
            ScriptLogger.getInstance().info("Timer cancelled: " + name);
        }
    }

    public Map<String, Timer> getTimers() {
        return Collections.unmodifiableMap(timers);
    }

    public void clearAll() {
        timers.clear();
    }

    public static class Timer {
        public final String name;
        public final int duration;
        public final String message;
        public final long startTime;
        public volatile boolean expired;
        public volatile boolean cancelled;

        public Timer(String name, int duration, String message) {
            this.name = name;
            this.duration = duration;
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.expired = false;
            this.cancelled = false;
        }

        public long getRemainingSeconds() {
            if (expired || cancelled)
                return 0;
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long remaining = duration - elapsed;
            return Math.max(0, remaining);
        }
    }
}

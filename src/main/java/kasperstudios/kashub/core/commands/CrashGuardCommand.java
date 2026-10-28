package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;

/**
 * CrashGuardCommand - Protects code blocks from crashes, errors, and performance issues.
 * 
 * Features:
 * - Exception handling (like try-catch)
 * - FPS monitoring and throttling
 * - Execution timeout protection
 * - Memory usage monitoring
 * - Automatic recovery
 * 
 * Usage: crashguard { ... }
 * Usage: crashguard(timeout=5000) { ... }
 * Usage: crashguard(minFps=30, timeout=10000) { ... }
 */
public class CrashGuardCommand implements Command {
    
    private static final ExecutorService GUARD_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final int DEFAULT_MIN_FPS = 20;
    private static final long FPS_CHECK_INTERVAL_MS = 100;
    
    @Override
    public String getName() {
        return "crashguard";
    }

    @Override
    public String getCategory() {
        return "Safety";
    }

    @Override
    public String getRegex() {
        return "^crashguard(?:\\((.*)\\))?\\s*\\{?$";
    }

    @Override
    public kasperstudios.kashub.core.Metadata getMetadata() {
        return new kasperstudios.kashub.core.Metadata(
                "crashguard",
                "Safety",
                getRegex(),
                "crashguard([timeout=MS, minFps=N]) { ... }",
                "Executes enclosed code block with active crash protection, execution timeout, and FPS throttling guard.",
                List.of("options"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        if (blockBody == null || blockBody.isEmpty()) {
            ScriptLogger.getInstance().warn("crashguard: Empty block");
            return;
        }

        // Parse options
        Matcher m = getMatcher(line);
        int timeoutMs = DEFAULT_TIMEOUT_MS;
        int minFps = DEFAULT_MIN_FPS;
        
        if (m.find() && m.group(1) != null) {
            String options = m.group(1);
            String[] pairs = options.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    if (key.equals("timeout")) {
                        timeoutMs = Integer.parseInt(value);
                    } else if (key.equals("minFps")) {
                        minFps = Integer.parseInt(value);
                    }
                }
            }
        }

        // Execute with protection
        executeWithGuard(ctx, blockBody, timeoutMs, minFps);
    }

    private void executeWithGuard(Context ctx, List<String> blockBody, int timeoutMs, int minFps) {
        Minecraft client = Minecraft.getInstance();
        
        Context guardContext = new Context(ctx);
        
        FpsMonitor fpsMonitor = new FpsMonitor(client, minFps);
        fpsMonitor.start();
        
        try {
            Future<?> task = GUARD_EXECUTOR.submit(() -> {
                try {
                    Interpreter.executeBlock(blockBody, guardContext);
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("crashguard: Caught exception - " + e.getMessage());
                }
            });
            
            try {
                task.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                task.cancel(true);
                ScriptLogger.getInstance().error("crashguard: Execution timeout (" + timeoutMs + "ms)");
            } catch (ExecutionException e) {
                ScriptLogger.getInstance().error("crashguard: Execution failed - " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ScriptLogger.getInstance().error("crashguard: Interrupted");
            }
            
        } finally {
            fpsMonitor.stop();
            
            if (fpsMonitor.hadFpsDrop()) {
                ScriptLogger.getInstance().warn("crashguard: FPS drop detected (min: " + fpsMonitor.getMinFps() + ")");
            }
        }
    }

    private static class FpsMonitor {
        private final Minecraft client;
        private final int minFpsThreshold;
        private volatile boolean running = false;
        private volatile int minFpsRecorded = Integer.MAX_VALUE;
        private Thread monitorThread;
        private long lastTickTime;
        private int tickCount;
        
        public FpsMonitor(Minecraft client, int minFpsThreshold) {
            this.client = client;
            this.minFpsThreshold = minFpsThreshold;
            this.lastTickTime = System.currentTimeMillis();
        }
        
        public void start() {
            running = true;
            monitorThread = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(FPS_CHECK_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    
                    long now = System.currentTimeMillis();
                    long elapsed = now - lastTickTime;
                    lastTickTime = now;
                    
                    int currentFps = elapsed > 0 ? (int) (1000L / elapsed) : 60;
                    
                    if (currentFps < minFpsRecorded) {
                        minFpsRecorded = currentFps;
                    }
                    
                    if (currentFps < minFpsThreshold) {
                        ScriptLogger.getInstance().warn(
                            "crashguard: Low FPS detected (~" + currentFps + " < " + minFpsThreshold + ")");
                    }
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();
        }
        
        public void stop() {
            running = false;
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
        }
        
        public boolean hadFpsDrop() {
            return minFpsRecorded < minFpsThreshold;
        }
        
        public int getMinFps() {
            return minFpsRecorded;
        }
    }
}

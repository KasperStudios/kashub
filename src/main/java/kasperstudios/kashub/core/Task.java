package kasperstudios.kashub.core;

import kasperstudios.kashub.api.NavigationAPI;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Task {
    private final int id;
    private final String name;
    private final String code;
    private final Set<String> tags;
    private final long startTime;
    private final Type type;

    private State state;
    private long lastTickTime;
    private String lastError;
    private int priority;
    private int currentLine;
    private int executedCommands;

    private final Context context;
    private Thread thread;
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    public Task(int id, String name, String code, Set<String> tags, Type type) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = startTime;
        this.state = State.RUNNING;
        this.priority = 0;
        this.currentLine = 0;
        this.executedCommands = 0;

        this.context = new Context();
        this.context.setVariable("SCRIPT_NAME", Value.of(name));
        this.context.setVariable("SCRIPT_PATH", Value.of(name + ".kashub"));
    }

    public void setVariable(String name, String value) {
        context.setVariable(name, Value.of(value));
    }

    public void tick() {
        if (state == State.RUNNING) {
            lastTickTime = System.currentTimeMillis();

            if (thread != null && !thread.isAlive()) {
                if (state != State.ERROR && state != State.STOPPED) {
                    state = State.STOPPED;
                    ScriptLogger.getInstance().info("Task " + id + " (" + name + ") finished execution");
                }
            }
        }
    }

    public void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }

        isStopped.set(false);
        state = State.RUNNING;
        lastError = null;
        executedCommands = 0;

        thread = new Thread(this::execute, "Kashub-Task-" + id);
        thread.setDaemon(true);
        thread.start();

        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") started in background thread");
    }

    private void execute() {
        try {
            List<String> lines = Arrays.asList(code.split("\\r?\\n"));
            Interpreter.executeBlock(lines, context);

            if (!isStopped.get()) {
                state = State.STOPPED;
            }
        } catch (Exception e) {
            state = State.ERROR;
            lastError = e.getMessage();
            ScriptLogger.getInstance().error("Task " + id + " (" + name + ") error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            ScriptLogger.getInstance().info("Task " + id + " (" + name + ") paused");
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            ScriptLogger.getInstance().info("Task " + id + " (" + name + ") resumed");
        }
    }

    public void stop() {
        isStopped.set(true);
        state = State.STOPPED;

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        context.stop();

        // Clean up navigation and player input
        NavigationAPI.getInstance().stopMovement();
        
        // Clear player input and keys on main thread
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                ClientPlayerEntity player = client.player;
                if (player != null && player.input != null) {
                    player.input.movementForward = 0;
                    player.input.movementSideways = 0;
                    player.input.pressingForward = false;
                    player.input.pressingBack = false;
                    player.input.pressingLeft = false;
                    player.input.pressingRight = false;
                    player.input.jumping = false;
                    player.input.sneaking = false;
                }
                
                // Also clear key bindings
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);
            });
        }

        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") stopped");
    }

    public void restart() {
        stop();
        start();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public State getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastTickTime() {
        return lastTickTime;
    }

    public String getLastError() {
        return lastError;
    }

    public int getPriority() {
        return priority;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getExecutedCommands() {
        return executedCommands;
    }

    public boolean isProcessing() {
        return thread != null && thread.isAlive();
    }

    public Type getType() {
        return type;
    }

    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getUptimeFormatted() {
        long uptime = getUptime();
        long seconds = (uptime / 1000) % 60;
        long minutes = (uptime / (1000 * 60)) % 60;
        long hours = uptime / (1000 * 60 * 60);

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public Map<String, String> getVariables() {
        Map<String, String> vars = new HashMap<>();
        context.getLocalVariables().forEach((k, v) -> vars.put(k, v.asString()));
        return vars;
    }

    public Context getContext() {
        return context;
    }
}

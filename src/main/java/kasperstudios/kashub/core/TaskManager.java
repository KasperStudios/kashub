package kasperstudios.kashub.core;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.ScriptManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaskManager {
    private static TaskManager instance;

    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private boolean enabled = true;

    private TaskManager() {
    }

    public static TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }

    public Task startScript(String name, String code) {
        return startScript(name, code, null, Type.USER);
    }

    public Task startScript(String name, String code, Set<String> tags) {
        return startScript(name, code, tags, Type.USER);
    }

    public Task startScript(String name, String code, Set<String> tags, Type type) {
        if (!enabled) {
            ScriptLogger.getInstance().warn("Script execution is disabled");
            return null;
        }

        int id = nextId.getAndIncrement();
        Task task = new Task(id, name, code, tags, type);
        tasks.put(id, task);

        ScriptLogger.getInstance().info("Started task " + id + ": " + name);

        if (KashubConfig.getInstance().hotReload && type == Type.USER) {
            ScriptFileWatcher.getInstance().registerScript(name, id);
        }

        try {
            task.start();
        } catch (Exception e) {
            task.stop();
            ScriptLogger.getInstance().error("Failed to start script " + name + ": " + e.getMessage());
        }

        return task;
    }

    public Task startScriptFromFile(String name) {
        try {
            String code = ScriptManager.loadScript(name);
            return startScript(name, code, null, Type.USER);
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Failed to load script " + name + ": " + e.getMessage());
            return null;
        }
    }

    public Task startSystemScript(String name, String code) {
        Set<String> tags = new HashSet<>();
        tags.add("system");
        return startScript(name, code, tags, Type.SYSTEM);
    }

    public void tick() {
        if (!enabled)
            return;

        KashubConfig config = KashubConfig.getInstance();
        int processed = 0;
        int runningCount = 0;

        for (Task task : tasks.values()) {
            if (processed >= config.maxScriptsPerTick)
                break;

            if (task.getState() == State.RUNNING) {
                runningCount++;
                try {
                    task.tick();
                    processed++;
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Task " + task.getId() + " tick error: " + e.getMessage());
                }
            }

            if (task.getState() == State.STOPPED &&
                    System.currentTimeMillis() - task.getLastTickTime() > 300000) {

                if (KashubConfig.getInstance().hotReload && task.getType() == Type.USER) {
                    ScriptFileWatcher.getInstance().unregisterScript(task.getName());
                }
                tasks.remove(task.getId());
            }
        }

        if (runningCount > 0 && System.currentTimeMillis() % 1000 < 50) {
            ScriptLogger.getInstance().debug(
                    "TaskManager: " + runningCount + " running tasks, processed " + processed + " this tick");
        }
    }

    public void pause(int id) {
        Task task = tasks.get(id);
        if (task != null)
            task.pause();
    }

    public void resume(int id) {
        Task task = tasks.get(id);
        if (task != null)
            task.resume();
    }

    public void stop(int id) {
        Task task = tasks.get(id);
        if (task != null) {

            if (KashubConfig.getInstance().hotReload && task.getType() == Type.USER) {
                ScriptFileWatcher.getInstance().unregisterScript(task.getName());
            }
            task.stop();
        }
    }

    public void restart(int id) {
        Task task = tasks.get(id);
        if (task != null)
            task.restart();
    }

    public void stopAll() {
        for (Task task : tasks.values()) {
            task.stop();
        }
        ScriptLogger.getInstance().info("All tasks stopped");
    }

    public void pauseAll() {
        for (Task task : tasks.values()) {
            if (task.getState() == State.RUNNING) {
                task.pause();
            }
        }
        ScriptLogger.getInstance().info("All tasks paused");
    }

    public void resumeAll() {
        for (Task task : tasks.values()) {
            if (task.getState() == State.PAUSED) {
                task.resume();
            }
        }
        ScriptLogger.getInstance().info("All tasks resumed");
    }

    public void stopByTag(String tag) {
        for (Task task : tasks.values()) {
            if (task.hasTag(tag)) {
                task.stop();
            }
        }
        ScriptLogger.getInstance().info("Stopped all tasks with tag: " + tag);
    }

    public void pauseByTag(String tag) {
        for (Task task : tasks.values()) {
            if (task.hasTag(tag) && task.getState() == State.RUNNING) {
                task.pause();
            }
        }
    }

    public Task getTask(int id) {
        return tasks.get(id);
    }

    public Collection<Task> getTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public List<Task> getRunningTasks() {
        return tasks.values().stream()
                .filter(t -> t.getState() == State.RUNNING)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByTag(String tag) {
        return tasks.values().stream()
                .filter(t -> t.hasTag(tag))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksByState(State state) {
        return tasks.values().stream()
                .filter(t -> t.getState() == state)
                .collect(Collectors.toList());
    }

    public Collection<Task> getAllTasks() {
        return getTasks();
    }

    public int getActiveCount() {
        return (int) tasks.values().stream()
                .filter(t -> t.getState() == State.RUNNING || t.getState() == State.PAUSED)
                .count();
    }

    public int getTotalCount() {
        return tasks.size();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stopAll();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void clear() {
        stopAll();
        tasks.clear();
        nextId.set(1);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", tasks.size());
        stats.put("running", getTasksByState(State.RUNNING).size());
        stats.put("paused", getTasksByState(State.PAUSED).size());
        stats.put("stopped", getTasksByState(State.STOPPED).size());
        stats.put("error", getTasksByState(State.ERROR).size());
        stats.put("enabled", enabled);
        return stats;
    }
}

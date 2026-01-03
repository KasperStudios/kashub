package kasperstudios.kashub.services.runtime;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.ScriptManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ScriptTaskManager {
    private static ScriptTaskManager instance;

    private final Map<Integer, ScriptTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private boolean enabled = true;

    private ScriptTaskManager() {
    }

    public static ScriptTaskManager getInstance() {
        if (instance == null) {
            instance = new ScriptTaskManager();
        }
        return instance;
    }

    public ScriptTask startScript(String name, String code) {
        return startScript(name, code, null, ScriptType.USER);
    }

    public ScriptTask startScript(String name, String code, Set<String> tags) {
        return startScript(name, code, tags, ScriptType.USER);
    }

    public ScriptTask startScript(String name, String code, Set<String> tags, ScriptType scriptType) {
        if (!enabled) {
            ScriptLogger.getInstance().warn("Script execution is disabled");
            return null;
        }

        int id = nextId.getAndIncrement();
        ScriptTask task = new ScriptTask(id, name, code, tags, scriptType);
        tasks.put(id, task);

        ScriptLogger.getInstance().info("Started task " + id + ": " + name);

        if (KashubConfig.getInstance().hotReload && scriptType == ScriptType.USER) {
            ScriptFileWatcher.getInstance().registerScript(name, id);
        }

        try {
            task.parseAndQueue();
        } catch (Exception e) {
            task.stop();
            ScriptLogger.getInstance().error("Failed to start script " + name + ": " + e.getMessage());
        }

        return task;
    }

    public ScriptTask startScriptFromFile(String name) {
        try {
            String code = ScriptManager.loadScript(name);
            return startScript(name, code, null, ScriptType.USER);
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Failed to load script " + name + ": " + e.getMessage());
            return null;
        }
    }

    public ScriptTask startSystemScript(String name, String code) {
        Set<String> tags = new HashSet<>();
        tags.add("system");
        return startScript(name, code, tags, ScriptType.SYSTEM);
    }

    public void tick() {
        if (!enabled)
            return;

        KashubConfig config = KashubConfig.getInstance();
        int processed = 0;
        int runningCount = 0;

        for (ScriptTask task : tasks.values()) {
            if (processed >= config.maxScriptsPerTick)
                break;

            if (task.getState() == ScriptState.RUNNING) {
                runningCount++;
                try {
                    task.tick();
                    processed++;
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Task " + task.getId() + " tick error: " + e.getMessage());
                }
            }

            if (task.getState() == ScriptState.STOPPED &&
                    System.currentTimeMillis() - task.getLastTickTime() > 300000) {

                if (KashubConfig.getInstance().hotReload && task.getScriptType() == ScriptType.USER) {
                    ScriptFileWatcher.getInstance().unregisterScript(task.getName());
                }
                tasks.remove(task.getId());
            }
        }

        if (runningCount > 0 && System.currentTimeMillis() % 1000 < 50) {
            ScriptLogger.getInstance().debug(
                    "ScriptTaskManager: " + runningCount + " running tasks, processed " + processed + " this tick");
        }
    }

    public void pause(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null)
            task.pause();
    }

    public void resume(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null)
            task.resume();
    }

    public void stop(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null) {

            if (KashubConfig.getInstance().hotReload && task.getScriptType() == ScriptType.USER) {
                ScriptFileWatcher.getInstance().unregisterScript(task.getName());
            }
            task.stop();
        }
    }

    public void restart(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null)
            task.restart();
    }

    public void stopAll() {
        for (ScriptTask task : tasks.values()) {
            task.stop();
        }
        ScriptInterpreter.getInstance().stopProcessing();
        ScriptLogger.getInstance().info("All tasks stopped");
    }

    public void pauseAll() {
        for (ScriptTask task : tasks.values()) {
            if (task.getState() == ScriptState.RUNNING) {
                task.pause();
            }
        }
        ScriptLogger.getInstance().info("All tasks paused");
    }

    public void resumeAll() {
        for (ScriptTask task : tasks.values()) {
            if (task.getState() == ScriptState.PAUSED) {
                task.resume();
            }
        }
        ScriptLogger.getInstance().info("All tasks resumed");
    }

    public void stopByTag(String tag) {
        for (ScriptTask task : tasks.values()) {
            if (task.hasTag(tag)) {
                task.stop();
            }
        }
        ScriptLogger.getInstance().info("Stopped all tasks with tag: " + tag);
    }

    public void pauseByTag(String tag) {
        for (ScriptTask task : tasks.values()) {
            if (task.hasTag(tag) && task.getState() == ScriptState.RUNNING) {
                task.pause();
            }
        }
    }

    public ScriptTask getTask(int id) {
        return tasks.get(id);
    }

    public Collection<ScriptTask> getTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public List<ScriptTask> getRunningTasks() {
        return tasks.values().stream()
                .filter(t -> t.getState() == ScriptState.RUNNING)
                .collect(Collectors.toList());
    }

    public List<ScriptTask> getTasksByTag(String tag) {
        return tasks.values().stream()
                .filter(t -> t.hasTag(tag))
                .collect(Collectors.toList());
    }

    public List<ScriptTask> getTasksByState(ScriptState state) {
        return tasks.values().stream()
                .filter(t -> t.getState() == state)
                .collect(Collectors.toList());
    }

    public Collection<ScriptTask> getAllTasks() {
        return getTasks();
    }

    public int getActiveCount() {
        return (int) tasks.values().stream()
                .filter(t -> t.getState() == ScriptState.RUNNING || t.getState() == ScriptState.PAUSED)
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
        stats.put("running", getTasksByState(ScriptState.RUNNING).size());
        stats.put("paused", getTasksByState(ScriptState.PAUSED).size());
        stats.put("stopped", getTasksByState(ScriptState.STOPPED).size());
        stats.put("error", getTasksByState(ScriptState.ERROR).size());
        stats.put("enabled", enabled);
        return stats;
    }
}

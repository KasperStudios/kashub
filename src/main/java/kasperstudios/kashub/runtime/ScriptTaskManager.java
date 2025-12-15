package kasperstudios.kashub.runtime;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.ScriptManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Менеджер запущенных скриптов (Runtime Manager)
 * Управляет жизненным циклом ScriptTask
 */
public class ScriptTaskManager {
    private static ScriptTaskManager instance;
    
    private final Map<Integer, ScriptTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private boolean enabled = true;

    private ScriptTaskManager() {}

    public static ScriptTaskManager getInstance() {
        if (instance == null) {
            instance = new ScriptTaskManager();
        }
        return instance;
    }

    /**
     * Запускает новый скрипт
     */
    public ScriptTask startScript(String name, String code) {
        return startScript(name, code, null, ScriptType.USER);
    }

    /**
     * Запускает новый скрипт с тегами
     */
    public ScriptTask startScript(String name, String code, Set<String> tags) {
        return startScript(name, code, tags, ScriptType.USER);
    }

    /**
     * Запускает новый скрипт с полными параметрами
     */
    public ScriptTask startScript(String name, String code, Set<String> tags, ScriptType scriptType) {
        if (!enabled) {
            ScriptLogger.getInstance().warn("Script execution is disabled");
            return null;
        }

        int id = nextId.getAndIncrement();
        ScriptTask task = new ScriptTask(id, name, code, tags, scriptType);
        tasks.put(id, task);
        
        ScriptLogger.getInstance().info("Started task " + id + ": " + name);
        
        // Register for hot-reload if enabled and it's a user script
        if (KashubConfig.getInstance().hotReload && scriptType == ScriptType.USER) {
            ScriptFileWatcher.getInstance().registerScript(name, id);
        }
        
        // Parse and queue commands to the TASK's own queue (not global interpreter)
        try {
            task.parseAndQueue();
        } catch (Exception e) {
            task.stop();
            ScriptLogger.getInstance().error("Failed to start script " + name + ": " + e.getMessage());
        }
        
        return task;
    }

    /**
     * Запускает скрипт из файла
     */
    public ScriptTask startScriptFromFile(String name) {
        try {
            String code = ScriptManager.loadScript(name);
            return startScript(name, code, null, ScriptType.USER);
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Failed to load script " + name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Запускает системный скрипт (read-only)
     */
    public ScriptTask startSystemScript(String name, String code) {
        Set<String> tags = new HashSet<>();
        tags.add("system");
        return startScript(name, code, tags, ScriptType.SYSTEM);
    }

    /**
     * Вызывается каждый тик клиента
     */
    public void tick() {
        if (!enabled) return;

        KashubConfig config = KashubConfig.getInstance();
        int processed = 0;
        int runningCount = 0;

        for (ScriptTask task : tasks.values()) {
            if (processed >= config.maxScriptsPerTick) break;
            
            if (task.getState() == ScriptState.RUNNING) {
                runningCount++;
                try {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTaskManager.tick:117\",\"message\":\"Calling tick on task\",\"data\":{\"taskId\":" + task.getId() + ",\"taskName\":\"" + task.getName() + "\",\"processed\":" + processed + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    task.tick();
                    processed++;
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Task " + task.getId() + " tick error: " + e.getMessage());
                }
            }
            
            // Удаляем завершённые задачи старше 5 минут
            if (task.getState() == ScriptState.STOPPED && 
                System.currentTimeMillis() - task.getLastTickTime() > 300000) {
                // Unregister from file watcher
                if (KashubConfig.getInstance().hotReload && task.getScriptType() == ScriptType.USER) {
                    ScriptFileWatcher.getInstance().unregisterScript(task.getName());
                }
                tasks.remove(task.getId());
            }
        }
        
        // Log periodically to track script execution
        if (runningCount > 0 && System.currentTimeMillis() % 1000 < 50) { // Log roughly once per second
            ScriptLogger.getInstance().debug("ScriptTaskManager: " + runningCount + " running tasks, processed " + processed + " this tick");
        }
    }

    // Управление отдельными задачами
    public void pause(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null) task.pause();
    }

    public void resume(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null) task.resume();
    }

    public void stop(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null) {
            // Unregister from file watcher
            if (KashubConfig.getInstance().hotReload && task.getScriptType() == ScriptType.USER) {
                ScriptFileWatcher.getInstance().unregisterScript(task.getName());
            }
            task.stop();
        }
    }

    public void restart(int id) {
        ScriptTask task = tasks.get(id);
        if (task != null) task.restart();
    }

    // Массовые операции
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

    // Получение информации
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

    // Управление менеджером
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

    /**
     * Получает статистику по задачам
     */
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

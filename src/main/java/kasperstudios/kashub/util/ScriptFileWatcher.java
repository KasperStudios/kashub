package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.State;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptFileWatcher {
    private static ScriptFileWatcher instance;

    private final Map<String, FileTime> fileTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> runningScriptIds = new ConcurrentHashMap<>();
    private final Path scriptsDir;
    private WatchService watchService;
    private Thread watchThread;
    private boolean running = false;

    private ScriptFileWatcher() {
        this.scriptsDir = FabricLoader.getInstance().getConfigDir().resolve("kashub").resolve("scripts");
    }

    public static ScriptFileWatcher getInstance() {
        if (instance == null) {
            instance = new ScriptFileWatcher();
        }
        return instance;
    }

    public void start() {
        if (running)
            return;

        try {
            Files.createDirectories(scriptsDir);

            initializeFileTimestamps();

            watchService = FileSystems.getDefault().newWatchService();
            scriptsDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

            running = true;
            watchThread = new Thread(this::watchLoop, "ScriptFileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();

            ScriptLogger.getInstance().info("Script file watcher started");
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to start script file watcher", e);
        }
    }

    public void stop() {
        if (!running)
            return;

        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                Kashub.LOGGER.error("Error closing watch service", e);
            }
        }

        if (watchThread != null) {
            watchThread.interrupt();
        }

        ScriptLogger.getInstance().info("Script file watcher stopped");
    }

    public void registerScript(String scriptName, int taskId) {
        runningScriptIds.put(scriptName, taskId);
        ScriptLogger.getInstance().debug("Hot-reload: Registered script " + scriptName + " (task #" + taskId + ")");

        Path scriptFile = getScriptPath(scriptName);
        if (Files.exists(scriptFile)) {
            try {
                FileTime lastModified = Files.getLastModifiedTime(scriptFile);
                fileTimestamps.put(scriptName, lastModified);
            } catch (IOException e) {
                Kashub.LOGGER.warn("Failed to get file timestamp for " + scriptName, e);
            }
        }
    }

    public void unregisterScript(String scriptName) {
        runningScriptIds.remove(scriptName);
        fileTimestamps.remove(scriptName);
        ScriptLogger.getInstance().debug("Hot-reload: Unregistered script " + scriptName);
    }

    public void checkForChanges() {
        if (!KashubConfig.getInstance().hotReload) {
            return;
        }

        for (Map.Entry<String, Integer> entry : runningScriptIds.entrySet()) {
            String scriptName = entry.getKey();
            Integer taskId = entry.getValue();

            Path scriptFile = getScriptPath(scriptName);
            if (!Files.exists(scriptFile)) {
                continue;
            }

            try {
                FileTime currentModified = Files.getLastModifiedTime(scriptFile);
                FileTime lastKnown = fileTimestamps.get(scriptName);

                if (lastKnown != null && currentModified.compareTo(lastKnown) > 0) {

                    ScriptLogger.getInstance().info("Hot-reload: Detected change in " + scriptName + ", reloading...");
                    reloadScript(scriptName, taskId);
                    fileTimestamps.put(scriptName, currentModified);
                }
            } catch (IOException e) {
                Kashub.LOGGER.warn("Failed to check file timestamp for " + scriptName, e);
            }
        }
    }

    private synchronized void reloadScript(String scriptName, int oldTaskId) {
        TaskManager manager = TaskManager.getInstance();
        Task oldTask = manager.getTask(oldTaskId);

        if (oldTask == null || oldTask.getState() == State.STOPPED) {

            unregisterScript(scriptName);
            return;
        }

        State currentState = oldTask.getState();
        if (currentState == State.RUNNING) {
            ScriptLogger.getInstance()
                    .debug("Hot-reload: Waiting for task " + oldTaskId + " to finish current command...");
        }

        try {

            String newContent = ScriptManager.loadScript(scriptName);
            if (newContent == null) {
                ScriptLogger.getInstance().warn("Failed to reload " + scriptName + ": file not found");
                return;
            }

            runningScriptIds.remove(scriptName);

            oldTask.stop();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (oldTask.getState() != State.STOPPED) {
                ScriptLogger.getInstance()
                        .warn("Hot-reload: Old task " + oldTaskId + " did not stop cleanly, forcing...");
                oldTask.stop();
            }

            Task newTask = manager.startScript(scriptName, newContent);
            if (newTask != null) {

                runningScriptIds.put(scriptName, newTask.getId());
                ScriptLogger.getInstance().success(
                        "Hot-reload: Successfully reloaded " + scriptName + " (new task #" + newTask.getId() + ")");
            } else {
                ScriptLogger.getInstance().error("Hot-reload: Failed to start reloaded script: " + scriptName);
            }
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Error reloading script " + scriptName + ": " + e.getMessage());

            if (oldTask.getState() == State.RUNNING) {
                runningScriptIds.put(scriptName, oldTaskId);
            }
        }
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll();
                if (key == null) {

                    Thread.sleep(KashubConfig.getInstance().hotReloadCheckInterval);
                    checkForChanges();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (!filename.toString().endsWith(".kh")) {
                        continue;
                    }

                    String scriptName = filename.toString().replace(".kh", "");

                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY ||
                            kind == StandardWatchEventKinds.ENTRY_CREATE) {

                        if (runningScriptIds.containsKey(scriptName)) {

                            Thread.sleep(100);
                            checkForChanges();
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Kashub.LOGGER.error("Error in file watcher loop", e);
            }
        }
    }

    private void initializeFileTimestamps() {
        try {
            if (!Files.exists(scriptsDir)) {
                return;
            }

            Files.walk(scriptsDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kh"))
                    .forEach(path -> {
                        try {
                            String scriptName = scriptsDir.relativize(path).toString()
                                    .replace(".kh", "")
                                    .replace("\\", "/");
                            FileTime lastModified = Files.getLastModifiedTime(path);
                            fileTimestamps.put(scriptName, lastModified);
                        } catch (IOException e) {

                        }
                    });
        } catch (IOException e) {
            Kashub.LOGGER.warn("Failed to initialize file timestamps", e);
        }
    }

    private Path getScriptPath(String scriptName) {
        String filename = scriptName.endsWith(".kh") ? scriptName : scriptName + ".kh";
        return scriptsDir.resolve(filename);
    }

    public Set<String> getRegisteredScripts() {
        return new HashSet<>(runningScriptIds.keySet());
    }
}

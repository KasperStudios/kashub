package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import kasperstudios.kashub.runtime.ScriptState;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches for file changes and automatically reloads scripts if hot-reload is enabled
 */
public class ScriptFileWatcher {
    private static ScriptFileWatcher instance;
    
    private final Map<String, FileTime> fileTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Integer> runningScriptIds = new ConcurrentHashMap<>(); // scriptName -> taskId
    private final Path scriptsDir;
    private WatchService watchService;
    private Thread watchThread;
    private boolean running = false;
    
    private ScriptFileWatcher() {
        this.scriptsDir = Paths.get("config", "kashub", "scripts");
    }
    
    public static ScriptFileWatcher getInstance() {
        if (instance == null) {
            instance = new ScriptFileWatcher();
        }
        return instance;
    }
    
    /**
     * Start watching for file changes
     */
    public void start() {
        if (running) return;
        
        try {
            Files.createDirectories(scriptsDir);
            
            // Initialize file timestamps for existing files
            initializeFileTimestamps();
            
            // Start watch service
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
    
    /**
     * Stop watching for file changes
     */
    public void stop() {
        if (!running) return;
        
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
    
    /**
     * Register a running script for hot-reload tracking
     */
    public void registerScript(String scriptName, int taskId) {
        runningScriptIds.put(scriptName, taskId);
        ScriptLogger.getInstance().debug("Hot-reload: Registered script " + scriptName + " (task #" + taskId + ")");
        
        // Record initial timestamp
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
    
    /**
     * Unregister a script (when it stops)
     */
    public void unregisterScript(String scriptName) {
        runningScriptIds.remove(scriptName);
        fileTimestamps.remove(scriptName);
        ScriptLogger.getInstance().debug("Hot-reload: Unregistered script " + scriptName);
    }
    
    /**
     * Check for file changes and reload if needed
     */
    public void checkForChanges() {
        if (!KashubConfig.getInstance().hotReload) {
            return;
        }
        
        for (Map.Entry<String, Integer> entry : runningScriptIds.entrySet()) {
            String scriptName = entry.getKey();
            Integer taskId = entry.getValue();
            
            Path scriptFile = getScriptPath(scriptName);
            if (!Files.exists(scriptFile)) {
                continue; // File deleted, but don't auto-stop script
            }
            
            try {
                FileTime currentModified = Files.getLastModifiedTime(scriptFile);
                FileTime lastKnown = fileTimestamps.get(scriptName);
                
                if (lastKnown != null && currentModified.compareTo(lastKnown) > 0) {
                    // File was modified, reload script
                    ScriptLogger.getInstance().info("Hot-reload: Detected change in " + scriptName + ", reloading...");
                    reloadScript(scriptName, taskId);
                    fileTimestamps.put(scriptName, currentModified);
                }
            } catch (IOException e) {
                Kashub.LOGGER.warn("Failed to check file timestamp for " + scriptName, e);
            }
        }
    }
    
    /**
     * Reload a script by stopping the old one and starting a new one
     */
    private void reloadScript(String scriptName, int oldTaskId) {
        ScriptTaskManager manager = ScriptTaskManager.getInstance();
        ScriptTask oldTask = manager.getTask(oldTaskId);
        
        if (oldTask == null || oldTask.getState() == ScriptState.STOPPED) {
            // Task already stopped, just remove from tracking
            unregisterScript(scriptName);
            return;
        }
        
        try {
            // Load new content
            String newContent = ScriptManager.loadScript(scriptName);
            if (newContent == null) {
                ScriptLogger.getInstance().warn("Failed to reload " + scriptName + ": file not found");
                return;
            }
            
            // Stop old task
            oldTask.stop();
            
            // Start new task with same name
            ScriptTask newTask = manager.startScript(scriptName, newContent);
            if (newTask != null) {
                // Register new task for hot-reload
                registerScript(scriptName, newTask.getId());
                ScriptLogger.getInstance().success("Hot-reload: Successfully reloaded " + scriptName + " (new task #" + newTask.getId() + ")");
            } else {
                ScriptLogger.getInstance().error("Hot-reload: Failed to start reloaded script: " + scriptName);
            }
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Error reloading script " + scriptName + ": " + e.getMessage());
        }
    }
    
    /**
     * Watch loop for file system events
     */
    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll();
                if (key == null) {
                    // No events, check for changes manually (for external edits)
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
                    
                    // Only process .kh files
                    if (!filename.toString().endsWith(".kh")) {
                        continue;
                    }
                    
                    String scriptName = filename.toString().replace(".kh", "");
                    
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY || 
                        kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        // Check if this script is running and needs reload
                        if (runningScriptIds.containsKey(scriptName)) {
                            // Small delay to ensure file write is complete
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
    
    /**
     * Initialize timestamps for all existing script files
     */
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
                        // Ignore
                    }
                });
        } catch (IOException e) {
            Kashub.LOGGER.warn("Failed to initialize file timestamps", e);
        }
    }
    
    /**
     * Get the full path to a script file
     */
    private Path getScriptPath(String scriptName) {
        String filename = scriptName.endsWith(".kh") ? scriptName : scriptName + ".kh";
        return scriptsDir.resolve(filename);
    }
    
    /**
     * Get all registered script names
     */
    public Set<String> getRegisteredScripts() {
        return new HashSet<>(runningScriptIds.keySet());
    }
}


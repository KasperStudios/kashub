package kasperstudios.kashub.services.modpack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.util.ScriptLogger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ModpackScriptManager - Manages server-side and modpack scripts.
 * 
 * Server-side scripts execute on server (or in modpack context) and are
 * mandatory for all players.
 * These scripts can define custom crafting, global events, and persistent data.
 * 
 * Part of v0.9.0 Server & Modpack Scripts feature.
 * 
 * @since 0.9.0
 */
public class ModpackScriptManager {

    private static volatile ModpackScriptManager instance;
    private static final Object LOCK = new Object();

    private static final Path MODPACK_SCRIPTS_DIR = Paths.get("config", "kashub", "modpack_scripts");
    private static final Path PERSISTENT_DATA_DIR = Paths.get("config", "kashub", "persistent_data");
    private static final Path PERSISTENT_DATA_FILE = PERSISTENT_DATA_DIR.resolve("data.json");

    private final Map<String, ModpackScript> loadedScripts;
    private final Map<String, Object> persistentData;
    private final Gson gson;
    private boolean initialized;

    private ModpackScriptManager() {
        this.loadedScripts = new ConcurrentHashMap<>();
        this.persistentData = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.initialized = false;

        try {
            Files.createDirectories(MODPACK_SCRIPTS_DIR);
            Files.createDirectories(PERSISTENT_DATA_DIR);
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to create modpack scripts directories", e);
        }
    }

    public static ModpackScriptManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ModpackScriptManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize modpack scripts system.
     * Called when world loads.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        ScriptLogger.getInstance().info("Initializing modpack scripts system...");

        // Load persistent data
        loadPersistentData();

        // Discover and load modpack scripts
        discoverModpackScripts();

        // Auto-start mandatory modpack scripts
        autoStartModpackScripts();

        initialized = true;
        ScriptLogger.getInstance().success("Modpack scripts system initialized");
    }

    /**
     * Shutdown modpack scripts system.
     * Called when world unloads.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        ScriptLogger.getInstance().info("Shutting down modpack scripts system...");

        // Save persistent data
        savePersistentData();

        // Stop all modpack scripts
        stopAllModpackScripts();

        loadedScripts.clear();
        initialized = false;

        ScriptLogger.getInstance().info("Modpack scripts system shut down");
    }

    /**
     * Discover modpack scripts from modpack_scripts directory.
     * Public for reloading after script changes.
     */
    public void discoverModpackScripts() {
        try {
            if (!Files.exists(MODPACK_SCRIPTS_DIR)) {
                return;
            }

            Files.walk(MODPACK_SCRIPTS_DIR)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kh"))
                    .forEach(path -> {
                        try {
                            String scriptName = MODPACK_SCRIPTS_DIR.relativize(path)
                                    .toString()
                                    .replace(".kh", "")
                                    .replace("\\", "/");

                            String content = Files.readString(path);
                            ModpackScript script = new ModpackScript(scriptName, content, path);
                            loadedScripts.put(scriptName, script);

                            ScriptLogger.getInstance().info("Discovered modpack script: " + scriptName);
                        } catch (Exception e) {
                            Kashub.LOGGER.error("Failed to load modpack script: " + path, e);
                        }
                    });

        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to discover modpack scripts", e);
        }
    }

    /**
     * Auto-start mandatory modpack scripts.
     */
    private void autoStartModpackScripts() {
        for (ModpackScript script : loadedScripts.values()) {
            if (script.isMandatory()) {
                try {
                    Set<String> tags = new HashSet<>();
                    tags.add("modpack");
                    TaskManager.getInstance().startScript(
                            script.getName(),
                            script.getContent(),
                            tags);
                    ScriptLogger.getInstance().info("Auto-started modpack script: " + script.getName());
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Failed to auto-start modpack script: " + script.getName());
                    Kashub.LOGGER.error("Modpack script error", e);
                }
            }
        }
    }

    /**
     * Stop all running modpack scripts.
     */
    private void stopAllModpackScripts() {
        // Stop all tasks with "modpack" tag
        TaskManager.getInstance().stopByTag("modpack");
    }

    /**
     * Load persistent data from disk.
     */
    private void loadPersistentData() {
        try {
            if (!Files.exists(PERSISTENT_DATA_FILE)) {
                ScriptLogger.getInstance().info("No persistent data file found, starting fresh");
                return;
            }

            String json = Files.readString(PERSISTENT_DATA_FILE);
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> loaded = gson.fromJson(json, type);

            if (loaded != null) {
                persistentData.putAll(loaded);
                ScriptLogger.getInstance().success("Loaded " + loaded.size() + " persistent data entries");
            }

        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to load persistent data", e);
            ScriptLogger.getInstance().error("Failed to load persistent data: " + e.getMessage());
        }
    }

    /**
     * Save persistent data to disk.
     */
    private void savePersistentData() {
        try {
            String json = gson.toJson(persistentData);
            Files.writeString(PERSISTENT_DATA_FILE, json);
            ScriptLogger.getInstance().info("Saved " + persistentData.size() + " persistent data entries");

        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to save persistent data", e);
            ScriptLogger.getInstance().error("Failed to save persistent data: " + e.getMessage());
        }
    }

    /**
     * Store persistent data that survives server restarts.
     */
    public void setPersistentData(String key, Object value) {
        persistentData.put(key, value);
        savePersistentData();
    }

    /**
     * Retrieve persistent data.
     */
    public Object getPersistentData(String key) {
        return persistentData.get(key);
    }

    /**
     * Check if persistent data exists.
     */
    public boolean hasPersistentData(String key) {
        return persistentData.containsKey(key);
    }

    /**
     * Remove persistent data.
     */
    public void removePersistentData(String key) {
        persistentData.remove(key);
        savePersistentData();
    }

    /**
     * Get all loaded modpack scripts.
     */
    public Collection<ModpackScript> getLoadedScripts() {
        return Collections.unmodifiableCollection(loadedScripts.values());
    }

    /**
     * Check if modpack scripts system is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}

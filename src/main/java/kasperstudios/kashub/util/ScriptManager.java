package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Type;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ScriptManager {
    private static final Path SCRIPTS_DIR = FabricLoader.getInstance().getConfigDir().resolve("kashub")
            .resolve("scripts");
    private static final Path SERVER_SCRIPTS_DIR = FabricLoader.getInstance().getConfigDir().resolve("kashub")
            .resolve("modpack_scripts");

    private static List<String> cachedSystemScripts = null;

    static {
        try {
            Files.createDirectories(SCRIPTS_DIR);
            Files.createDirectories(SERVER_SCRIPTS_DIR);
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to create scripts directory", e);
        }
    }

    public static boolean saveScript(String name, String content) {
        try {
            if (isSystemScript(name)) {
                Kashub.LOGGER.warn("Cannot save system script: " + name);
                return false;
            }

            String filename = name.endsWith(".kh") ? name : name + ".kh";

            Path scriptFile = SCRIPTS_DIR.resolve(filename);
            Files.createDirectories(scriptFile.getParent());
            Files.writeString(scriptFile, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to save script: " + name, e);
            return false;
        }
    }

    public static String loadScript(String name) {
        try {
            String filename = name.endsWith(".kh") ? name : name + ".kh";
            String baseName = name.replace(".kh", "");

            if (isSystemScript(baseName)) {
                return loadSystemScript(baseName);
            }

            Path scriptFile = SCRIPTS_DIR.resolve(filename);
            if (Files.exists(scriptFile)) {
                return Files.readString(scriptFile, StandardCharsets.UTF_8);
            }

            return null;
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to load script: " + name, e);
            return null;
        }
    }

    public static String loadSystemScript(String name) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getResourceManager() == null) {
                return null;
            }

            String path = name.contains("/") ? "scripts/" + name + ".kh" : "scripts/" + name + ".kh";
            Identifier id = Identifier.of(Kashub.MOD_ID, path);
            Optional<Resource> resourceOpt = client.getResourceManager().getResource(id);

            if (resourceOpt.isPresent()) {
                try (InputStream is = resourceOpt.get().getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to load system script: " + name, e);
        }
        return null;
    }

    public static boolean scriptExists(String name) {
        String baseName = name.replace(".kh", "");
        if (isSystemScript(baseName)) {
            return true;
        }
        return Files.exists(SCRIPTS_DIR.resolve(baseName + ".kh"));
    }

    public static boolean isSystemScript(String name) {
        String baseName = name.replace(".kh", "");

        if (cachedSystemScripts != null && cachedSystemScripts.contains(baseName)) {
            return true;
        }

        return baseName.startsWith("example_") || baseName.startsWith("system_");
    }

    public static Type getScriptType(String name) {
        if (isSystemScript(name)) {
            return Type.SYSTEM;
        }
        return Type.USER;
    }

    public static boolean deleteScript(String name) {
        try {
            if (isSystemScript(name)) {
                Kashub.LOGGER.warn("Cannot delete system script: " + name);
                return false;
            }
            String filename = name.endsWith(".kh") ? name : name + ".kh";
            Path scriptFile = SCRIPTS_DIR.resolve(filename);
            return Files.deleteIfExists(scriptFile);
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to delete script: " + name, e);
            return false;
        }
    }

    public static List<String> getUserScripts() {
        return getUserScripts("");
    }

    public static List<String> getUserScripts(String subdir) {
        try {
            Path scriptsPath = subdir.isEmpty() ? SCRIPTS_DIR : SCRIPTS_DIR.resolve(subdir);
            if (!Files.exists(scriptsPath)) {
                return new ArrayList<>();
            }

            List<String> scripts = new ArrayList<>();
            Files.walk(scriptsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kh"))
                    .forEach(path -> {
                        try {
                            Path relativePath = SCRIPTS_DIR.relativize(path);
                            String scriptName = relativePath.toString().replace(".kh", "").replace("\\", "/");
                            if (!isSystemScript(scriptName)) {
                                scripts.add(scriptName);
                            }
                        } catch (Exception e) {
                            Kashub.LOGGER.warn("Failed to process script path: " + path, e);
                        }
                    });

            scripts.sort(String::compareToIgnoreCase);
            return scripts;
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to list user scripts", e);
            return new ArrayList<>();
        }
    }

    public static List<String> getSystemScripts() {

        if (cachedSystemScripts != null) {
            return new ArrayList<>(cachedSystemScripts);
        }

        List<String> scripts = new ArrayList<>();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                ResourceManager resourceManager = client.getResourceManager();

                Map<Identifier, Resource> foundResources = resourceManager.findResources("scripts",
                        id -> id.getPath().endsWith(".kh"));

                for (Identifier id : foundResources.keySet()) {

                    if (!id.getNamespace().equals(Kashub.MOD_ID)) {
                        continue;
                    }

                    String path = id.getPath();
                    if (path.startsWith("scripts/") && path.endsWith(".kh")) {
                        String scriptName = path.substring(8, path.length() - 3);

                        scripts.add(scriptName);
                    }
                }
            }
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to discover system scripts", e);

            List<String> fallback = List.of(
                    "example_vision", "example_http", "example_input", "example_animations",
                    "example_tasks", "example_ore_detector", "example_music",
                    "example_basic", "example_building", "example_combat", "example_farming",
                    "example_mining", "example_autocraft", "example_autotrade", "example_events",
                    "example_pathfind_advanced", "example_scanner_advanced", "example_deepslate_miner",
                    "example_area_clearer");
            for (String name : fallback) {
                if (loadSystemScript(name) != null) {
                    scripts.add(name);
                }
            }
        }

        scripts.sort(String::compareToIgnoreCase);
        cachedSystemScripts = scripts;
        return new ArrayList<>(scripts);
    }

    public static void clearSystemScriptsCache() {
        cachedSystemScripts = null;
    }

    public static List<String> getAllScripts() {
        List<String> all = new ArrayList<>();
        all.addAll(getSystemScripts());
        all.addAll(getUserScripts());
        return all;
    }

    public static List<ScriptInfo> getAllScriptsWithInfo() {
        return getAllScriptsWithInfo(false);
    }

    public static List<ScriptInfo> getAllScriptsWithInfo(boolean hideSystemScripts) {
        List<ScriptInfo> result = new ArrayList<>();

        if (!hideSystemScripts) {
            for (String name : getSystemScripts()) {
                result.add(new ScriptInfo(name, Type.SYSTEM));
            }
        }

        for (String name : getUserScripts()) {
            result.add(new ScriptInfo(name, Type.USER));
        }

        return result;
    }

    public static class ScriptInfo {
        public final String name;
        public final Type type;

        public ScriptInfo(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public boolean isEditable() {
            return type.isEditable();
        }
    }

    // ========== SERVER SCRIPTS (v0.9.0) ==========

    /**
     * Check if player has operator permissions (can edit server scripts)
     */
    public static boolean isOperator() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return false;

        // In singleplayer, always allow
        if (client.isInSingleplayer())
            return true;

        // In multiplayer, check if player has permission level 2+ (operator)
        return client.player.hasPermissionLevel(2);
    }

    /**
     * Get list of server scripts (modpack scripts)
     */
    public static List<String> getServerScripts() {
        try {
            if (!Files.exists(SERVER_SCRIPTS_DIR)) {
                return new ArrayList<>();
            }

            List<String> scripts = new ArrayList<>();
            Files.walk(SERVER_SCRIPTS_DIR)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".kh"))
                    .forEach(path -> {
                        try {
                            Path relativePath = SERVER_SCRIPTS_DIR.relativize(path);
                            String scriptName = relativePath.toString().replace(".kh", "").replace("\\", "/");
                            scripts.add(scriptName);
                        } catch (Exception e) {
                            Kashub.LOGGER.warn("Failed to process server script path: " + path, e);
                        }
                    });

            scripts.sort(String::compareToIgnoreCase);
            return scripts;
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to list server scripts", e);
            return new ArrayList<>();
        }
    }

    /**
     * Load server script content
     */
    public static String loadServerScript(String name) {
        try {
            String filename = name.endsWith(".kh") ? name : name + ".kh";
            Path scriptFile = SERVER_SCRIPTS_DIR.resolve(filename);
            if (Files.exists(scriptFile)) {
                return Files.readString(scriptFile, StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to load server script: " + name, e);
            return null;
        }
    }

    /**
     * Save server script (requires operator permissions)
     */
    public static boolean saveServerScript(String name, String content) {
        if (!isOperator()) {
            Kashub.LOGGER.warn("Cannot save server script without operator permissions: " + name);
            return false;
        }

        try {
            String filename = name.endsWith(".kh") ? name : name + ".kh";
            Path scriptFile = SERVER_SCRIPTS_DIR.resolve(filename);
            Files.createDirectories(scriptFile.getParent());
            Files.writeString(scriptFile, content, StandardCharsets.UTF_8);

            // Reload modpack scripts after save
            kasperstudios.kashub.services.modpack.ModpackScriptManager.getInstance().discoverModpackScripts();

            return true;
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to save server script: " + name, e);
            return false;
        }
    }

    /**
     * Delete server script (requires operator permissions)
     */
    public static boolean deleteServerScript(String name) {
        if (!isOperator()) {
            Kashub.LOGGER.warn("Cannot delete server script without operator permissions: " + name);
            return false;
        }

        try {
            String filename = name.endsWith(".kh") ? name : name + ".kh";
            Path scriptFile = SERVER_SCRIPTS_DIR.resolve(filename);
            boolean deleted = Files.deleteIfExists(scriptFile);

            if (deleted) {
                // Reload modpack scripts after delete
                kasperstudios.kashub.services.modpack.ModpackScriptManager.getInstance().discoverModpackScripts();
            }

            return deleted;
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to delete server script: " + name, e);
            return false;
        }
    }

    /**
     * Check if script is a server script
     */
    public static boolean isServerScript(String name) {
        String baseName = name.replace(".kh", "");
        return getServerScripts().contains(baseName);
    }
}

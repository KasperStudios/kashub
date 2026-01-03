package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.services.runtime.ScriptType;
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
    private static final Path SCRIPTS_DIR = Paths.get("config", "kashub", "scripts");

    private static List<String> cachedSystemScripts = null;

    static {
        try {
            Files.createDirectories(SCRIPTS_DIR);
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

    public static ScriptType getScriptType(String name) {
        if (isSystemScript(name)) {
            return ScriptType.SYSTEM;
        }
        return ScriptType.USER;
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
                result.add(new ScriptInfo(name, ScriptType.SYSTEM));
            }
        }

        for (String name : getUserScripts()) {
            result.add(new ScriptInfo(name, ScriptType.USER));
        }

        return result;
    }

    public static class ScriptInfo {
        public final String name;
        public final ScriptType type;

        public ScriptInfo(String name, ScriptType type) {
            this.name = name;
            this.type = type;
        }

        public boolean isEditable() {
            return type.isEditable();
        }
    }
}

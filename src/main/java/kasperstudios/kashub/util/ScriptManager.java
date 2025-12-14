package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.runtime.ScriptType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ScriptManager {
    private static final Path SCRIPTS_DIR = Paths.get("config", "kashub", "scripts");
    
    private static final List<String> SYSTEM_SCRIPTS = List.of(
        "example_vision",
        "example_http", 
        "example_input",
        "example_animations",
        "example_tasks",
        "example_ore_detector",
        "example_music"
    );

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
            
            // Try system script first
            if (isSystemScript(baseName)) {
                return loadSystemScript(baseName);
            }
            
            // Try user script
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

            Identifier id = Identifier.of(Kashub.MOD_ID, "scripts/" + name + ".kh");
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
        return SYSTEM_SCRIPTS.contains(baseName) || baseName.startsWith("example_") || baseName.startsWith("system_");
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
        try {
            if (!Files.exists(SCRIPTS_DIR)) {
                return new ArrayList<>();
            }
            
            return Files.list(SCRIPTS_DIR)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".kh"))
                .map(name -> name.replace(".kh", ""))
                .filter(name -> !isSystemScript(name))
                .sorted()
                .collect(Collectors.toList());
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to list user scripts", e);
            return new ArrayList<>();
        }
    }

    public static List<String> getSystemScripts() {
        return new ArrayList<>(SYSTEM_SCRIPTS);
    }

    public static List<String> getAllScripts() {
        List<String> all = new ArrayList<>();
        all.addAll(getSystemScripts());
        all.addAll(getUserScripts());
        return all;
    }

    public static List<ScriptInfo> getAllScriptsWithInfo() {
        List<ScriptInfo> result = new ArrayList<>();
        
        for (String name : getSystemScripts()) {
            result.add(new ScriptInfo(name, ScriptType.SYSTEM));
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

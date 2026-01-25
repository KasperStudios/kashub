package kasperstudios.kashub.server.script;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kasperstudios.kashub.server.KasHubServerPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerScriptManager {
    private final KasHubServerPlugin plugin;
    private final File scriptsFolder;
    private final File autorunFile;
    private final Gson gson = new Gson();

    // Authorization: User UUID -> List of script names to run on join
    private Map<String, List<String>> autorunConfig = new HashMap<>();

    public ServerScriptManager(KasHubServerPlugin plugin) {
        this.plugin = plugin;
        this.scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        if (!this.scriptsFolder.exists()) {
            this.scriptsFolder.mkdirs();
        }

        this.autorunFile = new File(plugin.getDataFolder(), "autorun.json");
        loadAutorunConfig();
    }

    private void loadAutorunConfig() {
        if (!autorunFile.exists()) {
            saveAutorunConfig();
            return;
        }
        try (FileReader reader = new FileReader(autorunFile)) {
            autorunConfig = gson.fromJson(reader, new TypeToken<Map<String, List<String>>>() {
            }.getType());
            if (autorunConfig == null)
                autorunConfig = new HashMap<>();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load autorun.json: " + e.getMessage());
            autorunConfig = new HashMap<>();
        }
    }

    public void saveAutorunConfig() {
        try (FileWriter writer = new FileWriter(autorunFile)) {
            gson.toJson(autorunConfig, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save autorun.json: " + e.getMessage());
        }
    }

    public List<String> listScripts() {
        List<String> results = new ArrayList<>();
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".kh"));
        if (files != null) {
            for (File file : files) {
                results.add(file.getName());
            }
        }
        return results;
    }

    public String getScriptContent(String name) {
        if (!name.endsWith(".kh"))
            name += ".kh";
        File file = new File(scriptsFolder, name);
        if (!file.exists())
            return null;

        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read script " + name + ": " + e.getMessage());
            return null;
        }
    }

    public boolean saveScript(String name, String content) {
        if (!name.endsWith(".kh"))
            name += ".kh";
        // Sanitize name to prevent path traversal
        name = new File(name).getName();
        File file = new File(scriptsFolder, name);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save script " + name + ": " + e.getMessage());
            return false;
        }
    }

    public boolean deleteScript(String name) {
        if (!name.endsWith(".kh"))
            name += ".kh";
        name = new File(name).getName();
        File file = new File(scriptsFolder, name);
        return file.exists() && file.delete();
    }

    public List<String> getGlobalAutorunScripts() {
        return autorunConfig.getOrDefault("global", new ArrayList<>());
    }

    public List<String> getPlayerAutorunScripts(String playerName) {
        return autorunConfig.getOrDefault(playerName, new ArrayList<>());
    }

    public void addAutorun(String target, String scriptName) {
        autorunConfig.computeIfAbsent(target, k -> new ArrayList<>()).add(scriptName);
        saveAutorunConfig();
    }

    public void removeAutorun(String target, String scriptName) {
        if (autorunConfig.containsKey(target)) {
            autorunConfig.get(target).remove(scriptName);
            saveAutorunConfig();
        }
    }
}

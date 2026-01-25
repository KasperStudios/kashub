package kasperstudios.kashub.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import kasperstudios.kashub.server.KasHubServerPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StorageManager {
    private final KasHubServerPlugin plugin;
    private final File dataFolder;
    private final Gson gson;
    private final Map<String, JsonObject> cache = new HashMap<>();

    public StorageManager(KasHubServerPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public JsonObject getData(String namespace) {
        if (cache.containsKey(namespace)) {
            return cache.get(namespace);
        }

        File file = new File(dataFolder, namespace + ".json");
        if (!file.exists()) {
            JsonObject empty = new JsonObject();
            cache.put(namespace, empty);
            return empty;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            if (data == null)
                data = new JsonObject();
            cache.put(namespace, data);
            return data;
        } catch (IOException | JsonSyntaxException e) {
            plugin.getLogger().warning("Failed to load data for namespace " + namespace + ": " + e.getMessage());
            return new JsonObject();
        }
    }

    public void saveData(String namespace, JsonObject data) {
        cache.put(namespace, data); // Update cache

        // Async save? For now, sync to ensure safety.
        // In loop could be bad, but typical usage is infrequent saves.

        File file = new File(dataFolder, namespace + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data for namespace " + namespace + ": " + e.getMessage());
        }
    }

    public void saveDataAsync(String namespace, JsonObject data) {
        cache.put(namespace, data);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(dataFolder, namespace + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Async save failed for " + namespace + ": " + e.getMessage());
            }
        });
    }

    public void clearCache() {
        cache.clear();
    }
}

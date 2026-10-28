package kasperstudios.kashub.services.integration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.Kashub;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the persistence of scanned ComputerCraft peripherals.
 * Saves the discovered types and their methods to a local JSON file.
 */
public class ScannedPeripheralManager {

    private static final ScannedPeripheralManager INSTANCE = new ScannedPeripheralManager();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Maps peripheral type to list of method names
    private final Map<String, List<String>> scannedData = new ConcurrentHashMap<>();
    private String lastScannedType = null;

    private ScannedPeripheralManager() {
        load();
    }

    public static ScannedPeripheralManager getInstance() {
        return INSTANCE;
    }

    public void add(String type, List<String> methods) {
        if (type == null || type.isEmpty() || type.equals("unknown")) return;
        
        // Always update to ensure we have the latest methods
        scannedData.put(type, new ArrayList<>(methods));
        lastScannedType = type;
        save();
        
        // Refresh IDE completions if on client
        if (KHWrapper.get().isClientSide()) {
            kasperstudios.kashub.gui.CodeCompletionManager.refreshCompletions();
        }
    }

    public String getLastScannedType() {
        return lastScannedType;
    }

    public List<String> getAllMethods() {
        List<String> all = new ArrayList<>();
        for (List<String> methods : scannedData.values()) {
            for (String m : methods) {
                if (!all.contains(m)) all.add(m);
            }
        }
        return all;
    }

    public Map<String, List<String>> getAllScanned() {
        return new HashMap<>(scannedData);
    }

    private Path getFilePath() {
        try {
            Path configDir = KHWrapper.get().getConfigDir().resolve("kashub");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            return configDir.resolve("scanned_peripherals.json");
        } catch (IOException e) {
            return null;
        }
    }

    public void load() {
        Path path = getFilePath();
        if (path == null || !Files.exists(path)) return;

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Map<String, List<String>> data = gson.fromJson(json, new TypeToken<Map<String, List<String>>>() {}.getType());
            if (data != null) {
                scannedData.clear();
                scannedData.putAll(data);
            }
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to load scanned peripherals", e);
        }
    }

    public void save() {
        Path path = getFilePath();
        if (path == null) return;

        try {
            String json = gson.toJson(scannedData);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to save scanned peripherals", e);
        }
    }
}

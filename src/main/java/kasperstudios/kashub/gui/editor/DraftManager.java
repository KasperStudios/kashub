package kasperstudios.kashub.gui.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Draft manager for script editor
 * Automatically saves unsaved changes and offers recovery
 */
public class DraftManager {
    private static final Logger LOGGER = LogManager.getLogger(DraftManager.class);
    private static final Path DRAFTS_DIR = Paths.get("config", "kashub", "drafts");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static DraftManager instance;
    
    // Current drafts in memory
    private final Map<String, Draft> activeDrafts = new HashMap<>();
    
    // Auto-save interval (ms)
    private static final long AUTO_SAVE_INTERVAL = 30000; // 30 seconds
    private long lastAutoSave = 0;
    
    private DraftManager() {
        ensureDraftsDirectory();
    }
    
    public static DraftManager getInstance() {
        if (instance == null) {
            instance = new DraftManager();
        }
        return instance;
    }
    
    private void ensureDraftsDirectory() {
        try {
            Files.createDirectories(DRAFTS_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create drafts directory", e);
        }
    }
    
    /**
     * Save draft for script
     */
    public void saveDraft(String scriptName, String content, int cursorPosition) {
        Draft draft = new Draft();
        draft.scriptName = scriptName;
        draft.content = content;
        draft.cursorPosition = cursorPosition;
        draft.timestamp = Instant.now().toEpochMilli();
        
        activeDrafts.put(scriptName, draft);
        
        // Save to disk
        Path draftPath = DRAFTS_DIR.resolve(sanitizeFileName(scriptName) + ".draft.json");
        try {
            String json = GSON.toJson(draft);
            Files.write(draftPath, json.getBytes());
            LOGGER.debug("Saved draft for: {}", scriptName);
        } catch (IOException e) {
            LOGGER.error("Failed to save draft for: {}", scriptName, e);
        }
    }
    
    /**
     * Load draft for script
     */
    public Draft loadDraft(String scriptName) {
        // First check memory
        if (activeDrafts.containsKey(scriptName)) {
            return activeDrafts.get(scriptName);
        }
        
        // Then check disk
        Path draftPath = DRAFTS_DIR.resolve(sanitizeFileName(scriptName) + ".draft.json");
        if (Files.exists(draftPath)) {
            try {
                String json = new String(Files.readAllBytes(draftPath));
                Draft draft = GSON.fromJson(json, Draft.class);
                activeDrafts.put(scriptName, draft);
                return draft;
            } catch (IOException e) {
                LOGGER.error("Failed to load draft for: {}", scriptName, e);
            }
        }
        
        return null;
    }
    
    /**
     * Check if draft exists for script
     */
    public boolean hasDraft(String scriptName) {
        if (activeDrafts.containsKey(scriptName)) {
            return true;
        }
        
        Path draftPath = DRAFTS_DIR.resolve(sanitizeFileName(scriptName) + ".draft.json");
        return Files.exists(draftPath);
    }
    
    /**
     * Delete draft (after successful save)
     */
    public void deleteDraft(String scriptName) {
        activeDrafts.remove(scriptName);
        
        Path draftPath = DRAFTS_DIR.resolve(sanitizeFileName(scriptName) + ".draft.json");
        try {
            Files.deleteIfExists(draftPath);
            LOGGER.debug("Deleted draft for: {}", scriptName);
        } catch (IOException e) {
            LOGGER.error("Failed to delete draft for: {}", scriptName, e);
        }
    }
    
    /**
     * Get list of all available drafts
     */
    public List<DraftInfo> getAllDrafts() {
        List<DraftInfo> drafts = new ArrayList<>();
        
        try {
            if (Files.exists(DRAFTS_DIR)) {
                Files.list(DRAFTS_DIR)
                    .filter(p -> p.toString().endsWith(".draft.json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Draft draft = GSON.fromJson(json, Draft.class);
                            drafts.add(new DraftInfo(
                                draft.scriptName,
                                draft.timestamp,
                                draft.content.length()
                            ));
                        } catch (IOException e) {
                            LOGGER.error("Failed to read draft: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list drafts", e);
        }
        
        // Sort by time (newest first)
        drafts.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        
        return drafts;
    }
    
    /**
     * Auto-save draft (called periodically)
     */
    public void autoSave(String scriptName, String content, int cursorPosition) {
        long now = System.currentTimeMillis();
        if (now - lastAutoSave >= AUTO_SAVE_INTERVAL) {
            saveDraft(scriptName, content, cursorPosition);
            lastAutoSave = now;
        }
    }
    
    /**
     * Cleanup old drafts (older than 7 days)
     */
    public void cleanupOldDrafts() {
        long cutoff = Instant.now().toEpochMilli() - (7 * 24 * 60 * 60 * 1000L);
        
        try {
            if (Files.exists(DRAFTS_DIR)) {
                Files.list(DRAFTS_DIR)
                    .filter(p -> p.toString().endsWith(".draft.json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Draft draft = GSON.fromJson(json, Draft.class);
                            if (draft.timestamp < cutoff) {
                                Files.delete(path);
                                LOGGER.info("Cleaned up old draft: {}", draft.scriptName);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed to cleanup draft: {}", path, e);
                        }
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to cleanup drafts", e);
        }
    }
    
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Class for storing draft
     */
    public static class Draft {
        public String scriptName;
        public String content;
        public int cursorPosition;
        public long timestamp;
    }
    
    /**
     * Brief draft information
     */
    public static class DraftInfo {
        public final String scriptName;
        public final long timestamp;
        public final int contentLength;
        
        public DraftInfo(String scriptName, long timestamp, int contentLength) {
            this.scriptName = scriptName;
            this.timestamp = timestamp;
            this.contentLength = contentLength;
        }
        
        public String getFormattedTime() {
            long age = Instant.now().toEpochMilli() - timestamp;
            if (age < 60000) {
                return "just now";
            } else if (age < 3600000) {
                return (age / 60000) + " min ago";
            } else if (age < 86400000) {
                return (age / 3600000) + " hours ago";
            } else {
                return (age / 86400000) + " days ago";
            }
        }
    }
}

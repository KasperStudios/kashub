package kasperstudios.kashub.marketplace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.*;

/**
 * Configuration for the Script Marketplace.
 * 
 * Settings include:
 * - Enable/disable marketplace features
 * - Repository URL configuration
 * - Cache settings
 * - Auto-update preferences
 */
public class MarketplaceConfig {
    private static MarketplaceConfig instance;
    private static final Path CONFIG_PATH = Paths.get("config", "kashub", "marketplace.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Default GitHub repository for verified scripts
    private static final String DEFAULT_REPOSITORY = "https://github.com/kasperstudios/kashub-scripts";
    
    // Marketplace settings
    private boolean enabled = true;
    private String repositoryUrl = DEFAULT_REPOSITORY;
    private String repositoryBranch = "main";
    
    // Cache settings
    private boolean cacheEnabled = true;
    private int cacheExpirationMinutes = 60; // 1 hour
    private int maxCachedScripts = 100;
    
    // Auto-update settings
    private boolean autoCheckUpdates = true;
    private int updateCheckIntervalHours = 24;
    private boolean notifyOnUpdates = true;
    
    // Download settings
    private boolean verifySignatures = true;
    private boolean allowUnsignedScripts = false;
    private String scriptsInstallPath = "scripts/marketplace";
    
    // UI settings
    private boolean showRatings = true;
    private boolean showDownloadCount = true;
    private String defaultCategory = "all";
    private String defaultSortBy = "downloads"; // downloads, rating, updated, name
    
    private MarketplaceConfig() {}
    
    public static synchronized MarketplaceConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    public static MarketplaceConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = new String(Files.readAllBytes(CONFIG_PATH));
                MarketplaceConfig config = GSON.fromJson(json, MarketplaceConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load marketplace config: " + e.getMessage());
        }
        
        MarketplaceConfig config = new MarketplaceConfig();
        config.save();
        return config;
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.write(CONFIG_PATH, json.getBytes());
        } catch (Exception e) {
            System.err.println("Failed to save marketplace config: " + e.getMessage());
        }
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getRepositoryBranch() { return repositoryBranch; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public int getCacheExpirationMinutes() { return cacheExpirationMinutes; }
    public int getMaxCachedScripts() { return maxCachedScripts; }
    public boolean isAutoCheckUpdates() { return autoCheckUpdates; }
    public int getUpdateCheckIntervalHours() { return updateCheckIntervalHours; }
    public boolean isNotifyOnUpdates() { return notifyOnUpdates; }
    public boolean isVerifySignatures() { return verifySignatures; }
    public boolean isAllowUnsignedScripts() { return allowUnsignedScripts; }
    public String getScriptsInstallPath() { return scriptsInstallPath; }
    public boolean isShowRatings() { return showRatings; }
    public boolean isShowDownloadCount() { return showDownloadCount; }
    public String getDefaultCategory() { return defaultCategory; }
    public String getDefaultSortBy() { return defaultSortBy; }
    
    // Setters with auto-save
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }
    
    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
        save();
    }
    
    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
        save();
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        save();
    }
    
    public void setCacheExpirationMinutes(int cacheExpirationMinutes) {
        this.cacheExpirationMinutes = cacheExpirationMinutes;
        save();
    }
    
    public void setAutoCheckUpdates(boolean autoCheckUpdates) {
        this.autoCheckUpdates = autoCheckUpdates;
        save();
    }
    
    public void setVerifySignatures(boolean verifySignatures) {
        this.verifySignatures = verifySignatures;
        save();
    }
    
    public void setAllowUnsignedScripts(boolean allowUnsignedScripts) {
        this.allowUnsignedScripts = allowUnsignedScripts;
        save();
    }
    
    public void setDefaultSortBy(String defaultSortBy) {
        this.defaultSortBy = defaultSortBy;
        save();
    }
    
    /**
     * Get the full URL for the scripts catalog JSON.
     * 
     * @return Catalog URL
     */
    public String getCatalogUrl() {
        return repositoryUrl + "/raw/" + repositoryBranch + "/catalog.json";
    }
    
    /**
     * Get the base URL for downloading scripts.
     * 
     * @return Scripts base URL
     */
    public String getScriptsBaseUrl() {
        return repositoryUrl + "/raw/" + repositoryBranch + "/scripts/";
    }
    
    /**
     * Reset to default settings.
     */
    public void resetToDefaults() {
        this.enabled = true;
        this.repositoryUrl = DEFAULT_REPOSITORY;
        this.repositoryBranch = "main";
        this.cacheEnabled = true;
        this.cacheExpirationMinutes = 60;
        this.maxCachedScripts = 100;
        this.autoCheckUpdates = true;
        this.updateCheckIntervalHours = 24;
        this.notifyOnUpdates = true;
        this.verifySignatures = true;
        this.allowUnsignedScripts = false;
        this.scriptsInstallPath = "scripts/marketplace";
        this.showRatings = true;
        this.showDownloadCount = true;
        this.defaultCategory = "all";
        this.defaultSortBy = "downloads";
        save();
    }
}

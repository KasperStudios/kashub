package kasperstudios.kashub.marketplace;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MarketplaceService {
    private static MarketplaceService instance;

    private final List<VerifiedScript> cachedScripts = new ArrayList<>();
    private final MarketplaceConfig config;
    private boolean initialized = false;
    private long lastFetchTime = 0;

    private MarketplaceService() {
        this.config = MarketplaceConfig.getInstance();
    }

    public static synchronized MarketplaceService getInstance() {
        if (instance == null) {
            instance = new MarketplaceService();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;
        Kashub.debug("MarketplaceService initialized");
    }

    public CompletableFuture<List<VerifiedScript>> fetchScripts() {
        return CompletableFuture.supplyAsync(() -> {

            Kashub.debug("Marketplace: fetchScripts() called - not yet implemented");
            return Collections.emptyList();
        });
    }

    public CompletableFuture<List<VerifiedScript>> fetchScriptsByCategory(String category) {
        return fetchScripts().thenApply(scripts ->
            scripts.stream()
                .filter(s -> s.getCategory().equalsIgnoreCase(category))
                .toList()
        );
    }

    public CompletableFuture<List<VerifiedScript>> searchScripts(String query) {
        return fetchScripts().thenApply(scripts -> {
            String lowerQuery = query.toLowerCase();
            return scripts.stream()
                .filter(s -> s.getName().toLowerCase().contains(lowerQuery) ||
                            s.getDescription().toLowerCase().contains(lowerQuery))
                .toList();
        });
    }

    public CompletableFuture<String> downloadScript(String scriptId) {
        return CompletableFuture.supplyAsync(() -> {

            Kashub.debug("Marketplace: downloadScript(" + scriptId + ") called - not yet implemented");
            return null;
        });
    }

    public CompletableFuture<Boolean> installScript(VerifiedScript script) {
        return downloadScript(script.getId()).thenApply(content -> {
            if (content == null) {
                ScriptLogger.getInstance().error("Failed to download script: " + script.getName());
                return false;
            }

            Kashub.debug("Marketplace: installScript(" + script.getName() + ") called - not yet implemented");
            return false;
        });
    }

    public CompletableFuture<List<VerifiedScript>> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {

            Kashub.debug("Marketplace: checkForUpdates() called - not yet implemented");
            return Collections.emptyList();
        });
    }

    public List<String> getCategories() {

        return Arrays.asList(
            "automation",
            "utility",
            "farming",
            "building",
            "combat",
            "navigation",
            "misc"
        );
    }

    public void clearCache() {
        cachedScripts.clear();
        lastFetchTime = 0;
        Kashub.debug("Marketplace cache cleared");
    }

    public boolean isAvailable() {
        return config.isEnabled();
    }

    public String getRepositoryUrl() {
        return config.getRepositoryUrl();
    }
}

package kasperstudios.kashub.marketplace;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Async HTTP client for KasHub Marketplace API.
 * Uses OkHttp with callbacks to avoid blocking UI thread.
 */
public class MarketplaceAPI {

    private static final String BASE_URL = "https://marketplace.kashub.dev/api/v1";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();

    /**
     * Generic GET request that returns CompletableFuture.
     * Does not block UI thread - uses OkHttp async enqueue.
     */
    private static CompletableFuture<String> get(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (response) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code()));
                        return;
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        future.completeExceptionally(new IOException("Empty response body"));
                        return;
                    }

                    future.complete(body.string());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    /**
     * Get trending scripts from marketplace.
     * Returns CompletableFuture to avoid blocking UI thread.
     * 
     * Usage:
     * 
     * <pre>
     * MarketplaceAPI.getTrendingScripts()
     *         .thenAccept(scripts -> {
     *             // Update UI on main thread
     *             MinecraftClient.getInstance().execute(() -> {
     *                 // Update marketplace view
     *             });
     *         })
     *         .exceptionally(e -> {
     *             LOGGER.error("Failed to fetch trending scripts", e);
     *             return null;
     *         });
     * </pre>
     */
    public static CompletableFuture<List<MarketplaceScript>> getTrendingScripts() {
        return get(BASE_URL + "/scripts?sort=trending&limit=20")
                .thenApply(json -> {
                    MarketplaceScript[] scripts = gson.fromJson(json, MarketplaceScript[].class);
                    return Arrays.asList(scripts);
                });
    }

    /**
     * Search scripts by query.
     * Returns CompletableFuture to avoid blocking UI thread.
     */
    public static CompletableFuture<List<MarketplaceScript>> searchScripts(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return get(BASE_URL + "/scripts?q=" + encodedQuery)
                .thenApply(json -> {
                    MarketplaceScript[] scripts = gson.fromJson(json, MarketplaceScript[].class);
                    return Arrays.asList(scripts);
                });
    }

    /**
     * Download script content by ID.
     * Returns CompletableFuture with script code.
     */
    public static CompletableFuture<String> downloadScript(String scriptId) {
        return get(BASE_URL + "/scripts/" + scriptId + "/download");
    }

    /**
     * Get script details by ID.
     * Returns CompletableFuture with script metadata.
     */
    public static CompletableFuture<MarketplaceScript> getScript(String scriptId) {
        return get(BASE_URL + "/scripts/" + scriptId)
                .thenApply(json -> gson.fromJson(json, MarketplaceScript.class));
    }
}

package kasperstudios.kashub.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptLogger;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Безопасный неблокирующий HTTP-сервис
 * Все запросы выполняются в отдельном потоке
 */
public class HttpService {
    private static HttpService instance;
    
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Gson gson;
    private final ConcurrentLinkedQueue<Runnable> mainThreadCallbacks;

    private HttpService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.gson = new Gson();
        this.mainThreadCallbacks = new ConcurrentLinkedQueue<>();
    }

    public static HttpService getInstance() {
        if (instance == null) {
            instance = new HttpService();
        }
        return instance;
    }

    /**
     * Результат HTTP-запроса
     */
    public static class HttpResult {
        public final boolean success;
        public final int statusCode;
        public final String body;
        public final String error;
        public final Map<String, String> headers;

        public HttpResult(boolean success, int statusCode, String body, String error, Map<String, String> headers) {
            this.success = success;
            this.statusCode = statusCode;
            this.body = body;
            this.error = error;
            this.headers = headers;
        }

        public JsonObject getJson() {
            if (body == null || body.isEmpty()) return null;
            try {
                return JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                return null;
            }
        }

        public static HttpResult error(String error) {
            return new HttpResult(false, 0, null, error, null);
        }

        public static HttpResult success(int statusCode, String body, Map<String, String> headers) {
            return new HttpResult(true, statusCode, body, null, headers);
        }
    }

    /**
     * Проверяет, разрешён ли домен
     */
    private boolean isDomainAllowed(String url) {
        KashubConfig config = KashubConfig.getInstance();
        
        if (!config.allowHttpRequests) {
            ScriptLogger.getInstance().warn("HTTP requests are disabled in config");
            return false;
        }

        if (config.httpWhitelistedDomains == null || config.httpWhitelistedDomains.isEmpty()) {
            return true; // Если whitelist пуст, разрешаем все
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            for (String allowed : config.httpWhitelistedDomains) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            
            ScriptLogger.getInstance().warn("Domain not whitelisted: " + host);
            return false;
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Invalid URL: " + url);
            return false;
        }
    }

    /**
     * Выполняет GET-запрос асинхронно
     */
    public CompletableFuture<HttpResult> get(String url) {
        return get(url, null);
    }

    public CompletableFuture<HttpResult> get(String url, Map<String, String> headers) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();

        if (!isDomainAllowed(url)) {
            future.complete(HttpResult.error("Domain not allowed"));
            return future;
        }

        executor.submit(() -> {
            try {
                Request.Builder builder = new Request.Builder().url(url).get();
                
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        builder.addHeader(entry.getKey(), entry.getValue());
                    }
                }

                Request request = builder.build();
                
                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    Map<String, String> responseHeaders = new java.util.HashMap<>();
                    for (String name : response.headers().names()) {
                        responseHeaders.put(name, response.header(name));
                    }
                    
                    future.complete(HttpResult.success(response.code(), body, responseHeaders));
                }
            } catch (Exception e) {
                ScriptLogger.getInstance().error("HTTP GET error: " + e.getMessage());
                future.complete(HttpResult.error(e.getMessage()));
            }
        });

        return future;
    }

    /**
     * Выполняет POST-запрос асинхронно
     */
    public CompletableFuture<HttpResult> post(String url, String body) {
        return post(url, body, "application/json", null);
    }

    public CompletableFuture<HttpResult> post(String url, String body, String contentType, Map<String, String> headers) {
        CompletableFuture<HttpResult> future = new CompletableFuture<>();

        if (!isDomainAllowed(url)) {
            future.complete(HttpResult.error("Domain not allowed"));
            return future;
        }

        executor.submit(() -> {
            try {
                MediaType mediaType = MediaType.parse(contentType);
                RequestBody requestBody = RequestBody.create(body, mediaType);
                
                Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(requestBody);
                
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        builder.addHeader(entry.getKey(), entry.getValue());
                    }
                }

                Request request = builder.build();
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Map<String, String> responseHeaders = new java.util.HashMap<>();
                    for (String name : response.headers().names()) {
                        responseHeaders.put(name, response.header(name));
                    }
                    
                    future.complete(HttpResult.success(response.code(), responseBody, responseHeaders));
                }
            } catch (Exception e) {
                ScriptLogger.getInstance().error("HTTP POST error: " + e.getMessage());
                future.complete(HttpResult.error(e.getMessage()));
            }
        });

        return future;
    }

    /**
     * Выполняет POST-запрос с JSON-телом
     */
    public CompletableFuture<HttpResult> postJson(String url, Object jsonObject) {
        String body = gson.toJson(jsonObject);
        return post(url, body, "application/json", null);
    }

    /**
     * Загружает текст скрипта по URL
     */
    public CompletableFuture<String> loadScriptFromUrl(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        KashubConfig config = KashubConfig.getInstance();
        if (!config.allowRemoteScripts) {
            ScriptLogger.getInstance().warn("Remote scripts are disabled");
            future.complete(null);
            return future;
        }

        get(url).thenAccept(result -> {
            if (result.success && result.body != null) {
                // Проверяем, что это текстовый контент, а не бинарный
                if (result.body.length() > 0 && !result.body.contains("\0")) {
                    future.complete(result.body);
                } else {
                    ScriptLogger.getInstance().error("Invalid script content from URL");
                    future.complete(null);
                }
            } else {
                ScriptLogger.getInstance().error("Failed to load script from URL: " + 
                    (result.error != null ? result.error : "Unknown error"));
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * Добавляет callback для выполнения в главном потоке
     */
    public void runOnMainThread(Runnable callback) {
        mainThreadCallbacks.add(callback);
    }

    /**
     * Вызывается каждый тик для обработки callbacks
     */
    public void tick() {
        Runnable callback;
        int processed = 0;
        while ((callback = mainThreadCallbacks.poll()) != null && processed < 10) {
            try {
                callback.run();
            } catch (Exception e) {
                ScriptLogger.getInstance().error("Main thread callback error: " + e.getMessage());
            }
            processed++;
        }
    }

    /**
     * Завершает работу сервиса
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

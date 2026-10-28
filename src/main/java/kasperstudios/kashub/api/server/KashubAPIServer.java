package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class KashubAPIServer {
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_OPTIONS = "OPTIONS";
    private static final String METHOD_NOT_ALLOWED = "{\"error\":\"Method not allowed\"}";

    private static KashubAPIServer instance;
    private HttpServer server;
    private KashubWebSocketServer wsServer;
    private final ExecutorService executor;
    private final Gson gson;
    private boolean running = false;

    private KashubAPIServer() {
        this.executor = Executors.newFixedThreadPool(4);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static KashubAPIServer getInstance() {
        if (instance == null) {
            instance = new KashubAPIServer();
        }
        return instance;
    }

    public void start() {
        KashubConfig config = KashubConfig.getInstance();
        if (!config.apiEnabled) {
            Kashub.LOGGER.info("Kashub API Server is disabled in config");
            return;
        }

        if (running) {
            Kashub.LOGGER.warn("Kashub API Server is already running");
            return;
        }

        try {
            int port = config.apiPort;
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(executor);

            registerEndpoints();

            server.start();
            running = true;
            Kashub.LOGGER.info("Kashub API Server started on port {}", port);

            int wsPort = config.apiWebSocketPort;
            wsServer = new KashubWebSocketServer(wsPort);
            wsServer.start();
            Kashub.LOGGER.info("Kashub WebSocket Server started on port {}", wsPort);

        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to start Kashub API Server", e);
        }
    }

    public void stop() {
        if (!running) return;

        try {
            if (server != null) {
                server.stop(0);
            }
            if (wsServer != null) {
                wsServer.stop();
            }
            running = false;
            Kashub.LOGGER.info("Kashub API Server stopped");
        } catch (Exception e) {
            Kashub.LOGGER.error("Error stopping Kashub API Server", e);
        }
    }

    private void registerEndpoints() {

        server.createContext("/api/status", exchange -> {
            handleCors(exchange);
            if (METHOD_GET.equals(exchange.getRequestMethod())) {
                StatusEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/validate", exchange -> {
            handleCors(exchange);
            if (METHOD_POST.equals(exchange.getRequestMethod())) {
                ValidateEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/autocomplete", exchange -> {
            handleCors(exchange);
            if (METHOD_POST.equals(exchange.getRequestMethod())) {
                AutocompleteEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/run", exchange -> {
            handleCors(exchange);
            if (METHOD_POST.equals(exchange.getRequestMethod())) {
                RunEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/tasks", exchange -> {
            handleCors(exchange);
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (METHOD_OPTIONS.equals(method)) {
                sendResponse(exchange, 200, "");
                return;
            }

            if (path.equals("/api/tasks") && METHOD_GET.equals(method)) {
                TasksEndpoint.handleList(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/stop") && METHOD_POST.equals(method)) {
                TasksEndpoint.handleStop(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/pause") && METHOD_POST.equals(method)) {
                TasksEndpoint.handlePause(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/resume") && METHOD_POST.equals(method)) {
                TasksEndpoint.handleResume(exchange, gson);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        });

        server.createContext("/api/variables", exchange -> {
            handleCors(exchange);
            if (METHOD_GET.equals(exchange.getRequestMethod())) {
                VariablesEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/commands", exchange -> {
            handleCors(exchange);
            if (METHOD_GET.equals(exchange.getRequestMethod())) {
                CommandsEndpoint.handle(exchange, gson);
            } else if (METHOD_OPTIONS.equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        // Telemetry: Recent script execution logs & prints
        server.createContext("/api/logs", exchange -> {
            handleCors(exchange);
            String method = exchange.getRequestMethod();
            if (METHOD_OPTIONS.equals(method)) {
                sendResponse(exchange, 200, "");
                return;
            }
            if (METHOD_GET.equals(method)) {
                int limit = 50;
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("limit=")) {
                    try {
                        for (String p : query.split("&")) {
                            if (p.startsWith("limit=")) {
                                limit = Math.min(200, Math.max(1, Integer.parseInt(p.substring(6))));
                            }
                        }
                    } catch (Exception ignored) {}
                }
                var logs = kasperstudios.kashub.util.ScriptLogger.getInstance().getRecentLogs(limit);
                sendResponse(exchange, 200, gson.toJson(logs));
            } else {
                sendResponse(exchange, 405, METHOD_NOT_ALLOWED);
            }
        });

        server.createContext("/api/profiler", exchange -> {
            handleCors(exchange);
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (METHOD_OPTIONS.equals(method)) {
                sendResponse(exchange, 200, "");
                return;
            }

            if (path.equals("/api/profiler/status") && METHOD_GET.equals(method)) {
                ProfilerEndpoint.handleStatus(exchange, gson);
            } else if (path.equals("/api/profiler/start") && METHOD_POST.equals(method)) {
                ProfilerEndpoint.handleStart(exchange, gson);
            } else if (path.equals("/api/profiler/stop") && METHOD_POST.equals(method)) {
                ProfilerEndpoint.handleStop(exchange, gson);
            } else if (path.equals("/api/profiler/clear") && METHOD_POST.equals(method)) {
                ProfilerEndpoint.handleClear(exchange, gson);
            } else if (path.equals("/api/profiler/report") && METHOD_GET.equals(method)) {
                ProfilerEndpoint.handleReport(exchange, gson);
            } else if (path.equals("/api/profiler/json") && METHOD_GET.equals(method)) {
                ProfilerEndpoint.handleJSON(exchange, gson);
            } else if (path.equals("/api/profiler/chrome-tracing") && METHOD_GET.equals(method)) {
                ProfilerEndpoint.handleChromeTracing(exchange, gson);
            } else if (path.equals("/api/profiler/save") && METHOD_POST.equals(method)) {
                ProfilerEndpoint.handleSave(exchange, gson);

            } else if (path.equals("/api/debug/scopes") && METHOD_GET.equals(method)) {
                DebugEndpoint.handleScopes(exchange, gson);
            } else if (path.equals("/api/debug/variables") && METHOD_GET.equals(method)) {
                DebugEndpoint.handleVariables(exchange, gson);
            } else if (path.equals("/api/debug/evaluate") && METHOD_POST.equals(method)) {
                DebugEndpoint.handleEvaluate(exchange, gson);
            } else if (path.equals("/api/debug/stacktrace") && METHOD_GET.equals(method)) {
                DebugEndpoint.handleStackTrace(exchange, gson);
            } else if (path.equals("/api/debug/setVariable") && METHOD_POST.equals(method)) {
                DebugEndpoint.handleSetVariable(exchange, gson);

            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        });
    }

    private void handleCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    public static void sendResponse(HttpExchange exchange, int code, String response) {
        try {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            Kashub.LOGGER.error("Error sending response", e);
        }
    }

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public KashubWebSocketServer getWebSocketServer() {
        return wsServer;
    }

    public static void broadcast(Object event) {
        KashubAPIServer server = getInstance();
        if (server.wsServer != null) {
            server.wsServer.broadcast(server.gson.toJson(event));
        }
    }
}

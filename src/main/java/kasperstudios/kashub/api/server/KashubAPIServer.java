package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * HTTP API Server for VSCode integration.
 * Provides REST endpoints for script validation, execution, and real-time communication.
 */
public class KashubAPIServer {
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
            
            // Register endpoints
            registerEndpoints();
            
            server.start();
            running = true;
            Kashub.LOGGER.info("Kashub API Server started on port {}", port);
            
            // Start WebSocket server
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
        // Status endpoint
        server.createContext("/api/status", exchange -> {
            handleCors(exchange);
            if ("GET".equals(exchange.getRequestMethod())) {
                StatusEndpoint.handle(exchange, gson);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });
        
        // Validate endpoint
        server.createContext("/api/validate", exchange -> {
            handleCors(exchange);
            if ("POST".equals(exchange.getRequestMethod())) {
                ValidateEndpoint.handle(exchange, gson);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });
        
        // Autocomplete endpoint
        server.createContext("/api/autocomplete", exchange -> {
            handleCors(exchange);
            if ("POST".equals(exchange.getRequestMethod())) {
                AutocompleteEndpoint.handle(exchange, gson);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });
        
        // Run script endpoint
        server.createContext("/api/run", exchange -> {
            handleCors(exchange);
            if ("POST".equals(exchange.getRequestMethod())) {
                RunEndpoint.handle(exchange, gson);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });
        
        // Tasks endpoint
        server.createContext("/api/tasks", exchange -> {
            handleCors(exchange);
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            
            if ("OPTIONS".equals(method)) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (path.equals("/api/tasks") && "GET".equals(method)) {
                TasksEndpoint.handleList(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/stop") && "POST".equals(method)) {
                TasksEndpoint.handleStop(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/pause") && "POST".equals(method)) {
                TasksEndpoint.handlePause(exchange, gson);
            } else if (path.matches("/api/tasks/\\d+/resume") && "POST".equals(method)) {
                TasksEndpoint.handleResume(exchange, gson);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        });
        
        // Variables endpoint
        server.createContext("/api/variables", exchange -> {
            handleCors(exchange);
            if ("GET".equals(exchange.getRequestMethod())) {
                VariablesEndpoint.handle(exchange, gson);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
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
    
    /**
     * Broadcast message to all connected WebSocket clients
     */
    public static void broadcast(Object event) {
        KashubAPIServer server = getInstance();
        if (server.wsServer != null) {
            server.wsServer.broadcast(server.gson.toJson(event));
        }
    }
}

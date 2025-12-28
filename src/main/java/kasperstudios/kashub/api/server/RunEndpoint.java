package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.api.server.events.ScriptOutputEvent;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import net.minecraft.client.MinecraftClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * POST /api/run - Executes KHScript code
 */
public class RunEndpoint {
    
    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            Map<String, Object> request = gson.fromJson(body, Map.class);
            
            String code = (String) request.get("code");
            String filename = (String) request.getOrDefault("filename", "vscode_script");
            
            if (code == null || code.isEmpty()) {
                KashubAPIServer.sendResponse(exchange, 400, "{\"error\":\"No code provided\"}");
                return;
            }
            
            // Check if player is in world
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Player not in world");
                KashubAPIServer.sendResponse(exchange, 400, gson.toJson(response));
                return;
            }
            
            // Validate code first
            List<ValidateEndpoint.ValidationError> errors = ValidateEndpoint.validate(code);
            long errorCount = errors.stream().filter(e -> "error".equals(e.severity)).count();
            
            if (errorCount > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Script has " + errorCount + " error(s)");
                response.put("errors", errors);
                KashubAPIServer.sendResponse(exchange, 400, gson.toJson(response));
                return;
            }
            
            // Execute script on main thread
            CompletableFuture<ScriptTask> future = new CompletableFuture<>();
            
            mc.execute(() -> {
                try {
                    ScriptTask task = ScriptTaskManager.getInstance().startScript(filename, code);
                    if (task != null) {
                        future.complete(task);
                        
                        // Broadcast script started event
                        KashubAPIServer.broadcast(new ScriptOutputEvent(
                            task.getId(),
                            "Script '" + filename + "' started",
                            "info",
                            System.currentTimeMillis()
                        ));
                    } else {
                        future.completeExceptionally(new RuntimeException("Failed to start script"));
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            
            try {
                ScriptTask task = future.get(5, TimeUnit.SECONDS);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("taskId", task.getId());
                response.put("message", "Script started successfully");
                response.put("state", task.getState().toString());
                
                KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
                
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", e.getMessage());
                KashubAPIServer.sendResponse(exchange, 500, gson.toJson(response));
            }
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in run endpoint", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

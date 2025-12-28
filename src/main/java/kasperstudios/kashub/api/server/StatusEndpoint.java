package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/status - Returns mod status and player info
 */
public class StatusEndpoint {
    
    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "running");
            response.put("version", Kashub.VERSION);
            response.put("modId", Kashub.MOD_ID);
            
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean inWorld = mc.player != null;
            response.put("inWorld", inWorld);
            
            if (inWorld) {
                Map<String, Object> player = new HashMap<>();
                player.put("name", mc.player.getName().getString());
                player.put("health", mc.player.getHealth());
                player.put("maxHealth", mc.player.getMaxHealth());
                player.put("food", mc.player.getHungerManager().getFoodLevel());
                player.put("x", mc.player.getX());
                player.put("y", mc.player.getY());
                player.put("z", mc.player.getZ());
                player.put("yaw", mc.player.getYaw());
                player.put("pitch", mc.player.getPitch());
                player.put("dimension", mc.player.getWorld().getRegistryKey().getValue().toString());
                player.put("gameMode", mc.player.isCreative() ? "creative" : 
                                       mc.player.isSpectator() ? "spectator" : "survival");
                response.put("player", player);
            }
            
            // Task info
            Map<String, Object> tasks = ScriptTaskManager.getInstance().getStats();
            response.put("tasks", tasks);
            
            // WebSocket info
            KashubWebSocketServer wsServer = KashubAPIServer.getInstance().getWebSocketServer();
            if (wsServer != null) {
                response.put("wsClients", wsServer.getClientCount());
            }
            
            String json = gson.toJson(response);
            KashubAPIServer.sendResponse(exchange, 200, json);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in status endpoint", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

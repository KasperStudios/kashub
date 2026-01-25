package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import net.minecraft.client.MinecraftClient;

import java.util.*;

public class VariablesEndpoint {

    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.player == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("variables", Collections.emptyMap());
                response.put("inWorld", false);
                KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
                return;
            }

            // Use Environment instead of ScriptInterpreter
            kasperstudios.kashub.core.Environment envProvider = kasperstudios.kashub.core.Environment
                    .getInstance();

            envProvider.update();

            Map<String, kasperstudios.kashub.core.Environment.Variable> varDefs = envProvider
                    .getVariableDefinitions();

            Map<String, Object> variables = new LinkedHashMap<>();
            Map<String, List<Map<String, String>>> categorized = new LinkedHashMap<>();

            categorized.put("player", new ArrayList<>());
            categorized.put("world", new ArrayList<>());
            categorized.put("system", new ArrayList<>());

            for (kasperstudios.kashub.core.Environment.Variable var : varDefs
                    .values()) {
                String name = var.getName();
                String value = var.getValue();
                String description = var.getDescription();

                variables.put(name, value);

                Map<String, String> varInfo = new HashMap<>();
                varInfo.put("name", name);
                varInfo.put("value", value != null ? value : "");
                varInfo.put("description", description != null ? description : "");

                if (name.startsWith("PLAYER_") || name.startsWith("IS_")) {
                    categorized.get("player").add(varInfo);
                } else if (name.startsWith("WORLD_") || name.startsWith("DIMENSION") || name.startsWith("GAME_")) {
                    categorized.get("world").add(varInfo);
                } else {
                    categorized.get("system").add(varInfo);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("variables", variables);
            response.put("categorized", categorized);
            response.put("inWorld", true);
            response.put("count", variables.size());

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));

        } catch (Exception e) {
            Kashub.LOGGER.error("Error in variables endpoint", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

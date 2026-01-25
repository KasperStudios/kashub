package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.types.KHType;

import java.util.List;

public class CommandsEndpoint {
    
    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            List<Command> commands = Registry.getAllCommands();
            
            JsonObject response = new JsonObject();
            JsonArray commandsArray = new JsonArray();
            
            for (Command cmd : commands) {
                kasperstudios.kashub.core.Metadata meta = cmd.getMetadata();
                JsonObject cmdObj = new JsonObject();
                cmdObj.addProperty("name", meta.name);
                cmdObj.addProperty("description", meta.description);
                cmdObj.addProperty("parameters", meta.syntax != null ? meta.syntax : "");
                cmdObj.addProperty("category", cmd.getCategory());
                
                if (meta.examples != null && !meta.examples.isEmpty()) {
                    cmdObj.addProperty("detailedHelp", "Examples: " + String.join(", ", meta.examples));
                }
                
                commandsArray.add(cmdObj);
            }
            
            response.add("commands", commandsArray);
            response.addProperty("count", commands.size());
            
            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }
}

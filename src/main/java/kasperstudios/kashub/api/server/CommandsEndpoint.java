package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.CommandRegistry;

import java.util.List;

public class CommandsEndpoint {
    
    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            List<Command> commands = CommandRegistry.getAllCommands();
            
            JsonObject response = new JsonObject();
            JsonArray commandsArray = new JsonArray();
            
            for (Command cmd : commands) {
                JsonObject cmdObj = new JsonObject();
                cmdObj.addProperty("name", cmd.getName());
                cmdObj.addProperty("description", cmd.getDescription());
                cmdObj.addProperty("parameters", cmd.getParameters());
                cmdObj.addProperty("category", cmd.getCategory());
                
                // Add detailed help if available
                try {
                    String detailedHelp = cmd.getDetailedHelp();
                    if (detailedHelp != null && !detailedHelp.isEmpty()) {
                        cmdObj.addProperty("detailedHelp", detailedHelp);
                    }
                } catch (Exception e) {
                    // Some commands might not have detailed help
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

package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.debug.ProfilerManager;

public class ProfilerEndpoint {

    public static void handleStatus(HttpExchange exchange, Gson gson) {
        try {
            ProfilerManager profiler = ProfilerManager.getInstance();

            JsonObject response = new JsonObject();
            response.addProperty("enabled", profiler.isEnabled());
            response.addProperty("profilingDurationMs", profiler.getProfilingDurationMs());
            response.addProperty("totalExecutionTimeMs", profiler.getTotalExecutionTimeMs());
            response.addProperty("commandCount", profiler.getCommandCount());
            response.addProperty("totalExecutions", profiler.getTotalExecutions());

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleStart(HttpExchange exchange, Gson gson) {
        try {
            ProfilerManager.getInstance().start();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Profiler started");

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleStop(HttpExchange exchange, Gson gson) {
        try {
            ProfilerManager.getInstance().stop();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Profiler stopped");

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleClear(HttpExchange exchange, Gson gson) {
        try {
            ProfilerManager.getInstance().clear();

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Profiler data cleared");

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleReport(HttpExchange exchange, Gson gson) {
        try {
            String report = ProfilerManager.getInstance().generateReport();

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            KashubAPIServer.sendResponse(exchange, 200, report);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleJSON(HttpExchange exchange, Gson gson) {
        try {
            String json = ProfilerManager.getInstance().exportToJSON();
            KashubAPIServer.sendResponse(exchange, 200, json);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleChromeTracing(HttpExchange exchange, Gson gson) {
        try {
            String tracing = ProfilerManager.getInstance().exportToChromeTracing();

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=trace.json");
            KashubAPIServer.sendResponse(exchange, 200, tracing);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleSave(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            JsonObject request = gson.fromJson(body, JsonObject.class);

            String format = request.has("format") ? request.get("format").getAsString() : "txt";
            String filename = request.has("filename") ? request.get("filename").getAsString() : "profile";

            switch (format.toLowerCase()) {
                case "json":
                    ProfilerManager.getInstance().saveJSON(filename);
                    break;
                case "chrome":
                case "tracing":
                    ProfilerManager.getInstance().saveChromeTracing(filename);
                    break;
                default:
                    ProfilerManager.getInstance().saveReport(filename);
                    break;
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Profile saved to logs/kashub/profiler/");
            response.addProperty("format", format);

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }
}

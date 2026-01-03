package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.api.dto.DebugScope;
import kasperstudios.kashub.api.dto.DebugStackFrame;
import kasperstudios.kashub.api.dto.DebugVariable;
import kasperstudios.kashub.debug.DebugFrame;
import kasperstudios.kashub.debug.DebugManager;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.services.runtime.ScriptTask;
import kasperstudios.kashub.services.runtime.ScriptTaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebugEndpoint {

    private static final int SCOPE_LOCAL = 1;
    private static final int SCOPE_GLOBAL = 2;
    private static final int SCOPE_EVENT = 3;
    private static final int SCOPE_ENVIRONMENT = 4;

    public static void handleScopes(HttpExchange exchange, Gson gson) {
        try {
            String query = exchange.getRequestURI().getQuery();
            int scriptId = extractIntParam(query, "scriptId", -1);
            int frameId = extractIntParam(query, "frameId", 0);

            if (scriptId == -1) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing scriptId parameter");
                KashubAPIServer.sendResponse(exchange, 400, gson.toJson(error));
                return;
            }

            List<DebugScope> scopes = new ArrayList<>();

            ScriptTask task = ScriptTaskManager.getInstance().getTask(scriptId);
            if (task != null) {
                int localVarCount = task.getVariables().size();
                DebugScope localScope = new DebugScope("Local", SCOPE_LOCAL, false, "locals");
                localScope.namedVariables = localVarCount;
                scopes.add(localScope);
            }

            Map<String, String> allVars = DebugManager.getInstance().getVariables(scriptId);
            int eventVarCount = (int) allVars.keySet().stream()
                .filter(k -> k.startsWith("event_"))
                .count();

            if (eventVarCount > 0) {
                DebugScope eventScope = new DebugScope("Event Variables", SCOPE_EVENT, false, "locals");
                eventScope.namedVariables = eventVarCount;
                scopes.add(eventScope);
            }

            int globalVarCount = ScriptInterpreter.getInstance().getVariables().size();
            DebugScope globalScope = new DebugScope("Global", SCOPE_GLOBAL, false, "globals");
            globalScope.namedVariables = globalVarCount;
            scopes.add(globalScope);

            int envVarCount = ScriptInterpreter.getInstance().getEnvironmentVariables().size();
            DebugScope envScope = new DebugScope("Environment", SCOPE_ENVIRONMENT, true, "registers");
            envScope.namedVariables = envVarCount;
            scopes.add(envScope);

            JsonObject response = new JsonObject();
            response.add("scopes", gson.toJsonTree(scopes));

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleVariables(HttpExchange exchange, Gson gson) {
        try {
            String query = exchange.getRequestURI().getQuery();
            int variablesReference = extractIntParam(query, "variablesReference", 0);
            int scriptId = extractIntParam(query, "scriptId", -1);

            if (variablesReference == 0) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing variablesReference parameter");
                KashubAPIServer.sendResponse(exchange, 400, gson.toJson(error));
                return;
            }

            List<DebugVariable> variables = new ArrayList<>();

            switch (variablesReference) {
                case SCOPE_LOCAL:

                    if (scriptId != -1) {
                        ScriptTask task = ScriptTaskManager.getInstance().getTask(scriptId);
                        if (task != null) {
                            for (Map.Entry<String, String> entry : task.getVariables().entrySet()) {
                                String type = DebugVariable.inferType(entry.getValue());
                                variables.add(new DebugVariable(entry.getKey(), entry.getValue(), type));
                            }
                        }
                    }
                    break;

                case SCOPE_GLOBAL:

                    for (Map.Entry<String, String> entry : ScriptInterpreter.getInstance().getVariables().entrySet()) {
                        String type = DebugVariable.inferType(entry.getValue());
                        variables.add(new DebugVariable(entry.getKey(), entry.getValue(), type));
                    }
                    break;

                case SCOPE_EVENT:

                    if (scriptId != -1) {
                        Map<String, String> allVars = DebugManager.getInstance().getVariables(scriptId);
                        for (Map.Entry<String, String> entry : allVars.entrySet()) {
                            if (entry.getKey().startsWith("event_")) {
                                String type = DebugVariable.inferType(entry.getValue());
                                variables.add(new DebugVariable("$" + entry.getKey(), entry.getValue(), type));
                            }
                        }
                    }
                    break;

                case SCOPE_ENVIRONMENT:

                    for (Map.Entry<String, kasperstudios.kashub.algorithm.EnvironmentVariable> entry :
                         ScriptInterpreter.getInstance().getEnvironmentVariables().entrySet()) {
                        String value = entry.getValue().getValue();
                        String type = DebugVariable.inferType(value);
                        variables.add(new DebugVariable("$" + entry.getKey(), value, type));
                    }
                    break;

                default:
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid variablesReference: " + variablesReference);
                    KashubAPIServer.sendResponse(exchange, 400, gson.toJson(error));
                    return;
            }

            JsonObject response = new JsonObject();
            response.add("variables", gson.toJsonTree(variables));

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleEvaluate(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            JsonObject request = gson.fromJson(body, JsonObject.class);

            String expression = request.get("expression").getAsString();
            int scriptId = request.has("scriptId") ? request.get("scriptId").getAsInt() : -1;

            Map<String, String> variables = scriptId != -1
                ? DebugManager.getInstance().getVariables(scriptId)
                : ScriptInterpreter.getInstance().getVariables();

            kasperstudios.kashub.debug.ConditionEvaluator evaluator =
                kasperstudios.kashub.debug.ConditionEvaluator.getInstance();

            boolean boolResult = evaluator.evaluate(expression, variables);

            String result = String.valueOf(boolResult);
            String type = "boolean";

            JsonObject response = new JsonObject();
            response.addProperty("result", result);
            response.addProperty("type", type);
            response.addProperty("variablesReference", 0);

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleStackTrace(HttpExchange exchange, Gson gson) {
        try {
            String query = exchange.getRequestURI().getQuery();
            int scriptId = extractIntParam(query, "scriptId", -1);

            if (scriptId == -1) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing scriptId parameter");
                KashubAPIServer.sendResponse(exchange, 400, gson.toJson(error));
                return;
            }

            List<DebugStackFrame> stackFrames = new ArrayList<>();
            List<DebugFrame> callStack = DebugManager.getInstance().getCallStack(scriptId);

            ScriptTask task = ScriptTaskManager.getInstance().getTask(scriptId);
            String scriptName = task != null ? task.getName() : "unknown";

            for (int i = 0; i < callStack.size(); i++) {
                DebugFrame frame = callStack.get(i);
                DebugStackFrame stackFrame = new DebugStackFrame(
                    i,
                    frame.getFunctionName() != null ? frame.getFunctionName() : "<main>",
                    scriptName,
                    frame.getLine()
                );
                stackFrames.add(stackFrame);
            }

            if (stackFrames.isEmpty() && task != null) {
                DebugStackFrame mainFrame = new DebugStackFrame(
                    0,
                    "<main>",
                    scriptName,
                    task.getCurrentLine()
                );
                stackFrames.add(mainFrame);
            }

            JsonObject response = new JsonObject();
            response.add("stackFrames", gson.toJsonTree(stackFrames));

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    public static void handleSetVariable(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            JsonObject request = gson.fromJson(body, JsonObject.class);

            String name = request.get("name").getAsString();
            String value = request.get("value").getAsString();
            int scriptId = request.has("scriptId") ? request.get("scriptId").getAsInt() : -1;
            String scope = request.has("scope") ? request.get("scope").getAsString() : "local";

            if (scope.equals("local") && scriptId != -1) {
                ScriptTask task = ScriptTaskManager.getInstance().getTask(scriptId);
                if (task != null) {
                    task.setVariable(name, value);
                }
            } else {

                ScriptInterpreter.getInstance().setVariable(name, value);
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("value", value);
            response.addProperty("type", DebugVariable.inferType(value));

            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            KashubAPIServer.sendResponse(exchange, 500, gson.toJson(error));
        }
    }

    private static int extractIntParam(String query, String paramName, int defaultValue) {
        if (query == null) return defaultValue;

        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals(paramName)) {
                try {
                    return Integer.parseInt(kv[1]);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }
}

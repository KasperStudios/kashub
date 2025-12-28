package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * /api/tasks endpoints - Manage running script tasks
 */
public class TasksEndpoint {
    
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("/api/tasks/(\\d+)/");
    
    /**
     * GET /api/tasks - List all tasks
     */
    public static void handleList(HttpExchange exchange, Gson gson) {
        try {
            Collection<ScriptTask> tasks = ScriptTaskManager.getInstance().getAllTasks();
            
            List<Map<String, Object>> taskList = tasks.stream()
                .map(TasksEndpoint::taskToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("tasks", taskList);
            response.put("total", taskList.size());
            response.put("stats", ScriptTaskManager.getInstance().getStats());
            
            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error listing tasks", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * POST /api/tasks/{id}/stop - Stop a task
     */
    public static void handleStop(HttpExchange exchange, Gson gson) {
        try {
            int taskId = extractTaskId(exchange.getRequestURI().getPath());
            ScriptTask task = ScriptTaskManager.getInstance().getTask(taskId);
            
            if (task == null) {
                KashubAPIServer.sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
                return;
            }
            
            ScriptTaskManager.getInstance().stop(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + taskId + " stopped");
            response.put("task", taskToMap(task));
            
            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error stopping task", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * POST /api/tasks/{id}/pause - Pause a task
     */
    public static void handlePause(HttpExchange exchange, Gson gson) {
        try {
            int taskId = extractTaskId(exchange.getRequestURI().getPath());
            ScriptTask task = ScriptTaskManager.getInstance().getTask(taskId);
            
            if (task == null) {
                KashubAPIServer.sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
                return;
            }
            
            ScriptTaskManager.getInstance().pause(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + taskId + " paused");
            response.put("task", taskToMap(task));
            
            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error pausing task", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * POST /api/tasks/{id}/resume - Resume a task
     */
    public static void handleResume(HttpExchange exchange, Gson gson) {
        try {
            int taskId = extractTaskId(exchange.getRequestURI().getPath());
            ScriptTask task = ScriptTaskManager.getInstance().getTask(taskId);
            
            if (task == null) {
                KashubAPIServer.sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
                return;
            }
            
            ScriptTaskManager.getInstance().resume(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task " + taskId + " resumed");
            response.put("task", taskToMap(task));
            
            KashubAPIServer.sendResponse(exchange, 200, gson.toJson(response));
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error resuming task", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    private static int extractTaskId(String path) {
        Matcher matcher = TASK_ID_PATTERN.matcher(path);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid task ID in path: " + path);
    }
    
    private static Map<String, Object> taskToMap(ScriptTask task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getName());
        map.put("state", task.getState().toString());
        map.put("uptime", task.getUptime());
        map.put("scriptType", task.getScriptType().toString());
        
        String lastError = task.getLastError();
        if (lastError != null) {
            map.put("lastError", lastError);
        }
        
        Set<String> tags = task.getTags();
        if (tags != null && !tags.isEmpty()) {
            map.put("tags", tags);
        }
        
        return map;
    }
}

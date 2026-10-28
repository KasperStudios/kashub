package kasperstudios.kashub.command;

import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.util.ScriptManager;
import com.kasperstudios.khwrapper.ClientCommandContext;

import java.util.Collection;

public class KashubCommandLogic {

    public static int runScript(ClientCommandContext ctx, String name) {
        try {
            String content = ScriptManager.loadScript(name);
            if (content == null) {
                ctx.sendError("Script not found: " + name);
                return 0;
            }
            TaskManager.getInstance().startScript(name, content, null, kasperstudios.kashub.core.Type.USER);
            ctx.sendFeedback("Started script: " + name);
            return 1;
        } catch (Exception e) {
            ctx.sendError("Failed: " + e.getMessage());
            return 0;
        }
    }

    public static int stopAll(ClientCommandContext ctx) {
        TaskManager.getInstance().stopAll();
        ctx.sendFeedback("Stopped all scripts");
        return 1;
    }

    public static int listScripts(ClientCommandContext ctx) {
        var scripts = ScriptManager.getAllScriptsWithInfo();
        ctx.sendFeedback("=== Scripts ===");
        for (var script : scripts) {
            ctx.sendFeedback("- " + script.name + " [" + script.type + "]");
        }
        return 1;
    }

    public static int listTasks(ClientCommandContext ctx) {
        Collection<kasperstudios.kashub.core.Task> tasks = TaskManager.getInstance().getAllTasks();
        ctx.sendFeedback("=== Tasks ===");
        for (kasperstudios.kashub.core.Task task : tasks) {
            ctx.sendFeedback("#" + task.getId() + " " + task.getName() + " [" + task.getState() + "]");
        }
        return 1;
    }

    public static int pauseTask(ClientCommandContext ctx, int id) {
        kasperstudios.kashub.core.Task task = TaskManager.getInstance().getTask(id);
        if (task == null) {
            ctx.sendError("Task not found");
            return 0;
        }
        task.pause();
        ctx.sendFeedback("Paused #" + id);
        return 1;
    }

    public static int resumeTask(ClientCommandContext ctx, int id) {
        kasperstudios.kashub.core.Task task = TaskManager.getInstance().getTask(id);
        if (task == null) {
            ctx.sendError("Task not found");
            return 0;
        }
        task.resume();
        ctx.sendFeedback("Resumed #" + id);
        return 1;
    }

    public static int killTask(ClientCommandContext ctx, int id) {
        kasperstudios.kashub.core.Task task = TaskManager.getInstance().getTask(id);
        if (task == null) {
            ctx.sendError("Task not found");
            return 0;
        }
        task.stop();
        ctx.sendFeedback("Killed #" + id);
        return 1;
    }
}

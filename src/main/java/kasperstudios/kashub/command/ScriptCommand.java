package kasperstudios.kashub.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import kasperstudios.kashub.runtime.ScriptType;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.ScriptManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;

public class ScriptCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("script")
                .then(CommandManager.literal("run")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> runScript(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("stop")
                    .executes(ScriptCommand::stopAll))
                .then(CommandManager.literal("list")
                    .executes(ScriptCommand::listScripts))
                .then(CommandManager.literal("tasks")
                    .executes(ScriptCommand::listTasks))
                .then(CommandManager.literal("pause")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> pauseTask(ctx, IntegerArgumentType.getInteger(ctx, "id")))))
                .then(CommandManager.literal("resume")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> resumeTask(ctx, IntegerArgumentType.getInteger(ctx, "id")))))
                .then(CommandManager.literal("kill")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                        .executes(ctx -> killTask(ctx, IntegerArgumentType.getInteger(ctx, "id")))))
                .then(CommandManager.literal("stopall")
                    .executes(ScriptCommand::stopAll))
        );
    }

    private static int runScript(CommandContext<ServerCommandSource> ctx, String name) {
        try {
            String content = ScriptManager.loadScript(name);
            if (content == null) {
                ctx.getSource().sendError(Text.literal("Script not found: " + name));
                return 0;
            }
            ScriptTaskManager.getInstance().startScript(name, content, null, ScriptType.USER);
            ctx.getSource().sendFeedback(() -> Text.literal("Started script: " + name), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int stopAll(CommandContext<ServerCommandSource> ctx) {
        ScriptTaskManager.getInstance().stopAll();
        ctx.getSource().sendFeedback(() -> Text.literal("Stopped all scripts"), false);
        return 1;
    }

    private static int listScripts(CommandContext<ServerCommandSource> ctx) {
        var scripts = ScriptManager.getAllScriptsWithInfo();
        ctx.getSource().sendFeedback(() -> Text.literal("=== Scripts ==="), false);
        for (var script : scripts) {
            ctx.getSource().sendFeedback(() -> Text.literal("- " + script.name + " [" + script.type + "]"), false);
        }
        return 1;
    }

    private static int listTasks(CommandContext<ServerCommandSource> ctx) {
        Collection<ScriptTask> tasks = ScriptTaskManager.getInstance().getAllTasks();
        ctx.getSource().sendFeedback(() -> Text.literal("=== Tasks ==="), false);
        for (ScriptTask task : tasks) {
            ctx.getSource().sendFeedback(() -> Text.literal("#" + task.getId() + " " + task.getName() + " [" + task.getState() + "]"), false);
        }
        return 1;
    }

    private static int pauseTask(CommandContext<ServerCommandSource> ctx, int id) {
        ScriptTask task = ScriptTaskManager.getInstance().getTask(id);
        if (task == null) { ctx.getSource().sendError(Text.literal("Task not found")); return 0; }
        task.pause();
        ctx.getSource().sendFeedback(() -> Text.literal("Paused #" + id), false);
        return 1;
    }

    private static int resumeTask(CommandContext<ServerCommandSource> ctx, int id) {
        ScriptTask task = ScriptTaskManager.getInstance().getTask(id);
        if (task == null) { ctx.getSource().sendError(Text.literal("Task not found")); return 0; }
        task.resume();
        ctx.getSource().sendFeedback(() -> Text.literal("Resumed #" + id), false);
        return 1;
    }

    private static int killTask(CommandContext<ServerCommandSource> ctx, int id) {
        ScriptTask task = ScriptTaskManager.getInstance().getTask(id);
        if (task == null) { ctx.getSource().sendError(Text.literal("Task not found")); return 0; }
        task.stop();
        ctx.getSource().sendFeedback(() -> Text.literal("Killed #" + id), false);
        return 1;
    }
}

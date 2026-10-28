package kasperstudios.kashub.core;

import com.kasperstudios.khwrapper.ClientCommandContext;
import kasperstudios.kashub.command.KashubCommandLogic;

public class KashubCommandExecutor {

    public static void execute(ClientCommandContext context) {
        String[] args = context.getArguments();
        if (args.length == 0) {
            context.sendFeedback("Usage: /kashub <run|stop|list|tasks|pause|resume|kill|stopall>");
            return;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "run":
                if (args.length < 2) {
                    context.sendError("Usage: /kashub run <name>");
                } else {
                    KashubCommandLogic.runScript(context, args[1]);
                }
                break;
            case "stop":
            case "stopall":
                KashubCommandLogic.stopAll(context);
                break;
            case "list":
                KashubCommandLogic.listScripts(context);
                break;
            case "tasks":
                KashubCommandLogic.listTasks(context);
                break;
            case "pause":
                if (args.length < 2) {
                    context.sendError("Usage: /kashub pause <id>");
                } else {
                    try {
                        KashubCommandLogic.pauseTask(context, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        context.sendError("Invalid task ID: " + args[1]);
                    }
                }
                break;
            case "resume":
                if (args.length < 2) {
                    context.sendError("Usage: /kashub resume <id>");
                } else {
                    try {
                        KashubCommandLogic.resumeTask(context, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        context.sendError("Invalid task ID: " + args[1]);
                    }
                }
                break;
            case "kill":
                if (args.length < 2) {
                    context.sendError("Usage: /kashub kill <id>");
                } else {
                    try {
                        KashubCommandLogic.killTask(context, Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        context.sendError("Invalid task ID: " + args[1]);
                    }
                }
                break;
            default:
                context.sendError("Unknown subcommand: " + subCommand);
                break;
        }
    }
}

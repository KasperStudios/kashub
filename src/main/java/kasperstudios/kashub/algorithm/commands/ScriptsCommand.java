package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.runtime.ScriptState;
import kasperstudios.kashub.runtime.ScriptTask;
import kasperstudios.kashub.runtime.ScriptTaskManager;

import java.util.Collection;
import java.util.Map;

/**
 * Команды для управления запущенными скриптами через DSL
 * Синтаксис:
 *   scripts.list - список задач
 *   scripts.stop <id> - остановить задачу
 *   scripts.pause <id> - приостановить
 *   scripts.resume <id> - возобновить
 *   scripts.stopAll - остановить все
 *   scripts.stopByTag <tag> - остановить по тегу
 *   scripts.info <id> - информация о задаче
 */
public class ScriptsCommand implements Command {

    @Override
    public String getName() {
        return "scripts";
    }

    @Override
    public String getDescription() {
        return "Manage running scripts";
    }

    @Override
    public String getParameters() {
        return "<action> [args] - list/stop/pause/resume/stopAll/stopByTag/info";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        ScriptTaskManager manager = ScriptTaskManager.getInstance();
        String action = args[0].toLowerCase();

        switch (action) {
            case "list": {
                Collection<ScriptTask> tasks = manager.getTasks();
                if (tasks.isEmpty()) {
                    System.out.println("No running scripts");
                } else {
                    System.out.println("Running scripts (" + tasks.size() + "):");
                    for (ScriptTask task : tasks) {
                        System.out.println(String.format("  [%d] %s - %s (%s)", 
                            task.getId(), 
                            task.getName(), 
                            task.getState().getDisplayName(),
                            task.getUptimeFormatted()));
                    }
                }
                break;
            }

            case "stop": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts stop <id>");
                    return;
                }
                int id = Integer.parseInt(args[1]);
                manager.stop(id);
                System.out.println("Stopped task " + id);
                break;
            }

            case "pause": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts pause <id>");
                    return;
                }
                int id = Integer.parseInt(args[1]);
                manager.pause(id);
                System.out.println("Paused task " + id);
                break;
            }

            case "resume": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts resume <id>");
                    return;
                }
                int id = Integer.parseInt(args[1]);
                manager.resume(id);
                System.out.println("Resumed task " + id);
                break;
            }

            case "restart": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts restart <id>");
                    return;
                }
                int id = Integer.parseInt(args[1]);
                manager.restart(id);
                System.out.println("Restarted task " + id);
                break;
            }

            case "stopall": {
                manager.stopAll();
                System.out.println("Stopped all scripts");
                break;
            }

            case "pauseall": {
                manager.pauseAll();
                System.out.println("Paused all scripts");
                break;
            }

            case "resumeall": {
                manager.resumeAll();
                System.out.println("Resumed all scripts");
                break;
            }

            case "stopbytag": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts stopByTag <tag>");
                    return;
                }
                String tag = args[1];
                manager.stopByTag(tag);
                System.out.println("Stopped all scripts with tag: " + tag);
                break;
            }

            case "info": {
                if (args.length < 2) {
                    System.out.println("Usage: scripts info <id>");
                    return;
                }
                int id = Integer.parseInt(args[1]);
                ScriptTask task = manager.getTask(id);
                if (task == null) {
                    System.out.println("Task not found: " + id);
                } else {
                    System.out.println("Task #" + task.getId() + ": " + task.getName());
                    System.out.println("  State: " + task.getState().getDisplayName());
                    System.out.println("  Type: " + task.getScriptType().getDisplayName());
                    System.out.println("  Uptime: " + task.getUptimeFormatted());
                    System.out.println("  Line: " + task.getCurrentLine());
                    System.out.println("  Commands: " + task.getExecutedCommands() + " executed, " + 
                                       task.getQueuedCommands() + " queued");
                    System.out.println("  Tags: " + String.join(", ", task.getTags()));
                    if (task.getLastError() != null) {
                        System.out.println("  Last Error: " + task.getLastError());
                    }
                }
                break;
            }

            case "stats": {
                Map<String, Object> stats = manager.getStats();
                System.out.println("Script Manager Stats:");
                System.out.println("  Total: " + stats.get("total"));
                System.out.println("  Running: " + stats.get("running"));
                System.out.println("  Paused: " + stats.get("paused"));
                System.out.println("  Stopped: " + stats.get("stopped"));
                System.out.println("  Errors: " + stats.get("error"));
                System.out.println("  Enabled: " + stats.get("enabled"));
                break;
            }

            case "enable": {
                manager.setEnabled(true);
                System.out.println("Script execution enabled");
                break;
            }

            case "disable": {
                manager.setEnabled(false);
                System.out.println("Script execution disabled");
                break;
            }

            case "clear": {
                manager.clear();
                System.out.println("Cleared all tasks");
                break;
            }

            default:
                printHelp();
        }
    }

    private void printHelp() {
        System.out.println("Scripts management commands:");
        System.out.println("  scripts list - show all tasks");
        System.out.println("  scripts stop <id> - stop task");
        System.out.println("  scripts pause <id> - pause task");
        System.out.println("  scripts resume <id> - resume task");
        System.out.println("  scripts restart <id> - restart task");
        System.out.println("  scripts stopAll - stop all tasks");
        System.out.println("  scripts pauseAll - pause all tasks");
        System.out.println("  scripts resumeAll - resume all tasks");
        System.out.println("  scripts stopByTag <tag> - stop by tag");
        System.out.println("  scripts info <id> - task details");
        System.out.println("  scripts stats - manager statistics");
        System.out.println("  scripts enable/disable - toggle execution");
        System.out.println("  scripts clear - remove all tasks");
    }
}

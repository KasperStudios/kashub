package kasperstudios.kashub.crashguard;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.crashguard.CrashGuard.*;

import java.util.Map;

/**
 * Команда для управления CrashGuard из скриптов и чата
 * 
 * Синтаксис:
 *   crashGuard status - показать статус всех скриптов
 *   crashGuard config <strictness> - установить глобальную строгость
 *   crashGuard pause <ms> - временно отключить защиту
 *   crashGuard stop <scriptName> - остановить скрипт
 *   crashGuard cleanup - очистить ресурсы текущего скрипта
 *   crashGuard report - подробный отчёт
 */
public class CrashGuardCommand implements Command {
    
    @Override
    public String getName() {
        return "crashGuard";
    }
    
    @Override
    public String getDescription() {
        return "Управление системой защиты от крашей";
    }
    
    @Override
    public String getParameters() {
        return "status|config|pause|stop|cleanup|report [args]";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        CrashGuard guard = CrashGuard.getInstance();
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "status":
                handleStatus(guard, interpreter);
                break;
            case "config":
                handleConfig(guard, args, interpreter);
                break;
            case "pause":
                handlePause(guard, args, interpreter);
                break;
            case "stop":
                handleStop(guard, args, interpreter);
                break;
            case "cleanup":
                handleCleanup(guard, interpreter);
                break;
            case "report":
                handleReport(guard, interpreter);
                break;
            case "strictness":
                handleStrictness(guard, args, interpreter);
                break;
            default:
                printHelp();
        }
    }
    
    private void handleStatus(CrashGuard guard, ScriptInterpreter interpreter) {
        GlobalStats global = guard.getGlobalStats();
        Map<String, ScriptStats> scripts = guard.getStats();
        
        System.out.println("=== CrashGuard Status ===");
        System.out.println("Global: " + global.strictness + 
                          " | CPU: " + global.totalCpuTimeMs + "ms" +
                          " | Actions: " + global.totalActionsPerSec + "/s" +
                          " | Scripts: " + global.activeScripts);
        System.out.println("");
        
        if (scripts.isEmpty()) {
            System.out.println("No active scripts");
        } else {
            for (Map.Entry<String, ScriptStats> entry : scripts.entrySet()) {
                ScriptStats stats = entry.getValue();
                String status = stats.getStatus();
                String icon = status.equals("OK") ? "✓" : (status.equals("PAUSED") ? "⏸" : "✗");
                
                System.out.println(String.format("  %s %s: CPU=%dms Actions=%d/s %s",
                    icon, entry.getKey(), stats.cpuTimeMs, stats.actionsPerSec, status));
            }
        }
        
        // Устанавливаем переменные
        interpreter.setVariable("crashGuard_strictness", global.strictness.name());
        interpreter.setVariable("crashGuard_cpuTotal", String.valueOf(global.totalCpuTimeMs));
        interpreter.setVariable("crashGuard_actionsTotal", String.valueOf(global.totalActionsPerSec));
        interpreter.setVariable("crashGuard_scriptCount", String.valueOf(global.activeScripts));
    }
    
    private void handleConfig(CrashGuard guard, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Current config:");
            System.out.println("  Strictness: " + guard.getGlobalStrictness());
            return;
        }
        
        // Парсим key=value пары
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                switch (parts[0].toLowerCase()) {
                    case "strictness":
                        guard.setGlobalStrictness(Strictness.fromString(parts[1]));
                        System.out.println("Strictness set to: " + parts[1]);
                        break;
                }
            } else {
                // Просто strictness без =
                guard.setGlobalStrictness(Strictness.fromString(arg));
                System.out.println("Strictness set to: " + arg);
            }
        }
    }
    
    private void handleStrictness(CrashGuard guard, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Usage: crashGuard strictness <off|loose|medium|strict|paranoid> [scriptName]");
            return;
        }
        
        Strictness strictness = Strictness.fromString(args[1]);
        
        if (args.length >= 3) {
            // Per-script
            guard.setScriptStrictness(args[2], strictness);
            System.out.println("Script " + args[2] + " strictness set to: " + strictness);
        } else {
            // Global
            guard.setGlobalStrictness(strictness);
            System.out.println("Global strictness set to: " + strictness);
        }
    }
    
    private void handlePause(CrashGuard guard, String[] args, ScriptInterpreter interpreter) {
        long duration = 5000; // default 5 sec
        if (args.length >= 2) {
            try {
                duration = Long.parseLong(args[1]);
            } catch (NumberFormatException ignored) {}
        }
        
        guard.pauseGlobal(duration);
        System.out.println("CrashGuard paused for " + duration + "ms");
        interpreter.setVariable("crashGuard_paused", "true");
    }
    
    private void handleStop(CrashGuard guard, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Usage: crashGuard stop <scriptName>");
            return;
        }
        
        String scriptName = args[1];
        guard.stopScript(scriptName, "Manual stop via command");
        System.out.println("Script " + scriptName + " stopped");
    }
    
    private void handleCleanup(CrashGuard guard, ScriptInterpreter interpreter) {
        String currentScript = interpreter.getCurrentScriptName();
        if (currentScript != null) {
            guard.cleanupScript(currentScript);
            System.out.println("Resources cleaned up for: " + currentScript);
        } else {
            System.out.println("No active script to cleanup");
        }
    }
    
    private void handleReport(CrashGuard guard, ScriptInterpreter interpreter) {
        GlobalStats global = guard.getGlobalStats();
        Map<String, ScriptStats> scripts = guard.getStats();
        
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║           CrashGuard Report                  ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ Global Settings                              ║");
        System.out.println("║   Strictness: " + padRight(global.strictness.name(), 30) + "║");
        System.out.println("║   Total CPU: " + padRight(global.totalCpuTimeMs + "ms/tick", 31) + "║");
        System.out.println("║   Total Actions: " + padRight(global.totalActionsPerSec + "/sec", 26) + "║");
        System.out.println("║   Active Scripts: " + padRight(String.valueOf(global.activeScripts), 25) + "║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║ Per-Script Status                            ║");
        
        if (scripts.isEmpty()) {
            System.out.println("║   (no active scripts)                        ║");
        } else {
            for (Map.Entry<String, ScriptStats> entry : scripts.entrySet()) {
                ScriptStats stats = entry.getValue();
                String line = String.format("  %s: %s CPU=%dms Act=%d/s",
                    truncate(entry.getKey(), 15), stats.getStatus(), stats.cpuTimeMs, stats.actionsPerSec);
                System.out.println("║ " + padRight(line, 43) + "║");
            }
        }
        
        System.out.println("╚══════════════════════════════════════════════╝");
    }
    
    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
    
    private String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n - 2) + ".." : s;
    }
    
    private void printHelp() {
        System.out.println("CrashGuard Command:");
        System.out.println("  crashGuard status");
        System.out.println("    - Show status of all scripts");
        System.out.println("  crashGuard config strictness=<level>");
        System.out.println("    - Set global strictness (off/loose/medium/strict/paranoid)");
        System.out.println("  crashGuard strictness <level> [scriptName]");
        System.out.println("    - Set strictness for script or globally");
        System.out.println("  crashGuard pause <ms>");
        System.out.println("    - Temporarily disable protection");
        System.out.println("  crashGuard stop <scriptName>");
        System.out.println("    - Emergency stop script");
        System.out.println("  crashGuard cleanup");
        System.out.println("    - Clean up current script resources");
        System.out.println("  crashGuard report");
        System.out.println("    - Detailed report");
    }
}

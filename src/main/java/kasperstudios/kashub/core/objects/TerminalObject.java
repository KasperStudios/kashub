package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.core.TerminalManager;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.ExecutionOrigin;

import java.util.HashMap;
import java.util.Map;

/**
 * TerminalObject - DSL interface for the Khinux Terminal.
 */
public class TerminalObject extends Value implements Documentable {

    public TerminalObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("currentDir", Value.of((Callable) (ctx, args) -> {
            return Value.of(TerminalManager.getInstance().getCurrentDir());
        }));

        methods.put("setCurrentDir", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            TerminalManager.getInstance().setCurrentDir(args.get(0).asString());
            return Value.TRUE;
        }));

        methods.put("hasSudo", Value.of((Callable) (ctx, args) -> {
            return Value.of(TerminalManager.getInstance().hasSudo());
        }));

        methods.put("hasSu", Value.of((Callable) (ctx, args) -> {
            return Value.of(TerminalManager.getInstance().hasSu());
        }));

        methods.put("execute", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.TERMINAL && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Terminal.execute requires TERMINAL or SERVER origin.");
            }
            if (args.isEmpty()) return Value.FALSE;
            String cmd = args.get(0).asString();
            if (cmd.contains("rm -rf") || cmd.contains("format")) {
                 if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowDangerousCommands) {
                     throw new SecurityException("Access Denied: Dangerous command blocked by config.");
                 }
            }
            TerminalManager.getInstance().execute(cmd);
            return Value.TRUE;
        }));

        methods.put("get", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.NULL;
            String val = TerminalManager.getInstance().getSetting(args.get(0).asString());
            return val != null ? Value.of(val) : Value.NULL;
        }));

        methods.put("set", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 2) return Value.FALSE;
            if (!ctx.hasSudo()) {
                throw new SecurityException("Access Denied: Terminal.set requires 'sudo' privileges.");
            }
            TerminalManager.getInstance().setSetting(args.get(0).asString(), args.get(1).asString());
            return Value.TRUE;
        }));

        methods.put("getPrompt", Value.of((Callable) (ctx, args) -> {
            return Value.of(TerminalManager.getInstance().getPrompt());
        }));

        methods.put("setPrompt", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            if (!ctx.hasSudo()) {
                throw new SecurityException("Access Denied: Terminal.setPrompt requires 'sudo' privileges.");
            }
            TerminalManager.getInstance().setSetting("PROMPT", args.get(0).asString());
            return Value.TRUE;
        }));

        return methods;
    }

    @Override
    public String getObjectName() {
        return "Terminal";
    }

    @Override
    public String getObjectDescription() {
        return "DSL interface for interacting with the Khinux Terminal environment.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("currentDir()", "Returns the current working directory of the terminal.");
        docs.put("setCurrentDir(path)", "Sets the current working directory.");
        docs.put("hasSudo()", "Returns true if the terminal session has sudo privileges.");
        docs.put("hasSu()", "Returns true if the terminal session has root (su) privileges.");
        docs.put("execute(command)", "Executes a terminal command (requires TERMINAL or SERVER origin).");
        docs.put("get(key)", "Retrieves a terminal setting environment variable.");
        docs.put("set(key, value)", "Sets a terminal environment variable (requires sudo).");
        docs.put("getPrompt()", "Returns the current terminal prompt string.");
        docs.put("setPrompt(prompt)", "Sets the terminal prompt string (requires sudo).");
        return docs;
    }
}

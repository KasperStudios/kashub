package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.api.server.events.ScriptOutputEvent;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import kasperstudios.kashub.core.ExecutionOrigin;

/**
 * SystemObject - Global system utilities and I/O functions.
 * 
 * Available methods:
 * - print(message) - Output to chat and console
 * - log(message) - Output to console only (no chat)
 * - chat(message) - Send chat message
 * - wait(ms) - Sleep for milliseconds
 * - time() - Get current timestamp
 * - exit() - Stop script execution
 * - gc() - Request garbage collection
 * - memory() - Get memory usage info
 */
public class SystemObject extends Value implements Documentable {
    
    public SystemObject() {
        super(createMethods());
    }

    @Override
    public Map<String, Object> getHighlighting() {
        Map<String, Object> highlights = new HashMap<>();
        // Curated colors for System calls (e.g., specific HEX for premium feel)
        String systemColor = "#56B6C2"; // Cyan-ish
        highlights.put("print", systemColor);
        highlights.put("log", systemColor);
        highlights.put("wait", "#D19A66"); // Orange-ish for time
        highlights.put("exit", "#E06C75"); // Red-ish for exit
        highlights.put("console", "#C678DD"); // Purple
        highlights.put("openFile", systemColor);
        highlights.put("memory", "#98C379"); // Green
        return highlights;
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();
        
        // print(message) - Output to chat and console
        methods.put("print", Value.of((Callable) (ctx, args) -> {
            String msg = args.isEmpty() ? "" : args.get(0).asString();
            
            // Client-side output (Chat)
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("§7[KH]§r " + msg), false);
            }
            
            // Console output
            ScriptLogger.getInstance().info("[Script] " + msg);
            
            // API broadcast (for Variables Panel/Debugger)
            KashubAPIServer.broadcast(new ScriptOutputEvent(
                    0, // scriptId
                    msg,
                    "info",
                    System.currentTimeMillis()));
            
            return Value.NULL;
        }));
        
        // log(message) - Output to console only (no chat)
        methods.put("log", Value.of((Callable) (ctx, args) -> {
            String msg = args.isEmpty() ? "" : args.get(0).asString();
            ScriptLogger.getInstance().info("[Script] " + msg);
            return Value.NULL;
        }));
        
        // wait(ms) - Sleep for milliseconds
        methods.put("wait", Value.of((Callable) (ctx, args) -> {
            long ms = args.isEmpty() ? 0 : (long) args.get(0).asDouble();
            if (ms > 0)
                Thread.sleep(ms);
            return Value.NULL;
        }));
        
        // time() - Get current timestamp
        methods.put("time", Value.of((Callable) (ctx, args) -> {
            return Value.of((double) System.currentTimeMillis());
        }));
        
        // exit() - Stop script execution
        methods.put("exit", Value.of((Callable) (ctx, args) -> {
            ctx.shouldStop = true;
            return Value.NULL;
        }));
        
        // console(command) - Bridge to execute terminal commands from script
        methods.put("console", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.TERMINAL && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: System.console requires TERMINAL or SERVER origin.");
            }
            String command = args.isEmpty() ? "" : args.get(0).asString().trim();
            if (command.startsWith("sudo ") || command.equals("sudo") || command.startsWith("su ") || command.equals("su")) {
                throw new SecurityException("Script cannot impersonate root terminal or invoke su/sudo directly.");
            }

            // Internal privileged hook for kh reload
            if (command.equals("__internal_reload_resources")) {
                kasperstudios.kashub.util.ResourceManager.forceReload();
                return Value.NULL;
            }

            kasperstudios.kashub.core.TerminalManager.getInstance().execute(command);
            return Value.NULL;
        }));

        // open(path) - Bridge to open a file in the GUI editor
        methods.put("openFile", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String path = args.get(0).asString();
            
            // Trigger terminal-based open command
            kasperstudios.kashub.core.TerminalManager.getInstance().execute("open " + path);
            return Value.TRUE;
        }));
        
        // gc() - Request garbage collection
        methods.put("gc", Value.of((Callable) (ctx, args) -> {
            System.gc();
            ScriptLogger.getInstance().info("[Script] Garbage collection requested");
            return Value.NULL;
        }));
        
        // memory() - Get memory usage info
        methods.put("memory", Value.of((Callable) (ctx, args) -> {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            Map<String, Value> memInfo = new HashMap<>();
            memInfo.put("used", Value.of((double) (usedMemory / 1024 / 1024))); // MB
            memInfo.put("free", Value.of((double) (freeMemory / 1024 / 1024))); // MB
            memInfo.put("total", Value.of((double) (totalMemory / 1024 / 1024))); // MB
            memInfo.put("max", Value.of((double) (maxMemory / 1024 / 1024))); // MB
            
            return Value.of(memInfo);
        }));
        
        // eval(code) - Execute code string dynamically
        methods.put("eval", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.SERVER && ctx.getOrigin() != ExecutionOrigin.TERMINAL) {
                throw new SecurityException("Access Denied: System.eval requires SERVER or TERMINAL origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowEval) {
                throw new SecurityException("Access Denied: eval() is disabled in config.");
            }
            if (args.isEmpty()) return Value.NULL;
            String code = args.get(0).asString();
            List<String> lines = java.util.Arrays.asList(code.split("\\r?\\n"));
            kasperstudios.kashub.core.Interpreter.execute(lines, ctx);
            return Value.NULL;
        }));

        return methods;
    }

    @Override
    public String getObjectName() {
        return "System";
    }

    @Override
    public String getObjectDescription() {
        return "Global system utilities and I/O functions.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("print(message)", "Outputs a message to both the chat and the IDE console.");
        docs.put("log(message)", "Outputs a message only to the IDE console.");
        docs.put("wait(ms)", "Pauses the execution of the current script for the given milliseconds.");
        docs.put("time()", "Returns the current system timestamp in milliseconds.");
        docs.put("exit()", "Stops the current script execution immediately.");
        docs.put("console(command)", "Executes a command in the IDE Terminal.");
        docs.put("openFile(path)", "Opens a file in the KasHub IDE Editor.");
        docs.put("gc()", "Requests Java garbage collection.");
        docs.put("memory()", "Returns memory usage info as a map (used, free, total, max) in MB.");
        docs.put("eval(code)", "Executes KasHub script code dynamically (requires eval permission).");
        return docs;
    }
}

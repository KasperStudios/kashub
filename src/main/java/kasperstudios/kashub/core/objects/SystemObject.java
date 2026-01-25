package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.api.server.events.ScriptOutputEvent;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

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
public class SystemObject extends Value {
    
    public SystemObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();
        
        // print(message) - Output to chat and console
        methods.put("print", Value.of((Callable) (ctx, args) -> {
            String msg = args.isEmpty() ? "" : args.get(0).asString();
            
            // Client-side output (Chat)
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§7[KH]§r " + msg), false);
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
        
        return methods;
    }
}

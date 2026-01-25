package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * W2PObject - Write to Play (Macro System).
 * Allows recording and replaying player actions.
 */
public class W2PObject extends Value {

    public W2PObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("record", Value.of((Callable) (ctx, args) -> {
            String name = args.isEmpty() ? "default" : args.get(0).asString();
            ScriptLogger.getInstance().info("[W2P] Started recording: " + name);
            // TODO: Hook into input events
            return Value.TRUE;
        }));

        methods.put("stop", Value.of((Callable) (ctx, args) -> {
            ScriptLogger.getInstance().info("[W2P] Stopped recording/playback");
            // TODO: Stop hooks
            return Value.TRUE;
        }));

        methods.put("play", Value.of((Callable) (ctx, args) -> {
            String name = args.isEmpty() ? "default" : args.get(0).asString();
            ScriptLogger.getInstance().info("[W2P] Playing macro: " + name);
            // TODO: Replay events
            return Value.TRUE;
        }));

        return methods;
    }
}

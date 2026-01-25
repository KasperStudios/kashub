package kasperstudios.kashub.core;

import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Interpreter {

    public static void execute(List<String> lines, Context ctx) {
        // Inject system objects if this is a root context and not already injected
        if (ctx.getVariable("player").isNull()) {
            // Core objects
            ctx.setVariable("System", new kasperstudios.kashub.core.objects.SystemObject());
            ctx.setVariable("player", new kasperstudios.kashub.core.objects.PlayerObject());
            ctx.setVariable("scanner", new kasperstudios.kashub.core.objects.ScannerObject());
            ctx.setVariable("vision", new kasperstudios.kashub.core.objects.VisionObject());
            ctx.setVariable("inventory", new kasperstudios.kashub.core.objects.InventoryObject());
            ctx.setVariable("world", new kasperstudios.kashub.core.objects.WorldObject());
            ctx.setVariable("game", new kasperstudios.kashub.core.objects.GameObject());
            ctx.setVariable("w2p", new kasperstudios.kashub.core.objects.W2PObject());

            // Legacy global functions (kept for backward compatibility)
            ctx.setVariable("print", Value.of((Callable) (c, args) -> {
                String msg = args.isEmpty() ? "" : args.get(0).asString();
                ScriptLogger.getInstance().info("[Script] " + msg);
                return Value.NULL;
            }));

            ctx.setVariable("wait", Value.of((Callable) (c, args) -> {
                long ms = args.isEmpty() ? 0 : (long) args.get(0).asDouble();
                if (ms > 0)
                    Thread.sleep(ms);
                return Value.NULL;
            }));

            ctx.setVariable("chat", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.NULL;
                String msg = args.get(0).asString();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null)
                    client.player.networkHandler.sendChatMessage(msg);
                return Value.NULL;
            }));

            // Math Object (Global Helper)
            Map<String, Value> mathMethods = new java.util.HashMap<>();
            mathMethods.put("sqrt", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(0);
                double value = args.get(0).asDouble();
                return Value.of(Math.sqrt(value));
            }));
            mathMethods.put("abs", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(0);
                double value = args.get(0).asDouble();
                return Value.of(Math.abs(value));
            }));
            mathMethods.put("min", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.of(0);
                double a = args.get(0).asDouble();
                double b = args.get(1).asDouble();
                return Value.of(Math.min(a, b));
            }));
            mathMethods.put("max", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.of(0);
                double a = args.get(0).asDouble();
                double b = args.get(1).asDouble();
                return Value.of(Math.max(a, b));
            }));
            mathMethods.put("floor", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(0);
                double value = args.get(0).asDouble();
                return Value.of(Math.floor(value));
            }));
            mathMethods.put("ceil", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(0);
                double value = args.get(0).asDouble();
                return Value.of(Math.ceil(value));
            }));
            mathMethods.put("round", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(0);
                double value = args.get(0).asDouble();
                return Value.of((double) Math.round(value));
            }));
            mathMethods.put("random", Value.of((Callable) (c, args) -> {
                return Value.of(Math.random());
            }));
            mathMethods.put("pow", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.of(0);
                double base = args.get(0).asDouble();
                double exponent = args.get(1).asDouble();
                return Value.of(Math.pow(base, exponent));
            }));
            ctx.setVariable("Math", Value.of(mathMethods));

            // Tag Object (Global Helper)
            Map<String, Value> tagMethods = new java.util.HashMap<>();
            tagMethods.put("itemHas", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.FALSE;
                return Value.of(kasperstudios.kashub.services.integration.TagManager.getInstance()
                        .itemHasTag(args.get(0).asString(), args.get(1).asString()));
            }));
            tagMethods.put("itemsWith", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.of(java.util.Collections.emptyList());
                return Value.of(kasperstudios.kashub.services.integration.TagManager.getInstance()
                        .getItemsWithTag(args.get(0).asString()));
            }));
            tagMethods.put("blockHas", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.FALSE;
                return Value.of(kasperstudios.kashub.services.integration.TagManager.getInstance()
                        .blockHasTag(args.get(0).asString(), args.get(1).asString()));
            }));
            ctx.setVariable("tag", Value.of(tagMethods));
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("//"))
                continue;

            if (ctx.shouldBreak || ctx.shouldContinue || ctx.shouldReturn || ctx.shouldStop)
                break;

            Command cmd = Registry.findMatch(line);

            List<String> blockBody = null;
            if (line.endsWith("{")) {
                BlockResult block = extractBlock(lines, i + 1);
                blockBody = block.content;
                i = block.endIndex;
            }

            try {
                if (cmd != null) {
                    cmd.execute(ctx, line, blockBody);
                } else {
                    // Fallback: execute as expression statement
                    Parser.evaluate(line, ctx);
                }
            } catch (Exception e) {
                ScriptLogger.getInstance().error(
                        "Error executing line " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    public static void executeBlock(List<String> lines, Context ctx) {
        if (lines == null || lines.isEmpty())
            return;
        execute(lines, ctx);
    }

    public static BlockResult extractBlock(List<String> lines, int startIndex) {
        List<String> blockContent = new ArrayList<>();
        int braceDepth = 1;
        int i = startIndex;

        while (i < lines.size() && braceDepth > 0) {
            String line = lines.get(i);
            for (char c : line.toCharArray()) {
                if (c == '{')
                    braceDepth++;
                if (c == '}')
                    braceDepth--;
            }
            if (braceDepth > 0)
                blockContent.add(line);
            i++;
        }
        return new BlockResult(blockContent, i - 1);
    }

    public static class BlockResult {
        public final List<String> content;
        public final int endIndex;

        public BlockResult(List<String> content, int endIndex) {
            this.content = content;
            this.endIndex = endIndex;
        }
    }
}

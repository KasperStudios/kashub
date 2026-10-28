package kasperstudios.kashub.core;

import kasperstudios.kashub.util.ScriptLogger;
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
            ctx.setVariable("Terminal", new kasperstudios.kashub.core.objects.TerminalObject());
            ctx.setVariable("VFS", new kasperstudios.kashub.core.objects.VfsObject());

            // Global methods
            ctx.setVariable("print", Value.of((Callable) (c, args) -> {
                String msg = args.isEmpty() ? "" : args.get(0).toString();
                ScriptLogger.getInstance().info(msg);
                return Value.NULL;
            }));

            ctx.setVariable("wait", Value.of((Callable) (c, args) -> {
                if (!args.isEmpty()) {
                    try {
                        Thread.sleep((long) args.get(0).asDouble());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return Value.NULL;
            }));

            ctx.setVariable("chat", Value.of((Callable) (c, args) -> {
                if (args.isEmpty())
                    return Value.NULL;
                String msg = args.get(0).asString();
                com.kasperstudios.khwrapper.KHWrapper.get().sendChat(msg);
                return Value.NULL;
            }));

            ctx.setVariable("time", Value.of((Callable) (c, args) -> Value.of((double) System.currentTimeMillis())));

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
                if (args.isEmpty()) return Value.of(Math.random());
                double max = args.get(0).asDouble();
                return Value.of(Math.random() * max);
            }));
            
            ctx.setVariable("random", Value.of((Callable) (c, args) -> {
                if (args.isEmpty()) return Value.of(Math.random());
                double max = args.get(0).asDouble();
                return Value.of(Math.floor(Math.random() * max));
            }));
            
            mathMethods.put("pow", Value.of((Callable) (c, args) -> {
                if (args.size() < 2)
                    return Value.of(0);
                double base = args.get(0).asDouble();
                double exponent = args.get(1).asDouble();
                return Value.of(Math.pow(base, exponent));
            }));

            ctx.setVariable("Math", Value.of(mathMethods));
            ctx.setVariable("math", Value.of(mathMethods));

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

        List<String> processedLines = preprocessComments(lines);

        for (int i = 0; i < processedLines.size(); i++) {
            if (ctx.shouldStop || Thread.currentThread().isInterrupted()) break;
            
            ctx.setCurrentLine(i + 1);
            String line = processedLines.get(i).trim();
            if (line.isEmpty() || line.startsWith("//"))
                continue;

            if (ctx.shouldBreak || ctx.shouldContinue || ctx.shouldReturn || ctx.shouldStop)
                break;

            Command cmd = Registry.findMatch(line);

            List<String> blockBody = null;
            if (line.endsWith("{")) {
                BlockResult block = extractBlock(processedLines, i + 1);
                blockBody = block.content;
                i = block.endIndex - 1;
            }

            try {
                if (cmd != null) {
                    if (cmd.requiresSu() && !ctx.hasSu()) {
                        throw new SecurityException("Access Denied: Command '" + cmd.getName() + "' requires 'su' (driver/kernel) privileges.");
                    }
                    if (cmd.requiresSudo() && !ctx.hasSudo()) {
                        throw new SecurityException("Access Denied: Command '" + cmd.getName() + "' requires 'sudo' privileges.");
                    }
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
            boolean inDoubleQuote = false;
            boolean inSingleQuote = false;
            boolean inBlockComment = false;
            boolean escaped = false;

            char[] chars = line.toCharArray();
            for (int j = 0; j < chars.length; j++) {
                char c = chars[j];

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\' && !inBlockComment) {
                    escaped = true;
                    continue;
                }

                if (!inBlockComment && c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    continue;
                }
                if (!inBlockComment && c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    continue;
                }

                if (!inDoubleQuote && !inSingleQuote) {
                    if (!inBlockComment && c == '/' && j + 1 < chars.length && chars[j + 1] == '*') {
                        inBlockComment = true;
                        j++;
                        continue;
                    }

                    if (inBlockComment && c == '*' && j + 1 < chars.length && chars[j + 1] == '/') {
                        inBlockComment = false;
                        j++;
                        continue;
                    }

                    if (inBlockComment) continue;

                    if (c == '/' && j + 1 < chars.length && chars[j + 1] == '/') {
                        break;
                    }

                    if (c == '{') {
                        braceDepth++;
                    }
                    if (c == '}') {
                        braceDepth--;
                    }

                    if (braceDepth == 0) break;
                }
            }

            if (braceDepth > 0) {
                blockContent.add(line);
            }
            i++;
            if (braceDepth == 0) break;
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

    private static List<String> preprocessComments(List<String> lines) {
        List<String> result = new ArrayList<>();
        boolean inBlockComment = false;
        for (String raw : lines) {
            StringBuilder sb = new StringBuilder();
            boolean inDoubleQuote = false;
            boolean inSingleQuote = false;
            boolean escaped = false;
            char[] chars = raw.toCharArray();

            for (int j = 0; j < chars.length; j++) {
                char c = chars[j];

                if (escaped) {
                    escaped = false;
                    if (!inBlockComment) sb.append(c);
                    continue;
                }

                if (c == '\\' && !inBlockComment) {
                    escaped = true;
                    sb.append(c);
                    continue;
                }

                if (!inBlockComment && c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                    sb.append(c);
                    continue;
                }
                if (!inBlockComment && c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                    sb.append(c);
                    continue;
                }

                if (!inDoubleQuote && !inSingleQuote) {
                    if (inBlockComment) {
                        if (c == '*' && j + 1 < chars.length && chars[j + 1] == '/') {
                            inBlockComment = false;
                            j++;
                        }
                        continue;
                    }

                    if (c == '/' && j + 1 < chars.length && chars[j + 1] == '/') {
                        break;
                    }
                    if (c == '/' && j + 1 < chars.length && chars[j + 1] == '*') {
                        inBlockComment = true;
                        j++;
                        continue;
                    }

                    sb.append(c);
                } else {
                    sb.append(c);
                }
            }
            result.add(sb.toString());
        }
        return result;
    }
}

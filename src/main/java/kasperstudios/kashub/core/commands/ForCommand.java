package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * ForCommand - Handles JS-like 'for' loops:
 * 1. Classic: for (let i = 0; i < 10; i = i + 1) { ... }
 * 2. For-of / For-in: for (let item of list) { ... } or for (let item in list) { ... }
 */
public class ForCommand implements Command {
    @Override
    public String getName() {
        return "for";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^for\\s*\\((.+)\\)\\s*\\{$";
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(
                "for",
                "Flow",
                getRegex(),
                "for (let i = 0; i < N; i = i + 1) { ... }",
                "Counted iteration loop or for-of iteration: for (let item of list) { ... }.",
                List.of("loop definition"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String inside = m.group(1).trim();

            if (inside.contains(";")) {
                // Classic C-style for loop: init; condition; update
                String[] parts = inside.split(";", 3);
                if (parts.length < 3) return;
                String init = parts[0].trim();
                String condition = parts[1].trim();
                String update = parts[2].trim();

                Context loopCtx = new Context(ctx);

                if (!init.isEmpty()) {
                    Command declCmd = kasperstudios.kashub.core.Registry.findMatch(init);
                    if (declCmd != null) {
                        declCmd.execute(loopCtx, init, null);
                    } else {
                        Parser.evaluate(init, loopCtx);
                    }
                }

                while (!ctx.shouldStop && !Thread.currentThread().isInterrupted()
                        && Parser.evaluate(condition, loopCtx).asBoolean()) {
                    Context innerCtx = new Context(loopCtx);
                    Interpreter.executeBlock(blockBody, innerCtx);

                    if (innerCtx.shouldBreak)
                        break;
                    if (innerCtx.shouldContinue) {
                        // continue -> run update, then next iteration
                    }
                    if (innerCtx.shouldReturn) {
                        ctx.shouldReturn = true;
                        ctx.returnValue = innerCtx.returnValue;
                        break;
                    }

                    if (!update.isEmpty()) {
                        Command updCmd = kasperstudios.kashub.core.Registry.findMatch(update);
                        if (updCmd != null) {
                            updCmd.execute(loopCtx, update, null);
                        } else {
                            Parser.evaluate(update, loopCtx);
                        }
                    }
                }
            } else if (inside.contains(" of ") || inside.contains(" in ")) {
                // Modern for-of / for-in loop
                boolean isOf = inside.contains(" of ");
                String[] parts = inside.split(isOf ? "\\s+of\\s+" : "\\s+in\\s+", 2);
                String varDecl = parts[0].trim();
                String iterExpr = parts[1].trim();

                String varName = varDecl;
                if (varName.startsWith("let ")) varName = varName.substring(4).trim();
                else if (varName.startsWith("var ")) varName = varName.substring(4).trim();

                Value collectionVal = Parser.evaluate(iterExpr, ctx);
                List<Value> items = new ArrayList<>();
                if (collectionVal.isList()) {
                    items = collectionVal.asList();
                } else if (collectionVal.isMap()) {
                    for (Map.Entry<String, Value> e : collectionVal.asObject().entrySet()) {
                        items.add(Value.of(e.getKey()));
                    }
                }

                for (Value item : items) {
                    if (ctx.shouldStop || Thread.currentThread().isInterrupted()) break;
                    Context innerCtx = new Context(ctx);
                    innerCtx.declareVariable(varName, item);
                    Interpreter.executeBlock(blockBody, innerCtx);

                    if (innerCtx.shouldBreak) break;
                    if (innerCtx.shouldContinue) continue;
                    if (innerCtx.shouldReturn) {
                        ctx.shouldReturn = true;
                        ctx.returnValue = innerCtx.returnValue;
                        break;
                    }
                }
            }
        }
    }
}

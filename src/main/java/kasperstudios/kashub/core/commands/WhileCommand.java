package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Metadata;

import java.util.List;
import java.util.regex.Matcher;

/**
 * WhileCommand - Handles JS-like 'while' loops.
 * Usage: while (condition) { ... }
 */
public class WhileCommand implements Command {
    @Override
    public String getName() {
        return "while";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^while\\s*\\((.+)\\)\\s*\\{$";
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(
                "while",
                "Flow",
                getRegex(),
                "while (condition) { ... }",
                "Executes the enclosed block repeatedly as long as the condition evaluates to true. Checked on every iteration.",
                List.of("condition"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String condition = m.group(1);

            while (!ctx.shouldStop && !Thread.currentThread().isInterrupted() && Parser.evaluate(condition, ctx).asBoolean()) {
                Context innerCtx = new Context(ctx);
                Interpreter.executeBlock(blockBody, innerCtx);

                // Handle break/continue/return
                if (innerCtx.shouldBreak)
                    break;
                if (innerCtx.shouldContinue) {
                    // Just continue the Java loop
                    continue;
                }
                if (innerCtx.shouldReturn) {
                    ctx.shouldReturn = true;
                    ctx.returnValue = innerCtx.returnValue;
                    break;
                }
            }
        }
    }
}

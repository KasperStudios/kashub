package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Command;

import java.util.List;
import java.util.regex.Matcher;

/**
 * IfCommand - Handles JS-like 'if' blocks.
 * Usage: if (condition) { ... }
 */
public class IfCommand implements Command {
    @Override
    public String getName() {
        return "if";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^if\\s*\\((.+)\\)\\s*\\{$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String condition = m.group(1);
            boolean result = Parser.evaluate(condition, ctx).asBoolean();
            ctx.lastIfConditionResult = result;

            if (result) {
                // Create a new nested scope for the block
                Context innerCtx = new Context(ctx);
                Interpreter.executeBlock(blockBody, innerCtx);

                // Propagate flow control flags to parent
                ctx.shouldBreak = innerCtx.shouldBreak;
                ctx.shouldContinue = innerCtx.shouldContinue;
                ctx.shouldReturn = innerCtx.shouldReturn;
                ctx.returnValue = innerCtx.returnValue;
            }
        }
    }
}

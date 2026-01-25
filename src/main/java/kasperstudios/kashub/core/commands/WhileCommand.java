package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Command;

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
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String condition = m.group(1);

            while (Parser.evaluate(condition, ctx).asBoolean()) {
                // Check if task should stop
                if (ctx.shouldStop) {
                    break;
                }
                
                Context innerCtx = new Context(ctx);
                Interpreter.executeBlock(blockBody, innerCtx);

                // Handle break/continue/return
                if (innerCtx.shouldBreak)
                    break;
                if (innerCtx.shouldReturn) {
                    ctx.shouldReturn = true;
                    ctx.returnValue = innerCtx.returnValue;
                    break;
                }
                
                // Check again after block execution
                if (ctx.shouldStop) {
                    break;
                }
                // 'continue' just ends the current iteration, so we don't need to do anything
                // special here
                // as long as we reset the flags or don't propagate them.

                // We don't propagate 'shouldContinue' to parent, as it's local to this loop.
            }
        }
    }
}

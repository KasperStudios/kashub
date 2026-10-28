package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Metadata;

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
        return "^(?:\\}\\s*)?(?:else\\s+)?if\\s*\\((.+)\\)\\s*\\{\\s*$";
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(
                "if",
                "Flow",
                getRegex(),
                "if (condition) { ... }",
                "Executes the enclosed code block if the boolean condition evaluates to true. Supports else if and else chains.",
                List.of("condition"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            boolean isElseIf = line.trim().startsWith("}") || line.trim().startsWith("else");
            
            // If this is an 'else if' and a previous 'if' in the chain already matched, skip this one
            if (isElseIf && ctx.lastIfConditionResult) {
                // Keep lastIfConditionResult as true so following 'else' blocks are also skipped
                return;
            }

            String condition = m.group(1);
            boolean result = Parser.evaluate(condition, ctx).asBoolean();
            ctx.lastIfConditionResult = result;

            if (result && blockBody != null) {
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

package kasperstudios.kashub.server.script.v2.commands.flow;

import kasperstudios.kashub.server.script.v2.ScriptCommand;
import kasperstudios.kashub.server.script.v2.ScriptContext;
import kasperstudios.kashub.server.script.v2.ScriptInterpreter;
import kasperstudios.kashub.server.script.core.ExpressionParser;
import kasperstudios.kashub.server.script.objects.ScriptValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhileCommand implements ScriptCommand {
    // Pattern: while (condition) {
    private static final Pattern PATTERN = Pattern.compile("^while\\s*\\((.+)\\)\\s*\\{$");

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public Matcher getMatcher(String line) {
        return PATTERN.matcher(line);
    }

    @Override
    public kasperstudios.kashub.server.script.v2.CommandMetadata getMetadata() {
        return new kasperstudios.kashub.server.script.v2.CommandMetadata(
                "while",
                getCategory(),
                "^while\\s*\\((.+)\\)\\s*\\{$",
                "while (condition) {",
                "Execute block repeatedly while condition is true").addExample("while (x < 10) {")
                .addExample("while (true) {");
    }

    @Override
    public void execute(ScriptContext ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (!m.find())
            return;

        String conditionExpr = m.group(1);

        try {
            while (true) {
                // Evaluate condition
                ScriptValue result = ExpressionParser.evaluate(conditionExpr, ctx);
                boolean condition = result.asBoolean();

                if (!condition)
                    break;

                if (blockBody != null) {
                    ScriptContext childCtx = new ScriptContext(ctx);
                    ScriptInterpreter.executeBlock(blockBody, childCtx);

                    // Handle flow control
                    if (childCtx.shouldReturn) {
                        ctx.shouldReturn = true;
                        ctx.returnValue = childCtx.returnValue;
                        break;
                    }
                    if (childCtx.shouldBreak) {
                        childCtx.shouldBreak = false; // Consume break
                        break;
                    }
                    if (childCtx.shouldContinue) {
                        childCtx.shouldContinue = false; // Consume continue
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[KasHub Script] While loop error: " + e.getMessage());
        }
    }
}

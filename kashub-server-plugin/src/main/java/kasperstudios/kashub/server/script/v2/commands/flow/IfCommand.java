package kasperstudios.kashub.server.script.v2.commands.flow;

import kasperstudios.kashub.server.script.v2.ScriptCommand;
import kasperstudios.kashub.server.script.v2.ScriptContext;
import kasperstudios.kashub.server.script.v2.ScriptInterpreter;
import kasperstudios.kashub.server.script.core.ExpressionParser;
import kasperstudios.kashub.server.script.objects.ScriptValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfCommand implements ScriptCommand {
    // Pattern: if (condition) {
    private static final Pattern PATTERN = Pattern.compile("^if\\s*\\((.+)\\)\\s*\\{$");

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
                "if",
                getCategory(),
                "^if\\s*\\((.+)\\)\\s*\\{$",
                "if (condition) {",
                "Execute block if condition is true").addExample("if (x > 10) {")
                .addExample("if (player.name == \"Admin\") {");
    }

    @Override
    public void execute(ScriptContext ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (!m.find())
            return;

        String conditionExpr = m.group(1);

        try {
            // Evaluate condition
            ScriptValue result = ExpressionParser.evaluate(conditionExpr, ctx);
            boolean condition = result.asBoolean();

            if (condition && blockBody != null) {
                // Execute block with child context
                ScriptContext childCtx = new ScriptContext(ctx);
                ScriptInterpreter.executeBlock(blockBody, childCtx);

                // Propagate flow control flags to parent
                if (childCtx.shouldReturn) {
                    ctx.shouldReturn = true;
                    ctx.returnValue = childCtx.returnValue;
                }
                if (childCtx.shouldBreak)
                    ctx.shouldBreak = true;
                if (childCtx.shouldContinue)
                    ctx.shouldContinue = true;
            }
        } catch (Exception e) {
            System.err.println("[KasHub Script] If statement error: " + e.getMessage());
        }
    }
}

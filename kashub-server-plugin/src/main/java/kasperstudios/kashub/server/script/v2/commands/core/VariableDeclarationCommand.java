package kasperstudios.kashub.server.script.v2.commands.core;

import kasperstudios.kashub.server.script.v2.ScriptCommand;
import kasperstudios.kashub.server.script.v2.ScriptContext;
import kasperstudios.kashub.server.script.core.ExpressionParser;
import kasperstudios.kashub.server.script.objects.ScriptValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableDeclarationCommand implements ScriptCommand {
    // Pattern: var name = value or let name = value
    private static final Pattern PATTERN = Pattern.compile("^(var|let)\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$");

    @Override
    public String getCategory() {
        return "Core";
    }

    @Override
    public Matcher getMatcher(String line) {
        return PATTERN.matcher(line);
    }

    @Override
    public kasperstudios.kashub.server.script.v2.CommandMetadata getMetadata() {
        return new kasperstudios.kashub.server.script.v2.CommandMetadata(
                "var",
                getCategory(),
                "^(var|let)\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$",
                "var <name> = <value>",
                "Declares a variable in the current scope").addExample("var x = 10")
                .addExample("let name = \"Player\"");
    }

    @Override
    public void execute(ScriptContext ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (!m.find())
            return;

        String varName = m.group(2);
        String expr = m.group(3);

        try {
            ScriptValue val = ExpressionParser.evaluate(expr, ctx);
            ctx.setVariable(varName, val);
        } catch (Exception e) {
            System.err.println("[KasHub Script] Variable declaration error: " + e.getMessage());
        }
    }
}

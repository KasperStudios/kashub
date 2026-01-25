package kasperstudios.kashub.server.script.v2.commands.core;

import kasperstudios.kashub.server.script.v2.ScriptCommand;
import kasperstudios.kashub.server.script.v2.ScriptContext;
import kasperstudios.kashub.server.script.core.ExpressionParser;
import kasperstudios.kashub.server.script.objects.ScriptValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssignmentCommand implements ScriptCommand {
    // Pattern: varName = value (no var/let keyword)
    private static final Pattern PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)\\s*=\\s*(.+)$");

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
                "assign",
                getCategory(),
                "^([a-zA-Z0-9_]+)\\s*=\\s*(.+)$",
                "<var> = <value>",
                "Assigns a new value to an existing variable").addExample("x = 5")
                .addExample("name = \"New Name\"");
    }

    @Override
    public void execute(ScriptContext ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (!m.find())
            return;

        String varName = m.group(1);
        String expr = m.group(2);

        try {
            ScriptValue val = ExpressionParser.evaluate(expr, ctx);
            ctx.setVariable(varName, val);
        } catch (Exception e) {
            System.err.println("[KasHub Script] Assignment error: " + e.getMessage());
        }
    }
}

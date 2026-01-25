package kasperstudios.kashub.server.script.v2.commands.std;

import kasperstudios.kashub.server.script.v2.ScriptCommand;
import kasperstudios.kashub.server.script.v2.ScriptContext;
import kasperstudios.kashub.server.script.core.ExpressionParser;
import kasperstudios.kashub.server.script.objects.ScriptValue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrintCommand implements ScriptCommand {
    private static final Pattern PATTERN = Pattern.compile("^print\\s+(.+)$");

    @Override
    public String getCategory() {
        return "Standard";
    }

    @Override
    public Matcher getMatcher(String line) {
        return PATTERN.matcher(line);
    }

    @Override
    public kasperstudios.kashub.server.script.v2.CommandMetadata getMetadata() {
        return new kasperstudios.kashub.server.script.v2.CommandMetadata(
                "print",
                getCategory(),
                "^print\\s+(.+)$",
                "print <expression>",
                "Prints the result of an expression to the console").addExample("print \"Hello World\"")
                .addExample("print x + y");
    }

    @Override
    public void execute(ScriptContext ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (!m.find())
            return;

        String expr = m.group(1);

        // Evaluate expression using existing ExpressionParser
        try {
            ScriptValue result = ExpressionParser.evaluate(expr, ctx);
            System.out.println("[KasHub Script] " + result.toString());
        } catch (Exception e) {
            System.err.println("[KasHub Script] Print error: " + e.getMessage());
        }
    }
}

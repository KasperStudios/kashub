package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Metadata;

import java.util.List;
import java.util.regex.Matcher;

/**
 * VariableDeclarationCommand - Handles 'let' and 'var' declarations.
 * Usage: let x = 10 + 5
 */
public class VariableDeclarationCommand implements Command {
    @Override
    public String getName() {
        return "var";
    }

    @Override
    public String getCategory() {
        return "Core";
    }

    @Override
    public String getRegex() {
        return "^(let|var)\\s+([a-zA-Z0-9_]+)(?:\\s*=\\s*(.+))?$";
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(
                "let",
                "Core",
                getRegex(),
                "let name = expression",
                "Declares a variable in the current lexical scope with optional initial value.",
                List.of("name", "value"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String name = m.group(2);
            String expr = m.group(3);
            Value value = expr != null ? Parser.evaluate(expr, ctx) : Value.NULL;
            ctx.declareVariable(name, value);
        }
    }
}

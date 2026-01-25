package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Parser;

import java.util.List;
import java.util.regex.Matcher;

/**
 * AssignmentCommand - Handles variable assignment.
 * Usage: x = x + 1
 */
public class AssignmentCommand implements Command {
    @Override
    public String getName() {
        return "assignment";
    }

    @Override
    public String getCategory() {
        return "Core";
    }

    // Matches: x = 10, but NOT starting with let/var
    @Override
    public String getRegex() {
        return "^(?!(?:let|var)\\s+)([a-zA-Z0-9_.]+)\\s*=\\s*(.+)$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String name = m.group(1);
            String expr = m.group(2);
            Value value = Parser.evaluate(expr, ctx);
            ctx.setVariable(name, value);
        }
    }
}

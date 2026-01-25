package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import java.util.List;
import java.util.regex.Matcher;

public class ReturnCommand implements Command {
    @Override
    public String getName() {
        return "return";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^return(?:\\s+(.+))?$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String expr = m.group(1);
            if (expr != null && !expr.trim().isEmpty()) {
                ctx.returnValue = Parser.evaluate(expr, ctx);
            } else {
                ctx.returnValue = Value.NULL;
            }
            ctx.shouldReturn = true;
        }
    }
}

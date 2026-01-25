package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Parser;
import java.util.List;
import java.util.regex.Matcher;
import java.util.ArrayList;

/**
 * FunctionCallCommand - Handles 'name(args)'
 */
public class FunctionCallCommand implements Command {
    @Override
    public String getName() {
        return "call";
    }

    @Override
    public String getCategory() {
        return "Core";
    }

    @Override
    public String getRegex() {
        return "^([a-zA-Z0-9_]+)\\s*\\((.*)\\)$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String name = m.group(1);
            String rawArgs = m.group(2);

            Value funcVal = ctx.getVariable(name);
            if (funcVal.getValue() instanceof FunctionDeclarationCommand.Function) {
                FunctionDeclarationCommand.Function func = (FunctionDeclarationCommand.Function) funcVal.getValue();

                // Parse arguments
                List<Value> args = new ArrayList<>();
                if (!rawArgs.trim().isEmpty()) {
                    String[] parts = rawArgs.split(",");
                    for (String part : parts) {
                        args.add(Parser.evaluate(part.trim(), ctx));
                    }
                }

                func.call(ctx, args);
            }
        }
    }
}

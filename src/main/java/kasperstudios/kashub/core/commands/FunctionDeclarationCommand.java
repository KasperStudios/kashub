package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * FunctionDeclarationCommand - Handles 'fn name(args) { ... }'
 */
public class FunctionDeclarationCommand implements Command {
    @Override
    public String getName() {
        return "fn";
    }

    @Override
    public String getCategory() {
        return "Core";
    }

    @Override
    public String getRegex() {
        return "^fn\\s+([a-zA-Z0-9_]+)\\s*\\((.*)\\)\\s*\\{$";
    }

    @Override
    public Metadata getMetadata() {
        return new Metadata(
                "fn",
                "Core",
                getRegex(),
                "fn name(arg1, arg2) { ... }",
                "Declares a reusable named function with parameters and return value.",
                List.of("name", "arguments"));
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) throws Exception {
        Matcher m = getMatcher(line);
        if (m.find()) {
            String name = m.group(1);
            String rawArgs = m.group(2);
            List<String> argNames = Arrays.stream(rawArgs.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            ctx.setVariable(name, new Value(new Function(name, argNames, blockBody)));
        }
    }

    public static class Function implements Callable {
        public final String name;
        public final List<String> argNames;
        public final List<String> body;

        public Function(String name, List<String> argNames, List<String> body) {
            this.name = name;
            this.argNames = argNames;
            this.body = body;
        }

        @Override
        public Value call(Context parentCtx, List<Value> args) {
            Context innerCtx = new Context(parentCtx, true);
            for (int i = 0; i < argNames.size(); i++) {
                Value val = (i < args.size()) ? args.get(i) : Value.NULL;
                innerCtx.declareVariable(argNames.get(i), val);
            }
            Interpreter.executeBlock(body, innerCtx);
            return innerCtx.returnValue;
        }
    }
}

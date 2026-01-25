package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import java.util.List;

public class ContinueCommand implements Command {
    @Override
    public String getName() {
        return "continue";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^continue$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) {
        ctx.shouldContinue = true;
    }
}

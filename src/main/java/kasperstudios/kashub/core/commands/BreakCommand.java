package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import java.util.List;

public class BreakCommand implements Command {
    @Override
    public String getName() {
        return "break";
    }

    @Override
    public String getCategory() {
        return "Flow";
    }

    @Override
    public String getRegex() {
        return "^break$";
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) {
        ctx.shouldBreak = true;
    }
}

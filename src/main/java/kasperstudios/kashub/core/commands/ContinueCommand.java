package kasperstudios.kashub.core.commands;

import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Metadata;
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
    public Metadata getMetadata() {
        return new Metadata(
                "continue",
                "Flow",
                getRegex(),
                "continue",
                "Skips the rest of the current loop iteration and advances to the next cycle.",
                List.of());
    }

    @Override
    public void execute(Context ctx, String line, List<String> blockBody) {
        ctx.shouldContinue = true;
    }
}

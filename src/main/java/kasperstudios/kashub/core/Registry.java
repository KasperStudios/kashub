package kasperstudios.kashub.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry - Registry for commands.
 */
public class Registry {
    private static final List<Command> commands = new ArrayList<>();
    private static final Registry INSTANCE = new Registry();

    public static void initialize() {
        // Registration of core commands
        register(new kasperstudios.kashub.core.commands.AssignmentCommand());
        register(new kasperstudios.kashub.core.commands.BreakCommand());
        register(new kasperstudios.kashub.core.commands.ContinueCommand());
        register(new kasperstudios.kashub.core.commands.ElseCommand());
        register(new kasperstudios.kashub.core.commands.FunctionCallCommand());
        register(new kasperstudios.kashub.core.commands.FunctionDeclarationCommand());
        register(new kasperstudios.kashub.core.commands.IfCommand());
        register(new kasperstudios.kashub.core.commands.ReturnCommand());
        register(new kasperstudios.kashub.core.commands.VariableDeclarationCommand());
        register(new kasperstudios.kashub.core.commands.WhileCommand());
    }

    public static Registry getInstance() {
        return INSTANCE;
    }

    public static void register(Command command) {
        commands.add(command);
    }

    public static Command findMatch(String line) {
        for (Command cmd : commands) {
            if (cmd.getMatcher(line).matches()) {
                return cmd;
            }
        }
        return null;
    }

    public static List<Command> getAllCommands() {
        return new ArrayList<>(commands);
    }

    public static List<Command> getCommands() {
        return getAllCommands();
    }

    public static void clear() {
        commands.clear();
    }
}

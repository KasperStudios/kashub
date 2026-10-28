package kasperstudios.kashub.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        register(new kasperstudios.kashub.core.commands.ForCommand());
        register(new kasperstudios.kashub.core.commands.FunctionDeclarationCommand());
        register(new kasperstudios.kashub.core.commands.IfCommand());
        register(new kasperstudios.kashub.core.commands.ReturnCommand());
        register(new kasperstudios.kashub.core.commands.VariableDeclarationCommand());
        register(new kasperstudios.kashub.core.commands.WhileCommand());
        register(new kasperstudios.kashub.core.commands.CrashGuardCommand());
    }

    public static Registry getInstance() {
        return INSTANCE;
    }

    public static void register(Command command) {
        for (Command existing : commands) {
            if (existing.getClass().equals(command.getClass())) return;
        }
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

    public static Map<String, Object> getHighlightingKeywords() {
        Map<String, Object> allKeywords = new java.util.HashMap<>();
        for (Command cmd : commands) {
            allKeywords.putAll(cmd.getHighlighting());
        }
        return allKeywords;
    }

    public static void clear() {
        commands.clear();
    }
}

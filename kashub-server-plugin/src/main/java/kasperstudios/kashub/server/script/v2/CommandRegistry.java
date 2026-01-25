package kasperstudios.kashub.server.script.v2;

import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {
    private static final List<ScriptCommand> commands = new ArrayList<>();

    public static void register(ScriptCommand cmd) {
        commands.add(cmd);
    }

    /**
     * Finds a matching command for the line.
     * Returns null if no match is found (or handle fallback logic elsewhere).
     */
    public static ScriptCommand findMatch(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//")) {
            return null;
        }

        for (ScriptCommand cmd : commands) {
            if (cmd.matches(trimmed)) {
                return cmd;
            }
        }
        return null;
    }

    public static void clear() {
        commands.clear();
    }

    public static java.util.List<CommandMetadata> getAllMetadata() {
        java.util.List<CommandMetadata> meta = new java.util.ArrayList<>();
        for (ScriptCommand cmd : commands) {
            meta.add(cmd.getMetadata());
        }
        return meta;
    }
}

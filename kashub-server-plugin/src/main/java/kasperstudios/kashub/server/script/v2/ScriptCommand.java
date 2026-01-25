package kasperstudios.kashub.server.script.v2;

import java.util.List;
import java.util.regex.Matcher;

public interface ScriptCommand {
    /**
     * @return Category for organization (e.g., "Flow", "Core")
     */
    String getCategory();

    /**
     * @return Compiled pattern for matching the line
     */
    Matcher getMatcher(String line);

    /**
     * Checks if the line matches this command
     */
    default boolean matches(String line) {
        return getMatcher(line).matches();
    }

    /**
     * Executes the command
     * 
     * @param ctx       Execution context (variables, return flags)
     * @param line      The header line itself
     * @param blockBody The code block inside {}, if any (for if/while)
     */
    void execute(ScriptContext ctx, String line, List<String> blockBody);

    /**
     * @return Metadata for editor autocomplete and validation
     */
    default CommandMetadata getMetadata() {
        return new CommandMetadata(
                "unknown",
                getCategory(),
                ".*",
                "Usage unknown",
                "No description available");
    }
}

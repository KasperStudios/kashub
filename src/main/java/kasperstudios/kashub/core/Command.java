package kasperstudios.kashub.core;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Command - Regex-based command interface.
 */
public interface Command {

    /**
     * @return Unique name of the command
     */
    String getName();

    /**
     * @return Map of tokens to color IDs (or specific HEX strings) for the highlighter.
     */
    default Map<String, Object> getHighlighting() {
        return Map.of();
    }

    /**
     * @return True if this command requires system-level (sudo) privileges.
     */
    default boolean requiresSudo() {
        return false;
    }

    /**
     * @return True if this command requires kernel-level (su) privileges.
     */
    default boolean requiresSu() {
        return false;
    }

    /**
     * @return Category for help and organization
     */
    default String getCategory() {
        return "Generic";
    }

    /**
     * @return Regex pattern used to match this command string
     */
    String getRegex();

    static final java.util.Map<String, java.util.regex.Pattern> PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * @param line Input line from the script
     * @return Matcher for the current line
     */
    default Matcher getMatcher(String line) {
        String regex = getRegex();
        java.util.regex.Pattern pattern = PATTERN_CACHE.computeIfAbsent(regex, java.util.regex.Pattern::compile);
        return pattern.matcher(line.trim());
    }

    /**
     * Executes the command logic.
     * 
     * @param ctx       Execution context for variable and state management
     * @param line      The raw line that triggered this command
     * @param blockBody Nested code block if the line ends with '{'
     */
    void execute(Context ctx, String line, List<String> blockBody) throws Exception;

    /**
     * @return Help metadata for documentation and UI
     */
    default Metadata getMetadata() {
        return new Metadata(
                getName(),
                getCategory(),
                getRegex(),
                getName(),
                "No description available",
                List.of());
    }

    /**
     * @param index   Argument index (0-based)
     * @param partial Current partial text for the argument
     * @return List of completions for the argument
     */
    default List<String> getArgumentCompletions(int index, String partial) {
        return List.of();
    }

    /**
     * @param line Input line from the script
     * @param ctx  Execution context
     * @return Error message if invalid, null otherwise
     */
    default String validate(String line, Context ctx) {
        return null;
    }
}


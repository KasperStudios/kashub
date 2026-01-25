package kasperstudios.kashub.gui;

import kasperstudios.kashub.core.*;
import java.util.*;

public class CodeCompletionManager {
    private static final Map<String, String> commandParameters = new HashMap<>();
    private static final Map<String, String> builtInParameters = new HashMap<>();
    private static final Map<String, String> snippets = new HashMap<>();
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "function", "return", "break", "continue"));

    private static final Map<String, String> snippetDescriptions = new HashMap<String, String>() {
        {
            put("if", "Conditional statement");
            put("ifelse", "Conditional with alternative");
            put("while", "Loop with condition");
            put("for", "Loop with counter");
            put("function", "Function declaration");
            put("loop", "Infinite loop");
            put("loop_break", "Loop with break");
            put("crashguard", "Protected code block");
            put("crashguard_timeout", "Protected block with timeout");
            put("crashguard_fps", "Protected block with FPS monitoring");
        }
    };

    private static final Map<String, String> environmentVariables = new HashMap<>();

    private static final Set<String> userVariables = new HashSet<>();

    private static final Map<String, List<String>> commandArguments = new HashMap<>();
    private static final Map<String, String> argumentDescriptions = new HashMap<>();

    private static final Map<String, List<String>> objectMembers = new HashMap<>();
    private static final Set<String> objectDisplayNames = new HashSet<>();

    static {
        initializeCommandParameters();
        initializeBuiltInParameters();
        initializeSnippets();
        initializeEnvironmentVariables();
        initializeCommandArguments();
        initializeV2Completions();
    }

    private static void initializeV2Completions() {
        refreshCompletions();
    }

    public static void refreshCompletions() {
        try {
            objectMembers.clear();
            objectDisplayNames.clear();

            // Create temporary context to discover all registered objects
            kasperstudios.kashub.core.Context tempCtx = new kasperstudios.kashub.core.Context();
            kasperstudios.kashub.core.Interpreter.execute(Collections.emptyList(), tempCtx);

            // Expected objects + fallback for safety
            String[] possibleObjects = { "System", "player", "scanner", "vision", "inventory", "world", "game", "w2p",
                    "Math", "tag" };

            for (String objName : possibleObjects) {
                Value val = tempCtx.getVariable(objName);
                if (val != null && !val.isNull() && val.isObject()) {
                    registerObjectMembers(objName, val);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to initialize V2 completions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerObjectMembers(String name, Value obj) {
        if (obj.asObject() == null)
            return;
        List<String> members = new ArrayList<>(obj.asObject().keySet());
        Collections.sort(members);
        // Always store with lowercase key for case-insensitive method lookup
        objectMembers.put(name.toLowerCase(), members);

        // Store original name for display in autocomplete list
        objectDisplayNames.add(name);
    }

    public static List<String> getMemberCompletions(String objectName, String partialMember) {
        List<String> result = new ArrayList<>();
        List<String> members = objectMembers.get(objectName.toLowerCase());

        if (members != null) {
            for (String member : members) {
                if (partialMember.isEmpty() || member.toLowerCase().startsWith(partialMember.toLowerCase())) {
                    result.add(member);
                }
            }
        }
        return result;
    }

    public static boolean hasObjectSuggestions(String objectName) {
        return objectMembers.containsKey(objectName.toLowerCase());
    }

    private static void initializeCommandParameters() {
        for (Command command : Registry.getCommands()) {
            String name = command.getName().toLowerCase();
            String params = command.getMetadata().syntax;
            commandParameters.put(name, params);
        }
    }

    private static void initializeBuiltInParameters() {
        builtInParameters.put("if", "<condition> { <commands> }");
        builtInParameters.put("else", "{ <commands> }");
        builtInParameters.put("while", "<condition> { <commands> }");
        builtInParameters.put("for", "<start> <end> <step> { <commands> }");
        builtInParameters.put("fn", "<name> { <commands> }");
        builtInParameters.put("return", "<value>");
        builtInParameters.put("break", "");
        builtInParameters.put("continue", "");
    }

    private static void initializeSnippets() {
        snippets.put("if", "if ($condition) {\n    $cursor\n}");
        snippets.put("ifelse", "if ($condition) {\n    $cursor\n} else {\n    \n}");
        snippets.put("while", "while ($condition) {\n    $cursor\n}");
        snippets.put("for", "for ($i = $start; $i < $end; $i += $step) {\n    $cursor\n}");
        snippets.put("fn", "fn $name() {\n    $cursor\n}");
        snippets.put("loop", "while (true) {\n    $cursor\n}");
        snippets.put("loop_break", "while (true) {\n    if ($condition) {\n        break\n    }\n    $cursor\n}");
        snippets.put("crashguard", "crashguard {\n    $cursor\n}");
        snippets.put("crashguard_timeout", "crashguard(timeout=$ms) {\n    $cursor\n}");
        snippets.put("crashguard_fps", "crashguard(minFps=$fps) {\n    $cursor\n}");
    }

    private static void initializeEnvironmentVariables() {
        updateEnvironmentVariables();
    }

    private static void initializeCommandArguments() {
        // Core command arguments are now handled by the commands themselves via
        // getArgumentCompletions
    }

    public static void updateEnvironmentVariables() {
        environmentVariables.clear();
        Environment provider = Environment
                .getInstance();

        for (Map.Entry<String, Environment.Variable> entry : provider
                .getVariableDefinitions().entrySet()) {
            String varName = "$" + entry.getKey();
            environmentVariables.put(varName, entry.getValue().getDescription());
        }
    }

    public static void addUserVariable(String name) {
        userVariables.add(name);
    }

    public static void clearUserVariables() {
        userVariables.clear();
    }

    public static List<String> getArgumentCompletions(String commandName, String partialArg) {
        List<String> result = new ArrayList<>();
        String cmdLower = commandName.toLowerCase();

        // Check Registry for dynamic completions
        for (Command cmd : Registry.getCommands()) {
            if (cmd.getName().equalsIgnoreCase(cmdLower)) {
                // For now, we only support the first argument group completion
                // ModernTextArea splits by spaces to determine the index in the future
                result.addAll(cmd.getArgumentCompletions(0, partialArg));
                break;
            }
        }

        // Fallback to server commands
        for (ServerCommandMetadata meta : serverCommands) {
            if (meta.name.equalsIgnoreCase(cmdLower)) {
                // Server commands don't have argument completions yet
                break;
            }
        }

        return result;
    }

    public static String getArgumentDescription(String command, String argument) {
        String key = command.toLowerCase() + ":" + argument;
        return argumentDescriptions.getOrDefault(key, "");
    }

    public static boolean hasArgumentSuggestions(String command) {
        String cmdLower = command.toLowerCase();
        if (commandArguments.containsKey(cmdLower))
            return true;

        for (Command cmd : Registry.getCommands()) {
            if (cmd.getName().equalsIgnoreCase(cmdLower))
                return true;
        }

        return false;
    }

    private static final List<ServerCommandMetadata> serverCommands = new ArrayList<>();

    public static List<ServerCommandMetadata> getServerCommandMetadata() {
        return serverCommands;
    }

    public static void setServerCommands(List<ServerCommandMetadata> commands) {
        serverCommands.clear();
        serverCommands.addAll(commands);
    }

    public static String getCommandDescription(String command) {
        if (command.startsWith("$")) {
            String description = environmentVariables.get(command.toUpperCase());
            if (description != null) {
                return description;
            }
            if (userVariables.contains(command.substring(1))) {
                return "User variable";
            }
            return "";
        }

        for (ServerCommandMetadata meta : serverCommands) {
            if (meta.name.equalsIgnoreCase(command)) {
                return meta.description + "\nSyntax: " + meta.syntax;
            }
        }

        Command cmd = null;
        for (Command sc : Registry.getCommands()) {
            if (sc.getName().equalsIgnoreCase(command)) {
                cmd = sc;
                break;
            }
        }

        if (cmd != null) {
            return cmd.getMetadata().description + "\nParams: " + cmd.getMetadata().syntax;
        }

        if (snippets.containsKey(command)) {
            return snippetDescriptions.getOrDefault(command, "");
        }

        return "";
    }

    public static String getCommandParameters(String command) {
        command = command.toLowerCase();

        for (ServerCommandMetadata meta : serverCommands) {
            if (meta.name.equalsIgnoreCase(command)) {
                return meta.syntax;
            }
        }

        if (builtInParameters.containsKey(command)) {
            return builtInParameters.get(command);
        }
        return commandParameters.getOrDefault(command, "");
    }

    public static List<String> getCompletions(String partialWord) {
        List<String> result = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword.toLowerCase().startsWith(partialWord.toLowerCase())) {
                result.add(keyword);
            }
        }

        if (partialWord.startsWith("$")) {
            String searchWord = partialWord.substring(1).toUpperCase();
            for (String var : environmentVariables.keySet()) {
                if (var.substring(1).startsWith(searchWord)) {
                    result.add(var);
                }
            }

            for (String var : userVariables) {
                String varWithPrefix = "$" + var;
                if (varWithPrefix.toLowerCase().startsWith(partialWord.toLowerCase())) {
                    result.add(varWithPrefix);
                }
            }
            return result;
        }

        for (ServerCommandMetadata meta : serverCommands) {
            if (meta.name.toLowerCase().startsWith(partialWord.toLowerCase())) {
                if (!result.contains(meta.name)) {
                    result.add(meta.name);
                }
            }
        }

        for (Command command : Registry.getCommands()) {
            String cmdName = command.getName().toLowerCase();
            if (cmdName.startsWith(partialWord.toLowerCase())) {
                if (!result.contains(cmdName)) {
                    result.add(cmdName);
                }
            }
        }

        for (String snippet : snippets.keySet()) {
            if (snippet.toLowerCase().startsWith(partialWord.toLowerCase())) {
                if (!result.contains(snippet)) {
                    result.add(snippet);
                }
            }
        }

        // Add object names to completions (using display names for correct casing)
        for (String objName : objectDisplayNames) {
            if (objName.toLowerCase().startsWith(partialWord.toLowerCase())) {
                if (!result.contains(objName)) {
                    result.add(objName);
                }
            }
        }

        return result;
    }

    public static String getSnippet(String name) {
        return snippets.getOrDefault(name.toLowerCase(), "");
    }

}

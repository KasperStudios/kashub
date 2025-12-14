package kasperstudios.kashub.gui;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.CommandRegistry;
import java.util.*;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.algorithm.EnvironmentVariable;

public class CodeCompletionManager {
    private static final Map<String, String> commandParameters = new HashMap<>();
    private static final Map<String, String> builtInParameters = new HashMap<>();
    private static final Map<String, String> snippets = new HashMap<>();
    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "function", "return", "break", "continue"
    ));

    private static final Map<String, String> snippetDescriptions = new HashMap<String, String>() {{
        put("if", "Conditional statement");
        put("ifelse", "Conditional with alternative");
        put("while", "Loop with condition");
        put("for", "Loop with counter");
        put("function", "Function declaration");
        put("loop", "Infinite loop");
        put("loop_break", "Loop with break");
    }};

    private static final Map<String, String> environmentVariables = new HashMap<>();
    
    private static final List<String> completions = new ArrayList<>();
    private static final Map<String, String> descriptions = new HashMap<>();

    private static final Set<String> userVariables = new HashSet<>();
    
    // Command arguments (command -> list of possible argument values)
    private static final Map<String, List<String>> commandArguments = new HashMap<>();
    private static final Map<String, String> argumentDescriptions = new HashMap<>();

    static {
        initializeCommandParameters();
        initializeBuiltInParameters();
        initializeSnippets();
        initializeEnvironmentVariables();
        initializeCommandArguments();
    }

    private static void initializeCommandParameters() {
        for (Command command : CommandRegistry.getAllCommands()) {
            String name = command.getName().toLowerCase();
            String params = command.getParameters();
            commandParameters.put(name, params);
        }
    }

    private static void initializeBuiltInParameters() {
        builtInParameters.put("if", "<condition> { <commands> }");
        builtInParameters.put("else", "{ <commands> }");
        builtInParameters.put("while", "<condition> { <commands> }");
        builtInParameters.put("for", "<start> <end> <step> { <commands> }");
        builtInParameters.put("function", "<name> { <commands> }");
        builtInParameters.put("return", "<value>");
        builtInParameters.put("break", "");
        builtInParameters.put("continue", "");
    }

    private static void initializeSnippets() {
        snippets.put("if", "if ($condition) {\n    $cursor\n}");
        snippets.put("ifelse", "if ($condition) {\n    $cursor\n} else {\n    \n}");
        snippets.put("while", "while ($condition) {\n    $cursor\n}");
        snippets.put("for", "for ($i = $start; $i < $end; $i += $step) {\n    $cursor\n}");
        snippets.put("function", "function $name() {\n    $cursor\n}");
        snippets.put("loop", "while (true) {\n    $cursor\n}");
        snippets.put("loop_break", "while (true) {\n    if ($condition) {\n        break\n    }\n    $cursor\n}");
    }

    private static void initializeEnvironmentVariables() {
        updateEnvironmentVariables();
    }
    
    private static void initializeCommandArguments() {
        // Movement commands
        commandArguments.put("moveto", Arrays.asList("~ ~ ~", "0 64 0", "$PLAYER_X $PLAYER_Y $PLAYER_Z"));
        argumentDescriptions.put("moveto:~ ~ ~", "Relative coordinates");
        argumentDescriptions.put("moveto:0 64 0", "Absolute coordinates");
        
        commandArguments.put("pathfind", Arrays.asList("~ ~ ~", "0 64 0", "home", "spawn"));
        argumentDescriptions.put("pathfind:home", "Home waypoint");
        argumentDescriptions.put("pathfind:spawn", "Spawn point");
        
        commandArguments.put("runto", Arrays.asList("~ ~ ~", "0 64 0"));
        
        commandArguments.put("lookat", Arrays.asList("~ ~ ~", "0 64 0", "entity", "block"));
        argumentDescriptions.put("lookat:entity", "Look at nearest entity");
        argumentDescriptions.put("lookat:block", "Look at target block");
        
        // Action commands
        commandArguments.put("wait", Arrays.asList("100", "500", "1000", "2000", "5000"));
        argumentDescriptions.put("wait:100", "100ms (0.1 sec)");
        argumentDescriptions.put("wait:500", "500ms (0.5 sec)");
        argumentDescriptions.put("wait:1000", "1 second");
        argumentDescriptions.put("wait:2000", "2 seconds");
        argumentDescriptions.put("wait:5000", "5 seconds");
        
        commandArguments.put("loop", Arrays.asList("5", "10", "100", "infinite"));
        argumentDescriptions.put("loop:infinite", "Infinite loop");
        
        commandArguments.put("selectslot", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8"));
        argumentDescriptions.put("selectslot:0", "First hotbar slot");
        argumentDescriptions.put("selectslot:8", "Last hotbar slot");
        
        // Chat/print
        commandArguments.put("print", Arrays.asList("\"Hello World!\"", "$PLAYER_NAME", "$PLAYER_HEALTH"));
        commandArguments.put("chat", Arrays.asList("/help", "/gamemode creative", "/tp ~ ~ ~"));
        
        // Inventory
        commandArguments.put("inventory", Arrays.asList("open", "close", "drop", "swap", "move"));
        argumentDescriptions.put("inventory:open", "Open inventory");
        argumentDescriptions.put("inventory:close", "Close inventory");
        argumentDescriptions.put("inventory:drop", "Drop item");
        
        // Interaction
        commandArguments.put("attack", Arrays.asList("once", "hold", "release"));
        argumentDescriptions.put("attack:once", "Single attack");
        argumentDescriptions.put("attack:hold", "Hold attack");
        argumentDescriptions.put("attack:release", "Release attack");
        
        commandArguments.put("useitem", Arrays.asList("once", "hold", "release"));
        commandArguments.put("interact", Arrays.asList("block", "entity"));
        
        // Movement modifiers
        commandArguments.put("sprint", Arrays.asList("on", "off", "toggle"));
        commandArguments.put("sneak", Arrays.asList("on", "off", "toggle"));
        commandArguments.put("jump", Arrays.asList("once", "hold", "release"));
        commandArguments.put("swim", Arrays.asList("on", "off"));
        
        // Block commands
        commandArguments.put("breakblock", Arrays.asList("~ ~ ~", "0 64 0"));
        commandArguments.put("placeblock", Arrays.asList("~ ~ ~", "0 64 0"));
        commandArguments.put("getblock", Arrays.asList("~ ~ ~", "0 64 0"));
        
        // Scanner
        commandArguments.put("scan", Arrays.asList("diamond_ore", "iron_ore", "gold_ore", "chest", "spawner"));
        argumentDescriptions.put("scan:diamond_ore", "Find diamond ore");
        argumentDescriptions.put("scan:chest", "Find chests");
        argumentDescriptions.put("scan:spawner", "Find spawners");
        
        commandArguments.put("scanner", Arrays.asList("start", "stop", "radius"));
        
        // Script control
        commandArguments.put("stop", Arrays.asList("all", "current"));
        argumentDescriptions.put("stop:all", "Stop all scripts");
        argumentDescriptions.put("stop:current", "Stop current script");
        
        // Vision/rendering
        commandArguments.put("fullbright", Arrays.asList("on", "off", "toggle"));
        commandArguments.put("vision", Arrays.asList("normal", "night", "xray"));
        
        // Cheats
        commandArguments.put("teleport", Arrays.asList("~ ~ ~", "0 64 0", "spawn", "home"));
        commandArguments.put("sethealth", Arrays.asList("20", "10", "1"));
        commandArguments.put("speedhack", Arrays.asList("1.0", "1.5", "2.0", "3.0", "off"));
        
        // Events
        commandArguments.put("onevent", Arrays.asList("tick", "chat", "damage", "death", "respawn"));
        argumentDescriptions.put("onevent:tick", "Every game tick");
        argumentDescriptions.put("onevent:chat", "On chat message");
        argumentDescriptions.put("onevent:damage", "On damage taken");
        argumentDescriptions.put("onevent:death", "On death");
        
        // AI/HTTP
        commandArguments.put("ai", Arrays.asList("ask", "generate", "analyze"));
        commandArguments.put("http", Arrays.asList("get", "post", "put", "delete"));
        
        // Eat/Drop
        commandArguments.put("eat", Arrays.asList("auto", "once"));
        commandArguments.put("dropitem", Arrays.asList("all", "one", "stack"));
        
        // Armor
        commandArguments.put("equiparmor", Arrays.asList("auto", "best", "slot"));
    }

    public static void updateEnvironmentVariables() {
        environmentVariables.clear();
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        for (Map.Entry<String, EnvironmentVariable> entry : interpreter.getEnvironmentVariables().entrySet()) {
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
    
    /**
     * Get argument completions for a specific command
     */
    public static List<String> getArgumentCompletions(String command, String partialArg) {
        List<String> result = new ArrayList<>();
        String cmdLower = command.toLowerCase();
        
        if (!commandArguments.containsKey(cmdLower)) {
            return result;
        }
        
        for (String arg : commandArguments.get(cmdLower)) {
            if (partialArg.isEmpty() || arg.toLowerCase().startsWith(partialArg.toLowerCase())) {
                result.add(arg);
            }
        }
        
        return result;
    }
    
    /**
     * Get description for a command argument
     */
    public static String getArgumentDescription(String command, String argument) {
        String key = command.toLowerCase() + ":" + argument;
        return argumentDescriptions.getOrDefault(key, "");
    }
    
    /**
     * Check if command has argument suggestions
     */
    public static boolean hasArgumentSuggestions(String command) {
        return commandArguments.containsKey(command.toLowerCase());
    }

    public static List<String> getCompletions(String partialWord) {
        List<String> result = new ArrayList<>();
        
        // Add keywords
        for (String keyword : keywords) {
            if (keyword.toLowerCase().startsWith(partialWord.toLowerCase())) {
                result.add(keyword);
            }
        }
        
        // If word starts with $, search environment and user variables
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
        
        // Add commands
        for (Command command : CommandRegistry.getAllCommands()) {
            String cmdName = command.getName().toLowerCase();
            if (cmdName.startsWith(partialWord.toLowerCase())) {
                result.add(cmdName);
            }
        }
        
        // Add snippets
        for (String snippet : snippets.keySet()) {
            if (snippet.toLowerCase().startsWith(partialWord.toLowerCase())) {
                result.add(snippet);
            }
        }
        
        return result;
    }

    public static String getCommandParameters(String command) {
        command = command.toLowerCase();
        if (builtInParameters.containsKey(command)) {
            return builtInParameters.get(command);
        }
        return commandParameters.getOrDefault(command, "");
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
        
        Command cmd = CommandRegistry.getCommand(command.toLowerCase());
        if (cmd != null) {
            return cmd.getDescription() + "\nParams: " + cmd.getParameters();
        }
        
        if (snippets.containsKey(command)) {
            return snippetDescriptions.getOrDefault(command, "");
        }
        
        return "";
    }

    public static String getSnippet(String name) {
        return snippets.getOrDefault(name.toLowerCase(), "");
    }

    private void initializeCompletions() {
        completions.clear();
        descriptions.clear();
        
        for (Command cmd : CommandRegistry.getCommands()) {
            completions.add(cmd.getName());
        }
        
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        for (Map.Entry<String, EnvironmentVariable> entry : interpreter.getEnvironmentVariables().entrySet()) {
            String varName = "$" + entry.getKey();
            String description = entry.getValue().getDescription();
            completions.add(varName);
            descriptions.put(varName, description);
        }
    }
}

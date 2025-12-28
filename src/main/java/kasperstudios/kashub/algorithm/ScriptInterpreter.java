package kasperstudios.kashub.algorithm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import kasperstudios.kashub.gui.CodeCompletionManager;
import kasperstudios.kashub.crashguard.CrashGuard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * KHScript interpreter.
 * Supports asynchronous command execution via queue and environment variables.
 */
public class ScriptInterpreter {
private static final Logger LOGGER = LogManager.getLogger(ScriptInterpreter.class);

    // Singleton instance for global access
private static ScriptInterpreter instance;

    private boolean isProcessing = false;
    private boolean shouldStop = false;
    private final Queue<CommandEntry> commandQueue = new LinkedList<>();
    private final VariableStore variableStore = new VariableStore();
    private final Map<String, String> variables = new HashMap<>(); // Legacy compatibility
    private final Map<String, Function> functions = new HashMap<>();
    private String currentScriptName = "unknown";
    private int currentLoopDepth = 0;
    private final Map<String, EnvironmentVariable> environmentVariables = new HashMap<>();
    
    // Return value support for functions
    private String returnValue = null;
    private boolean hasReturned = false;
    
    // Patterns for parsing - updated for Rust/JS style syntax
    private static final Pattern LET_PATTERN = Pattern.compile("^\\s*let\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern CONST_PATTERN = Pattern.compile("^\\s*const\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern IF_PATTERN = Pattern.compile("^\\s*if\\s+(.+?)\\s*\\{\\s*$|^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern WHILE_PATTERN = Pattern.compile("^\\s*while\\s+(.+?)\\s*\\{\\s*$|^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^\\s*for\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*(?:fn|function)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*\\{?\\s*$");
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*$");
    private static final Pattern RETURN_PATTERN = Pattern.compile("^\\s*return(?:\\s+(.*))?$");
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$([A-Z_][A-Z0-9_]*)");
    private static final Pattern USER_VAR_PATTERN = Pattern.compile("\\$([a-z_][a-z0-9_]*)");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s*\\{?\\s*$");
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s+if\\s+(.+?)\\s*\\{\\s*$|^\\s*\\}?\\s*else\\s+if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern LOOP_PATTERN = Pattern.compile("^\\s*loop(?:\\s+(\\d+))?\\s*\\{?\\s*$");
    private static final Pattern VARIABLE_ASSIGN_WITH_FUNC = Pattern.compile("^\\s*(?:let\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*)\\)\\s*$");

    // Private constructor for singleton
    private ScriptInterpreter() {
        initializeEnvironmentVariables();
    }

    /**
     * Get interpreter instance (singleton)
     */
    public static ScriptInterpreter getInstance() {
        if (instance == null) {
            instance = new ScriptInterpreter();
        }
        return instance;
    }

    public String getCurrentScriptName() {
        return currentScriptName;
    }

    public void setCurrentScriptName(String name) {
        this.currentScriptName = name;
    }

    private void initializeEnvironmentVariables() {
        // Player variables
        environmentVariables.put("PLAYER_NAME", new EnvironmentVariable("PLAYER_NAME", "", "Current player name"));
        environmentVariables.put("PLAYER_X", new EnvironmentVariable("PLAYER_X", "", "Player X coordinate"));
        environmentVariables.put("PLAYER_Y", new EnvironmentVariable("PLAYER_Y", "", "Player Y coordinate"));
        environmentVariables.put("PLAYER_Z", new EnvironmentVariable("PLAYER_Z", "", "Player Z coordinate"));
        environmentVariables.put("PLAYER_YAW", new EnvironmentVariable("PLAYER_YAW", "", "Player horizontal rotation"));
        environmentVariables.put("PLAYER_PITCH", new EnvironmentVariable("PLAYER_PITCH", "", "Player vertical rotation"));
        environmentVariables.put("PLAYER_HEALTH", new EnvironmentVariable("PLAYER_HEALTH", "", "Player health"));
        environmentVariables.put("PLAYER_FOOD", new EnvironmentVariable("PLAYER_FOOD", "", "Player food level"));
        environmentVariables.put("PLAYER_LEVEL", new EnvironmentVariable("PLAYER_LEVEL", "", "Player level"));
        environmentVariables.put("PLAYER_SPEED", new EnvironmentVariable("PLAYER_SPEED", "", "Player speed"));
        environmentVariables.put("PLAYER_XP", new EnvironmentVariable("PLAYER_XP", "", "Player experience level"));
        environmentVariables.put("IS_SNEAKING", new EnvironmentVariable("IS_SNEAKING", "", "Is player sneaking"));
        environmentVariables.put("IS_SPRINTING", new EnvironmentVariable("IS_SPRINTING", "", "Is player sprinting"));
        
        // World variables
        environmentVariables.put("WORLD_TIME", new EnvironmentVariable("WORLD_TIME", "", "Current world time"));
        environmentVariables.put("WORLD_DAY", new EnvironmentVariable("WORLD_DAY", "", "Current world day"));
        environmentVariables.put("WORLD_WEATHER", new EnvironmentVariable("WORLD_WEATHER", "", "Current weather"));
        environmentVariables.put("WORLD_DIFFICULTY", new EnvironmentVariable("WORLD_DIFFICULTY", "", "World difficulty"));
        
        // System variables
        environmentVariables.put("SCRIPT_NAME", new EnvironmentVariable("SCRIPT_NAME", "", "Current script name"));
        environmentVariables.put("SCRIPT_PATH", new EnvironmentVariable("SCRIPT_PATH", "", "Current script path"));
        environmentVariables.put("SCRIPT_DIR", new EnvironmentVariable("SCRIPT_DIR", "", "Current script directory"));
    }
    

    public void updateEnvironmentVariables() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            updateEnvVar("PLAYER_NAME", mc.player.getName().getString());
            updateEnvVar("PLAYER_X", String.format("%.2f", mc.player.getX()));
            updateEnvVar("PLAYER_Y", String.format("%.2f", mc.player.getY()));
            updateEnvVar("PLAYER_Z", String.format("%.2f", mc.player.getZ()));
            updateEnvVar("PLAYER_YAW", String.format("%.2f", mc.player.getYaw()));
            updateEnvVar("PLAYER_PITCH", String.format("%.2f", mc.player.getPitch()));
            float health = mc.player.getHealth();
            String healthStr = String.format("%.1f", health);
            updateEnvVar("PLAYER_HEALTH", healthStr);
            updateEnvVar("PLAYER_FOOD", String.valueOf(mc.player.getHungerManager().getFoodLevel()));
            updateEnvVar("PLAYER_XP", String.valueOf(mc.player.experienceLevel));
            updateEnvVar("PLAYER_LEVEL", String.valueOf(mc.player.experienceLevel));
            updateEnvVar("PLAYER_SPEED", String.format("%.2f", mc.player.getMovementSpeed()));
            updateEnvVar("WORLD_TIME", String.valueOf(mc.world.getTimeOfDay()));
            updateEnvVar("WORLD_DAY", String.valueOf(mc.world.getTimeOfDay() / 24000L));
            updateEnvVar("WORLD_WEATHER", mc.world.isRaining() ? (mc.world.isThundering() ? "thunder" : "rain") : "clear");
            updateEnvVar("WORLD_DIFFICULTY", mc.world.getDifficulty().getName());
            updateEnvVar("GAME_MODE", mc.player.isCreative() ? "creative" : "survival");
            updateEnvVar("DIMENSION", mc.player.getWorld().getRegistryKey().getValue().toString());
            updateEnvVar("IS_SNEAKING", String.valueOf(mc.player.isSneaking()));
            updateEnvVar("IS_SPRINTING", String.valueOf(mc.player.isSprinting()));
            updateEnvVar("IS_RIDING", String.valueOf(mc.player.isRiding()));
            updateEnvVar("IS_SWIMMING", String.valueOf(mc.player.isSwimming()));
            
            // Update editor hints
            CodeCompletionManager.updateEnvironmentVariables();
        }
    }

    private void updateEnvVar(String name, String value) {
        EnvironmentVariable var = environmentVariables.get(name);
        if (var != null) {
            var.setValue(value);
        } else {
            environmentVariables.put(name, new EnvironmentVariable(name, value, ""));
        }
    }

    /**
     * Parse code and return list of commands to execute.
     *
     * @param code KHScript code string
     * @return List of recognized commands
     */
    public List<Command> parseCommands(String code) {
        List<Command> commands = new ArrayList<>();
        String[] lines = code.split("\\r?\\n");
        int i = 0;
        while (i < lines.length) {
            // Check for early return from function
            if (hasReturned) {
                break;
            }
            
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("//")) {
                try {
                    // Check for return statement FIRST
                    Matcher returnMatcher = RETURN_PATTERN.matcher(line);
                    if (returnMatcher.find()) {
                        String returnExpr = returnMatcher.group(1);
                        if (returnExpr != null && !returnExpr.trim().isEmpty()) {
                            returnValue = evaluateExpression(returnExpr.trim());
                        } else {
                            returnValue = null;
                        }
                        hasReturned = true;
                        break;
                    }
                    
                    // Check for function definition
                    Matcher funcMatcher = FUNCTION_PATTERN.matcher(line);
                    if (funcMatcher.find()) {
                        String funcName = funcMatcher.group(1);
                        String paramsStr = funcMatcher.group(2);
                        List<String> parameters = new ArrayList<>();
                        if (!paramsStr.trim().isEmpty()) {
                            parameters.addAll(Arrays.asList(paramsStr.split(",")));
                            parameters.replaceAll(String::trim);
                        }
                        
                        StringBuilder funcBody = new StringBuilder();
                        int blockLevel = 1;
                        i++;
                        while (i < lines.length && blockLevel > 0) {
                            String blockLine = lines[i].trim();
                            if (blockLine.contains("{")) blockLevel++;
                            if (blockLine.contains("}")) blockLevel--;
                            if (blockLevel > 0) {
                                funcBody.append(blockLine).append("\n");
                            }
                            i++;
                        }
                        
                        functions.put(funcName, new Function(funcName, parameters, funcBody.toString()));
                        continue;
                    }

                    // Check for variable assignment with function call: let x = func(args) or x = func(args)
                    Matcher varFuncMatcher = VARIABLE_ASSIGN_WITH_FUNC.matcher(line);
                    if (varFuncMatcher.find()) {
                        String varName = varFuncMatcher.group(1);
                        String funcName = varFuncMatcher.group(2);
                        String argsStr = varFuncMatcher.group(3);
                        
                        // Check if it's a user-defined function
                        Function func = functions.get(funcName);
                        if (func != null) {
                            String result = executeFunction(func, argsStr);
                            try {
                                variableStore.set(varName, result);
                                variables.put(varName, result);
                                CodeCompletionManager.addUserVariable(varName);
                            } catch (IllegalStateException e) {
                                LOGGER.error("Variable error at line {}: {}", i + 1, e.getMessage());
                            }
                            i++;
                            continue;
                        }
                        // If not a function, fall through to normal variable assignment
                    }

                    // Check for const declaration (const MAX = 100)
                    Matcher constMatcher = CONST_PATTERN.matcher(line);
                    if (constMatcher.find()) {
                        String varName = constMatcher.group(1);
                        String varValue = evaluateExpression(constMatcher.group(2).trim());
                        try {
                            variableStore.declareConst(varName, varValue);
                            variables.put(varName, varValue);
                            CodeCompletionManager.addUserVariable(varName);
                        } catch (IllegalStateException e) {
                            LOGGER.error("Const error at line {}: {}", i + 1, e.getMessage());
                        }
                        i++;
                        continue;
                    }
                    
                    // Check for let declaration (let x = 5)
                    Matcher letMatcher = LET_PATTERN.matcher(line);
                    if (letMatcher.find()) {
                        String varName = letMatcher.group(1);
                        String varValue = evaluateExpression(letMatcher.group(2).trim());
                        try {
                            variableStore.declareLet(varName, varValue);
                            variables.put(varName, varValue);
                            CodeCompletionManager.addUserVariable(varName);
                        } catch (IllegalStateException e) {
                            LOGGER.error("Let error at line {}: {}", i + 1, e.getMessage());
                        }
                        i++;
                        continue;
                    }
                    
                    // Check for legacy variable assignment (x = 5)
                    Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
                    if (varMatcher.find()) {
                        String varName = varMatcher.group(1);
                        String varValue = evaluateExpression(varMatcher.group(2).trim());
                        try {
                            variableStore.set(varName, varValue);
                            variables.put(varName, varValue);
                            CodeCompletionManager.addUserVariable(varName);
                        } catch (IllegalStateException e) {
                            LOGGER.error("Variable error at line {}: {}", i + 1, e.getMessage());
                        }
                        i++;
                        continue;
                    }

                    // Check for conditional statements with else support
                    Matcher ifMatcher = IF_PATTERN.matcher(line);
                    if (ifMatcher.find()) {
                        i = processIfElseBlock(lines, i, ifMatcher, commands);
                        continue;
                    }

                    // Check for while loop with environment variable updates
                    Matcher whileMatcher = WHILE_PATTERN.matcher(line);
                    if (whileMatcher.find()) {
                        String condition = whileMatcher.group(1) != null ? whileMatcher.group(1) : whileMatcher.group(2);
                        StringBuilder whileBlock = new StringBuilder();
                        int blockLevel = 1;
                        i++;
                        while (i < lines.length && blockLevel > 0) {
                            String blockLine = lines[i].trim();
                            if (blockLine.contains("{")) blockLevel++;
                            if (blockLine.contains("}")) blockLevel--;
                            if (blockLevel > 0) {
                                whileBlock.append(blockLine).append("\n");
                            }
                            i++;
                        }
                        
                        // Execute while loop with environment updates
                        int maxIterations = 10000; // Safety limit
                        int iterations = 0;
                        while (iterations < maxIterations) {
                            updateEnvironmentVariables(); // Update env vars before each check
                            if (!evaluateCondition(condition)) {
                                break;
                            }
                            parseCommands(whileBlock.toString());
                            if (hasReturned) break; // Support return inside while
                            iterations++;
                        }
                        if (iterations >= maxIterations) {
                            LOGGER.warn("While loop exceeded max iterations at line {}", i);
                        }
                        continue;
                    }

                    // Check for loop (infinite or counted)
                    Matcher loopMatcher = LOOP_PATTERN.matcher(line);
                    if (loopMatcher.find()) {
                        String countStr = loopMatcher.group(1);
                        int loopCount = countStr != null ? Integer.parseInt(countStr) : Integer.MAX_VALUE;
                        
                        StringBuilder loopBlock = new StringBuilder();
                        int blockLevel = 1;
                        i++;
                        while (i < lines.length && blockLevel > 0) {
                            String blockLine = lines[i].trim();
                            if (blockLine.contains("{")) blockLevel++;
                            if (blockLine.contains("}")) blockLevel--;
                            if (blockLevel > 0) {
                                loopBlock.append(blockLine).append("\n");
                            }
                            i++;
                        }
                        
                        for (int iter = 0; iter < loopCount && !shouldStop; iter++) {
                            updateEnvironmentVariables();
                            parseCommands(loopBlock.toString());
                            if (hasReturned) break;
                        }
                        continue;
                    }

                    // Check for for loop
                    Matcher forMatcher = FOR_PATTERN.matcher(line);
                    if (forMatcher.find()) {
                        String[] forParts = forMatcher.group(1).split(";");
                        if (forParts.length == 3) {
                            String init = forParts[0].trim();
                            String condition = forParts[1].trim();
                            String increment = forParts[2].trim();
                            
                            if (!init.isEmpty()) {
                                parseCommands(init);
                            }
                            
                            StringBuilder forBlock = new StringBuilder();
                            int blockLevel = 1;
                            i++;
                            while (i < lines.length && blockLevel > 0) {
                                String blockLine = lines[i].trim();
                                if (blockLine.contains("{")) blockLevel++;
                                if (blockLine.contains("}")) blockLevel--;
                                if (blockLevel > 0) {
                                    forBlock.append(blockLine).append("\n");
                                }
                                i++;
                            }
                            
                            int maxIterations = 10000;
                            int iterations = 0;
                            while (iterations < maxIterations && evaluateCondition(condition)) {
                                parseCommands(forBlock.toString());
                                if (hasReturned) break;
                                if (!increment.isEmpty()) {
                                    parseCommands(increment);
                                }
                                iterations++;
                            }
                        }
                        continue;
                    }

                    // IMPORTANT: Check for commands BEFORE function calls!
                    // This prevents commands like print("hello") from being treated as functions
                    String processedLine = processVariables(line);
                    List<String> parts = parseArguments(processedLine);
                    if (parts.isEmpty()) {
                        i++;
                        continue;
                    }

                    String commandName = parts.get(0).toLowerCase();
                    
                    // First check if it's a registered command
                    Command command = CommandRegistry.getCommand(commandName);
                    if (command != null) {
                        String[] args = parts.subList(1, parts.size()).toArray(new String[0]);
                        LOGGER.debug("Executing command: {} with args: {}", commandName, String.join(", ", args));
                        commands.add(command);
                        queueCommand(command, args);
                        i++;
                        continue;
                    }
                    
                    // Then check for function call (only if not a command)
                    Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
                    if (funcCallMatcher.find()) {
                        String funcName = funcCallMatcher.group(1);
                        String argsStr = funcCallMatcher.group(2);
                        
                        Function func = functions.get(funcName);
                        if (func != null) {
                            executeFunction(func, argsStr);
                            i++;
                            continue;
                        } else {
                            LOGGER.warn("Unknown function or command at line {}: {}", i + 1, funcName);
                        }
                    } else {
                        LOGGER.warn("Unknown command at line {}: {}", i + 1, commandName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing line {}: {}", i + 1, line, e);
                }
            }
            i++;
        }
        return commands;
    }
    
    /**
     * Execute a function with proper scope isolation and return value support.
     */
    private String executeFunction(Function func, String argsStr) {
        // Parse and evaluate arguments
        List<String> arguments = new ArrayList<>();
        if (argsStr != null && !argsStr.trim().isEmpty()) {
            // Split by comma but respect parentheses
            arguments = splitFunctionArguments(argsStr);
            // Evaluate each argument
            for (int j = 0; j < arguments.size(); j++) {
                arguments.set(j, evaluateExpression(arguments.get(j).trim()));
            }
        }
        
        // Save current state for scope isolation
        Map<String, String> savedVariables = new HashMap<>(variables);
        String savedReturnValue = returnValue;
        boolean savedHasReturned = hasReturned;
        
        // Reset return state for this function call
        returnValue = null;
        hasReturned = false;
        
        // Set function parameters with evaluated values
        List<String> params = func.getParameters();
        for (int j = 0; j < params.size() && j < arguments.size(); j++) {
            String paramName = params.get(j).trim();
            variables.put(paramName, arguments.get(j));
            variableStore.set(paramName, arguments.get(j));
        }
        
        // Execute function body
        parseCommands(func.getBody());
        
        // Capture return value
        String result = returnValue != null ? returnValue : "";
        
        // Restore scope
        variables.clear();
        variables.putAll(savedVariables);
        returnValue = savedReturnValue;
        hasReturned = savedHasReturned;
        
        return result;
    }
    
    /**
     * Split function arguments respecting nested parentheses.
     */
    private List<String> splitFunctionArguments(String argsStr) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inString = false;
        
        for (char c : argsStr.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '(') parenDepth++;
                else if (c == ')') parenDepth--;
                else if (c == ',' && parenDepth == 0) {
                    args.add(current.toString().trim());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        
        if (current.length() > 0) {
            args.add(current.toString().trim());
        }
        
        return args;
    }
    
    /**
     * Process if/else/else-if blocks properly.
     */
    private int processIfElseBlock(String[] lines, int startIndex, Matcher ifMatcher, List<Command> commands) {
        String condition = ifMatcher.group(1) != null ? ifMatcher.group(1) : ifMatcher.group(2);
        
        // Collect if block
        StringBuilder ifBlock = new StringBuilder();
        int blockLevel = 1;
        int i = startIndex + 1;
        
        while (i < lines.length && blockLevel > 0) {
            String blockLine = lines[i].trim();
            
            // Check for else at the end of block
            if (blockLevel == 1 && (ELSE_PATTERN.matcher(blockLine).find() || ELSE_IF_PATTERN.matcher(blockLine).find())) {
                break;
            }
            
            if (blockLine.contains("{")) blockLevel++;
            if (blockLine.contains("}")) blockLevel--;
            
            if (blockLevel > 0) {
                ifBlock.append(blockLine).append("\n");
            }
            i++;
        }
        
        boolean conditionResult = evaluateCondition(condition);
        boolean executed = false;
        
        if (conditionResult) {
            parseCommands(ifBlock.toString());
            executed = true;
        }
        
        // Check for else or else-if
        while (i < lines.length) {
            String currentLine = lines[i].trim();
            
            // Check for else-if
            Matcher elseIfMatcher = ELSE_IF_PATTERN.matcher(currentLine);
            if (elseIfMatcher.find()) {
                String elseIfCondition = elseIfMatcher.group(1) != null ? elseIfMatcher.group(1) : elseIfMatcher.group(2);
                
                StringBuilder elseIfBlock = new StringBuilder();
                blockLevel = 1;
                i++;
                
                while (i < lines.length && blockLevel > 0) {
                    String blockLine = lines[i].trim();
                    
                    if (blockLevel == 1 && (ELSE_PATTERN.matcher(blockLine).find() || ELSE_IF_PATTERN.matcher(blockLine).find())) {
                        break;
                    }
                    
                    if (blockLine.contains("{")) blockLevel++;
                    if (blockLine.contains("}")) blockLevel--;
                    
                    if (blockLevel > 0) {
                        elseIfBlock.append(blockLine).append("\n");
                    }
                    i++;
                }
                
                if (!executed && evaluateCondition(elseIfCondition)) {
                    parseCommands(elseIfBlock.toString());
                    executed = true;
                }
                continue;
            }
            
            // Check for else
            Matcher elseMatcher = ELSE_PATTERN.matcher(currentLine);
            if (elseMatcher.find()) {
                StringBuilder elseBlock = new StringBuilder();
                blockLevel = 1;
                i++;
                
                while (i < lines.length && blockLevel > 0) {
                    String blockLine = lines[i].trim();
                    if (blockLine.contains("{")) blockLevel++;
                    if (blockLine.contains("}")) blockLevel--;
                    
                    if (blockLevel > 0) {
                        elseBlock.append(blockLine).append("\n");
                    }
                    i++;
                }
                
                if (!executed) {
                    parseCommands(elseBlock.toString());
                }
                break;
            }
            
            // No more else/else-if, exit
            break;
        }
        
        return i;
    }

    /**
     * Process variable assignment
     */
    private void processVariableAssignment(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
String varName = parts[0].trim();
            String varValue = parts[1].trim();

            // Remove quotes if present
            if (varValue.startsWith("\"") && varValue.endsWith("\"")) {
    varValue = varValue.substring(1, varValue.length() - 1);
            }

            variables.put(varName, varValue);
            LOGGER.debug("Variable set: {} = {}", varName, varValue);
    }
    }

    /**
     * Replace variables in string with their values
     */
    private String processVariables(String line) {
        String result = line;
        
        // Process environment variables
        Matcher envMatcher = ENV_VAR_PATTERN.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (envMatcher.find()) {
            String varName = envMatcher.group(1).toUpperCase();
            EnvironmentVariable envVar = environmentVariables.get(varName);
            if (envVar != null) {
                System.out.println("Found env var: " + varName + " = " + envVar.getValue());
                envMatcher.appendReplacement(sb, envVar.getValue());
            }
        }
        envMatcher.appendTail(sb);
        result = sb.toString();

        // Process user variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            Pattern varPattern = Pattern.compile("\\$" + Pattern.quote(entry.getKey()));
            Matcher varMatcher = varPattern.matcher(result);
            sb = new StringBuffer();
            while (varMatcher.find()) {
                varMatcher.appendReplacement(sb, entry.getValue());
            }
            varMatcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    /**
     * Add command to queue for async execution.
     *
     * @param command Command to execute
     * @param args    Command arguments
     */
    public void queueCommand(Command command, String[] args) {
        // Process variables in arguments
        String[] processedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            processedArgs[i] = processVariables(args[i]);
        }
        
        commandQueue.add(new CommandEntry(command, processedArgs));
        if (!isProcessing && !shouldStop) {
            processNextCommand();
        }
    }

    /**
     * Start executing queued commands.
     * Can be called from GUI to run script.
     */
    public void executeQueuedCommands() {
        if (!isProcessing && !shouldStop && !commandQueue.isEmpty()) {
            updateEnvironmentVariables(); // Update environment variables before execution
            processNextCommand();
        }
    }

    /**
     * Process next command from queue asynchronously.
     */
    private void processNextCommand() {
        if (commandQueue.isEmpty() || shouldStop) {
            isProcessing = false;
            shouldStop = false;
            return;
        }

        isProcessing = true;
        CommandEntry entry = commandQueue.poll();

        CompletableFuture<Void> future;
        try {
            future = entry.command.executeAsync(entry.args);
        } catch (Exception e) {
            LOGGER.error("Error starting command '{}': {}", entry.command.getName(), e.getMessage());
            processNextCommand();
            return;
        }

        future.exceptionally(throwable -> {
            LOGGER.error("Error executing command '{}': {}", entry.command.getName(), throwable.getMessage());
            return null;
        }).thenRun(this::processNextCommand);
    }

    /**
     * Stop execution of all queued commands.
     */
    public void stopProcessing() {
        shouldStop = true;
        commandQueue.clear();
        // Clear functions to prevent memory leaks
        functions.clear();
        // Reset return state
        returnValue = null;
        hasReturned = false;
        LOGGER.info("Command processing stopped, functions cleared.");
    }

    /**
     * Parse line into commands and arguments, respecting quotes.
     *
     * @param line Line to parse
     * @return List of parts: command and arguments
     */
    private static List<String> parseArguments(String line) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ' ' && !inQuotes) {
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }
        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }
        return args;
    }

    /**
     * Clear all variables
     */
    public void clearVariables() {
        variables.clear();
        variableStore.clear();
        // Clear user variables in CodeCompletionManager
        CodeCompletionManager.clearUserVariables();
    }
    
    /**
     * Reset interpreter state for new script execution
     */
    public void resetState() {
        clearVariables();
        clearFunctions();
        returnValue = null;
        hasReturned = false;
        shouldStop = false;
        commandQueue.clear();
    }

    /**
     * Get variable value
     */
    public String getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Set variable value
     */
    public void setVariable(String name, String value) {
        variables.put(name, value);
        // Notify CodeCompletionManager about new variable
        CodeCompletionManager.addUserVariable(name);
    }

    /**
     * Check if variable exists
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    /**
     * Return all script variables
     */
    public Map<String, String> getVariables() {
        return variables;
    }

    /**
     * Evaluates a condition using the new ExpressionParser.
     * Supports proper operator precedence, ternary operator, and all JS/Rust-like features.
     */
    private boolean evaluateCondition(String condition) {
        try {
            LOGGER.debug("Evaluating condition: {}", condition);
            
            // Use the new ExpressionParser with proper operator precedence
            ExpressionParser.Value result = ExpressionParser.evaluate(condition, this::resolveVariable);
            return result.toBoolean();
            
        } catch (Exception e) {
            LOGGER.error("Error evaluating condition: {}", condition, e);
            return false;
        }
    }
    
    /**
     * Evaluates an expression and returns the result as a string.
     * Supports arithmetic, ternary operator, and all expression features.
     */
    private String evaluateExpression(String expression) {
        try {
            ExpressionParser.Value result = ExpressionParser.evaluate(expression, this::resolveVariable);
            return result.toString();
        } catch (Exception e) {
            LOGGER.error("Error evaluating expression: {}", expression, e);
            return expression; // Return original on error
        }
    }
    
    /**
     * Resolves a variable name to its value.
     * Checks: variableStore -> variables map -> environment variables
     */
    private String resolveVariable(String name) {
        // First check the new variable store
        String value = variableStore.get(name);
        if (value != null) {
            return value;
        }
        
        // Check legacy variables map
        value = variables.get(name);
        if (value != null) {
            return value;
        }
        
        // Check environment variables
        EnvironmentVariable envVar = environmentVariables.get(name.toUpperCase());
        if (envVar != null && envVar.getValue() != null) {
            return envVar.getValue();
        }
        
        return null;
    }
    
    /**
     * Internal class for storing command and its arguments in queue.
     */
    private static class CommandEntry {
        final Command command;
        final String[] args;

        CommandEntry(Command command, String[] args) {
            this.command = command;
            this.args = args;
        }
    }

    public void clearFunctions() {
        functions.clear();
    }

    public Function getFunction(String name) {
        return functions.get(name);
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    public List<EnvironmentVariable> getAllEnvironmentVariables() {
        return new ArrayList<>(environmentVariables.values());
    }

    public EnvironmentVariable getEnvironmentVariable(String name) {
        return environmentVariables.get(name);
    }

    public boolean hasEnvironmentVariable(String name) {
        return environmentVariables.containsKey(name);
    }

    public Map<String, String> getContext() {
        Map<String, String> context = new HashMap<>();
        context.putAll(variables);
        for (Map.Entry<String, EnvironmentVariable> entry : environmentVariables.entrySet()) {
            context.put(entry.getKey(), entry.getValue().getValue());
        }
        return context;
    }

    public Map<String, EnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }
}

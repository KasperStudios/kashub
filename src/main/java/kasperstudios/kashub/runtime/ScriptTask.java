package kasperstudios.kashub.runtime;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.EnvironmentVariable;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a running script as a task
 */
public class ScriptTask {
    private final int id;
    private final String name;
    private final String code;
    private final Set<String> tags;
    private final long startTime;
    private final ScriptType scriptType;
    
    private ScriptState state;
    private long lastTickTime;
    private String lastError;
    private int priority;
    private int currentLine;
    private int executedCommands;
    
    // Command queue for this task
    private final Queue<CommandEntry> commandQueue;
    private volatile boolean isProcessingCommand;
    private CompletableFuture<Void> currentCommandFuture;
    private final Object processLock = new Object(); // Lock for processNextCommand synchronization
    
    // Loop control - prevent duplicate loop iterations
    private LoopMarkerCommand pendingLoopMarker = null;
    private static final int MAX_QUEUE_SIZE = 1000; // Prevent queue overflow
    private static final int LOOP_REQUEUE_THRESHOLD = 5; // Re-add loop marker when queue has <= this many commands
    
    // Script variables
    private final Map<String, String> variables = new HashMap<>();
    
    // Parsing patterns - Legacy and Rust-style syntax support
    // Legacy: x = 5, Rust-style: let x = 5 / const x = 5
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*(?:let\\s+|const\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    // Legacy: if (cond) {, Rust-style: if cond {
    private static final Pattern IF_PATTERN = Pattern.compile("^\\s*if\\s+(.+?)\\s*\\{\\s*$|^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    // Legacy: } else if (cond) {, Rust-style: } else if cond {
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s+if\\s+(.+?)\\s*\\{\\s*$|^\\s*\\}?\\s*else\\s+if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s*\\{?\\s*$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^\\s*for\\s*\\((.*)\\)\\s*\\{?\\s*$");
    // Legacy: while (cond) {, Rust-style: while cond {
    private static final Pattern WHILE_PATTERN = Pattern.compile("^\\s*while\\s+(.+?)\\s*\\{\\s*$|^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern LOOP_PATTERN = Pattern.compile("^\\s*loop(?:\\s+(\\d+))?\\s*\\{?\\s*$");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*\\{?\\s*$");
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*$");
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$([A-Z_][A-Z0-9_]*)");
    private static final Pattern USER_VAR_PATTERN = Pattern.compile("\\$([a-z_][a-z0-9_]*)");
    
    // Functions defined in this script
    private final Map<String, FunctionDef> localFunctions = new HashMap<>();
    
    // Control flow flags
    private boolean shouldBreak = false;
    private boolean shouldContinue = false;

    public ScriptTask(int id, String name, String code, Set<String> tags, ScriptType scriptType) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        this.scriptType = scriptType;
        this.startTime = System.currentTimeMillis();
        this.lastTickTime = startTime;
        this.state = ScriptState.RUNNING;
        this.priority = 0;
        this.currentLine = 0;
        this.executedCommands = 0;
        this.commandQueue = new ConcurrentLinkedQueue<>();
        this.isProcessingCommand = false;
        // Initialize loop control flags
        this.shouldBreak = false;
        this.shouldContinue = false;
        this.pendingLoopMarker = null;
        ScriptLogger.getInstance().debug("Task " + id + " (" + name + ") created, loop flags initialized");
    }

    /**
     * Execute one tick of the script
     */
    public void tick() {
        if (state != ScriptState.RUNNING) {
            ScriptLogger.getInstance().debug("Task " + id + ": tick() skipped, state=" + state);
            return;
        }
        
        lastTickTime = System.currentTimeMillis();
        
        // Check if we should re-queue loop marker (only when queue is completely empty and no command is processing)
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.tick:104\",\"message\":\"Checking loop marker re-queue\",\"data\":{\"taskId\":" + id + ",\"pendingLoopMarker\":" + (pendingLoopMarker != null) + ",\"isProcessingCommand\":" + isProcessingCommand + ",\"queueSize\":" + commandQueue.size() + ",\"shouldBreak\":" + shouldBreak + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"C\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        if (pendingLoopMarker != null && !isProcessingCommand && commandQueue.isEmpty() && !shouldBreak) {
            ScriptLogger.getInstance().debug("Task " + id + ": Re-queuing loop marker, queue empty (pendingLoopMarker=" + (pendingLoopMarker != null) + ", isProcessingCommand=" + isProcessingCommand + ", queueSize=" + commandQueue.size() + ", shouldBreak=" + shouldBreak + ")");
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.tick:106\",\"message\":\"Re-queuing loop marker\",\"data\":{\"taskId\":" + id + ",\"startLine\":" + pendingLoopMarker.startLine + ",\"endLine\":" + pendingLoopMarker.endLine + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            commandQueue.add(new CommandEntry(pendingLoopMarker, new String[0]));
            pendingLoopMarker = null; // Clear pending marker
            // Process the newly queued marker immediately
            processNextCommand();
            return;
        }
        
        // If no active command, take next from queue
        if (!isProcessingCommand && !commandQueue.isEmpty()) {
            ScriptLogger.getInstance().debug("Task " + id + ": tick() processing next command, queueSize=" + commandQueue.size());
            processNextCommand();
        } else if (pendingLoopMarker != null) {
            ScriptLogger.getInstance().debug("Task " + id + ": tick() waiting for queue to empty (pendingLoopMarker=" + (pendingLoopMarker != null) + ", isProcessingCommand=" + isProcessingCommand + ", queueSize=" + commandQueue.size() + ", shouldBreak=" + shouldBreak + ")");
        }
    }

    /**
     * Parse and queue commands for this task
     */
    public void parseAndQueue() {
        try {
            String[] lines = code.split("\\r?\\n");
            
            // Reset loop control flags before parsing
            shouldBreak = false;
            shouldContinue = false;
            pendingLoopMarker = null;
            commandQueue.clear();
            
            ScriptLogger.getInstance().debug("Parsing script " + name + " with " + lines.length + " lines, loop flags reset");
            
            parseLines(lines, 0, lines.length);
            
            ScriptLogger.getInstance().info("Task " + id + " queued " + commandQueue.size() + " commands");
            
        } catch (Exception e) {
            lastError = e.getMessage();
            state = ScriptState.ERROR;
            ScriptLogger.getInstance().error("Script " + name + " parse error: " + e.getMessage());
        }
    }
    
    /**
     * Recursively parse code lines with block support
     */
    private void parseLines(String[] lines, int start, int end) {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:165\",\"message\":\"parseLines called\",\"data\":{\"taskId\":" + id + ",\"start\":" + start + ",\"end\":" + end + ",\"queueSizeBefore\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        int i = start;
        while (i < end && state != ScriptState.STOPPED && !shouldBreak) {
            // Check for continue flag
            if (shouldContinue) {
                shouldContinue = false;
                return; // Exit current block iteration
            }
            
            String line = lines[i].trim();
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }
            
            // Skip closing braces and end
            if (line.equals("}") || line.equals("end")) {
                i++;
                continue;
            }
            
            // Skip else and else if (handled in if)
            if (ELSE_PATTERN.matcher(line).matches() || ELSE_IF_PATTERN.matcher(line).matches()) {
                i++;
                continue;
            }
            
            currentLine = i + 1;
            
            // Note: break and continue are now handled as commands in the queue
            // This allows them to work correctly in loops that execute from queue
            
            // Check for function definition
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(line);
            if (funcMatcher.find()) {
                String funcName = funcMatcher.group(1);
                String paramsStr = funcMatcher.group(2);
                List<String> parameters = new ArrayList<>();
                if (!paramsStr.trim().isEmpty()) {
                    for (String param : paramsStr.split(",")) {
                        parameters.add(param.trim());
                    }
                }
                
                int blockEnd = findBlockEnd(lines, i + 1, end);
                localFunctions.put(funcName, new FunctionDef(funcName, parameters, lines, i + 1, blockEnd));
                // Also register in ScriptInterpreter for global access
                StringBuilder funcBody = new StringBuilder();
                for (int j = i + 1; j < blockEnd; j++) {
                    funcBody.append(lines[j]).append("\n");
                }
                ScriptInterpreter.getInstance().setVariable("__func_" + funcName, "defined");
                
                i = blockEnd + 1;
                continue;
            }
            
            // Check for function call
            Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
            if (funcCallMatcher.find()) {
                String funcName = funcCallMatcher.group(1);
                String argsStr = funcCallMatcher.group(2);
                
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:233\",\"message\":\"Function call detected\",\"data\":{\"taskId\":" + id + ",\"funcName\":\"" + funcName + "\",\"queueSizeBefore\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
                
                FunctionDef func = localFunctions.get(funcName);
                if (func != null) {
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:240\",\"message\":\"Function found, parsing body\",\"data\":{\"taskId\":" + id + ",\"funcName\":\"" + funcName + "\",\"startLine\":" + func.startLine + ",\"endLine\":" + func.endLine + ",\"queueSizeBefore\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    // Parse arguments
                    List<String> arguments = new ArrayList<>();
                    if (!argsStr.trim().isEmpty()) {
                        for (String arg : argsStr.split(",")) {
                            arguments.add(processVariables(arg.trim()));
                        }
                    }
                    
                    // Save current variables
                    Map<String, String> savedVars = new HashMap<>(variables);
                    
                    // Set function parameters
                    for (int j = 0; j < func.parameters.size() && j < arguments.size(); j++) {
                        String paramValue = arguments.get(j);
                        // Remove quotes if present
                        if (paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                            paramValue = paramValue.substring(1, paramValue.length() - 1);
                        }
                        variables.put(func.parameters.get(j), paramValue);
                        ScriptInterpreter.getInstance().setVariable(func.parameters.get(j), paramValue);
                    }
                    
                    // Execute function body
                    parseLines(func.lines, func.startLine, func.endLine);
                    
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:262\",\"message\":\"Function body parsed\",\"data\":{\"taskId\":" + id + ",\"funcName\":\"" + funcName + "\",\"queueSizeAfter\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    
                    // Restore variables
                    variables.clear();
                    variables.putAll(savedVars);
                    
                    i++;
                    continue;
                }
                // If function not found, fall through to command processing
            }
            
            // Check for variable assignment
            Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String varValue = processVariables(varMatcher.group(2).trim());
                // Remove quotes if present
                if (varValue.startsWith("\"") && varValue.endsWith("\"")) {
                    varValue = varValue.substring(1, varValue.length() - 1);
                } else {
                    // Try to evaluate as arithmetic expression
                    try {
                        double result = evaluateExpressionAsDouble(varValue);
                        if (!Double.isNaN(result)) {
                            // Format as integer if it's a whole number
                            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                                varValue = String.valueOf((int) result);
                            } else {
                                varValue = String.valueOf(result);
                            }
                        }
                    } catch (Exception e) {
                        // Keep original value if evaluation fails
                    }
                }
                variables.put(varName, varValue);
                // Also set in ScriptInterpreter for global access
                ScriptInterpreter.getInstance().setVariable(varName, varValue);
                i++;
                continue;
            }
            
            // Check for if with else if and else support
            Matcher ifMatcher = IF_PATTERN.matcher(line);
            if (ifMatcher.find()) {
                // Support both Rust-style (group 1) and Legacy (group 2)
                String condition = ifMatcher.group(1) != null ? ifMatcher.group(1) : ifMatcher.group(2);
                // Find the chain - search until we find the complete if-else chain
                // We need to search beyond the initial if block to find else if and else blocks
                // Extend the search range to include a few more lines to catch else blocks
                // that might be on the next line after the closing brace
                int searchEnd = Math.min(end, lines.length);
                if (searchEnd < lines.length) {
                    // Extend by a few lines to catch else blocks
                    searchEnd = Math.min(searchEnd + 5, lines.length);
                }
                List<int[]> chain = findIfElseChain(lines, i + 1, searchEnd);
                int blockEnd = chain.get(0)[0];
                
                // Always defer condition evaluation - check at execution time, not parse time
                // This ensures conditions are evaluated after previous commands in the loop have executed
                    int ifBlockEnd = blockEnd;
                    if (chain.size() > 1) {
                        ifBlockEnd = chain.get(1)[0];
                    }
                
                // Build else if / else chain
                List<ConditionalBlock> conditionalBlocks = new ArrayList<>();
                conditionalBlocks.add(new ConditionalBlock(condition, i + 1, ifBlockEnd));
                
                    for (int ci = 1; ci < chain.size(); ci++) {
                        int[] entry = chain.get(ci);
                        int pos = entry[0];
                        int type = entry[1];
                        
                        if (type == 0) { // else if
                            Matcher elseIfMatcher = ELSE_IF_PATTERN.matcher(lines[pos].trim());
                            if (elseIfMatcher.find()) {
                            String elseIfCondition = elseIfMatcher.group(1) != null ? elseIfMatcher.group(1) : elseIfMatcher.group(2);
                                    int elseIfEnd = blockEnd;
                                    if (ci + 1 < chain.size()) {
                                        elseIfEnd = chain.get(ci + 1)[0];
                                    }
                            conditionalBlocks.add(new ConditionalBlock(elseIfCondition, pos + 1, elseIfEnd));
                            }
                        } else if (type == 1) { // else
                            // #region agent log
                            try {
                                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:363\",\"message\":\"Adding else block\",\"data\":{\"taskId\":" + id + ",\"pos\":" + pos + ",\"blockEnd\":" + blockEnd + ",\"startLine\":" + (pos + 1) + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                                fw.close();
                            } catch (Exception e) {}
                            // #endregion
                        conditionalBlocks.add(new ConditionalBlock(null, pos + 1, blockEnd)); // null condition = else
                    }
                }
                
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:370\",\"message\":\"Conditional blocks created\",\"data\":{\"taskId\":" + id + ",\"blocksCount\":" + conditionalBlocks.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
                
                commandQueue.add(new CommandEntry(new ConditionalCommand(this, lines, conditionalBlocks), new String[0]));
                i = blockEnd + 1;
                continue;
            }
            
            // Check for for loop
            Matcher forMatcher = FOR_PATTERN.matcher(line);
            if (forMatcher.find()) {
                String forContent = forMatcher.group(1);
                String[] forParts = forContent.split(";");
                
                if (forParts.length == 3) {
                    String init = forParts[0].trim();
                    String condition = forParts[1].trim();
                    String increment = forParts[2].trim();
                    
                    int blockEnd = findBlockEnd(lines, i + 1, end);
                    
                    // Initialization
                    if (!init.isEmpty()) {
                        Matcher initMatcher = VARIABLE_PATTERN.matcher(init);
                        if (initMatcher.find()) {
                            String initValue = processVariables(initMatcher.group(2).trim());
                            variables.put(initMatcher.group(1), initValue);
                            ScriptInterpreter.getInstance().setVariable(initMatcher.group(1), initValue);
                        }
                    }
                    
                    // Loop
                    int maxIterations = 10000; // Protection against infinite loops
                    int iterations = 0;
                    shouldBreak = false;
                    while (evaluateCondition(condition) && iterations < maxIterations && !shouldBreak) {
                        parseLines(lines, i + 1, blockEnd);
                        
                        if (shouldBreak) break;
                        
                        // Increment
                        if (!increment.isEmpty()) {
                            executeIncrement(increment);
                        }
                        iterations++;
                    }
                    shouldBreak = false;
                    
                    i = blockEnd + 1;
                    continue;
                }
                i++;
                continue;
            }
            
            // Check for while loop
            Matcher whileMatcher = WHILE_PATTERN.matcher(line);
            if (whileMatcher.find()) {
                // Support both Rust-style (group 1) and Legacy (group 2)
                String condition = whileMatcher.group(1) != null ? whileMatcher.group(1) : whileMatcher.group(2);
                int blockEnd = findBlockEnd(lines, i + 1, end);
                
                // Check if condition is always true (infinite loop)
                String trimmedCondition = condition.trim().toLowerCase();
                // Remove parentheses if present
                while (trimmedCondition.startsWith("(") && trimmedCondition.endsWith(")")) {
                    trimmedCondition = trimmedCondition.substring(1, trimmedCondition.length() - 1).trim();
                }
                if (trimmedCondition.equals("true")) {
                    // Infinite while loop - use LoopMarkerCommand like loop {}
                    ScriptLogger.getInstance().debug("Task " + id + ": Detected infinite while loop (while true), using LoopMarkerCommand");
                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    commandQueue.add(new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd), new String[0]));
                } else {
                    // Conditional while loop - evaluate condition at runtime
                    // For now, use the old behavior but with runtime condition checking
                    // TODO: Implement proper runtime condition checking for while loops
                    ScriptLogger.getInstance().debug("Task " + id + ": Detected conditional while loop, condition: " + condition);
                int maxIterations = 10000;
                int iterations = 0;
                shouldBreak = false;
                while (evaluateCondition(condition) && iterations < maxIterations && !shouldBreak) {
                    parseLines(lines, i + 1, blockEnd);
                    if (shouldBreak) break;
                    iterations++;
                }
                shouldBreak = false;
                }
                
                i = blockEnd + 1;
                continue;
            }
            
            // Check for loop (with optional iteration count)
            Matcher loopMatcher = LOOP_PATTERN.matcher(line);
            if (loopMatcher.find()) {
                String countStr = loopMatcher.group(1);
                int blockEnd = findBlockEnd(lines, i + 1, end);
                
                if (countStr != null && !countStr.isEmpty()) {
                    // loop N - execute N times
                    int count = Integer.parseInt(countStr);
                    shouldBreak = false;
                    for (int iter = 0; iter < count && !shouldBreak && state == ScriptState.RUNNING; iter++) {
                        parseLines(lines, i + 1, blockEnd);
                        if (shouldBreak) break;
                    }
                    shouldBreak = false;
                } else {
                    // loop without number - infinite loop via marker
                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    commandQueue.add(new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd), new String[0]));
                }
                
                i = blockEnd + 1;
                continue;
            }
            
            // Regular command - process variables
            String processedLine = processVariables(line);
            List<String> parts = parseArguments(processedLine);
            if (parts.isEmpty()) {
                i++;
                continue;
            }
            
            String commandName = parts.get(0).toLowerCase();
            String[] args = parts.subList(1, parts.size()).toArray(new String[0]);
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:470\",\"message\":\"Parsing command\",\"data\":{\"taskId\":" + id + ",\"line\":" + (i + 1) + ",\"commandName\":\"" + commandName + "\",\"queueSizeBefore\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            // Handle break and continue as special commands
            if (commandName.equals("break")) {
                ScriptLogger.getInstance().debug("Task " + id + ": Parsing break command at line " + currentLine);
                commandQueue.add(new CommandEntry(new BreakCommand(), new String[0]));
                i++;
                continue;
            }
            if (commandName.equals("continue")) {
                ScriptLogger.getInstance().debug("Task " + id + ": Parsing continue command at line " + currentLine);
                commandQueue.add(new CommandEntry(new ContinueCommand(), new String[0]));
                i++;
                continue;
            }
            
            Command command = CommandRegistry.getCommand(commandName);
            if (command != null) {
                commandQueue.add(new CommandEntry(command, args));
                ScriptLogger.getInstance().debug("Queued command: " + commandName + " with " + args.length + " args");
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:488\",\"message\":\"Command queued\",\"data\":{\"taskId\":" + id + ",\"commandName\":\"" + commandName + "\",\"queueSizeAfter\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
            } else {
                ScriptLogger.getInstance().warn("Unknown command at line " + currentLine + ": " + commandName);
            }
            
            i++;
        }
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.parseLines:496\",\"message\":\"parseLines finished\",\"data\":{\"taskId\":" + id + ",\"start\":" + start + ",\"end\":" + end + ",\"queueSizeAfter\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
    }
    
    /**
     * Find end of block (closing brace or end)
     */
    private int findBlockEnd(String[] lines, int start, int end) {
        int level = 1;
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            
            // Check for block closers FIRST (before openers) to handle "} else {" correctly
            // Special handling for "} else {" - it closes one block but opens another
            if (line.startsWith("} else {") || (line.contains("} else") && line.contains("{"))) {
                // "} else {" - closes previous block (decrease level) but opens new one (increase level)
                // Net effect: level stays the same, but we need to process both
                level--; // Close previous block
                // Opening brace will be handled below
            } else if (line.equals("}") || line.equals("end")) {
                level--;
            } else if (line.contains("}") && !line.contains("{")) {
                level--;
            }
            
            // Check for block openers AFTER closers
            if (line.contains("{") || 
                line.startsWith("if ") || line.startsWith("if(") ||
                line.startsWith("for ") || line.startsWith("for(") ||
                line.startsWith("while ") || line.startsWith("while(") ||
                line.startsWith("loop ") || line.equals("loop") || line.equals("loop{") ||
                line.startsWith("function ")) {
                // Only increment if it's a new block opener without closing on same line
                if (line.contains("{") && !line.contains("}")) {
                    level++;
                } else if (line.contains("{") && line.contains("}")) {
                    // Both open and close on same line (like "} else {")
                    level++; // Opening brace increases level
                } else if (!line.contains("{") && !line.contains("}")) {
                    // Block opener without braces (uses end)
                    level++;
                }
            }
            
            if (level == 0) return i;
        }
        return end - 1;
    }
    
    /**
     * Find end of if block and return list of else if and else positions
     * Returns: [blockEnd, elseIfPos1, elseIfPos2, ..., elsePos] (-1 if not present)
     */
    private List<int[]> findIfElseChain(String[] lines, int start, int end) {
        List<int[]> chain = new ArrayList<>(); // Each entry: [position, type] where type: 0=else if, 1=else
        int level = 1;
        
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:582\",\"message\":\"Finding if-else chain\",\"data\":{\"taskId\":" + id + ",\"start\":" + start + ",\"end\":" + end + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:612\",\"message\":\"Processing line\",\"data\":{\"taskId\":" + id + ",\"line\":" + (i + 1) + ",\"content\":\"" + line + "\",\"level\":" + level + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            // Special handling for "} else {" or "} else if" - check BEFORE decreasing level
            // This is critical because the closing brace would decrease level to 0 before we check for else
            boolean isElseIf = false;
            boolean isElse = false;
            boolean isElseOnSameLine = false;
            
            // Check if line contains "} else" - this means we need to handle it specially
            if (line.contains("} else")) {
                isElseOnSameLine = true;
                // Check for else if first
                if (ELSE_IF_PATTERN.matcher(line).matches()) {
                    isElseIf = true;
                } else if (line.contains("} else") && !line.contains("if")) {
                    // It's "} else {" or "} else {"
                    isElse = true;
                }
            } else if (level == 1 || level == 0) {
                // Normal case - check for else if or else at level 1 or 0 (after closing previous block)
                // Also check at level 0 because we might have just closed a block
                if (ELSE_IF_PATTERN.matcher(line).matches()) {
                    isElseIf = true;
                } else if (!ELSE_IF_PATTERN.matcher(line).matches()) {
                    boolean matchesElse = line.equals("else") || line.equals("else {") || 
                                         ELSE_PATTERN.matcher(line).matches();
                    if (matchesElse) {
                        isElse = true;
                    }
                }
            }
            
            // Check for block openers (nested blocks)
            if (line.contains("{") || 
                (line.startsWith("if ") || line.startsWith("if(")) && !line.contains("else") ||
                line.startsWith("for ") || line.startsWith("for(") ||
                line.startsWith("while ") || line.startsWith("while(") ||
                line.startsWith("loop ") || line.equals("loop") || line.equals("loop{") ||
                line.startsWith("function ")) {
                if (line.contains("{") && !line.contains("}")) {
                    level++;
                } else if (!line.contains("{") && !line.contains("}")) {
                    level++;
                }
            }
            
            // Add else if or else to chain
            if (isElseIf) {
                chain.add(new int[]{i, 0}); // 0 = else if
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:650\",\"message\":\"Added else if to chain\",\"data\":{\"taskId\":" + id + ",\"line\":" + (i + 1) + ",\"content\":\"" + line + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
            } else if (isElse) {
                chain.add(new int[]{i, 1}); // 1 = else
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:660\",\"message\":\"Added else to chain\",\"data\":{\"taskId\":" + id + ",\"line\":" + (i + 1) + ",\"content\":\"" + line + "\",\"level\":" + level + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
            }
            
            // Check for block closers
            // For "} else {" we need to decrease level for the closing brace, but not for the opening brace
            if (isElseOnSameLine) {
                // For "} else {" - decrease level for the closing brace, but the opening brace will increase it back
                if (line.contains("}") && !line.contains("{")) {
                    level--; // Only closing brace
                } else if (line.startsWith("}") && line.contains("{")) {
                    // "} else {" - decrease for closing brace, increase for opening brace
                    level--; // Closing brace
                    if (line.contains("{") && !line.endsWith("}")) {
                        level++; // Opening brace (if not balanced on same line)
                    }
                }
            } else {
                // Normal case
            if (line.equals("}") || line.equals("end")) {
                level--;
                } else if (line.contains("}") && !line.contains("{")) {
                level--;
                }
            }
            
            // If level reaches 0, we've closed the current block
            // But we need to check if there's an else or else if after this
            // So we continue searching for a few more lines to find else/else if
            if (level == 0) {
                // Check if there's an else or else if on the next line(s)
                // Look ahead up to 3 lines to find else/else if
                boolean foundElse = false;
                for (int j = i + 1; j < Math.min(i + 4, end); j++) {
                    String nextLine = lines[j].trim();
                    if (ELSE_IF_PATTERN.matcher(nextLine).matches() || 
                        (nextLine.contains("} else") && !nextLine.contains("if")) ||
                        (nextLine.equals("else") || nextLine.equals("else {"))) {
                        foundElse = true;
                        break;
                    }
                    // If we hit a non-empty line that's not else/else if, stop looking
                    if (!nextLine.isEmpty() && !nextLine.startsWith("//") && 
                        !nextLine.contains("else")) {
                        break;
                    }
                }
                
                // If we found an else/else if, continue searching
                // Otherwise, this is the end of the if-else chain
                if (!foundElse) {
                chain.add(0, new int[]{i, -1}); // Insert block end at beginning
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:699\",\"message\":\"Chain complete\",\"data\":{\"taskId\":" + id + ",\"chainSize\":" + chain.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                return chain;
                }
                // If we found else/else if, continue - level will be managed by the else block
            }
        }
        chain.add(0, new int[]{end - 1, -1});
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.findIfElseChain:704\",\"message\":\"Chain complete (end reached)\",\"data\":{\"taskId\":" + id + ",\"chainSize\":" + chain.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion
        return chain;
    }
    
    /**
     * Legacy method for simple if-else (backwards compatibility)
     */
    private int[] findIfBlockEndWithElse(String[] lines, int start, int end) {
        List<int[]> chain = findIfElseChain(lines, start, end);
        int blockEnd = chain.get(0)[0];
        int elsePos = -1;
        
        // Find first else (not else if)
        for (int i = 1; i < chain.size(); i++) {
            if (chain.get(i)[1] == 1) { // else
                elsePos = chain.get(i)[0];
                break;
            } else if (chain.get(i)[1] == 0) { // else if - treat as else for legacy
                elsePos = chain.get(i)[0];
                break;
            }
        }
        
        return new int[]{blockEnd, elsePos};
    }
    
    /**
     * Execute increment for for loop
     */
    private void executeIncrement(String increment) {
        increment = increment.trim();
        
        // i++ or i--
        if (increment.endsWith("++")) {
            String varName = increment.substring(0, increment.length() - 2).trim();
            String value = variables.get(varName);
            if (value != null) {
                try {
                    int intVal = Integer.parseInt(value);
                    String newValue = String.valueOf(intVal + 1);
                    variables.put(varName, newValue);
                    ScriptInterpreter.getInstance().setVariable(varName, newValue);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        } else if (increment.endsWith("--")) {
            String varName = increment.substring(0, increment.length() - 2).trim();
            String value = variables.get(varName);
            if (value != null) {
                try {
                    int intVal = Integer.parseInt(value);
                    String newValue = String.valueOf(intVal - 1);
                    variables.put(varName, newValue);
                    ScriptInterpreter.getInstance().setVariable(varName, newValue);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        } else {
            // i = i + 1 or similar
            Matcher varMatcher = VARIABLE_PATTERN.matcher(increment);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String expression = processVariables(varMatcher.group(2).trim());
                // Simple calculation
                try {
                    int result = evaluateSimpleExpression(expression);
                    String newValue = String.valueOf(result);
                    variables.put(varName, newValue);
                    ScriptInterpreter.getInstance().setVariable(varName, newValue);
                } catch (Exception e) {
                    variables.put(varName, expression);
                    ScriptInterpreter.getInstance().setVariable(varName, expression);
                }
            }
        }
    }
    
    /**
     * Evaluate simple arithmetic expression
     */
    private int evaluateSimpleExpression(String expr) {
        expr = expr.trim();
        
        // Try simple number
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            // Continue
        }
        
        // Try modulo
        if (expr.contains("%")) {
            String[] parts = expr.split("%");
            if (parts.length == 2) {
                int left = Integer.parseInt(parts[0].trim());
                int right = Integer.parseInt(parts[1].trim());
                return left % right;
            }
        }
        
        // Try addition
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            int sum = 0;
            for (String part : parts) {
                sum += Integer.parseInt(part.trim());
            }
            return sum;
        }
        
        // Try subtraction
        if (expr.contains("-") && !expr.startsWith("-")) {
            String[] parts = expr.split("-");
            int result = Integer.parseInt(parts[0].trim());
            for (int i = 1; i < parts.length; i++) {
                result -= Integer.parseInt(parts[i].trim());
            }
            return result;
        }
        
        // Try multiplication
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            int result = 1;
            for (String part : parts) {
                result *= Integer.parseInt(part.trim());
            }
            return result;
        }
        
        // Try division
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            if (parts.length == 2) {
                int left = Integer.parseInt(parts[0].trim());
                int right = Integer.parseInt(parts[1].trim());
                return left / right;
            }
        }
        
        throw new NumberFormatException("Cannot evaluate: " + expr);
    }
    
    /**
     * Process variables in string
     */
    private String processVariables(String line) {
        String result = line;
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        // Process environment variables ($PLAYER_X etc.)
        Matcher envMatcher = ENV_VAR_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (envMatcher.find()) {
            String varName = envMatcher.group(1);
            EnvironmentVariable envVar = interpreter.getEnvironmentVariable(varName);
            if (envVar != null && envVar.getValue() != null && !envVar.getValue().isEmpty()) {
                envMatcher.appendReplacement(sb, Matcher.quoteReplacement(envVar.getValue()));
            }
        }
        envMatcher.appendTail(sb);
        result = sb.toString();
        
        // Process user variables from ScriptInterpreter ($varname from commands like vision, scan)
        Map<String, String> interpreterContext = interpreter.getContext();
        for (Map.Entry<String, String> entry : interpreterContext.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                String pattern = "\\$" + Pattern.quote(entry.getKey());
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }
        
        // Process local script variables ($varname)
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                String pattern = "\\$" + Pattern.quote(entry.getKey());
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }
        
        return result;
    }
    
    /**
     * Evaluate condition
     */
    private boolean evaluateCondition(String condition) {
        try {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                EnvironmentVariable healthVar = ScriptInterpreter.getInstance().getEnvironmentVariable("PLAYER_HEALTH");
                String healthValue = healthVar != null ? healthVar.getValue() : "null";
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.evaluateCondition:910\",\"message\":\"Evaluating condition\",\"data\":{\"taskId\":" + id + ",\"condition\":\"" + condition + "\",\"healthValue\":\"" + healthValue + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            String processed = processVariables(condition);
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.evaluateCondition:912\",\"message\":\"Condition processed\",\"data\":{\"taskId\":" + id + ",\"original\":\"" + condition + "\",\"processed\":\"" + processed + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            
            // Also replace variables WITHOUT $ prefix (for Rust-style syntax: if x > 3)
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String varName = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    // Replace standalone variable names (word boundaries)
                    processed = processed.replaceAll("\\b" + Pattern.quote(varName) + "\\b", Matcher.quoteReplacement(value));
                }
            }
            // Also check ScriptInterpreter variables
            Map<String, String> interpreterVars = ScriptInterpreter.getInstance().getVariables();
            for (Map.Entry<String, String> entry : interpreterVars.entrySet()) {
                String varName = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    processed = processed.replaceAll("\\b" + Pattern.quote(varName) + "\\b", Matcher.quoteReplacement(value));
                }
            }
            
            // Boolean literals
            String trimmed = processed.trim().toLowerCase();
            if (trimmed.equals("true")) return true;
            if (trimmed.equals("false")) return false;
            
            // Handle logical AND (&&)
            if (processed.contains("&&")) {
                String[] parts = processed.split("&&");
                for (String part : parts) {
                    if (!evaluateCondition(part.trim())) {
                        return false;
                    }
                }
                return true;
            }
            
            // Handle logical OR (||)
            if (processed.contains("||")) {
                String[] parts = processed.split("\\|\\|");
                for (String part : parts) {
                    if (evaluateCondition(part.trim())) {
                        return true;
                    }
                }
                return false;
            }
            
            // Comparison operators
            String[] operators = {">=", "<=", "!=", "==", ">", "<"};
            for (String op : operators) {
                if (processed.contains(op)) {
                    String[] parts = processed.split(Pattern.quote(op), 2);
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim();
                        
                        // Remove surrounding parentheses if present
                        // Remove leading ( and trailing ) independently
                        while (left.startsWith("(")) {
                            left = left.substring(1).trim();
                        }
                        while (left.endsWith(")")) {
                            left = left.substring(0, left.length() - 1).trim();
                        }
                        while (right.startsWith("(")) {
                            right = right.substring(1).trim();
                        }
                        while (right.endsWith(")")) {
                            right = right.substring(0, right.length() - 1).trim();
                        }
                        
                        // Evaluate expressions on both sides (for things like i % 8 == 0)
                        double leftNum = evaluateExpressionAsDouble(left);
                        double rightNum = evaluateExpressionAsDouble(right);
                        
                        if (!Double.isNaN(leftNum) && !Double.isNaN(rightNum)) {
                            boolean result = switch (op) {
                                case ">=" -> leftNum >= rightNum;
                                case "<=" -> leftNum <= rightNum;
                                case "!=" -> leftNum != rightNum;
                                case "==" -> Math.abs(leftNum - rightNum) < 0.0001;
                                case ">" -> leftNum > rightNum;
                                case "<" -> leftNum < rightNum;
                                default -> false;
                            };
                            // #region agent log
                            try {
                                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.evaluateCondition:1005\",\"message\":\"Condition result\",\"data\":{\"taskId\":" + id + ",\"condition\":\"" + condition + "\",\"left\":\"" + left + "\",\"right\":\"" + right + "\",\"leftNum\":" + leftNum + ",\"rightNum\":" + rightNum + ",\"op\":\"" + op + "\",\"result\":" + result + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                                fw.close();
                            } catch (Exception e) {}
                            // #endregion
                            return result;
                        } else {
                            return switch (op) {
                                case "==" -> left.equalsIgnoreCase(right);
                                case "!=" -> !left.equalsIgnoreCase(right);
                                default -> false;
                            };
                        }
                    }
                }
            }
            
            // Truthy check
            return !trimmed.isEmpty() && !trimmed.equals("0") && !trimmed.equals("null");
            
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Error evaluating condition: " + condition + " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluates an expression and returns a double, or NaN if not a number
     */
    private double evaluateExpressionAsDouble(String expr) {
        expr = expr.trim();
        
        // Try simple number first
        try {
            return Double.parseDouble(expr.replace(',', '.'));
        } catch (NumberFormatException e) {
            // Continue
        }
        
        // Try modulo
        if (expr.contains("%")) {
            String[] parts = expr.split("%");
            if (parts.length == 2) {
                try {
                    int left = Integer.parseInt(parts[0].trim());
                    int right = Integer.parseInt(parts[1].trim());
                    return left % right;
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
            }
        }
        
        // Try addition
        if (expr.contains("+") && !expr.startsWith("+")) {
            try {
                String[] parts = expr.split("\\+");
                double sum = 0;
                for (String part : parts) {
                    sum += Double.parseDouble(part.trim().replace(',', '.'));
                }
                return sum;
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        
        // Try subtraction
        if (expr.contains("-") && !expr.startsWith("-")) {
            try {
                String[] parts = expr.split("-");
                double result = Double.parseDouble(parts[0].trim().replace(',', '.'));
                for (int i = 1; i < parts.length; i++) {
                    result -= Double.parseDouble(parts[i].trim().replace(',', '.'));
                }
                return result;
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        
        // Try multiplication
        if (expr.contains("*")) {
            try {
                String[] parts = expr.split("\\*");
                double result = 1;
                for (String part : parts) {
                    result *= Double.parseDouble(part.trim().replace(',', '.'));
                }
                return result;
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
        
        // Try division
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim().replace(',', '.'));
                    double right = Double.parseDouble(parts[1].trim().replace(',', '.'));
                    return left / right;
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
            }
        }
        
        return Double.NaN;
    }
    
    /**
     * Parses a line into command and arguments, respecting quotes
     */
    private List<String> parseArguments(String line) {
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
     * Add command to task queue
     */
    public void queueCommand(Command command, String[] args) {
        commandQueue.add(new CommandEntry(command, args));
    }

    private void processNextCommand() {
        // Synchronize to prevent concurrent execution
        synchronized (processLock) {
            // Prevent concurrent execution - if already processing, return
            if (isProcessingCommand) {
                return;
            }
            
            if (state != ScriptState.RUNNING) {
                return;
            }
            
            // Don't stop if queue is empty but we have a pending loop marker
            if (commandQueue.isEmpty()) {
                if (pendingLoopMarker == null) {
                    ScriptLogger.getInstance().debug("Task " + id + " (" + name + "): Queue empty, no pending loop marker, stopping. State was: " + state);
                state = ScriptState.STOPPED;
                } else {
                    ScriptLogger.getInstance().debug("Task " + id + " (" + name + "): Queue empty but pending loop marker exists (startLine=" + pendingLoopMarker.startLine + ", endLine=" + pendingLoopMarker.endLine + "), will re-queue on next tick");
                    // Queue is empty but we have pending loop marker - tick() will re-queue it
            }
            return;
        }

        // Update environment variables before each command
        ScriptInterpreter.getInstance().updateEnvironmentVariables();

        isProcessingCommand = true;
        CommandEntry entry = commandQueue.poll();
        executedCommands++;
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1007\",\"message\":\"Processing command\",\"data\":{\"taskId\":" + id + ",\"commandName\":\"" + (entry != null ? entry.command.getName() : "null") + "\",\"queueSize\":" + commandQueue.size() + ",\"pendingLoopMarker\":" + (pendingLoopMarker != null) + ",\"isProcessingCommand\":" + isProcessingCommand + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
            fw.close();
        } catch (Exception e) {}
        // #endregion

        // Handle loop marker specially
        if (entry.command instanceof LoopMarkerCommand) {
            LoopMarkerCommand loopCmd = (LoopMarkerCommand) entry.command;
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1202\",\"message\":\"Processing LoopMarkerCommand\",\"data\":{\"taskId\":" + id + ",\"startLine\":" + loopCmd.startLine + ",\"endLine\":" + loopCmd.endLine + ",\"queueSize\":" + commandQueue.size() + ",\"shouldBreak\":" + shouldBreak + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
                
                // Check queue size to prevent overflow
                if (commandQueue.size() > MAX_QUEUE_SIZE) {
                    ScriptLogger.getInstance().warn("Task " + id + ": Command queue overflow, stopping loop");
                    state = ScriptState.ERROR;
                    lastError = "Command queue overflow (>" + MAX_QUEUE_SIZE + " commands)";
                    isProcessingCommand = false;
                    return;
                }
                
                // Reset break/continue flags before re-parsing loop
                boolean hadBreak = shouldBreak;
                boolean hadContinue = shouldContinue;
                shouldBreak = false;
                shouldContinue = false;
                
                if (hadBreak || hadContinue) {
                    ScriptLogger.getInstance().debug("Task " + id + ": Resetting loop flags (hadBreak=" + hadBreak + ", hadContinue=" + hadContinue + ")");
                }
                
                // Store loop marker for later re-queuing (don't add immediately)
                pendingLoopMarker = loopCmd;
                ScriptLogger.getInstance().debug("Task " + id + ": Stored pending loop marker (startLine=" + loopCmd.startLine + ", endLine=" + loopCmd.endLine + ")");
                // #region agent log
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                    fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1034\",\"message\":\"Stored pending loop marker\",\"data\":{\"taskId\":" + id + ",\"startLine\":" + loopCmd.startLine + ",\"endLine\":" + loopCmd.endLine + ",\"queueSize\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
                    fw.close();
                } catch (Exception e) {}
                // #endregion
            
            // Re-parse the loop body and add commands to queue
            int queueSizeBefore = commandQueue.size();
            parseLines(loopCmd.lines, loopCmd.startLine, loopCmd.endLine);
            int queueSizeAfter = commandQueue.size();
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1236\",\"message\":\"Loop iteration parsed\",\"data\":{\"taskId\":" + id + ",\"startLine\":" + loopCmd.startLine + ",\"endLine\":" + loopCmd.endLine + ",\"queueSizeBefore\":" + queueSizeBefore + ",\"queueSizeAfter\":" + queueSizeAfter + ",\"commandsQueued\":" + (queueSizeAfter - queueSizeBefore) + ",\"shouldBreak\":" + shouldBreak + ",\"shouldContinue\":" + shouldContinue + ",\"pendingLoopMarker\":" + (pendingLoopMarker != null) + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"E\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            ScriptLogger.getInstance().debug("Task " + id + ": Loop iteration parsed, queued " + (queueSizeAfter - queueSizeBefore) + " commands, shouldBreak=" + shouldBreak + ", shouldContinue=" + shouldContinue + ", pendingLoopMarker=" + (pendingLoopMarker != null));
            
                // If break was triggered, clear pending loop marker
                if (shouldBreak) {
                    ScriptLogger.getInstance().debug("Task " + id + ": Break triggered in loop, clearing pending loop marker");
                    pendingLoopMarker = null;
                    shouldBreak = false;
                }
            
                // Don't re-add marker immediately - it will be added when queue is nearly empty
            isProcessingCommand = false;
            processNextCommand();
            return;
        }
        
        // Handle break command
        if (entry.command instanceof BreakCommand) {
            ScriptLogger.getInstance().debug("Task " + id + ": Break command executed");
            shouldBreak = true;
            pendingLoopMarker = null; // Clear pending loop marker
            isProcessingCommand = false;
            // Continue processing next command (which will skip loop)
            processNextCommand();
            return;
        }
        
        // Handle continue command
        if (entry.command instanceof ContinueCommand) {
            ScriptLogger.getInstance().debug("Task " + id + ": Continue command executed, clearing queue (pendingLoopMarker=" + (pendingLoopMarker != null) + ")");
            shouldContinue = true;
            // Clear queue until next loop iteration
            commandQueue.clear();
            // Note: pendingLoopMarker is NOT cleared here - it will be re-added by tick() if loop is still active
            isProcessingCommand = false;
            // Don't process next command immediately - let tick() handle loop re-queuing
            return;
        }
        
        // Handle conditional command synchronously
        if (entry.command instanceof ConditionalCommand) {
            // Update environment variables BEFORE evaluating conditions
            // This ensures conditions use current values (e.g., current health)
            ScriptInterpreter.getInstance().updateEnvironmentVariables();
            
            ConditionalCommand condCmd = (ConditionalCommand) entry.command;
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1108\",\"message\":\"Processing ConditionalCommand\",\"data\":{\"taskId\":" + id + ",\"queueSizeBefore\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            // Execute synchronously to ensure commands are added to queue before continuing
            condCmd.execute(new String[0]);
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1115\",\"message\":\"ConditionalCommand executed\",\"data\":{\"taskId\":" + id + ",\"queueSizeAfter\":" + commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            isProcessingCommand = false;
            // Continue processing next command (which should be from the conditional block)
            processNextCommand();
            return;
        }

        // Process variables at execution time (not parse time)
        String[] processedArgs = new String[entry.args.length];
        for (int i = 0; i < entry.args.length; i++) {
            processedArgs[i] = processVariables(entry.args[i]);
        }
            
            // Release lock before async execution
            final CommandEntry finalEntry = entry;
            final String[] finalArgs = processedArgs;

        try {
                currentCommandFuture = finalEntry.command.executeAsync(finalArgs);
            currentCommandFuture
                .exceptionally(throwable -> {
                    lastError = throwable.getMessage();
                    ScriptLogger.getInstance().error("Task " + id + " command error: " + throwable.getMessage());
                        // Don't stop script on command error, just log it
                    return null;
                })
                .thenRun(() -> {
                        synchronized (processLock) {
                    isProcessingCommand = false;
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ScriptTask.processNextCommand:1101\",\"message\":\"Command completed\",\"data\":{\"taskId\":" + id + ",\"commandName\":\"" + finalEntry.command.getName() + "\",\"queueSize\":" + commandQueue.size() + ",\"pendingLoopMarker\":" + (pendingLoopMarker != null) + ",\"isProcessingCommand\":" + isProcessingCommand + ",\"state\":\"" + state + "\"},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (Exception e) {}
                        // #endregion
                        }
                        
                    if (state == ScriptState.RUNNING) {
                        processNextCommand();
                        } else if (state == ScriptState.STOPPED || state == ScriptState.ERROR) {
                            synchronized (processLock) {
                                // Script stopped, clear queue and pending loop marker
                                commandQueue.clear();
                                pendingLoopMarker = null;
                            }
                    }
                });
        } catch (Exception e) {
                synchronized (processLock) {
            lastError = e.getMessage();
            isProcessingCommand = false;
            ScriptLogger.getInstance().error("Task " + id + " execution error: " + e.getMessage());
                    // Don't stop script on exception, just log it and continue
                    if (state == ScriptState.RUNNING) {
                        processNextCommand();
                    }
                }
            }
        }
    }
    
    /**
     * Internal marker command for loop blocks
     */
    private static class LoopMarkerCommand implements Command {
        final String[] lines;
        final int startLine;
        final int endLine;
        
        LoopMarkerCommand(String[] lines, int startLine, int endLine) {
            this.lines = lines;
            this.startLine = startLine;
            this.endLine = endLine;
        }
        
        @Override
        public String getName() { return "__loop_marker__"; }
        
        @Override
        public String getDescription() { return "Internal loop marker"; }
        
        @Override
        public String getParameters() { return ""; }
        
        @Override
        public void execute(String[] args) { }
    }

    /**
     * Internal command for break statement
     */
    private class BreakCommand implements Command {
        @Override
        public String getName() { return "__break__"; }
        
        @Override
        public String getDescription() { return "Internal break command"; }
        
        @Override
        public String getParameters() { return ""; }
        
        @Override
        public void execute(String[] args) {
            shouldBreak = true;
        }
    }
    
    /**
     * Internal command for continue statement
     */
    private class ContinueCommand implements Command {
        @Override
        public String getName() { return "__continue__"; }
        
        @Override
        public String getDescription() { return "Internal continue command"; }
        
        @Override
        public String getParameters() { return ""; }
        
        @Override
        public void execute(String[] args) {
            shouldContinue = true;
        }
    }

    /**
     * Internal command for conditional blocks (if/else if/else)
     * Evaluates conditions at execution time, not parse time
     */
    private static class ConditionalCommand implements Command {
        final ScriptTask task;
        final String[] lines;
        final List<ConditionalBlock> blocks;
        
        ConditionalCommand(ScriptTask task, String[] lines, List<ConditionalBlock> blocks) {
            this.task = task;
            this.lines = lines;
            this.blocks = blocks;
        }
        
        @Override
        public String getName() { return "__conditional__"; }
        
        @Override
        public String getDescription() { return "Internal conditional command"; }
        
        @Override
        public String getParameters() { return ""; }
        
        @Override
        public void execute(String[] args) {
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1306\",\"message\":\"Evaluating conditional blocks\",\"data\":{\"taskId\":" + task.id + ",\"blocksCount\":" + blocks.size() + ",\"queueSizeBefore\":" + task.commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                fw.close();
            } catch (Exception e) {}
            // #endregion
            for (ConditionalBlock block : blocks) {
                if (block.condition == null) {
                    // else block - always execute
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        StringBuilder blockLines = new StringBuilder();
                        for (int li = block.startLine; li < block.endLine && li < lines.length; li++) {
                            blockLines.append("\"").append(li + 1).append(":").append(lines[li].trim()).append("\",");
                        }
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1339\",\"message\":\"Executing else block\",\"data\":{\"taskId\":" + task.id + ",\"startLine\":" + block.startLine + ",\"endLine\":" + block.endLine + ",\"queueSizeBefore\":" + task.commandQueue.size() + ",\"blockLines\":[" + blockLines.toString() + "]},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    task.parseLines(lines, block.startLine, block.endLine);
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1352\",\"message\":\"Else block parsed\",\"data\":{\"taskId\":" + task.id + ",\"queueSizeAfter\":" + task.commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    break;
                } else {
                    // if or else if - evaluate condition
                    boolean conditionResult = task.evaluateCondition(block.condition);
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                        fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1320\",\"message\":\"Evaluating condition\",\"data\":{\"taskId\":" + task.id + ",\"condition\":\"" + block.condition + "\",\"result\":" + conditionResult + ",\"startLine\":" + block.startLine + ",\"endLine\":" + block.endLine + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                        fw.close();
                    } catch (Exception e) {}
                    // #endregion
                    if (conditionResult) {
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1325\",\"message\":\"Executing conditional block\",\"data\":{\"taskId\":" + task.id + ",\"queueSizeBefore\":" + task.commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (Exception e) {}
                        // #endregion
                        task.parseLines(lines, block.startLine, block.endLine);
                        // #region agent log
                        try {
                            java.io.FileWriter fw = new java.io.FileWriter("c:\\Users\\kasperenok\\Desktop\\projects\\kashub\\.cursor\\debug.log", true);
                            fw.write("{\"timestamp\":" + System.currentTimeMillis() + ",\"location\":\"ConditionalCommand.execute:1332\",\"message\":\"Conditional block parsed\",\"data\":{\"taskId\":" + task.id + ",\"queueSizeAfter\":" + task.commandQueue.size() + "},\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"A\"}\n");
                            fw.close();
                        } catch (Exception e) {}
                        // #endregion
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Represents a conditional block (if, else if, or else)
     */
    private static class ConditionalBlock {
        final String condition; // null for else blocks
        final int startLine;
        final int endLine;
        
        ConditionalBlock(String condition, int startLine, int endLine) {
            this.condition = condition;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    // State management
    public void pause() {
        if (state == ScriptState.RUNNING) {
            state = ScriptState.PAUSED;
            ScriptLogger.getInstance().info("Task " + id + " (" + name + ") paused");
        }
    }

    public void resume() {
        if (state == ScriptState.PAUSED) {
            state = ScriptState.RUNNING;
            ScriptLogger.getInstance().info("Task " + id + " (" + name + ") resumed");
        }
    }

    public void stop() {
        state = ScriptState.STOPPED;
        commandQueue.clear();
        pendingLoopMarker = null; // Clear pending loop marker
        shouldBreak = false; // Reset break flag
        shouldContinue = false; // Reset continue flag
        if (currentCommandFuture != null && !currentCommandFuture.isDone()) {
            currentCommandFuture.cancel(true);
        }
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") stopped, loop state cleared");
    }

    public void restart() {
        stop();
        commandQueue.clear();
        currentLine = 0;
        executedCommands = 0;
        lastError = null;
        state = ScriptState.RUNNING;
        // Reset all loop control flags
        shouldBreak = false;
        shouldContinue = false;
        pendingLoopMarker = null;
        isProcessingCommand = false;
        parseAndQueue();
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") restarted, loop state reset");
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public Set<String> getTags() { return Collections.unmodifiableSet(tags); }
    public ScriptState getState() { return state; }
    public long getStartTime() { return startTime; }
    public long getLastTickTime() { return lastTickTime; }
    public String getLastError() { return lastError; }
    public int getPriority() { return priority; }
    public int getCurrentLine() { return currentLine; }
    public int getExecutedCommands() { return executedCommands; }
    public int getQueuedCommands() { return commandQueue.size(); }
    public ScriptType getScriptType() { return scriptType; }
    
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getUptimeFormatted() {
        long uptime = getUptime();
        long seconds = (uptime / 1000) % 60;
        long minutes = (uptime / (1000 * 60)) % 60;
        long hours = uptime / (1000 * 60 * 60);
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    public void setPriority(int priority) { this.priority = priority; }
    public void addTag(String tag) { tags.add(tag); }
    public void removeTag(String tag) { tags.remove(tag); }
    public boolean hasTag(String tag) { return tags.contains(tag); }

    /**
     * Internal class for storing command and arguments
     */
    private static class CommandEntry {
        final Command command;
        final String[] args;

        CommandEntry(Command command, String[] args) {
            this.command = command;
            this.args = args;
        }
    }
    
    /**
     * Internal class for storing function definition
     */
    private static class FunctionDef {
        final String name;
        final List<String> parameters;
        final String[] lines;
        final int startLine;
        final int endLine;
        
        FunctionDef(String name, List<String> parameters, String[] lines, int startLine, int endLine) {
            this.name = name;
            this.parameters = parameters;
            this.lines = lines;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}

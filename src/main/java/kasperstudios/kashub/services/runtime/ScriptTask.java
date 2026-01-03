package kasperstudios.kashub.services.runtime;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.EnvironmentVariable;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.debug.DebugManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    private final Deque<CommandEntry> commandQueue;
    private volatile boolean isProcessingCommand;
    private CompletableFuture<Void> currentCommandFuture;
    private final Object processLock = new Object();

    private LoopMarkerCommand pendingLoopMarker = null;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int LOOP_REQUEUE_THRESHOLD = 5;

    private final Map<String, String> variables = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> registeredEvents = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final Pattern VARIABLE_PATTERN;
    private static final Pattern IF_PATTERN;
    private static final Pattern ELSE_IF_PATTERN;
    private static final Pattern ELSE_PATTERN;
    private static final Pattern FOR_PATTERN;
    private static final Pattern WHILE_PATTERN;
    private static final Pattern LOOP_PATTERN;
    private static final Pattern FUNCTION_PATTERN;
    private static final Pattern FUNCTION_CALL_PATTERN;
    private static final Pattern ENV_VAR_PATTERN;
    private static final Pattern USER_VAR_PATTERN;

    static {
        try {
            VARIABLE_PATTERN = Pattern.compile("^\\s*(?:let\\s+|const\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
            IF_PATTERN = Pattern.compile("^\\s*if\\s+(.+?)\\s*\\{\\s*$|^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$");
            ELSE_IF_PATTERN = Pattern.compile(
                    "^\\s*\\}?\\s*else\\s+if\\s+(.+?)\\s*\\{\\s*$|^\\s*\\}?\\s*else\\s+if\\s*\\((.*)\\)\\s*\\{?\\s*$");
            ELSE_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s*\\{?\\s*$");
            FOR_PATTERN = Pattern.compile("^\\s*for\\s*\\((.*)\\)\\s*\\{?\\s*$");
            WHILE_PATTERN = Pattern.compile("^\\s*while\\s+(.+?)\\s*\\{\\s*$|^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$");
            LOOP_PATTERN = Pattern.compile("^\\s*loop(?:\\s+(\\d+))?\\s*\\{?\\s*$");
            FUNCTION_PATTERN = Pattern.compile("^\\s*function\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*\\{?\\s*$");
            FUNCTION_CALL_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*$");
            ENV_VAR_PATTERN = Pattern.compile("\\$([A-Z_][A-Z0-9_]*)");
            USER_VAR_PATTERN = Pattern.compile("\\$([a-z_][a-z0-9_]*)");
        } catch (Throwable t) {
            System.err.println("CRITICAL: Error initializing ScriptTask regex patterns: " + t.getMessage());
            t.printStackTrace();
            throw t;
        }
    }

    private final Map<String, FunctionDef> localFunctions = new java.util.concurrent.ConcurrentHashMap<>();

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
        this.commandQueue = new ConcurrentLinkedDeque<>();
        this.isProcessingCommand = false;

        this.shouldBreak = false;
        this.shouldContinue = false;
        this.pendingLoopMarker = null;

        this.variables.put("SCRIPT_NAME", name);
        this.variables.put("SCRIPT_PATH", name + ".kashub");

        try {
            ScriptInterpreter.getInstance().setVariable("SCRIPT_NAME", name);
        } catch (Exception e) {

        }

        try {
            ScriptLogger.getInstance().debug("Task " + id + " (" + name + ") created, loop flags initialized");
        } catch (Exception e) {

        }
    }

    public void setVariable(String name, String value) {
        variables.put(name, value);

        try {
            ScriptInterpreter.getInstance().setVariable(name, value);
        } catch (Exception e) {

        }
    }

    public void tick() {
        try {
            if (state == null) {
                System.err.println("DEBUG: Task " + id + " state is NULL!");
                return;
            }
            if (state != ScriptState.RUNNING) {
                return;
            }

            lastTickTime = System.currentTimeMillis();

            if (!isProcessingCommand && !commandQueue.isEmpty()) {
                CommandEntry next = commandQueue.peek();
                if (next != null && next.getLineNumber() > 0) {
                    this.currentLine = next.getLineNumber();
                    try {
                        if (DebugManager.getInstance().shouldPause(this.id, this.name, this.currentLine)) {
                            return;
                        }
                    } catch (Throwable t) {
                        System.err.println("CRITICAL: Error in DebugManager.shouldPause: " + t.getMessage());
                    }
                }
            }

            if (pendingLoopMarker != null && !isProcessingCommand && commandQueue.isEmpty() && !shouldBreak) {
                try {
                    ScriptLogger.getInstance()
                            .debug("Task " + id + ": Re-queuing loop marker, queue empty (pendingLoopMarker="
                                    + (pendingLoopMarker != null) + ", isProcessingCommand=" + isProcessingCommand
                                    + ", queueSize=" + commandQueue.size() + ", shouldBreak=" + shouldBreak + ")");
                } catch (Throwable t) {
                }
                commandQueue.add(new CommandEntry(pendingLoopMarker, new String[0], pendingLoopMarker.startLine));
                pendingLoopMarker = null;

                processNextCommand();
                return;
            }

            if (!isProcessingCommand && !commandQueue.isEmpty()) {
                try {
                    ScriptLogger.getInstance()
                            .debug("Task " + id + ": tick() processing next command, queueSize=" + commandQueue.size());
                } catch (Throwable t) {
                }
                processNextCommand();
            } else if (pendingLoopMarker != null) {
                try {
                    ScriptLogger.getInstance()
                            .debug("Task " + id + ": tick() waiting for queue to empty (pendingLoopMarker="
                                    + (pendingLoopMarker != null) + ", isProcessingCommand=" + isProcessingCommand
                                    + ", queueSize=" + commandQueue.size() + ", shouldBreak=" + shouldBreak + ")");
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
            System.err.println("CRITICAL: Exception in tick(): " + t.getMessage());
            t.printStackTrace();
            throw t;
        }
    }

    public void parseAndQueue() {
        try {
            // Set script name in interpreter for export/import
            ScriptInterpreter.getInstance().setCurrentScriptName(name);
            
            String[] lines = code.split("\\r?\\n");

            shouldBreak = false;
            shouldContinue = false;
            pendingLoopMarker = null;
            commandQueue.clear();

            try {
                ScriptLogger.getInstance()
                        .debug("Parsing script " + name + " with " + lines.length + " lines, loop flags reset");
            } catch (Exception e) {

            }

            parseLines(lines, 0, lines.length);

            try {
                ScriptLogger.getInstance().info("Task " + id + " queued " + commandQueue.size() + " commands");
            } catch (Exception e) {

            }

        } catch (Exception e) {
            lastError = e.getMessage();
            state = ScriptState.ERROR;
            try {
                ScriptLogger.getInstance().error("Script " + name + " parse error: " + e.getMessage());
            } catch (Exception logEx) {

            }
        }
    }

    public void parseLines(String[] lines, int start, int end) {
        parseLinesToCollection(lines, start, end, commandQueue);
    }

    public void parseLinesToHead(String[] lines, int start, int end) {
        java.util.List<CommandEntry> temp = new java.util.ArrayList<>();
        parseLinesToCollection(lines, start, end, temp);

        for (int i = temp.size() - 1; i >= 0; i--) {
            commandQueue.addFirst(temp.get(i));
        }
    }

    private void parseLinesToCollection(String[] lines, int start, int end, java.util.Collection<CommandEntry> target) {
        int i = start;
        while (i < end && state != ScriptState.STOPPED && !shouldBreak) {

            if (shouldContinue) {
                shouldContinue = false;
                return;
            }

            String line = lines[i].trim();

            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }

            if (line.equals("}") || line.equals("end")) {
                i++;
                continue;
            }

            if (ELSE_PATTERN.matcher(line).matches() || ELSE_IF_PATTERN.matcher(line).matches()) {
                i++;
                continue;
            }

            currentLine = i + 1;

            currentLine = i + 1;

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

                StringBuilder funcBody = new StringBuilder();
                for (int j = i + 1; j < blockEnd; j++) {
                    funcBody.append(lines[j]).append("\n");
                }
                try {
                    ScriptInterpreter.getInstance().setVariable("__func_" + funcName, "defined");
                } catch (Exception e) {

                }

                i = blockEnd + 1;
                continue;
            }

            Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
            if (funcCallMatcher.find()) {
                String funcName = funcCallMatcher.group(1);
                String argsStr = funcCallMatcher.group(2);

                FunctionDef func = localFunctions.get(funcName);
                if (func != null) {

                    List<String> arguments = new ArrayList<>();
                    if (!argsStr.trim().isEmpty()) {
                        for (String arg : argsStr.split(",")) {
                            arguments.add(processVariables(arg.trim()));
                        }
                    }

                    Map<String, String> savedVars = new HashMap<>(variables);

                    for (int j = 0; j < func.parameters.size() && j < arguments.size(); j++) {
                        String paramValue = arguments.get(j);

                        if (paramValue.startsWith("\"") && paramValue.endsWith("\"")) {
                            paramValue = paramValue.substring(1, paramValue.length() - 1);
                        }
                        variables.put(func.parameters.get(j), paramValue);
                        try {
                            ScriptInterpreter.getInstance().setVariable(func.parameters.get(j), paramValue);
                        } catch (Exception e) {

                        }
                    }

                    parseLinesToCollection(func.lines, func.startLine, func.endLine, target);

                    variables.clear();
                    variables.putAll(savedVars);

                    i++;
                    continue;
                }

            }

            // Handle onEvent blocks
            if (line.toLowerCase().startsWith("onevent ") || line.toLowerCase().startsWith("onevent{")) {
                String eventLine = line.substring(7).trim(); // Remove "onEvent "
                int braceIndex = eventLine.indexOf('{');
                String eventName;
                if (braceIndex != -1) {
                    eventName = eventLine.substring(0, braceIndex).trim();
                } else {
                    eventName = eventLine.trim();
                }

                int blockEnd = findBlockEnd(lines, i + 1, end);
                StringBuilder eventScript = new StringBuilder();
                for (int j = i + 1; j < blockEnd; j++) {
                    eventScript.append(lines[j]).append("\n");
                }

                kasperstudios.kashub.algorithm.events.EventManager.getInstance()
                    .registerEventScript(eventName, eventScript.toString().trim());
                
                // Track this event for cleanup on stop
                registeredEvents.add(eventName);
                
                try {
                    ScriptLogger.getInstance().info("Registered event handler for: " + eventName);
                } catch (Exception e) {
                    // Ignore
                }

                i = blockEnd + 1;
                continue;
            }

            Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String varExpression = varMatcher.group(2).trim();
                target.add(new CommandEntry(new VariableCommand(varName, varExpression), new String[0], i + 1));
                i++;
                continue;
            }

            Matcher ifMatcher = IF_PATTERN.matcher(line);
            if (ifMatcher.find()) {

                String condition = ifMatcher.group(1) != null ? ifMatcher.group(1) : ifMatcher.group(2);

                int searchEnd = Math.min(end, lines.length);
                if (searchEnd < lines.length) {

                    searchEnd = Math.min(searchEnd + 5, lines.length);
                }
                List<int[]> chain = findIfElseChain(lines, i + 1, searchEnd);
                int blockEnd = chain.get(0)[0];

                int ifBlockEnd = blockEnd;
                if (chain.size() > 1) {
                    ifBlockEnd = chain.get(1)[0];
                }

                List<ConditionalBlock> conditionalBlocks = new ArrayList<>();
                conditionalBlocks.add(new ConditionalBlock(condition, i + 1, ifBlockEnd));

                for (int ci = 1; ci < chain.size(); ci++) {
                    int[] entry = chain.get(ci);
                    int pos = entry[0];
                    int type = entry[1];

                    if (type == 0) {
                        Matcher elseIfMatcher = ELSE_IF_PATTERN.matcher(lines[pos].trim());
                        if (elseIfMatcher.find()) {
                            String elseIfCondition = elseIfMatcher.group(1) != null ? elseIfMatcher.group(1)
                                    : elseIfMatcher.group(2);
                            int elseIfEnd = blockEnd;
                            if (ci + 1 < chain.size()) {
                                elseIfEnd = chain.get(ci + 1)[0];
                            }
                            conditionalBlocks.add(new ConditionalBlock(elseIfCondition, pos + 1, elseIfEnd));
                        }
                    } else if (type == 1) {
                        conditionalBlocks.add(new ConditionalBlock(null, pos + 1, blockEnd));
                    }
                }

                target.add(
                        new CommandEntry(new ConditionalCommand(this, lines, conditionalBlocks), new String[0], i + 1));
                i = blockEnd + 1;
                continue;
            }

            Matcher forMatcher = FOR_PATTERN.matcher(line);
            if (forMatcher.find()) {
                String forContent = forMatcher.group(1);
                String[] forParts = forContent.split(";");

                if (forParts.length == 3) {
                    String init = forParts[0].trim();
                    String condition = forParts[1].trim();
                    String increment = forParts[2].trim();

                    int blockEnd = findBlockEnd(lines, i + 1, end);

                    if (!init.isEmpty()) {
                        Matcher initMatcher = VARIABLE_PATTERN.matcher(init);
                        if (initMatcher.find()) {
                            String initValue = processVariables(initMatcher.group(2).trim());
                            variables.put(initMatcher.group(1), initValue);
                            try {
                                ScriptInterpreter.getInstance().setVariable(initMatcher.group(1), initValue);
                            } catch (Exception e) {

                            }
                        }
                    }

                    int maxIterations = 10000;
                    int iterations = 0;
                    shouldBreak = false;
                    while (evaluateCondition(condition) && iterations < maxIterations && !shouldBreak) {
                        parseLinesToCollection(lines, i + 1, blockEnd, target);

                        if (shouldBreak)
                            break;

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

            Matcher whileMatcher = WHILE_PATTERN.matcher(line);
            if (whileMatcher.find()) {

                String condition = whileMatcher.group(1) != null ? whileMatcher.group(1) : whileMatcher.group(2);
                int blockEnd = findBlockEnd(lines, i + 1, end);

                String trimmedCondition = condition.trim().toLowerCase();

                while (trimmedCondition.startsWith("(") && trimmedCondition.endsWith(")")) {
                    trimmedCondition = trimmedCondition.substring(1, trimmedCondition.length() - 1).trim();
                }
                if (trimmedCondition.equals("true")) {

                    ScriptLogger.getInstance().debug(
                            "Task " + id + ": Detected infinite while loop (while true), using LoopMarkerCommand");
                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    target.add(
                            new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd, null), new String[0],
                                    i + 1));
                } else {

                    ScriptLogger.getInstance()
                            .debug("Task " + id + ": Detected conditional while loop, condition: " + condition);
                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    target.add(
                            new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd, condition), new String[0],
                                    i + 1));
                }

                i = blockEnd + 1;
                continue;
            }

            Matcher loopMatcher = LOOP_PATTERN.matcher(line);
            if (loopMatcher.find()) {
                String countStr = loopMatcher.group(1);
                int blockEnd = findBlockEnd(lines, i + 1, end);

                if (countStr != null && !countStr.isEmpty()) {

                    int count = Integer.parseInt(countStr);
                    shouldBreak = false;
                    for (int iter = 0; iter < count && !shouldBreak && state == ScriptState.RUNNING; iter++) {
                        parseLinesToCollection(lines, i + 1, blockEnd, target);
                        if (shouldBreak)
                            break;
                    }
                    shouldBreak = false;
                } else {

                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    target.add(
                            new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd, null), new String[0],
                                    i + 1));
                }

                i = blockEnd + 1;
                continue;
            }

            String processedLine = processVariables(line);
            List<String> parts = parseArguments(processedLine);
            if (parts.isEmpty()) {
                i++;
                continue;
            }

            String commandName = parts.get(0).toLowerCase();
            String[] args = parts.subList(1, parts.size()).toArray(new String[0]);

            if (commandName.equals("break")) {
                ScriptLogger.getInstance().debug("Task " + id + ": Parsing break command at line " + currentLine);
                target.add(new CommandEntry(new BreakCommand(), new String[0], i + 1));
                i++;
                continue;
            }
            if (commandName.equals("continue")) {
                ScriptLogger.getInstance().debug("Task " + id + ": Parsing continue command at line " + currentLine);
                target.add(new CommandEntry(new ContinueCommand(), new String[0], i + 1));
                i++;
                continue;
            }

            Command command = CommandRegistry.getCommand(commandName);
            if (command != null) {
                target.add(new CommandEntry(command, args, i + 1));
                ScriptLogger.getInstance().debug("Queued command: " + commandName + " with " + args.length + " args");
            } else {
                ScriptLogger.getInstance().warn("Unknown command at line " + currentLine + ": " + commandName);
            }

            i++;
        }
    }

    private int findBlockEnd(String[] lines, int start, int end) {
        int level = 1;
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();

            if (line.startsWith("} else {") || (line.contains("} else") && line.contains("{"))) {

                level--;

            } else if (line.equals("}") || line.equals("end")) {
                level--;
            } else if (line.contains("}") && !line.contains("{")) {
                level--;
            }

            if (line.contains("{") ||
                    line.startsWith("if ") || line.startsWith("if(") ||
                    line.startsWith("for ") || line.startsWith("for(") ||
                    line.startsWith("while ") || line.startsWith("while(") ||
                    line.startsWith("loop ") || line.equals("loop") || line.equals("loop{") ||
                    line.startsWith("function ")) {

                if (line.contains("{") && !line.contains("}")) {
                    level++;
                } else if (line.contains("{") && line.contains("}")) {

                    level++;
                } else if (!line.contains("{") && !line.contains("}")) {

                    level++;
                }
            }

            if (level == 0)
                return i;
        }
        return end - 1;
    }

    private List<int[]> findIfElseChain(String[] lines, int start, int end) {
        List<int[]> chain = new ArrayList<>();
        int level = 1;

        for (int i = start; i < end; i++) {
            String line = lines[i].trim();

            boolean isElseIf = false;
            boolean isElse = false;
            boolean isElseOnSameLine = false;

            if (line.contains("} else")) {
                isElseOnSameLine = true;

                if (ELSE_IF_PATTERN.matcher(line).matches()) {
                    isElseIf = true;
                } else if (line.contains("} else") && !line.contains("if")) {

                    isElse = true;
                }
            } else if (level == 1 || level == 0) {

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

            if (isElseIf) {
                chain.add(new int[] { i, 0 });
            } else if (isElse) {
                chain.add(new int[] { i, 1 });
            }

            if (isElseOnSameLine) {

                if (line.contains("}") && !line.contains("{")) {
                    level--;
                } else if (line.startsWith("}") && line.contains("{")) {

                    level--;
                    if (line.contains("{") && !line.endsWith("}")) {
                        level++;
                    }
                }
            } else {

                if (line.equals("}") || line.equals("end")) {
                    level--;
                } else if (line.contains("}") && !line.contains("{")) {
                    level--;
                }
            }

            if (level == 0) {

                boolean foundElse = false;
                for (int j = i + 1; j < Math.min(i + 4, end); j++) {
                    String nextLine = lines[j].trim();
                    if (ELSE_IF_PATTERN.matcher(nextLine).matches() ||
                            (nextLine.contains("} else") && !nextLine.contains("if")) ||
                            (nextLine.equals("else") || nextLine.equals("else {"))) {
                        foundElse = true;
                        break;
                    }

                    if (!nextLine.isEmpty() && !nextLine.startsWith("//") &&
                            !nextLine.contains("else")) {
                        break;
                    }
                }

                if (!foundElse) {
                    chain.add(0, new int[] { i, -1 });
                    return chain;
                }

            }
        }
        chain.add(0, new int[] { end - 1, -1 });
        return chain;
    }

    private int[] findIfBlockEndWithElse(String[] lines, int start, int end) {
        List<int[]> chain = findIfElseChain(lines, start, end);
        int blockEnd = chain.get(0)[0];
        int elsePos = -1;

        for (int i = 1; i < chain.size(); i++) {
            if (chain.get(i)[1] == 1) {
                elsePos = chain.get(i)[0];
                break;
            } else if (chain.get(i)[1] == 0) {
                elsePos = chain.get(i)[0];
                break;
            }
        }

        return new int[] { blockEnd, elsePos };
    }

    private void executeIncrement(String increment) {
        increment = increment.trim();

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

                }
            }
        } else {

            Matcher varMatcher = VARIABLE_PATTERN.matcher(increment);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String expression = processVariables(varMatcher.group(2).trim());

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

    private int evaluateSimpleExpression(String expr) {
        expr = expr.trim();

        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {

        }

        if (expr.contains("%")) {
            String[] parts = expr.split("%");
            if (parts.length == 2) {
                int left = Integer.parseInt(parts[0].trim());
                int right = Integer.parseInt(parts[1].trim());
                return left % right;
            }
        }

        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            int sum = 0;
            for (String part : parts) {
                sum += Integer.parseInt(part.trim());
            }
            return sum;
        }

        if (expr.contains("-") && !expr.startsWith("-")) {
            String[] parts = expr.split("-");
            int result = Integer.parseInt(parts[0].trim());
            for (int i = 1; i < parts.length; i++) {
                result -= Integer.parseInt(parts[i].trim());
            }
            return result;
        }

        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            int result = 1;
            for (String part : parts) {
                result *= Integer.parseInt(part.trim());
            }
            return result;
        }

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

    private String processVariables(String line) {
        String result = line;
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();

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

        Map<String, String> interpreterContext = interpreter.getContext();
        for (Map.Entry<String, String> entry : interpreterContext.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                String pattern = "\\$" + Pattern.quote(entry.getKey());
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                String pattern = "\\$" + Pattern.quote(entry.getKey());
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }

        return result;
    }

    private boolean evaluateCondition(String condition) {
        try {
            String processed = processVariables(condition);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String varName = entry.getKey();
                String value = entry.getValue();
                if (value != null) {

                    processed = processed.replaceAll("\\b" + Pattern.quote(varName) + "\\b",
                            Matcher.quoteReplacement(value));
                }
            }

            Map<String, String> interpreterVars = ScriptInterpreter.getInstance().getVariables();
            for (Map.Entry<String, String> entry : interpreterVars.entrySet()) {
                String varName = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    processed = processed.replaceAll("\\b" + Pattern.quote(varName) + "\\b",
                            Matcher.quoteReplacement(value));
                }
            }

            String trimmed = processed.trim().toLowerCase();
            if (trimmed.equals("true"))
                return true;
            if (trimmed.equals("false"))
                return false;

            if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
                return evaluateCondition(trimmed.substring(1, trimmed.length() - 1));
            }

            if (processed.contains("&&")) {
                String[] parts = processed.split("&&");
                for (String part : parts) {
                    if (!evaluateCondition(part.trim())) {
                        return false;
                    }
                }
                return true;
            }

            if (processed.contains("||")) {
                String[] parts = processed.split("\\|\\|");
                for (String part : parts) {
                    if (evaluateCondition(part.trim())) {
                        return true;
                    }
                }
                return false;
            }

            String[] operators = { ">=", "<=", "!=", "==", ">", "<" };
            for (String op : operators) {
                if (processed.contains(op)) {
                    String[] parts = processed.split(Pattern.quote(op), 2);
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim();

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

            return !trimmed.isEmpty() && !trimmed.equals("0") && !trimmed.equals("null");

        } catch (Exception e) {
            ScriptLogger.getInstance().error("Error evaluating condition: " + condition + " - " + e.getMessage());
            return false;
        }
    }

    private double evaluateExpressionAsDouble(String expr) {
        expr = expr.trim();

        try {
            return Double.parseDouble(expr.replace(',', '.'));
        } catch (NumberFormatException e) {

        }

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

    public void queueCommand(Command command, String[] args) {
        commandQueue.add(new CommandEntry(command, args, -1));
    }

    private void processNextCommand() {

        synchronized (processLock) {

            if (isProcessingCommand) {
                return;
            }

            if (state != ScriptState.RUNNING) {
                return;
            }

            if (commandQueue.isEmpty()) {
                if (pendingLoopMarker == null) {
                    ScriptLogger.getInstance().debug("Task " + id + " (" + name
                            + "): Queue empty, no pending loop marker, stopping. State was: " + state);
                    state = ScriptState.STOPPED;
                } else {
                    ScriptLogger.getInstance().debug("Task " + id + " (" + name
                            + "): Queue empty but pending loop marker exists (startLine=" + pendingLoopMarker.startLine
                            + ", endLine=" + pendingLoopMarker.endLine + "), will re-queue on next tick");

                }
                return;
            }

            try {
                ScriptInterpreter.getInstance().updateEnvironmentVariables();
            } catch (Exception e) {

            }

            isProcessingCommand = true;
            CommandEntry entry = commandQueue.poll();
            executedCommands++;

            if (entry.command instanceof LoopMarkerCommand) {
                LoopMarkerCommand loopCmd = (LoopMarkerCommand) entry.command;

                if (loopCmd.condition != null) {
                    ScriptInterpreter.getInstance().updateEnvironmentVariables();
                    if (!evaluateCondition(loopCmd.condition)) {
                        ScriptLogger.getInstance().debug("Task " + id + ": Loop condition false, loop ending");
                        pendingLoopMarker = null;
                        isProcessingCommand = false;
                        return;
                    }
                }

                if (commandQueue.size() > MAX_QUEUE_SIZE) {
                    ScriptLogger.getInstance().warn("Task " + id + ": Command queue overflow, stopping loop");
                    state = ScriptState.ERROR;
                    lastError = "Command queue overflow (>" + MAX_QUEUE_SIZE + " commands)";
                    isProcessingCommand = false;
                    return;
                }

                boolean hadBreak = shouldBreak;
                boolean hadContinue = shouldContinue;
                shouldBreak = false;
                shouldContinue = false;

                if (hadBreak || hadContinue) {
                    ScriptLogger.getInstance().debug("Task " + id + ": Resetting loop flags (hadBreak=" + hadBreak
                            + ", hadContinue=" + hadContinue + ")");
                }

                pendingLoopMarker = loopCmd;
                ScriptLogger.getInstance().debug("Task " + id + ": Stored pending loop marker (startLine="
                        + loopCmd.startLine + ", endLine=" + loopCmd.endLine + ")");

                int queueSizeBefore = commandQueue.size();
                parseLinesToHead(loopCmd.lines, loopCmd.startLine, loopCmd.endLine);
                int queueSizeAfter = commandQueue.size();
                ScriptLogger.getInstance()
                        .debug("Task " + id + ": Loop iteration parsed, queued " + (queueSizeAfter - queueSizeBefore)
                                + " commands, shouldBreak=" + shouldBreak + ", shouldContinue=" + shouldContinue
                                + ", pendingLoopMarker=" + (pendingLoopMarker != null));

                if (shouldBreak) {
                    ScriptLogger.getInstance()
                            .debug("Task " + id + ": Break triggered in loop, clearing pending loop marker");
                    pendingLoopMarker = null;
                    shouldBreak = false;
                }

                isProcessingCommand = false;

                return;
            }

            if (entry.command instanceof BreakCommand) {
                ScriptLogger.getInstance().debug("Task " + id + ": Break command executed");
                shouldBreak = true;
                pendingLoopMarker = null;
                isProcessingCommand = false;

                processNextCommand();
                return;
            }

            if (entry.command instanceof ContinueCommand) {
                ScriptLogger.getInstance()
                        .debug("Task " + id + ": Continue command executed, clearing queue (pendingLoopMarker="
                                + (pendingLoopMarker != null) + ")");
                shouldContinue = true;

                commandQueue.clear();

                isProcessingCommand = false;

                return;
            }

            if (entry.command instanceof ConditionalCommand) {

                ScriptInterpreter.getInstance().updateEnvironmentVariables();

                ConditionalCommand condCmd = (ConditionalCommand) entry.command;

                condCmd.execute(new String[0]);
                isProcessingCommand = false;

                return;
            }

            String[] processedArgs = new String[entry.args.length];
            for (int i = 0; i < entry.args.length; i++) {
                processedArgs[i] = processVariables(entry.args[i]);
            }

            final CommandEntry finalEntry = entry;
            final String[] finalArgs = processedArgs;

            final long startTime = System.nanoTime();

            try {
                currentCommandFuture = finalEntry.command.executeAsync(finalArgs);
                currentCommandFuture
                        .exceptionally(throwable -> {
                            lastError = throwable.getMessage();
                            ScriptLogger.getInstance()
                                    .error("Task " + id + " command error: " + throwable.getMessage());
                            return null;
                        })
                        .thenRun(() -> {

                            long duration = System.nanoTime() - startTime;
                            kasperstudios.kashub.debug.ProfilerManager.getInstance()
                                    .recordCommand(finalEntry.command.getName(), duration);

                            synchronized (processLock) {
                                isProcessingCommand = false;
                            }

                            if (state == ScriptState.RUNNING) {

                                boolean shouldPause = false;
                                try {
                                    if (!commandQueue.isEmpty()) {
                                        CommandEntry next = commandQueue.peek();
                                        if (next != null && next.getLineNumber() > 0) {
                                            if (DebugManager.getInstance().shouldPause(id, name,
                                                    next.getLineNumber())) {
                                                shouldPause = true;
                                            }
                                        }
                                    }
                                } catch (Exception e) {

                                }

                                if (shouldPause) {

                                    return;
                                }
                                processNextCommand();
                            } else if (state == ScriptState.STOPPED || state == ScriptState.ERROR) {
                                synchronized (processLock) {
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
                    if (state == ScriptState.RUNNING) {
                        processNextCommand();
                    }
                }
            }
        }
    }

    private static class LoopMarkerCommand implements Command {
        final String[] lines;
        final int startLine;
        final int endLine;
        final String condition;

        LoopMarkerCommand(String[] lines, int startLine, int endLine, String condition) {
            this.lines = lines;
            this.startLine = startLine;
            this.endLine = endLine;
            this.condition = condition;
        }

        @Override
        public String getName() {
            return "__loop_marker__";
        }

        @Override
        public String getDescription() {
            return "Internal loop marker";
        }

        @Override
        public String getParameters() {
            return "";
        }

        @Override
        public void execute(String[] args) {
        }
    }

    private class BreakCommand implements Command {
        @Override
        public String getName() {
            return "__break__";
        }

        @Override
        public String getDescription() {
            return "Internal break command";
        }

        @Override
        public String getParameters() {
            return "";
        }

        @Override
        public void execute(String[] args) {
            shouldBreak = true;
        }
    }

    private class ContinueCommand implements Command {
        @Override
        public String getName() {
            return "__continue__";
        }

        @Override
        public String getDescription() {
            return "Internal continue command";
        }

        @Override
        public String getParameters() {
            return "";
        }

        @Override
        public void execute(String[] args) {
            shouldContinue = true;
        }
    }

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
        public String getName() {
            return "__conditional__";
        }

        @Override
        public String getDescription() {
            return "Internal conditional command";
        }

        @Override
        public String getParameters() {
            return "";
        }

        @Override
        public void execute(String[] args) {
            for (ConditionalBlock block : blocks) {
                if (block.condition == null) {

                    task.parseLinesToHead(lines, block.startLine, block.endLine);
                    break;
                } else {

                    boolean conditionResult = task.evaluateCondition(block.condition);
                    if (conditionResult) {
                        task.parseLinesToHead(lines, block.startLine, block.endLine);
                        break;
                    }
                }
            }
        }
    }

    private static class ConditionalBlock {
        final String condition;
        final int startLine;
        final int endLine;

        ConditionalBlock(String condition, int startLine, int endLine) {
            this.condition = condition;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    private class VariableCommand implements Command {
        private final String name;
        private final String expression;

        VariableCommand(String name, String expression) {
            this.name = name;
            this.expression = expression;
        }

        @Override
        public String getName() {
            return "var";
        }

        @Override
        public String getDescription() {
            return "Set variable " + name + " = " + expression;
        }

        @Override
        public String getParameters() {
            return name + " = " + expression;
        }

        @Override
        public void execute(String[] args) {
            String varValue = processVariables(expression.trim());

            if (varValue.startsWith("\"") && varValue.endsWith("\"")) {
                varValue = varValue.substring(1, varValue.length() - 1);
            } else {

                try {
                    double result = evaluateExpressionAsDouble(varValue);
                    if (!Double.isNaN(result)) {

                        if (result == Math.floor(result) && !Double.isInfinite(result)) {
                            varValue = String.valueOf((int) result);
                        } else {
                            varValue = String.valueOf(result);
                        }
                    }
                } catch (Exception e) {

                }
            }
            setVariable(name, varValue);
        }

    }

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
        pendingLoopMarker = null;
        shouldBreak = false;
        shouldContinue = false;
        localFunctions.clear();
        variables.clear();
        
        // Unregister all events registered by this script
        for (String eventName : registeredEvents) {
            kasperstudios.kashub.algorithm.events.EventManager.getInstance().unregisterEventScript(eventName);
            try {
                ScriptLogger.getInstance().debug("Unregistered event handler for: " + eventName);
            } catch (Exception e) {
                // Ignore
            }
        }
        registeredEvents.clear();
        
        // Clear exports from this script
        try {
            kasperstudios.kashub.algorithm.ExportManager.getInstance().clearScriptExports(name);
        } catch (Exception e) {
            // Ignore
        }
        
        if (currentCommandFuture != null && !currentCommandFuture.isDone()) {
            currentCommandFuture.cancel(true);
        }
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") stopped, all state cleared");
    }

    public void restart() {
        stop();

        state = ScriptState.RUNNING;
        lastError = null;
        executedCommands = 0;
        currentLine = 0;

        shouldBreak = false;
        shouldContinue = false;
        pendingLoopMarker = null;
        isProcessingCommand = false;
        
        // registeredEvents already cleared by stop(), will be repopulated by parseAndQueue()
        parseAndQueue();
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") restarted, loop state reset");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public ScriptState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastTickTime() {
        return lastTickTime;
    }

    public String getLastError() {
        return lastError;
    }

    public int getPriority() {
        return priority;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getExecutedCommands() {
        return executedCommands;
    }

    public int getQueuedCommands() {
        return commandQueue.size();
    }

    public boolean isProcessingCommand() {
        return isProcessingCommand;
    }

    public ScriptType getScriptType() {
        return scriptType;
    }

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

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public Map<String, String> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    private static class CommandEntry {
        final Command command;
        final String[] args;
        final int lineNumber;

        CommandEntry(Command command, String[] args) {
            this(command, args, -1);
        }

        CommandEntry(Command command, String[] args, int lineNumber) {
            this.command = command;
            this.args = args;
            this.lineNumber = lineNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

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

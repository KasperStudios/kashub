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

/**
 * Представляет запущенный скрипт как задачу
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
    
    // Очередь команд для этой задачи
    private final Queue<CommandEntry> commandQueue;
    private boolean isProcessingCommand;
    private CompletableFuture<Void> currentCommandFuture;
    
    // Переменные скрипта
    private final Map<String, String> variables = new HashMap<>();
    
    // Паттерны для парсинга
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern IF_PATTERN = Pattern.compile("^\\s*if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s+if\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s*\\{?\\s*$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^\\s*for\\s*\\((.*)\\)\\s*\\{?\\s*$");
    private static final Pattern WHILE_PATTERN = Pattern.compile("^\\s*while\\s*\\((.*)\\)\\s*\\{?\\s*$");
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
        this.commandQueue = new LinkedList<>();
        this.isProcessingCommand = false;
    }

    /**
     * Выполняет один тик скрипта
     */
    public void tick() {
        if (state != ScriptState.RUNNING) return;
        
        lastTickTime = System.currentTimeMillis();
        
        // Если нет активной команды, берём следующую из очереди
        if (!isProcessingCommand && !commandQueue.isEmpty()) {
            processNextCommand();
        }
    }

    /**
     * Парсит и ставит команды в очередь задачи
     */
    public void parseAndQueue() {
        try {
            String[] lines = code.split("\\r?\\n");
            
            ScriptLogger.getInstance().debug("Parsing script " + name + " with " + lines.length + " lines");
            
            parseLines(lines, 0, lines.length);
            
            ScriptLogger.getInstance().info("Task " + id + " queued " + commandQueue.size() + " commands");
            
        } catch (Exception e) {
            lastError = e.getMessage();
            state = ScriptState.ERROR;
            ScriptLogger.getInstance().error("Script " + name + " parse error: " + e.getMessage());
        }
    }
    
    /**
     * Рекурсивно парсит строки кода с поддержкой блоков
     */
    private void parseLines(String[] lines, int start, int end) {
        int i = start;
        while (i < end && state != ScriptState.STOPPED && !shouldBreak) {
            // Check for continue flag
            if (shouldContinue) {
                shouldContinue = false;
                return; // Exit current block iteration
            }
            
            String line = lines[i].trim();
            
            // Пропускаем пустые строки и комментарии
            if (line.isEmpty() || line.startsWith("//")) {
                i++;
                continue;
            }
            
            // Пропускаем закрывающие скобки и end
            if (line.equals("}") || line.equals("end")) {
                i++;
                continue;
            }
            
            // Пропускаем else и else if (обрабатывается в if)
            if (ELSE_PATTERN.matcher(line).matches() || ELSE_IF_PATTERN.matcher(line).matches()) {
                i++;
                continue;
            }
            
            currentLine = i + 1;
            
            // Проверяем на break
            if (line.equals("break")) {
                shouldBreak = true;
                return;
            }
            
            // Проверяем на continue
            if (line.equals("continue")) {
                shouldContinue = true;
                return;
            }
            
            // Проверяем на определение функции
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
            
            // Проверяем на вызов функции
            Matcher funcCallMatcher = FUNCTION_CALL_PATTERN.matcher(line);
            if (funcCallMatcher.find()) {
                String funcName = funcCallMatcher.group(1);
                String argsStr = funcCallMatcher.group(2);
                
                FunctionDef func = localFunctions.get(funcName);
                if (func != null) {
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
                    
                    // Restore variables
                    variables.clear();
                    variables.putAll(savedVars);
                    
                    i++;
                    continue;
                }
                // If function not found, fall through to command processing
            }
            
            // Проверяем на присваивание переменной
            Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String varValue = processVariables(varMatcher.group(2).trim());
                // Удаляем кавычки если есть
                if (varValue.startsWith("\"") && varValue.endsWith("\"")) {
                    varValue = varValue.substring(1, varValue.length() - 1);
                }
                variables.put(varName, varValue);
                // Also set in ScriptInterpreter for global access
                ScriptInterpreter.getInstance().setVariable(varName, varValue);
                i++;
                continue;
            }
            
            // Проверяем на if с поддержкой else if и else
            Matcher ifMatcher = IF_PATTERN.matcher(line);
            if (ifMatcher.find()) {
                String condition = ifMatcher.group(1);
                List<int[]> chain = findIfElseChain(lines, i + 1, end);
                int blockEnd = chain.get(0)[0];
                
                boolean executed = false;
                
                // Check main if condition
                if (evaluateCondition(condition)) {
                    // Find where if block ends (first else if or else or block end)
                    int ifBlockEnd = blockEnd;
                    if (chain.size() > 1) {
                        ifBlockEnd = chain.get(1)[0];
                    }
                    parseLines(lines, i + 1, ifBlockEnd);
                    executed = true;
                }
                
                // Check else if / else chain
                if (!executed) {
                    for (int ci = 1; ci < chain.size(); ci++) {
                        int[] entry = chain.get(ci);
                        int pos = entry[0];
                        int type = entry[1];
                        
                        if (type == 0) { // else if
                            Matcher elseIfMatcher = ELSE_IF_PATTERN.matcher(lines[pos].trim());
                            if (elseIfMatcher.find()) {
                                String elseIfCondition = elseIfMatcher.group(1);
                                if (evaluateCondition(elseIfCondition)) {
                                    // Find end of this else if block
                                    int elseIfEnd = blockEnd;
                                    if (ci + 1 < chain.size()) {
                                        elseIfEnd = chain.get(ci + 1)[0];
                                    }
                                    parseLines(lines, pos + 1, elseIfEnd);
                                    executed = true;
                                    break;
                                }
                            }
                        } else if (type == 1) { // else
                            parseLines(lines, pos + 1, blockEnd);
                            executed = true;
                            break;
                        }
                    }
                }
                
                i = blockEnd + 1;
                continue;
            }
            
            // Проверяем на for
            Matcher forMatcher = FOR_PATTERN.matcher(line);
            if (forMatcher.find()) {
                String forContent = forMatcher.group(1);
                String[] forParts = forContent.split(";");
                
                if (forParts.length == 3) {
                    String init = forParts[0].trim();
                    String condition = forParts[1].trim();
                    String increment = forParts[2].trim();
                    
                    int blockEnd = findBlockEnd(lines, i + 1, end);
                    
                    // Инициализация
                    if (!init.isEmpty()) {
                        Matcher initMatcher = VARIABLE_PATTERN.matcher(init);
                        if (initMatcher.find()) {
                            String initValue = processVariables(initMatcher.group(2).trim());
                            variables.put(initMatcher.group(1), initValue);
                            ScriptInterpreter.getInstance().setVariable(initMatcher.group(1), initValue);
                        }
                    }
                    
                    // Цикл
                    int maxIterations = 10000; // Защита от бесконечных циклов
                    int iterations = 0;
                    shouldBreak = false;
                    while (evaluateCondition(condition) && iterations < maxIterations && !shouldBreak) {
                        parseLines(lines, i + 1, blockEnd);
                        
                        if (shouldBreak) break;
                        
                        // Инкремент
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
            
            // Проверяем на while
            Matcher whileMatcher = WHILE_PATTERN.matcher(line);
            if (whileMatcher.find()) {
                String condition = whileMatcher.group(1);
                int blockEnd = findBlockEnd(lines, i + 1, end);
                
                int maxIterations = 10000;
                int iterations = 0;
                shouldBreak = false;
                while (evaluateCondition(condition) && iterations < maxIterations && !shouldBreak) {
                    parseLines(lines, i + 1, blockEnd);
                    if (shouldBreak) break;
                    iterations++;
                }
                shouldBreak = false;
                
                i = blockEnd + 1;
                continue;
            }
            
            // Проверяем на loop (с опциональным количеством итераций)
            Matcher loopMatcher = LOOP_PATTERN.matcher(line);
            if (loopMatcher.find()) {
                String countStr = loopMatcher.group(1);
                int blockEnd = findBlockEnd(lines, i + 1, end);
                
                if (countStr != null && !countStr.isEmpty()) {
                    // loop N - выполнить N раз
                    int count = Integer.parseInt(countStr);
                    shouldBreak = false;
                    for (int iter = 0; iter < count && !shouldBreak && state == ScriptState.RUNNING; iter++) {
                        parseLines(lines, i + 1, blockEnd);
                        if (shouldBreak) break;
                    }
                    shouldBreak = false;
                } else {
                    // loop без числа - бесконечный цикл через маркер
                    final int loopStart = i + 1;
                    final int loopEnd = blockEnd;
                    commandQueue.add(new CommandEntry(new LoopMarkerCommand(lines, loopStart, loopEnd), new String[0]));
                }
                
                i = blockEnd + 1;
                continue;
            }
            
            // Обычная команда - обрабатываем переменные
            String processedLine = processVariables(line);
            List<String> parts = parseArguments(processedLine);
            if (parts.isEmpty()) {
                i++;
                continue;
            }
            
            String commandName = parts.get(0).toLowerCase();
            String[] args = parts.subList(1, parts.size()).toArray(new String[0]);
            
            Command command = CommandRegistry.getCommand(commandName);
            if (command != null) {
                commandQueue.add(new CommandEntry(command, args));
                ScriptLogger.getInstance().debug("Queued command: " + commandName + " with " + args.length + " args");
            } else {
                ScriptLogger.getInstance().warn("Unknown command at line " + currentLine + ": " + commandName);
            }
            
            i++;
        }
    }
    
    /**
     * Находит конец блока (закрывающую скобку или end)
     */
    private int findBlockEnd(String[] lines, int start, int end) {
        int level = 1;
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            
            // Check for block openers
            if (line.contains("{") || 
                line.startsWith("if ") || line.startsWith("if(") ||
                line.startsWith("for ") || line.startsWith("for(") ||
                line.startsWith("while ") || line.startsWith("while(") ||
                line.startsWith("loop ") || line.equals("loop") || line.equals("loop{") ||
                line.startsWith("function ")) {
                // Only increment if it's a new block opener without closing on same line
                if (line.contains("{") && !line.contains("}")) {
                    level++;
                } else if (!line.contains("{") && !line.contains("}")) {
                    // Block opener without braces (uses end)
                    level++;
                }
            }
            
            // Check for block closers
            if (line.equals("}") || line.equals("end") || line.startsWith("} else") || line.equals("} else {")) {
                level--;
            } else if (line.contains("}") && !line.contains("{")) {
                level--;
            }
            
            if (level == 0) return i;
        }
        return end - 1;
    }
    
    /**
     * Находит конец if блока и возвращает список позиций else if и else
     * Returns: [blockEnd, elseIfPos1, elseIfPos2, ..., elsePos] (-1 if not present)
     */
    private List<int[]> findIfElseChain(String[] lines, int start, int end) {
        List<int[]> chain = new ArrayList<>(); // Each entry: [position, type] where type: 0=else if, 1=else
        int level = 1;
        
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            
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
            
            // Check for else if at current level
            if (level == 1 && ELSE_IF_PATTERN.matcher(line).matches()) {
                chain.add(new int[]{i, 0}); // 0 = else if
            }
            // Check for else at current level (but not else if)
            else if (level == 1 && !ELSE_IF_PATTERN.matcher(line).matches() && 
                     (line.equals("else") || line.equals("else {") || line.startsWith("} else") && !line.contains("if") || ELSE_PATTERN.matcher(line).matches())) {
                chain.add(new int[]{i, 1}); // 1 = else
            }
            
            // Check for block closers
            if (line.equals("}") || line.equals("end")) {
                level--;
            } else if (line.contains("}") && !line.contains("{") && !line.startsWith("} else")) {
                level--;
            }
            
            if (level == 0) {
                chain.add(0, new int[]{i, -1}); // Insert block end at beginning
                return chain;
            }
        }
        chain.add(0, new int[]{end - 1, -1});
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
     * Выполняет инкремент для цикла for
     */
    private void executeIncrement(String increment) {
        increment = increment.trim();
        
        // i++ или i--
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
            // i = i + 1 или подобное
            Matcher varMatcher = VARIABLE_PATTERN.matcher(increment);
            if (varMatcher.find()) {
                String varName = varMatcher.group(1);
                String expression = processVariables(varMatcher.group(2).trim());
                // Простое вычисление
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
     * Вычисляет простое арифметическое выражение
     */
    private int evaluateSimpleExpression(String expr) {
        expr = expr.trim();
        
        // Пробуем просто число
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            // Продолжаем
        }
        
        // Пробуем модуло
        if (expr.contains("%")) {
            String[] parts = expr.split("%");
            if (parts.length == 2) {
                int left = Integer.parseInt(parts[0].trim());
                int right = Integer.parseInt(parts[1].trim());
                return left % right;
            }
        }
        
        // Пробуем сложение
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            int sum = 0;
            for (String part : parts) {
                sum += Integer.parseInt(part.trim());
            }
            return sum;
        }
        
        // Пробуем вычитание
        if (expr.contains("-") && !expr.startsWith("-")) {
            String[] parts = expr.split("-");
            int result = Integer.parseInt(parts[0].trim());
            for (int i = 1; i < parts.length; i++) {
                result -= Integer.parseInt(parts[i].trim());
            }
            return result;
        }
        
        // Пробуем умножение
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            int result = 1;
            for (String part : parts) {
                result *= Integer.parseInt(part.trim());
            }
            return result;
        }
        
        // Пробуем деление
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
     * Обрабатывает переменные в строке
     */
    private String processVariables(String line) {
        String result = line;
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        // Обрабатываем переменные окружения ($PLAYER_X и т.д.)
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
        
        // Обрабатываем пользовательские переменные из ScriptInterpreter ($varname от команд типа vision, scan)
        Map<String, String> interpreterContext = interpreter.getContext();
        for (Map.Entry<String, String> entry : interpreterContext.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                String pattern = "\\$" + Pattern.quote(entry.getKey());
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }
        
        // Обрабатываем локальные переменные скрипта ($varname)
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
     * Вычисляет условие
     */
    private boolean evaluateCondition(String condition) {
        try {
            String processed = processVariables(condition);
            
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
            
            // Операторы сравнения
            String[] operators = {">=", "<=", "!=", "==", ">", "<"};
            for (String op : operators) {
                if (processed.contains(op)) {
                    String[] parts = processed.split(Pattern.quote(op), 2);
                    if (parts.length == 2) {
                        String left = parts[0].trim();
                        String right = parts[1].trim();
                        
                        // Evaluate expressions on both sides (for things like i % 8 == 0)
                        double leftNum = evaluateExpressionAsDouble(left);
                        double rightNum = evaluateExpressionAsDouble(right);
                        
                        if (!Double.isNaN(leftNum) && !Double.isNaN(rightNum)) {
                            return switch (op) {
                                case ">=" -> leftNum >= rightNum;
                                case "<=" -> leftNum <= rightNum;
                                case "!=" -> leftNum != rightNum;
                                case "==" -> Math.abs(leftNum - rightNum) < 0.0001;
                                case ">" -> leftNum > rightNum;
                                case "<" -> leftNum < rightNum;
                                default -> false;
                            };
                        } else {
                            // Строковое сравнение
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
     * Добавляет команду в очередь задачи
     */
    public void queueCommand(Command command, String[] args) {
        commandQueue.add(new CommandEntry(command, args));
    }

    private void processNextCommand() {
        if (commandQueue.isEmpty() || state != ScriptState.RUNNING) {
            if (commandQueue.isEmpty()) {
                state = ScriptState.STOPPED;
            }
            return;
        }

        // Update environment variables before each command
        ScriptInterpreter.getInstance().updateEnvironmentVariables();

        isProcessingCommand = true;
        CommandEntry entry = commandQueue.poll();
        executedCommands++;

        // Handle loop marker specially
        if (entry.command instanceof LoopMarkerCommand) {
            LoopMarkerCommand loopCmd = (LoopMarkerCommand) entry.command;
            
            // Re-parse the loop body and add commands to queue
            parseLines(loopCmd.lines, loopCmd.startLine, loopCmd.endLine);
            
            // Re-add the loop marker at the end to continue looping
            commandQueue.add(entry);
            
            isProcessingCommand = false;
            processNextCommand();
            return;
        }

        // Process variables at execution time (not parse time)
        String[] processedArgs = new String[entry.args.length];
        for (int i = 0; i < entry.args.length; i++) {
            processedArgs[i] = processVariables(entry.args[i]);
        }

        try {
            currentCommandFuture = entry.command.executeAsync(processedArgs);
            currentCommandFuture
                .exceptionally(throwable -> {
                    lastError = throwable.getMessage();
                    ScriptLogger.getInstance().error("Task " + id + " command error: " + throwable.getMessage());
                    return null;
                })
                .thenRun(() -> {
                    isProcessingCommand = false;
                    if (state == ScriptState.RUNNING) {
                        processNextCommand();
                    }
                });
        } catch (Exception e) {
            lastError = e.getMessage();
            isProcessingCommand = false;
            ScriptLogger.getInstance().error("Task " + id + " execution error: " + e.getMessage());
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

    // Управление состоянием
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
        if (currentCommandFuture != null && !currentCommandFuture.isDone()) {
            currentCommandFuture.cancel(true);
        }
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") stopped");
    }

    public void restart() {
        stop();
        commandQueue.clear();
        currentLine = 0;
        executedCommands = 0;
        lastError = null;
        state = ScriptState.RUNNING;
        parseAndQueue();
        ScriptLogger.getInstance().info("Task " + id + " (" + name + ") restarted");
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
     * Внутренний класс для хранения команды и аргументов
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
     * Внутренний класс для хранения определения функции
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

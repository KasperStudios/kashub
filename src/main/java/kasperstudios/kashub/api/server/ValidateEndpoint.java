package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.algorithm.CommandRegistry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * POST /api/validate - Validates KHScript code and returns errors/warnings
 */
public class ValidateEndpoint {
    
    // Patterns for validation
    private static final Pattern LET_PATTERN = Pattern.compile("^\\s*let\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern CONST_PATTERN = Pattern.compile("^\\s*const\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern IF_PATTERN = Pattern.compile("^\\s*if\\s+.+\\s*\\{\\s*$|^\\s*if\\s*\\(.+\\)\\s*\\{?\\s*$");
    private static final Pattern WHILE_PATTERN = Pattern.compile("^\\s*while\\s+.+\\s*\\{\\s*$|^\\s*while\\s*\\(.+\\)\\s*\\{?\\s*$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^\\s*for\\s*\\(.+\\)\\s*\\{?\\s*$");
    private static final Pattern LOOP_PATTERN = Pattern.compile("^\\s*loop(?:\\s+\\d+)?\\s*\\{?\\s*$");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*(?:fn|function)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(.*\\)\\s*\\{?\\s*$");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s*\\{?\\s*$");
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^\\s*\\}?\\s*else\\s+if\\s+.+\\s*\\{\\s*$");
    private static final Pattern CLOSE_BRACE = Pattern.compile("^\\s*\\}\\s*$");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*//.*$");
    private static final Pattern BLOCK_COMMENT_START = Pattern.compile("/\\*");
    private static final Pattern BLOCK_COMMENT_END = Pattern.compile("\\*/");
    
    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            Map<String, Object> request = gson.fromJson(body, Map.class);
            String code = (String) request.get("code");
            
            if (code == null || code.isEmpty()) {
                KashubAPIServer.sendResponse(exchange, 400, "{\"error\":\"No code provided\"}");
                return;
            }
            
            List<ValidationError> errors = validate(code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", errors.isEmpty());
            response.put("errors", errors);
            response.put("errorCount", errors.stream().filter(e -> "error".equals(e.severity)).count());
            response.put("warningCount", errors.stream().filter(e -> "warning".equals(e.severity)).count());
            
            String json = gson.toJson(response);
            KashubAPIServer.sendResponse(exchange, 200, json);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in validate endpoint", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    public static List<ValidationError> validate(String code) {
        List<ValidationError> errors = new ArrayList<>();
        String[] lines = code.split("\\r?\\n");
        
        int braceDepth = 0;
        boolean inBlockComment = false;
        Set<String> declaredVariables = new HashSet<>();
        Set<String> declaredFunctions = new HashSet<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            int lineNum = i + 1;
            
            // Handle block comments
            if (inBlockComment) {
                if (BLOCK_COMMENT_END.matcher(line).find()) {
                    inBlockComment = false;
                }
                continue;
            }
            
            if (BLOCK_COMMENT_START.matcher(line).find()) {
                if (!BLOCK_COMMENT_END.matcher(line).find()) {
                    inBlockComment = true;
                }
                continue;
            }
            
            // Skip empty lines and comments
            if (trimmed.isEmpty() || COMMENT_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            
            // Track braces
            int openBraces = countChar(line, '{');
            int closeBraces = countChar(line, '}');
            braceDepth += openBraces - closeBraces;
            
            if (braceDepth < 0) {
                errors.add(new ValidationError(lineNum, 0, "Unexpected closing brace", "error"));
                braceDepth = 0;
            }
            
            // Check for variable declarations
            Matcher letMatcher = LET_PATTERN.matcher(trimmed);
            Matcher constMatcher = CONST_PATTERN.matcher(trimmed);
            
            if (letMatcher.matches()) {
                String varName = letMatcher.group(1);
                if (declaredVariables.contains(varName)) {
                    errors.add(new ValidationError(lineNum, 0, "Variable '" + varName + "' already declared", "warning"));
                }
                declaredVariables.add(varName);
                continue;
            }
            
            if (constMatcher.matches()) {
                String varName = constMatcher.group(1);
                if (declaredVariables.contains(varName)) {
                    errors.add(new ValidationError(lineNum, 0, "Constant '" + varName + "' already declared", "error"));
                }
                declaredVariables.add(varName);
                continue;
            }
            
            // Check for function declarations
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(trimmed);
            if (funcMatcher.matches()) {
                String funcName = funcMatcher.group(1);
                if (declaredFunctions.contains(funcName)) {
                    errors.add(new ValidationError(lineNum, 0, "Function '" + funcName + "' already declared", "error"));
                }
                declaredFunctions.add(funcName);
                continue;
            }
            
            // Skip control flow statements
            if (IF_PATTERN.matcher(trimmed).matches() ||
                WHILE_PATTERN.matcher(trimmed).matches() ||
                FOR_PATTERN.matcher(trimmed).matches() ||
                LOOP_PATTERN.matcher(trimmed).matches() ||
                ELSE_PATTERN.matcher(trimmed).matches() ||
                ELSE_IF_PATTERN.matcher(trimmed).matches() ||
                CLOSE_BRACE.matcher(trimmed).matches()) {
                continue;
            }
            
            // Check for variable assignment (x = value)
            if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s*.+$")) {
                continue;
            }
            
            // Check for command
            String[] parts = trimmed.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();
            
            // Skip break/continue/return
            if (commandName.equals("break") || commandName.equals("continue") || commandName.equals("return")) {
                continue;
            }
            
            // Check if command exists
            if (!CommandRegistry.hasCommand(commandName)) {
                // Check if it's a function call
                if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*\\)\\s*$")) {
                    String funcName = trimmed.split("\\(")[0].trim();
                    if (!declaredFunctions.contains(funcName)) {
                        errors.add(new ValidationError(lineNum, 0, "Unknown function: " + funcName, "warning"));
                    }
                } else {
                    errors.add(new ValidationError(lineNum, 0, "Unknown command: " + commandName, "error"));
                }
            }
        }
        
        // Check for unclosed braces
        if (braceDepth > 0) {
            errors.add(new ValidationError(lines.length, 0, "Unclosed brace(s): " + braceDepth + " remaining", "error"));
        }
        
        // Check for unclosed block comment
        if (inBlockComment) {
            errors.add(new ValidationError(lines.length, 0, "Unclosed block comment", "error"));
        }
        
        return errors;
    }
    
    private static int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }
    
    public static class ValidationError {
        public int line;
        public int column;
        public String message;
        public String severity; // "error", "warning", "info"
        
        public ValidationError(int line, int column, String message, String severity) {
            this.line = line;
            this.column = column;
            this.message = message;
            this.severity = severity;
        }
    }
}

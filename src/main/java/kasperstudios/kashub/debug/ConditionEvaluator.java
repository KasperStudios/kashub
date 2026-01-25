package kasperstudios.kashub.debug;

import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Parser;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConditionEvaluator {
    private static ConditionEvaluator instance;

    private final Map<String, CompiledCondition> conditionCache = new ConcurrentHashMap<>();

    private static final int MAX_CACHE_SIZE = 100;

    private static final int MAX_COMPLEXITY = 10;

    private ConditionEvaluator() {
    }

    public static ConditionEvaluator getInstance() {
        if (instance == null) {
            instance = new ConditionEvaluator();
        }
        return instance;
    }

    public boolean evaluate(String condition, Map<String, String> variables) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {

            CompiledCondition compiled = conditionCache.get(condition);
            if (compiled == null) {

                if (!validateComplexity(condition)) {
                    ScriptLogger.getInstance().warn("Condition too complex (max {} operators): {}",
                            MAX_COMPLEXITY, condition);
                    return false;
                }

                compiled = new CompiledCondition(condition);

                if (conditionCache.size() >= MAX_CACHE_SIZE) {
                    conditionCache.clear();
                    ScriptLogger.getInstance().debug("Condition cache cleared (size limit reached)");
                }

                conditionCache.put(condition, compiled);
            }

            return compiled.evaluate(variables);

        } catch (Exception e) {
            ScriptLogger.getInstance().error("Error evaluating condition '{}': {}", condition, e.getMessage());
            return false;
        }
    }

    private boolean validateComplexity(String condition) {
        int operatorCount = 0;
        String[] operators = { "&&", "||", "==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/", "%" };

        for (String op : operators) {
            int index = 0;
            while ((index = condition.indexOf(op, index)) != -1) {
                operatorCount++;
                index += op.length();
            }
        }

        return operatorCount <= MAX_COMPLEXITY;
    }

    public String interpolateLogMessage(String message, Map<String, String> variables) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String result = message;

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{$" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "null");
        }

        return result;
    }

    public void clearCache() {
        conditionCache.clear();
    }

    private static class CompiledCondition {
        private final String condition;

        public CompiledCondition(String condition) {
            this.condition = condition;
        }

        public boolean evaluate(Map<String, String> variables) {
            // Create a temporary Context with the variables
            Context ctx = new Context();

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                ctx.setVariable(key, Value.of(value));
                // Also set without $ prefix
                if (key.startsWith("$")) {
                    ctx.setVariable(key.substring(1), Value.of(value));
                }
            }

            Value result = Parser.evaluate(condition, ctx);
            return result.asBoolean();
        }
    }
}

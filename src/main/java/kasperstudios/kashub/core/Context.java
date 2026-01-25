package kasperstudios.kashub.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context - Hierarchical execution context for the engine.
 * Supports nested scopes for variables and flow control flags.
 */
public class Context {
    private final Map<String, Value> variables = new ConcurrentHashMap<>();
    private final Context parent;

    // Flow control flags
    public boolean shouldBreak = false;
    public boolean shouldContinue = false;
    public boolean shouldReturn = false;
    public boolean shouldStop = false;
    public boolean lastIfConditionResult = false; // For 'else' support
    public Value returnValue = Value.NULL;

    public Context() {
        this(null);
    }

    public Context(Context parent) {
        this.parent = parent;
    }

    public Value getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.getVariable(name);
        }
        return Value.NULL;
    }

    public void setVariable(String name, Value value) {
        // Scope bubbling: if variable exists in parent scope, update it there
        if (parent != null && parent.hasVariableLocal(name)) {
            parent.setVariable(name, value);
        } else {
            variables.put(name, value);
        }
    }

    /**
     * Declares a variable strictly in the LOCAL scope (used for 'let'/'var').
     */
    public void declareVariable(String name, Value value) {
        variables.put(name, value);
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name) || (parent != null && parent.hasVariable(name));
    }

    private boolean hasVariableLocal(String name) {
        return variables.containsKey(name);
    }

    public Map<String, Value> getLocalVariables() {
        return new HashMap<>(variables);
    }

    public Context getParent() {
        return parent;
    }

    public void resetFlowFlags() {
        shouldBreak = false;
        shouldContinue = false;
        shouldReturn = false;
        shouldStop = false;
        returnValue = Value.NULL;
    }

    public void stop() {
        shouldStop = true;
    }
}

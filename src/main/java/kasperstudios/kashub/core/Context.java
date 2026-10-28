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
    private final boolean isFunctionScope;

    // Flow control flags
    public boolean shouldBreak = false;
    public boolean shouldContinue = false;
    public boolean shouldReturn = false;
    public boolean shouldStop = false;
    public boolean lastIfConditionResult = false; // For 'else' support
    public Value returnValue = Value.NULL;

    private boolean sudoMode = false;
    private boolean suMode = false;
    private int currentLine = 0;
    private ExecutionOrigin origin = ExecutionOrigin.USER; // Default to safest

    public ExecutionOrigin getOrigin() { return origin; }
    public void setOrigin(ExecutionOrigin origin) { this.origin = origin; }

    public void setSudo(boolean sudo) { this.sudoMode = sudo; }
    public void setSu(boolean su) { this.suMode = su; }
    
    public int getCurrentLine() { return currentLine; }
    public void setCurrentLine(int line) { this.currentLine = line; }

    public boolean hasSudo() {
        if (sudoMode || suMode) return true;
        return parent != null && parent.hasSudo();
    }

    public boolean hasSu() {
        if (suMode) return true;
        return parent != null && parent.hasSu();
    }

    public Context() {
        this(null);
    }

    public Context(Context parent) {
        this(parent, false);
    }

    public Context(Context parent, boolean isFunctionScope) {
        this.parent = parent;
        this.isFunctionScope = isFunctionScope;
        if (parent != null) {
            this.origin = parent.origin;
        }
    }

    public Value getVariable(String name) {
        if (name.startsWith("$")) {
            return Environment.getInstance().getValue(name.substring(1));
        }
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.getVariable(name);
        }
        return Value.NULL;
    }

    public void setVariable(String name, Value value) {
        // If declared locally in this scope, update it locally
        if (variables.containsKey(name)) {
            variables.put(name, value);
        } else if (parent != null && parent.hasVariable(name)) {
            // Otherwise bubble up to nearest parent scope that contains it
            parent.setVariable(name, value);
        } else if (parent != null && !isFunctionScope) {
            // In a block scope (while, for, if), bubble undeclared assignments up to parent scope
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

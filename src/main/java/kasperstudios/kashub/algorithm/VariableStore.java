package kasperstudios.kashub.algorithm;

import java.util.*;

/**
 * Variable store for KHScript with support for:
 * - let: mutable variables
 * - const: immutable constants
 * - Legacy variables (without keyword, treated as let)
 */
public class VariableStore {
    
    public enum VariableType {
        LET,    // Mutable variable (let x = 5)
        CONST,  // Immutable constant (const MAX = 100)
        LEGACY  // Legacy variable without keyword (x = 5)
    }
    
    private static class Variable {
        final String name;
        String value;
        final VariableType type;
        final boolean isConst;
        
        Variable(String name, String value, VariableType type) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.isConst = type == VariableType.CONST;
        }
    }
    
    private final Map<String, Variable> variables = new HashMap<>();
    private final VariableStore parent; // For scoping (function calls, blocks)
    
    public VariableStore() {
        this.parent = null;
    }
    
    public VariableStore(VariableStore parent) {
        this.parent = parent;
    }
    
    /**
     * Declare a new variable with 'let' keyword
     * @throws IllegalStateException if variable already exists in current scope
     */
    public void declareLet(String name, String value) {
        if (variables.containsKey(name)) {
            Variable existing = variables.get(name);
            if (existing.isConst) {
                throw new IllegalStateException("Cannot redeclare const '" + name + "' with let");
            }
        }
        variables.put(name, new Variable(name, value, VariableType.LET));
    }
    
    /**
     * Declare a new constant with 'const' keyword
     * @throws IllegalStateException if variable already exists in current scope
     */
    public void declareConst(String name, String value) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException("Cannot redeclare variable '" + name + "' as const");
        }
        variables.put(name, new Variable(name, value, VariableType.CONST));
    }
    
    /**
     * Set a variable value (legacy syntax: x = 5)
     * Creates new variable if doesn't exist, updates if exists and not const
     * @throws IllegalStateException if trying to modify a const
     */
    public void set(String name, String value) {
        // First check current scope
        if (variables.containsKey(name)) {
            Variable var = variables.get(name);
            if (var.isConst) {
                throw new IllegalStateException("Cannot reassign const '" + name + "'");
            }
            var.value = value;
            return;
        }
        
        // Check parent scopes
        if (parent != null && parent.has(name)) {
            parent.set(name, value);
            return;
        }
        
        // Create new legacy variable in current scope
        variables.put(name, new Variable(name, value, VariableType.LEGACY));
    }
    
    /**
     * Get variable value, checking parent scopes
     */
    public String get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).value;
        }
        if (parent != null) {
            return parent.get(name);
        }
        return null;
    }
    
    /**
     * Check if variable exists in any scope
     */
    public boolean has(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parent != null && parent.has(name);
    }
    
    /**
     * Check if variable is const
     */
    public boolean isConst(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).isConst;
        }
        if (parent != null) {
            return parent.isConst(name);
        }
        return false;
    }
    
    /**
     * Get variable type
     */
    public VariableType getType(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).type;
        }
        if (parent != null) {
            return parent.getType(name);
        }
        return null;
    }
    
    /**
     * Get all variable names in current scope (not including parent)
     */
    public Set<String> getLocalNames() {
        return new HashSet<>(variables.keySet());
    }
    
    /**
     * Get all variable names including parent scopes
     */
    public Set<String> getAllNames() {
        Set<String> names = new HashSet<>(variables.keySet());
        if (parent != null) {
            names.addAll(parent.getAllNames());
        }
        return names;
    }
    
    /**
     * Get all variables as a map (for compatibility)
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        if (parent != null) {
            map.putAll(parent.toMap());
        }
        for (Map.Entry<String, Variable> entry : variables.entrySet()) {
            map.put(entry.getKey(), entry.getValue().value);
        }
        return map;
    }
    
    /**
     * Clear all variables in current scope
     */
    public void clear() {
        variables.clear();
    }
    
    /**
     * Create a child scope (for function calls, blocks)
     */
    public VariableStore createChildScope() {
        return new VariableStore(this);
    }
    
    /**
     * Import variables from a map (for compatibility with legacy code)
     */
    public void importFrom(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!variables.containsKey(entry.getKey())) {
                variables.put(entry.getKey(), new Variable(entry.getKey(), entry.getValue(), VariableType.LEGACY));
            }
        }
    }
    
    /**
     * Remove a variable from current scope
     */
    public void remove(String name) {
        variables.remove(name);
    }
}

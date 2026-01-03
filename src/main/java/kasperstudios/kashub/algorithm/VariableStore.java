package kasperstudios.kashub.algorithm;

import java.util.*;

public class VariableStore {

    public enum VariableType {
        LET,
        CONST,
        LEGACY
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
    private final VariableStore parent;

    public VariableStore() {
        this.parent = null;
    }

    public VariableStore(VariableStore parent) {
        this.parent = parent;
    }

    public void declareLet(String name, String value) {
        if (variables.containsKey(name)) {
            Variable existing = variables.get(name);
            if (existing.isConst) {
                throw new IllegalStateException("Cannot redeclare const '" + name + "' with let");
            }
        }
        variables.put(name, new Variable(name, value, VariableType.LET));
    }

    public void declareConst(String name, String value) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException("Cannot redeclare variable '" + name + "' as const");
        }
        variables.put(name, new Variable(name, value, VariableType.CONST));
    }

    public void set(String name, String value) {

        if (variables.containsKey(name)) {
            Variable var = variables.get(name);
            if (var.isConst) {
                throw new IllegalStateException("Cannot reassign const '" + name + "'");
            }
            var.value = value;
            return;
        }

        if (parent != null && parent.has(name)) {
            parent.set(name, value);
            return;
        }

        variables.put(name, new Variable(name, value, VariableType.LEGACY));
    }

    public String get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).value;
        }
        if (parent != null) {
            return parent.get(name);
        }
        return null;
    }

    public boolean has(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parent != null && parent.has(name);
    }

    public boolean isConst(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).isConst;
        }
        if (parent != null) {
            return parent.isConst(name);
        }
        return false;
    }

    public VariableType getType(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name).type;
        }
        if (parent != null) {
            return parent.getType(name);
        }
        return null;
    }

    public Set<String> getLocalNames() {
        return new HashSet<>(variables.keySet());
    }

    public Set<String> getAllNames() {
        Set<String> names = new HashSet<>(variables.keySet());
        if (parent != null) {
            names.addAll(parent.getAllNames());
        }
        return names;
    }

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

    public void clear() {
        variables.clear();
    }

    public VariableStore createChildScope() {
        return new VariableStore(this);
    }

    public void importFrom(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!variables.containsKey(entry.getKey())) {
                variables.put(entry.getKey(), new Variable(entry.getKey(), entry.getValue(), VariableType.LEGACY));
            }
        }
    }

    public void remove(String name) {
        variables.remove(name);
    }
}

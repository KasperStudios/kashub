package kasperstudios.kashub.server.script.v2;

import kasperstudios.kashub.server.script.objects.ScriptValue;
import java.util.HashMap;
import java.util.Map;

public class ScriptContext {
    private final Map<String, ScriptValue> variables = new HashMap<>();
    private final ScriptContext parent; // For closures/blocks

    // Flow control flags
    public boolean shouldBreak = false;
    public boolean shouldContinue = false;
    public boolean shouldReturn = false;
    public ScriptValue returnValue = ScriptValue.NULL;

    public ScriptContext(ScriptContext parent) {
        this.parent = parent;
    }

    public ScriptValue getVariable(String name) {
        if (variables.containsKey(name))
            return variables.get(name);
        if (parent != null)
            return parent.getVariable(name);
        return ScriptValue.NULL;
    }

    public void setVariable(String name, ScriptValue value) {
        // If variable exists in parent, update it there (scope bubbling)
        if (parent != null && parent.hasVariable(name)) {
            parent.setVariable(name, value);
        } else {
            variables.put(name, value);
        }
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name) || (parent != null && parent.hasVariable(name));
    }
}

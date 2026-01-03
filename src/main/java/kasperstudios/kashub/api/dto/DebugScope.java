package kasperstudios.kashub.api.dto;

public class DebugScope {

    public String name;

    public int variablesReference;

    public boolean expensive;

    public Integer namedVariables;

    public String presentationHint;

    public DebugScope() {
    }

    public DebugScope(String name, int variablesReference) {
        this.name = name;
        this.variablesReference = variablesReference;
        this.expensive = false;
    }

    public DebugScope(String name, int variablesReference, boolean expensive) {
        this.name = name;
        this.variablesReference = variablesReference;
        this.expensive = expensive;
    }

    public DebugScope(String name, int variablesReference, boolean expensive, String presentationHint) {
        this.name = name;
        this.variablesReference = variablesReference;
        this.expensive = expensive;
        this.presentationHint = presentationHint;
    }
}

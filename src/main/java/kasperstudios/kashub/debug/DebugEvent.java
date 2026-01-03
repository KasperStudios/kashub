package kasperstudios.kashub.debug;

public class DebugEvent {
    public enum Type {
        PAUSED,
        RESUMED,
        BREAKPOINT_HIT,
        STEP_COMPLETE
    }

    private final Type type;
    private final int scriptId;
    private final int line;

    public DebugEvent(Type type, int scriptId, int line) {
        this.type = type;
        this.scriptId = scriptId;
        this.line = line;
    }

    public Type getType() {
        return type;
    }

    public int getScriptId() {
        return scriptId;
    }

    public int getLine() {
        return line;
    }
}

package kasperstudios.kashub.api.dto;

public class DebugStackFrame {

    public int id;

    public String name;

    public String source;

    public int line;

    public Integer column;

    public String presentationHint;

    public DebugStackFrame() {
    }

    public DebugStackFrame(int id, String name, String source, int line) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.line = line;
        this.presentationHint = "normal";
    }
}

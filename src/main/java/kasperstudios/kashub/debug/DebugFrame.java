package kasperstudios.kashub.debug;

public class DebugFrame {
    private final int line;
    private final String functionName;
    private final int depth;

    public DebugFrame(int line, String functionName, int depth) {
        this.line = line;
        this.functionName = functionName;
        this.depth = depth;
    }

    public int getLine() {
        return line;
    }

    public String getFunctionName() {
        return functionName;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return String.format("DebugFrame{line=%d, function='%s', depth=%d}",
            line, functionName != null ? functionName : "<main>", depth);
    }
}

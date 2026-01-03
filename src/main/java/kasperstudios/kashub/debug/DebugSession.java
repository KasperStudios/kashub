package kasperstudios.kashub.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DebugSession {
    private final int scriptId;
    private volatile DebugState state = DebugState.RUNNING;
    private volatile StepMode stepMode = StepMode.NONE;
    private volatile int executionDepth = 0;
    private volatile int stepStartDepth = 0;
    private volatile int stopAtLine = -1;
    private volatile int ignoreBreakpointsAtLine = -1;
    private final Deque<DebugFrame> callStack = new ArrayDeque<>();

    public DebugSession(int scriptId) {
        this.scriptId = scriptId;
    }

    public int getScriptId() {
        return scriptId;
    }

    public DebugState getState() {
        return state;
    }

    public void setState(DebugState state) {
        this.state = state;
    }

    public StepMode getStepMode() {
        return stepMode;
    }

    public void setStepMode(StepMode stepMode) {
        this.stepMode = stepMode;
    }

    public int getExecutionDepth() {
        return executionDepth;
    }

    public void setExecutionDepth(int depth) {
        this.executionDepth = depth;
    }

    public int getStepStartDepth() {
        return stepStartDepth;
    }

    public void setStepStartDepth(int depth) {
        this.stepStartDepth = depth;
    }

    public int getStopAtLine() {
        return stopAtLine;
    }

    public void setStopAtLine(int line) {
        this.stopAtLine = line;
    }

    public int getIgnoreBreakpointsAtLine() {
        return ignoreBreakpointsAtLine;
    }

    public void setIgnoreBreakpointsAtLine(int line) {
        this.ignoreBreakpointsAtLine = line;
    }

    public void pushFrame(DebugFrame frame) {
        callStack.push(frame);
    }

    public DebugFrame popFrame() {
        return callStack.isEmpty() ? null : callStack.pop();
    }

    public DebugFrame getCurrentFrame() {
        return callStack.isEmpty() ? null : callStack.peek();
    }

    public List<DebugFrame> getCallStack() {
        return new ArrayList<>(callStack);
    }

    public int getCallDepth() {
        return callStack.size();
    }

    public void clearCallStack() {
        callStack.clear();
    }
}

package kasperstudios.kashub.debug;

import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.Environment;
import kasperstudios.kashub.util.ScriptLogger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

public class DebugManager {
    private static DebugManager instance;
    private final Map<Integer, DebugSession> sessions = new ConcurrentHashMap<>();

    private final Map<String, Map<Integer, Breakpoint>> breakpoints = new ConcurrentHashMap<>();
    private final List<Consumer<DebugEvent>> listeners = new CopyOnWriteArrayList<>();

    private DebugManager() {
    }

    public static DebugManager getInstance() {
        if (instance == null) {
            instance = new DebugManager();
        }
        return instance;
    }

    public void addDebugEventListener(Consumer<DebugEvent> listener) {
        listeners.add(listener);
    }

    public void removeDebugEventListener(Consumer<DebugEvent> listener) {
        listeners.remove(listener);
    }

    private void fireEvent(DebugEvent event) {
        for (Consumer<DebugEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                ScriptLogger.getInstance().error("Error in debug event listener", e);
            }
        }
    }

    public boolean shouldPause(int scriptId, String scriptName, int currentLine) {

        DebugSession session = sessions.computeIfAbsent(scriptId, DebugSession::new);

        if (session.getState() == DebugState.PAUSED) {
            return true;
        }

        if (session.getIgnoreBreakpointsAtLine() != -1) {
            if (currentLine == session.getIgnoreBreakpointsAtLine()) {

                return false;
            } else {

                ScriptLogger.getInstance().debug("Debug: Cleared ignore flag (new line {})", currentLine);
                session.setIgnoreBreakpointsAtLine(-1);
            }
        }

        if (scriptName != null && breakpoints.containsKey(scriptName)) {
            Map<Integer, Breakpoint> fileBreakpoints = breakpoints.get(scriptName);
            Breakpoint bp = fileBreakpoints.get(currentLine);

            if (bp != null && bp.isEnabled()) {

                bp.incrementHitCount();

                if (!bp.checkHitCondition()) {
                    ScriptLogger.getInstance().debug("Debug: Breakpoint hit condition not satisfied (hit count: {})",
                            bp.getHitCount());
                    return false;
                }

                if (bp.getType() == BreakpointType.LOGPOINT) {

                    Map<String, String> vars = getVariables(scriptId);
                    String message = ConditionEvaluator.getInstance().interpolateLogMessage(bp.getLogMessage(), vars);
                    ScriptLogger.getInstance().info("[Logpoint:{}] {}", currentLine, message);
                    return false;

                } else if (bp.getType() == BreakpointType.CONDITIONAL) {

                    Map<String, String> vars = getVariables(scriptId);
                    boolean conditionMet = ConditionEvaluator.getInstance().evaluate(bp.getCondition(), vars);

                    if (!conditionMet) {
                        ScriptLogger.getInstance().debug("Debug: Conditional breakpoint condition not met: {}",
                                bp.getCondition());
                        return false;
                    }

                    ScriptLogger.getInstance().debug(
                            "Debug: Pausing at conditional breakpoint on line {} (condition: {})",
                            currentLine, bp.getCondition());
                    pause(scriptId, currentLine);
                    return true;

                } else {

                    ScriptLogger.getInstance().debug("Debug: Pausing at breakpoint on line {}", currentLine);
                    pause(scriptId, currentLine);
                    return true;
                }
            }
        }

        if (session.getStepMode() != StepMode.NONE) {
            if (session.getStepMode() == StepMode.STEP_INTO) {

                session.setStepMode(StepMode.NONE);
                pause(scriptId, currentLine);
                return true;
            }

            if (session.getStepMode() == StepMode.STEP_OVER) {

                session.setStepMode(StepMode.NONE);
                pause(scriptId, currentLine);
                return true;
            }

            if (session.getStepMode() == StepMode.STEP_OUT) {

                if (session.getCallDepth() <= session.getStepStartDepth()) {
                    session.setStepMode(StepMode.NONE);
                    pause(scriptId, currentLine);
                    ScriptLogger.getInstance().debug("Debug: Step Out completed at depth {}", session.getCallDepth());
                    return true;
                }
            }
        }

        return false;
    }

    public void pause(int scriptId, int line) {
        DebugSession session = sessions.computeIfAbsent(scriptId, DebugSession::new);
        session.setState(DebugState.PAUSED);
        ScriptLogger.getInstance().debug("Debug: Paused script " + scriptId + " at line " + line);
        fireEvent(new DebugEvent(DebugEvent.Type.PAUSED, scriptId, line));
    }

    public void resume(int scriptId) {
        ScriptLogger.getInstance().debug("Debug: Resuming script {}", scriptId);
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            session.setState(DebugState.RUNNING);
            session.setStepMode(StepMode.NONE);

            session.setIgnoreBreakpointsAtLine(getCurrentLine(scriptId));
            ScriptLogger.getInstance().debug("Debug: Set ignore line to {}", session.getIgnoreBreakpointsAtLine());
            fireEvent(new DebugEvent(DebugEvent.Type.RESUMED, scriptId, -1));
        } else {
            ScriptLogger.getInstance().warn("Debug: Session not found for script {}", scriptId);
        }
    }

    public void stepInto(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            session.setState(DebugState.RUNNING);
            session.setStepMode(StepMode.STEP_INTO);

            Task task = TaskManager.getInstance().getTask(scriptId);
            if (task != null) {
                session.setIgnoreBreakpointsAtLine(task.getCurrentLine());
            }

            fireEvent(new DebugEvent(DebugEvent.Type.RESUMED, scriptId, -1));
        }
    }

    public void stepOver(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            Task task = TaskManager.getInstance().getTask(scriptId);
            if (task != null) {
                session.setStopAtLine(task.getCurrentLine());
                session.setIgnoreBreakpointsAtLine(task.getCurrentLine());
            }

            session.setState(DebugState.RUNNING);
            session.setStepMode(StepMode.STEP_OVER);
            fireEvent(new DebugEvent(DebugEvent.Type.RESUMED, scriptId, -1));
        }
    }

    public void stepOut(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            if (session.getCallDepth() == 0) {
                resume(scriptId);
                return;
            }

            int currentDepth = session.getCallDepth();
            session.setStepStartDepth(currentDepth - 1);

            Task task = TaskManager.getInstance().getTask(scriptId);
            if (task != null) {
                session.setIgnoreBreakpointsAtLine(task.getCurrentLine());
            }

            session.setState(DebugState.RUNNING);
            session.setStepMode(StepMode.STEP_OUT);
            fireEvent(new DebugEvent(DebugEvent.Type.RESUMED, scriptId, -1));
            ScriptLogger.getInstance().debug("Debug: Step Out from depth {} to {}", currentDepth, currentDepth - 1);
        }
    }

    public boolean isPaused(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        return session != null && session.getState() == DebugState.PAUSED;
    }

    public void resumeAll() {
        ScriptLogger.getInstance().debug("Debug: resumeAll() called");
        for (Map.Entry<Integer, DebugSession> entry : sessions.entrySet()) {
            DebugSession session = entry.getValue();
            session.setState(DebugState.RUNNING);
            session.setStepMode(StepMode.NONE);

            Task task = TaskManager.getInstance().getTask(entry.getKey());
            if (task != null) {
                session.setIgnoreBreakpointsAtLine(task.getCurrentLine());
                ScriptLogger.getInstance().debug("Debug: Set ignore line for session {} to {}",
                        entry.getKey(), task.getCurrentLine());
            } else {
                ScriptLogger.getInstance().warn("Debug: Task not found for session {}", entry.getKey());
            }
        }
        ScriptLogger.getInstance().debug("Debug: Resumed all sessions");
    }

    public Map<String, String> getAggregateVariables() {
        Map<String, String> vars = new HashMap<>();

        // Add Environment Variables
        vars.putAll(Environment.getInstance().getAllVariables());

        // Add variables from all paused sessions/tasks
        for (Map.Entry<Integer, DebugSession> entry : sessions.entrySet()) {
            if (entry.getValue().getState() == DebugState.PAUSED) {
            Task task = TaskManager.getInstance().getTask(entry.getKey());
                if (task != null) {
                    vars.putAll(task.getVariables());
                }
                // Break or continue? Original code broke after first paused session?
                // "break;" was in original. Probably meant to just show variables of ONE paused
                // session context?
                // If multiple are paused, which one?
                // For "Aggregate", implies all? But conflicting names?
                // If the UI expects a single context, this method is ambiguous.
                // Preserving original behavior (break after first).
                break;
            }
        }
        return vars;
    }

    public Map<String, String> getVariables(int scriptId) {
        Map<String, String> allVars = new HashMap<>();

        // Add Environment Variables
        allVars.putAll(
                Environment.getInstance().getAllVariables());

        // Add Script Variables
        Task task = TaskManager.getInstance().getTask(scriptId);
        if (task != null) {
            allVars.putAll(task.getVariables());
        }
        return allVars;
    }

    public void toggleBreakpoint(String scriptName, int line) {
        breakpoints.computeIfAbsent(scriptName, k -> new ConcurrentHashMap<>());
        Map<Integer, Breakpoint> fileBreakpoints = breakpoints.get(scriptName);

        if (fileBreakpoints.containsKey(line)) {
            fileBreakpoints.remove(line);
        } else {
            fileBreakpoints.put(line, new Breakpoint(line));
        }
    }

    public boolean hasBreakpoint(String scriptName, int line) {
        return breakpoints.containsKey(scriptName) && breakpoints.get(scriptName).containsKey(line);
    }

    public void setBreakpoints(String scriptName, List<Integer> lines) {

        Map<Integer, Breakpoint> fileBreakpoints = new ConcurrentHashMap<>();
        for (int line : lines) {
            fileBreakpoints.put(line, new Breakpoint(line));
        }
        breakpoints.put(scriptName, fileBreakpoints);
        ScriptLogger.getInstance().debug("Debug: Set {} breakpoints for {}", lines.size(), scriptName);
    }

    public void setConditionalBreakpoint(String scriptName, int line, String condition) {
        breakpoints.computeIfAbsent(scriptName, k -> new ConcurrentHashMap<>());
        Map<Integer, Breakpoint> fileBreakpoints = breakpoints.get(scriptName);

        Breakpoint bp = fileBreakpoints.computeIfAbsent(line, Breakpoint::new);
        bp.setCondition(condition);

        ScriptLogger.getInstance().debug("Debug: Set conditional breakpoint at {}:{} (condition: {})",
                scriptName, line, condition);
    }

    public void setLogpoint(String scriptName, int line, String logMessage) {
        breakpoints.computeIfAbsent(scriptName, k -> new ConcurrentHashMap<>());
        Map<Integer, Breakpoint> fileBreakpoints = breakpoints.get(scriptName);

        Breakpoint bp = fileBreakpoints.computeIfAbsent(line, Breakpoint::new);
        bp.setLogMessage(logMessage);

        ScriptLogger.getInstance().debug("Debug: Set logpoint at {}:{} (message: {})",
                scriptName, line, logMessage);
    }

    public void setHitCondition(String scriptName, int line, String hitCondition) {
        if (!breakpoints.containsKey(scriptName)) {
            ScriptLogger.getInstance().warn("Debug: No breakpoints for script {}", scriptName);
            return;
        }

        Map<Integer, Breakpoint> fileBreakpoints = breakpoints.get(scriptName);
        Breakpoint bp = fileBreakpoints.get(line);

        if (bp == null) {
            ScriptLogger.getInstance().warn("Debug: No breakpoint at {}:{}", scriptName, line);
            return;
        }

        bp.setHitCondition(hitCondition);
        ScriptLogger.getInstance().debug("Debug: Set hit condition for {}:{} ({})",
                scriptName, line, hitCondition);
    }

    public Breakpoint getBreakpoint(String scriptName, int line) {
        if (!breakpoints.containsKey(scriptName)) {
            return null;
        }
        return breakpoints.get(scriptName).get(line);
    }

    public void clearBreakpoints(String scriptName) {
        breakpoints.remove(scriptName);
    }

    public void clearAllBreakpoints() {
        breakpoints.clear();
        ScriptLogger.getInstance().debug("Debug: Cleared all breakpoints");
    }

    public List<Integer> getBreakpoints(String scriptName) {
        if (!breakpoints.containsKey(scriptName))
            return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(breakpoints.get(scriptName).keySet());
    }

    public int getCurrentLine(int scriptId) {
        Task task = TaskManager.getInstance().getTask(scriptId);
        if (task != null) {
            return task.getCurrentLine();
        }
        return -1;
    }

    public DebugState getDebugState(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            return session.getState();
        }
        return DebugState.RUNNING;
    }

    public List<DebugFrame> getCallStack(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            return session.getCallStack();
        }
        return new java.util.ArrayList<>();
    }

    public void pushFrame(int scriptId, int line, String functionName) {
        DebugSession session = sessions.computeIfAbsent(scriptId, DebugSession::new);
        int depth = session.getCallDepth();
        DebugFrame frame = new DebugFrame(line, functionName, depth);
        session.pushFrame(frame);
        ScriptLogger.getInstance().debug("Debug: Pushed frame {} for script {}", frame, scriptId);
    }

    public void popFrame(int scriptId) {
        DebugSession session = sessions.get(scriptId);
        if (session != null) {
            DebugFrame frame = session.popFrame();
            if (frame != null) {
                ScriptLogger.getInstance().debug("Debug: Popped frame {} for script {}", frame, scriptId);
            }
        }
    }

    public void cleanupSession(int scriptId) {
        DebugSession session = sessions.remove(scriptId);
        if (session != null) {
            session.clearCallStack();
        }
        ScriptLogger.getInstance().debug("Debug: Cleaned up session for script {}", scriptId);
    }

    public void setVariable(int scriptId, String name, String value) {
        Task task = TaskManager.getInstance().getTask(scriptId);
        if (task != null) {
            task.setVariable(name, value);
            ScriptLogger.getInstance().debug("Debug: Set variable " + name + " = " + value + " in script " + scriptId);
        } else {
            ScriptLogger.getInstance()
                    .warn("Debug: Cannot set variable " + name + " - Script " + scriptId + " not found");
        }
    }
}

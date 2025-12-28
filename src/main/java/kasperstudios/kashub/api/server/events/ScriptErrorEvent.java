package kasperstudios.kashub.api.server.events;

/**
 * WebSocket event for script errors
 */
public class ScriptErrorEvent {
    public final String type = "script_error";
    public int taskId;
    public String error;
    public int line;
    public long timestamp;
    
    public ScriptErrorEvent(int taskId, String error, int line, long timestamp) {
        this.taskId = taskId;
        this.error = error;
        this.line = line;
        this.timestamp = timestamp;
    }
}

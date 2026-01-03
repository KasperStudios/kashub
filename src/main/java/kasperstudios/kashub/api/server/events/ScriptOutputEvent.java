package kasperstudios.kashub.api.server.events;

public class ScriptOutputEvent {
    public final String type = "script_output";
    public int taskId;
    public String message;
    public String level;
    public long timestamp;

    public ScriptOutputEvent(int taskId, String message, String level, long timestamp) {
        this.taskId = taskId;
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
    }
}

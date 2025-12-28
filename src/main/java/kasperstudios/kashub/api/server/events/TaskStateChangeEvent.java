package kasperstudios.kashub.api.server.events;

/**
 * WebSocket event for task state changes
 */
public class TaskStateChangeEvent {
    public final String type = "task_state_change";
    public int taskId;
    public String taskName;
    public String state; // "RUNNING", "PAUSED", "STOPPED", "ERROR"
    public long timestamp;
    
    public TaskStateChangeEvent(int taskId, String taskName, String state, long timestamp) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.state = state;
        this.timestamp = timestamp;
    }
}

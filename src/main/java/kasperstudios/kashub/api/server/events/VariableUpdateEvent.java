package kasperstudios.kashub.api.server.events;

/**
 * WebSocket event for variable updates
 */
public class VariableUpdateEvent {
    public final String type = "variable_update";
    public String variable;
    public String value;
    public long timestamp;
    
    public VariableUpdateEvent(String variable, String value, long timestamp) {
        this.variable = variable;
        this.value = value;
        this.timestamp = timestamp;
    }
}

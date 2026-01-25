package kasperstudios.kashub.core;

/**
 * State - Represents the current execution state of a task.
 */
public enum State {
    RUNNING(0xFF2ECC71, "Running"),
    PAUSED(0xFFF1C40F, "Paused"),
    STOPPED(0xFF95A5A6, "Stopped"),
    ERROR(0xFFE74C3C, "Error");

    private final int color;
    private final String displayName;

    State(int color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public int getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }
}

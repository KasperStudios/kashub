package kasperstudios.kashub.runtime;

/**
 * Состояния выполнения скрипта
 */
public enum ScriptState {
    RUNNING("Running", 0xFF50FA7B),
    PAUSED("Paused", 0xFFFFB86C),
    STOPPED("Stopped", 0xFF6272A4),
    ERROR("Error", 0xFFFF5555),
    WAITING("Waiting", 0xFF8BE9FD);

    private final String displayName;
    private final int color;

    ScriptState(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}

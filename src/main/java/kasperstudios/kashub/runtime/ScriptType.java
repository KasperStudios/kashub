package kasperstudios.kashub.runtime;

/**
 * Типы скриптов по источнику
 */
public enum ScriptType {
    USER("User", 0xFF50FA7B, true),
    SYSTEM("System", 0xFF8BE9FD, false),
    REMOTE("Remote", 0xFFFFB86C, false);

    private final String displayName;
    private final int color;
    private final boolean editable;

    ScriptType(String displayName, int color, boolean editable) {
        this.displayName = displayName;
        this.color = color;
        this.editable = editable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public boolean isEditable() {
        return editable;
    }
}

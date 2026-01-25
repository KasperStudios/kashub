package kasperstudios.kashub.core;

/**
 * Type - Represents the source/type of a script.
 */
public enum Type {
    SYSTEM(false),
    USER(true),
    REMOTE(false),
    MODPACK(false),
    AI(false);

    private final boolean editable;

    Type(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return editable;
    }
}

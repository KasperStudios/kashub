package kasperstudios.kashub.algorithm.events;

/**
 * Базовый класс для событий скриптинга
 */
public abstract class ScriptEvent {
    private final String name;
    private boolean cancelled = false;

    public ScriptEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

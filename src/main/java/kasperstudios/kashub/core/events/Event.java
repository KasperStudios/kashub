package kasperstudios.kashub.core.events;

public abstract class Event {
    private final String name;
    private boolean cancelled = false;

    public Event(String name) {
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

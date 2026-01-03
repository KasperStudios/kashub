package kasperstudios.kashub.algorithm.events;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {
    private static EventManager instance;

    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final Map<String, String> eventScripts = new ConcurrentHashMap<>();

    private static final Set<String> IMPLEMENTED_EVENTS = Set.of(
        "onTick",
        "onDamage",
        "onHeal",
        "onHunger",
        "onDeath",
        "onChat",
        "onBlockBreak",
        "onBlockPlace",
        "onAttack"
    );

    private static final Set<String> WIP_EVENTS = Set.of(
        "onRespawn",
        "onJump",
        "onSneak",
        "onSprint",
        "onItemUse",
        "onInventoryChange"
    );

    private float lastHealth = 20.0f;
    private int lastFood = 20;
    private int tickCounter = 0;

    private EventManager() {
        initializeDefaultEvents();
    }

    public static EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    private void initializeDefaultEvents() {

        handlers.put("onTick", new ArrayList<>());
        handlers.put("onDamage", new ArrayList<>());
        handlers.put("onHeal", new ArrayList<>());
        handlers.put("onHunger", new ArrayList<>());
        handlers.put("onChat", new ArrayList<>());
        handlers.put("onDeath", new ArrayList<>());
        handlers.put("onRespawn", new ArrayList<>());
        handlers.put("onJump", new ArrayList<>());
        handlers.put("onSneak", new ArrayList<>());
        handlers.put("onSprint", new ArrayList<>());
        handlers.put("onAttack", new ArrayList<>());
        handlers.put("onBlockBreak", new ArrayList<>());
        handlers.put("onBlockPlace", new ArrayList<>());
        handlers.put("onItemUse", new ArrayList<>());
        handlers.put("onInventoryChange", new ArrayList<>());
    }

    public void registerEventScript(String eventName, String scriptCode) {
        // Check if event is known
        if (!IMPLEMENTED_EVENTS.contains(eventName) && !WIP_EVENTS.contains(eventName)) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().warn(
                "Unknown event '" + eventName + "'. Available events: " +
                String.join(", ", getAvailableEvents()));
            return;
        }

        if (WIP_EVENTS.contains(eventName)) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().warn(
                "Event '" + eventName + "' is declared but not yet implemented (WIP).");
            kasperstudios.kashub.util.ScriptLogger.getInstance().warn(
                "The script will be registered but may not trigger until the event is implemented.");
        }

        eventScripts.put(eventName, scriptCode);
        kasperstudios.kashub.util.ScriptLogger.getInstance().debug(
            "EventManager: Registered script for " + eventName + ", total events: " + eventScripts.size());
    }

    public void unregisterEventScript(String eventName) {
        eventScripts.remove(eventName);
    }

    public void registerHandler(String eventName, EventHandler handler) {
        handlers.computeIfAbsent(eventName, k -> new ArrayList<>()).add(handler);
    }

    public void unregisterHandler(String eventName, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
        }
    }

    public void fireEvent(String eventName, Map<String, Object> data) {
        // Fire to registered handlers
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(data);
                } catch (Exception e) {
                    kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                        "Error in event handler for " + eventName + ": " + e.getMessage());
                }
            }
        }

        String script = eventScripts.get(eventName);
        if (script != null && !script.isEmpty()) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().debug(
                "Firing event " + eventName + " with script: " + script.substring(0, Math.min(50, script.length())) + "...");
            try {
                ScriptInterpreter interpreter = ScriptInterpreter.getInstance();

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    interpreter.setVariable("event_" + entry.getKey(), String.valueOf(entry.getValue()));
                }
                interpreter.parseCommands(script);
                interpreter.executeQueuedCommands();
            } catch (Exception e) {
                kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                    "Error executing event script for " + eventName + ": " + e.getMessage());
            }
        } else {
            if (eventName.equals("onTick")) {
                kasperstudios.kashub.util.ScriptLogger.getInstance().debug(
                    "EventManager: onTick fired but no script registered (total scripts: " + eventScripts.size() + ")");
            }
        }
    }

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        tickCounter++;

        if (tickCounter % 20 == 0) {
            Map<String, Object> tickData = new HashMap<>();
            tickData.put("tick", tickCounter);
            tickData.put("time", System.currentTimeMillis());
            kasperstudios.kashub.util.ScriptLogger.getInstance().debug(
                "EventManager.tick(): Firing onTick event, registered scripts: " + eventScripts.size());
            fireEvent("onTick", tickData);
        }

        float currentHealth = player.getHealth();
        if (currentHealth < lastHealth) {
            Map<String, Object> damageData = new HashMap<>();
            damageData.put("damage", lastHealth - currentHealth);
            damageData.put("health", currentHealth);
            damageData.put("maxHealth", player.getMaxHealth());
            fireEvent("onDamage", damageData);
        } else if (currentHealth > lastHealth) {
            Map<String, Object> healData = new HashMap<>();
            healData.put("healed", currentHealth - lastHealth);
            healData.put("health", currentHealth);
            healData.put("maxHealth", player.getMaxHealth());
            fireEvent("onHeal", healData);
        }
        lastHealth = currentHealth;

        int currentFood = player.getHungerManager().getFoodLevel();
        if (currentFood != lastFood) {
            Map<String, Object> hungerData = new HashMap<>();
            hungerData.put("food", currentFood);
            hungerData.put("previousFood", lastFood);
            hungerData.put("saturation", player.getHungerManager().getSaturationLevel());
            fireEvent("onHunger", hungerData);
        }
        lastFood = currentFood;

        if (player.isDead()) {
            Map<String, Object> deathData = new HashMap<>();
            deathData.put("position_x", player.getX());
            deathData.put("position_y", player.getY());
            deathData.put("position_z", player.getZ());
            fireEvent("onDeath", deathData);
        }
    }

    public void clear() {
        kasperstudios.kashub.util.ScriptLogger.getInstance().debug(
            "EventManager: Clearing all events (had " + eventScripts.size() + " scripts)");
        for (List<EventHandler> handlerList : handlers.values()) {
            handlerList.clear();
        }
        eventScripts.clear();
    }

    public Set<String> getAvailableEvents() {
        Set<String> allEvents = new java.util.HashSet<>();
        allEvents.addAll(IMPLEMENTED_EVENTS);
        allEvents.addAll(WIP_EVENTS);
        return allEvents;
    }

    public Set<String> getImplementedEvents() {
        return new java.util.HashSet<>(IMPLEMENTED_EVENTS);
    }

    public boolean isEventImplemented(String eventName) {
        return IMPLEMENTED_EVENTS.contains(eventName);
    }

    @FunctionalInterface
    public interface EventHandler {
        void handle(Map<String, Object> data);
    }
}

package kasperstudios.kashub.algorithm.events;

import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Менеджер событий для скриптов
 * Позволяет регистрировать обработчики на различные игровые события
 */
public class EventManager {
    private static EventManager instance;
    
    private final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final Map<String, String> eventScripts = new ConcurrentHashMap<>();
    
    // Состояние для отслеживания изменений
    private float lastHealth = 20.0f;
    private int lastFood = 20;
    private long lastTickTime = 0;
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
        // Регистрируем стандартные типы событий
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

    /**
     * Регистрирует скрипт для выполнения при событии
     */
    public void registerEventScript(String eventName, String scriptCode) {
        eventScripts.put(eventName, scriptCode);
    }

    /**
     * Удаляет скрипт события
     */
    public void unregisterEventScript(String eventName) {
        eventScripts.remove(eventName);
    }

    /**
     * Регистрирует обработчик события
     */
    public void registerHandler(String eventName, EventHandler handler) {
        handlers.computeIfAbsent(eventName, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Удаляет обработчик события
     */
    public void unregisterHandler(String eventName, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
        }
    }

    /**
     * Вызывает событие
     */
    public void fireEvent(String eventName, Map<String, Object> data) {
        // Выполняем зарегистрированные обработчики
        List<EventHandler> eventHandlers = handlers.get(eventName);
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(data);
                } catch (Exception e) {
                    System.err.println("Error in event handler for " + eventName + ": " + e.getMessage());
                }
            }
        }

        // Выполняем скрипт события
        String script = eventScripts.get(eventName);
        if (script != null && !script.isEmpty()) {
            try {
                ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
                // Устанавливаем переменные события
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    interpreter.setVariable("event_" + entry.getKey(), String.valueOf(entry.getValue()));
                }
                interpreter.parseCommands(script);
                interpreter.executeQueuedCommands();
            } catch (Exception e) {
                System.err.println("Error executing event script for " + eventName + ": " + e.getMessage());
            }
        }
    }

    /**
     * Вызывается каждый тик для проверки событий
     */
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) return;

        tickCounter++;
        
        // onTick - каждые 20 тиков (1 секунда)
        if (tickCounter % 20 == 0) {
            Map<String, Object> tickData = new HashMap<>();
            tickData.put("tick", tickCounter);
            tickData.put("time", System.currentTimeMillis());
            fireEvent("onTick", tickData);
        }

        // Проверяем изменение здоровья
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

        // Проверяем изменение голода
        int currentFood = player.getHungerManager().getFoodLevel();
        if (currentFood != lastFood) {
            Map<String, Object> hungerData = new HashMap<>();
            hungerData.put("food", currentFood);
            hungerData.put("previousFood", lastFood);
            hungerData.put("saturation", player.getHungerManager().getSaturationLevel());
            fireEvent("onHunger", hungerData);
        }
        lastFood = currentFood;

        // Проверяем смерть
        if (player.isDead()) {
            Map<String, Object> deathData = new HashMap<>();
            deathData.put("position_x", player.getX());
            deathData.put("position_y", player.getY());
            deathData.put("position_z", player.getZ());
            fireEvent("onDeath", deathData);
        }
    }

    /**
     * Вызывается при получении сообщения в чат
     */
    public void onChatMessage(String message, String sender) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("message", message);
        chatData.put("sender", sender);
        fireEvent("onChat", chatData);
    }

    /**
     * Очищает все обработчики и скрипты
     */
    public void clear() {
        for (List<EventHandler> handlerList : handlers.values()) {
            handlerList.clear();
        }
        eventScripts.clear();
    }

    /**
     * Получает список всех доступных событий
     */
    public Set<String> getAvailableEvents() {
        return handlers.keySet();
    }

    /**
     * Интерфейс обработчика события
     */
    @FunctionalInterface
    public interface EventHandler {
        void handle(Map<String, Object> data);
    }
}

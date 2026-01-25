package kasperstudios.kashub.services.modpack;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.events.EventManager;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalEventHooks - Global event hooks for server/modpack scripts.
 * 
 * Allows modpack scripts to register global event handlers that execute
 * for all players and persist across sessions.
 * 
 * Part of v0.9.0 Server & Modpack Scripts feature.
 * 
 * @since 0.9.0
 */
public class GlobalEventHooks {
    
    private static volatile GlobalEventHooks instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, List<GlobalHook>> globalHooks;
    
    private GlobalEventHooks() {
        this.globalHooks = new ConcurrentHashMap<>();
    }
    
    public static GlobalEventHooks getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new GlobalEventHooks();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a global event hook.
     * 
     * @param eventName Event name (e.g., "onTick", "onPlayerJoin")
     * @param hookName Unique hook identifier
     * @param scriptCode Script code to execute
     * @param priority Execution priority (higher = earlier)
     */
    public void registerHook(String eventName, String hookName, String scriptCode, int priority) {
        GlobalHook hook = new GlobalHook(hookName, scriptCode, priority);
        
        globalHooks.computeIfAbsent(eventName, k -> new ArrayList<>()).add(hook);
        
        // Sort by priority (descending)
        globalHooks.get(eventName).sort((a, b) -> Integer.compare(b.priority, a.priority));
        
        // Register with EventManager
        EventManager.getInstance().registerEventScript(eventName, scriptCode, "global:" + hookName);
        
        ScriptLogger.getInstance().info("Registered global hook '" + hookName + "' for event '" + eventName + "'");
    }
    
    /**
     * Unregister a global event hook.
     */
    public void unregisterHook(String eventName, String hookName) {
        List<GlobalHook> hooks = globalHooks.get(eventName);
        if (hooks != null) {
            hooks.removeIf(h -> h.name.equals(hookName));
            ScriptLogger.getInstance().info("Unregistered global hook '" + hookName + "'");
        }
    }
    
    /**
     * Get all hooks for an event.
     */
    public List<GlobalHook> getHooks(String eventName) {
        return globalHooks.getOrDefault(eventName, Collections.emptyList());
    }
    
    /**
     * Clear all hooks for an event.
     */
    public void clearHooks(String eventName) {
        globalHooks.remove(eventName);
        ScriptLogger.getInstance().info("Cleared all hooks for event '" + eventName + "'");
    }
    
    /**
     * Clear all global hooks.
     */
    public void clearAllHooks() {
        globalHooks.clear();
        ScriptLogger.getInstance().info("Cleared all global hooks");
    }
    
    /**
     * Get all registered event names.
     */
    public Set<String> getRegisteredEvents() {
        return globalHooks.keySet();
    }
    
    /**
     * GlobalHook class.
     */
    public static class GlobalHook {
        private final String name;
        private final String scriptCode;
        private final int priority;
        
        public GlobalHook(String name, String scriptCode, int priority) {
            this.name = name;
            this.scriptCode = scriptCode;
            this.priority = priority;
        }
        
        public String getName() {
            return name;
        }
        
        public String getScriptCode() {
            return scriptCode;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}

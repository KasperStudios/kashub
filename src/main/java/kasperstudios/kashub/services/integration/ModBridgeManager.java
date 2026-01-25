package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ModBridgeManager - Universal integration system for other mods.
 * 
 * Allows scripts to check for mod presence, call mod APIs via reflection,
 * and adapt behavior dynamically based on available mods.
 * 
 * Part of v0.9.0 Universal Integration System feature.
 * 
 * @since 0.9.0
 */
public class ModBridgeManager {
    
    private static volatile ModBridgeManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, ModInfo> loadedMods;
    private final Map<String, Object> modInstances;
    
    private ModBridgeManager() {
        this.loadedMods = new ConcurrentHashMap<>();
        this.modInstances = new ConcurrentHashMap<>();
        discoverMods();
    }
    
    public static ModBridgeManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ModBridgeManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Discover all loaded mods.
     */
    private void discoverMods() {
        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        for (ModContainer mod : mods) {
            String modId = mod.getMetadata().getId();
            String modName = mod.getMetadata().getName();
            String version = mod.getMetadata().getVersion().getFriendlyString();
            
            ModInfo info = new ModInfo(modId, modName, version);
            loadedMods.put(modId, info);
        }
        
        ScriptLogger.getInstance().info("Discovered " + loadedMods.size() + " mods");
    }
    
    /**
     * Check if a mod is loaded.
     */
    public boolean isModLoaded(String modId) {
        return loadedMods.containsKey(modId);
    }
    
    /**
     * Get mod information.
     */
    public ModInfo getModInfo(String modId) {
        return loadedMods.get(modId);
    }
    
    /**
     * Get all loaded mods.
     */
    public Collection<ModInfo> getAllMods() {
        return Collections.unmodifiableCollection(loadedMods.values());
    }
    
    /**
     * Call a mod's API method via reflection.
     * 
     * @param className Fully qualified class name
     * @param methodName Method name to call
     * @param args Method arguments
     * @return Method result or null
     */
    public Object callModMethod(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);
            
            // Find method by name and argument count
            Method method = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                    method = m;
                    break;
                }
            }
            
            if (method == null) {
                ScriptLogger.getInstance().error("Method not found: " + methodName);
                return null;
            }
            
            method.setAccessible(true);
            
            // Try to get instance or call static method
            Object instance = modInstances.get(className);
            if (instance == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                // Try to get singleton instance
                try {
                    Method getInstance = clazz.getMethod("getInstance");
                    instance = getInstance.invoke(null);
                    modInstances.put(className, instance);
                } catch (Exception e) {
                    // Try to create new instance
                    instance = clazz.getDeclaredConstructor().newInstance();
                    modInstances.put(className, instance);
                }
            }
            
            return method.invoke(instance, args);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to call mod method: " + className + "." + methodName, e);
            return null;
        }
    }
    
    /**
     * Get a field value from a mod's class via reflection.
     */
    public Object getModField(String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                return field.get(null);
            } else {
                Object instance = modInstances.get(className);
                if (instance == null) {
                    try {
                        Method getInstance = clazz.getMethod("getInstance");
                        instance = getInstance.invoke(null);
                        modInstances.put(className, instance);
                    } catch (Exception e) {
                        instance = clazz.getDeclaredConstructor().newInstance();
                        modInstances.put(className, instance);
                    }
                }
                return field.get(instance);
            }
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to get mod field: " + className + "." + fieldName, e);
            return null;
        }
    }
    
    /**
     * Set a field value in a mod's class via reflection.
     */
    public boolean setModField(String className, String fieldName, Object value) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.set(null, value);
            } else {
                Object instance = modInstances.get(className);
                if (instance == null) {
                    try {
                        Method getInstance = clazz.getMethod("getInstance");
                        instance = getInstance.invoke(null);
                        modInstances.put(className, instance);
                    } catch (Exception e) {
                        instance = clazz.getDeclaredConstructor().newInstance();
                        modInstances.put(className, instance);
                    }
                }
                field.set(instance, value);
            }
            
            return true;
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to set mod field: " + className + "." + fieldName, e);
            return false;
        }
    }
    
    /**
     * ModInfo class.
     */
    public static class ModInfo {
        private final String modId;
        private final String name;
        private final String version;
        
        public ModInfo(String modId, String name, String version) {
            this.modId = modId;
            this.name = name;
            this.version = version;
        }
        
        public String getModId() {
            return modId;
        }
        
        public String getName() {
            return name;
        }
        
        public String getVersion() {
            return version;
        }
        
        @Override
        public String toString() {
            return name + " (" + modId + ") v" + version;
        }
    }
}

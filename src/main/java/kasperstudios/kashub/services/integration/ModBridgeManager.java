package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.PlatformModInfo;

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
    private final Map<String, Method> methodCache;
    
    private ModBridgeManager() {
        this.loadedMods = new ConcurrentHashMap<>();
        this.modInstances = new ConcurrentHashMap<>();
        this.methodCache = new ConcurrentHashMap<>();
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
        for (PlatformModInfo mod : KHWrapper.get().getAllMods()) {
            String modId = mod.getId();
            String modName = mod.getName();
            String version = mod.getVersion();
            
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
            return callModMethodOnClass(clazz, null, methodName, args);
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to call mod method on class: " + className + "." + methodName, e);
            return null;
        }
    }

    /**
     * Call a method on a specific target object via reflection.
     * 
     * @param target The object to call the method on
     * @param methodName Method name to call
     * @param args Method arguments
     * @return Method result or null
     */
    public Object callModMethod(Object target, String methodName, Object... args) {
        if (target == null) return null;
        return callModMethodOnClass(target.getClass(), target, methodName, args);
    }

    private Object callModMethodOnClass(Class<?> clazz, Object target, String methodName, Object... args) {
        try {
            String cacheKey = clazz.getName() + ":" + methodName + ":" + args.length;
            Method method = methodCache.get(cacheKey);
            
            if (method == null) {
                // Find method by name and argument count
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                        method = m;
                        break;
                    }
                }
                
                if (method == null) {
                    // Try superclasses
                    for (Method m : clazz.getMethods()) {
                        if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                            method = m;
                            break;
                        }
                    }
                }

                if (method == null) {
                    // Return null and let caller handle it, avoids log spam in tick loops
                    return null;
                }
                
                method.setAccessible(true);
                methodCache.put(cacheKey, method);
            }
            
            Object instance = target;
            if (instance == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                String className = clazz.getName();
                // Try to get instance from cache or create
                instance = modInstances.get(className);
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
            }
            
            return method.invoke(instance, args);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to call mod method: " + methodName, e);
            return null;
        }
    }
    
    /**
     * Get a field value from a specific object instance via reflection.
     */
    public Object getModField(Object target, String fieldName) {
        if (target == null) return null;
        try {
            Class<?> clazz = target.getClass();
            java.lang.reflect.Field field = null;
            
            // Try to find field in class or superclasses
            Class<?> current = clazz;
            while (current != null && field == null) {
                try {
                    field = current.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            
            if (field == null) return null;
            
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to get mod field: " + target.getClass().getName() + "." + fieldName, e);
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

package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * UIIntegrationManager - Add custom buttons/widgets to other mods' screens.
 * 
 * Allows scripts to inject UI elements into screens from other mods.
 * Uses reflection to access screen internals safely.
 * 
 * @since 0.9.0
 */
public class UIIntegrationManager {
    
    private static volatile UIIntegrationManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, UIHook> hooks = new ConcurrentHashMap<>();
    
    private UIIntegrationManager() {}
    
    public static UIIntegrationManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UIIntegrationManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register UI hook for screen class.
     */
    public void registerHook(String screenClassName, String buttonText, int x, int y, int width, int height, Consumer<Screen> onClick) {
        UIHook hook = new UIHook(screenClassName, buttonText, x, y, width, height, onClick);
        hooks.put(screenClassName, hook);
        Kashub.LOGGER.info("Registered UI hook for: " + screenClassName);
    }
    
    /**
     * Unregister UI hook.
     */
    public void unregisterHook(String screenClassName) {
        hooks.remove(screenClassName);
        Kashub.LOGGER.info("Unregistered UI hook for: " + screenClassName);
    }
    
    /**
     * Clear all hooks.
     */
    public void clearHooks() {
        hooks.clear();
    }
    
    /**
     * Try to inject UI elements into current screen.
     */
    public void injectIntoScreen(Screen screen) {
        if (screen == null) return;
        
        String className = screen.getClass().getName();
        UIHook hook = hooks.get(className);
        
        if (hook != null) {
            try {
                injectButton(screen, hook);
            } catch (Exception e) {
                Kashub.LOGGER.error("Failed to inject UI into " + className, e);
            }
        }
    }
    
    /**
     * Inject button into screen using reflection.
     */
    private void injectButton(Screen screen, UIHook hook) throws Exception {
        // Create button
        ButtonWidget button = ButtonWidget.builder(
            Text.literal(hook.buttonText),
            btn -> hook.onClick.accept(screen)
        )
        .dimensions(hook.x, hook.y, hook.width, hook.height)
        .build();
        
        // Try to add button using reflection
        try {
            // Method 1: Try addDrawableChild (modern Minecraft)
            Method addDrawableChild = Screen.class.getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Element.class);
            addDrawableChild.setAccessible(true);
            addDrawableChild.invoke(screen, button);
            Kashub.LOGGER.info("Button injected successfully");
        } catch (NoSuchMethodException e) {
            // Method 2: Try direct field access
            Field drawablesField = Screen.class.getDeclaredField("drawables");
            drawablesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> drawables = (java.util.List<Object>) drawablesField.get(screen);
            drawables.add(button);
            
            Field selectablesField = Screen.class.getDeclaredField("selectables");
            selectablesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Object> selectables = (java.util.List<Object>) selectablesField.get(screen);
            selectables.add(button);
            
            Kashub.LOGGER.info("Button injected via field access");
        }
    }
    
    /**
     * Get screen class name from current screen.
     */
    public String getCurrentScreenClass() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) {
            return mc.currentScreen.getClass().getName();
        }
        return null;
    }
    
    /**
     * Check if screen is open.
     */
    public boolean isScreenOpen(String screenClassName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) {
            return mc.currentScreen.getClass().getName().equals(screenClassName);
        }
        return false;
    }
    
    /**
     * UI hook data.
     */
    private static class UIHook {
        final String screenClassName;
        final String buttonText;
        final int x, y, width, height;
        final Consumer<Screen> onClick;
        
        UIHook(String screenClassName, String buttonText, int x, int y, int width, int height, Consumer<Screen> onClick) {
            this.screenClassName = screenClassName;
            this.buttonText = buttonText;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.onClick = onClick;
        }
    }
}

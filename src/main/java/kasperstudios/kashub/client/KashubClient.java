package kasperstudios.kashub.client;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.commands.PathfindCommand;
import kasperstudios.kashub.algorithm.events.EventManager;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.editor.ModernEditorScreen;
import kasperstudios.kashub.network.AnimationManager;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import kasperstudios.kashub.services.HttpService;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KashubClient implements ClientModInitializer {
    public static KeyBinding openEditorKey;
    public static KeyBinding stopScriptsKey;
    public static KeyBinding openAiAgentKey;
    
    private static long lastKeyPress = 0;
    private static final long KEY_COOLDOWN = 200;
    private static boolean autorunExecuted = false;
    
    @Override
    public void onInitializeClient() {
        Kashub.LOGGER.info("Kashub Client initializing...");
        
        // Initialize command registry (needed for client-only mode when mod is not on server)
        CommandRegistry.initialize();
        
        // Load config
        KashubConfig config = KashubConfig.getInstance();
        ScriptLogger.getInstance().info("Kashub Client v3.0 starting...");
        
        // Start file watcher for hot-reload
        ScriptFileWatcher.getInstance().start();
        
        // Run autorun scripts after world is loaded (only once)
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!autorunExecuted && client.player != null && config.autorunEnabled && !config.autorunScripts.isEmpty()) {
                autorunExecuted = true;
                runAutorunScripts();
            }
        });
        
        // Register keybindings
        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.kashub.open_editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.kashub"
        ));
        
        stopScriptsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.kashub.stop_scripts",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "category.kashub"
        ));
        
        openAiAgentKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.kashub.open_ai_agent",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            "category.kashub"
        ));
        
        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            long currentTime = System.currentTimeMillis();
            
            try {
                // Process script events
                EventManager.getInstance().tick();
                
                // Process script tasks
                ScriptTaskManager.getInstance().tick();
                
                // Process HTTP callbacks
                HttpService.getInstance().tick();
                
                // Process animations
                AnimationManager.getInstance().tick();
                
                // Process pathfinding
                PathfindCommand.tick();
                
                // Check keybindings with cooldown
                if (openEditorKey.wasPressed() && currentTime - lastKeyPress > KEY_COOLDOWN) {
                    lastKeyPress = currentTime;
                    client.setScreen(new ModernEditorScreen());
                }
                
                if (openAiAgentKey.wasPressed() && currentTime - lastKeyPress > KEY_COOLDOWN) {
                    lastKeyPress = currentTime;
                    client.setScreen(new kasperstudios.kashub.gui.AiAgentScreen());
                }
                
                if (stopScriptsKey.wasPressed()) {
                    ScriptTaskManager.getInstance().stopAll();
                    ScriptLogger.getInstance().warn("All scripts stopped by hotkey");
                }
                
                // Check script keybinds (only when not in GUI)
                if (client.currentScreen == null) {
                    checkScriptKeybinds(client);
                }
            } catch (Exception e) {
                Kashub.LOGGER.error("Error in client tick", e);
            }
        });
        
        ScriptLogger.getInstance().success("Kashub Client initialized!");
    }
    
    /**
     * Run autorun scripts configured in config
     */
    private static void runAutorunScripts() {
        KashubConfig config = KashubConfig.getInstance();
        if (!config.autorunEnabled || config.autorunScripts.isEmpty()) {
            return;
        }
        
        ScriptLogger.getInstance().info("Running autorun scripts: " + config.autorunScripts.size());
        
        for (String scriptName : config.autorunScripts) {
            try {
                ScriptTaskManager.getInstance().startScriptFromFile(scriptName);
                ScriptLogger.getInstance().info("Autorun: Started " + scriptName);
            } catch (Exception e) {
                ScriptLogger.getInstance().error("Autorun: Failed to start " + scriptName + ": " + e.getMessage());
            }
        }
    }
    
    private static final java.util.Set<Integer> pressedKeys = new java.util.HashSet<>();
    
    private void checkScriptKeybinds(MinecraftClient client) {
        KashubConfig config = KashubConfig.getInstance();
        long handle = client.getWindow().getHandle();
        
        for (java.util.Map.Entry<Integer, String> entry : config.scriptKeybinds.entrySet()) {
            int keyCode = entry.getKey();
            String scriptName = entry.getValue();
            
            boolean isPressed = InputUtil.isKeyPressed(handle, keyCode);
            
            if (isPressed && !pressedKeys.contains(keyCode)) {
                // Key just pressed - run script
                pressedKeys.add(keyCode);
                ScriptTaskManager.getInstance().startScriptFromFile(scriptName);
            } else if (!isPressed && pressedKeys.contains(keyCode)) {
                // Key released
                pressedKeys.remove(keyCode);
            }
        }
    }
}

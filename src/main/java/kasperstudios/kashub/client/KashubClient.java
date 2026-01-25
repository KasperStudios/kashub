package kasperstudios.kashub.client;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.events.EventManager;
import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.editor.ModernEditorScreen;
import kasperstudios.kashub.network.AnimationManager;
import kasperstudios.kashub.network.NetworkingManager;
import kasperstudios.kashub.services.network.PacketManager;
import kasperstudios.kashub.services.network.ConfigSyncManager;
import kasperstudios.kashub.services.modpack.ModpackScriptManager;
import kasperstudios.kashub.services.discord.DiscordRichPresence;
import kasperstudios.kashub.core.Environment;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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

    // v0.9.0 - Discord Rich Presence update timer
    private static long lastDiscordUpdate = 0;
    private static final long DISCORD_UPDATE_INTERVAL = 15000; // 15 seconds

    @Override
    public void onInitializeClient() {
        Kashub.LOGGER.info("Kashub Client initializing...");

        Registry.initialize();

        KashubConfig config = KashubConfig.getInstance();
        ScriptLogger.getInstance().info("Kashub Client v3.0 starting...");

        ScriptFileWatcher.getInstance().start();

        if (config.apiEnabled) {
            KashubAPIServer.getInstance().start();
        }

        // v0.9.0 - Discord Rich Presence
        DiscordRichPresence.getInstance().initialize();

        // v0.9.0 - Network packet handling
        PacketManager.getInstance().initialize();
        NetworkingManager.getInstance().initialize(); // Server-Authoritative integration

        // v0.9.0 - Client config enforcement
        ConfigSyncManager.getInstance().initialize();

        // v0.9.0 - Shutdown modpack scripts when disconnecting from world
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ModpackScriptManager modpackManager = ModpackScriptManager.getInstance();
            if (modpackManager.isInitialized()) {
                modpackManager.shutdown();
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!autorunExecuted && client.player != null && config.autorunEnabled
                    && !config.autorunScripts.isEmpty()) {
                autorunExecuted = true;
                runAutorunScripts();
            }

            // v0.9.0 - Initialize modpack scripts when world loads
            if (client.player != null && client.world != null) {
                ModpackScriptManager modpackManager = ModpackScriptManager.getInstance();
                if (!modpackManager.isInitialized()) {
                    modpackManager.initialize();
                }
            }
        });

        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kashub.open_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.kashub"));

        stopScriptsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kashub.stop_scripts",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.kashub"));

        openAiAgentKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.kashub.open_ai_agent",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                "category.kashub"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            long currentTime = System.currentTimeMillis();

            try {
                // Update environment variables (new v0.9.0 provider)
                Environment.getInstance().update();

                EventManager.getInstance().tick();

                TaskManager.getInstance().tick();

                AnimationManager.getInstance().tick();

                // v0.9.0-beta - Pathfinding service tick
                kasperstudios.kashub.services.PathfindingService.getInstance().tick();

                // v0.9.0 - Update Discord Rich Presence every 15 seconds
                if (currentTime - lastDiscordUpdate > DISCORD_UPDATE_INTERVAL) {
                    lastDiscordUpdate = currentTime;
                    DiscordRichPresence.getInstance().updatePresence();
                }

                if (openEditorKey.wasPressed() && currentTime - lastKeyPress > KEY_COOLDOWN) {
                    lastKeyPress = currentTime;

                    if (kasperstudios.kashub.network.ServerModeManager.getInstance().isEditorAllowed()) {
                        client.setScreen(new ModernEditorScreen());
                    } else {
                        kasperstudios.kashub.util.ScriptLogger.getInstance().error("Editor is disabled by server.");
                        kasperstudios.kashub.network.dto.ServerConfig serverConfig = kasperstudios.kashub.network.ServerModeManager
                                .getInstance().getServerConfig();
                        if (serverConfig != null) {
                            kasperstudios.kashub.util.ScriptLogger.getInstance()
                                    .error(serverConfig.getMessageOnDisabled());
                        }
                    }
                }

                if (openAiAgentKey.wasPressed() && currentTime - lastKeyPress > KEY_COOLDOWN) {
                    lastKeyPress = currentTime;
                    client.setScreen(new kasperstudios.kashub.gui.AiAgentScreen());
                }

                if (stopScriptsKey.wasPressed()) {
                    TaskManager.getInstance().stopAll();
                    ScriptLogger.getInstance().warn("All scripts stopped by hotkey");
                }

                if (client.currentScreen == null) {
                    checkScriptKeybinds(client);
                }
            } catch (Exception e) {
                Kashub.LOGGER.error("Error in client tick", e);
            }
        });

        ScriptLogger.getInstance().success("Kashub Client initialized!");
    }

    private static void runAutorunScripts() {
        KashubConfig config = KashubConfig.getInstance();
        if (!config.autorunEnabled || config.autorunScripts.isEmpty()) {
            return;
        }

        ScriptLogger.getInstance().info("Running autorun scripts: " + config.autorunScripts.size());

        for (String scriptName : config.autorunScripts) {
            try {
                TaskManager.getInstance().startScriptFromFile(scriptName);
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

                pressedKeys.add(keyCode);
                TaskManager.getInstance().startScriptFromFile(scriptName);
            } else if (!isPressed && pressedKeys.contains(keyCode)) {

                pressedKeys.remove(keyCode);
            }
        }
    }
}

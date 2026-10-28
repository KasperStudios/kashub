package kasperstudios.kashub.client;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.events.EventManager;
import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.KashubEditorContext;
import kasperstudios.khgui.KHGuiScreens;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import kasperstudios.kashub.network.AnimationManager;
import kasperstudios.kashub.network.NetworkingManager;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import kasperstudios.kashub.services.modpack.ModpackScriptManager;
import kasperstudios.kashub.services.discord.DiscordRichPresence;
import kasperstudios.kashub.core.Environment;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.util.ScriptFileWatcher;
import kasperstudios.kashub.util.ScriptLogger;
import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.KHClientWrapper;
import com.kasperstudios.khwrapper.IKeyBindingHandle;
import org.lwjgl.glfw.GLFW;

public class KashubClient {
    public static IKeyBindingHandle openEditorKey;
    public static IKeyBindingHandle stopScriptsKey;
    public static IKeyBindingHandle scanPeripheralKey;

    private static long lastScanTime = 0;

    private static long scanHoldStartTime = 0;
    private static final long SCAN_HOLD_DURATION = 1000; // 1 second (Better responsiveness)

    public static float getScanProgress() {
        if (scanHoldStartTime == 0) return 0f;
        return Math.min(1f, (System.currentTimeMillis() - scanHoldStartTime) / (float) SCAN_HOLD_DURATION);
    }

    private static long lastKeyPress = 0;
    private static final long KEY_COOLDOWN = 200;
    private static boolean autorunExecuted = false;

    // v0.9.0 - Discord Rich Presence update timer
    private static long lastDiscordUpdate = 0;
    private static final long DISCORD_UPDATE_INTERVAL = 15000; // 15 seconds

    public static void init() {
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


        // Register Client Handlers BEFORE NetworkingManager.initialize()
        NetworkingManager.setClientHandler(NetworkingPayloads.ConfigPayload.ID, ClientNetworkingHandlers::handleConfig);
        NetworkingManager.setClientHandler(NetworkingPayloads.ScriptExecutionPayload.ID, ClientNetworkingHandlers::handleScriptExecution);
        NetworkingManager.setClientHandler(NetworkingPayloads.BlackoutPayload.ID, ClientNetworkingHandlers::handleBlackout);
        NetworkingManager.setClientHandler(NetworkingPayloads.InjectionStatusPayload.ID, ClientNetworkingHandlers::handleInjectionStatus);
        NetworkingManager.setClientHandler(NetworkingPayloads.ImplantSyncPayload.ID, ClientNetworkingHandlers::handleImplantSync);
        NetworkingManager.setClientHandler(NetworkingPayloads.VisualEffectPayload.ID, ClientNetworkingHandlers::handleVisualEffect);
        NetworkingManager.setClientHandler(NetworkingPayloads.CCFileListPayload.ID, ClientNetworkingHandlers::handleCCFileList);
        NetworkingManager.setClientHandler(NetworkingPayloads.CCTerminalOutputPayload.ID, ClientNetworkingHandlers::handleCCTerminalOutput);
        NetworkingManager.setClientHandler(NetworkingPayloads.CCFileContentPayload.ID, ClientNetworkingHandlers::handleCCFileContent);
        NetworkingManager.setClientHandler(NetworkingPayloads.PeripheralScanResultPayload.ID, ClientNetworkingHandlers::handlePeripheralScanResult);
        
        NetworkingManager.getInstance().initialize(); // Server-Authoritative integration

        // v1.0.0 - Neural Implants visual effects
        kasperstudios.kashub.client.BlackoutRenderer.getInstance().initialize();
        kasperstudios.kashub.client.ComputerInteractionOverlay.initialize();


        // v0.9.0 - Shutdown modpack scripts when disconnecting from world
        KHClientWrapper.onClientStopping(() -> {
            ModpackScriptManager modpackManager = ModpackScriptManager.getInstance();
            if (modpackManager.isInitialized()) {
                modpackManager.shutdown();
            }
        });

        openEditorKey = KHClientWrapper.registerKeyBinding(
                "key.kashub.open_editor",
                GLFW.GLFW_KEY_K,
                "category.kashub");

        stopScriptsKey = KHClientWrapper.registerKeyBinding(
                "key.kashub.stop_scripts",
                GLFW.GLFW_KEY_Z,
                "category.kashub");

        scanPeripheralKey = KHClientWrapper.registerKeyBinding(
                "key.kashub.scan_peripheral",
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.kashub");


        KHClientWrapper.onClientTick(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                autorunExecuted = false; // Reset when leaving world so autorun works on next join!
                return;
            }

            if (!autorunExecuted && config.autorunEnabled && !config.autorunScripts.isEmpty()) {
                if (kasperstudios.kashub.services.AccessPolicy.getInstance().hasAccess(client.player)) {
                    autorunExecuted = true;
                    runAutorunScripts();
                }
            }

            // v0.9.0 - Initialize modpack scripts when world loads
            if (client.level != null) {
                ModpackScriptManager modpackManager = ModpackScriptManager.getInstance();
                if (!modpackManager.isInitialized()) {
                    modpackManager.initialize();
                }
            }

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

                    if (!kasperstudios.kashub.services.AccessPolicy.getInstance().hasAccess(client.player)) {
                        client.player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.kashub.no_implant").withStyle(net.minecraft.ChatFormatting.RED),
                            true
                        );
                    } else if (kasperstudios.kashub.network.ServerModeManager.getInstance().isEditorAllowed()) {
                        // Create a default context and open editor
                        KashubEditorContext context = new KashubEditorContext(null);
                        client.setScreen(KHGuiScreens.createCodeEditorScreen(context));
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

                if (stopScriptsKey.wasPressed()) {
                    TaskManager.getInstance().stopAll();
                    ScriptLogger.getInstance().warn("All scripts stopped by hotkey");
                }


                int implantTier = kasperstudios.kashub.services.AccessPolicy.getInstance().getImplantTier(client.player);

                // SCAN LOGIC - Only if no screen is open
                if (client.screen == null) {
                    boolean isHoldingScan = KHClientWrapper.isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT);
                    
                    if (isHoldingScan && implantTier >= 2) {
                        if (scanHoldStartTime == 0) {
                            Kashub.LOGGER.info("KashubClient: Starting Peripheral Scan Hold... (Tier: {}, Pos: {})", implantTier, client.hitResult != null ? client.hitResult.getType() : "NONE");
                            scanHoldStartTime = currentTime;
                        }
                        
                        if (currentTime - scanHoldStartTime >= SCAN_HOLD_DURATION) {
                            // SCAN COMPLETE
                            scanHoldStartTime = 0;
                            lastScanTime = currentTime;
                            
                            net.minecraft.world.phys.HitResult hit = client.hitResult;
                            if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                net.minecraft.core.BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
                                // Visual feedback
                                String blockName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(pos).getBlock()).getPath();
                                client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bNeural Implant: §aAnalysis Complete: " + blockName), true);
                                client.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                                // Send packet to server
                                Kashub.LOGGER.info("KashubClient: Sending scan packet for block at {}", pos);
                                KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.PeripheralScanRequestPayload(pos));
                            } else {
                                Kashub.LOGGER.warn("KashubClient: Scan completed but hit result is null or not a block.");
                            }
                        }
                    } else {
                        // Only warn about missing tier if we're NOT in the cooldown window after a successful scan
                        if (isHoldingScan && implantTier < 2 && currentTime - lastScanTime > SCAN_HOLD_DURATION + 500) {
                             lastScanTime = currentTime;
                             Kashub.LOGGER.warn("KashubClient: Scan Hold failed - Tier {} < 2", implantTier);
                             client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cNeural Implant: §7Tier 2 required. (Detected: T" + implantTier + ")"), true);
                        }
                        scanHoldStartTime = 0;
                    }
                } else {
                    scanHoldStartTime = 0;
                }

                if (client.screen == null) {
                    checkScriptKeybinds();
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

    private static void checkScriptKeybinds() {
        KashubConfig config = KashubConfig.getInstance();

        for (java.util.Map.Entry<Integer, String> entry : config.scriptKeybinds.entrySet()) {
            int keyCode = entry.getKey();
            String scriptName = entry.getValue();

            boolean isPressed = KHWrapper.get().isKeyPressed(keyCode);

            if (isPressed && !pressedKeys.contains(keyCode)) {

                pressedKeys.add(keyCode);
                TaskManager.getInstance().startScriptFromFile(scriptName);
            } else if (!isPressed && pressedKeys.contains(keyCode)) {

                pressedKeys.remove(keyCode);
            }
        }
    }

}

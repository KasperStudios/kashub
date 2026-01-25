package kasperstudios.kashub.services.discord;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.api.server.KashubAPIServer;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.gui.editor.ModernEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * Discord Rich Presence integration for Kashub mod.
 * Shows current game state in Discord profile.
 * 
 * Available assets: end, mclogo, mclogonew, nether, overworld
 */
public class DiscordRichPresence {
    private static final long APPLICATION_ID = 1202154855427211274L;
    private static DiscordRichPresence instance;
    
    private IPCClient client;
    private boolean connected = false;
    private boolean initialized = false;
    private long startTimestamp;
    
    // Asset keys from Discord Developer Portal
    private static final String LARGE_IMAGE_KEY = "mclogonew";
    private static final String SMALL_IMAGE_OVERWORLD = "overworld";
    private static final String SMALL_IMAGE_NETHER = "nether";
    private static final String SMALL_IMAGE_END = "end";
    
    private DiscordRichPresence() {}
    
    public static DiscordRichPresence getInstance() {
        if (instance == null) {
            instance = new DiscordRichPresence();
        }
        return instance;
    }
    
    public void initialize() {
        if (initialized) return;
        
        KashubConfig config = KashubConfig.getInstance();
        if (!config.discordRpcEnabled) {
            Kashub.LOGGER.info("Discord RPC is disabled in config");
            return;
        }
        
        try {
            client = new IPCClient(APPLICATION_ID);
            client.setListener(new DiscordListener());
            client.connect();
            initialized = true;
            Kashub.LOGGER.info("Discord RPC initialized");
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to initialize Discord RPC: " + e.getMessage());
            connected = false;
            initialized = false;
        }
    }
    
    private class DiscordListener implements IPCListener {
        @Override
        public void onReady(IPCClient client) {
            Kashub.LOGGER.info("Discord RPC connected!");
            connected = true;
            startTimestamp = System.currentTimeMillis() / 1000;
            updatePresence();
        }
        
        @Override
        public void onClose(IPCClient client, JsonObject json) {
            Kashub.LOGGER.info("Discord RPC closed");
            connected = false;
        }
        
        @Override
        public void onDisconnect(IPCClient client, Throwable t) {
            Kashub.LOGGER.warn("Discord RPC disconnected: " + (t != null ? t.getMessage() : "unknown"));
            connected = false;
        }
        
        @Override
        public void onPacketSent(IPCClient client, Packet packet) {}
        
        @Override
        public void onPacketReceived(IPCClient client, Packet packet) {}
        
        @Override
        public void onActivityJoin(IPCClient client, String secret) {}
        
        @Override
        public void onActivitySpectate(IPCClient client, String secret) {}
        
        @Override
        public void onActivityJoinRequest(IPCClient client, String secret, User user) {}
    }

    public void updatePresence() {
        if (!connected || client == null) return;
        
        KashubConfig config = KashubConfig.getInstance();
        if (!config.discordRpcEnabled) {
            clearPresence();
            return;
        }
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        
        try {
            RichPresence.Builder builder = new RichPresence.Builder();
            
            // Set start timestamp for "elapsed" time
            builder.setStartTimestamp(startTimestamp);
            
            // Large image - Kashub logo
            builder.setLargeImage(LARGE_IMAGE_KEY, "Kashub Mod");
            
            if (mc.player != null && mc.world != null) {
                // Check if VSCode is connected
                if (isVSCodeConnected()) {
                    builder.setDetails("Coding in VSCode");
                    builder.setState("External editor connected");
                }
                // Check if in editor
                else if (ModernEditorScreen.isEditorOpen()) {
                    String file = ModernEditorScreen.getActiveFile();
                    builder.setDetails("Editing Script");
                    if (file != null) {
                        builder.setState(file);
                    } else {
                        builder.setState("New file");
                    }
                } else {
                    // In-game state
                    String state = buildStateString(mc, config);
                    String details = buildDetailsString(mc, config);
                    
                    builder.setDetails(details);
                    builder.setState(state);
                    
                    // Small image - dimension icon
                    if (config.discordShowDimension) {
                        String dimensionIcon = getDimensionIcon(mc.world.getRegistryKey());
                        String dimensionName = getDimensionName(mc.world.getRegistryKey());
                        builder.setSmallImage(dimensionIcon, dimensionName);
                    }
                }
            } else if (mc.currentScreen != null) {
                // In menu
                builder.setDetails("In Menu");
                builder.setState("Browsing menus");
            } else {
                // Main menu
                builder.setDetails("Main Menu");
                builder.setState("Idle");
            }
            
            client.sendRichPresence(builder.build());
        } catch (Exception e) {
            Kashub.LOGGER.debug("Failed to update Discord presence: " + e.getMessage());
        }
    }
    
    private String buildDetailsString(MinecraftClient mc, KashubConfig config) {
        if (mc.isInSingleplayer()) {
            return "Playing Singleplayer";
        } else {
            ServerInfo serverInfo = mc.getCurrentServerEntry();
            if (serverInfo != null && config.discordShowServerName) {
                String serverName = serverInfo.name;
                if (serverName.length() > 32) {
                    serverName = serverName.substring(0, 29) + "...";
                }
                return "Playing on " + serverName;
            }
            return "Playing Multiplayer";
        }
    }
    
    private String buildStateString(MinecraftClient mc, KashubConfig config) {
        StringBuilder state = new StringBuilder();
        
        if (config.discordShowCoords && mc.player != null) {
            int x = (int) mc.player.getX();
            int y = (int) mc.player.getY();
            int z = (int) mc.player.getZ();
            state.append(String.format("X: %d Y: %d Z: %d", x, y, z));
        } else if (config.discordShowDimension && mc.world != null) {
            state.append(getDimensionName(mc.world.getRegistryKey()));
        } else {
            state.append("Exploring");
        }
        
        return state.toString();
    }
    
    private String getDimensionIcon(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return SMALL_IMAGE_NETHER;
        } else if (dimension == World.END) {
            return SMALL_IMAGE_END;
        }
        return SMALL_IMAGE_OVERWORLD;
    }
    
    private String getDimensionName(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return "The Nether";
        } else if (dimension == World.END) {
            return "The End";
        }
        return "Overworld";
    }
    
    private boolean isVSCodeConnected() {
        try {
            KashubAPIServer server = KashubAPIServer.getInstance();
            if (server.isRunning() && server.getWebSocketServer() != null) {
                return server.getWebSocketServer().getClientCount() > 0;
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
    
    public void clearPresence() {
        if (client != null && connected) {
            try {
                client.sendRichPresence(null);
            } catch (Exception e) {
                Kashub.LOGGER.debug("Failed to clear Discord presence: " + e.getMessage());
            }
        }
    }
    
    public void shutdown() {
        if (client != null) {
            try {
                clearPresence();
                client.close();
            } catch (Exception e) {
                Kashub.LOGGER.debug("Error shutting down Discord RPC: " + e.getMessage());
            }
            connected = false;
            initialized = false;
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isEnabled() {
        return KashubConfig.getInstance().discordRpcEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        KashubConfig config = KashubConfig.getInstance();
        config.discordRpcEnabled = enabled;
        config.save();
        
        if (enabled && !initialized) {
            initialize();
        } else if (!enabled && connected) {
            clearPresence();
        }
    }
    
    public void reconnect() {
        shutdown();
        initialized = false;
        initialize();
    }
}

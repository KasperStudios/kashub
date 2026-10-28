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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Discord Rich Presence integration for Kashub mod.
 */
public class DiscordRichPresence {
    private static final long APPLICATION_ID = 1280831105146163210L;
    private static DiscordRichPresence instance;
    
    private IPCClient client;
    private boolean connected = false;
    private boolean initialized = false;
    private long startTimestamp;
    
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
        if (!config.discordRpcEnabled) return;
        
        try {
            client = new IPCClient(APPLICATION_ID);
            client.setListener(new DiscordListener());
            client.connect();
            initialized = true;
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to initialize Discord RPC: " + e.getMessage());
            connected = false;
            initialized = false;
        }
    }
    
    private class DiscordListener implements IPCListener {
        @Override
        public void onReady(IPCClient client) {
            connected = true;
            startTimestamp = System.currentTimeMillis() / 1000;
            updatePresence();
        }
        
        @Override public void onClose(IPCClient client, JsonObject json) { connected = false; }
        @Override public void onDisconnect(IPCClient client, Throwable t) { connected = false; }
        @Override public void onPacketSent(IPCClient client, Packet packet) {}
        @Override public void onPacketReceived(IPCClient client, Packet packet) {}
        @Override public void onActivityJoin(IPCClient client, String secret) {}
        @Override public void onActivitySpectate(IPCClient client, String secret) {}
        @Override public void onActivityJoinRequest(IPCClient client, String secret, User user) {}
    }

    public void updatePresence() {
        if (!connected || client == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        
        try {
            RichPresence.Builder builder = new RichPresence.Builder();
            builder.setStartTimestamp(startTimestamp);
            builder.setLargeImage(LARGE_IMAGE_KEY, "Kashub Mod");
            
            if (mc.player != null && mc.level != null) {
                if (isVSCodeConnected()) {
                    builder.setDetails("Coding in VSCode");
                    builder.setState("External editor connected");
                } else if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("CodeEditorScreen")) {
                    builder.setDetails("Editing Script");
                    builder.setState("IDE Active");
                } else {
                    String state = buildStateString(mc, KashubConfig.getInstance());
                    String details = buildDetailsString(mc, KashubConfig.getInstance());
                    builder.setDetails(details);
                    builder.setState(state);
                    
                    if (KashubConfig.getInstance().discordShowDimension) {
                        builder.setSmallImage(getDimensionIcon(mc.level.dimension()), getDimensionName(mc.level.dimension()));
                    }
                }
            } else {
                builder.setDetails("Main Menu");
                builder.setState("Idle");
            }
            
            client.sendRichPresence(builder.build());
        } catch (Exception e) {
            Kashub.LOGGER.debug("Failed to update Discord presence: " + e.getMessage());
        }
    }
    
    public void clearPresence() {
        if (client != null && connected) {
            try { client.sendRichPresence(null); } catch (Exception ignored) {
                Kashub.LOGGER.debug("Failed to clear Discord presence: {}", ignored.getMessage());
            }
        }
    }
    
    public void reconnect() {
        shutdown();
        initialize();
    }
    
    private String buildDetailsString(Minecraft mc, KashubConfig config) {
        if (mc.isLocalServer()) return "Playing Singleplayer";
        ServerData serverInfo = mc.getCurrentServer();
        if (serverInfo != null && config.discordShowServerName) return "Playing on " + serverInfo.name;
        return "Playing Multiplayer";
    }
    
    private String buildStateString(Minecraft mc, KashubConfig config) {
        if (config.discordShowCoords && mc.player != null) {
            return String.format("X: %d Y: %d Z: %d", (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
        }
        return mc.level != null ? getDimensionName(mc.level.dimension()) : "Exploring";
    }
    
    private String getDimensionIcon(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) return SMALL_IMAGE_NETHER;
        if (dimension == Level.END) return SMALL_IMAGE_END;
        return SMALL_IMAGE_OVERWORLD;
    }
    
    private String getDimensionName(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) return "The Nether";
        if (dimension == Level.END) return "The End";
        return "Overworld";
    }
    
    private boolean isVSCodeConnected() {
        try {
            KashubAPIServer server = KashubAPIServer.getInstance();
            return server.isRunning() && server.getWebSocketServer() != null && server.getWebSocketServer().getClientCount() > 0;
        } catch (Exception e) { return false; }
    }
    
    public void shutdown() {
        if (client != null) {
            try { client.sendRichPresence(null); client.close(); } catch (Exception ignored) {
                Kashub.LOGGER.debug("Error during Discord RPC shutdown: {}", ignored.getMessage());
            }
            connected = false;
            initialized = false;
        }
    }
}

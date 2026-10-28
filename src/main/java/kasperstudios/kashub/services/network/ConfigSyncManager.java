package kasperstudios.kashub.services.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kasperstudios.khwrapper.IPacketBuffer;
import com.kasperstudios.khwrapper.IPacketPayload;
import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;

import java.util.List;

/**
 * ConfigSyncManager - Synchronize server config with clients.
 * 
 * When player joins server with Kashub, server sends config enforcement.
 * Client applies server settings (restricted mode, disabled commands, etc).
 * 
 * @since 0.9.0
 */
public class ConfigSyncManager {
    
    private static volatile ConfigSyncManager instance;
    private static final Object LOCK = new Object();
    private static final Gson GSON = new GsonBuilder().create();
    
    private boolean serverConfigActive = false;
    private ServerConfig serverConfig;
    
    private ConfigSyncManager() {}
    
    public static ConfigSyncManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ConfigSyncManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize config sync.
     */
    public void initialize() {
        // Unified registration
        KHWrapper.register(com.kasperstudios.khwrapper.NetworkRegistration.s2c(
            ConfigSyncPacket.ID,
            ConfigSyncPacket::read,
            (packet, context) -> {
                ConfigSyncPacket payload = (ConfigSyncPacket) packet;
                context.execute(() -> {
                    try {
                        applyServerConfig(payload.config());
                    } catch (Exception e) {
                        Kashub.LOGGER.error("Failed to apply server config", e);
                    }
                });
            }
        ));
        
        Kashub.LOGGER.info("ConfigSyncManager initialized");
    }
    
    /**
     * Apply server config to client.
     */
    private void applyServerConfig(ServerConfig config) {
        if (!config.clientEnforcement) {
            serverConfigActive = false;
            Kashub.LOGGER.info("Server config enforcement disabled");
            return;
        }
        
        serverConfigActive = true;
        serverConfig = config;
        
        Kashub.LOGGER.info("Server config applied:");
        Kashub.LOGGER.info("  Restricted Mode: " + config.restrictedMode);
        Kashub.LOGGER.info("  Editor Access: " + config.allowEditorAccess);
        Kashub.LOGGER.info("  Debugger Access: " + config.allowDebuggerAccess);
        Kashub.LOGGER.info("  Disabled Commands: " + config.disabledCommands.size());
    }
    
    /**
     * Reset to local config (when disconnecting from server).
     */
    public void resetToLocal() {
        serverConfigActive = false;
        serverConfig = null;
        Kashub.LOGGER.info("Reset to local config");
    }
    
    /**
     * Check if server config is active.
     */
    public boolean isServerConfigActive() {
        return serverConfigActive;
    }
    
    /**
     * Get effective config (server if active, otherwise local).
     */
    public KashubConfig getEffectiveConfig() {
        if (!serverConfigActive || serverConfig == null) {
            return KashubConfig.getInstance();
        }
        
        // Return local config with server overrides applied
        return KashubConfig.getInstance();
    }
    
    /**
     * Check if command is allowed (considering server config).
     */
    public boolean isCommandAllowed(String command) {
        if (!serverConfigActive || serverConfig == null) {
            return KashubConfig.getInstance().isCommandAllowed(command);
        }
        
        return !serverConfig.disabledCommands.contains(command.toLowerCase());
    }
    
    /**
     * Check if editor access is allowed.
     */
    public boolean isEditorAllowed() {
        if (!serverConfigActive || serverConfig == null) {
            return KashubConfig.getInstance().allowEditorAccess;
        }
        
        return serverConfig.allowEditorAccess;
    }
    
    /**
     * Check if debugger access is allowed.
     */
    public boolean isDebuggerAllowed() {
        if (!serverConfigActive || serverConfig == null) {
            return KashubConfig.getInstance().allowDebuggerAccess;
        }
        
        return serverConfig.allowDebuggerAccess;
    }
    
    /**
     * Server config data.
     */
    public static class ServerConfig {
        public boolean restrictedMode;
        public boolean allowEditorAccess;
        public boolean allowDebuggerAccess;
        public List<String> disabledCommands;
        public boolean clientEnforcement;
        
        public ServerConfig() {}
        
        public ServerConfig(KashubConfig config) {
            this.restrictedMode = config.restrictedMode;
            this.allowEditorAccess = config.allowEditorAccess;
            this.allowDebuggerAccess = config.allowDebuggerAccess;
            this.disabledCommands = config.disabledCommands;
            this.clientEnforcement = config.clientEnforcement;
        }
    }
    
    /**
     * Config sync packet.
     */
    public record ConfigSyncPacket(ServerConfig config) implements IPacketPayload {
        public static final String ID = "kashub:config_sync";
        
        @Override
        public String getPacketId() {
            return ID;
        }
        
        @Override
        public void write(IPacketBuffer buffer) {
            String json = GSON.toJson(config);
            buffer.writeString(json);
        }
        
        public static ConfigSyncPacket read(IPacketBuffer buffer) {
            String json = buffer.readString();
            ServerConfig config = GSON.fromJson(json, ServerConfig.class);
            return new ConfigSyncPacket(config);
        }
    }
}

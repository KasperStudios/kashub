package kasperstudios.kashub.services.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.config.KashubConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
        // Register payload type
        PayloadTypeRegistry.playS2C().register(ConfigSyncPacket.ID, ConfigSyncPacket.CODEC);
        
        // Register receiver
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                try {
                    applyServerConfig(payload.config());
                } catch (Exception e) {
                    Kashub.LOGGER.error("Failed to apply server config", e);
                }
            });
        });
        
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
        // (Server settings are checked via isCommandAllowed, isEditorAllowed, etc.)
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
    public record ConfigSyncPacket(ServerConfig config) implements CustomPayload {
        
        public static final CustomPayload.Id<ConfigSyncPacket> ID = 
            new CustomPayload.Id<>(Identifier.of("kashub", "config_sync"));
        
        public static final PacketCodec<RegistryByteBuf, ConfigSyncPacket> CODEC = 
            new PacketCodec<RegistryByteBuf, ConfigSyncPacket>() {
                @Override
                public ConfigSyncPacket decode(RegistryByteBuf buf) {
                    return read(buf);
                }
                
                @Override
                public void encode(RegistryByteBuf buf, ConfigSyncPacket packet) {
                    write(buf, packet);
                }
            };
        
        private static void write(RegistryByteBuf buf, ConfigSyncPacket packet) {
            String json = GSON.toJson(packet.config);
            buf.writeString(json);
        }
        
        private static ConfigSyncPacket read(RegistryByteBuf buf) {
            String json = buf.readString();
            ServerConfig config = GSON.fromJson(json, ServerConfig.class);
            return new ConfigSyncPacket(config);
        }
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

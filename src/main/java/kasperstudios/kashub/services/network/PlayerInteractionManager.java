package kasperstudios.kashub.services.network;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerInteractionManager - Manage player interactions and permissions.
 * 
 * Handles player permissions, messaging, and network interactions.
 * 
 * Part of v0.9.0 Player & Network Interaction feature.
 * 
 * @since 0.9.0
 */
public class PlayerInteractionManager {
    
    private static volatile PlayerInteractionManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, Set<String>> playerPermissions;
    private final Map<String, PlayerData> playerData;
    
    private PlayerInteractionManager() {
        this.playerPermissions = new ConcurrentHashMap<>();
        this.playerData = new ConcurrentHashMap<>();
    }
    
    public static PlayerInteractionManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PlayerInteractionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Check if player has permission.
     */
    public boolean hasPermission(String playerName, String permission) {
        Set<String> perms = playerPermissions.get(playerName);
        if (perms == null) {
            return false;
        }
        
        // Check exact permission or wildcard
        return perms.contains(permission) || perms.contains("*") || 
               perms.contains(permission.split("\\.")[0] + ".*");
    }
    
    /**
     * Grant permission to player.
     */
    public void grantPermission(String playerName, String permission) {
        playerPermissions.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet())
                        .add(permission);
        ScriptLogger.getInstance().info("Granted permission '" + permission + "' to " + playerName);
    }
    
    /**
     * Revoke permission from player.
     */
    public void revokePermission(String playerName, String permission) {
        Set<String> perms = playerPermissions.get(playerName);
        if (perms != null) {
            perms.remove(permission);
            ScriptLogger.getInstance().info("Revoked permission '" + permission + "' from " + playerName);
        }
    }
    
    /**
     * Get all permissions for player.
     */
    public Set<String> getPermissions(String playerName) {
        return playerPermissions.getOrDefault(playerName, Collections.emptySet());
    }
    
    /**
     * Clear all permissions for player.
     */
    public void clearPermissions(String playerName) {
        playerPermissions.remove(playerName);
        ScriptLogger.getInstance().info("Cleared all permissions for " + playerName);
    }
    
    /**
     * Send message to player (client-side only for now).
     */
    public void sendMessage(String playerName, String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getName().getString().equals(playerName)) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
    
    /**
     * Store player data.
     */
    public void setPlayerData(String playerName, String key, String value) {
        PlayerData data = playerData.computeIfAbsent(playerName, k -> new PlayerData(playerName));
        data.set(key, value);
    }
    
    /**
     * Get player data.
     */
    public String getPlayerData(String playerName, String key) {
        PlayerData data = playerData.get(playerName);
        return data != null ? data.get(key) : null;
    }
    
    /**
     * Check if player data exists.
     */
    public boolean hasPlayerData(String playerName, String key) {
        PlayerData data = playerData.get(playerName);
        return data != null && data.has(key);
    }
    
    /**
     * Remove player data.
     */
    public void removePlayerData(String playerName, String key) {
        PlayerData data = playerData.get(playerName);
        if (data != null) {
            data.remove(key);
        }
    }
    
    /**
     * Get current player name.
     */
    public String getCurrentPlayerName() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        return player != null ? player.getName().getString() : null;
    }
    
    /**
     * PlayerData class for storing per-player data.
     */
    public static class PlayerData {
        private final String playerName;
        private final Map<String, String> data;
        
        public PlayerData(String playerName) {
            this.playerName = playerName;
            this.data = new ConcurrentHashMap<>();
        }
        
        public void set(String key, String value) {
            data.put(key, value);
        }
        
        public String get(String key) {
            return data.get(key);
        }
        
        public boolean has(String key) {
            return data.containsKey(key);
        }
        
        public void remove(String key) {
            data.remove(key);
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
}

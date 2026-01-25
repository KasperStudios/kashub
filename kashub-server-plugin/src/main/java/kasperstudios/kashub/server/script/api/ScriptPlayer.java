package kasperstudios.kashub.server.script.api;

import kasperstudios.kashub.server.KasHubServerPlugin;
import kasperstudios.kashub.server.dto.KasHubClientInfo;
import org.bukkit.entity.Player;

public class ScriptPlayer {
    private final Player player;
    private final KasHubServerPlugin plugin;

    public ScriptPlayer(Player player) {
        this.player = player;
        this.plugin = KasHubServerPlugin.getInstance();
    }

    public String getName() {
        return player.getName();
    }

    public String getUUID() {
        return player.getUniqueId().toString();
    }

    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    public void kick(String reason) {
        player.kickPlayer(reason);
    }

    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    public boolean isOp() {
        return player.isOp();
    }

    public String getPlatform() {
        // Platform isn't currently sent in handshake payload explicitly in mod v1.
        // But we can check capabilities or add it to handshake later.
        // For now, return "Unknown" or infer from client info if added.
        return "Unknown";
    }

    public String getVersion() {
        KasHubClientInfo info = plugin.getHandshakeListener().getClientInfo(player.getUniqueId());
        return info != null ? info.getVersion() : "Vanilla";
    }

    public boolean hasMod() {
        return plugin.getHandshakeListener().getClientInfo(player.getUniqueId()) != null;
    }

    public Player getBukkitPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return getName(); // Return name for string representation in script
    }
}

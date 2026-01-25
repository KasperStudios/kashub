package kasperstudios.kashub.server;

import com.google.gson.Gson;
import kasperstudios.kashub.server.dto.LogPayload;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

public class LogListener implements PluginMessageListener {
    private final KasHubServerPlugin plugin;
    private final Gson gson = new Gson();

    public LogListener(KasHubServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("kashub:log"))
            return;

        try {
            String json = readStringFromBytes(message);
            LogPayload payload = gson.fromJson(json, LogPayload.class);

            if ("blocked_command".equals(payload.getType())) {
                if (plugin.getConfigManager().getServerConfig().isLogBlockedCommands()) {
                    plugin.getLogger().info("[KasHub] BLOCKED COMMAND - Player: " + player.getName() + ", Command: "
                            + payload.getData());
                }
            } else {
                plugin.getLogger().info("[KasHub] LOG (" + payload.getType() + ") - Player: " + player.getName() + ": "
                        + payload.getData());
            }

        } catch (Exception e) {
            // plugin.getLogger().warning("Failed to process log packet: " +
            // e.getMessage());
        }
    }

    // Duplicate helper to avoid common util class dependency for now (Keep it
    // simple)
    private String readStringFromBytes(byte[] data) {
        int offset = 0;
        int length = 0;
        int shift = 0;
        byte b;
        do {
            if (offset >= data.length)
                throw new RuntimeException("VarInt parsing error");
            b = data[offset++];
            length |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);

        if (offset + length > data.length) {
            throw new RuntimeException("String length mismatch");
        }

        return new String(data, offset, length, StandardCharsets.UTF_8);
    }
}

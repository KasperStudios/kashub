package kasperstudios.kashub.server;

import com.google.gson.Gson;
import kasperstudios.kashub.server.dto.KasHubClientInfo;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HandshakeListener implements PluginMessageListener {
    private final KasHubServerPlugin plugin;
    private final Gson gson = new Gson();
    private final Map<UUID, KasHubClientInfo> connectedClients = new HashMap<>();

    public HandshakeListener(KasHubServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("kashub:handshake"))
            return;

        try {
            // Read CustomPayload format (String length + String bytes)
            // But depending on how Fabric sends it, it might just be the raw bytes of the
            // string or prefixed.
            // Client NetworkPayload uses: buf.writeString(json). writeString usually writes
            // VarInt length + bytes.
            // Bukkit's onPluginMessageReceived receives the raw payload bytes.
            // We need to read the string.
            // Simple approach: Assume text if it's simple or try to parse.
            // Wait, CustomPayload writes to FriendlyByteBuf. writeString uses VarInt length
            // prefix.
            // We need a helper to read that if not standard.
            // Let's look at how to read string from byte[] in Bukkit corresponding to MC
            // protocol.
            // Actually, for simplicity on client side I used buf.writeString(json).
            // On server side, I'm receiving bytes. I can wrap in DataInputStream or just
            // handle it.
            // Using a simple helper to read the string.

            String json = readStringFromBytes(message);

            KasHubClientInfo info = gson.fromJson(json, KasHubClientInfo.class);
            info.updateLastSeen();

            connectedClients.put(player.getUniqueId(), info);

            plugin.getLogger().info("KasHub Client connected: " + player.getName() + " (v" + info.getVersion() + ")");

            sendConfig(player);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process handshake from " + player.getName() + ": " + e.getMessage());
        }
    }

    public void sendConfig(Player player) {
        String configJson = plugin.getConfigManager().getServerConfigJson();
        sendStringPayload(player, "kashub:config", configJson);
    }

    public KasHubClientInfo getClientInfo(UUID uuid) {
        return connectedClients.get(uuid);
    }

    // Helpers for Packet Buffer (VarInt Length + UTF-8 String)
    // Client sends: buf.writeString(json)

    private String readStringFromBytes(byte[] data) {
        // Simple implementation assuming standard MC string format
        // First part is VarInt length.

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
            // Fallback: maybe it's just raw bytes? (Some mods do that)
            // If length is huge or invalid, try safe decoding?
            // Standard PacketCodec uses writeString.
            throw new RuntimeException("String length mismatch");
        }

        return new String(data, offset, length, StandardCharsets.UTF_8);
    }

    private void sendStringPayload(Player player, String channel, String content) {
        byte[] stringBytes = content.getBytes(StandardCharsets.UTF_8);
        int length = stringBytes.length;

        // Create buffer with VarInt length + bytes
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        // Write VarInt length
        while ((length & -128) != 0) {
            baos.write(length & 127 | 128);
            length >>>= 7;
        }
        baos.write(length);

        try {
            baos.write(stringBytes);
        } catch (java.io.IOException e) {
        } // Should not happen

        player.sendPluginMessage(plugin, channel, baos.toByteArray());
    }
}

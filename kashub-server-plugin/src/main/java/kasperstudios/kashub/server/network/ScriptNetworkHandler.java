package kasperstudios.kashub.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import kasperstudios.kashub.server.KasHubServerPlugin;
import kasperstudios.kashub.server.script.ServerScriptInterpreter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

public class ScriptNetworkHandler implements PluginMessageListener {
    private final KasHubServerPlugin plugin;
    private final Gson gson = new Gson();

    public ScriptNetworkHandler(KasHubServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("kashub:script_mgmt"))
            return;

        if (!player.isOp()) {
            plugin.getLogger().warning("Unauthorized script access attempt by " + player.getName());
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            // Protocol: [Action: Byte] [Payload: JSON String]
            // Actions: 0=List, 1=Save, 2=Delete, 3=Run
            byte action = in.readByte();
            String payloadJson = readString(in);

            handleAction(player, action, payloadJson);

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to parse script mgmt packet from " + player.getName());
        }
    }

    private void handleAction(Player player, byte action, String json) {
        JsonObject payload = gson.fromJson(json, JsonObject.class);

        switch (action) {
            case 0: // LIST
                List<String> scripts = plugin.getScriptManager().listScripts();
                sendResponse(player, (byte) 0, gson.toJson(scripts));
                break;

            case 1: // SAVE
                if (payload.has("name") && payload.has("content")) {
                    String name = payload.get("name").getAsString();
                    String content = payload.get("content").getAsString();
                    boolean success = plugin.getScriptManager().saveScript(name, content);
                    sendResponse(player, (byte) 1, success ? "OK" : "Error");
                }
                break;

            case 2: // DELETE
                if (payload.has("name")) {
                    String name = payload.get("name").getAsString();
                    boolean success = plugin.getScriptManager().deleteScript(name);
                    sendResponse(player, (byte) 2, success ? "OK" : "Error");
                }
                break;

            case 3: // RUN
                if (payload.has("name")) {
                    String name = payload.get("name").getAsString();
                    String content = plugin.getScriptManager().getScriptContent(name);
                    if (content != null) {
                        try {
                            new ServerScriptInterpreter(plugin).execute(content);
                            sendResponse(player, (byte) 3, "Executed");
                        } catch (Exception e) {
                            sendResponse(player, (byte) 3, "Error: " + e.getMessage());
                        }
                    } else {
                        sendResponse(player, (byte) 3, "Not Found");
                    }
                }
                break;

            case 4: // READ
                if (payload.has("name")) {
                    String name = payload.get("name").getAsString();
                    String content = plugin.getScriptManager().getScriptContent(name);
                    if (content != null) {
                        sendResponse(player, (byte) 4, content);
                    } else {
                        sendResponse(player, (byte) 4, ""); // Empty if not found? Or error?
                    }
                }
                break;

            case 5: // METADATA
                java.util.List<kasperstudios.kashub.server.script.v2.CommandMetadata> meta = kasperstudios.kashub.server.script.v2.CommandRegistry
                        .getAllMetadata();
                sendResponse(player, (byte) 5, gson.toJson(meta));
                break;
        }
    }

    private void sendResponse(Player player, byte action, String content) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(baos);

            out.writeByte(action);
            writeString(out, content);

            player.sendPluginMessage(plugin, "kashub:script_mgmt", baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper to read string (matches HandshakeListener/Mod networking)
    private String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeString(java.io.DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    private void writeVarInt(java.io.DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }
}

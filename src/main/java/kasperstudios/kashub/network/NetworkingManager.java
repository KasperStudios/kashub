package kasperstudios.kashub.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.network.dto.ServerConfig;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import kasperstudios.kashub.util.ScriptLogger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;

public class NetworkingManager {
    private static final NetworkingManager INSTANCE = new NetworkingManager();
    private final Gson gson = new Gson();

    private NetworkingManager() {
    }

    public static NetworkingManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        // Register Payload Types
        PayloadTypeRegistry.playC2S().register(NetworkingPayloads.HandshakePayload.ID,
                NetworkingPayloads.HandshakePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkingPayloads.LogPayload.ID, NetworkingPayloads.LogPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkingPayloads.ConfigPayload.ID,
                NetworkingPayloads.ConfigPayload.CODEC);

        // Script Mgmt (Bidirectional)
        PayloadTypeRegistry.playC2S().register(NetworkingPayloads.ScriptMgmtPayload.ID,
                NetworkingPayloads.ScriptMgmtPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkingPayloads.ScriptMgmtPayload.ID,
                NetworkingPayloads.ScriptMgmtPayload.CODEC);

        // Script Execution (S2C)
        PayloadTypeRegistry.playS2C().register(NetworkingPayloads.ScriptExecutionPayload.ID,
                NetworkingPayloads.ScriptExecutionPayload.CODEC);

        // Send Handshake on Join
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            sendHandshake(client);
        });

        // Reset logic on Disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerModeManager.getInstance().switchToLocal();
        });

        // Register Config Receiver
        ClientPlayNetworking.registerGlobalReceiver(NetworkingPayloads.ConfigPayload.ID, (payload, context) -> {
            String json = payload.json();

            // Handle on client thread
            context.client().execute(() -> {
                try {
                    JsonObject root = gson.fromJson(json, JsonObject.class);

                    if (root.has("config")) {
                        ServerConfig config = gson.fromJson(root.get("config"), ServerConfig.class);
                        ServerModeManager.getInstance().switchToServerControlled(config);

                        String serverName = root.has("serverName")
                                ? root.get("serverName").getAsString()
                                : "Server";

                        ScriptLogger.getInstance().info(
                                "KasHub: Config received from " + serverName + ". Switched to SERVER_CONTROLLED mode.");
                    } else {
                        Kashub.LOGGER.warn("KasHub: Received config packet without 'config' object");
                    }
                } catch (JsonSyntaxException e) {
                    Kashub.LOGGER.error("KasHub: Failed to parse server config JSON", e);
                    ScriptLogger.getInstance()
                            .error("KasHub: Received invalid config from server. Remaining in LOCAL mode.");
                } catch (Exception e) {
                    Kashub.LOGGER.error("KasHub: Error processing server config", e);
                }
            });
        });
        // Register Script Execution Receiver
        ClientPlayNetworking.registerGlobalReceiver(NetworkingPayloads.ScriptExecutionPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        // Execute script
                        // Check security? Server forced it.
                        // TODO: check allowCheatMode if script uses restricted features?
                        // For now, trust the server.
                        try {
                            // v0.9.0: Use ScriptTasks for server scripts
                            TaskManager.getInstance()
                                    .startScript(payload.scriptName(), payload.content(),
                                            java.util.Collections.singleton("server_pushed"),
                                            kasperstudios.kashub.core.Type.REMOTE);

                            ScriptLogger.getInstance().info("Started server script: " + payload.scriptName());
                        } catch (Exception e) {
                            ScriptLogger.getInstance().error(
                                    "Failed to execute server script " + payload.scriptName() + ": " + e.getMessage());
                        }
                    });
                });

        // Register Script Management Response Receiver (from plugin)
        // Register Script Management Response Receiver (from plugin)
        ClientPlayNetworking.registerGlobalReceiver(NetworkingPayloads.ScriptMgmtPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        kasperstudios.kashub.util.RemoteScriptManager.getInstance().handleMessage(payload.action(),
                                payload.data());
                    });
                });
    }

    private void sendHandshake(MinecraftClient client) {
        if (client.player == null)
            return;

        try {
            JsonObject json = new JsonObject();
            json.addProperty("modId", NetworkingConstants.MOD_ID);
            json.addProperty("version", Kashub.VERSION);
            json.addProperty("clientId", client.player.getUuid().toString());

            // Telemetry
            json.addProperty("platform", System.getProperty("os.name")); // Simple OS name
            json.addProperty("modLoader", "Fabric"); // Hardcoded for Fabric mod
            json.addProperty("deviceSpec", Runtime.getRuntime().availableProcessors() + " cores, "
                    + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB RAM");

            JsonObject caps = new JsonObject();
            caps.addProperty("supportsServerConfig", true);
            caps.addProperty("supportsCommandBlocking", true);
            caps.addProperty("supportsEditorLock", true);
            caps.addProperty("supportsClientScripts", true);

            json.add("capabilities", caps);

            ClientPlayNetworking.send(new NetworkingPayloads.HandshakePayload(gson.toJson(json)));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send handshake", e);
        }
    }

    public void sendLog(String type, String data) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null)
            return;

        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("data", data);
            json.addProperty("time", System.currentTimeMillis());

            ClientPlayNetworking.send(new NetworkingPayloads.LogPayload(gson.toJson(json)));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send log packet", e);
        }
    }
}

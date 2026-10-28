package kasperstudios.kashub.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kasperstudios.khwrapper.IPacketContext;
import com.kasperstudios.khwrapper.IPacketPayload;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.network.ServerModeManager;
import kasperstudios.kashub.network.dto.ServerConfig;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import kasperstudios.kashub.util.ScriptLogger;
import java.util.Collections;

/**
 * Client-only network packet handlers.
 * This class is intended to be loaded ONLY on the physical client.
 */
public class ClientNetworkingHandlers {
    private static final Gson gson = new Gson();

    public static void handleConfig(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.ConfigPayload payload = (NetworkingPayloads.ConfigPayload) packet;
        String json = payload.json();

        context.execute(() -> {
            try {
                JsonObject root = gson.fromJson(json, JsonObject.class);
                if (root.has("config")) {
                    ServerConfig config = gson.fromJson(root.get("config"), ServerConfig.class);
                    ServerModeManager.getInstance().switchToServerControlled(config);

                    String serverName = root.has("serverName") ? root.get("serverName").getAsString() : "Server";
                    ScriptLogger.getInstance().info("KasHub: Config received from " + serverName + ". Switched to SERVER_CONTROLLED mode.");
                } else {
                    Kashub.LOGGER.warn("KasHub: Received config packet without 'config' object");
                }
            } catch (JsonSyntaxException e) {
                Kashub.LOGGER.error("KasHub: Failed to parse server config JSON", e);
                ScriptLogger.getInstance().error("KasHub: Received invalid config from server. Remaining in LOCAL mode.");
            } catch (Exception e) {
                Kashub.LOGGER.error("KasHub: Error processing server config", e);
            }
        });
    }

    public static void handleScriptExecution(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.ScriptExecutionPayload payload = (NetworkingPayloads.ScriptExecutionPayload) packet;
        context.execute(() -> {
            try {
                TaskManager.getInstance().startScript(payload.scriptName(), payload.content(),
                        Collections.singleton("server_pushed"),
                        kasperstudios.kashub.core.Type.REMOTE);
                ScriptLogger.getInstance().info("Started server script: " + payload.scriptName());
            } catch (Exception e) {
                ScriptLogger.getInstance().error("Failed to execute server script " + payload.scriptName() + ": " + e.getMessage());
            }
        });
    }

    public static void handleBlackout(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.BlackoutPayload payload = (NetworkingPayloads.BlackoutPayload) packet;
        context.execute(() -> {
            kasperstudios.kashub.client.BlackoutRenderer.getInstance().triggerBlackout(payload.durationSeconds(), payload.message());
        });
    }

    public static void handleInjectionStatus(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.InjectionStatusPayload payload = (NetworkingPayloads.InjectionStatusPayload) packet;
        context.execute(() -> {
            String msg = payload.message();
            if (msg != null && msg.startsWith("CONNECTED:")) {
                try {
                    int id = Integer.parseInt(msg.substring("CONNECTED:".length()));
                    kasperstudios.kashub.gui.KashubEditorContext.onCCConnected(id);
                } catch (NumberFormatException ignored) {
                    Kashub.LOGGER.debug("Failed to parse CC computer ID from injection status message: {}", msg);
                }
            } else if (msg != null && msg.equals("DISCONNECTED")) {
                kasperstudios.kashub.gui.KashubEditorContext.onCCDisconnected();
            } else {
                ScriptLogger.getInstance().info("Injection Status: " + msg);
            }
        });
    }

    public static void handleImplantSync(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.ImplantSyncPayload payload = (NetworkingPayloads.ImplantSyncPayload) packet;
        context.execute(() -> {
            kasperstudios.kashub.core.Environment.getInstance().setImplantTier(payload.tier());
        });
    }

    public static void handleVisualEffect(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.VisualEffectPayload payload = (NetworkingPayloads.VisualEffectPayload) packet;
        context.execute(() -> {
            if (payload.type() == NetworkingPayloads.VisualEffectPayload.TYPE_FLASH) {
                kasperstudios.kashub.client.VisualEffectRenderer.getInstance().flash(payload.intensity(), payload.durationTicks());
            } else if (payload.type() == NetworkingPayloads.VisualEffectPayload.TYPE_SHAKE) {
                kasperstudios.kashub.client.VisualEffectRenderer.getInstance().shake(payload.intensity(), payload.durationTicks());
            }
        });
    }

    public static void handleCCFileList(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCFileListPayload payload = (NetworkingPayloads.CCFileListPayload) packet;
        context.execute(() -> {
            kasperstudios.kashub.client.CCTClientBridge.getInstance().onFileListReceived(payload.computerId(), payload.filenames());
            kasperstudios.kashub.gui.KashubEditorContext.onCCFileListReceived(payload.computerId());
        });
    }

    public static void handleCCTerminalOutput(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCTerminalOutputPayload payload = (NetworkingPayloads.CCTerminalOutputPayload) packet;
        context.execute(() -> ScriptLogger.getInstance().scriptLogAppend("CC#" + payload.computerId(), ScriptLogger.LogLevel.INFO, payload.text()));
    }

    public static void handleCCFileContent(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCFileContentPayload payload = (NetworkingPayloads.CCFileContentPayload) packet;
        context.execute(() -> kasperstudios.kashub.client.CCTClientBridge
                .getInstance().onFileContentReceived(payload.computerId(), payload.filename(), payload.content(), payload.success()));
    }

    public static void handlePeripheralScanResult(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.PeripheralScanResultPayload payload = (NetworkingPayloads.PeripheralScanResultPayload) packet;
        context.execute(() -> kasperstudios.kashub.client.CCTClientBridge
                .getInstance().onPeripheralScanResultReceived(payload.type(), payload.methods()));
    }
}

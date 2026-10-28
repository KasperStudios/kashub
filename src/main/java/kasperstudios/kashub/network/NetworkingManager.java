package kasperstudios.kashub.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.IPacketPayload;
import com.kasperstudios.khwrapper.IPacketContext;
import com.kasperstudios.khwrapper.NetworkRegistration;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class NetworkingManager {
    private static final NetworkingManager INSTANCE = new NetworkingManager();
    private final Gson gson = new Gson();
    private boolean initialized = false;

    private static final Map<String, BiConsumer<IPacketPayload, IPacketContext>> CLIENT_HANDLERS = new HashMap<>();

    private NetworkingManager() {
    }

    public static NetworkingManager getInstance() {
        return INSTANCE;
    }

    public static void setClientHandler(String id, BiConsumer<IPacketPayload, IPacketContext> handler) {
        CLIENT_HANDLERS.put(id, handler);
    }

    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        // --- C2S PACKETS (Server-side handlers) ---

        // Register Handshake (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.HandshakePayload.ID,
            NetworkingPayloads.HandshakePayload::read,
            null // Sent by client, server handler added if needed elsewhere
        ));

        // Register Log (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.LogPayload.ID,
            NetworkingPayloads.LogPayload::read,
            null
        ));

        // Computer Link (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.ComputerLinkPayload.ID,
            NetworkingPayloads.ComputerLinkPayload::read,
            NetworkingHandlers::handleComputerLink
        ));

        // CC Script Push (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.CCScriptPushPayload.ID,
            NetworkingPayloads.CCScriptPushPayload::read,
            NetworkingHandlers::handleCCScriptPush
        ));

        // CC Terminal Input (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.CCTerminalInputPayload.ID,
            NetworkingPayloads.CCTerminalInputPayload::read,
            NetworkingHandlers::handleCCTerminalInput
        ));

        // CC File Read Request (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.CCFileReadRequestPayload.ID,
            NetworkingPayloads.CCFileReadRequestPayload::read,
            NetworkingHandlers::handleCCFileReadRequest
        ));

        // Peripheral Scan Request (C2S)
        KHWrapper.register(NetworkRegistration.c2s(
            NetworkingPayloads.PeripheralScanRequestPayload.ID,
            NetworkingPayloads.PeripheralScanRequestPayload::read,
            NetworkingHandlers::handlePeripheralScanRequest
        ));


        // --- S2C PACKETS (Client-side handlers via proxy) ---

        registerS2C(NetworkingPayloads.ConfigPayload.ID, NetworkingPayloads.ConfigPayload::read);
        registerS2C(NetworkingPayloads.ScriptExecutionPayload.ID, NetworkingPayloads.ScriptExecutionPayload::read);
        registerS2C(NetworkingPayloads.BlackoutPayload.ID, NetworkingPayloads.BlackoutPayload::read);
        registerS2C(NetworkingPayloads.InjectionStatusPayload.ID, NetworkingPayloads.InjectionStatusPayload::read);
        registerS2C(NetworkingPayloads.ImplantSyncPayload.ID, NetworkingPayloads.ImplantSyncPayload::read);
        registerS2C(NetworkingPayloads.VisualEffectPayload.ID, NetworkingPayloads.VisualEffectPayload::read);
        registerS2C(NetworkingPayloads.CCFileListPayload.ID, NetworkingPayloads.CCFileListPayload::read);
        registerS2C(NetworkingPayloads.CCTerminalOutputPayload.ID, NetworkingPayloads.CCTerminalOutputPayload::read);
        registerS2C(NetworkingPayloads.CCFileContentPayload.ID, NetworkingPayloads.CCFileContentPayload::read);
        registerS2C(NetworkingPayloads.PeripheralScanResultPayload.ID, NetworkingPayloads.PeripheralScanResultPayload::read);

        // --- BIDIRECTIONAL PACKETS ---

        KHWrapper.register(NetworkRegistration.bidirectional(
            NetworkingPayloads.ScriptMgmtPayload.ID,
            NetworkingPayloads.ScriptMgmtPayload::read,
            (packet, context) -> {
                // Server-side
                NetworkingHandlers.handleScriptMgmt(packet, context);
                // Client-side proxy
                BiConsumer<IPacketPayload, IPacketContext> clientHandler = CLIENT_HANDLERS.get(NetworkingPayloads.ScriptMgmtPayload.ID);
                if (clientHandler != null) clientHandler.accept(packet, context);
            }
        ));



        // --- Connection Events ---

        KHWrapper.onPlayerJoin(this::onPlayerJoin);
        KHWrapper.onPlayerLeave(this::onPlayerLeave);
    }

    private void registerS2C(String id, java.util.function.Function<com.kasperstudios.khwrapper.IPacketBuffer, IPacketPayload> decoder) {
        KHWrapper.register(NetworkRegistration.s2c(id, decoder, (packet, context) -> {
            BiConsumer<IPacketPayload, IPacketContext> handler = CLIENT_HANDLERS.get(id);
            if (handler != null) {
                handler.accept(packet, context);
            }
        }));
    }

    private void onPlayerJoin() {
        if (KHWrapper.get().isClientSide()) {
            // Delay handshake slightly to ensure networking is stable
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    sendHandshake();
                } catch (InterruptedException e) {
                    Kashub.LOGGER.error("Handshake delay interrupted", e);
                }
            }).start();
        }
    }

    private void onPlayerLeave() {
        if (KHWrapper.get().isClientSide()) {
            ServerModeManager.getInstance().switchToLocal();
        }
    }

    private void sendHandshake() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("modId", "kashub");
            json.addProperty("version", "1.0.0");
            json.addProperty("platform", System.getProperty("os.name"));
            
            JsonObject caps = new JsonObject();
            caps.addProperty("supportsServerConfig", true);
            caps.addProperty("supportsCommandBlocking", true);
            caps.addProperty("supportsEditorLock", true);
            caps.addProperty("supportsClientScripts", true);

            json.add("capabilities", caps);

            KHWrapper.sendPacket(new NetworkingPayloads.HandshakePayload(gson.toJson(json)));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send handshake", e);
        }
    }

    public void sendLog(String type, String data) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json.addProperty("data", data);
            json.addProperty("time", System.currentTimeMillis());

            KHWrapper.sendPacket(new NetworkingPayloads.LogPayload(gson.toJson(json)));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send log packet", e);
        }
    }

    public void sendCCTerminalInput(int computerId, String input) {
        try {
            KHWrapper.sendPacket(new NetworkingPayloads.CCTerminalInputPayload(computerId, input));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send CC terminal input", e);
        }
    }

    public void sendCCScriptPush(int computerId, String filename, String content) {
        try {
            KHWrapper.sendPacket(new NetworkingPayloads.CCScriptPushPayload(computerId, filename, content));
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to send CC script push", e);
        }
    }
}

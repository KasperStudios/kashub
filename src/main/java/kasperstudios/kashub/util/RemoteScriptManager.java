package kasperstudios.kashub.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RemoteScriptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteScriptManager.class);
    private static final RemoteScriptManager INSTANCE = new RemoteScriptManager();
    private final Gson gson = new Gson();

    // Callbacks
    private Consumer<List<String>> onListReceived;
    private Consumer<String> onActionResponse;
    private Consumer<String> onScriptContentReceived; 
    private Consumer<List<kasperstudios.kashub.gui.ServerCommandMetadata>> onMetadataReceived;

    private RemoteScriptManager() {
    }

    public static RemoteScriptManager getInstance() {
        return INSTANCE;
    }

    // --- Public API ---

    public void fetchList(Consumer<List<String>> callback) {
        this.onListReceived = callback;
        sendAction(0, "{}");
    }

    public void saveScript(String name, String content, Consumer<String> callback) {
        this.onActionResponse = callback;
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("content", content);
        sendAction(1, gson.toJson(json));
    }

    public void deleteScript(String name, Consumer<String> callback) {
        this.onActionResponse = callback;
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        sendAction(2, gson.toJson(json));
    }

    public void runScript(String name, Consumer<String> callback) {
        this.onActionResponse = callback;
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        sendAction(3, gson.toJson(json));
    }

    public void fetchContent(String name, Consumer<String> callback) {
        this.onScriptContentReceived = callback;
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        sendAction(4, gson.toJson(json));
    }

    public void fetchMetadata(Consumer<List<kasperstudios.kashub.gui.ServerCommandMetadata>> callback) {
        this.onMetadataReceived = callback;
        sendAction(5, "{}");
    }

    // --- Internal Networking ---

    public void handleMessage(byte action, String payload) {
        switch (action) {
            case 0: // LIST
                if (onListReceived != null) {
                    try {
                        List<String> scripts = gson.fromJson(payload, new TypeToken<List<String>>() {
                        }.getType());
                        onListReceived.accept(scripts);
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse script list", e);
                        onListReceived.accept(new ArrayList<>());
                    }
                    onListReceived = null;
                }
                break;
            case 1: // SAVE response
            case 2: // DELETE response
            case 3: // RUN response
                if (onActionResponse != null) {
                    onActionResponse.accept(payload);
                    onActionResponse = null;
                }
                break;
            case 4: // READ response
                if (onScriptContentReceived != null) {
                    onScriptContentReceived.accept(payload);
                    onScriptContentReceived = null;
                }
                break;
            case 5: // METADATA response
                if (onMetadataReceived != null) {
                    try {
                        List<kasperstudios.kashub.gui.ServerCommandMetadata> meta = gson.fromJson(payload,
                                new TypeToken<List<kasperstudios.kashub.gui.ServerCommandMetadata>>() {
                                }.getType());
                        onMetadataReceived.accept(meta);
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse server command metadata", e);
                        onMetadataReceived.accept(new ArrayList<>());
                    }
                    onMetadataReceived = null;
                }
                break;
        }
    }

    private void sendAction(int action, String json) {
        KHWrapper.sendPacket(new NetworkingPayloads.ScriptMgmtPayload((byte) action, json));
    }
}

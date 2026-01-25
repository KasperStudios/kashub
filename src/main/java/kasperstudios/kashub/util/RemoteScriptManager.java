package kasperstudios.kashub.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import kasperstudios.kashub.network.payload.NetworkingPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RemoteScriptManager {
    private static final RemoteScriptManager INSTANCE = new RemoteScriptManager();
    private final Gson gson = new Gson();

    public static final Identifier CHANNEL = Identifier.of("kashub", "script_mgmt");

    // Callbacks
    private Consumer<List<String>> onListReceived;
    private Consumer<String> onActionResponse;
    private Consumer<String> onScriptContentReceived; // Not really supported by current plugin protocol?
    // Wait, plugin "Run" executes it. Plugin "List" lists it. Plugin "Save" saves
    // it.
    // Does Plugin "Read" exist?
    // Checking ScriptNetworkHandler: 0=List, 1=Save, 2=Delete, 3=Run.
    // It does NOT have a "Read" (Get Content) action yet.
    // I need to add that to plugin if I want to edit files.

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

    private Consumer<List<kasperstudios.kashub.gui.ServerCommandMetadata>> onMetadataReceived;

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
                        e.printStackTrace();
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
                        e.printStackTrace();
                        onMetadataReceived.accept(new ArrayList<>());
                    }
                    onMetadataReceived = null;
                }
                break;
        }
    }

    private void sendAction(int action, String json) {
        ClientPlayNetworking.send(new NetworkingPayloads.ScriptMgmtPayload((byte) action, json));
    }
}

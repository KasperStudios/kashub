package kasperstudios.kashub.util;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientTickCallback {
    private static final List<Consumer<MinecraftClient>> END_TICK_CALLBACKS = new ArrayList<>();

    public static void registerEndTick(Consumer<MinecraftClient> callback) {
        END_TICK_CALLBACKS.add(callback);
    }

    public static void onEndTick(MinecraftClient client) {
        for (Consumer<MinecraftClient> callback : END_TICK_CALLBACKS) {
            try {
                callback.accept(client);
            } catch (Exception e) {
                System.err.println("Error in client tick callback: " + e.getMessage());
            }
        }
    }
}
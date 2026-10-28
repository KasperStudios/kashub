package kasperstudios.kashub.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTickCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTickCallback.class);
    private static final List<Consumer<Minecraft>> END_TICK_CALLBACKS = new ArrayList<>();

    public static void registerEndTick(Consumer<Minecraft> callback) {
        END_TICK_CALLBACKS.add(callback);
    }

    public static void onEndTick(Minecraft client) {
        for (Consumer<Minecraft> callback : END_TICK_CALLBACKS) {
            try {
                callback.accept(client);
            } catch (Exception e) {
                LOGGER.error("Error in client tick callback: {}", e.getMessage());
            }
        }
    }
}

package kasperstudios.kashub;

import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.command.ScriptCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kashub implements ModInitializer {
    public static final String MOD_ID = "kashub";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    public static final boolean DEBUG_MODE = false;

    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Kashub {} initializing...", VERSION);

        CommandRegistry.initialize();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ScriptCommand.register(dispatcher);
        });

        LOGGER.info("Kashub {} initialized successfully!", VERSION);
    }
}
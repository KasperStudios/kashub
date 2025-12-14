package kasperstudios.kashub;

import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.command.ScriptCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kashub implements ModInitializer {
    public static final String MOD_ID = "kashub";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Kashub v3.0 initializing...");
        
        // Initialize command registry
        CommandRegistry.initialize();
        
        // Register server commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ScriptCommand.register(dispatcher);
        });
        
        LOGGER.info("Kashub initialized successfully!");
    }
}
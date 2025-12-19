package kasperstudios.kashub;

import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.command.ScriptCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Kashub mod.
 * Kashub is a scripting mod for Minecraft that allows players to automate
 * in-game actions using KHScript - a simple, Lua/JavaScript-inspired language.
 */
public class Kashub implements ModInitializer {
    public static final String MOD_ID = "kashub";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    /** Current mod version, loaded from fabric.mod.json */
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    
    /** Enable debug logging (set to false for production builds) */
    public static final boolean DEBUG_MODE = false;
    
    /**
     * Log a debug message (only if DEBUG_MODE is enabled)
     * @param message The message to log
     * @param args Format arguments
     */
    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Kashub {} initializing...", VERSION);
        
        // Initialize command registry
        CommandRegistry.initialize();
        
        // Register server commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ScriptCommand.register(dispatcher);
        });
        
        LOGGER.info("Kashub {} initialized successfully!", VERSION);
    }
}
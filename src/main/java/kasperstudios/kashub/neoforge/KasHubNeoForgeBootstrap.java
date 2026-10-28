package kasperstudios.kashub.neoforge;

import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.neoforge.NeoForgePlatform;
import kasperstudios.kashub.Kashub;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Kashub.MOD_ID)
public class KasHubNeoForgeBootstrap {
    private static final Logger LOGGER = LogManager.getLogger();

    public KasHubNeoForgeBootstrap(IEventBus modEventBus) {
        // Initialize the platform bridge
        KHWrapper.init(new NeoForgePlatform());
        
        // Register the core mod
        Kashub.init();

        // Register mod bus listeners for the wrapper
        modEventBus.register(com.kasperstudios.khwrapper.neoforge.NeoForgeNetworkingBootstrap.class);

        // Register forge bus listeners for the wrapper
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.kasperstudios.khwrapper.neoforge.NeoForgeEventHandlers.class);
        
        // Curios slot registration is now handled via data/curios/slots/neural.json
        // No IMC registration needed for NeoForge 1.21.1+ (data-driven approach)

        // Register common mod bus handlers for the wrapper
        modEventBus.register(com.kasperstudios.khwrapper.neoforge.NeoForgeModBusHandlers.class);

        // Initialize client-side systems if running on a physical client
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            kasperstudios.kashub.neoforge.client.KasHubNeoForgeClientInit.init(modEventBus);
        }
    }
}

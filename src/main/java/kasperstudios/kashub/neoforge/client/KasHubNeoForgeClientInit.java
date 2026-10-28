package kasperstudios.kashub.neoforge.client;

import com.kasperstudios.khwrapper.KHClientWrapper;
import com.kasperstudios.khwrapper.neoforge.NeoForgeClientPlatform;
import kasperstudios.kashub.client.KashubClient;
import kasperstudios.kashub.core.KashubCommandExecutor;
import net.neoforged.bus.api.IEventBus;

public class KasHubNeoForgeClientInit {

    public static void init(IEventBus modEventBus) {
        // Initialize client wrapper
        KHClientWrapper.init(new NeoForgeClientPlatform());

        // Register unified client commands through the client wrapper
        KHClientWrapper.registerClientCommand("kashub", "Main command for KasHub mod", context -> {
            KashubCommandExecutor.execute(context);
        });

        // Initialize KasHub client logic
        KashubClient.init();

        // Register client mod bus handlers for the wrapper
        modEventBus.register(com.kasperstudios.khwrapper.neoforge.NeoForgeClientModBusHandlers.class);
        
        // Register client forge bus handlers for the wrapper
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(com.kasperstudios.khwrapper.neoforge.NeoForgeClientEventHandlers.class);
    }
}

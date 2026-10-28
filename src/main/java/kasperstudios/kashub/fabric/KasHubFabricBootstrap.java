package kasperstudios.kashub.fabric;

import net.fabricmc.api.ModInitializer;
import kasperstudios.kashub.Kashub;
import com.kasperstudios.khwrapper.KHWrapper;
import com.kasperstudios.khwrapper.fabric.FabricPlatform;

public class KasHubFabricBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        // Initialize the wrapper for Fabric
        KHWrapper.init(new FabricPlatform());
        
        // Run core initialization
        Kashub.init();
        
        // Register unified client commands through the wrapper
        KHWrapper.get().registerClientCommand("kashub", "Main command for KasHub mod", context -> {
            kasperstudios.kashub.core.KashubCommandExecutor.execute(context);
        });
    }
}

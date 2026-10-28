package kasperstudios.kashub.fabric;

import net.fabricmc.api.ClientModInitializer;
import kasperstudios.kashub.client.KashubClient;
import com.kasperstudios.khwrapper.KHClientWrapper;
import com.kasperstudios.khwrapper.fabric.FabricClientPlatform;

public class KashubFabricClientBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KHClientWrapper.init(new FabricClientPlatform());
        KashubClient.init();
    }
}

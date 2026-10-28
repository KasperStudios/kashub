package kasperstudios.kashub;

import kasperstudios.kashub.core.Registry;
import kasperstudios.khgui.core.DraftManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kasperstudios.khwrapper.KHWrapper;

public class Kashub {
    public static final String MOD_ID = "kashub";
    public static final String MOD_VERSION = "1.0.0-FSR";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String VERSION = "unknown";

    public static final boolean DEBUG_MODE = false;

    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }

    public static void init() {
        VERSION = KHWrapper.get().getModVersion(MOD_ID);

        LOGGER.info("Kashub {} initializing...", VERSION);

        kasperstudios.kashub.core.content.ModContent.initialize();
        kasperstudios.kashub.services.InjectionService.getInstance().initialize();
        kasperstudios.kashub.services.AccessPolicy.getInstance().initialize();
        Registry.initialize();

        // Extract system scripts to config dir on first launch
        kasperstudios.kashub.util.ResourceManager.initialize();
        kasperstudios.kashub.network.NetworkingManager.getInstance().initialize();
        kasperstudios.kashub.services.network.PacketManager.getInstance().initialize();
        kasperstudios.kashub.services.network.ConfigSyncManager.getInstance().initialize();

        // Initialize KHGui DraftManager
        DraftManager.initialize(KHWrapper.getConfigDir().resolve("kashub/drafts"));

        // Register Curios slot via IMC (Inter-Mod Communication) for NeoForge/Forge
        // This is the most reliable way to ensure the slot appears in GUI.
        if (KHWrapper.get().isModLoaded("curios")) {
            try {
                // We use reflection to send IMC without a hard dependency on NeoForge/Curios
                Class<?> slotTypeMessageClass = Class.forName("top.theillusivec4.curios.api.SlotTypeMessage");
                Class<?> builderClass = Class.forName("top.theillusivec4.curios.api.SlotTypeMessage$Builder");
                
                Object builder = builderClass.getConstructor(String.class).newInstance("neural");
                builderClass.getMethod("size", int.class).invoke(builder, 1);
                builderClass.getMethod("icon", net.minecraft.resources.ResourceLocation.class)
                    .invoke(builder, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kashub", "gui/implant_slot_bg"));
                
                try {
                    builderClass.getMethod("useNativeGui", boolean.class).invoke(builder, true);
                } catch (Exception ignored) {
                    // Method removed in newer Curios API versions, safe to ignore
                    LOGGER.debug("useNativeGui method not available in this Curios API version: {}", ignored.getMessage());
                }
                
                Object message = builderClass.getMethod("build").invoke(builder);
                
                // Send IMC: InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> message)
                Class<?> imcClass = Class.forName("net.neoforged.fml.InterModComms");
                java.lang.reflect.Method sendTo = imcClass.getMethod("sendTo", String.class, String.class, java.util.function.Supplier.class);
                
                sendTo.invoke(null, "curios", "register_type", (java.util.function.Supplier<?>) () -> message);
                LOGGER.info("Kashub: Sent IMC to Curios for 'neural' slot registration");
            } catch (Exception e) {
                LOGGER.warn("Kashub: Failed to send IMC to Curios: {}", e.getMessage());
            }
        }

        // Client-side initializations (moved to KashubClient for lifecycle reasons)
        KHWrapper.onClientInit(() -> {
            // DocumentationManager registers client-only objects (PlayerObject, VisionObject, etc.)
            // and MUST NOT run on a dedicated server — client only.
            kasperstudios.kashub.util.DocumentationManager.initialize();
            kasperstudios.kashub.client.VisualEffectRenderer.getInstance().initialize();
        });

        LOGGER.info("Kashub {} initialized successfully!", VERSION);
    }
}

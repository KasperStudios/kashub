package kasperstudios.kashub.core.content;

import com.kasperstudios.khwrapper.KHWrapper;
import net.minecraft.world.item.Item;

public class ModContent {
    public static NeuroImplantItem NEURO_IMPLANT_T1;
    public static NeuroImplantItem NEURO_IMPLANT_T2;

    public static void initialize() {
        // T1 is the core base implant — always available in vanilla
        NEURO_IMPLANT_T1 = new NeuroImplantItem(new Item.Properties().stacksTo(1), 1);
        KHWrapper.get().registerItem("neuro_implant_t1", NEURO_IMPLANT_T1);

        java.util.List<Item> tabItems = new java.util.ArrayList<>();
        tabItems.add(NEURO_IMPLANT_T1);

        // T2 requires ComputerCraft: Tweaked
        if (KHWrapper.get().isModLoaded("computercraft")) {
            NEURO_IMPLANT_T2 = new NeuroImplantItem(new Item.Properties().stacksTo(1), 2);
            KHWrapper.get().registerItem("neuro_implant_t2", NEURO_IMPLANT_T2);
            tabItems.add(NEURO_IMPLANT_T2);
        }

        // Creative Tab with T1 as the icon
        KHWrapper.get().registerCreativeTab("main", 
            net.minecraft.network.chat.Component.literal("KasHub"), 
            () -> new net.minecraft.world.item.ItemStack(NEURO_IMPLANT_T1),
            tabItems);
    }
}

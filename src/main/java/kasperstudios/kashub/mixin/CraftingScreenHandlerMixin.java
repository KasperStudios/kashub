package kasperstudios.kashub.mixin;

import kasperstudios.kashub.services.modpack.CustomCraftingManager;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to integrate custom crafting recipes into the crafting table.
 * Checks custom recipes when crafting grid changes.
 */
@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin extends ScreenHandler {

    protected CraftingScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "onContentChanged", at = @At("TAIL"))
    private void onCraftingGridChanged(CallbackInfo ci) {
        try {
            // Get the crafting inventory (first 9 slots are the crafting grid)
            RecipeInputInventory craftingInventory = null;
            
            // Find the crafting inventory from slots
            for (Slot slot : this.slots) {
                if (slot.inventory instanceof RecipeInputInventory) {
                    craftingInventory = (RecipeInputInventory) slot.inventory;
                    break;
                }
            }
            
            if (craftingInventory == null) {
                return;
            }
            
            // Check if any custom recipe matches
            ItemStack result = CustomCraftingManager.getInstance().matchRecipe(craftingInventory);
            
            if (!result.isEmpty()) {
                // Set the result slot (slot 0 is the output)
                this.slots.get(0).setStack(result);
            }
            
        } catch (Exception e) {
            kasperstudios.kashub.Kashub.LOGGER.error("Error in CraftingScreenHandlerMixin", e);
        }
    }
}

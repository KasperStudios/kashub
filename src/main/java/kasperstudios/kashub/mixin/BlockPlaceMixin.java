package kasperstudios.kashub.mixin;

import kasperstudios.kashub.core.events.EventManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockPlaceMixin {

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void onPlaceBlock(net.minecraft.client.network.ClientPlayerEntity player, Hand hand,
                              BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        try {
            // Only fire event if block was successfully placed
            if (cir.getReturnValue() == ActionResult.SUCCESS && player.getStackInHand(hand).getItem() instanceof net.minecraft.item.BlockItem) {
                Map<String, Object> placeData = new HashMap<>();
                placeData.put("x", hitResult.getBlockPos().getX());
                placeData.put("y", hitResult.getBlockPos().getY());
                placeData.put("z", hitResult.getBlockPos().getZ());
                placeData.put("block", player.getStackInHand(hand).getName().getString());
                placeData.put("side", hitResult.getSide().getName());

                EventManager.getInstance().fireEvent("onBlockPlace", placeData);
            }
        } catch (Exception e) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                "Error in BlockPlaceMixin: " + e.getMessage());
        }
    }
}

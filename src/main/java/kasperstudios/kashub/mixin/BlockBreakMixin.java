package kasperstudios.kashub.mixin;

import kasperstudios.kashub.algorithm.events.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(ClientPlayerInteractionManager.class)
public class BlockBreakMixin {

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            String blockName = client.world.getBlockState(pos).getBlock().getName().getString();

            Map<String, Object> breakData = new HashMap<>();
            breakData.put("x", pos.getX());
            breakData.put("y", pos.getY());
            breakData.put("z", pos.getZ());
            breakData.put("block", blockName);

            EventManager.getInstance().fireEvent("onBlockBreak", breakData);
        } catch (Exception e) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                "Error in BlockBreakMixin: " + e.getMessage());
        }
    }
}

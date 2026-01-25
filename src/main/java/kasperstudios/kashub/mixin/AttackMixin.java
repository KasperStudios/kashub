package kasperstudios.kashub.mixin;

import kasperstudios.kashub.core.events.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(MinecraftClient.class)
public class AttackMixin {

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        try {
            @SuppressWarnings("resource")
            MinecraftClient client = (MinecraftClient) (Object) this;

            if (client.crosshairTarget instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();

                Map<String, Object> attackData = new HashMap<>();
                attackData.put("target_name", target.getName().getString());
                attackData.put("target_id", target.getId());
                attackData.put("target_type", target.getType().toString());
                attackData.put("target_x", target.getX());
                attackData.put("target_y", target.getY());
                attackData.put("target_z", target.getZ());

                EventManager.getInstance().fireEvent("onAttack", attackData);
            }
        } catch (Exception e) {
            kasperstudios.kashub.util.ScriptLogger.getInstance().error(
                "Error in AttackMixin: " + e.getMessage());
        }
    }
}

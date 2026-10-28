package kasperstudios.kashub.mixin;

import kasperstudios.kashub.core.objects.GameObject;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 6)
    private float modifyGamma(float gamma) {
        if (GameObject.fullBrightEnabled) {
            return 100.0f;
        }
        return gamma;
    }
}

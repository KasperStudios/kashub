package kasperstudios.kashub.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class TeleportStaffItem extends Item {
    private static final double MAX_DISTANCE = 50.0;
    private static final int COOLDOWN_TICKS = 20;

    public TeleportStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.multiply(MAX_DISTANCE));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
            eyePos,
            endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            Vec3d teleportPos = Vec3d.ofCenter(hitResult.getBlockPos().up());
            player.teleport(teleportPos.x, teleportPos.y, teleportPos.z, false);
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }
}

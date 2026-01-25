package kasperstudios.kashub.core.fairplay;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.network.ServerModeManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.math.BlockPos;

/**
 * FairPlayGuard enforces rules to prevent cheating when cheats are disabled.
 * It strictly validates interactions against server policies and physical
 * reach.
 */
public class FairPlayGuard {
    private static final double DEFAULT_REACH = 4.5;

    /**
     * Checks if Fair Play restrictions should apply.
     * Restrictions apply if:
     * 1. Server mode enforces restricted access (overrides local config).
     * 2. OR local config has allowCheats = false.
     * 
     * @return true if fair play (anti-cheat) logic is active.
     */
    public static boolean isFairPlayEnforced() {
        // 1. Server Authority Logic
        ServerModeManager serverManager = ServerModeManager.getInstance();
        if (serverManager.isServerControlled()) {
            if (!serverManager.isCheatModeAllowed()) {
                return true; // Server forbids cheats -> Forced Fair Play
            }
        }

        // 2. Local Config Logic
        return !KashubConfig.getInstance().allowCheats;
    }

    /**
     * Validates reach and visibility for Block interaction.
     * 
     * Rules:
     * - Must be within reach distance.
     * - Must be visible (Line of Sight).
     * - Glass/Water considered transparent (Visual raycast).
     */
    public static boolean validateBlockReach(PlayerEntity player, BlockPos targetPos) {
        if (!isFairPlayEnforced())
            return true;

        Vec3d cameraPos = player.getCameraPosVec(1.0f);
        Vec3d targetCenter = targetPos.toCenterPos();

        // 1. Distance Check
        double reach = getReachDistance(player);
        if (cameraPos.squaredDistanceTo(targetCenter) > reach * reach) {
            return false;
        }

        // 2. Line of Sight Check (Raycast)
        // FluidHandling.NONE -> See through water
        // ShapeType.VISUAL -> See through glass/grass, hit solid blocks
        RaycastContext context = new RaycastContext(
                cameraPos,
                targetCenter,
                RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE,
                player);

        BlockHitResult hit = player.getWorld().raycast(context);

        // Success if we hit ANY part of the target block, or if we didn't hit anything
        // blocking us (unlikely for block check but safe)
        // Or if the hit block IS the target block.
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos().equals(targetPos);
        }

        return false;
    }

    /**
     * Validates reach and visibility for Entity interaction (Attack/Interact).
     * Uses ProjectileUtil for precise bounding box checking.
     */
    public static boolean validateEntityReach(PlayerEntity player, Entity targetEntity) {
        if (!isFairPlayEnforced())
            return true;
        if (targetEntity == null)
            return false;

        double reach = getReachDistance(player);
        Vec3d cameraPos = player.getCameraPosVec(1.0f);
        Vec3d rotationVec = player.getRotationVec(1.0f);
        Vec3d endPos = cameraPos.add(rotationVec.multiply(reach));

        // 1. Broad Phase: Bounding Box expansion
        Box box = player.getBoundingBox().stretch(rotationVec.multiply(reach)).expand(1.0D, 1.0D, 1.0D);

        // 2. Narrow Phase: Entity Raycast
        EntityHitResult hit = ProjectileUtil.raycast(
                player,
                cameraPos,
                endPos,
                box,
                (entity) -> !entity.isSpectator() && entity.canHit(),
                reach * reach);

        // Success only if the raycast explicitly hit our target
        return hit != null && hit.getEntity() == targetEntity;
    }

    private static double getReachDistance(PlayerEntity player) {
        // In future: read attribute 'generic.block_interaction_range'
        return DEFAULT_REACH;
    }
}

package kasperstudios.kashub.core.fairplay;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.network.ServerModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
    public static boolean validateBlockReach(Player player, BlockPos targetPos) {
        if (!isFairPlayEnforced())
            return true;

        Vec3 cameraPos = player.getEyePosition(1.0f);
        Vec3 targetCenter = targetPos.getCenter();

        // 1. Distance Check
        double reach = getReachDistance(player);
        if (cameraPos.distanceToSqr(targetCenter) > reach * reach) {
            return false;
        }

        // 2. Line of Sight Check (Raycast)
        // FluidHandling.NONE -> See through water
        // ShapeType.VISUAL -> See through glass/grass, hit solid blocks
        ClipContext context = new ClipContext(
                cameraPos,
                targetCenter,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player);

        BlockHitResult hit = player.level().clip(context);

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
    public static boolean validateEntityReach(Player player, Entity targetEntity) {
        if (!isFairPlayEnforced())
            return true;
        if (targetEntity == null)
            return false;

        double reach = getReachDistance(player);
        Vec3 cameraPos = player.getEyePosition(1.0f);
        Vec3 rotationVec = player.getViewVector(1.0f);
        Vec3 endPos = cameraPos.add(rotationVec.scale(reach));

        // 1. Broad Phase: Bounding Box expansion
        AABB box = player.getBoundingBox().expandTowards(rotationVec.scale(reach)).inflate(1.0D, 1.0D, 1.0D);

        // 2. Narrow Phase: Entity Raycast
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                cameraPos,
                endPos,
                box,
                (entity) -> !entity.isSpectator() && entity.isPickable(),
                reach * reach);

        // Success only if the raycast explicitly hit our target
        return hit != null && hit.getEntity() == targetEntity;
    }

    private static double getReachDistance(Player player) {
        // In future: read attribute 'generic.block_interaction_range'
        return DEFAULT_REACH;
    }
}

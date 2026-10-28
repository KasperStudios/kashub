package kasperstudios.kashub.services;

import kasperstudios.kashub.config.KashubConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * AccessPolicy - Enforces interaction rules for KasHub features.
 * 
 * Supports three modes:
 * 1. SURVIVAL: Requires physical proximity and Neuro-Implant (Phase 1 Concept).
 * 2. CASUAL: Progression gates are skipped, but range check might still apply.
 * 3. CREATIVE: Unlimited remote access to all components.
 */
public class AccessPolicy {

    private static volatile AccessPolicy instance;
    private static final Object LOCK = new Object();

    private AccessPolicy() {}

    public static AccessPolicy getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new AccessPolicy();
                }
            }
        }
        return instance;
    }

    /**
     * Checks if the player can interact with a component at the given position.
     */
    public boolean canInteractWith(net.minecraft.world.entity.player.Player player, BlockPos pos) {
        if (player == null) return false;

        // 1. Creative Mode - Always allowed (both client and server)
        if (player.isCreative()) return true;

        KashubConfig config = KashubConfig.getInstance();

        // 2. Casual Policy - Skip range/implant gates
        if ("CASUAL".equalsIgnoreCase(config.accessPolicy)) {
            return true;
        }

        // 3. Survival Policy - Gated by Implant and Range
        if (config.neuroImplantGating && !isImplantInstalled(player)) {
            return false;
        }

        // Check Range
        double distSq = player.distanceToSqr(Vec3.atCenterOf(pos));
        double maxRange = getEffectiveReach(player);

        return distSq <= (maxRange * maxRange);
    }

    /**
     * Check if the Neuro-Implant is installed.
     */
    public boolean isImplantInstalled(net.minecraft.world.entity.player.Player player) {
        return getImplantTier(player) > 0;
    }

    /**
     * Returns the installed implant tier (0 = none).
     */
    public int getImplantTier(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0;
        if (player.isCreative()) return 3; // Creative mode has full access

        // v1.0.0-FSR: In Casual mode, we grant Tier 2 access by default for features like scanning,
        // but still check for the physical implant for Tier 3 features if any.
        if ("CASUAL".equalsIgnoreCase(KashubConfig.getInstance().accessPolicy)) {
            return 2;
        }

        if (player instanceof kasperstudios.kashub.core.IImplantData access) {
            return access.getImplantTier();
        }
        return 0;
    }

    /**
     * Returns the effective interaction range based on current access mode.
     * Survival: min(player attribute reach, config cap, 15 blocks hard limit)
     * Casual: 15 blocks
     * Creative: unlimited (128 blocks, effectively all loaded chunks)
     */
    public double getEffectiveReach(net.minecraft.world.entity.player.Player player) {
        if (player == null) return 0;
        if (player.isCreative()) return 128.0;

        KashubConfig config = KashubConfig.getInstance();
        if ("CASUAL".equalsIgnoreCase(config.accessPolicy)) return 15.0;

        // Survival: Use player's actual block reach attribute if available,
        // then clamp to config cap (default 10, max 15 per spec).
        double playerReach;
        try {
            playerReach = player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        } catch (Exception ignored) {
            playerReach = 4.5; // Vanilla survival default
        }

        double cap = Math.min(Math.max(config.maxInteractionRange, 4.5), 15.0);
        return Math.min(playerReach, cap);
    }

    public boolean hasAccess(net.minecraft.world.entity.player.Player player) {
        if (player == null) return false;
        if (player.isCreative() || player.isSpectator()) return true;
        if (player.hasPermissions(2)) return true;
        if ("CASUAL".equalsIgnoreCase(KashubConfig.getInstance().accessPolicy)) return true;
        return isImplantInstalled(player);
    }

    public void initialize() {
        // No-op for now, but kept for compatibility with entrypoints
    }
}

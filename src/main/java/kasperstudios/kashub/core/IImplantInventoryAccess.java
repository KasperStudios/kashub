package kasperstudios.kashub.core;

import net.minecraft.world.Container;

/**
 * Duck interface implemented by PlayerEntityMixin.
 * Allows non-mixin code to safely access the implant inventory
 * without a direct dependency on the mixin class.
 *
 * Usage: if (player instanceof IImplantInventoryAccess access) { ... }
 */
public interface IImplantInventoryAccess {
    Container kashub$getImplantInventory();
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class InteractCommand implements Command {
    @Override
    public String getName() {
        return "interact";
    }

    @Override
    public String getDescription() {
        return "Interacts with the targeted block or entity";
    }

    @Override
    public String getParameters() {
        return "[block|entity|auto]";
    }
    
    @Override
    public String getCategory() {
        return "Interaction";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Right-clicks on targeted block or entity.\n\n" +
               "Usage:\n" +
               "  interact                - Auto-detect target type\n" +
               "  interact block          - Only interact with blocks\n" +
               "  interact entity         - Only interact with entities\n\n" +
               "Details:\n" +
               "  - Simulates right-click action\n" +
               "  - 'auto' mode detects what player is looking at\n" +
               "  - Block interactions: open chests, doors, buttons, etc.\n" +
               "  - Entity interactions: trade with villagers, ride horses, etc.\n" +
               "  - Uses item in main hand if applicable\n\n" +
               "Block Examples:\n" +
               "  - Chests, barrels, shulker boxes (opens GUI)\n" +
               "  - Doors, trapdoors, fence gates (toggles)\n" +
               "  - Buttons, levers (activates)\n" +
               "  - Crafting table, furnace (opens GUI)\n" +
               "  - Beds (sleep if night)\n\n" +
               "Entity Examples:\n" +
               "  - Villagers (opens trade GUI)\n" +
               "  - Horses, pigs, boats (mount)\n" +
               "  - Item frames (rotate item)\n" +
               "  - Armor stands (equip armor)\n\n" +
               "Examples:\n" +
               "  lookAt entity villager\n" +
               "  interact entity         // Open trade GUI\n\n" +
               "  lookAt 100 64 200\n" +
               "  interact block          // Open chest at coords";
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        String mode = args.length > 0 ? args[0].toLowerCase() : "auto";

        HitResult hit = client.crosshairTarget;
        if (hit == null) return;

        switch (mode) {
            case "block":
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                }
                break;
            case "entity":
                if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    Entity entity = entityHit.getEntity();
                    client.interactionManager.interactEntity(player, entity, Hand.MAIN_HAND);
                }
                break;
            default:
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHit);
                } else if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    Entity entity = entityHit.getEntity();
                    client.interactionManager.interactEntity(player, entity, Hand.MAIN_HAND);
                }
                break;
        }
    }
}

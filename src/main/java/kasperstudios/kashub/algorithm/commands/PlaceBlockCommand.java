package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PlaceBlockCommand implements Command {
    @Override
    public String getName() {
        return "place";
    }

    @Override
    public String getDescription() {
        return "Places a block at the target position";
    }

    @Override
    public String getParameters() {
        return "[x y z] - optional coordinates";
    }
    
    @Override
    public String getCategory() {
        return "Interaction";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Places a block from hotbar at target position.\n\n" +
               "Usage:\n" +
               "  place                   - Place at crosshair target\n" +
               "  place <x> <y> <z>       - Place at coordinates\n\n" +
               "Details:\n" +
               "  - Uses block in currently selected hotbar slot\n" +
               "  - Without coords: places on face of targeted block\n" +
               "  - With coords: places at exact position\n" +
               "  - Simulates right-click with block\n" +
               "  - Respects block placement rules\n\n" +
               "Examples:\n" +
               "  selectSlot item cobblestone\n" +
               "  place                   // Place at crosshair\n\n" +
               "  selectSlot 0\n" +
               "  place 100 64 200        // Place at coords\n\n" +
               "Building script example:\n" +
               "  for (i = 0; i < 10; i++) {\n" +
               "      place ~$i ~ ~0\n" +
               "      wait 100\n" +
               "  }\n\n" +
               "Note: Select block in hotbar first with 'selectSlot'.";
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        if (args.length >= 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                BlockPos pos = new BlockPos(x, y, z);
                placeBlockAt(client, player, pos);
            } catch (NumberFormatException e) {
                // Invalid coordinates
            }
        } else {
            HitResult hit = client.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos placePos = blockHit.getBlockPos().offset(blockHit.getSide());
                placeBlockAt(client, player, placePos);
            }
        }
    }

    private void placeBlockAt(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        if (client.interactionManager != null) {
            BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos),
                Direction.UP,
                pos,
                false
            );
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
        }
    }
}

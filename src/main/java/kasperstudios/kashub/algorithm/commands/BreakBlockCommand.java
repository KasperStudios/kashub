package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.mixin.KeyBindingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BreakBlockCommand implements Command {
    @Override
    public String getName() {
        return "breakBlock";
    }

    @Override
    public String getDescription() {
        return "Breaks a block at the target position or in front of player";
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
        return "Breaks a block at specified position or crosshair target.\n\n" +
               "Usage:\n" +
               "  breakBlock              - Break block at crosshair\n" +
               "  breakBlock <x> <y> <z>  - Break block at coordinates\n\n" +
               "Details:\n" +
               "  - Without args: breaks block player is looking at\n" +
               "  - With coords: breaks block at exact position\n" +
               "  - Simulates left-click attack action\n" +
               "  - Respects tool requirements (pickaxe for stone, etc.)\n" +
               "  - Breaking time depends on tool and block hardness\n" +
               "  - Does NOT wait for block to fully break\n\n" +
               "Examples:\n" +
               "  breakBlock                  // Break targeted block\n" +
               "  breakBlock 100 64 200       // Break at coords\n" +
               "  loop { breakBlock; wait 50 } // Mining loop\n\n" +
               "Tip: Use with 'lookAt' to target specific blocks:\n" +
               "  lookAt 100 64 200\n" +
               "  breakBlock";
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
                breakBlockAt(client, pos);
            } catch (NumberFormatException e) {
                // Invalid coordinates
            }
        } else {
            HitResult hit = client.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                breakBlockAt(client, blockHit.getBlockPos());
            } else {
                Vec3d lookVec = player.getRotationVec(1.0f);
                BlockPos targetPos = BlockPos.ofFloored(
                    player.getX() + lookVec.x * 3,
                    player.getEyeY() + lookVec.y * 3,
                    player.getZ() + lookVec.z * 3
                );
                breakBlockAt(client, targetPos);
            }
        }
    }

    private void breakBlockAt(MinecraftClient client, BlockPos pos) {
        if (client.interactionManager != null) {
            ((KeyBindingMixin) client.options.attackKey).setPressed(true);
            client.execute(() -> {
                client.interactionManager.attackBlock(pos, net.minecraft.util.math.Direction.UP);
                ((KeyBindingMixin) client.options.attackKey).setPressed(false);
            });
        }
    }
}

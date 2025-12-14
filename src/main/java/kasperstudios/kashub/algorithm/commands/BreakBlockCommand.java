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
        return "[x y z]";
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

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class GetBlockCommand implements Command {
    @Override
    public String getName() {
        return "getBlock";
    }

    @Override
    public String getDescription() {
        return "Gets information about a block at position";
    }

    @Override
    public String getParameters() {
        return "[x y z] [print]";
    }
    
    @Override
    public String getCategory() {
        return "Vision";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Gets block information and stores in variables.\n\n" +
               "Usage:\n" +
               "  getBlock                - Block at crosshair\n" +
               "  getBlock <x> <y> <z>    - Block at coordinates\n" +
               "  getBlock print          - Print result to chat\n" +
               "  getBlock <x> <y> <z> print\n\n" +
               "Variables Set:\n" +
               "  $block_id   - Block identifier (e.g. 'minecraft:stone')\n" +
               "  $block_x    - Block X coordinate\n" +
               "  $block_y    - Block Y coordinate\n" +
               "  $block_z    - Block Z coordinate\n" +
               "  $error      - Error message if failed\n\n" +
               "Details:\n" +
               "  - Without coords: uses crosshair target\n" +
               "  - If no block targeted: uses position 3 blocks ahead\n" +
               "  - 'print' flag shows result in chat\n" +
               "  - Block ID includes namespace (minecraft:)\n\n" +
               "Examples:\n" +
               "  getBlock                // Check crosshair\n" +
               "  print $block_id         // Show block type\n\n" +
               "  getBlock 100 64 200\n" +
               "  if $block_id == \"minecraft:diamond_ore\" {\n" +
               "      print \"Found diamonds!\"\n" +
               "  }\n\n" +
               "  getBlock print          // Quick check with output\n\n" +
               "Tip: Use with 'scan' for finding specific blocks.";
    }

    @Override
    public void execute(String[] args) {
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        BlockPos pos;
        if (args.length >= 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                pos = new BlockPos(x, y, z);
            } catch (NumberFormatException e) {
                interpreter.setVariable("error", "Invalid coordinates");
                return;
            }
        } else {
            HitResult hit = client.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                pos = ((BlockHitResult) hit).getBlockPos();
            } else {
                Vec3d lookVec = player.getRotationVec(1.0f);
                pos = BlockPos.ofFloored(
                    player.getX() + lookVec.x * 3,
                    player.getEyeY() + lookVec.y * 3,
                    player.getZ() + lookVec.z * 3
                );
            }
        }

        BlockState state = player.getWorld().getBlockState(pos);
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        
        interpreter.setVariable("block_id", blockId);
        interpreter.setVariable("block_x", String.valueOf(pos.getX()));
        interpreter.setVariable("block_y", String.valueOf(pos.getY()));
        interpreter.setVariable("block_z", String.valueOf(pos.getZ()));

        if (args.length > 0 && args[args.length - 1].equalsIgnoreCase("print")) {
            String message = "Block at " + pos.toShortString() + ": " + blockId;
            player.sendMessage(Text.literal(message), false);
        }
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Command for teleporting player
 * Syntax: tp x y z or tp ~dx ~dy ~dz
 */
public class TeleportCommand implements Command {

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleports player to specified coordinates";
    }

    @Override
    public String getParameters() {
        return "<x> <y> <z> - coordinates";
    }
    
    @Override
    public String getCategory() {
        return "Movement";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Instantly teleports player to coordinates.\n\n" +
               "Usage:\n" +
               "  tp <x> <y> <z>          - Absolute coordinates\n" +
               "  tp ~<dx> ~<dy> ~<dz>    - Relative coordinates\n\n" +
               "Parameters:\n" +
               "  x, y, z  - Target coordinates\n" +
               "  ~value   - Relative to current position\n\n" +
               "Details:\n" +
               "  - Instant teleportation (not smooth movement)\n" +
               "  - Works in singleplayer and on servers with cheats\n" +
               "  - May be blocked by anti-cheat on servers\n" +
               "  - Relative coords: ~5 means current + 5\n" +
               "  - ~ alone means current position (no change)\n\n" +
               "Examples:\n" +
               "  tp 100 64 200           // Go to coords\n" +
               "  tp ~ ~10 ~              // Go up 10 blocks\n" +
               "  tp ~100 ~ ~0            // Move 100 blocks in X\n" +
               "  tp 0 100 0              // Go to world spawn area\n\n" +
               "Warning: May trigger anti-cheat on multiplayer servers!\n" +
               "For safe movement, use 'moveTo' or 'pathfind' instead.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || args.length < 3) {
            System.out.println("Использование: tp x y z");
            return;
        }

        try {
            Vec3d currentPos = player.getPos();
            double x = parseCoordinate(args[0], currentPos.x);
            double y = parseCoordinate(args[1], currentPos.y);
            double z = parseCoordinate(args[2], currentPos.z);

            client.execute(() -> {
                player.setPos(x, y, z);
                System.out.println(String.format("Телепортация на %.2f, %.2f, %.2f", x, y, z));
            });

        } catch (NumberFormatException e) {
            System.out.println("Неверный формат координат");
        }
    }

    private double parseCoordinate(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }
}

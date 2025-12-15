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
        return "<x> <y> <z> - absolute or relative (~) coordinates";
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

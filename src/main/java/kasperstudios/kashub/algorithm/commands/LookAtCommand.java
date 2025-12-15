package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

/**
 * Command for rotating player to look at coordinates or entity
 * Syntax: lookAt x y z or lookAt entity [type]
 */
public class LookAtCommand implements Command {

    @Override
    public String getName() {
        return "lookAt";
    }

    @Override
    public String getDescription() {
        return "Rotates player to look at specified coordinates or nearest entity";
    }

    @Override
    public String getParameters() {
        return "<x> <y> <z> | entity [type] [range]";
    }
    
    @Override
    public String getCategory() {
        return "Movement";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Rotates player to look at coordinates or entity.\n\n" +
               "Usage:\n" +
               "  lookAt <x> <y> <z> - Look at coordinates\n" +
               "  lookAt ~ ~5 ~ - Relative coordinates\n" +
               "  lookAt entity - Look at nearest entity\n" +
               "  lookAt entity zombie - Look at nearest zombie\n" +
               "  lookAt entity player 20 - Look at player within 20 blocks\n\n" +
               "Entity types:\n" +
               "  all - Any living entity\n" +
               "  player - Players\n" +
               "  zombie, skeleton, creeper - Hostile mobs\n" +
               "  pig, cow, sheep, chicken - Passive mobs\n" +
               "  villager, trader - NPCs\n" +
               "  Any entity type name (partial match)";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || args.length == 0) return;

        client.execute(() -> {
            if (args[0].equalsIgnoreCase("entity")) {
                String entityType = args.length > 1 ? args[1].toLowerCase() : "all";
                double range = args.length > 2 ? Double.parseDouble(args[2]) : 10.0;
                
                LivingEntity target = findNearestEntity(player, range, entityType);
                if (target != null) {
                    lookAt(player, target.getX(), target.getEyeY(), target.getZ());
                } else {
                    System.out.println("Сущность не найдена");
                }
            } else {
                try {
                    Vec3d currentPos = player.getPos();
                    double x = parseCoordinate(args[0], currentPos.x);
                    double y = args.length > 1 ? parseCoordinate(args[1], currentPos.y) : currentPos.y;
                    double z = args.length > 2 ? parseCoordinate(args[2], currentPos.z) : currentPos.z;
                    
                    lookAt(player, x, y, z);
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат координат");
                }
            }
        });
    }

    private void lookAt(ClientPlayerEntity player, double x, double y, double z) {
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    private double parseCoordinate(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    private LivingEntity findNearestEntity(ClientPlayerEntity player, double range, String type) {
        Box searchBox = player.getBoundingBox().expand(range);
        
        List<LivingEntity> entities = player.getWorld().getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                if (entity == player) return false;
                if (!entity.isAlive()) return false;
                
                if (type.equals("all")) return true;
                
                String entityName = entity.getType().getTranslationKey().toLowerCase();
                return entityName.contains(type);
            }
        );

        return entities.stream()
            .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
            .orElse(null);
    }
}

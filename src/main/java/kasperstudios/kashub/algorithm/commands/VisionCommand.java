package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.api.VisionAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.util.List;

public class VisionCommand implements Command {
    @Override
    public String getName() {
        return "vision";
    }

    @Override
    public String getDescription() {
        return "Vision and raycast utilities";
    }

    @Override
    public String getParameters() {
        return "<target|scan|nearest|count|isLookingAt> [args...]";
    }

    @Override
    public String getCategory() {
        return "Vision";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Vision and entity detection.\n\n" +
               "Usage:\n" +
               "  vision target        - Get crosshair target\n" +
               "  vision nearest <type> <radius>\n" +
               "  vision count <type> <radius>\n" +
               "  vision isLookingAt <block|entity> <id> <dist>\n\n" +
               "Entity types:\n" +
               "  living   - All living entities\n" +
               "  hostile  - Hostile mobs\n" +
               "  passive  - Passive mobs\n" +
               "  player   - Players only\n\n" +
               "Variables set:\n" +
               "  $target_type, $target_x/y/z\n" +
               "  $nearest_found, $nearest_type\n" +
               "  $nearest_x/y/z, $nearest_health\n" +
               "  $mob_count, $vision_result";
    }

    @Override
    public void execute(String[] args) {
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (args.length == 0) {
            interpreter.setVariable("error", "Usage: vision " + getParameters());
            return;
        }

        String subcommand = args[0].toLowerCase();
        VisionAPI vision = VisionAPI.getInstance();

        switch (subcommand) {
            case "target":
                handleTarget(client, player, args, interpreter);
                break;
            case "scan":
                handleScan(player, args, interpreter);
                break;
            case "nearest":
                handleNearest(player, args, interpreter);
                break;
            case "looking":
                handleLooking(client, args, interpreter);
                break;
            case "count":
                handleCount(player, args, interpreter);
                break;
            case "islookingat":
                handleIsLookingAt(client, player, args, interpreter);
                break;
            default:
                interpreter.setVariable("error", "Unknown subcommand: " + subcommand);
        }
    }

    private void handleTarget(MinecraftClient client, ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        HitResult hit = client.crosshairTarget;
        if (hit == null) {
            interpreter.setVariable("target_type", "none");
            return;
        }

        interpreter.setVariable("target_type", hit.getType().toString().toLowerCase());
        interpreter.setVariable("target_x", String.valueOf(hit.getPos().x));
        interpreter.setVariable("target_y", String.valueOf(hit.getPos().y));
        interpreter.setVariable("target_z", String.valueOf(hit.getPos().z));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            String blockId = Registries.BLOCK.getId(
                player.getWorld().getBlockState(blockHit.getBlockPos()).getBlock()
            ).toString();
            interpreter.setVariable("target_block", blockId);
            interpreter.setVariable("target_block_id", blockId);
            interpreter.setVariable("target_block_x", String.valueOf(blockHit.getBlockPos().getX()));
            interpreter.setVariable("target_block_y", String.valueOf(blockHit.getBlockPos().getY()));
            interpreter.setVariable("target_block_z", String.valueOf(blockHit.getBlockPos().getZ()));
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            Entity entity = entityHit.getEntity();
            String entityType = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            interpreter.setVariable("target_entity", entityType);
            interpreter.setVariable("target_entity_type", entityType);
            interpreter.setVariable("target_entity_id", String.valueOf(entity.getId()));
        }
    }

    private void handleScan(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        double radius = args.length > 1 ? Double.parseDouble(args[1]) : 10.0;
        String filter = args.length > 2 ? args[2].toLowerCase() : "all";

        Box box = player.getBoundingBox().expand(radius);
        List<Entity> entities = player.getWorld().getOtherEntities(player, box);

        int count = 0;
        for (Entity entity : entities) {
            if (filter.equals("all") || 
                filter.equals("living") && entity instanceof LivingEntity ||
                Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(filter)) {
                
                interpreter.setVariable("scan_" + count + "_type", 
                    Registries.ENTITY_TYPE.getId(entity.getType()).getPath());
                interpreter.setVariable("scan_" + count + "_id", String.valueOf(entity.getId()));
                interpreter.setVariable("scan_" + count + "_x", String.valueOf(entity.getX()));
                interpreter.setVariable("scan_" + count + "_y", String.valueOf(entity.getY()));
                interpreter.setVariable("scan_" + count + "_z", String.valueOf(entity.getZ()));
                count++;
            }
        }
        interpreter.setVariable("scan_count", String.valueOf(count));
    }

    private void handleNearest(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String type = args.length > 1 ? args[1].toLowerCase() : "living";
        double maxDist = args.length > 2 ? Double.parseDouble(args[2]) : 32.0;

        Box box = player.getBoundingBox().expand(maxDist);
        List<Entity> entities = player.getWorld().getOtherEntities(player, box);

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (type.equals("hostile") && !isHostile(entity)) continue;
            if (type.equals("living") && !(entity instanceof LivingEntity)) continue;
            if (!type.equals("living") && !type.equals("all") && !type.equals("hostile") &&
                !Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(type)) continue;

            double dist = player.squaredDistanceTo(entity);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        if (nearest != null) {
            interpreter.setVariable("nearest_found", "true");
            interpreter.setVariable("nearest_type", Registries.ENTITY_TYPE.getId(nearest.getType()).getPath());
            interpreter.setVariable("nearest_id", String.valueOf(nearest.getId()));
            interpreter.setVariable("nearest_x", String.valueOf(nearest.getX()));
            interpreter.setVariable("nearest_y", String.valueOf(nearest.getY()));
            interpreter.setVariable("nearest_z", String.valueOf(nearest.getZ()));
            interpreter.setVariable("nearest_dist", String.valueOf(Math.sqrt(nearestDist)));
            if (nearest instanceof LivingEntity living) {
                interpreter.setVariable("nearest_health", String.valueOf(living.getHealth()));
            }
        } else {
            interpreter.setVariable("nearest_found", "false");
            interpreter.setVariable("nearest_type", "none");
        }
    }
    
    private void handleCount(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String type = args.length > 1 ? args[1].toLowerCase() : "all";
        double radius = args.length > 2 ? Double.parseDouble(args[2]) : 30.0;

        Box box = player.getBoundingBox().expand(radius);
        List<Entity> entities = player.getWorld().getOtherEntities(player, box);

        int count = 0;
        for (Entity entity : entities) {
            if (type.equals("hostile") && !isHostile(entity)) continue;
            if (type.equals("living") && !(entity instanceof LivingEntity)) continue;
            if (!type.equals("all") && !type.equals("hostile") && !type.equals("living") &&
                !Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(type)) continue;
            count++;
        }
        
        interpreter.setVariable("mob_count", String.valueOf(count));
    }
    
    private void handleIsLookingAt(MinecraftClient client, ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 3) {
            interpreter.setVariable("vision_result", "false");
            return;
        }
        
        String targetType = args[1].toLowerCase(); // "block" or "entity"
        String targetId = args[2].toLowerCase();
        double maxDist = args.length > 3 ? Double.parseDouble(args[3]) : 5.0;
        
        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getPos().distanceTo(player.getPos()) > maxDist) {
            interpreter.setVariable("vision_result", "false");
            return;
        }
        
        boolean result = false;
        
        if (targetType.equals("block") && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            String blockId = Registries.BLOCK.getId(
                player.getWorld().getBlockState(blockHit.getBlockPos()).getBlock()
            ).toString();
            result = blockId.contains(targetId);
        } else if (targetType.equals("entity") && hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            String entityId = Registries.ENTITY_TYPE.getId(entityHit.getEntity().getType()).toString();
            result = entityId.contains(targetId);
        }
        
        interpreter.setVariable("vision_result", String.valueOf(result));
    }
    
    private boolean isHostile(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        String type = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        return type.contains("zombie") || type.contains("skeleton") || type.contains("creeper") ||
               type.contains("spider") || type.contains("enderman") || type.contains("witch") ||
               type.contains("slime") || type.contains("phantom") || type.contains("drowned") ||
               type.contains("husk") || type.contains("stray") || type.contains("blaze") ||
               type.contains("ghast") || type.contains("magma_cube") || type.contains("piglin") ||
               type.contains("hoglin") || type.contains("warden") || type.contains("vindicator") ||
               type.contains("pillager") || type.contains("ravager") || type.contains("evoker");
    }

    private void handleLooking(MinecraftClient client, String[] args, ScriptInterpreter interpreter) {
        HitResult hit = client.crosshairTarget;
        if (hit == null) {
            interpreter.setVariable("looking_at", "false");
            return;
        }

        String target = args.length > 1 ? args[1].toLowerCase() : "";
        boolean looking = false;

        if (hit.getType() == HitResult.Type.BLOCK && !target.isEmpty()) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            String blockId = Registries.BLOCK.getId(
                client.player.getWorld().getBlockState(blockHit.getBlockPos()).getBlock()
            ).getPath();
            looking = blockId.contains(target);
        } else if (hit.getType() == HitResult.Type.ENTITY && !target.isEmpty()) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            String entityId = Registries.ENTITY_TYPE.getId(entityHit.getEntity().getType()).getPath();
            looking = entityId.contains(target);
        } else {
            looking = hit.getType() != HitResult.Type.MISS;
        }

        interpreter.setVariable("looking_at", String.valueOf(looking));
    }
}

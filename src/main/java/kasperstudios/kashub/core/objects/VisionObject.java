package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VisionObject - OO wrapper for vision and raycast utilities.
 * Usage: vision.getTarget(), vision.getNearest("hostile")
 */
public class VisionObject extends Value {

    public VisionObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("getTarget", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            HitResult hit = client.crosshairTarget;

            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return Value.NULL;
            }

            Map<String, Value> result = new HashMap<>();
            result.put("type", Value.of(hit.getType().toString().toLowerCase()));
            result.put("x", Value.of(hit.getPos().x));
            result.put("y", Value.of(hit.getPos().y));
            result.put("z", Value.of(hit.getPos().z));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                result.put("blockId", Value.of(Registries.BLOCK
                        .getId(client.world.getBlockState(blockHit.getBlockPos()).getBlock()).toString()));
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                result.put("entityId", Value.of(Registries.ENTITY_TYPE.getId(entity.getType()).toString()));
                result.put("id", Value.of(entity.getId()));
            }

            return Value.of(result);
        }));

        methods.put("getNearest", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : "living";
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 32.0;
            String targetPart = args.size() > 2 ? args.get(2).asString().toLowerCase() : "body";

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.NULL;

            Box box = player.getBoundingBox().expand(maxDist);
            List<Entity> entities = player.getWorld().getOtherEntities(player, box);

            Entity nearest = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {
                if (type.equals("hostile") && !isHostile(entity))
                    continue;
                if (type.equals("living") && !(entity instanceof LivingEntity))
                    continue;
                if (!type.equals("living") && !type.equals("all") && !type.equals("hostile") &&
                        !Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(type))
                    continue;

                double distSq = player.squaredDistanceTo(entity);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = entity;
                }
            }

            if (nearest == null)
                return Value.NULL;

            // Calculate target position based on targetPart
            double targetX = nearest.getX();
            double targetY = nearest.getY();
            double targetZ = nearest.getZ();
            
            if (targetPart.equals("head")) {
                targetY = nearest.getEyeY();
            } else if (targetPart.equals("body")) {
                targetY = nearest.getY() + nearest.getHeight() * 0.5;
            } else if (targetPart.equals("legs")) {
                targetY = nearest.getY() + nearest.getHeight() * 0.2;
            }

            Map<String, Value> res = new HashMap<>();
            res.put("type", Value.of(Registries.ENTITY_TYPE.getId(nearest.getType()).getPath()));
            res.put("id", Value.of(nearest.getId()));
            
            // Add pos object with target part coordinates
            Map<String, Value> pos = new HashMap<>();
            pos.put("x", Value.of(targetX));
            pos.put("y", Value.of(targetY));
            pos.put("z", Value.of(targetZ));
            res.put("pos", Value.of(pos));
            
            // Also add direct coordinates
            res.put("x", Value.of(targetX));
            res.put("y", Value.of(targetY));
            res.put("z", Value.of(targetZ));
            res.put("distance", Value.of(Math.sqrt(nearestDistSq)));
            res.put("dist", Value.of(Math.sqrt(nearestDistSq))); // Alias
            if (nearest instanceof LivingEntity) {
                res.put("health", Value.of(((LivingEntity) nearest).getHealth()));
            }
            
            // Store entity reference for attack
            res.put("_entity", Value.of(nearest));
            
            return Value.of(res);
        }));
        
        // Add alias "nearest" for compatibility
        methods.put("nearest", methods.get("getNearest"));

        methods.put("isLookingAt", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            String targetId = args.get(0).asString();
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 5.0;

            MinecraftClient client = MinecraftClient.getInstance();
            HitResult hit = client.crosshairTarget;
            if (hit == null || hit.getType() == HitResult.Type.MISS)
                return Value.FALSE;
            if (hit.getPos().distanceTo(client.player.getPos()) > maxDist)
                return Value.FALSE;

            String currentId = "";
            if (hit.getType() == HitResult.Type.BLOCK) {
                currentId = Registries.BLOCK
                        .getId(client.world.getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock()).toString();
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                currentId = Registries.ENTITY_TYPE.getId(((EntityHitResult) hit).getEntity().getType()).toString();
            }

            return Value.of(currentId.contains(targetId));
        }));

        methods.put("count", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : "living";
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 32.0;

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.of(0);

            Box box = player.getBoundingBox().expand(maxDist);
            List<Entity> entities = player.getWorld().getOtherEntities(player, box);

            int count = 0;
            for (Entity entity : entities) {
                if (type.equals("hostile") && !isHostile(entity))
                    continue;
                if (type.equals("living") && !(entity instanceof LivingEntity))
                    continue;
                if (!type.equals("living") && !type.equals("all") && !type.equals("hostile") &&
                        !Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(type))
                    continue;
                count++;
            }

            return Value.of(count);
        }));

        methods.put("canSee", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            String type = args.get(0).asString();
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 20.0;

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.FALSE;

            Box box = player.getBoundingBox().expand(maxDist);
            List<Entity> entities = player.getWorld().getOtherEntities(player, box);

            for (Entity entity : entities) {
                if (!Registries.ENTITY_TYPE.getId(entity.getType()).getPath().contains(type))
                    continue;
                
                // Simple distance check (can be enhanced with raycast later)
                double dist = player.distanceTo(entity);
                if (dist <= maxDist) {
                    return Value.TRUE;
                }
            }

            return Value.FALSE;
        }));

        return methods;
    }

    private static boolean isHostile(Entity entity) {
        if (!(entity instanceof LivingEntity))
            return false;
        String type = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        return type.contains("zombie") || type.contains("skeleton") || type.contains("creeper") ||
                type.contains("spider") || type.contains("enderman") || type.contains("witch") ||
                type.contains("slime") || type.contains("phantom") || type.contains("drowned") ||
                type.contains("husk") || type.contains("stray") || type.contains("blaze") ||
                type.contains("ghast") || type.contains("magma_cube") || type.contains("piglin") ||
                type.contains("hoglin") || type.contains("warden") || type.contains("vindicator") ||
                type.contains("pillager") || type.contains("ravager") || type.contains("evoker");
    }
}

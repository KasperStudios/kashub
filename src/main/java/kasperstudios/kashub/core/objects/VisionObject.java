package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import kasperstudios.kashub.core.Callable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VisionObject - OO wrapper for vision and raycast utilities.
 * Usage: vision.getTarget(), vision.getNearest("hostile")
 */
public class VisionObject extends Value implements Documentable {

    private static final String TYPE_LIVING = "living";
    private static final String TYPE_HOSTILE = "hostile";

    public VisionObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("getTarget", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            HitResult hit = client.hitResult;

            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return Value.NULL;
            }

            Map<String, Value> result = new HashMap<>();
            result.put("type", Value.of(hit.getType().toString().toLowerCase()));
            result.put("x", Value.of(hit.getLocation().x));
            result.put("y", Value.of(hit.getLocation().y));
            result.put("z", Value.of(hit.getLocation().z));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                result.put("blockId", Value.of(BuiltInRegistries.BLOCK
                        .getKey(client.level.getBlockState(blockHit.getBlockPos()).getBlock()).toString()));
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                Entity entity = entityHit.getEntity();
                result.put("entityId", Value.of(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
                result.put("id", Value.of(entity.getId()));
            }

            return Value.of(result);
        }));

        methods.put("getNearest", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : TYPE_LIVING;
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 32.0;
            String targetPart = args.size() > 2 ? args.get(2).asString().toLowerCase() : "body";

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.NULL;

            AABB box = player.getBoundingBox().inflate(maxDist);
            List<Entity> entities = player.level().getEntities(player, box);

            Entity nearest = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {
                if (type.equals(TYPE_HOSTILE) && !isHostile(entity))
                    continue;
                if (type.equals(TYPE_LIVING) && !(entity instanceof LivingEntity))
                    continue;
                if (!type.equals(TYPE_LIVING) && !type.equals("all") && !type.equals(TYPE_HOSTILE) &&
                        !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().contains(type))
                    continue;

                double distSq = player.distanceToSqr(entity);
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
                targetY = nearest.getY() + nearest.getBbHeight() * 0.5;
            } else if (targetPart.equals("legs")) {
                targetY = nearest.getY() + nearest.getBbHeight() * 0.2;
            }

            Map<String, Value> res = new HashMap<>();
            res.put("type", Value.of(BuiltInRegistries.ENTITY_TYPE.getKey(nearest.getType()).getPath()));
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

            Minecraft client = Minecraft.getInstance();
            HitResult hit = client.hitResult;
            if (hit == null || hit.getType() == HitResult.Type.MISS)
                return Value.FALSE;
            if (hit.getLocation().distanceTo(client.player.position()) > maxDist)
                return Value.FALSE;

            String currentId = "";
            if (hit.getType() == HitResult.Type.BLOCK) {
                currentId = BuiltInRegistries.BLOCK
                        .getKey(client.level.getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock()).toString();
            } else if (hit.getType() == HitResult.Type.ENTITY) {
                currentId = BuiltInRegistries.ENTITY_TYPE.getKey(((EntityHitResult) hit).getEntity().getType()).toString();
            }

            return Value.of(currentId.contains(targetId));
        }));

        methods.put("count", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : TYPE_LIVING;
            double maxDist = args.size() > 1 ? args.get(1).asDouble() : 32.0;

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.of(0);

            AABB box = player.getBoundingBox().inflate(maxDist);
            List<Entity> entities = player.level().getEntities(player, box);

            int count = 0;
            for (Entity entity : entities) {
                if (type.equals(TYPE_HOSTILE) && !isHostile(entity))
                    continue;
                if (type.equals(TYPE_LIVING) && !(entity instanceof LivingEntity))
                    continue;
                if (!type.equals(TYPE_LIVING) && !type.equals("all") && !type.equals(TYPE_HOSTILE) &&
                        !BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().contains(type))
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

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.FALSE;

            AABB box = player.getBoundingBox().inflate(maxDist);
            List<Entity> entities = player.level().getEntities(player, box);

            for (Entity entity : entities) {
                if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().contains(type))
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
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return type.contains("zombie") || type.contains("skeleton") || type.contains("creeper") ||
                type.contains("spider") || type.contains("enderman") || type.contains("witch") ||
                type.contains("slime") || type.contains("phantom") || type.contains("drowned") ||
                type.contains("husk") || type.contains("stray") || type.contains("blaze") ||
                type.contains("ghast") || type.contains("magma_cube") || type.contains("piglin") ||
                type.contains("hoglin") || type.contains("warden") || type.contains("vindicator") ||
                type.contains("pillager") || type.contains("ravager") || type.contains("evoker");
    }

    @Override
    public String getObjectName() {
        return "vision";
    }

    @Override
    public String getObjectDescription() {
        return "OO wrapper for vision and raycast utilities to query the player's line of sight and surroundings.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("getTarget()", "Returns a map containing details about the block or entity the player is currently looking at.");
        docs.put("getNearest(type, [maxDist], [targetPart])", "Finds the nearest entity matching 'type' ('living', 'hostile', 'all', or entity ID) within 'maxDist'. Returns a map with position details relative to 'targetPart' ('head', 'body', 'legs').");
        docs.put("nearest(type, ...)", "Alias for getNearest.");
        docs.put("isLookingAt(targetId, [maxDist])", "Returns true if the player is directly looking at a block or entity whose ID contains 'targetId' within 'maxDist'.");
        docs.put("count(type, [maxDist])", "Returns the number of entities matching 'type' within 'maxDist'.");
        docs.put("canSee(type, [maxDist])", "Returns true if an entity whose ID contains 'type' is within 'maxDist'.");
        return docs;
    }
}

package kasperstudios.kashub.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.stream.Collectors;

public class VisionAPI {
    private static VisionAPI instance;

    private VisionAPI() {}

    public static VisionAPI getInstance() {
        if (instance == null) {
            instance = new VisionAPI();
        }
        return instance;
    }

    public static class RaycastResult {
        public final HitResult.Type type;
        public final BlockPos blockPos;
        public final String blockId;
        public final Entity entity;
        public final String entityType;
        public final Vec3d hitPos;
        public final double distance;

        public RaycastResult(HitResult.Type type, BlockPos blockPos, String blockId, 
                           Entity entity, String entityType, Vec3d hitPos, double distance) {
            this.type = type;
            this.blockPos = blockPos;
            this.blockId = blockId;
            this.entity = entity;
            this.entityType = entityType;
            this.hitPos = hitPos;
            this.distance = distance;
        }

        public boolean isBlock() { return type == HitResult.Type.BLOCK; }
        public boolean isEntity() { return type == HitResult.Type.ENTITY; }
        public boolean isMiss() { return type == HitResult.Type.MISS; }
    }

    public static class EntityInfo {
        public final Entity entity;
        public final String type;
        public final String name;
        public final double distance;
        public final Vec3d position;
        public final float health;
        public final float maxHealth;

        public EntityInfo(Entity entity, double distance) {
            this.entity = entity;
            this.type = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            this.name = entity.getName().getString();
            this.distance = distance;
            this.position = entity.getPos();
            
            if (entity instanceof LivingEntity living) {
                this.health = living.getHealth();
                this.maxHealth = living.getMaxHealth();
            } else {
                this.health = 0;
                this.maxHealth = 0;
            }
        }
    }

    public RaycastResult raycast(double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return new RaycastResult(HitResult.Type.MISS, null, null, null, null, null, 0);
        }

        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        // Block raycast
        RaycastContext context = new RaycastContext(
            start, end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        );
        BlockHitResult blockHit = client.world.raycast(context);

        // Entity raycast
        Box searchBox = player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0);
        Entity closestEntity = null;
        double closestDistance = maxDistance;

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> hitOpt = entityBox.raycast(start, end);
            
            if (hitOpt.isPresent()) {
                double dist = start.distanceTo(hitOpt.get());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestEntity = entity;
                }
            }
        }

        // Determine what was hit first
        double blockDistance = blockHit.getType() != HitResult.Type.MISS 
            ? start.distanceTo(blockHit.getPos()) : Double.MAX_VALUE;

        if (closestEntity != null && closestDistance < blockDistance) {
            String entityType = Registries.ENTITY_TYPE.getId(closestEntity.getType()).toString();
            return new RaycastResult(
                HitResult.Type.ENTITY,
                null, null,
                closestEntity, entityType,
                closestEntity.getPos(),
                closestDistance
            );
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            return new RaycastResult(
                HitResult.Type.BLOCK,
                blockHit.getBlockPos(), blockId,
                null, null,
                blockHit.getPos(),
                blockDistance
            );
        }

        return new RaycastResult(HitResult.Type.MISS, null, null, null, null, end, maxDistance);
    }

    public List<EntityInfo> scanEntities(double maxDistance, double coneAngle) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return Collections.emptyList();
        }

        Vec3d playerPos = player.getPos();
        Vec3d lookDir = player.getRotationVec(1.0f);
        double cosAngle = Math.cos(Math.toRadians(coneAngle / 2));

        List<EntityInfo> results = new ArrayList<>();
        Box searchBox = player.getBoundingBox().expand(maxDistance);

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            Vec3d entityPos = entity.getPos();
            double distance = playerPos.distanceTo(entityPos);
            
            if (distance > maxDistance) continue;

            Vec3d toEntity = entityPos.subtract(playerPos).normalize();
            double dot = lookDir.dotProduct(toEntity);
            
            if (dot >= cosAngle) {
                results.add(new EntityInfo(entity, distance));
            }
        }

        results.sort(Comparator.comparingDouble(e -> e.distance));
        return results;
    }

    public EntityInfo findNearest(String entityType, double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return null;
        }

        Vec3d playerPos = player.getPos();
        Box searchBox = player.getBoundingBox().expand(maxDistance);
        
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            if (!matchesType(entity, entityType)) continue;
            
            double dist = playerPos.distanceTo(entity.getPos());
            if (dist < nearestDist && dist <= maxDistance) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest != null ? new EntityInfo(nearest, nearestDist) : null;
    }

    public int countEntities(String entityType, double maxDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return 0;
        }

        Vec3d playerPos = player.getPos();
        Box searchBox = player.getBoundingBox().expand(maxDistance);
        
        int count = 0;
        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            if (matchesType(entity, entityType)) {
                double dist = playerPos.distanceTo(entity.getPos());
                if (dist <= maxDistance) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isLookingAt(String targetType, String targetId, double maxDistance) {
        RaycastResult result = raycast(maxDistance);
        
        if ("block".equalsIgnoreCase(targetType)) {
            return result.isBlock() && (targetId == null || result.blockId.contains(targetId));
        } else if ("entity".equalsIgnoreCase(targetType)) {
            return result.isEntity() && (targetId == null || result.entityType.contains(targetId));
        }
        
        return false;
    }

    private boolean matchesType(Entity entity, String type) {
        if (type == null || type.isEmpty() || type.equals("*") || type.equalsIgnoreCase("all")) {
            return true;
        }
        
        String lowerType = type.toLowerCase();
        
        return switch (lowerType) {
            case "hostile", "monster", "mob" -> entity instanceof HostileEntity;
            case "passive", "animal" -> entity instanceof AnimalEntity;
            case "player" -> entity instanceof PlayerEntity;
            case "living" -> entity instanceof LivingEntity;
            default -> {
                String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
                yield entityId.contains(lowerType);
            }
        };
    }
}

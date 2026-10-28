package kasperstudios.kashub.api;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        public final Vec3 hitPos;
        public final double distance;

        public RaycastResult(HitResult.Type type, BlockPos blockPos, String blockId,
                           Entity entity, String entityType, Vec3 hitPos, double distance) {
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
        public final Vec3 position;
        public final float health;
        public final float maxHealth;

        public EntityInfo(Entity entity, double distance) {
            this.entity = entity;
            this.type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            this.name = entity.getName().getString();
            this.distance = distance;
            this.position = entity.position();

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
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return new RaycastResult(HitResult.Type.MISS, null, null, null, null, null, 0);
        }

        Vec3 start = player.getEyePosition(1.0f);
        Vec3 direction = player.getViewVector(1.0f);
        Vec3 end = start.add(direction.scale(maxDistance));

        ClipContext context = new ClipContext(
            start, end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        );
        BlockHitResult blockHit = client.level.clip(context);

        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(maxDistance)).inflate(1.0);
        Entity closestEntity = null;
        double closestDistance = maxDistance;

        for (Entity entity : client.level.getEntities(player, searchBox)) {
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hitOpt = entityBox.clip(start, end);

            if (hitOpt.isPresent()) {
                double dist = start.distanceTo(hitOpt.get());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestEntity = entity;
                }
            }
        }

        double blockDistance = blockHit.getType() != HitResult.Type.MISS
            ? start.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;

        if (closestEntity != null && closestDistance < blockDistance) {
            String entityType = BuiltInRegistries.ENTITY_TYPE.getKey(closestEntity.getType()).toString();
            return new RaycastResult(
                HitResult.Type.ENTITY,
                null, null,
                closestEntity, entityType,
                closestEntity.position(),
                closestDistance
            );
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.level.getBlockState(blockHit.getBlockPos());
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return new RaycastResult(
                HitResult.Type.BLOCK,
                blockHit.getBlockPos(), blockId,
                null, null,
                blockHit.getLocation(),
                blockDistance
            );
        }

        return new RaycastResult(HitResult.Type.MISS, null, null, null, null, end, maxDistance);
    }

    public List<EntityInfo> scanEntities(double maxDistance, double coneAngle) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return Collections.emptyList();
        }

        Vec3 playerPos = player.position();
        Vec3 lookDir = player.getViewVector(1.0f);
        double cosAngle = Math.cos(Math.toRadians(coneAngle / 2));

        List<EntityInfo> results = new ArrayList<>();
        AABB searchBox = player.getBoundingBox().inflate(maxDistance);

        for (Entity entity : client.level.getEntities(player, searchBox)) {
            Vec3 entityPos = entity.position();
            double distance = playerPos.distanceTo(entityPos);

            if (distance > maxDistance) continue;

            Vec3 toEntity = entityPos.subtract(playerPos).normalize();
            double dot = lookDir.dot(toEntity);

            if (dot >= cosAngle) {
                results.add(new EntityInfo(entity, distance));
            }
        }

        results.sort(Comparator.comparingDouble(e -> e.distance));
        return results;
    }

    public EntityInfo findNearest(String entityType, double maxDistance) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return null;
        }

        Vec3 playerPos = player.position();
        AABB searchBox = player.getBoundingBox().inflate(maxDistance);

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : client.level.getEntities(player, searchBox)) {
            if (!matchesType(entity, entityType)) continue;

            double dist = playerPos.distanceTo(entity.position());
            if (dist < nearestDist && dist <= maxDistance) {
                nearestDist = dist;
                nearest = entity;
            }
        }

        return nearest != null ? new EntityInfo(nearest, nearestDist) : null;
    }

    public int countEntities(String entityType, double maxDistance) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return 0;
        }

        Vec3 playerPos = player.position();
        AABB searchBox = player.getBoundingBox().inflate(maxDistance);

        int count = 0;
        for (Entity entity : client.level.getEntities(player, searchBox)) {
            if (matchesType(entity, entityType)) {
                double dist = playerPos.distanceTo(entity.position());
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
            case "hostile", "monster", "mob" -> entity instanceof Monster;
            case "passive", "animal" -> entity instanceof Animal;
            case "player" -> entity instanceof Player;
            case "living" -> entity instanceof LivingEntity;
            default -> {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                yield entityId.contains(lowerType);
            }
        };
    }
}

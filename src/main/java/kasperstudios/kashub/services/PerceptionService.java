package kasperstudios.kashub.services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * PerceptionService - High-performance ASCII sensory perception for AI agents.
 * Provides 2D rotated heading radar and 3D first-person raymarching vision.
 */
public class PerceptionService {
    private static final PerceptionService INSTANCE = new PerceptionService();

    public static PerceptionService getInstance() {
        return INSTANCE;
    }

    private PerceptionService() {}

    /**
     * 2D Dynamically Rotated Heading Radar around the player.
     * Rotates grid so UP is always the direction player is facing.
     */
    public String render2DRadar(int radius) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) return "World or Player not loaded";

        radius = Math.max(3, Math.min(24, radius));
        Vec3 botPos = player.position();
        Vec3 eye = player.getEyePosition();
        float botYaw = player.getYRot();
        double botYawRad = Math.toRadians(-botYaw);

        double cos = Math.cos(botYawRad);
        double sin = Math.sin(botYawRad);

        int size = radius * 2 + 1;
        char[][] grid = new char[size][size];
        for (char[] row : grid) Arrays.fill(row, ' ');

        List<Entity> nearbyEntities = level.getEntities(player, player.getBoundingBox().inflate(radius * 1.5));
        List<String> entityListLegend = new ArrayList<>();

        for (int lz = -radius; lz <= radius; lz++) {
            for (int lx = -radius; lx <= radius; lx++) {
                if (lx * lx + lz * lz > radius * radius) continue;

                // Local coords (lx = right, lz = forward/UP) -> world coords
                double wx = botPos.x + (lx * cos - lz * sin);
                double wz = botPos.z + (lx * sin + lz * cos);

                int gridRow = radius - lz; // lz forward is top row
                int gridCol = radius + lx;

                if (lx == 0 && lz == 0) {
                    grid[gridRow][gridCol] = '▲'; // Player heading marker
                    continue;
                }

                BlockPos groundPos = new BlockPos((int) Math.floor(wx), (int) Math.floor(botPos.y), (int) Math.floor(wz));
                BlockState state = level.getBlockState(groundPos);
                BlockState stateBelow = level.getBlockState(groundPos.below());
                BlockState stateAbove = level.getBlockState(groundPos.above());

                // Line-of-sight raycast check
                Vec3 targetVec = new Vec3(wx + 0.5, groundPos.getY() + 0.5, wz + 0.5);
                BlockHitResult hit = level.clip(new ClipContext(eye, targetVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                boolean hasLOS = (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(targetVec) < 1.2);

                if (!hasLOS) {
                    grid[gridRow][gridCol] = '?'; // Line of sight blocked (fog of war)
                } else if (!state.getCollisionShape(level, groundPos).isEmpty() || !stateAbove.getCollisionShape(level, groundPos.above()).isEmpty()) {
                    grid[gridRow][gridCol] = '#'; // Solid wall / obstacle
                } else if (state.getFluidState().is(FluidTags.WATER) || stateBelow.getFluidState().is(FluidTags.WATER)) {
                    grid[gridRow][gridCol] = '~'; // Water
                } else if (state.getFluidState().is(FluidTags.LAVA) || stateBelow.getFluidState().is(FluidTags.LAVA)) {
                    grid[gridRow][gridCol] = '!'; // Lava hazard
                } else {
                    grid[gridRow][gridCol] = '.'; // Walkable surface
                }
            }
        }

        // Overlay entities with 8-way gaze arrows
        for (Entity e : nearbyEntities) {
            if (e == player || !e.isAlive()) continue;
            double dx = e.getX() - botPos.x;
            double dz = e.getZ() - botPos.z;

            // Rotate into camera space
            double lx = dx * cos + dz * sin;
            double lz = -dx * sin + dz * cos;

            int gridRow = (int) Math.round(radius - lz);
            int gridCol = (int) Math.round(radius + lx);

            if (gridRow >= 0 && gridRow < size && gridCol >= 0 && gridCol < size) {
                float relYaw = normAngle(e.getYRot() - botYaw);
                char arrow = getGazeArrow(relYaw);
                char mark = (e instanceof Player) ? '@' : (e instanceof Monster ? 'M' : 'E');
                grid[gridRow][gridCol] = mark;

                double dist = player.distanceTo(e);
                entityListLegend.add(String.format("[%c%c] %s (%.1fm | %s | HP: %.0f)",
                        mark, arrow, e.getName().getString(), dist,
                        (e.getY() > botPos.y + 1 ? "above" : e.getY() < botPos.y - 1 ? "below" : "level"),
                        (e instanceof LivingEntity le ? le.getHealth() : 0.0)));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== RADAR [Facing: ↑ FORWARD | Pos: %.1f, %.1f, %.1f | Radius: %dm] ===\n", botPos.x, botPos.y, botPos.z, radius));
        sb.append("                  [ ВПЕРЁД (Взгляд) ]\n");

        for (int r = 0; r < size; r++) {
            sb.append("         ");
            for (int c = 0; c < size; c++) {
                sb.append(grid[r][c]).append(" ");
            }
            sb.append("\n");
        }
        sb.append("                  [ СЗАДИ (Спина) ]\n");
        if (!entityListLegend.isEmpty()) {
            sb.append("--- СУЩНОСТИ РЯДОМ ---\n");
            for (String item : entityListLegend) {
                sb.append(item).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 3D First-Person ASCII Raymarcher
     */
    public String render3DViewAscii(int width, int height, float fovDeg) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) return "World or Player not loaded";

        width = Math.max(16, Math.min(64, width));
        height = Math.max(8, Math.min(32, height));
        fovDeg = Math.max(30.0f, Math.min(110.0f, fovDeg));

        Vec3 eye = player.getEyePosition();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Basis vectors
        Vec3 forward = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad)).normalize();
        Vec3 right = new Vec3(Math.cos(yawRad), 0, Math.sin(yawRad)).normalize();
        Vec3 up = right.cross(forward).normalize();

        double aspect = (double) width / height * 0.55; // Character aspect ratio correction
        double halfFovTan = Math.tan(Math.toRadians(fovDeg * 0.5));

        List<Entity> nearbyEntities = level.getEntities(player, player.getBoundingBox().inflate(24.0));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== 3D VIEW [Pitch: %.1f° | Yaw: %.1f° | Pos: %.1f, %.1f, %.1f] ===\n", pitch, yaw, eye.x, eye.y, eye.z));

        int midX = width / 2;
        int midY = height / 2;
        String crosshairHit = "Air / Sky";

        for (int y = 0; y < height; y++) {
            sb.append("|");
            double screenY = (1.0 - (2.0 * y) / (height - 1)) * halfFovTan;

            for (int x = 0; x < width; x++) {
                double screenX = ((2.0 * x) / (width - 1) - 1.0) * halfFovTan * aspect;

                Vec3 rayDir = forward.add(right.scale(screenX)).add(up.scale(screenY)).normalize();
                Vec3 rayEnd = eye.add(rayDir.scale(28.0));

                // 1. Entity intersection
                Entity hitEntity = null;
                double entityDist = Double.MAX_VALUE;
                for (Entity e : nearbyEntities) {
                    if (e == player || !e.isAlive()) continue;
                    AABB box = e.getBoundingBox().inflate(0.15);
                    Optional<Vec3> hitOpt = box.clip(eye, rayEnd);
                    if (hitOpt.isPresent()) {
                        double d = eye.distanceTo(hitOpt.get());
                        if (d < entityDist) {
                            entityDist = d;
                            hitEntity = e;
                        }
                    }
                }

                // 2. Block raycast
                BlockHitResult blockHit = level.clip(new ClipContext(eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
                double blockDist = (blockHit.getType() != HitResult.Type.MISS) ? eye.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;

                char c;
                if (hitEntity != null && entityDist < blockDist) {
                    if (hitEntity instanceof Player) c = '@';
                    else if (hitEntity instanceof Monster) c = 'M';
                    else c = 'E';

                    if (x == midX && y == midY) {
                        crosshairHit = hitEntity.getName().getString() + " (" + String.format("%.1fm", entityDist) + ")";
                    }
                } else if (blockDist < Double.MAX_VALUE) {
                    BlockPos bp = blockHit.getBlockPos();
                    BlockState bs = level.getBlockState(bp);

                    if (bs.getFluidState().is(FluidTags.WATER)) c = '~';
                    else if (bs.getFluidState().is(FluidTags.LAVA)) c = '!';
                    else {
                        // Depth shading characters
                        if (blockDist < 3.0) c = '█';
                        else if (blockDist < 6.0) c = '#';
                        else if (blockDist < 11.0) c = ':';
                        else if (blockDist < 18.0) c = '.';
                        else c = ' ';
                    }

                    if (x == midX && y == midY) {
                        crosshairHit = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString() +
                                " at " + bp.getX() + "," + bp.getY() + "," + bp.getZ() + " (" + String.format("%.1fm", blockDist) + ")";
                    }
                } else {
                    c = (screenY > 0) ? ' ' : '.'; // Horizon separation
                }

                // Crosshair overlay in center
                if (x == midX && y == midY && c != '@' && c != 'M') {
                    c = '+';
                }

                sb.append(c);
            }
            sb.append("|\n");
        }

        sb.append("Crosshair target: ").append(crosshairHit).append("\n");
        return sb.toString();
    }

    private static float normAngle(float a) {
        while (a > 180.0f) a -= 360.0f;
        while (a <= -180.0f) a += 360.0f;
        return a;
    }

    private static char getGazeArrow(float relAngle) {
        if (relAngle >= -22.5f && relAngle < 22.5f) return '↑';
        if (relAngle >= 22.5f && relAngle < 67.5f) return '↗';
        if (relAngle >= 67.5f && relAngle < 112.5f) return '→';
        if (relAngle >= 112.5f && relAngle < 157.5f) return '↘';
        if (relAngle >= 157.5f || relAngle < -157.5f) return '↓';
        if (relAngle >= -157.5f && relAngle < -112.5f) return '↙';
        if (relAngle >= -112.5f && relAngle < -67.5f) return '←';
        return '↖';
    }
}

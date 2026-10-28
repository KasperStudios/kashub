package kasperstudios.kashub.services;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

/**
 * PathfindingService - A* pathfinding with obstacle avoidance.
 * Integrated into player.moveTo() for automatic navigation.
 */
public class PathfindingService {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathfinding");

    private static PathfindingService INSTANCE;

    // Pathfinding state
    private volatile boolean isActive = false;
    private volatile List<BlockPos> currentPath = null;
    private volatile int pathIndex = 0;
    private volatile BlockPos targetPos = null;
    private volatile float targetRadius = 1.0f;
    private volatile long currentRequestId = 0;

    // Stuck detection
    private Vec3 lastPlayerPos = null;
    private int stuckTicks = 0;
    private static final int STUCK_THRESHOLD = 40; // 2 seconds

    // Configuration
    private boolean avoidDanger = true;
    private boolean allowParkour = true;
    private int maxFallDistance = 5;
    private boolean useSprint = true;
    private boolean allowSwim = true;
    private int maxIterations = 8000;

    // Path cache
    private final Map<PathCacheKey, CachedPath> pathCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 50;
    private static final long CACHE_EXPIRY_MS = 30000;

    // Dangerous blocks set
    private Set<net.minecraft.world.level.block.Block> dangerousBlocks = null;

    public static PathfindingService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathfindingService();
        }
        return INSTANCE;
    }

    private Set<net.minecraft.world.level.block.Block> getDangerousBlocks() {
        if (dangerousBlocks == null) {
            try {
                dangerousBlocks = Set.of(
                        Blocks.LAVA,
                        Blocks.FIRE,
                        Blocks.SOUL_FIRE,
                        Blocks.CACTUS,
                        Blocks.SWEET_BERRY_BUSH,
                        Blocks.WITHER_ROSE,
                        Blocks.MAGMA_BLOCK,
                        Blocks.CAMPFIRE,
                        Blocks.SOUL_CAMPFIRE,
                        Blocks.POWDER_SNOW);
            } catch (Throwable t) {
                dangerousBlocks = Collections.emptySet();
            }
        }
        return dangerousBlocks;
    }

    /**
     * Start pathfinding to target coordinates.
     * This is the main entry point called by player.moveTo().
     */
    public void navigateTo(double x, double y, double z, float radius) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null)
            return;

        BlockPos newEndPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        
        // If we are already navigating OR CALCULATING for this exact block, don't reset!
        if (targetPos != null && targetPos.equals(newEndPos)) {
            return;
        }

        isActive = false;
        currentPath = null;
        ScriptLogger.getInstance().info("[Pathfinding] Navigating to: " + x + ", " + y + ", " + z);

        long requestId = ++currentRequestId;
        targetPos = newEndPos;
        targetRadius = radius;
        int maxIterations = 10000;

        Level world = player.level();

        // Target Normalization: If target is air or liquid, try to find the floor below
        // (unless flying/creative)
        BlockState targetState = world.getBlockState(targetPos);
        if ((targetState.isAir() || !targetState.getFluidState().isEmpty()) && !player.isCreative()
                && !player.getAbilities().flying) {
            BlockPos floor = targetPos;
            int scanLimit = 16; // Reduced scan limit for better performance and to avoid over-normalization
            while (scanLimit > 0 && floor.getY() > world.getMinBuildHeight()) {
                BlockState s = world.getBlockState(floor);
                if (!s.isAir() && s.getFluidState().isEmpty()) {
                    targetPos = floor.above();
                    break;
                }
                floor = floor.below();
                scanLimit--;
            }
        }

        // If target is still solid (e.g., standing INSIDE a block), try to stand ON it
        if (world.getBlockState(targetPos).isRedstoneConductor(world, targetPos)) {
            BlockPos up = targetPos.above();
            if (!world.getBlockState(up).isRedstoneConductor(world, up)) {
                targetPos = up;
            }
        }

        BlockPos start = player.blockPosition();

        // Check if already at target
        double tx = x + 0.5;
        double ty = y;
        double tz = z + 0.5;
        double distSq = player.position().distanceToSqr(tx, ty, tz);
        
        ScriptLogger.getInstance().info("[Pathfinding] Start -> From: " + player.blockPosition() + " To: " + tx + "," + ty + "," + tz + " DistSq: " + String.format("%.4f", distSq));

        if (distSq <= 0.1 * 0.1) {
            ScriptLogger.getInstance().info("[Pathfinding] Already at target.");
            return;
        }

        // Check cache first
        PathCacheKey cacheKey = new PathCacheKey(start, targetPos);
        CachedPath cached = pathCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            startPathExecution(cached.path);
            return;
        }

        // Calculate path asynchronously
        PathOptions options = new PathOptions();
        options.avoidDanger = avoidDanger;
        options.allowParkour = allowParkour;
        options.maxFallDistance = maxFallDistance;
        options.useSprint = useSprint;
        options.allowSwim = allowSwim;
        options.isCreative = player.isCreative();
        options.targetRadius = radius;

        CompletableFuture.supplyAsync(() -> findPath(world, start, targetPos, maxIterations, options))
                .thenAccept(path -> {
                    Minecraft.getInstance().execute(() -> {
                        if (requestId != currentRequestId)
                            return; // Obsolete request

                        if (path == null || path.isEmpty()) {
                            ScriptLogger.getInstance().error("[Pathfinding] No path found!");
                            isActive = false;
                            return;
                        }

                        LOGGER.debug("Path found with {} nodes", path.size());

                        // Cache the path
                        if (pathCache.size() >= CACHE_MAX_SIZE) {
                            cleanExpiredCache();
                        }
                        pathCache.put(cacheKey, new CachedPath(path));

                        startPathExecution(path);
                    });
                });
    }

    private void startPathExecution(List<BlockPos> path) {
        currentPath = path;
        pathIndex = 0;
        stuckTicks = 0;
        lastPlayerPos = null;

        // Path Smoothing: skip the first point if we're already almost there
        // (prevents turning back to the center of the current block)
        if (path.size() > 1) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                Vec3 playerPos = client.player.position();
                BlockPos first = path.get(0);
                double d2 = playerPos.distanceToSqr(first.getX() + 0.5, first.getY(), first.getZ() + 0.5);
                if (d2 < 1.0) { // If closer than 1 block, skip to next
                    pathIndex = 1;
                }
            }
        }

        isActive = true;
    }

    /**
     * Called every tick to execute pathfinding movement.
     */
    public void tick() {
        if (!isActive || currentPath == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null)
            return;

        // Check if reached destination
        if (pathIndex >= currentPath.size()) {
            ScriptLogger.getInstance().info("[Pathfinding] Destination reached.");
            stop();
            return;
        }

        Vec3 playerPos = player.position();
        lastPlayerPos = playerPos;

        // Check if close enough to final target (Include Y coordinate!)
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY();
        double targetZ = targetPos.getZ() + 0.5;
        double distToTargetSq = playerPos.distanceToSqr(targetX, targetY, targetZ);
        
        if (distToTargetSq <= targetRadius * targetRadius) {
            ScriptLogger.getInstance().info("[Pathfinding] Reached! DistSq: " + String.format("%.4f", distToTargetSq) + " <= " + String.format("%.4f", targetRadius * targetRadius));
            stop();
            return;
        }

        // We strictly follow the A* path nodes. The A* algorithm already correctly checks 
        // bounding box clearances for diagonal moves. 
        // Aggressive line-of-sight path smoothing has been removed to prevent getting stuck on trees.

        // Advance waypoints smoothly: if within 0.8 blocks or standing in the block column
        while (pathIndex < currentPath.size() - 1) {
            BlockPos wp = currentPath.get(pathIndex);
            double wdx = wp.getX() + 0.5 - playerPos.x;
            double wdz = wp.getZ() + 0.5 - playerPos.z;
            double wdy = wp.getY() - playerPos.y;
            double wDistSq = wdx * wdx + wdz * wdz;

            boolean inBlock = player.blockPosition().getX() == wp.getX() && player.blockPosition().getZ() == wp.getZ();
            if ((wDistSq < 0.64 || inBlock) && Math.abs(wdy) < 1.5) {
                pathIndex++;
            } else {
                break;
            }
        }

        BlockPos target = currentPath.get(pathIndex);
        double dx = target.getX() + 0.5 - playerPos.x;
        double dy = target.getY() - playerPos.y;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double distSq = dx * dx + dz * dz;

        // Face the waypoint we are moving towards
        float targetYaw = net.minecraft.util.Mth.wrapDegrees((float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0F / (float) Math.PI)) - 90.0F);
        float currentYaw = player.getYRot();
        float yawDiff = net.minecraft.util.Mth.wrapDegrees(targetYaw - currentYaw);
        float step = 35.0f;
        float newYaw = currentYaw + net.minecraft.util.Mth.clamp(yawDiff, -step, step);
        player.setYRot(newYaw);
        player.setYHeadRot(newYaw);
        player.yBodyRot = newYaw;
        player.yRotO = newYaw;
        player.yHeadRotO = newYaw;

        float targetPitch = 0.0f;
        if (Math.abs(dy) >= 0.8) {
            double distHoriz = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            targetPitch = (float) (-(net.minecraft.util.Mth.atan2(dy, distHoriz) * (180.0F / (float) Math.PI)));
            targetPitch = net.minecraft.util.Mth.clamp(targetPitch, -35.0f, 35.0f);
        }
        float currentPitch = player.getXRot();
        float pitchDiff = net.minecraft.util.Mth.wrapDegrees(targetPitch - currentPitch);
        float newPitch = currentPitch + net.minecraft.util.Mth.clamp(pitchDiff, -15.0f, 15.0f);
        player.setXRot(newPitch);
        player.xRotO = newPitch;

        // Movement input - Use KeyBindings to ensure they are processed correctly
        if (distSq > 0.1) {
            com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("forward", true);
        } else {
            com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("forward", false);
        }

        // Vertical movement (Jumping / Flying)
        if (player.isCreative()) {
            if (Math.abs(dy) > 0.5) {
                if (!player.getAbilities().flying) player.getAbilities().flying = true;
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("jump", dy > 0.2);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("sneak", dy < -0.2);
            } else {
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("jump", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("sneak", false);
            }
        } else {
            // Survival jump logic & Swimming
            boolean inLiquid = player.isInWater() || player.isInLava();
            if ((dy > 0.5 && player.onGround()) || (inLiquid && dy > -0.2)) {
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("jump", true);
            } else {
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("jump", false);
            }
        }

        // Sprinting
        player.setSprinting(useSprint && distSq > 4.0);
    }

    /**
     * Stop pathfinding and clear movement input.
     */
    public void stop() {
        if (isActive) {
            ScriptLogger.getInstance().info("[Pathfinding] Stopping.");
        }
        currentRequestId++; // Invalidate any pending pathfinding tasks
        isActive = false;
        currentPath = null;
        pathIndex = 0;
        targetPos = null;
        lastPlayerPos = null;
        stuckTicks = 0;

        // Clear player input using options keys
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> {
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("forward", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("back", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("left", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("right", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("jump", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("sneak", false);
                com.kasperstudios.khwrapper.KHClientWrapper.setKeyState("sprint", false);
            });
        }

        LocalPlayer player = client != null ? client.player : null;
        if (player != null) {
            player.setSprinting(false);
            if (player.input != null) {
                player.input.up = false;
                player.input.forwardImpulse = 0.0f;
            }
        }
    }

    /**
     * Check if pathfinding is currently active.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Get current path length.
     */
    public int getPathLength() {
        return currentPath != null ? currentPath.size() : 0;
    }

    /**
     * A* pathfinding algorithm.
     */
    private List<BlockPos> findPath(Level world, BlockPos start, BlockPos end, int maxIter, PathOptions options) {
        if (start.equals(end))
            return List.of(end);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIter) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(end)
                    || current.pos.distSqr(end) <= (options.targetRadius * options.targetRadius)) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (Neighbor neighbor : getEnhancedNeighbors(world, current.pos, options)) {
                if (closedSet.contains(neighbor.pos))
                    continue;

                double moveCost = neighbor.cost;
                double tentativeG = current.gScore + moveCost;

                Node neighborNode = allNodes.get(neighbor.pos);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor.pos, current, tentativeG,
                            tentativeG + heuristic(neighbor.pos, end));
                    allNodes.put(neighbor.pos, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeG < neighborNode.gScore) {
                    openSet.remove(neighborNode);
                    neighborNode.parent = current;
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + heuristic(neighbor.pos, end);
                    openSet.add(neighborNode);
                }
            }
        }

        return null;
    }

    private double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.pos);
            node = node.parent;
        }
        return path;
    }

    private List<Neighbor> getEnhancedNeighbors(Level world, BlockPos pos, PathOptions options) {
        List<Neighbor> neighbors = new ArrayList<>();

        BlockPos[] horizontal = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        BlockPos[] diagonal = {
                pos.north().east(), pos.north().west(),
                pos.south().east(), pos.south().west()
        };

        // Creative Flight movement - Direct flight jumps
        if (options.isCreative) {
            int r = 4;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        double dSq = dx*dx + dy*dy + dz*dz;
                        if (dSq > r*r || dSq < 1.0) continue;
                        
                        BlockPos diag = pos.offset(dx, dy, dz);
                        if (isWalkable(world, diag, options)) {
                            if (hasLineOfSight(world, pos, diag)) {
                                neighbors.add(new Neighbor(diag, Math.sqrt(dSq) * 0.9));
                            }
                        }
                    }
                }
            }
        }
        // Horizontal movement
        for (BlockPos dir : horizontal) {
            // Check both feet AND head clearance for the horizontal step
            if (isWalkable(world, dir, options) && world.getBlockState(dir.above()).getCollisionShape(world, dir.above()).isEmpty()) {
                neighbors.add(new Neighbor(dir, 1.0));
            }

            // Jump up (even if same level blocked)
            BlockPos up = dir.above();
            if (isWalkable(world, up, options) && canJumpTo(world, pos, up)) {
                neighbors.add(new Neighbor(up, 1.5));
            }

            // Falling movement
            for (int fall = 1; fall <= options.maxFallDistance; fall++) {
                BlockPos down = dir.below(fall);
                if (world.getBlockState(dir.above()).getCollisionShape(world, dir.above()).isEmpty() && 
                    isWalkable(world, down, options) && 
                    canFallTo(world, pos, down, fall)) {
                    neighbors.add(new Neighbor(down, 1.0 + fall * 0.2));
                    break;
                }
            }

            // Parkour (Jump across 1-2 block gaps)
            if (options.allowParkour || true) { // Default to true for better AI
                BlockPos gap1 = dir;
                BlockPos landing1 = dir.offset(dir.getX() - pos.getX(), 0, dir.getZ() - pos.getZ());
                if (world.getBlockState(gap1).getCollisionShape(world, gap1).isEmpty() && 
                    isWalkable(world, landing1, options) && 
                    canParkourTo(world, pos, landing1)) {
                    neighbors.add(new Neighbor(landing1, 2.0));
                }
            }
        }

        // Diagonal movement - Check corners to prevent getting stuck
        for (int i = 0; i < diagonal.length; i++) {
            BlockPos diag = diagonal[i];
            if (isWalkable(world, diag, options)) {
                // Check the two horizontal neighbors that form this diagonal
                BlockPos h1, h2;
                if (i == 0) { // NE
                    h1 = pos.north(); h2 = pos.east();
                } else if (i == 1) { // NW
                    h1 = pos.north(); h2 = pos.west();
                } else if (i == 2) { // SE
                    h1 = pos.south(); h2 = pos.east();
                } else { // SW
                    h1 = pos.south(); h2 = pos.west();
                }

                if (isWalkable(world, h1, options) && isWalkable(world, h2, options)) {
                    neighbors.add(new Neighbor(diag, 1.414));
                }
            }
        }

        // Climbing
        if (isClimbable(world, pos)) {
            BlockPos up = pos.above();
            if (isWalkable(world, up, options) || isClimbable(world, up)) {
                neighbors.add(new Neighbor(up, 1.2));
            }
        }

        if (isClimbable(world, pos.below())) {
            BlockPos down = pos.below();
            if (isWalkable(world, down, options) || isClimbable(world, down)) {
                neighbors.add(new Neighbor(down, 1.0));
            }
        }

        // Swimming
        if (options.allowSwim && isInWater(world, pos)) {
            BlockPos up = pos.above();
            BlockPos down = pos.below();
            if (isInWater(world, up) || world.getBlockState(up).getCollisionShape(world, up).isEmpty()) {
                neighbors.add(new Neighbor(up, 1.5));
            }
            if (isInWater(world, down)) {
                neighbors.add(new Neighbor(down, 1.0));
            }
        }

        return neighbors;
    }

    private boolean isWalkable(Level world, BlockPos pos, PathOptions options) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState ground = world.getBlockState(pos.below());

        if (options.avoidDanger) {
            if (isDangerous(world, pos) || isDangerous(world, pos.below()) || isDangerous(world, pos.above())) {
                return false;
            }
        }

        // Use strict collision checks and explicit leaves avoidance
        boolean feetPassable = feet.getCollisionShape(world, pos).isEmpty() || isClimbable(world, pos) || isInWater(world, pos);
        boolean headPassable = head.getCollisionShape(world, pos.above()).isEmpty();
        
        // Explicitly avoid leaves as they are often problematic for hitboxes
        if (feet.getBlock() instanceof LeavesBlock || head.getBlock() instanceof LeavesBlock) {
            return false;
        }

        // Must have something to stand on (unless flying/creative)
        boolean hasGround = !ground.getCollisionShape(world, pos.below()).isEmpty() ||
                isInWater(world, pos.below()) ||
                isClimbable(world, pos.below()) ||
                isClimbable(world, pos) ||
                pos.getY() <= world.getMinBuildHeight() ||
                options.isCreative;

        return feetPassable && headPassable && hasGround;
    }

    private boolean isDangerous(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return getDangerousBlocks().contains(state.getBlock());
    }

    private boolean isClimbable(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof LadderBlock ||
                state.getBlock() instanceof VineBlock ||
                state.is(Blocks.SCAFFOLDING);
    }

    private boolean isInWater(Level world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return fluid.is(FluidTags.WATER);
    }

    private boolean canJumpTo(Level world, BlockPos from, BlockPos to) {
        // Must be on solid ground or in water/ladder
        boolean onGround = !world.getBlockState(from.below()).getCollisionShape(world, from.below()).isEmpty() ||
                isInWater(world, from) || isClimbable(world, from);
        if (!onGround)
            return false;

        // Check head clearance at start (need 3 blocks to jump comfortably, but 2 might
        // work for some steps)
        // For a full jump up, we need space at from.up(2)
        if (!world.getBlockState(from.above(2)).getCollisionShape(world, from.above(2)).isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean canFallTo(Level world, BlockPos from, BlockPos to, int fallDistance) {
        for (int i = 1; i < fallDistance; i++) {
            BlockPos check = from.below(i);
            if (!world.getBlockState(check).getCollisionShape(world, check).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean canParkourTo(Level world, BlockPos from, BlockPos to) {
        // Must be on solid ground
        if (world.getBlockState(from.below()).getCollisionShape(world, from.below()).isEmpty()) return false;

        // Gap size
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        
        // Ensure head clearance during the jump
        BlockPos mid = new BlockPos((from.getX() + to.getX()) / 2, from.getY() + 1, (from.getZ() + to.getZ()) / 2);
        if (!world.getBlockState(mid).getCollisionShape(world, mid).isEmpty()) return false;
        if (!world.getBlockState(from.above(2)).getCollisionShape(world, from.above(2)).isEmpty()) return false;

        return true;
    }

    private boolean hasLineOfSight(Level world, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        for (double i = 0.5; i < dist; i += 0.5) {
            BlockPos check = from.offset((int)(dx * i / dist), (int)(dy * i / dist), (int)(dz * i / dist));
            if (!world.getBlockState(check).getCollisionShape(world, check).isEmpty()) return false;
        }
        return true;
    }

    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        pathCache.entrySet().removeIf(e -> e.getValue().isExpired());
        if (pathCache.size() >= CACHE_MAX_SIZE) {
            pathCache.clear();
        }
    }

    /**
     * Clear all cached paths.
     */
    public void clearCache() {
        pathCache.clear();
    }

    // Inner classes

    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore;
        double fScore;

        Node(BlockPos pos, Node parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private static class Neighbor {
        BlockPos pos;
        double cost;

        Neighbor(BlockPos pos, double cost) {
            this.pos = pos;
            this.cost = cost;
        }
    }

    private static class PathOptions {
        boolean avoidDanger = true;
        boolean allowParkour = false;
        int maxFallDistance = 3;
        boolean useSprint = true;
        boolean allowSwim = true;
        boolean isCreative = false;
        double targetRadius = 1.0;
    }

    private static class PathCacheKey {
        final BlockPos start;
        final BlockPos end;

        PathCacheKey(BlockPos start, BlockPos end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            PathCacheKey that = (PathCacheKey) o;
            return start.equals(that.start) && end.equals(that.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    private static class CachedPath {
        final List<BlockPos> path;
        final long timestamp;

        CachedPath(List<BlockPos> path) {
            this.path = path;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}

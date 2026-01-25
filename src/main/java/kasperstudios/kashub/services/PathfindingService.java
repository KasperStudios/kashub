package kasperstudios.kashub.services;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PathfindingService - A* pathfinding with obstacle avoidance.
 * Integrated into player.moveTo() for automatic navigation.
 */
public class PathfindingService {

    private static PathfindingService INSTANCE;

    // Pathfinding state
    private volatile boolean isActive = false;
    private volatile List<BlockPos> currentPath = null;
    private volatile int pathIndex = 0;
    private volatile BlockPos targetPos = null;
    private volatile float targetRadius = 1.0f;

    // Configuration
    private boolean avoidDanger = true;
    private boolean allowParkour = false;
    private int maxFallDistance = 3;
    private boolean useSprint = true;
    private boolean allowSwim = true;
    private int maxIterations = 2000;

    // Path cache
    private final Map<PathCacheKey, CachedPath> pathCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 50;
    private static final long CACHE_EXPIRY_MS = 30000;

    // Dangerous blocks set
    private Set<net.minecraft.block.Block> dangerousBlocks = null;

    public static PathfindingService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PathfindingService();
        }
        return INSTANCE;
    }

    private Set<net.minecraft.block.Block> getDangerousBlocks() {
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null)
            return;

        System.out.println("[PathfindingService] navigateTo called: " + x + ", " + y + ", " + z);

        stop(); // Stop any existing pathfinding

        targetPos = new BlockPos((int) x, (int) y, (int) z);
        targetRadius = radius;

        BlockPos start = player.getBlockPos();
        World world = player.getWorld();

        System.out.println("[PathfindingService] Start: " + start + ", Target: " + targetPos);

        // Check if already at target
        if (start.getManhattanDistance(targetPos) <= radius) {
            System.out.println(
                    "[PathfindingService] Already at target, distance: " + start.getManhattanDistance(targetPos));
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

        System.out.println("[PathfindingService] Starting async pathfinding...");

        CompletableFuture.supplyAsync(() -> findPath(world, start, targetPos, maxIterations, options))
                .thenAccept(path -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (path == null || path.isEmpty()) {
                            System.out.println("[PathfindingService] No path found!");
                            isActive = false;
                            return;
                        }

                        System.out.println("[PathfindingService] Path found with " + path.size() + " nodes");

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
        isActive = true;
        System.out.println("[PathfindingService] Path execution started, isActive=" + isActive);
    }

    /**
     * Called every tick to execute pathfinding movement.
     */
    public void tick() {
        if (!isActive || currentPath == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null)
            return;

        System.out.println("[PathfindingService] Tick: pathIndex=" + pathIndex + "/" + currentPath.size());

        // Check if reached destination
        if (pathIndex >= currentPath.size()) {
            stop();
            return;
        }

        // Check if close enough to final target
        Vec3d playerPos = player.getPos();
        double distToTarget = Math.sqrt(
                Math.pow(targetPos.getX() + 0.5 - playerPos.x, 2) +
                        Math.pow(targetPos.getZ() + 0.5 - playerPos.z, 2));
        if (distToTarget <= targetRadius) {
            stop();
            return;
        }

        BlockPos target = currentPath.get(pathIndex);
        double dx = target.getX() + 0.5 - playerPos.x;
        double dy = target.getY() - playerPos.y;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        // Move to next waypoint if close enough
        if (dist < 0.5 && Math.abs(dy) < 1.5) {
            pathIndex++;
            return;
        }

        // Look at target
        float yaw = (float) (Math.atan2(-dx, dz) * 180 / Math.PI);
        player.setYaw(yaw);

        // Move forward - MUST use options keys, not player.input!
        client.options.forwardKey.setPressed(true);

        System.out.println("[PathfindingService] Moving to waypoint " + pathIndex + ": " + target + ", dist=" + dist);

        // Jump if needed
        if (dy > 0.5 && player.isOnGround()) {
            player.jump();
            System.out.println("[PathfindingService] Jumping");
        }

        // Sprint if far from target
        if (useSprint && dist > 5) {
            player.setSprinting(true);
        } else {
            player.setSprinting(false);
        }
    }

    /**
     * Stop pathfinding and clear movement input.
     */
    public void stop() {
        isActive = false;
        currentPath = null;
        pathIndex = 0;
        targetPos = null;

        // Clear player input using options keys
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> {
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
            });
        }

        ClientPlayerEntity player = client != null ? client.player : null;
        if (player != null) {
            player.setSprinting(false);
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
    private List<BlockPos> findPath(World world, BlockPos start, BlockPos end, int maxIter, PathOptions options) {
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

            if (current.pos.getManhattanDistance(end) <= 1) {
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
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.pos);
            node = node.parent;
        }
        return path;
    }

    private List<Neighbor> getEnhancedNeighbors(World world, BlockPos pos, PathOptions options) {
        List<Neighbor> neighbors = new ArrayList<>();

        BlockPos[] horizontal = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        BlockPos[] diagonal = {
                pos.north().east(), pos.north().west(),
                pos.south().east(), pos.south().west()
        };

        // Horizontal movement
        for (BlockPos dir : horizontal) {
            if (isWalkable(world, dir, options)) {
                neighbors.add(new Neighbor(dir, 1.0));
            }

            // Jump up
            BlockPos up = dir.up();
            if (isWalkable(world, up, options) && canJumpTo(world, pos, up)) {
                neighbors.add(new Neighbor(up, 1.5));
            }

            // Fall down
            for (int fall = 1; fall <= options.maxFallDistance; fall++) {
                BlockPos down = dir.down(fall);
                if (isWalkable(world, down, options) && canFallTo(world, pos, down, fall)) {
                    neighbors.add(new Neighbor(down, 1.0 + fall * 0.2));
                    break;
                }
            }
        }

        // Diagonal movement
        for (BlockPos dir : diagonal) {
            if (isWalkable(world, dir, options)) {
                neighbors.add(new Neighbor(dir, 1.4));
            }
        }

        // Climbing
        if (isClimbable(world, pos)) {
            BlockPos up = pos.up();
            if (isWalkable(world, up, options) || isClimbable(world, up)) {
                neighbors.add(new Neighbor(up, 1.2));
            }
        }

        if (isClimbable(world, pos.down())) {
            BlockPos down = pos.down();
            if (isWalkable(world, down, options) || isClimbable(world, down)) {
                neighbors.add(new Neighbor(down, 1.0));
            }
        }

        // Swimming
        if (options.allowSwim && isInWater(world, pos)) {
            BlockPos up = pos.up();
            BlockPos down = pos.down();
            if (isInWater(world, up) || !world.getBlockState(up).isSolidBlock(world, up)) {
                neighbors.add(new Neighbor(up, 1.5));
            }
            if (isInWater(world, down)) {
                neighbors.add(new Neighbor(down, 1.0));
            }
        }

        // Parkour
        if (options.allowParkour) {
            for (BlockPos dir : horizontal) {
                BlockPos gap = dir.add(dir.subtract(pos));
                if (isWalkable(world, gap, options) && canParkourTo(world, pos, gap)) {
                    neighbors.add(new Neighbor(gap, 2.5));
                }
            }
        }

        return neighbors;
    }

    private boolean isWalkable(World world, BlockPos pos, PathOptions options) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState ground = world.getBlockState(pos.down());

        if (options.avoidDanger) {
            if (isDangerous(world, pos) || isDangerous(world, pos.down()) || isDangerous(world, pos.up())) {
                return false;
            }
        }

        boolean feetClear = !feet.isSolidBlock(world, pos) || isClimbable(world, pos);
        boolean headClear = !head.isSolidBlock(world, pos.up());

        boolean hasGround = ground.isSolidBlock(world, pos.down()) ||
                isInWater(world, pos.down()) ||
                isClimbable(world, pos.down()) ||
                pos.getY() <= world.getBottomY();

        return feetClear && headClear && hasGround;
    }

    private boolean isDangerous(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return getDangerousBlocks().contains(state.getBlock());
    }

    private boolean isClimbable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof LadderBlock ||
                state.getBlock() instanceof VineBlock ||
                state.isOf(Blocks.SCAFFOLDING);
    }

    private boolean isInWater(World world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return fluid.isIn(FluidTags.WATER);
    }

    private boolean canJumpTo(World world, BlockPos from, BlockPos to) {
        return world.getBlockState(from).isSolidBlock(world, from.down());
    }

    private boolean canFallTo(World world, BlockPos from, BlockPos to, int fallDistance) {
        for (int i = 1; i < fallDistance; i++) {
            BlockPos check = from.down(i);
            if (world.getBlockState(check).isSolidBlock(world, check)) {
                return false;
            }
        }
        return true;
    }

    private boolean canParkourTo(World world, BlockPos from, BlockPos to) {
        BlockPos middle = new BlockPos(
                (from.getX() + to.getX()) / 2,
                from.getY(),
                (from.getZ() + to.getZ()) / 2);
        return !world.getBlockState(middle).isSolidBlock(world, middle) &&
                !world.getBlockState(middle.up()).isSolidBlock(world, middle.up());
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

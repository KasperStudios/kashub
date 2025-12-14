package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Pathfinding command - moves player to target coordinates
 * 
 * Syntax:
 *   pathfind <x> <y> <z> [options] - Navigate to coordinates
 *   pathfind stop - Stop current pathfinding
 *   pathfind home - Go to saved home position
 *   pathfind sethome - Save current position as home
 *   pathfind config <option> <value> - Configure pathfinding
 * 
 * Options:
 *   avoidDanger=true/false - Avoid dangerous blocks (lava, fire, cactus)
 *   allowParkour=true/false - Allow jumping over gaps
 *   maxFall=N - Maximum safe fall distance
 *   sprint=true/false - Use sprint when possible
 *   swim=true/false - Allow swimming
 */
public class PathfindCommand implements Command {
    
    private static BlockPos homePosition = null;
    private static boolean isPathfinding = false;
    private static List<BlockPos> currentPath = null;
    private static int pathIndex = 0;
    
    // Конфигурация pathfinding
    private static boolean avoidDanger = true;
    private static boolean allowParkour = false;
    private static int maxFallDistance = 3;
    private static boolean useSprint = true;
    private static boolean allowSwim = true;
    private static int maxIterations = 2000; // Увеличено с 100
    
    // Кэш путей
    private static final Map<PathCacheKey, List<BlockPos>> pathCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 50;
    private static final long CACHE_EXPIRY_MS = 30000; // 30 секунд
    
    // Опасные блоки
    private static final Set<net.minecraft.block.Block> DANGEROUS_BLOCKS = Set.of(
        Blocks.LAVA,
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.CACTUS,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.WITHER_ROSE,
        Blocks.MAGMA_BLOCK,
        Blocks.CAMPFIRE,
        Blocks.SOUL_CAMPFIRE,
        Blocks.POWDER_SNOW
    );
    
    @Override
    public String getName() {
        return "pathfind";
    }
    
    @Override
    public String getDescription() {
        return "Navigate to coordinates with obstacle avoidance";
    }
    
    @Override
    public String getParameters() {
        return "<x> <y> <z> [options] | stop | home | sethome | config";
    }

    @Override
    public String getCategory() {
        return "Movement";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Advanced pathfinding with A* algorithm.\n\n" +
               "Basic usage:\n" +
               "  pathfind <x> <y> <z>\n" +
               "  pathfind stop\n" +
               "  pathfind sethome\n" +
               "  pathfind home\n\n" +
               "Options:\n" +
               "  avoidDanger=true  - Avoid lava/fire/cactus\n" +
               "  allowParkour=true - Allow 2-block jumps\n" +
               "  sprint=true       - Sprint while moving\n" +
               "  swim=true         - Allow swimming\n" +
               "  maxFall=3         - Max fall height\n\n" +
               "Example:\n" +
               "  pathfind 100 64 200 avoidDanger=true sprint=true\n\n" +
               "Variables set:\n" +
               "  $pathfind_active   - Currently navigating\n" +
               "  $pathfind_complete - Reached destination\n" +
               "  $pathfind_length   - Path length";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "stop":
                isPathfinding = false;
                currentPath = null;
                interpreter.setVariable("pathfind_active", "false");
                System.out.println("Pathfinding stopped");
                break;
                
            case "sethome":
                homePosition = player.getBlockPos();
                interpreter.setVariable("home_x", String.valueOf(homePosition.getX()));
                interpreter.setVariable("home_y", String.valueOf(homePosition.getY()));
                interpreter.setVariable("home_z", String.valueOf(homePosition.getZ()));
                System.out.println("Home set to: " + homePosition.toShortString());
                break;
                
            case "home":
                if (homePosition == null) {
                    System.out.println("No home position set! Use 'pathfind sethome' first");
                    interpreter.setVariable("pathfind_success", "false");
                    return;
                }
                navigateTo(player, homePosition, interpreter, new PathOptions());
                break;
                
            case "config":
                handleConfig(args, interpreter);
                break;
                
            case "cache":
                handleCache(args, interpreter);
                break;
                
            default:
                // Parse coordinates
                if (args.length >= 3) {
                    try {
                        int x = Integer.parseInt(args[0]);
                        int y = Integer.parseInt(args[1]);
                        int z = Integer.parseInt(args[2]);
                        
                        // Парсим опции
                        PathOptions options = parseOptions(args, 3);
                        navigateTo(player, new BlockPos(x, y, z), interpreter, options);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid coordinates. Usage: pathfind <x> <y> <z>");
                    }
                } else {
                    printHelp();
                }
        }
    }
    
    private void handleConfig(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 3) {
            System.out.println("Current config:");
            System.out.println("  avoidDanger: " + avoidDanger);
            System.out.println("  allowParkour: " + allowParkour);
            System.out.println("  maxFall: " + maxFallDistance);
            System.out.println("  sprint: " + useSprint);
            System.out.println("  swim: " + allowSwim);
            System.out.println("  maxIterations: " + maxIterations);
            return;
        }
        
        String option = args[1].toLowerCase();
        String value = args[2].toLowerCase();
        
        switch (option) {
            case "avoiddanger":
                avoidDanger = value.equals("true");
                break;
            case "allowparkour":
                allowParkour = value.equals("true");
                break;
            case "maxfall":
                try { maxFallDistance = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                break;
            case "sprint":
                useSprint = value.equals("true");
                break;
            case "swim":
                allowSwim = value.equals("true");
                break;
            case "maxiterations":
                try { maxIterations = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                break;
        }
        
        interpreter.setVariable("pathfind_config_" + option, value);
        System.out.println("Set " + option + " = " + value);
    }
    
    private void handleCache(String[] args, ScriptInterpreter interpreter) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            pathCache.clear();
            System.out.println("Path cache cleared");
        } else {
            System.out.println("Path cache size: " + pathCache.size());
        }
        interpreter.setVariable("pathfind_cache_size", String.valueOf(pathCache.size()));
    }
    
    private PathOptions parseOptions(String[] args, int startIndex) {
        PathOptions options = new PathOptions();
        
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                switch (parts[0]) {
                    case "avoiddanger":
                        options.avoidDanger = parts[1].equals("true");
                        break;
                    case "allowparkour":
                        options.allowParkour = parts[1].equals("true");
                        break;
                    case "maxfall":
                        try { options.maxFallDistance = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        break;
                    case "sprint":
                        options.useSprint = parts[1].equals("true");
                        break;
                    case "swim":
                        options.allowSwim = parts[1].equals("true");
                        break;
                }
            }
        }
        
        return options;
    }
    
    private void navigateTo(ClientPlayerEntity player, BlockPos target, ScriptInterpreter interpreter, PathOptions options) {
        BlockPos start = player.getBlockPos();
        World world = player.getWorld();
        
        System.out.println("Pathfinding from " + start.toShortString() + " to " + target.toShortString());
        
        // Проверяем кэш
        PathCacheKey cacheKey = new PathCacheKey(start, target);
        List<BlockPos> cachedPath = pathCache.get(cacheKey);
        if (cachedPath != null && !cachedPath.isEmpty()) {
            System.out.println("Using cached path (" + cachedPath.size() + " nodes)");
            startPathExecution(cachedPath, interpreter);
            return;
        }
        
        // Асинхронный поиск пути
        long startTime = System.currentTimeMillis();
        
        CompletableFuture.supplyAsync(() -> findPath(world, start, target, maxIterations, options))
            .thenAccept(path -> {
                MinecraftClient.getInstance().execute(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    
                    if (path == null || path.isEmpty()) {
                        System.out.println("No path found! (" + elapsed + "ms)");
                        interpreter.setVariable("pathfind_success", "false");
                        interpreter.setVariable("pathfind_active", "false");
                        return;
                    }
                    
                    System.out.println("Path found with " + path.size() + " nodes (" + elapsed + "ms)");
                    
                    // Кэшируем путь
                    if (pathCache.size() >= CACHE_MAX_SIZE) {
                        // Удаляем старые записи
                        pathCache.clear();
                    }
                    pathCache.put(cacheKey, path);
                    
                    startPathExecution(path, interpreter);
                });
            });
    }
    
    private void startPathExecution(List<BlockPos> path, ScriptInterpreter interpreter) {
        interpreter.setVariable("pathfind_success", "true");
        interpreter.setVariable("pathfind_active", "true");
        interpreter.setVariable("pathfind_length", String.valueOf(path.size()));
        
        currentPath = path;
        pathIndex = 0;
        isPathfinding = true;
    }
    
    /**
     * Execute one step of the path - called from tick
     */
    public static void tick() {
        if (!isPathfinding || currentPath == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        if (pathIndex >= currentPath.size()) {
            // Path complete
            isPathfinding = false;
            currentPath = null;
            ScriptInterpreter.getInstance().setVariable("pathfind_active", "false");
            ScriptInterpreter.getInstance().setVariable("pathfind_complete", "true");
            System.out.println("Destination reached!");
            return;
        }
        
        BlockPos target = currentPath.get(pathIndex);
        Vec3d playerPos = player.getPos();
        double dx = target.getX() + 0.5 - playerPos.x;
        double dy = target.getY() - playerPos.y;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        // Check if reached current waypoint
        if (dist < 0.5 && Math.abs(dy) < 1.5) {
            pathIndex++;
            return;
        }
        
        // Look towards target
        float yaw = (float) (Math.atan2(-dx, dz) * 180 / Math.PI);
        player.setYaw(yaw);
        
        // Move forward
        player.input.pressingForward = true;
        
        // Jump if needed
        if (dy > 0.5 && player.isOnGround()) {
            player.jump();
        }
        
        // Sprint if far
        player.setSprinting(dist > 5);
    }
    
    private void executePathStep(ClientPlayerEntity player, ScriptInterpreter interpreter) {
        // Path execution is handled in tick()
    }
    
    /**
     * Enhanced A* pathfinding implementation with danger avoidance
     */
    private List<BlockPos> findPath(World world, BlockPos start, BlockPos end, int maxIter, PathOptions options) {
        if (start.equals(end)) return List.of(end);
        
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
                // Found path
                return reconstructPath(current);
            }
            
            closedSet.add(current.pos);
            
            // Check neighbors with enhanced logic
            for (Neighbor neighbor : getEnhancedNeighbors(world, current.pos, options)) {
                if (closedSet.contains(neighbor.pos)) continue;
                
                // Стоимость перехода с учётом типа движения
                double moveCost = neighbor.cost;
                double tentativeG = current.gScore + moveCost;
                
                Node neighborNode = allNodes.get(neighbor.pos);
                if (neighborNode == null) {
                    neighborNode = new Node(neighbor.pos, current, tentativeG, tentativeG + heuristic(neighbor.pos, end));
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
        
        return null; // No path found
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
        
        // Горизонтальные направления
        BlockPos[] horizontal = {
            pos.north(), pos.south(), pos.east(), pos.west()
        };
        
        // Диагональные направления
        BlockPos[] diagonal = {
            pos.north().east(), pos.north().west(),
            pos.south().east(), pos.south().west()
        };
        
        // Проверяем горизонтальные
        for (BlockPos dir : horizontal) {
            if (isWalkable(world, dir, options)) {
                neighbors.add(new Neighbor(dir, 1.0));
            }
            
            // Проверяем подъём (прыжок на блок)
            BlockPos up = dir.up();
            if (isWalkable(world, up, options) && canJumpTo(world, pos, up)) {
                neighbors.add(new Neighbor(up, 1.5)); // Прыжок дороже
            }
            
            // Проверяем спуск
            for (int fall = 1; fall <= options.maxFallDistance; fall++) {
                BlockPos down = dir.down(fall);
                if (isWalkable(world, down, options) && canFallTo(world, pos, down, fall)) {
                    neighbors.add(new Neighbor(down, 1.0 + fall * 0.2)); // Падение немного дороже
                    break; // Берём первый валидный спуск
                }
            }
        }
        
        // Проверяем диагональные (дороже)
        for (BlockPos dir : diagonal) {
            if (isWalkable(world, dir, options)) {
                neighbors.add(new Neighbor(dir, 1.4)); // sqrt(2)
            }
        }
        
        // Вертикальное движение (лестницы, лианы)
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
        
        // Плавание
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
        
        // Паркур (прыжки через промежутки)
        if (options.allowParkour) {
            for (BlockPos dir : horizontal) {
                BlockPos gap = dir.add(dir.subtract(pos)); // 2 блока в направлении
                if (isWalkable(world, gap, options) && canParkourTo(world, pos, gap)) {
                    neighbors.add(new Neighbor(gap, 2.5)); // Паркур дорогой
                }
            }
        }
        
        return neighbors;
    }
    
    private boolean isWalkable(World world, BlockPos pos, PathOptions options) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState ground = world.getBlockState(pos.down());
        
        // Проверяем опасные блоки
        if (options.avoidDanger) {
            if (isDangerous(world, pos) || isDangerous(world, pos.down()) || isDangerous(world, pos.up())) {
                return false;
            }
        }
        
        // Need empty space for feet and head
        boolean feetClear = !feet.isSolidBlock(world, pos) || isClimbable(world, pos);
        boolean headClear = !head.isSolidBlock(world, pos.up());
        
        // Need solid ground (or water, or climbable)
        boolean hasGround = ground.isSolidBlock(world, pos.down()) || 
                           isInWater(world, pos.down()) ||
                           isClimbable(world, pos.down()) ||
                           pos.getY() <= world.getBottomY();
        
        return feetClear && headClear && hasGround;
    }
    
    private boolean isDangerous(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return DANGEROUS_BLOCKS.contains(state.getBlock());
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
        // Проверяем, что можем прыгнуть (есть место для разбега)
        return world.getBlockState(from).isSolidBlock(world, from.down());
    }
    
    private boolean canFallTo(World world, BlockPos from, BlockPos to, int fallDistance) {
        // Проверяем, что путь падения свободен
        for (int i = 1; i < fallDistance; i++) {
            BlockPos check = from.down(i);
            if (world.getBlockState(check).isSolidBlock(world, check)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean canParkourTo(World world, BlockPos from, BlockPos to) {
        // Проверяем, что промежуток свободен
        BlockPos middle = new BlockPos(
            (from.getX() + to.getX()) / 2,
            from.getY(),
            (from.getZ() + to.getZ()) / 2
        );
        return !world.getBlockState(middle).isSolidBlock(world, middle) &&
               !world.getBlockState(middle.up()).isSolidBlock(world, middle.up());
    }
    
    private void printHelp() {
        System.out.println("Pathfind Command:");
        System.out.println("  pathfind <x> <y> <z> [options] - Navigate to coordinates");
        System.out.println("  pathfind stop - Stop pathfinding");
        System.out.println("  pathfind sethome - Save current position");
        System.out.println("  pathfind home - Return to saved position");
        System.out.println("  pathfind config [option] [value] - Configure pathfinding");
        System.out.println("  pathfind cache clear - Clear path cache");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  avoidDanger=true/false - Avoid dangerous blocks");
        System.out.println("  allowParkour=true/false - Allow jumping over gaps");
        System.out.println("  maxFall=N - Maximum safe fall distance");
        System.out.println("  sprint=true/false - Use sprint");
        System.out.println("  swim=true/false - Allow swimming");
    }
    
    public static boolean isActive() {
        return isPathfinding;
    }
    
    public static void stop() {
        isPathfinding = false;
        currentPath = null;
    }
    
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
        boolean avoidDanger = PathfindCommand.avoidDanger;
        boolean allowParkour = PathfindCommand.allowParkour;
        int maxFallDistance = PathfindCommand.maxFallDistance;
        boolean useSprint = PathfindCommand.useSprint;
        boolean allowSwim = PathfindCommand.allowSwim;
    }
    
    private static class PathCacheKey {
        final BlockPos start;
        final BlockPos end;
        final long timestamp;
        
        PathCacheKey(BlockPos start, BlockPos end) {
            this.start = start;
            this.end = end;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathCacheKey that = (PathCacheKey) o;
            return start.equals(that.start) && end.equals(that.end);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}

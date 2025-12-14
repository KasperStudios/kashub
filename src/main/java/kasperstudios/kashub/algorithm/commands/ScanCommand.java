package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.*;

/**
 * Command for scanning blocks in view/area
 * Syntax:
 *   scan blocks <filter> <radius> - Scan for specific blocks in radius
 *   scan view <filter> <distance> - Scan blocks in player's view direction
 *   scan area <x1> <y1> <z1> <x2> <y2> <z2> <filter> - Scan area for blocks
 *   scan nearest <filter> <radius> - Find nearest matching block
 * 
 * Filter examples: diamond_ore, *_ore, chest, spawner
 */
public class ScanCommand implements Command {
    
    // Common ore blocks for quick detection
    private static final Set<String> VALUABLE_ORES = Set.of(
        "diamond_ore", "deepslate_diamond_ore",
        "emerald_ore", "deepslate_emerald_ore",
        "ancient_debris",
        "gold_ore", "deepslate_gold_ore", "nether_gold_ore",
        "iron_ore", "deepslate_iron_ore",
        "copper_ore", "deepslate_copper_ore",
        "lapis_ore", "deepslate_lapis_ore",
        "redstone_ore", "deepslate_redstone_ore",
        "coal_ore", "deepslate_coal_ore",
        "nether_quartz_ore"
    );
    
    @Override
    public String getName() {
        return "scan";
    }
    
    @Override
    public String getDescription() {
        return "Scan for blocks in area or view";
    }
    
    @Override
    public String getParameters() {
        return "<blocks|view|nearest|ores> <filter> <radius/distance>";
    }

    @Override
    public String getCategory() {
        return "Scanner";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Scans for blocks in specified radius.\n\n" +
               "Usage:\n" +
               "  scan blocks <filter> <radius>\n" +
               "  scan ores <radius>\n" +
               "  scan nearest <filter> <radius>\n\n" +
               "Filters:\n" +
               "  diamond_ore, iron_ore, gold_ore\n" +
               "  chest, barrel, spawner\n" +
               "  *_ore - Any ore (wildcard)\n\n" +
               "Variables set:\n" +
               "  $scan_found        - true/false\n" +
               "  $scan_count        - Number found\n" +
               "  $scan_nearest_x/y/z - Nearest position\n" +
               "  $scan_nearest_dist  - Distance\n" +
               "  $scan_diamond_count - Diamond ore count";
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
            case "blocks":
            case "block":
                scanBlocks(player, args, interpreter);
                break;
                
            case "view":
                scanView(player, args, interpreter);
                break;
                
            case "nearest":
                scanNearest(player, args, interpreter);
                break;
                
            case "ores":
                scanOres(player, args, interpreter);
                break;
                
            case "count":
                countBlocks(player, args, interpreter);
                break;
                
            default:
                // Assume it's a filter for quick scan
                String[] newArgs = new String[]{"blocks", subcommand, args.length > 1 ? args[1] : "16"};
                scanBlocks(player, newArgs, interpreter);
        }
    }
    
    private void scanBlocks(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String filter = args.length > 1 ? args[1].toLowerCase() : "*";
        int radius = args.length > 2 ? Integer.parseInt(args[2]) : 16;
        radius = Math.min(radius, 32); // Limit for performance
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        List<BlockPos> found = new ArrayList<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                    
                    if (matchesFilter(blockId, filter)) {
                        found.add(pos);
                    }
                }
            }
        }
        
        // Set results
        interpreter.setVariable("scan_count", String.valueOf(found.size()));
        interpreter.setVariable("scan_found", found.isEmpty() ? "false" : "true");
        
        // Store first 10 results
        for (int i = 0; i < Math.min(found.size(), 10); i++) {
            BlockPos pos = found.get(i);
            BlockState state = world.getBlockState(pos);
            String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
            
            interpreter.setVariable("scan_" + i + "_x", String.valueOf(pos.getX()));
            interpreter.setVariable("scan_" + i + "_y", String.valueOf(pos.getY()));
            interpreter.setVariable("scan_" + i + "_z", String.valueOf(pos.getZ()));
            interpreter.setVariable("scan_" + i + "_block", blockId);
        }
        
        // Find nearest
        if (!found.isEmpty()) {
            BlockPos nearest = found.stream()
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(playerPos)))
                .orElse(null);
            
            if (nearest != null) {
                interpreter.setVariable("scan_nearest_x", String.valueOf(nearest.getX()));
                interpreter.setVariable("scan_nearest_y", String.valueOf(nearest.getY()));
                interpreter.setVariable("scan_nearest_z", String.valueOf(nearest.getZ()));
                interpreter.setVariable("scan_nearest_dist", String.valueOf(
                    Math.sqrt(nearest.getSquaredDistance(playerPos))));
            }
        }
    }
    
    private void scanView(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String filter = args.length > 1 ? args[1].toLowerCase() : "*";
        int distance = args.length > 2 ? Integer.parseInt(args[2]) : 64;
        distance = Math.min(distance, 128);
        
        World world = player.getWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0f);
        
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> checked = new HashSet<>();
        
        // Raycast with some width
        for (int d = 1; d <= distance; d++) {
            Vec3d checkPos = eyePos.add(lookVec.multiply(d));
            
            // Check in a small cone
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos pos = new BlockPos(
                            (int) checkPos.x + dx,
                            (int) checkPos.y + dy,
                            (int) checkPos.z + dz
                        );
                        
                        if (checked.contains(pos)) continue;
                        checked.add(pos);
                        
                        BlockState state = world.getBlockState(pos);
                        String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                        
                        if (matchesFilter(blockId, filter)) {
                            found.add(pos);
                        }
                    }
                }
            }
        }
        
        interpreter.setVariable("scan_count", String.valueOf(found.size()));
        interpreter.setVariable("scan_found", found.isEmpty() ? "false" : "true");
        
        if (!found.isEmpty()) {
            BlockPos nearest = found.get(0);
            interpreter.setVariable("scan_nearest_x", String.valueOf(nearest.getX()));
            interpreter.setVariable("scan_nearest_y", String.valueOf(nearest.getY()));
            interpreter.setVariable("scan_nearest_z", String.valueOf(nearest.getZ()));
        }
    }
    
    private void scanNearest(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String filter = args.length > 1 ? args[1].toLowerCase() : "*";
        int radius = args.length > 2 ? Integer.parseInt(args[2]) : 16;
        radius = Math.min(radius, 32);
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        String nearestBlock = "";
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                    
                    if (matchesFilter(blockId, filter)) {
                        double dist = pos.getSquaredDistance(playerPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                            nearestBlock = blockId;
                        }
                    }
                }
            }
        }
        
        if (nearest != null) {
            interpreter.setVariable("scan_found", "true");
            interpreter.setVariable("scan_nearest_x", String.valueOf(nearest.getX()));
            interpreter.setVariable("scan_nearest_y", String.valueOf(nearest.getY()));
            interpreter.setVariable("scan_nearest_z", String.valueOf(nearest.getZ()));
            interpreter.setVariable("scan_nearest_block", nearestBlock);
            interpreter.setVariable("scan_nearest_dist", String.valueOf(Math.sqrt(nearestDist)));
        } else {
            interpreter.setVariable("scan_found", "false");
        }
    }
    
    private void scanOres(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        int radius = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        radius = Math.min(radius, 16); // Smaller radius for performance
        
        boolean allowCheats = KashubConfig.getInstance().allowCheats;
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        Vec3d playerEyes = player.getEyePos();
        
        Map<String, List<BlockPos>> oresByType = new HashMap<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                    
                    if (VALUABLE_ORES.contains(blockId)) {
                        // If cheats disabled, check if block is visible (not behind other blocks)
                        if (!allowCheats && !isBlockVisible(world, playerEyes, pos)) {
                            continue;
                        }
                        oresByType.computeIfAbsent(blockId, k -> new ArrayList<>()).add(pos);
                    }
                }
            }
        }
        
        // Set results
        int totalOres = oresByType.values().stream().mapToInt(List::size).sum();
        interpreter.setVariable("scan_ore_count", String.valueOf(totalOres));
        interpreter.setVariable("scan_found", totalOres > 0 ? "true" : "false");
        
        // Count by type
        interpreter.setVariable("scan_diamond_count", String.valueOf(
            oresByType.getOrDefault("diamond_ore", List.of()).size() +
            oresByType.getOrDefault("deepslate_diamond_ore", List.of()).size()));
        interpreter.setVariable("scan_emerald_count", String.valueOf(
            oresByType.getOrDefault("emerald_ore", List.of()).size() +
            oresByType.getOrDefault("deepslate_emerald_ore", List.of()).size()));
        interpreter.setVariable("scan_gold_count", String.valueOf(
            oresByType.getOrDefault("gold_ore", List.of()).size() +
            oresByType.getOrDefault("deepslate_gold_ore", List.of()).size() +
            oresByType.getOrDefault("nether_gold_ore", List.of()).size()));
        interpreter.setVariable("scan_iron_count", String.valueOf(
            oresByType.getOrDefault("iron_ore", List.of()).size() +
            oresByType.getOrDefault("deepslate_iron_ore", List.of()).size()));
        interpreter.setVariable("scan_ancient_debris_count", String.valueOf(
            oresByType.getOrDefault("ancient_debris", List.of()).size()));
        
        // Find nearest valuable ore (prioritize diamond > emerald > gold)
        BlockPos nearestValuable = null;
        String nearestType = "";
        double nearestDist = Double.MAX_VALUE;
        
        String[] priority = {"diamond_ore", "deepslate_diamond_ore", "ancient_debris", 
                            "emerald_ore", "deepslate_emerald_ore",
                            "gold_ore", "deepslate_gold_ore", "nether_gold_ore",
                            "iron_ore", "deepslate_iron_ore",
                            "lapis_ore", "deepslate_lapis_ore",
                            "redstone_ore", "deepslate_redstone_ore",
                            "copper_ore", "deepslate_copper_ore",
                            "coal_ore", "deepslate_coal_ore", "nether_quartz_ore"};
        
        for (String ore : priority) {
            List<BlockPos> positions = oresByType.get(ore);
            if (positions != null) {
                for (BlockPos pos : positions) {
                    double dist = pos.getSquaredDistance(playerPos);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestValuable = pos;
                        nearestType = ore;
                    }
                }
            }
        }
        
        if (nearestValuable != null) {
            interpreter.setVariable("scan_valuable_found", "true");
            interpreter.setVariable("scan_valuable_x", String.valueOf(nearestValuable.getX()));
            interpreter.setVariable("scan_valuable_y", String.valueOf(nearestValuable.getY()));
            interpreter.setVariable("scan_valuable_z", String.valueOf(nearestValuable.getZ()));
            interpreter.setVariable("scan_valuable_type", nearestType);
            interpreter.setVariable("scan_valuable_dist", String.valueOf(Math.sqrt(nearestDist)));
        } else {
            interpreter.setVariable("scan_valuable_found", "false");
        }
    }
    
    private void countBlocks(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String filter = args.length > 1 ? args[1].toLowerCase() : "*";
        int radius = args.length > 2 ? Integer.parseInt(args[2]) : 16;
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                    
                    if (matchesFilter(blockId, filter)) {
                        count++;
                    }
                }
            }
        }
        
        interpreter.setVariable("scan_count", String.valueOf(count));
    }
    
    private boolean matchesFilter(String blockId, String filter) {
        if (filter.equals("*")) return !blockId.equals("air");
        if (filter.startsWith("*") && filter.endsWith("*")) {
            return blockId.contains(filter.substring(1, filter.length() - 1));
        }
        if (filter.startsWith("*")) {
            return blockId.endsWith(filter.substring(1));
        }
        if (filter.endsWith("*")) {
            return blockId.startsWith(filter.substring(0, filter.length() - 1));
        }
        return blockId.equals(filter) || blockId.contains(filter);
    }
    
    private void printHelp() {
        System.out.println("Scan commands:");
        System.out.println("  scan blocks <filter> <radius> - Scan for blocks");
        System.out.println("  scan view <filter> <distance> - Scan in view direction");
        System.out.println("  scan nearest <filter> <radius> - Find nearest block");
        System.out.println("  scan ores <radius> - Scan for valuable ores");
        System.out.println("  scan count <filter> <radius> - Count matching blocks");
        System.out.println("");
        System.out.println("Filter examples: diamond_ore, *_ore, chest, spawner");
    }
    
    /**
     * Check if a block is visible from player's eyes (not obstructed by other blocks)
     * Used when cheats are disabled to prevent X-ray scanning
     */
    private boolean isBlockVisible(World world, Vec3d eyePos, BlockPos targetPos) {
        Vec3d targetCenter = Vec3d.ofCenter(targetPos);
        
        // Raycast from player eyes to block center
        RaycastContext context = new RaycastContext(
            eyePos, 
            targetCenter, 
            RaycastContext.ShapeType.COLLIDER, 
            RaycastContext.FluidHandling.NONE, 
            net.minecraft.block.ShapeContext.absent()
        );
        
        BlockHitResult result = world.raycast(context);
        
        // Block is visible if raycast hits the target block or nothing
        if (result.getType() == HitResult.Type.MISS) {
            return true;
        }
        
        // Check if we hit the target block
        BlockPos hitPos = result.getBlockPos();
        return hitPos.equals(targetPos);
    }
}

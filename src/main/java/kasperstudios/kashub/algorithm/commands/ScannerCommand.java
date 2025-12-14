package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Scanner command with async scanning and spatial indexing
 * 
 * Syntax:
 *   scanner blocks <types> [options] - Scan for blocks
 *   scanner entities <types> [options] - Scan for entities
 *   scanner cache clear - Clear scan cache
 * 
 * Options:
 *   radius=N - Search radius (default 32)
 *   yMin=N, yMax=N - Y range filter
 *   sortBy=distance|count - Sort results
 *   limit=N - Max results
 */
public class ScannerCommand implements Command {
    
    // Кэш результатов сканирования
    private static final Map<ScanCacheKey, ScanResult> scanCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 5000; // 5 секунд
    private static final int CACHE_MAX_SIZE = 20;
    
    // Последнее сканирование для инкрементального обновления
    private static BlockPos lastPlayerPos = null;
    private static long lastScanTime = 0;
    
    @Override
    public String getName() {
        return "scanner";
    }
    
    @Override
    public String getDescription() {
        return "Advanced block and entity scanning with caching";
    }
    
    @Override
    public String getParameters() {
        return "blocks|entities <types> [options]";
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
                scanBlocksAdvanced(player, args, interpreter);
                break;
            case "entities":
                scanEntitiesAdvanced(player, args, interpreter);
                break;
            case "cache":
                handleCache(args, interpreter);
                break;
            default:
                printHelp();
        }
    }
    
    private void scanBlocksAdvanced(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        // Парсим опции
        ScanOptions options = parseOptions(args);
        Set<String> targetTypes = parseTypes(args.length > 1 ? args[1] : "*");
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Проверяем кэш
        ScanCacheKey cacheKey = new ScanCacheKey("blocks", targetTypes, options, playerPos);
        ScanResult cached = scanCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            applyBlockResults(cached.blockResults, interpreter, options);
            System.out.println("Using cached scan results (" + cached.blockResults.size() + " blocks)");
            return;
        }
        
        // Асинхронное сканирование по чанкам
        long startTime = System.currentTimeMillis();
        
        CompletableFuture.supplyAsync(() -> {
            List<BlockScanResult> results = new ArrayList<>();
            
            int chunkRadius = (options.radius / 16) + 1;
            ChunkPos playerChunk = new ChunkPos(playerPos);
            
            for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
                for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                    ChunkPos chunkPos = new ChunkPos(playerChunk.x + cx, playerChunk.z + cz);
                    
                    // Сканируем чанк
                    scanChunk(world, chunkPos, playerPos, targetTypes, options, results);
                }
            }
            
            return results;
        }).thenAccept(scanResults -> {
            MinecraftClient.getInstance().execute(() -> {
                long elapsed = System.currentTimeMillis() - startTime;
                
                // Сортируем результаты
                List<BlockScanResult> finalResults = new ArrayList<>(scanResults);
                sortResults(finalResults, playerPos, options);
                
                // Ограничиваем количество
                if (options.limit > 0 && finalResults.size() > options.limit) {
                    finalResults = new ArrayList<>(finalResults.subList(0, options.limit));
                }
                
                // Кэшируем
                if (scanCache.size() >= CACHE_MAX_SIZE) {
                    cleanupCache();
                }
                scanCache.put(cacheKey, new ScanResult(finalResults, null));
                
                // Применяем результаты
                applyBlockResults(finalResults, interpreter, options);
                
                System.out.println("Scan complete: " + finalResults.size() + " blocks found (" + elapsed + "ms)");
            });
        });
    }
    
    private void scanChunk(World world, ChunkPos chunkPos, BlockPos playerPos, 
                          Set<String> targetTypes, ScanOptions options, List<BlockScanResult> results) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        
        int yMin = options.yMin != null ? options.yMin : world.getBottomY();
        int yMax = options.yMax != null ? options.yMax : world.getTopY();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                
                // Проверяем радиус
                double distXZ = Math.sqrt(Math.pow(worldX - playerPos.getX(), 2) + 
                                         Math.pow(worldZ - playerPos.getZ(), 2));
                if (distXZ > options.radius) continue;
                
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state = world.getBlockState(pos);
                    
                    if (state.isAir() && options.excludeAir) continue;
                    
                    String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
                    
                    if (matchesAnyType(blockId, targetTypes)) {
                        double dist = Math.sqrt(pos.getSquaredDistance(playerPos));
                        results.add(new BlockScanResult(pos, blockId, dist));
                    }
                }
            }
        }
    }
    
    private void scanEntitiesAdvanced(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        ScanOptions options = parseOptions(args);
        Set<String> targetTypes = parseTypes(args.length > 1 ? args[1] : "*");
        
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Проверяем кэш
        ScanCacheKey cacheKey = new ScanCacheKey("entities", targetTypes, options, playerPos);
        ScanResult cached = scanCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            applyEntityResults(cached.entityResults, interpreter, options);
            System.out.println("Using cached entity scan (" + cached.entityResults.size() + " entities)");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        Box searchBox = player.getBoundingBox().expand(options.radius);
        List<Entity> entities = world.getOtherEntities(player, searchBox);
        
        List<EntityScanResult> results = new ArrayList<>();
        
        for (Entity entity : entities) {
            String entityType = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
            
            // Фильтр по типу
            if (!matchesAnyType(entityType, targetTypes) && !matchesEntityCategory(entity, targetTypes)) {
                continue;
            }
            
            // Фильтр по Y
            if (options.yMin != null && entity.getBlockY() < options.yMin) continue;
            if (options.yMax != null && entity.getBlockY() > options.yMax) continue;
            
            // Фильтр по здоровью
            if (options.healthMin != null && entity instanceof LivingEntity living) {
                if (living.getHealth() < options.healthMin) continue;
            }
            
            // Фильтр по AI
            if (options.hasAI != null && entity instanceof net.minecraft.entity.mob.MobEntity mob) {
                if (mob.isAiDisabled() != options.hasAI) continue;
            }
            
            double dist = player.distanceTo(entity);
            float health = entity instanceof LivingEntity living ? living.getHealth() : 0;
            
            results.add(new EntityScanResult(entity, entityType, dist, health));
        }
        
        // Сортируем
        if ("distance".equals(options.sortBy)) {
            results.sort(Comparator.comparingDouble(r -> r.distance));
        } else if ("health".equals(options.sortBy)) {
            results.sort(Comparator.comparingDouble(r -> -r.health));
        }
        
        // Ограничиваем
        if (options.limit > 0 && results.size() > options.limit) {
            results = new ArrayList<>(results.subList(0, options.limit));
        }
        
        // Кэшируем
        scanCache.put(cacheKey, new ScanResult(null, results));
        
        // Применяем результаты
        applyEntityResults(results, interpreter, options);
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Entity scan: " + results.size() + " found (" + elapsed + "ms)");
    }
    
    private boolean matchesEntityCategory(Entity entity, Set<String> types) {
        for (String type : types) {
            switch (type.toLowerCase()) {
                case "hostile":
                    if (entity instanceof HostileEntity) return true;
                    break;
                case "passive":
                    if (entity instanceof PassiveEntity) return true;
                    break;
                case "living":
                    if (entity instanceof LivingEntity) return true;
                    break;
                case "merchant":
                case "villager":
                    if (entity instanceof MerchantEntity) return true;
                    break;
                case "*":
                case "all":
                    return true;
            }
        }
        return false;
    }
    
    private void applyBlockResults(List<BlockScanResult> results, ScriptInterpreter interpreter, ScanOptions options) {
        interpreter.setVariable("scanner_count", String.valueOf(results.size()));
        interpreter.setVariable("scanner_found", results.isEmpty() ? "false" : "true");
        
        // Группируем по типу блока
        Map<String, Integer> countByType = new HashMap<>();
        for (BlockScanResult r : results) {
            countByType.merge(r.blockId, 1, Integer::sum);
        }
        
        interpreter.setVariable("scanner_types", String.valueOf(countByType.size()));
        
        // Сохраняем первые N результатов
        int maxResults = Math.min(results.size(), 20);
        for (int i = 0; i < maxResults; i++) {
            BlockScanResult r = results.get(i);
            interpreter.setVariable("scanner_" + i + "_x", String.valueOf(r.pos.getX()));
            interpreter.setVariable("scanner_" + i + "_y", String.valueOf(r.pos.getY()));
            interpreter.setVariable("scanner_" + i + "_z", String.valueOf(r.pos.getZ()));
            interpreter.setVariable("scanner_" + i + "_block", r.blockId);
            interpreter.setVariable("scanner_" + i + "_dist", String.format("%.1f", r.distance));
        }
        
        // Ближайший
        if (!results.isEmpty()) {
            BlockScanResult nearest = results.get(0);
            interpreter.setVariable("scanner_nearest_x", String.valueOf(nearest.pos.getX()));
            interpreter.setVariable("scanner_nearest_y", String.valueOf(nearest.pos.getY()));
            interpreter.setVariable("scanner_nearest_z", String.valueOf(nearest.pos.getZ()));
            interpreter.setVariable("scanner_nearest_block", nearest.blockId);
            interpreter.setVariable("scanner_nearest_dist", String.format("%.1f", nearest.distance));
        }
    }
    
    private void applyEntityResults(List<EntityScanResult> results, ScriptInterpreter interpreter, ScanOptions options) {
        interpreter.setVariable("scanner_entity_count", String.valueOf(results.size()));
        interpreter.setVariable("scanner_entity_found", results.isEmpty() ? "false" : "true");
        
        int maxResults = Math.min(results.size(), 20);
        for (int i = 0; i < maxResults; i++) {
            EntityScanResult r = results.get(i);
            interpreter.setVariable("scanner_entity_" + i + "_type", r.entityType);
            interpreter.setVariable("scanner_entity_" + i + "_id", String.valueOf(r.entity.getId()));
            interpreter.setVariable("scanner_entity_" + i + "_x", String.format("%.1f", r.entity.getX()));
            interpreter.setVariable("scanner_entity_" + i + "_y", String.format("%.1f", r.entity.getY()));
            interpreter.setVariable("scanner_entity_" + i + "_z", String.format("%.1f", r.entity.getZ()));
            interpreter.setVariable("scanner_entity_" + i + "_dist", String.format("%.1f", r.distance));
            interpreter.setVariable("scanner_entity_" + i + "_health", String.format("%.1f", r.health));
        }
        
        if (!results.isEmpty()) {
            EntityScanResult nearest = results.get(0);
            interpreter.setVariable("scanner_nearest_entity_type", nearest.entityType);
            interpreter.setVariable("scanner_nearest_entity_x", String.format("%.1f", nearest.entity.getX()));
            interpreter.setVariable("scanner_nearest_entity_y", String.format("%.1f", nearest.entity.getY()));
            interpreter.setVariable("scanner_nearest_entity_z", String.format("%.1f", nearest.entity.getZ()));
            interpreter.setVariable("scanner_nearest_entity_dist", String.format("%.1f", nearest.distance));
        }
    }
    
    private void sortResults(List<BlockScanResult> results, BlockPos playerPos, ScanOptions options) {
        if ("distance".equals(options.sortBy) || options.sortBy == null) {
            results.sort(Comparator.comparingDouble(r -> r.distance));
        } else if ("count".equals(options.sortBy)) {
            // Группируем и сортируем по количеству
            Map<String, Long> counts = new HashMap<>();
            for (BlockScanResult r : results) {
                counts.merge(r.blockId, 1L, Long::sum);
            }
            results.sort((a, b) -> Long.compare(counts.get(b.blockId), counts.get(a.blockId)));
        }
    }
    
    private ScanOptions parseOptions(String[] args) {
        ScanOptions options = new ScanOptions();
        
        for (int i = 2; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                switch (parts[0]) {
                    case "radius":
                        try { options.radius = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        options.radius = Math.min(options.radius, 64);
                        break;
                    case "ymin":
                        try { options.yMin = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        break;
                    case "ymax":
                        try { options.yMax = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        break;
                    case "sortby":
                        options.sortBy = parts[1];
                        break;
                    case "limit":
                        try { options.limit = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                        break;
                    case "excludeair":
                        options.excludeAir = parts[1].equals("true");
                        break;
                    case "healthmin":
                        try { options.healthMin = Float.parseFloat(parts[1]); } catch (NumberFormatException ignored) {}
                        break;
                    case "hasai":
                        options.hasAI = parts[1].equals("true");
                        break;
                }
            }
        }
        
        return options;
    }
    
    private Set<String> parseTypes(String typesStr) {
        Set<String> types = new HashSet<>();
        for (String type : typesStr.split(",")) {
            types.add(type.trim().toLowerCase());
        }
        return types;
    }
    
    private boolean matchesAnyType(String id, Set<String> types) {
        for (String type : types) {
            if (type.equals("*") || type.equals("all")) return true;
            if (type.startsWith("*") && type.endsWith("*")) {
                if (id.contains(type.substring(1, type.length() - 1))) return true;
            } else if (type.startsWith("*")) {
                if (id.endsWith(type.substring(1))) return true;
            } else if (type.endsWith("*")) {
                if (id.startsWith(type.substring(0, type.length() - 1))) return true;
            } else {
                if (id.equals(type) || id.contains(type)) return true;
            }
        }
        return false;
    }
    
    private void handleCache(String[] args, ScriptInterpreter interpreter) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            scanCache.clear();
            System.out.println("Scanner cache cleared");
        } else {
            System.out.println("Scanner cache size: " + scanCache.size());
        }
        interpreter.setVariable("scanner_cache_size", String.valueOf(scanCache.size()));
    }
    
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        scanCache.entrySet().removeIf(e -> e.getValue().isExpired());
    }
    
    private void printHelp() {
        System.out.println("Scanner Command (Advanced):");
        System.out.println("  scanner blocks <types> [options]");
        System.out.println("    - Scan for blocks (types: diamond_ore,iron_ore or *_ore)");
        System.out.println("  scanner entities <types> [options]");
        System.out.println("    - Scan for entities (types: villager,zombie or hostile)");
        System.out.println("  scanner cache clear");
        System.out.println("    - Clear scan cache");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  radius=N - Search radius (max 64)");
        System.out.println("  yMin=N, yMax=N - Y range filter");
        System.out.println("  sortBy=distance|count|health - Sort results");
        System.out.println("  limit=N - Max results");
        System.out.println("  excludeAir=true/false - Exclude air blocks");
        System.out.println("  healthMin=N - Min health for entities");
        System.out.println("  hasAI=true/false - Filter by AI state");
    }
    
    // Вспомогательные классы
    
    private static class ScanOptions {
        int radius = 32;
        Integer yMin = null;
        Integer yMax = null;
        String sortBy = "distance";
        int limit = 0;
        boolean excludeAir = true;
        Float healthMin = null;
        Boolean hasAI = null;
    }
    
    private static class BlockScanResult {
        final BlockPos pos;
        final String blockId;
        final double distance;
        
        BlockScanResult(BlockPos pos, String blockId, double distance) {
            this.pos = pos;
            this.blockId = blockId;
            this.distance = distance;
        }
    }
    
    private static class EntityScanResult {
        final Entity entity;
        final String entityType;
        final double distance;
        final float health;
        
        EntityScanResult(Entity entity, String entityType, double distance, float health) {
            this.entity = entity;
            this.entityType = entityType;
            this.distance = distance;
            this.health = health;
        }
    }
    
    private static class ScanResult {
        final List<BlockScanResult> blockResults;
        final List<EntityScanResult> entityResults;
        final long timestamp;
        
        ScanResult(List<BlockScanResult> blockResults, List<EntityScanResult> entityResults) {
            this.blockResults = blockResults;
            this.entityResults = entityResults;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
    
    private static class ScanCacheKey {
        final String type;
        final Set<String> targetTypes;
        final int radius;
        final BlockPos playerPos;
        
        ScanCacheKey(String type, Set<String> targetTypes, ScanOptions options, BlockPos playerPos) {
            this.type = type;
            this.targetTypes = targetTypes;
            this.radius = options.radius;
            // Округляем позицию для лучшего кэширования
            this.playerPos = new BlockPos(
                playerPos.getX() / 4 * 4,
                playerPos.getY() / 4 * 4,
                playerPos.getZ() / 4 * 4
            );
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScanCacheKey that = (ScanCacheKey) o;
            return radius == that.radius && 
                   type.equals(that.type) && 
                   targetTypes.equals(that.targetTypes) &&
                   playerPos.equals(that.playerPos);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, targetTypes, radius, playerPos);
        }
    }
}

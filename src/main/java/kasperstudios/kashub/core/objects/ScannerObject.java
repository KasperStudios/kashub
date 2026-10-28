package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import kasperstudios.kashub.core.Callable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScannerObject - OO wrapper for scanner functionality.
 * Usage: scanner.blocks("diamond_ore", radius=32)
 */
public class ScannerObject extends Value implements Documentable {

    private static final String KEY_DIST = "dist";

    public ScannerObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("blocks", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : "*";
            int radius = args.size() > 1 ? args.get(1).asInt() : 32;

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.NULL;

            Level world = player.level();
            BlockPos playerPos = player.blockPosition();
            List<Value> results = new ArrayList<>();

            int chunkRadius = (radius / 16) + 1;
            ChunkPos playerChunk = new ChunkPos(playerPos);

            // Phase 1: Broad Scan
            for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
                for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                    scanChunk(world, new ChunkPos(playerChunk.x + cx, playerChunk.z + cz), playerPos, type, radius,
                            results);
                }
            }

            // Phase 2: Sort by distance
            results.sort((a, b) -> {
                double d1 = a.getMember(KEY_DIST).asDouble();
                double d2 = b.getMember(KEY_DIST).asDouble();
                return Double.compare(d1, d2);
            });

            // Phase 3: Fair Play Filter (Narrow Phase)
            if (kasperstudios.kashub.core.fairplay.FairPlayGuard.isFairPlayEnforced()) {
                List<Value> visibleResults = new ArrayList<>();
                int checks = 0;
                int maxChecks = 50; // Performance limit for raycasts

                for (Value val : results) {
                    if (checks >= maxChecks)
                        break; // Return what we found so far (partial result)

                    int x = val.getMember("x").asInt();
                    int y = val.getMember("y").asInt();
                    int z = val.getMember("z").asInt();
                    BlockPos pos = new BlockPos(x, y, z);

                    if (kasperstudios.kashub.core.fairplay.FairPlayGuard.validateBlockReach(player, pos)) {
                        visibleResults.add(val);
                    }
                    checks++;
                }
                return Value.of(visibleResults);
            }

            return Value.of(results);
        }));

        methods.put("entities", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : "living";
            int radius = args.size() > 1 ? args.get(1).asInt() : 32;

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.NULL;

            net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(radius);
            List<net.minecraft.world.entity.Entity> entities = player.level().getEntities(player, box);
            List<Value> results = new ArrayList<>();

            for (net.minecraft.world.entity.Entity entity : entities) {
                String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
                
                boolean match = false;
                if (type.equals("*") || type.equals("all")) match = true;
                else if (type.equals("living") && entity instanceof net.minecraft.world.entity.LivingEntity) match = true;
                else if (type.equals("monsters") && entity instanceof net.minecraft.world.entity.monster.Monster) match = true;
                else if (type.equals("players") && entity instanceof net.minecraft.world.entity.player.Player) match = true;
                else if (entityId.contains(type)) match = true;

                if (!match) continue;

                Map<String, Value> entityData = new HashMap<>();
                entityData.put("type", Value.of(entityId));
                entityData.put("id", Value.of(entity.getId()));
                entityData.put("x", Value.of(entity.getX()));
                entityData.put("y", Value.of(entity.getY()));
                entityData.put("z", Value.of(entity.getZ()));
                entityData.put(KEY_DIST, Value.of(player.distanceTo(entity)));
                
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    entityData.put("health", Value.of(living.getHealth()));
                }
                
                // Internal reference for combat methods
                entityData.put("_entity", Value.of(entity));
                
                results.add(Value.of(entityData));
            }

            // Sort by distance
            results.sort((a, b) -> {
                double d1 = a.getMember(KEY_DIST).asDouble();
                double d2 = b.getMember(KEY_DIST).asDouble();
                return Double.compare(d1, d2);
            });

            return Value.of(results);
        }));

        methods.put("getBlock", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3) return Value.NULL;
            BlockPos pos = new BlockPos(args.get(0).asInt(), args.get(1).asInt(), args.get(2).asInt());
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return Value.NULL;
            return Value.of(com.kasperstudios.khwrapper.KHWrapper.get().getBlockId(client.player.level().getBlockState(pos).getBlock()));
        }));

        methods.put("isSolid", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3) return Value.FALSE;
            BlockPos pos = new BlockPos(args.get(0).asInt(), args.get(1).asInt(), args.get(2).asInt());
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return Value.FALSE;
            return Value.of(client.level.getBlockState(pos).isSolid());
        }));

        methods.put("isAir", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3) return Value.FALSE;
            BlockPos pos = new BlockPos(args.get(0).asInt(), args.get(1).asInt(), args.get(2).asInt());
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return Value.FALSE;
            return Value.of(client.level.getBlockState(pos).isAir());
        }));

        return methods;
    }

    private static void scanChunk(Level world, ChunkPos chunkPos, BlockPos playerPos, String targetType, int radius,
            List<Value> results) {
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                double distXZ = Math
                        .sqrt(Math.pow(worldX - playerPos.getX(), 2) + Math.pow(worldZ - playerPos.getZ(), 2));
                if (distXZ > radius)
                    continue;

                for (int y = world.getMinBuildHeight(); y <= world.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    String blockId = com.kasperstudios.khwrapper.KHWrapper.get().getBlockId(world.getBlockState(pos).getBlock());

                    if (targetType.equals("*") || blockId.contains(targetType)) {
                        Map<String, Value> block = new HashMap<>();
                        block.put("x", Value.of(pos.getX()));
                        block.put("y", Value.of(pos.getY()));
                        block.put("z", Value.of(pos.getZ()));
                        block.put("id", Value.of(blockId));
                        block.put(KEY_DIST, Value.of(Math.sqrt(pos.distSqr(playerPos))));
                        results.add(Value.of(block));
                    }
                }
            }
        }
    }

    @Override
    public String getObjectName() {
        return "scanner";
    }

    @Override
    public String getObjectDescription() {
        return "Environmental scanner for locating blocks and entities nearby.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("blocks(type, [radius])", "Scans for blocks matching 'type' within the given radius (default 32). Use '*' for all blocks. Returns a list of block maps sorted by distance.");
        docs.put("entities(type, [radius])", "Scans for entities matching 'type' within the given radius (default 32). Types: '*', 'living', 'monsters', 'players', or entity ID. Returns a list of entity maps.");
        docs.put("getBlock(x, y, z)", "Returns the block ID string at the specified coordinates.");
        docs.put("isSolid(x, y, z)", "Returns true if the block at the specified coordinates is solid.");
        docs.put("isAir(x, y, z)", "Returns true if the block at the specified coordinates is air.");
        return docs;
    }
}

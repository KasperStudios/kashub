package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * ScannerObject - OO wrapper for scanner functionality.
 * Usage: scanner.blocks("diamond_ore", radius=32)
 */
public class ScannerObject extends Value {

    public ScannerObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("blocks", Value.of((Callable) (ctx, args) -> {
            String type = !args.isEmpty() ? args.get(0).asString() : "*";
            int radius = args.size() > 1 ? args.get(1).asInt() : 32;

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.NULL;

            World world = player.getWorld();
            BlockPos playerPos = player.getBlockPos();
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
                double d1 = a.getMember("dist").asDouble();
                double d2 = b.getMember("dist").asDouble();
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

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.NULL;

            net.minecraft.util.math.Box box = player.getBoundingBox().expand(radius);
            List<net.minecraft.entity.Entity> entities = player.getWorld().getOtherEntities(player, box);
            List<Value> results = new ArrayList<>();

            for (net.minecraft.entity.Entity entity : entities) {
                String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
                
                if (!type.equals("*") && !type.equals("all") && !entityId.contains(type))
                    continue;

                Map<String, Value> entityData = new HashMap<>();
                entityData.put("type", Value.of(entityId));
                entityData.put("id", Value.of(entity.getId()));
                entityData.put("x", Value.of(entity.getX()));
                entityData.put("y", Value.of(entity.getY()));
                entityData.put("z", Value.of(entity.getZ()));
                entityData.put("dist", Value.of(player.distanceTo(entity)));
                
                if (entity instanceof net.minecraft.entity.LivingEntity living) {
                    entityData.put("health", Value.of(living.getHealth()));
                }
                
                results.add(Value.of(entityData));
            }

            // Sort by distance
            results.sort((a, b) -> {
                double d1 = a.getMember("dist").asDouble();
                double d2 = b.getMember("dist").asDouble();
                return Double.compare(d1, d2);
            });

            return Value.of(results);
        }));

        return methods;
    }

    private static void scanChunk(World world, ChunkPos chunkPos, BlockPos playerPos, String targetType, int radius,
            List<Value> results) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                double distXZ = Math
                        .sqrt(Math.pow(worldX - playerPos.getX(), 2) + Math.pow(worldZ - playerPos.getZ(), 2));
                if (distXZ > radius)
                    continue;

                for (int y = world.getBottomY(); y <= world.getTopY(); y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    String blockId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).getPath();

                    if (targetType.equals("*") || blockId.contains(targetType)) {
                        Map<String, Value> block = new HashMap<>();
                        block.put("x", Value.of(pos.getX()));
                        block.put("y", Value.of(pos.getY()));
                        block.put("z", Value.of(pos.getZ()));
                        block.put("id", Value.of(blockId));
                        block.put("dist", Value.of(Math.sqrt(pos.getSquaredDistance(playerPos))));
                        results.add(Value.of(block));
                    }
                }
            }
        }
    }
}

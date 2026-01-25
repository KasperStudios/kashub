package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.services.modpack.CustomCraftingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class WorldObject extends Value {

    public WorldObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("getBlock", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3)
                return Value.NULL;
            int x = args.get(0).asInt();
            int y = args.get(1).asInt();
            int z = args.get(2).asInt();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null)
                return Value.NULL;

            BlockPos pos = new BlockPos(x, y, z);
            String id = Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).toString();
            return Value.of(id);
        }));

        methods.put("getTime", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null)
                return Value.of(0);
            return Value.of(client.world.getTimeOfDay());
        }));

        methods.put("getWeather", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null)
                return Value.of("unknown");
            if (client.world.isThundering())
                return Value.of("thunder");
            if (client.world.isRaining())
                return Value.of("rain");
            return Value.of("clear");
        }));

        // Recipes (Legacy CraftCommand)
        methods.put("defineRecipe", Value.of((Callable) (ctx, args) -> {
            // world.defineRecipe("shaped", "id", "AAA", " B ", " ", "A:stick", "B:diamond",
            // "result:diamond_shovel", 1)
            if (args.size() < 2)
                return Value.FALSE;
            String type = args.get(0).asString().toLowerCase();
            String id = args.get(1).asString();
            CustomCraftingManager manager = CustomCraftingManager.getInstance();

            try {
                if (type.equals("shaped")) {
                    if (args.size() < 5)
                        return Value.FALSE;
                    String[] pattern = new String[] {
                            args.get(2).asString(),
                            args.get(3).asString(),
                            args.get(4).asString()
                    };
                    Map<Character, String> ingredients = new HashMap<>();
                    int resultIdx = -1;
                    for (int i = 5; i < args.size(); i++) {
                        String s = args.get(i).asString();
                        if (s.startsWith("result:")) {
                            resultIdx = i;
                            break;
                        }
                        String[] parts = s.split(":");
                        if (parts.length == 2)
                            ingredients.put(parts[0].charAt(0), parts[1]);
                    }

                    if (resultIdx != -1) {
                        String result = args.get(resultIdx).asString().split(":")[1];
                        int count = args.size() > resultIdx + 1 ? args.get(resultIdx + 1).asInt() : 1;
                        manager.registerShapedRecipe(id, pattern, ingredients, result, count);
                        return Value.TRUE;
                    }
                } else if (type.equals("shapeless")) {
                    // Syntax: defineRecipe("shapeless", "id", "stick", "diamond", "result:sword",
                    // 1)
                    List<String> inputs = new ArrayList<>();
                    String result = "";
                    int count = 1;

                    for (int i = 2; i < args.size(); i++) {
                        String s = args.get(i).asString();
                        if (s.startsWith("result:")) {
                            result = s.split(":")[1];
                            count = args.size() > i + 1 ? args.get(i + 1).asInt() : 1;
                            break;
                        }
                        inputs.add(s);
                    }
                    if (!result.isEmpty()) {
                        manager.registerShapelessRecipe(id, inputs, result, count);
                        return Value.TRUE;
                    }
                } else if (type.equals("remove")) {
                    manager.unregisterRecipe(id);
                    return Value.TRUE;
                }
            } catch (Exception e) {
                return Value.FALSE;
            }
            return Value.FALSE;
        }));

        return methods;
    }
}

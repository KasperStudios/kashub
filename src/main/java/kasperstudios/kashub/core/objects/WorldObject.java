package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.services.modpack.CustomCraftingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kasperstudios.kashub.core.ExecutionOrigin;

public class WorldObject extends Value implements Documentable {

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

            Minecraft client = Minecraft.getInstance();
            if (client.level == null)
                return Value.NULL;

            BlockPos pos = new BlockPos(x, y, z);
            String id = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(pos).getBlock()).toString();
            return Value.of(id);
        }));

        methods.put("getTime", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null)
                return Value.of(0);
            return Value.of(client.level.getDayTime());
        }));

        methods.put("getWeather", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null)
                return Value.of("unknown");
            if (client.level.isThundering())
                return Value.of("thunder");
            if (client.level.isRaining())
                return Value.of("rain");
            return Value.of("clear");
        }));

        // Recipes (Legacy CraftCommand)
        methods.put("defineRecipe", Value.of((Callable) (ctx, args) -> {
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowWorldModification) {
                throw new SecurityException("Access Denied: World modification is disabled by server/config.");
            }
            if (ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: world.defineRecipe requires SERVER origin (Root/System level).");
            }
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

        // 2D Heading-Rotated Dynamic Radar
        methods.put("radar", Value.of((Callable) (ctx, args) -> {
            int radius = args.isEmpty() ? 10 : args.get(0).asInt();
            String radar = kasperstudios.kashub.services.PerceptionService.getInstance().render2DRadar(radius);
            return Value.of(radar);
        }));

        // 3D First-Person ASCII Raymarching View
        methods.put("viewAscii", Value.of((Callable) (ctx, args) -> {
            int w = args.isEmpty() ? 36 : args.get(0).asInt();
            int h = args.size() > 1 ? args.get(1).asInt() : 18;
            float fov = args.size() > 2 ? (float) args.get(2).asDouble() : 70.0f;
            String view = kasperstudios.kashub.services.PerceptionService.getInstance().render3DViewAscii(w, h, fov);
            return Value.of(view);
        }));

        methods.put("visor", methods.get("viewAscii"));

        return methods;
    }

    @Override
    public String getObjectName() {
        return "world";
    }

    @Override
    public String getObjectDescription() {
        return "Access world data such as blocks, time, weather, and dynamic spatial perception (radar, 3D ASCII visor).";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("getBlock(x, y, z)", "Returns the ID of the block at the specified coordinates.");
        docs.put("getTime()", "Returns the current world time.");
        docs.put("getWeather()", "Returns the current weather as a string: 'thunder', 'rain', or 'clear'.");
        docs.put("radar([radius=10])", "Generates a 2D heading-rotated ASCII radar with line-of-sight wall shadows and entity gaze arrows.");
        docs.put("viewAscii([w=36, h=18, fov=70])", "Renders a 3D first-person ASCII raymarched view from player eyes with depth shading and crosshair targeting.");
        docs.put("visor([w, h, fov])", "Alias for viewAscii().");
        docs.put("defineRecipe(type, id, ...)", "Registers or removes a custom recipe (requires SERVER origin).");
        return docs;
    }
}

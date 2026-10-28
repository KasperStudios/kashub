package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import kasperstudios.kashub.core.Callable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class GameObject extends Value implements Documentable {

    public GameObject() {
        super(createMethods());
    }

    public static boolean fullBrightEnabled = false;

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("setGamma", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            double gamma = args.get(0).asDouble();
            fullBrightEnabled = (gamma > 1.0);
            setGammaDirectly(Minecraft.getInstance().options.gamma(), gamma);
            return Value.TRUE;
        }));

        methods.put("fullBright", Value.of((Callable) (ctx, args) -> {
            fullBrightEnabled = true;
            setGammaDirectly(Minecraft.getInstance().options.gamma(), 100.0);
            return Value.TRUE;
        }));

        methods.put("setFov", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            int fov = args.get(0).asInt();
            Minecraft.getInstance().options.fov().set(fov);
            return Value.TRUE;
        }));

        return methods;
    }

    private static void setGammaDirectly(OptionInstance<Double> gammaOption, double value) {
        try {
            // Direct field access via AccessWidener: OptionInstance.value
            gammaOption.value = value;
            return;
        } catch (Throwable ignored) {}

        // Fallback: Reflection
        try {
            Field valueField = OptionInstance.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(gammaOption, value);
            return;
        } catch (Throwable ignored) {}

        try {
            Field valueField = OptionInstance.class.getDeclaredField("field_37862");
            valueField.setAccessible(true);
            valueField.set(gammaOption, value);
            return;
        } catch (Throwable ignored) {}

        gammaOption.set(value);
    }

    @Override
    public String getObjectName() {
        return "game";
    }

    @Override
    public String getObjectDescription() {
        return "Client-side game settings and rendering utilities.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("setGamma(value)", "Sets the game's brightness/gamma level directly.");
        docs.put("fullBright()", "Sets the game's gamma to maximum for full visibility.");
        docs.put("setFov(value)", "Changes the player's Field of View (FOV).");
        return docs;
    }
}

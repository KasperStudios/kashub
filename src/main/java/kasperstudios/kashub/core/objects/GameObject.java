package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class GameObject extends Value {

    public GameObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("setGamma", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            double gamma = args.get(0).asDouble();
            setGammaDirectly(MinecraftClient.getInstance().options.getGamma(), gamma);
            return Value.TRUE;
        }));

        methods.put("fullBright", Value.of((Callable) (ctx, args) -> {
            setGammaDirectly(MinecraftClient.getInstance().options.getGamma(), 100.0);
            return Value.TRUE;
        }));

        methods.put("setFov", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            int fov = args.get(0).asInt();
            MinecraftClient.getInstance().options.getFov().setValue(fov);
            return Value.TRUE;
        }));

        return methods;
    }

    private static void setGammaDirectly(SimpleOption<Double> gammaOption, double value) {
        try {
            Field valueField = SimpleOption.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(gammaOption, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If reflection fails, just use normal setValue (will be clamped)
            gammaOption.setValue(Math.min(value, 1.0));
        }
    }
}

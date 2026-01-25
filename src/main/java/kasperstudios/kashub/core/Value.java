package kasperstudios.kashub.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Value - Unified value type for the engine.
 * Supports numbers, strings, booleans, lists, objects, and callables.
 */
public class Value {
    public static final Value NULL = new Value(null);
    public static final Value TRUE = new Value(true);
    public static final Value FALSE = new Value(false);

    private final Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean asBoolean() {
        if (value == null)
            return false;
        if (value instanceof Boolean)
            return (Boolean) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue() != 0;
        if (value instanceof String) {
            String s = (String) value;
            return !s.isEmpty() && !s.equalsIgnoreCase("false") && !s.equals("0");
        }
        if (value instanceof List)
            return !((List<?>) value).isEmpty();
        if (value instanceof Map)
            return !((Map<?, ?>) value).isEmpty();
        return true;
    }

    public int asInt() {
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public double asDouble() {
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    public String asString() {
        return toString();
    }

    @SuppressWarnings("unchecked")
    public List<Value> asList() {
        if (value instanceof List)
            return (List<Value>) value;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Value> asObject() {
        if (value instanceof Map)
            return (Map<String, Value>) value;
        return Collections.emptyMap();
    }

    public boolean isList() {
        return value instanceof List;
    }

    public boolean isObject() {
        return value instanceof Map;
    }

    public boolean isCallable() {
        return value instanceof Callable;
    }

    public Value call(Context ctx, List<Value> args) throws Exception {
        if (value instanceof Callable) {
            return ((Callable) value).call(ctx, args);
        }
        throw new Exception("Value is not callable: " + toString());
    }

    public Value getMember(String name) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Object res = map.get(name);
            return res != null ? Value.of(res) : NULL;
        }

        // Proto methods (Generic)
        if (name.equals("toString")) {
            return Value.of((Callable) (ctx, args) -> Value.of(toString()));
        }

        // String Proto
        if (value instanceof String) {
            String s = (String) value;
            switch (name) {
                case "length":
                    return Value.of((Callable) (ctx, args) -> Value.of(s.length()));
                case "substring":
                    return Value.of((Callable) (ctx, args) -> {
                        if (args.isEmpty())
                            return Value.of(s);
                        int start = args.get(0).asInt();
                        if (args.size() == 1)
                            return Value.of(s.substring(start));
                        return Value.of(s.substring(start, args.get(1).asInt()));
                    });
                case "toLowerCase":
                    return Value.of((Callable) (ctx, args) -> Value.of(s.toLowerCase()));
                case "toUpperCase":
                    return Value.of((Callable) (ctx, args) -> Value.of(s.toUpperCase()));
                case "contains":
                    return Value.of((Callable) (ctx, args) -> Value
                            .of(!args.isEmpty() && s.contains(args.get(0).asString())));
            }
        }

        // List Proto
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            switch (name) {
                case "length":
                case "size":
                    return Value.of((Callable) (ctx, args) -> Value.of(list.size()));
                case "get":
                    return Value.of((Callable) (ctx, args) -> {
                        int idx = !args.isEmpty() ? args.get(0).asInt() : -1;
                        if (idx >= 0 && idx < list.size())
                            return Value.of(list.get(idx));
                        return NULL;
                    });
            }
        }

        return NULL;
    }

    public Value getIndex(Value index) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int idx = index.asInt();
            if (idx >= 0 && idx < list.size())
                return Value.of(list.get(idx));
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Object res = map.get(index.asString());
            return res != null ? Value.of(res) : NULL;
        }
        return NULL;
    }

    @Override
    public String toString() {
        if (value == null)
            return "null";
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == (long) d)
                return String.format("%d", (long) d);
        }
        if (value instanceof List) {
            return value.toString();
        }
        if (value instanceof Map) {
            return "{...}";
        }
        return value.toString();
    }

    public static Value of(Object value) {
        if (value == null)
            return NULL;
        if (value instanceof Value)
            return (Value) value;
        if (value instanceof Boolean)
            return (Boolean) value ? TRUE : FALSE;
        return new Value(value);
    }
}

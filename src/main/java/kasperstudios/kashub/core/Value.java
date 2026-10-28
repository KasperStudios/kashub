package kasperstudios.kashub.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Value - Unified value type for the engine.
 * Supports numbers, strings, booleans, lists, objects, and callables.
 */
public class Value {
    private static final Logger LOGGER = LoggerFactory.getLogger(Value.class);
    public static final Value NULL = new Value(null);
    public static final Value TRUE = new Value(true);
    public static final Value FALSE = new Value(false);

    private final Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Map<String, Object> getHighlighting() {
        if (value instanceof Map) {
            Map<String, Object> highlight = new java.util.HashMap<>();
            ((Map<?, ?>) value).keySet().forEach(k -> highlight.put(k.toString(), "function"));
            return highlight;
        }
        return Map.of();
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

    public boolean isMap() {
        return value instanceof Map;
    }

    public boolean isCallable() {
        return value instanceof Callable;
    }

    public boolean isNumber() {
        return value instanceof Number;
    }

    public boolean isString() {
        return value instanceof String;
    }

    public boolean isBoolean() {
        return value instanceof Boolean;
    }

    public Value call(Context ctx, List<Value> args) throws Exception {
        if (value instanceof Callable) {
            return ((Callable) value).call(ctx, args);
        }
        if (value instanceof Number && args.isEmpty()) {
            return this;
        }
        throw new Exception("Value is not callable: " + toString());
    }

    public void setMember(String name, Value value) {
        if (this.value instanceof Map) {
            ((Map<String, Object>) this.value).put(name, value.getValue());
        }
    }

    private static final Callable TO_STRING_CALLABLE = (ctx, args) -> Value.of("Value"); // Placeholder, will fix properly in prototype expansion

    public Value getMember(String name) {

        // Proto methods (Generic)
        if (name.equals("toString")) {
            return Value.of((Callable) (ctx, args) -> Value.of(toString()));
        }

        // String Proto
        if (value instanceof String) {
            String s = (String) value;
            return getStringProto(s, name);
        }

        // List Proto
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return getListProto(list, name);
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Value proto = getMapProto(map, name);
            if (proto != NULL) return proto;
            
            // Fallback: Treat as property access
            Object res = map.get(name);
            return res != null ? Value.of(res) : NULL;
        }

        return NULL;
    }

    private Value getMapProto(Map<?, ?> map, String name) {
        switch (name) {
            case "get":
                return Value.of((Callable) (ctx, args) -> {
                    if (args.isEmpty()) return NULL;
                    String key = args.get(0).asString();
                    Object res = map.get(key);
                    return res != null ? Value.of(res) : NULL;
                });
            case "size":
            case "length":
                return new Value(new CallableNumber(map.size()));
            case "isEmpty":
                return Value.of((Callable) (ctx, args) -> Value.of(map.isEmpty()));
            case "containsKey":
            case "has":
                return Value.of((Callable) (ctx, args) -> Value.of(!args.isEmpty() && map.containsKey(args.get(0).asString())));
        }
        return NULL;
    }

    private Value getStringProto(String s, String name) {
        switch (name) {
            case "length":
            case "size":
                return new Value(new CallableNumber(s.length()));
            case "substring":
                return Value.of((Callable) (ctx, args) -> {
                    if (args.isEmpty())
                        return Value.of(s);
                    int start = args.get(0).asInt();
                    if (args.size() == 1)
                        return Value.of(s.substring(Math.max(0, Math.min(start, s.length()))));
                    int end = args.get(1).asInt();
                    return Value.of(s.substring(Math.max(0, Math.min(start, s.length())), 
                                             Math.max(0, Math.min(end, s.length()))));
                });
            case "toLowerCase":
                return Value.of((Callable) (ctx, args) -> Value.of(s.toLowerCase()));
            case "toUpperCase":
                return Value.of((Callable) (ctx, args) -> Value.of(s.toUpperCase()));
            case "contains":
                return Value.of((Callable) (ctx, args) -> Value
                        .of(!args.isEmpty() && s.contains(args.get(0).asString())));
            case "trim":
                return Value.of((Callable) (ctx, args) -> Value.of(s.trim()));
            case "get":
                return Value.of((Callable) (ctx, args) -> {
                    if (args.isEmpty()) return NULL;
                    String key = args.get(0).asString();
                    return getMember(key);
                });
            case "split":
                return Value.of((Callable) (ctx, args) -> {
                    if (args.isEmpty()) return Value.of(List.of(Value.of(s)));
                    String sep = args.get(0).asString();
                    String[] parts = s.split(java.util.regex.Pattern.quote(sep));
                    List<Value> res = new java.util.ArrayList<>();
                    for (String p : parts) res.add(Value.of(p));
                    return Value.of(res);
                });
        }
        return NULL;
    }

    private Value getListProto(List<?> list, String name) {
        switch (name) {
            case "length":
            case "size":
                return new Value(new CallableNumber(list.size()));
            case "isEmpty":
                return Value.of((Callable) (ctx, args) -> Value.of(list.isEmpty()));
            case "get":
                return Value.of((Callable) (ctx, args) -> {
                    int idx = !args.isEmpty() ? args.get(0).asInt() : -1;
                    if (idx >= 0 && idx < list.size())
                        return Value.of(list.get(idx));
                    return NULL;
                });
            case "add":
            case "push":
                return Value.of((Callable) (ctx, args) -> {
                    if (!args.isEmpty()) {
                        // This assumes the list is mutable! ConcurrentHashMap or ArrayList
                        try {
                            ((List<Object>) list).add(args.get(0).getValue());
                        } catch (Exception ignored) {
                            LOGGER.warn("Failed to add element to list: {}", ignored.getMessage());
                        }
                    }
                    return Value.of(list.size());
                });
            case "clear":
                return Value.of((Callable) (ctx, args) -> {
                    try {
                        ((List<?>) list).clear();
                    } catch (Exception ignored) {
                        LOGGER.warn("Failed to clear list: {}", ignored.getMessage());
                    }
                    return NULL;
                });
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value1 = (Value) o;
        if (value == null || value1.value == null) return value == value1.value;
        
        // Handle number comparison across different types (Integer, Double, etc.)
        if (value instanceof Number && value1.value instanceof Number) {
            return Double.compare(((Number) value).doubleValue(), ((Number) value1.value).doubleValue()) == 0;
        }
        
        return value.equals(value1.value);
    }

    @Override
    public int hashCode() {
        if (value == null) return 0;
        if (value instanceof Number) {
            return Double.hashCode(((Number) value).doubleValue());
        }
        return value.hashCode();
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
            return value.toString();
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

    public static class CallableNumber extends Number implements Callable {
        private final double val;

        public CallableNumber(double val) {
            this.val = val;
        }

        @Override
        public Value call(Context ctx, List<Value> args) {
            return Value.of(val);
        }

        @Override
        public int intValue() {
            return (int) val;
        }

        @Override
        public long longValue() {
            return (long) val;
        }

        @Override
        public float floatValue() {
            return (float) val;
        }

        @Override
        public double doubleValue() {
            return val;
        }

        @Override
        public String toString() {
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Number) {
                return Double.compare(val, ((Number) o).doubleValue()) == 0;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(val);
        }
    }
}

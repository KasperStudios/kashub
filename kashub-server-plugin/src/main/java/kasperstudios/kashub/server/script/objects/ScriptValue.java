package kasperstudios.kashub.server.script.objects;

public class ScriptValue {
    public static final ScriptValue NULL = new ScriptValue(null);

    private final Object value;

    public ScriptValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public boolean asBoolean() {
        if (value instanceof Boolean)
            return (Boolean) value;
        if (value instanceof Number)
            return ((Number) value).doubleValue() != 0;
        return value != null && !value.toString().equalsIgnoreCase("false");
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

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }
}

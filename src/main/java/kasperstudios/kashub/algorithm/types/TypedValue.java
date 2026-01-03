package kasperstudios.kashub.algorithm.types;

public class TypedValue {
    private final String value;
    private final KHType type;
    private final KHType declaredType;

    public TypedValue(String value) {
        this.value = value;
        this.type = inferType(value);
        this.declaredType = KHType.ANY;
    }

    public TypedValue(String value, KHType declaredType) {
        this.value = value;
        this.type = inferType(value);
        this.declaredType = declaredType;
    }

    public String getValue() {
        return value;
    }

    public KHType getType() {
        return type;
    }

    public KHType getDeclaredType() {
        return declaredType;
    }

    private static KHType inferType(String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("null")) {
            return KHType.NULL;
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return KHType.BOOL;
        }

        try {
            Double.parseDouble(value.replace(',', '.'));
            return KHType.NUMBER;
        } catch (NumberFormatException ignored) {}

        String[] parts = value.split("[,\\s]+");
        if (parts.length == 3) {
            boolean allNumbers = true;
            for (String part : parts) {
                try {
                    Double.parseDouble(part.trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    allNumbers = false;
                    break;
                }
            }
            if (allNumbers) {
                return KHType.POSITION;
            }
        }

        return KHType.STRING;
    }

    public boolean isValid() {
        if (declaredType == KHType.ANY) {
            return true;
        }
        return declaredType.isCompatibleWith(type);
    }

    public double asNumber() {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public int asInt() {
        return (int) asNumber();
    }

    public boolean asBool() {
        return value.equalsIgnoreCase("true") ||
               (!value.isEmpty() && !value.equals("0") && !value.equalsIgnoreCase("false"));
    }

    public String asString() {
        return value;
    }

    public double[] asPosition() {
        String[] parts = value.split("[,\\s]+");
        if (parts.length != 3) {
            return new double[]{0, 0, 0};
        }
        try {
            return new double[]{
                Double.parseDouble(parts[0].trim().replace(',', '.')),
                Double.parseDouble(parts[1].trim().replace(',', '.')),
                Double.parseDouble(parts[2].trim().replace(',', '.'))
            };
        } catch (NumberFormatException e) {
            return new double[]{0, 0, 0};
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

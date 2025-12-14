package kasperstudios.kashub.algorithm.types;

/**
 * Обёртка для значения с информацией о типе
 */
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
    
    /**
     * Автоматически определяет тип значения
     */
    private static KHType inferType(String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("null")) {
            return KHType.NULL;
        }
        
        // Проверяем boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return KHType.BOOL;
        }
        
        // Проверяем число
        try {
            Double.parseDouble(value.replace(',', '.'));
            return KHType.NUMBER;
        } catch (NumberFormatException ignored) {}
        
        // Проверяем позицию (x, y, z)
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
        
        // По умолчанию — строка
        return KHType.STRING;
    }
    
    /**
     * Проверяет, соответствует ли значение объявленному типу
     */
    public boolean isValid() {
        if (declaredType == KHType.ANY) {
            return true;
        }
        return declaredType.isCompatibleWith(type);
    }
    
    /**
     * Конвертирует значение в число
     */
    public double asNumber() {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Конвертирует значение в int
     */
    public int asInt() {
        return (int) asNumber();
    }
    
    /**
     * Конвертирует значение в boolean
     */
    public boolean asBool() {
        return value.equalsIgnoreCase("true") || 
               (!value.isEmpty() && !value.equals("0") && !value.equalsIgnoreCase("false"));
    }
    
    /**
     * Возвращает значение как строку
     */
    public String asString() {
        return value;
    }
    
    /**
     * Парсит позицию в массив координат
     */
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

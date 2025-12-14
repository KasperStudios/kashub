package kasperstudios.kashub.algorithm.types;

/**
 * Базовые типы для KHScript
 * Используются для опциональной типизации переменных
 */
public enum KHType {
    // Примитивные типы
    NUMBER("number", "Числовое значение (int или float)"),
    STRING("string", "Строковое значение"),
    BOOL("bool", "Логическое значение (true/false)"),
    
    // Игровые типы
    POSITION("position", "Координаты в мире (x, y, z)"),
    ITEM("item", "Предмет в инвентаре"),
    ENTITY("entity", "Сущность в мире"),
    BLOCK("block", "Блок в мире"),
    
    // Специальные типы для API
    TRADE_OFFER("TradeOffer", "Предложение торговли у жителя"),
    RECIPE("Recipe", "Рецепт крафта"),
    PATH("Path", "Путь для навигации"),
    
    // Коллекции
    LIST("list", "Список значений"),
    MAP("map", "Словарь ключ-значение"),
    
    // Специальные
    ANY("any", "Любой тип (без проверки)"),
    VOID("void", "Отсутствие значения"),
    NULL("null", "Пустое значение");
    
    private final String name;
    private final String description;
    
    KHType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Парсит строку в тип
     */
    public static KHType fromString(String str) {
        if (str == null || str.isEmpty()) {
            return ANY;
        }
        
        String lower = str.toLowerCase().trim();
        for (KHType type : values()) {
            if (type.name.equalsIgnoreCase(lower)) {
                return type;
            }
        }
        return ANY;
    }
    
    /**
     * Проверяет, является ли значение валидным для данного типа
     */
    public boolean isValidValue(String value) {
        if (this == ANY || this == NULL) {
            return true;
        }
        
        if (value == null || value.isEmpty()) {
            return this == NULL || this == VOID;
        }
        
        switch (this) {
            case NUMBER:
                return isNumber(value);
            case BOOL:
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            case STRING:
                return true;
            case POSITION:
                return isPosition(value);
            default:
                return true;
        }
    }
    
    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value.replace(',', '.'));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static boolean isPosition(String value) {
        String[] parts = value.split("[,\\s]+");
        if (parts.length != 3) {
            return false;
        }
        for (String part : parts) {
            if (!isNumber(part.trim())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Проверяет совместимость типов
     */
    public boolean isCompatibleWith(KHType other) {
        if (this == ANY || other == ANY) {
            return true;
        }
        if (this == other) {
            return true;
        }
        // NUMBER совместим с STRING (автоконвертация)
        if ((this == NUMBER && other == STRING) || (this == STRING && other == NUMBER)) {
            return true;
        }
        return false;
    }
}

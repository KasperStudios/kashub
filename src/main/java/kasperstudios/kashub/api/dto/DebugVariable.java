package kasperstudios.kashub.api.dto;

public class DebugVariable {

    public String name;

    public String value;

    public String type;

    public int variablesReference;

    public Integer namedVariables;

    public Integer indexedVariables;

    public String presentationHint;

    public String memoryReference;

    public DebugVariable() {
    }

    public DebugVariable(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.variablesReference = 0;
    }

    public DebugVariable(String name, String value, String type, int variablesReference) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.variablesReference = variablesReference;
    }

    public static String inferType(String value) {
        if (value == null || value.equals("null")) {
            return "null";
        }

        try {
            Double.parseDouble(value);
            return "number";
        } catch (NumberFormatException e) {

        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "boolean";
        }

        return "string";
    }
}

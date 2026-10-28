package kasperstudios.kashub.api.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugVariable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugVariable.class);

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
            // Value is not a number, fall through to other type checks
            LOGGER.debug("Value '{}' is not a number: {}", value, e.getMessage());
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "boolean";
        }

        return "string";
    }
}

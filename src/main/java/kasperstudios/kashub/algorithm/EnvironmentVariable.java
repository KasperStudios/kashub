package kasperstudios.kashub.algorithm;

public class EnvironmentVariable {
    private final String name;
    private String value;
    private final String description;

    public EnvironmentVariable(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }
} 
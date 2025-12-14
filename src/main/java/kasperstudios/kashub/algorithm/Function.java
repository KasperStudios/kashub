package kasperstudios.kashub.algorithm;

import java.util.List;
import java.util.ArrayList;

public class Function {
    private final String name;
    private final List<String> parameters;
    private final String body;

    public Function(String name, List<String> parameters, String body) {
        this.name = name;
        this.parameters = new ArrayList<>(parameters);
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getBody() {
        return body;
    }
} 
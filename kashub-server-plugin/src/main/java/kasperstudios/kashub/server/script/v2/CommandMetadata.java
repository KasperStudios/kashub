package kasperstudios.kashub.server.script.v2;

import java.util.ArrayList;
import java.util.List;

public class CommandMetadata {
    public String name;
    public String category;
    public String pattern; // Regex pattern for validation
    public String syntax; // Human readable usage: "if (condition) {"
    public String description; // Tooltip help
    public List<String> examples;

    public CommandMetadata(String name, String category, String pattern, String syntax, String description) {
        this.name = name;
        this.category = category;
        this.pattern = pattern;
        this.syntax = syntax;
        this.description = description;
        this.examples = new ArrayList<>();
    }

    public CommandMetadata addExample(String example) {
        this.examples.add(example);
        return this;
    }
}

package kasperstudios.kashub.gui;

import java.util.ArrayList;
import java.util.List;

public class ServerCommandMetadata {
    public String name;
    public String category;
    public String pattern;
    public String syntax;
    public String description;
    public List<String> examples;

    public ServerCommandMetadata(String name, String category, String pattern, String syntax, String description) {
        this.name = name;
        this.category = category;
        this.pattern = pattern;
        this.syntax = syntax;
        this.description = description;
        this.examples = new ArrayList<>();
    }

    public ServerCommandMetadata() {
        // Default constructor for serialization
        this.examples = new ArrayList<>();
    }

    public void addExample(String example) {
        this.examples.add(example);
    }
}

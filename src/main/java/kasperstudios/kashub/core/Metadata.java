package kasperstudios.kashub.core;

import java.util.ArrayList;
import java.util.List;

public class Metadata {
    public final String name;
    public final String category;
    public final String pattern;
    public final String syntax;
    public final String description;
    public final List<String> examples;

    public Metadata(String name, String category, String pattern, String syntax, String description, List<String> examples) {
        this.name = name;
        this.category = category;
        this.pattern = pattern;
        this.syntax = syntax;
        this.description = description;
        this.examples = new ArrayList<>(examples);
    }
}

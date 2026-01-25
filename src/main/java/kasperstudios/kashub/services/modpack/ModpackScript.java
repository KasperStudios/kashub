package kasperstudios.kashub.services.modpack;

import java.nio.file.Path;

/**
 * ModpackScript - Represents a server-side/modpack script.
 * 
 * Modpack scripts are mandatory scripts that run on server or in modpack context.
 * They can define custom crafting recipes, global events, and persistent data.
 * 
 * Part of v0.9.0 Server & Modpack Scripts feature.
 * 
 * @since 0.9.0
 */
public class ModpackScript {
    
    private final String name;
    private final String content;
    private final Path filePath;
    private final boolean mandatory;
    private final int priority;
    
    public ModpackScript(String name, String content, Path filePath) {
        this(name, content, filePath, true, 0);
    }
    
    public ModpackScript(String name, String content, Path filePath, boolean mandatory, int priority) {
        this.name = name;
        this.content = content;
        this.filePath = filePath;
        this.mandatory = mandatory;
        this.priority = priority;
    }
    
    public String getName() {
        return name;
    }
    
    public String getContent() {
        return content;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public boolean isMandatory() {
        return mandatory;
    }
    
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String toString() {
        return "ModpackScript{" +
                "name='" + name + '\'' +
                ", mandatory=" + mandatory +
                ", priority=" + priority +
                '}';
    }
}

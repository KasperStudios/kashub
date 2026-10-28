package kasperstudios.kashub.core.objects;

import java.util.Map;

/**
 * Interface for Built-in Objects to provide dynamic documentation
 * for the KasHub IDE Editor.
 */
public interface Documentable {
    
    /**
     * @return The variable name of the object in the script (e.g., "player", "world").
     */
    String getObjectName();

    /**
     * @return A brief summary of the object's purpose and capabilities.
     */
    String getObjectDescription();

    /**
     * @return A map where keys are method signatures (e.g., "moveTo(x, y, z)") 
     *         and values are descriptions of what the method does.
     */
    Map<String, String> getMethodDescriptions();
}

package kasperstudios.kashub.algorithm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages exported variables from scripts for cross-script sharing.
 * This is the first step towards a full package system.
 */
public class ExportManager {
    private static ExportManager instance;
    
    // scriptName -> (variableName -> value)
    private final Map<String, Map<String, String>> exports = new ConcurrentHashMap<>();
    
    private ExportManager() {
    }
    
    public static ExportManager getInstance() {
        if (instance == null) {
            instance = new ExportManager();
        }
        return instance;
    }
    
    /**
     * Export a variable from a script
     * @param scriptName Name of the script exporting the variable
     * @param variableName Name of the variable to export
     * @param value Value of the variable
     */
    public void export(String scriptName, String variableName, String value) {
        exports.computeIfAbsent(scriptName, k -> new ConcurrentHashMap<>())
               .put(variableName, value);
        
        kasperstudios.kashub.util.ScriptLogger.getInstance()
            .debug("Exported: " + scriptName + "." + variableName + " = " + value);
    }
    
    /**
     * Import a variable from another script
     * @param scriptName Name of the script to import from
     * @param variableName Name of the variable to import
     * @return The value of the variable, or null if not found
     */
    public String importVariable(String scriptName, String variableName) {
        Map<String, String> scriptExports = exports.get(scriptName);
        if (scriptExports != null) {
            return scriptExports.get(variableName);
        }
        return null;
    }
    
    /**
     * Check if a script has exported a variable
     * @param scriptName Name of the script
     * @param variableName Name of the variable
     * @return true if the variable is exported
     */
    public boolean hasExport(String scriptName, String variableName) {
        Map<String, String> scriptExports = exports.get(scriptName);
        return scriptExports != null && scriptExports.containsKey(variableName);
    }
    
    /**
     * Get all exports from a script
     * @param scriptName Name of the script
     * @return Map of variable names to values, or empty map if no exports
     */
    public Map<String, String> getScriptExports(String scriptName) {
        return exports.getOrDefault(scriptName, new ConcurrentHashMap<>());
    }
    
    /**
     * Clear all exports from a script (called when script stops)
     * @param scriptName Name of the script
     */
    public void clearScriptExports(String scriptName) {
        exports.remove(scriptName);
        kasperstudios.kashub.util.ScriptLogger.getInstance()
            .debug("Cleared exports for script: " + scriptName);
    }
    
    /**
     * Clear all exports
     */
    public void clearAll() {
        exports.clear();
    }
    
    /**
     * Get all exported scripts
     * @return Set of script names that have exports
     */
    public java.util.Set<String> getExportedScripts() {
        return exports.keySet();
    }
}

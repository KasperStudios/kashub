package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ExportManager;
import kasperstudios.kashub.algorithm.ScriptInterpreter;

/**
 * Import command - imports exported variables from other scripts
 * 
 * Syntax: import variableName from scriptName
 * Example: import myVar from script1
 */
public class ImportCommand implements Command {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getDescription() {
        return "Import exported variables from other scripts";
    }

    @Override
    public String getParameters() {
        return "<variableName> from <scriptName>";
    }

    @Override
    public String getCategory() {
        return "Variables";
    }

    @Override
    public String getDetailedHelp() {
        return "Import exported variables from other scripts.\n\n" +
               "This is the first step towards a full package system.\n" +
               "You can import variables that were exported by other scripts using the 'export' command.\n\n" +
               "Usage:\n" +
               "  import <variableName> from <scriptName>\n" +
               "  import <var1>, <var2> from <scriptName>\n\n" +
               "Examples:\n" +
               "  import myVar from script1\n" +
               "  import playerName, coords from script1\n" +
               "  print $myVar\n\n" +
               "Note: The source script must be running and have exported the variable.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: import <variableName> from <scriptName>");
        }

        // Parse: import var1, var2 from scriptName
        // or: import var1 from scriptName
        
        int fromIndex = -1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("from")) {
                fromIndex = i;
                break;
            }
        }
        
        if (fromIndex == -1 || fromIndex == 0 || fromIndex >= args.length - 1) {
            throw new IllegalArgumentException("Usage: import <variableName> from <scriptName>");
        }
        
        String scriptName = args[fromIndex + 1];
        
        // Remove .kh extension if present
        if (scriptName.endsWith(".kh")) {
            scriptName = scriptName.substring(0, scriptName.length() - 3);
        }
        
        // Parse variable names (may be comma-separated)
        java.util.List<String> variableNames = new java.util.ArrayList<>();
        for (int i = 0; i < fromIndex; i++) {
            String arg = args[i].trim();
            if (arg.endsWith(",")) {
                arg = arg.substring(0, arg.length() - 1);
            }
            if (!arg.isEmpty()) {
                variableNames.add(arg);
            }
        }
        
        if (variableNames.isEmpty()) {
            throw new IllegalArgumentException("No variable names specified");
        }
        
        // Import each variable
        ExportManager exportManager = ExportManager.getInstance();
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        for (String variableName : variableNames) {
            String value = exportManager.importVariable(scriptName, variableName);
            
            if (value == null) {
                throw new IllegalArgumentException(
                    "Variable '" + variableName + "' not exported by script '" + scriptName + "'");
            }
            
            interpreter.setVariable(variableName, value);
            System.out.println("Imported: " + variableName + " = " + value + " (from " + scriptName + ")");
        }
    }
}

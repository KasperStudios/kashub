package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ExportManager;
import kasperstudios.kashub.algorithm.ScriptInterpreter;

/**
 * Export command - exports a variable for use in other scripts
 * 
 * Syntax: export variableName value
 * Example: export myVar 100
 */
public class ExportCommand implements Command {

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public String getDescription() {
        return "Export a variable for use in other scripts";
    }

    @Override
    public String getParameters() {
        return "<variableName> <value>";
    }

    @Override
    public String getCategory() {
        return "Variables";
    }

    @Override
    public String getDetailedHelp() {
        return "Export a variable for use in other scripts.\n\n" +
               "This is the first step towards a full package system.\n" +
               "Exported variables can be imported by other scripts using the 'import' command.\n\n" +
               "Usage:\n" +
               "  export <variableName> <value>\n\n" +
               "Examples:\n" +
               "  export myVar 100\n" +
               "  export playerName \"Kasper\"\n" +
               "  export coords \"100,64,200\"\n\n" +
               "Then in another script:\n" +
               "  import myVar from script1\n" +
               "  print $myVar  // Outputs: 100\n\n" +
               "Note: Exports are cleared when the script stops.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: export <variableName> <value>");
        }

        String variableName = args[0];
        String value = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        
        // Remove quotes if present
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        
        String scriptName = ScriptInterpreter.getInstance().getCurrentScriptName();
        ExportManager.getInstance().export(scriptName, variableName, value);
        
        // Also set as regular variable in current script
        ScriptInterpreter.getInstance().setVariable(variableName, value);
        
        System.out.println("Exported: " + scriptName + "." + variableName + " = " + value);
    }
}

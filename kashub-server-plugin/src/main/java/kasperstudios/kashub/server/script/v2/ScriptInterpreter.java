package kasperstudios.kashub.server.script.v2;

import kasperstudios.kashub.server.script.v2.commands.core.AssignmentCommand;
import kasperstudios.kashub.server.script.v2.commands.core.VariableDeclarationCommand;
import kasperstudios.kashub.server.script.v2.commands.flow.IfCommand;
import kasperstudios.kashub.server.script.v2.commands.flow.WhileCommand;
import kasperstudios.kashub.server.script.v2.commands.std.PrintCommand;

import java.util.ArrayList;
import java.util.List;

public class ScriptInterpreter {

    // Initialize the command registry
    static {
        // Register commands in priority order
        // Flow control first
        CommandRegistry.register(new IfCommand());
        CommandRegistry.register(new WhileCommand());

        // Then variable handling
        CommandRegistry.register(new VariableDeclarationCommand());

        // Standard library
        CommandRegistry.register(new PrintCommand());

        // Assignment must be last (most generic pattern)
        CommandRegistry.register(new AssignmentCommand());
    }

    /**
     * Execute a list of script lines
     */
    public static void execute(List<String> lines, ScriptContext ctx) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("//"))
                continue;

            // Find matching command
            ScriptCommand cmd = CommandRegistry.findMatch(line);
            if (cmd == null) {
                System.err.println("[KasHub Script] Unknown command: " + line);
                continue;
            }

            // Extract block body if line ends with {
            List<String> blockBody = null;
            if (line.endsWith("{")) {
                BlockResult block = extractBlock(lines, i + 1);
                blockBody = block.content;
                i = block.endIndex; // Skip lines that are part of the block
            }

            // Execute command
            cmd.execute(ctx, line, blockBody);

            // Check flow control flags
            if (ctx.shouldReturn || ctx.shouldBreak || ctx.shouldContinue) {
                break;
            }
        }
    }

    /**
     * Execute a block of code (used by flow control commands)
     */
    public static void executeBlock(List<String> lines, ScriptContext ctx) {
        execute(lines, ctx);
    }

    /**
     * Extract a block of code between { and matching }
     */
    private static BlockResult extractBlock(List<String> lines, int startIndex) {
        List<String> blockContent = new ArrayList<>();
        int braceDepth = 1; // We already saw the opening {
        int i = startIndex;

        while (i < lines.size() && braceDepth > 0) {
            String line = lines.get(i);

            // Count braces
            for (char c : line.toCharArray()) {
                if (c == '{')
                    braceDepth++;
                if (c == '}')
                    braceDepth--;
            }

            if (braceDepth > 0) {
                blockContent.add(line);
            }
            i++;
        }

        return new BlockResult(blockContent, i - 1);
    }

    /**
     * Helper class to return block extraction result
     */
    private static class BlockResult {
        final List<String> content;
        final int endIndex;

        BlockResult(List<String> content, int endIndex) {
            this.content = content;
            this.endIndex = endIndex;
        }
    }
}

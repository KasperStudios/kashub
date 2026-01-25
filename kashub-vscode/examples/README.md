# Kashub VSCode Extension Examples

This folder contains example scripts to demonstrate the features of the Kashub VSCode extension.

## Files

### syntax_showcase.kh
Demonstrates all KHScript syntax features:
- Variables and constants
- Control flow (if/else, loops)
- Functions
- Events
- Comments
- All 44+ commands organized by category

### test_hover.kh
Test file for hover documentation:
- Hover over any method name to see documentation
- Shows usage examples for all object methods
- Tests System, player, scanner, vision, inventory, and Math objects

## Features to Test

### Hover Documentation
Open `test_hover.kh` and hover over any method like:
- `System.print` - See description, usage, and return type
- `player.moveTo` - See parameters and examples
- `scanner.blocks` - See what it returns

### Auto-completion
Start typing in any .kh file:
- Type `player.` to see all player methods
- Type `System.` to see all System methods
- Use snippets like `sysprint`, `pmove`, `scanblocks`

### Syntax Highlighting
Open `syntax_showcase.kh` to see:
- Keywords in purple
- Objects in teal
- Methods in yellow
- Strings in orange
- Comments in green

## How to Use

1. Open any `.kh` file in VSCode
2. The extension will automatically activate
3. Hover over commands to see documentation
4. Use auto-completion (Ctrl+Space) for suggestions
5. Press F5 to run the script (requires connection to Kashub mod)

## Tips

1. **Connect to Kashub**: Use `Ctrl+Shift+P` → "Kashub: Reconnect" to connect to Minecraft
2. **Run Scripts**: Press `F5` or use "Kashub: Run Script" command
3. **View Console**: Use "Kashub: Open Console" to see script output
4. **Stop Scripts**: Use "Kashub: Stop All Tasks" to stop running scripts

## More Examples

For more complete examples, see the main documentation:
- [Quick Start Guide](../../docs/quick-start.md)
- [API Documentation](../../docs/api-overview.md)
- [Example Scripts](../../docs/examples/README.md)

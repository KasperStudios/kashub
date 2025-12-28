# KHScript Examples

This folder contains example scripts demonstrating various features of KHScript.

## Files

### syntax_showcase.kh
Complete syntax highlighting showcase demonstrating:
- Comments (single-line and multi-line)
- Variables (user and environment)
- Control flow (if/else, while, for, loop)
- Operators (comparison, logical, arithmetic, assignment)
- All 44+ commands organized by category
- Functions and function calls
- Event handlers
- Special keywords
- Numbers and strings
- Complex real-world example

Open this file in VSCode to see the full syntax highlighting in action!

## Testing Syntax Highlighting

1. Open `syntax_showcase.kh` in VSCode
2. Make sure the Kashub extension is installed
3. Check that:
   - Keywords are highlighted (if, while, for, etc.)
   - Commands are highlighted (print, moveTo, attack, etc.)
   - Variables with `$` are highlighted as one unit
   - Strings show variable interpolation
   - Operators are distinct
   - Comments are grayed out
   - Numbers are highlighted

## Color Scheme

The syntax highlighting uses standard VSCode token types:
- **Keywords** - Purple/Pink (if, while, let, const)
- **Commands** - Blue (print, moveTo, attack)
- **Variables** - Light Blue/Cyan ($PLAYER_X, $myVar)
- **Strings** - Orange/Yellow
- **Numbers** - Green
- **Comments** - Gray/Green
- **Operators** - White/Gray

Colors may vary depending on your VSCode theme!

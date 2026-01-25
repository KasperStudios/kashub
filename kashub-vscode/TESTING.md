# Testing the Kashub VSCode Extension

This guide helps you test the extension after making changes.

## Prerequisites

1. Install dependencies:
   ```bash
   cd kashub-vscode
   npm install
   ```

2. Compile TypeScript:
   ```bash
   npm run compile
   ```

## Testing in VSCode

### Method 1: Debug Mode (Recommended)

1. Open `kashub-vscode` folder in VSCode
2. Press `F5` to launch Extension Development Host
3. In the new window, open a `.kh` file from `examples/`
4. Test features:
   - Hover over methods like `player.moveTo`
   - Type `player.` and check auto-completion
   - Use snippets like `sysprint`, `pmove`
   - Check syntax highlighting

### Method 2: Install VSIX

1. Package the extension:
   ```bash
   npm run package
   ```

2. Install the generated `.vsix` file:
   - Open VSCode
   - Go to Extensions (Ctrl+Shift+X)
   - Click "..." menu → "Install from VSIX"
   - Select `kashub-vscode-0.9.1.vsix`

3. Reload VSCode and test

## What to Test

### ✅ Hover Documentation

Open `examples/test_hover.kh` and hover over:
- [x] `System.print` - Should show description and usage
- [x] `player.moveTo` - Should show parameters
- [x] `scanner.blocks` - Should show return type
- [x] `Math.sqrt` - Should show Math object method

**Expected**: Hover popup with description, usage, and return type

### ✅ Auto-completion

Type in a `.kh` file:
- [x] `player.` - Should show all player methods
- [x] `System.` - Should show all System methods
- [x] `scanner.` - Should show scanner methods
- [x] `sysprint` - Should insert System.print snippet

**Expected**: Dropdown with method suggestions

### ✅ Syntax Highlighting

Open `examples/syntax_showcase.kh`:
- [x] Keywords (if, while, let) are highlighted
- [x] Objects (System, player) are highlighted
- [x] Methods (print, moveTo) are highlighted
- [x] Strings are highlighted
- [x] Comments are grayed out

**Expected**: Different colors for different token types

### ✅ Snippets

Type these prefixes and press Tab:
- [x] `sysprint` → `System.print()`
- [x] `pmove` → `player.moveTo(x, y, z)`
- [x] `pstop` → `player.stopMoving()`
- [x] `pinput` → `player.input(action, type)`
- [x] `scanblocks` → `scanner.blocks(type, radius)`

**Expected**: Code template inserted with placeholders

## Common Issues

### Hover not working
- Check that file has `.kh` extension
- Verify language mode is "KHScript" (bottom right corner)
- Try reloading VSCode window (Ctrl+Shift+P → "Reload Window")

### Auto-completion not working
- Press `Ctrl+Space` to manually trigger
- Check that extension is activated (look for Kashub in status bar)
- Verify no TypeScript compilation errors

### Syntax highlighting wrong
- Check `syntaxes/khscript.tmLanguage.json` for errors
- Reload window after changes
- Try different VSCode theme

## Debugging

### View Extension Logs

1. Open Output panel (Ctrl+Shift+U)
2. Select "Kashub" from dropdown
3. Check for errors or warnings

### Debug TypeScript Code

1. Open `kashub-vscode` in VSCode
2. Set breakpoints in `.ts` files
3. Press F5 to launch debug session
4. Trigger the feature you want to debug

### Check Language Configuration

Verify `language-configuration.json`:
- Word pattern includes dots for `object.method`
- Auto-closing pairs work correctly
- Indentation rules are correct

## Reporting Issues

If you find a bug:
1. Check console for errors (F12 → Console)
2. Note the exact steps to reproduce
3. Include VSCode version and OS
4. Attach relevant log output
5. Create an issue on GitHub

## Building for Release

1. Update version in `package.json`
2. Update `CHANGELOG.md`
3. Compile and test:
   ```bash
   npm run compile
   npm run package
   ```
4. Test the `.vsix` file
5. Publish to marketplace (if applicable)

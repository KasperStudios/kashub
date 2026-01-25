# Editor Guide

Complete guide to using the Kashub built-in code editor.

## Overview

Kashub includes a professional VSCode-style code editor built directly into Minecraft. No external tools needed!

## Opening the Editor

Press `K` to open the editor (default keybind).

You can change this keybind in:
- Minecraft Settings → Controls → Key Binds → Kashub

## Editor Layout

The editor consists of three main areas:

```
┌─────────────────────────────────────────────┐
│  Script Browser  │  Code Editor             │
│                  │                          │
│  • script1.kh    │  let health = player...  │
│  • script2.kh    │  System.print(health)    │
│  • script3.kh    │                          │
│                  │                          │
│  [New] [Delete]  │  [Run] [Stop] [Debug]    │
└─────────────────────────────────────────────┘
│  Console Output                              │
│  [KH] Script started...                      │
│  [KH] Health: 20                             │
└─────────────────────────────────────────────┘
```

### Script Browser (Left Panel)

- Lists all your scripts
- Click to open a script
- Right-click for options
- Drag to reorder

### Code Editor (Center)

- Main editing area
- Syntax highlighting
- Line numbers
- Auto-completion

### Console (Bottom)

- Shows script output
- Displays errors
- Real-time logging

## Keyboard Shortcuts

### File Operations

| Shortcut | Action |
|----------|--------|
| `Ctrl+N` | New script |
| `Ctrl+S` | Save script |
| `Ctrl+O` | Open script |
| `Ctrl+W` | Close script |

### Editing

| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Ctrl+X` | Cut |
| `Ctrl+C` | Copy |
| `Ctrl+V` | Paste |
| `Ctrl+A` | Select all |
| `Ctrl+D` | Duplicate line |
| `Ctrl+/` | Toggle comment |

### Navigation

| Shortcut | Action |
|----------|--------|
| `Ctrl+F` | Find |
| `Ctrl+H` | Find and replace |
| `Ctrl+G` | Go to line |
| `Home` | Go to line start |
| `End` | Go to line end |
| `Ctrl+Home` | Go to file start |
| `Ctrl+End` | Go to file end |

### Running Scripts

| Shortcut | Action |
|----------|--------|
| `F5` | Run script |
| `Shift+F5` | Stop script |
| `F9` | Toggle breakpoint |
| `F10` | Step over (debug) |
| `F11` | Step into (debug) |
| `Shift+F11` | Step out (debug) |

### Code Intelligence

| Shortcut | Action |
|----------|--------|
| `Ctrl+Space` | Trigger auto-complete |
| `Ctrl+Shift+Space` | Parameter hints |
| `F12` | Go to definition |

### View

| Shortcut | Action |
|----------|--------|
| `Ctrl++` | Increase font size |
| `Ctrl+-` | Decrease font size |
| `Ctrl+0` | Reset font size |

## Features

### Syntax Highlighting

The editor automatically highlights:
- **Keywords** (blue): `let`, `if`, `while`, `fn`
- **Strings** (green): `"Hello, World!"`
- **Numbers** (orange): `42`, `3.14`
- **Comments** (gray): `// This is a comment`
- **Objects** (orange): `player`, `System`, `scanner`
- **Methods** (purple): `getHealth()`, `print()`, `moveTo()`
- **Operators** (white): `+`, `-`, `==`, `&&`

### Auto-Completion

Press `Ctrl+Space` to trigger auto-completion:

```javascript
player.  // Shows: getHealth, moveTo, attack, etc.
System.  // Shows: print, log, wait, etc.
```

Auto-completion provides:
- Object methods
- Function names
- Variable names
- Keywords

### Line Numbers

Line numbers are displayed on the left side of the editor. Click a line number to:
- Set a breakpoint (red dot appears)
- Select the entire line

### Error Detection

The editor highlights errors in real-time:
- **Red underline**: Syntax error
- **Yellow underline**: Warning
- Hover over the error for details

### Code Folding

Click the arrow next to line numbers to fold/unfold code blocks:

```javascript
fn myFunction() {  ▼
    // Code here
}

fn myFunction() {  ▶
```

### Multiple Tabs

Open multiple scripts at once:
- Each script opens in a new tab
- Click tabs to switch between scripts
- Close tabs with the X button

## Themes

Kashub includes 10+ professional themes:

### Available Themes

1. **Dracula** (default) - Dark purple theme
2. **One Dark Pro** - Atom-inspired dark theme
3. **Tokyo Night** - Japanese-inspired dark theme
4. **Cyberpunk** - Neon-style theme
5. **Monokai** - Classic Sublime Text theme
6. **Nord** - Arctic-inspired theme
7. **Solarized Dark** - Popular dark theme
8. **Solarized Light** - Light variant
9. **GitHub Light** - GitHub-style light theme
10. **Material** - Material Design theme

### Changing Themes

1. Open the editor
2. Click "Settings" button
3. Select "Theme"
4. Choose your preferred theme

Or edit `config/kashub/config.json`:

```json
{
  "editorTheme": "dracula"
}
```

## Script Management

### Creating Scripts

1. Click "New Script" button or press `Ctrl+N`
2. Enter a name (e.g., `my_script.kh`)
3. Start writing code

### Saving Scripts

Scripts are automatically saved when you:
- Press `Ctrl+S`
- Run the script
- Close the editor

Scripts are saved to: `config/kashub/scripts/`

### Deleting Scripts

1. Right-click the script in the browser
2. Select "Delete"
3. Confirm deletion

Or manually delete from `config/kashub/scripts/`

### Renaming Scripts

1. Right-click the script
2. Select "Rename"
3. Enter new name

### Duplicating Scripts

1. Right-click the script
2. Select "Duplicate"
3. A copy is created with "_copy" suffix

## Running Scripts

### Run Button

Click the "Run" button or press `F5` to execute the script.

### Stop Button

Click "Stop" or press `Shift+F5` to stop the running script.

### Background Execution

Scripts continue running even when the editor is closed:
- Press `Esc` to close editor
- Script keeps running
- Press `Z` to stop all scripts

### Task Manager

View all running scripts:
1. Click "Tasks" button
2. See list of active scripts
3. Stop individual scripts

## Console

The console shows script output and errors.

### Output Types

- **Info** (white): Normal output from `System.print()`
- **Warning** (yellow): Warnings and alerts
- **Error** (red): Errors and exceptions
- **Debug** (gray): Debug messages from `System.log()`

### Console Commands

- **Clear**: Clear all output
- **Copy**: Copy output to clipboard
- **Export**: Save output to file

### Filtering

Filter console output by type:
- Click "Info" to show/hide info messages
- Click "Warnings" to show/hide warnings
- Click "Errors" to show/hide errors

## Debugging

### Setting Breakpoints

1. Click the line number where you want to pause
2. A red dot appears
3. Run the script in debug mode

### Debug Mode

Press `F5` with breakpoints set to enter debug mode.

When paused at a breakpoint:
- **F10**: Step over (execute current line)
- **F11**: Step into (enter function)
- **Shift+F11**: Step out (exit function)
- **F5**: Continue execution

### Variable Inspection

When paused, hover over variables to see their values:

```javascript
let health = player.getHealth()  // Hover shows: health = 20
```

### Call Stack

View the call stack in the debug panel:
- Shows current function
- Shows caller functions
- Click to navigate

## Search and Replace

### Find

Press `Ctrl+F` to open find dialog:
1. Enter search term
2. Press Enter to find next
3. Use arrows to navigate results

### Replace

Press `Ctrl+H` to open replace dialog:
1. Enter search term
2. Enter replacement
3. Click "Replace" or "Replace All"

### Find Options

- **Match case**: Case-sensitive search
- **Whole word**: Match whole words only
- **Regex**: Use regular expressions

## Settings

### Font Size

Change font size:
1. Click "Settings"
2. Adjust "Font Size" slider
3. Or use `Ctrl++` / `Ctrl+-`

### Tab Size

Set indentation width:
1. Click "Settings"
2. Set "Tab Size" (2, 4, or 8 spaces)

### Auto-Save

Enable auto-save:
1. Click "Settings"
2. Enable "Auto-Save"
3. Set interval (seconds)

### Line Wrapping

Enable line wrapping:
1. Click "Settings"
2. Enable "Word Wrap"

## Tips and Tricks

### 1. Use Auto-Complete

Type the first few letters and press `Ctrl+Space`:

```javascript
play  // Press Ctrl+Space → player
Sys   // Press Ctrl+Space → System
```

### 2. Duplicate Lines Quickly

Press `Ctrl+D` to duplicate the current line:

```javascript
System.print("Hello")
// Press Ctrl+D
System.print("Hello")
System.print("Hello")
```

### 3. Comment Multiple Lines

Select lines and press `Ctrl+/`:

```javascript
let health = player.getHealth()
let hunger = player.getHunger()
// Becomes:
// let health = player.getHealth()
// let hunger = player.getHunger()
```

### 4. Quick Navigation

Use `Ctrl+G` to jump to a specific line:
1. Press `Ctrl+G`
2. Enter line number
3. Press Enter

### 5. Multi-Cursor Editing

Hold `Alt` and click to add cursors:
- Edit multiple lines at once
- Type once, edit everywhere

### 6. Bracket Matching

Click a bracket to highlight its pair:
```javascript
if (health < 10) {  // Click here
    // Code
}  // Highlights here
```

### 7. Code Snippets

Type shortcuts and press Tab:
- `while` → while loop template
- `if` → if statement template
- `fn` → function template

## Troubleshooting

### Editor won't open

1. Check keybind isn't conflicting
2. Restart Minecraft
3. Check logs for errors

### Syntax highlighting not working

1. Make sure file ends with `.kh`
2. Try changing theme
3. Restart editor

### Auto-complete not working

1. Press `Ctrl+Space` to trigger
2. Make sure you're typing after an object (e.g., `player.`)
3. Check if IntelliSense is enabled in settings

### Scripts not saving

1. Check file permissions
2. Make sure `config/kashub/scripts/` exists
3. Try manual save with `Ctrl+S`

## See Also

- [Quick Start Guide](quick-start.md)
- [KHScript Syntax](khscript-syntax.md)
- [Debugging Guide](advanced/debugging.md)
- [VSCode Integration](advanced/vscode-integration.md)

# Kashub VSCode Extension

Full language support for Kashub (.kh) scripts with real-time debugging and live connection to Minecraft.

## Features

### 🎨 Syntax Highlighting
Complete syntax highlighting for all KHScript commands, keywords, and constructs.

### 💡 IntelliSense
- **Online mode**: Full autocomplete from Kashub mod with real command signatures
- **Offline mode**: Basic autocomplete with static command list

### 🐛 Real-Time Validation
- Syntax errors highlighted instantly
- Powered by actual Kashub parser (when connected)
- Works offline with basic validation

### 🖥️ Kashub Console
- Live output from running scripts
- Color-coded log levels (info/warn/error/debug)
- Task tracking
- Auto-scroll
- Export logs

### ⚡ Run Scripts from VSCode
- Press `Ctrl+Shift+K` to run current script in Minecraft
- Results appear in Kashub Console
- No need to switch windows!

### 🔄 Hot-Reload Support
Works seamlessly with Kashub v0.6.0+ hot-reload feature!

## Requirements

- Minecraft with Kashub mod v0.7.0+
- Mod's API server must be enabled (default: enabled)
- Default ports: HTTP 25566, WebSocket 25567

## Installation

1. Install the extension from VSCode Marketplace
2. Start Minecraft with Kashub mod
3. Open a `.kh` file in VSCode
4. Start coding! IntelliSense will work automatically
5. Press `Ctrl+Shift+K` to run script

## Commands

| Command | Keybinding | Description |
|---------|------------|-------------|
| `Kashub: Run Current Script` | `Ctrl+Shift+K` | Run the current script in Minecraft |
| `Kashub: Open Console` | `Ctrl+Shift+\`` | Open the Kashub Console panel |
| `Kashub: Show Variables` | - | Show all environment variables |
| `Kashub: Stop All Tasks` | - | Stop all running scripts |
| `Kashub: Reconnect to Mod` | - | Reconnect to Kashub API |

## Configuration

```json
{
  "kashub.apiUrl": "http://localhost:25566",
  "kashub.wsUrl": "ws://localhost:25567",
  "kashub.autoConnect": true,
  "kashub.showConsoleOnRun": true
}
```

## Extension Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `kashub.apiUrl` | `http://localhost:25566` | Kashub HTTP API server URL |
| `kashub.wsUrl` | `ws://localhost:25567` | Kashub WebSocket server URL |
| `kashub.autoConnect` | `true` | Auto-connect on startup |
| `kashub.showConsoleOnRun` | `true` | Show console when running scripts |

## Snippets

The extension includes useful snippets:

- `for` - For loop
- `while` - While loop
- `loop` - Loop N times
- `if` / `ifelse` - Conditional statements
- `fn` / `function` - Function definition
- `let` / `const` - Variable declarations
- `event` / `onevent` - Event handlers
- `automine` - Auto-mining template
- `autofarm` - Auto-farming template
- `healthmonitor` - Health monitoring template

## Troubleshooting

### Not connecting to Kashub
1. Make sure Minecraft is running with Kashub mod
2. Check that API is enabled in mod config (`apiEnabled: true`)
3. Verify ports are not blocked (25566, 25567)
4. Try clicking the status bar to reconnect

### Scripts not running
1. Make sure you're in a world (not main menu)
2. Check the Kashub Console for errors
3. Validate your script syntax

### WebSocket disconnecting
- This is normal if Minecraft is closed
- Extension will auto-reconnect when Minecraft starts

## Known Issues

- WebSocket may disconnect on slow connections (auto-reconnects)
- Large scripts (>5000 lines) may have slower validation

## Release Notes

### 0.1.0
- Initial release
- Full language support
- Live debugging via WebSocket
- Kashub Console panel
- IntelliSense with online/offline modes

---

**Enjoy scripting!** 🚀

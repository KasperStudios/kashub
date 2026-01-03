# Kashub - Minecraft Scripting Mod

<p align="center">
  <img src="src/main/resources/assets/kashub/icon.png" alt="Kashub Logo" width="128"/>
</p>

**Kashub** is a powerful scripting mod for Minecraft Fabric that allows players to automate in-game actions using a simple, Lua/JavaScript-inspired scripting language called **KHScript**.

## 🎮 Features

### Core Features
- **Custom Scripting Language** - Easy-to-learn KHScript syntax similar to Lua/JavaScript
- **Built-in Code Editor** - VSCode-like editor with syntax highlighting and autocomplete
- **10+ Editor Themes** - Dracula, One Dark, Monokai, Nord, Tokyo Night, and more
- **Event System** - React to game events (damage, chat, ticks, etc.) - 9 events working!
- **Debug Tools** - Breakpoints, step-through debugging, variable watching, profiler

### v0.8.0 Features (Latest) 🔥
- **�  Professional Debugging** - Click line numbers to set breakpoints, step through code
- **📊 Variables Panel** - Real-time variable inspection in GUI
- **⚡ Performance Profiler** - Track command execution times, export to Chrome Tracing
- **� IConditional Breakpoints** - Break on conditions, logpoints, hit counts
- **�  Full VSCode Debug Adapter** - Complete DAP implementation with variables/call stack
- **📡 WebSocket Debug Events** - Real-time debugging from VSCode
- **✅ Event System Fixed** - All 9 events work perfectly, even after stop/restart
- **📦 Export/Import System** - Share variables between scripts

### v0.7.0 Features
- **� VSConde Integration** - Full IDE support with dedicated VSCode extension
- **🌐 HTTP API Server** - REST API for external tool integration (port 25566)
- **📡 WebSocket Server** - Real-time script output streaming (port 25567)
- **💡 IntelliSense** - Autocomplete powered by actual Kashub parser
- **🖥️ Kashub Console** - Live output panel in VSCode
- **⚡ Run from VSCode** - Execute scripts with Ctrl+Shift+K

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Download [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download the latest Kashub release
4. Place both `.jar` files in your `mods` folder
5. Launch Minecraft!

## 🎹 Controls

| Key | Action |
|-----|--------|
| `K` | Open Script Editor |
| `Z` | Stop all running scripts |
| `Y` | Open AI Agent (if enabled) |
| `F9` | Toggle breakpoint (in editor) |
| `F5` | Run/Continue script |
| `F10` | Step over (debug mode) |
| `F11` | Step into (debug mode) |

## 🔌 VSCode Integration (v0.7.0+)

Kashub now includes full VSCode integration for professional development experience!

### Installation

1. Install the **Kashub VSCode Extension** from the `kashub-vscode` folder
2. Start Minecraft with Kashub mod
3. Open any `.kh` file in VSCode
4. The extension will auto-connect to the mod

### Features

- **Syntax Highlighting** - Full KHScript language support
- **IntelliSense** - Autocomplete powered by actual Kashub parser
- **Real-time Validation** - Errors highlighted as you type
- **Kashub Console** - Live script output in VSCode
- **Run Scripts** - Press `Ctrl+Shift+K` to run current script
- **Debug Adapter** - Full debugging support (v0.8.0+):
  - Set breakpoints in VSCode
  - Step through code (Over, Into, Out)
  - View variables in real-time
  - Inspect call stack
  - Evaluate expressions
- **Hover Provider** - Hover over commands/variables for documentation

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | Mod status, player info |
| `/api/validate` | POST | Validate script code |
| `/api/autocomplete` | POST | Get suggestions |
| `/api/run` | POST | Execute script |
| `/api/tasks` | GET | List running tasks |
| `/api/variables` | GET | Get environment variables |
| `/api/debug/*` | Various | Debug Adapter Protocol endpoints |
| `/api/profiler/*` | Various | Performance profiler endpoints |

### Configuration

```json
{
  "apiEnabled": true,
  "apiPort": 25566,
  "apiWebSocketPort": 25567
}
```

## 📝 KHScript Syntax

### Basic Commands

```javascript
// Print message to chat
print "Hello, World!"

// Wait for milliseconds
wait 1000

// Make player jump
jump 3

// Move to coordinates
moveTo 100 64 200

// Run to coordinates
run 100 64 200

// Teleport (requires cheats)
tp ~10 ~ ~10
```

### Variables

```javascript
// User variables
myVar = 10
name = "Player"

// Environment variables (read-only)
print $PLAYER_X
print $PLAYER_HEALTH
print $WORLD_TIME
```

### Available Environment Variables

| Variable | Description |
|----------|-------------|
| `$PLAYER_X`, `$PLAYER_Y`, `$PLAYER_Z` | Player coordinates |
| `$PLAYER_HEALTH` | Current health (0-20) |
| `$PLAYER_FOOD` | Hunger level (0-20) |
| `$PLAYER_XP` | Experience level |
| `$PLAYER_NAME` | Player username |
| `$IS_SNEAKING` | Is player sneaking |
| `$IS_SPRINTING` | Is player sprinting |
| `$WORLD_TIME` | World time in ticks |
| `$GAME_MODE` | Current game mode |

### Control Flow

```javascript
// If statement
if ($PLAYER_HEALTH < 10) {
    print "Low health!"
    eat golden_apple
}

// While loop
while ($PLAYER_FOOD < 20) {
    eat
    wait 2000
}

// For loop
for (i = 0; i < 10; i = i + 1) {
    jump
    wait 500
}
```

### Functions

```javascript
// Define function
function greet(name) {
    print "Hello, $name!"
}

// Call function
greet("Steve")

// Function with multiple commands
function attackAndHeal() {
    attack 5 hostile
    wait 500
    if ($PLAYER_HEALTH < 15) {
        eat
    }
}
```

### Events

```javascript
// React to damage
onEvent onDamage {
    print "Took $event_damage damage!"
    if ($event_health < 5) {
        eat golden_apple
    }
}

// React to chat
onEvent onChat {
    log info "Message from $event_sender: $event_message"
}

// Periodic tick (every second)
onEvent onTick {
    // Check conditions periodically
    if ($PLAYER_HEALTH < 10) {
        print "Low health warning!"
    }
}

// React to block breaking
onEvent onBlockBreak {
    print "Broke block at $event_x $event_y $event_z"
}

// React to attacks
onEvent onAttack {
    print "Attacked $event_target_name"
}
```

### Available Events (v0.8.0)

| Event | Description | Variables |
|-------|-------------|-----------|
| `onTick` | Fires every second | `$event_tick`, `$event_time` |
| `onDamage` | Player takes damage | `$event_damage`, `$event_health` |
| `onHeal` | Player heals | `$event_healed`, `$event_health` |
| `onHunger` | Hunger changes | `$event_food`, `$event_previousFood` |
| `onDeath` | Player dies | `$event_position_x/y/z` |
| `onChat` | Chat message received | `$event_message`, `$event_sender` |
| `onBlockBreak` | Block broken | `$event_x/y/z`, `$event_block` |
| `onBlockPlace` | Block placed | `$event_x/y/z`, `$event_block` |
| `onAttack` | Entity attacked | `$event_target_name`, `$event_target_type` |

### Export/Import System (v0.8.0)

Share variables between scripts:

```javascript
// script1.kh
export myVar 100
export playerName "Kasper"

// script2.kh
import myVar from script1
import playerName from script1
print $myVar  // Outputs: 100
print $playerName  // Outputs: Kasper
```

## 🔧 Commands Reference

### Player Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `jump` | `[count]` | Make player jump |
| `run` | `<x> <y> <z>` | Run to coordinates |
| `moveTo` | `<x> <y> <z> [speed]` | Move smoothly to position |
| `tp` | `<x> <y> <z>` | Teleport (requires cheats) |
| `lookAt` | `<x> <y> <z>` or `entity [type]` | Look at position/entity |
| `sneak` | `[duration_ms]` or `toggle` | Sneak |
| `sprint` | `[duration_ms]` or `toggle` | Sprint |

### Combat Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `attack` | `[range] [type] [count]` | Attack nearby entities |

### Inventory Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `useItem` | `[itemName]` or `slot [n]` | Use item |
| `eat` | `[itemName]` | Eat food |
| `equipArmor` | `[type]` or `best` | Equip armor |
| `selectSlot` | `<slot>` or `item <name>` | Select hotbar slot |
| `drop` | `[itemName] [count]` or `all` | Drop items |

### World Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `breakBlock` | `[x y z]` | Break block |
| `placeBlock` | `<x> <y> <z> [blockName]` | Place block |
| `getBlock` | `<x> <y> <z>` | Get block info |

### Utility Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `print` | `<message>` | Print to chat |
| `chat` | `<message>` | Send chat message |
| `wait` | `<milliseconds>` | Pause execution |
| `log` | `[level] <message>` | Log message |

## ⚙️ Configuration

Configuration file: `config/kashub/config.json`

```json
{
  "editorTheme": "dracula",
  "editorFontSize": 12,
  "maxScriptsPerTick": 10,
  "sandboxMode": true,
  "allowCheats": false,
  "maxLoopIterations": 10000,
  "enableLogging": true
}
```

## 🎨 Editor Themes

Available themes:
- **Dracula** (default)
- **One Dark**
- **Monokai**
- **Solarized Dark/Light**
- **GitHub Dark**
- **Nord**
- **Gruvbox**
- **Tokyo Night**
- **Catppuccin**

Press the "Theme" button in the editor to cycle through themes.

## 📂 Script Files

Scripts are saved in: `config/kashub/scripts/`

File extension: `.kh`

## 🔄 Autorun Scripts

You can configure scripts to automatically start when the game launches:

1. Open **Task Manager** (accessible from the editor or via `/script tasks`)
2. Click on the **Autorun** tab
3. Use the arrow buttons (→/←) to add or remove scripts from autorun
4. Changes are saved automatically

**Note:** Make sure `autorunEnabled` is set to `true` in your config file for autorun to work. (or in settings gui)

## 🔒 Security

Kashub includes a sandbox mode that restricts potentially dangerous commands:

- **Whitelisted commands**: Safe commands that work in sandbox mode
- **Blacklisted commands**: Commands requiring `allowCheats: true`
- **Loop limits**: Prevents infinite loops
- **Tick limits**: Limits commands per game tick

## 🐛 Debugging

### In-Game Editor

1. Open the editor with `K`
2. **Click on line numbers** to set breakpoints (red dots appear)
3. Press "Debug" or `F5` to start debug mode
4. Use `F10` to step over, `F11` to step into
5. Watch variables in the **Debug Panel** (right side):
    - **Variables**: All script and environment variables
    - Real-time updates as you step through code

### VSCode Debugging (v0.8.0+)

1. Open your `.kh` file in VSCode
2. Click in the gutter to set breakpoints
3. Press `F5` to start debugging
4. Use VSCode's debug controls:
    - **Continue** (F5)
    - **Step Over** (F10)
    - **Step Into** (F11)
    - **Step Out** (Shift+F11)
5. View variables, call stack, and more in VSCode's debug panels

### Conditional Breakpoints

Set breakpoints that only trigger when a condition is true:

```javascript
// Via API or VSCode
// Break only when health is low
if ($PLAYER_HEALTH < 5) {
    // Breakpoint here will only trigger when health < 5
}
```

### Performance Profiler (v0.8.0)

Track command execution times:

1. Start profiler via API: `POST /api/profiler/start`
2. Run your scripts
3. Get report: `GET /api/profiler/report`
4. Export to Chrome Tracing: `GET /api/profiler/chrome-tracing`
5. Open in `chrome://tracing` for visual analysis

## 📋 Chat Commands

| Command | Description |
|---------|-------------|
| `/script load <name>` | Load a script |
| `/script run <name>` | Run a script |
| `/script stop` | Stop all scripts |
| `/script list` | List saved scripts |
| `/script tasks` | List running tasks |
| `/script pause <id>` | Pause a task |
| `/script resume <id>` | Resume a task |
| `/script kill <id>` | Kill a task |
| `/script stopall` | Stop all tasks |
| `/script reload` | Reload configuration |
| `/script debug <name>` | Show script info |

## 🔌 API Reference (v2.0)

### Vision API
```javascript
vision target [distance]        // Get what player looks at
vision block [distance]         // Get target block
vision entity [distance]        // Get target entity
vision nearest <type> <dist>    // Find nearest mob
vision count <type> <dist>      // Count mobs
vision scan <angle> <dist>      // Scan cone for entities
vision isLookingAt <type> <id>  // Check if looking at specific thing
```

### Input API
```javascript
input jump                      // Make player jump
input sneak <true/false/toggle> // Sneak control
input sprint <true/false>       // Sprint control
input attack                    // Attack once
input use                       // Use item
input hotbar <0-8>              // Select hotbar slot
input look <yaw> <pitch>        // Set camera angle
input lookAt <x> <y> <z>        // Look at position
input move <direction> <bool>   // Movement control
input stop                      // Stop all movement
```

### Scripts API
```javascript
scripts list                    // List running tasks
scripts stop <id>               // Stop task
scripts pause <id>              // Pause task
scripts resume <id>             // Resume task
scripts stopAll                 // Stop all tasks
scripts stopByTag <tag>         // Stop by tag
scripts info <id>               // Task details
```

### Animations API
```javascript
animations play <id> [params]   // Play animation
animations stop <id>            // Stop animation
animations stopAll              // Stop all animations
animations list                 // List available animations
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## 📄 License

This project is licensed under CC0-1.0 - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**KasperStudios** - [GitHub](https://github.com/KasperStudios)

---

<p align="center">
  Made with ❤️ for the Minecraft community
</p>

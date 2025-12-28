# Changelog

All notable changes to Kashub will be documented in this file.

## [v0.7.0 beta] - 2025-12-21

### 🔥 Major Features

#### VSCode Integration & API Server
- **HTTP API Server** for external tool integration (VSCode, custom tools)
  - Default port: 25566 (configurable)
  - REST endpoints for script management
  - CORS support for web-based tools
  
- **WebSocket Server** for real-time communication
  - Default port: 25567 (configurable)
  - Live script output streaming
  - Task state change notifications
  - Error broadcasting

- **VSCode Extension** (separate package: kashub-vscode)
  - Full KHScript syntax highlighting
  - IntelliSense with online/offline modes
  - Real-time validation powered by actual Kashub parser
  - Kashub Console panel for live output
  - Run scripts directly from VSCode (Ctrl+Shift+K)
  - Environment variables viewer
  - Task management

### ✨ API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | Mod status, player info, task stats |
| `/api/validate` | POST | Validate KHScript code |
| `/api/autocomplete` | POST | Get autocomplete suggestions |
| `/api/run` | POST | Execute script code |
| `/api/tasks` | GET | List all running tasks |
| `/api/tasks/{id}/stop` | POST | Stop a specific task |
| `/api/tasks/{id}/pause` | POST | Pause a specific task |
| `/api/tasks/{id}/resume` | POST | Resume a paused task |
| `/api/variables` | GET | Get all environment variables |

### ✨ WebSocket Events

| Event | Description |
|-------|-------------|
| `script_output` | Script print/log output |
| `script_error` | Script execution errors |
| `task_state_change` | Task started/stopped/paused |
| `variable_update` | Environment variable changed |

### ⚙️ New Configuration Options

```json
{
  "apiEnabled": true,
  "apiPort": 25566,
  "apiWebSocketPort": 25567,
  "apiRequireAuth": false
}
```

### 🔧 Improvements

- Print and Log commands now broadcast to WebSocket for VSCode console
- Better error messages with line numbers
- Improved command validation

### 📝 Notes
- API server starts automatically when mod loads (if enabled)
- VSCode extension available separately
- Works with hot-reload from v0.6.0

---

## [v0.6.1] - 2025-12-19

### ⚙️ IBug Fixes

#### Task Manager Autorun GUI
- **Fixed script selection** - Clicking on scripts in autorun lists now properly selects them
- **Fixed arrow buttons** - Add/Remove buttons now work with selected scripts
- **Added double-click support** - Double-click to quickly add/remove scripts from autorun
- **Visual selection indicator** - Selected scripts are now highlighted with accent color

#### Text Editor Crash Fix
- **Fixed paste crash** - Fixed `StringIndexOutOfBoundsException` when pasting text with selection
- **Bounds checking** - Added proper bounds validation in `deleteSelection()` and selection methods
- **Safer text operations** - Selection indices are now clamped to valid line lengths

### 🔧 Improvements

#### Code Cleanup
- **Removed debug logging** - Eliminated all hardcoded FileWriter debug logs from ScriptTask and ScriptTaskManager
- **Cleaner codebase** - Removed 20+ debug log blocks that were writing to absolute paths

#### Documentation
- **Horizontal scrolling in docs** - DocsDialog now supports Shift+MouseWheel for horizontal scrolling
- **No more truncated text** - Command descriptions display in full without "..." truncation
- **Updated KHScriptGuide.md** - Removed HTTP references, added Marketplace section
- **Complete command documentation** - All commands now have detailed help with:
  - Full usage syntax and parameters
  - Multiple examples for each command
  - Variables set by commands
  - Notes and important details
  - Category classification

#### Commands with Enhanced Documentation
- **Movement**: moveTo, run, jump, sneak, sprint, swim, teleport, stop
- **Combat**: attack (with target types, range, count)
- **Interaction**: interact, placeBlock, breakBlock, useItem, eat, dropItem, equipArmor
- **Inventory**: inventory, selectSlot, autoCraft, autoTrade
- **Scanner**: scan, scanner (with caching and async support)
- **Visual**: fullbright, animation
- **Sound**: sound (with music/melody support)
- **Input**: input (comprehensive player input control)
- **AI**: ai (AI assistant integration)
- **Events**: onEvent (event-driven scripting)
- **Control Flow**: loop (with nested loop support)
- **Output**: log, chat
- **Script Management**: scripts (task management)
- **Player**: setHealth, speed

#### Security
- **Removed HTTP commands** - Deleted HttpCommand, LoadScriptCommand, and HttpService for safer scripting
- **Removed HTTP config options** - Cleaned up allowHttpRequests, httpWhitelistedDomains, allowRemoteScripts, remoteScriptSources from config
- **Updated GUI** - Removed HTTP toggle from Settings panels

### ✨ Added

#### Marketplace Skeleton (Preparation for future)
- **MarketplaceService.java** - Service for future GitHub-based script repository
- **VerifiedScript.java** - Model for verified scripts with ratings, downloads, signatures
- **MarketplaceConfig.java** - Configuration for marketplace settings
- Categories: automation, utility, farming, building, combat, navigation, misc

### 🗑️ Removed
- `HttpCommand.java` - HTTP GET/POST commands for scripts
- `HttpService.java` - Async HTTP service
- `LoadScriptCommand.java` - Remote script loading
- HTTP-related config options from KashubConfig
- HTTP tick processing from KashubClient

### 📝 Notes
- This is a patch release focusing on stability and cleanup
- Marketplace is skeleton only - full implementation coming in future version

---

## [v0.6.0 beta] - 2025-12-15

### 🔥 Major Features

#### Hot-Reload System
- **Automatic script reloading** when files are modified externally
- Edit scripts in your favorite editor (VSCode, IntelliJ, Notepad++, etc.) and see changes instantly
- Dual monitoring system:
  - Java NIO `WatchService` for real-time file system events
  - Periodic polling (configurable interval, default: 1000ms) for external editor compatibility
- Configurable via Settings panel (`hotReload` option)
- Automatic registration/unregistration with script lifecycle
- Thread-safe reloading with error handling
- **Use case**: Edit long-running scripts without stopping them!
- **Note**: May have 1-2 second delay for external editors (this is normal)

#### Autorun System
- **Automatic script execution** on world load
- Configure multiple scripts to start automatically when joining a world
- Visual management interface in Task Manager (new "Autorun" tab)
- Scripts execute sequentially with error handling
- Failed scripts don't block other autorun scripts
- Configurable via Settings panel (`autorunEnabled`, `autorunScripts`)
- **Use case**: Always-on utility scripts (anti-AFK, auto-farm, monitoring, etc.)

### ✨ Added

#### Task Manager UI Enhancements
- **Tabbed Interface** - Task Manager now has tabs like Windows Task Manager
  - **Processes Tab** - View and manage running scripts (existing functionality)
  - **Autorun Tab** - NEW! Manage scripts that auto-start on game launch
- **Autorun Management Panel** - Visual interface for autorun scripts
  - Left panel: Currently configured autorun scripts
  - Right panel: Available scripts that can be added
  - Add/Remove buttons (→/←) to move scripts between panels
  - Real-time status indicators (running/stopped)
  - Changes saved automatically to config

#### Configuration
- New settings in `config/kashub/config.json`:
  - `hotReload` - Enable/disable hot-reload (default: false)
  - `hotReloadCheckInterval` - Polling interval in milliseconds (default: 1000)
  - `autorunEnabled` - Enable/disable autorun (default: false)
  - `autorunScripts` - List of scripts to auto-start (default: [])
- Settings accessible via in-game Settings panel
- Auto-save on configuration changes

### 🐛 Fixed

#### Script Execution
- **If/Else If/Else Blocks** - Fixed detection of `else` blocks in conditional chains
  - `findIfElseChain` now correctly finds all blocks including `else`
  - Fixed parsing of `} else {` syntax on same line
  - Extended search range to include blocks outside initial if block
- **While (true) Loops** - Fixed infinite loop execution
  - Loops now correctly re-queue after each iteration
  - Fixed `findBlockEnd` to properly handle `} else {` syntax
  - Commands after conditional blocks (like `wait 200`) now execute correctly
- **Environment Variables** - Fixed timing of variable updates
  - Variables now update before condition evaluation
  - Health checks now use current values instead of stale data
  - Fixed incorrect health messages when player has full health

#### Example Scripts
- **Fixed multiple example scripts** to ensure 100% working examples
  - `example_animations.kh` - Fixed animation command syntax (`animation stopAll` → `animation stop`)
  - `example_deepslate_miner.kh` - Fixed loop syntax, input commands, and scan commands
  - `example_area_clearer.kh` - Fixed `lookAt` relative coordinates syntax and loop syntax
  - `example_scanner_advanced.kh` - Fixed for loop increment syntax (`i = i + 1` → `i++`)
  - All examples now use correct command syntax and proper quote formatting

### 🔄 Changed
- Task Manager dialog now uses tabbed interface for better organization
- Autorun configuration moved from Settings to Task Manager for easier access
- Improved command queue management for better stability
- Enhanced error handling - script errors no longer stop entire script execution
- Better state cleanup on script stop/restart
- Improved loop marker re-queuing logic for better performance

### 🔒 Security & Stability
- Thread-safe hot-reload implementation
- Debouncing for rapid file changes (prevents reload spam)
- Protected against file lock issues on Windows
- Safe handling of missing/deleted files during hot-reload
- Autorun scripts validate before execution

### 🔄 Migration from v0.5.x

**No breaking changes!** All existing scripts work as-is.

**To enable new features:**
1. Open Settings → Enable "Hot-Reload" (optional)
2. Open Task Manager → Autorun tab → Add scripts with → button
3. Enable "Autorun" in Settings
4. Done! 🎉

**Recommended workflow:**
- Use Kashub editor for quick tests and debugging
- Use external editor (VSCode/IntelliJ) for serious development
- Let hot-reload sync changes automatically

### ⚠️ Known Issues (v0.6.0 Beta)

- **Hot-reload**: 1-2 second delay for external editors (by design, ensures file write completion)
- **Windows**: Rare file lock delays on rapid saves (working on fix for v0.6.1)
- **Large scripts** (>1000 lines): Slower reload time (optimization planned)

**These will be addressed in v0.6.1!**

### 📚 Notes
- **Hot-reload** may have 1-2 second delay for external editors (this is normal)
- **Autorun** scripts execute once per world join
- Both features disabled by default for backward compatibility
- **v0.6.0 beta** - Please report any issues on GitHub!

## [v0.2.1 beta] - 2025-12-05

### Added

#### Script Task Manager (Runtime Manager)
- **ScriptTask** entity with id, name, state, uptime, error tracking
- **ScriptTaskManager** for managing running scripts
- States: RUNNING, PAUSED, STOPPED, ERROR, WAITING
- Commands: `scripts list/stop/pause/resume/restart/stopAll/stopByTag`
- Chat commands: `/script tasks`, `/script pause <id>`, `/script resume <id>`, `/script kill <id>`
- Tag-based script grouping and control

#### Vision API
- **Raycast** from player camera position
- `vision target/block/entity [distance]` - get what player looks at
- `vision nearest <type> <distance>` - find nearest mob
- `vision count <type> <distance>` - count mobs in range
- `vision scan <angle> <distance>` - scan cone for entities
- `vision isLookingAt <block|entity> <id>` - check specific target
- Results stored in `$target_*`, `$nearest_*`, `$scan_*` variables

#### HTTP API
- **HttpService** with async GET/POST requests
- Domain whitelisting for security
- `http get <url>` / `http post <url> <body>`
- Results in `$http_status`, `$http_code`, `$http_body`, `$http_error`
- **loadscript** command for dynamic script execution
- `loadscript <code>` - execute inline .kh code
- `loadscript fromUrl <url>` - load and execute remote script
- Security: No Java deserialization, .kh-only execution

#### Input API
- High-level player input abstraction
- `input jump/sneak/sprint/attack/use/drop/swap`
- `input hotbar <slot>` / `input hotbar next/prev`
- `input look <yaw> <pitch>` / `input lookAt <x> <y> <z>`
- `input move <forward/back/left/right> [bool]`
- `input stop` - stop all movement

#### Animation System
- **AnimationManager** with network sync foundation
- `animations play <id> [params]` / `animations stop <id>`
- Built-in animations: wave, dance, sit, point, celebrate, bow, glow, spin
- Animation states with duration and looping support

#### Permission System
- **ScriptType**: USER, SYSTEM, REMOTE
- System scripts (example_*) are read-only
- User scripts are fully editable
- Config options: `allowUserScriptsEdit`, `allowDangerousCommands`
- `allowSystemScriptsCopy` for copying system scripts

#### Security Enhancements
- `allowHttpRequests` - enable/disable HTTP
- `allowRemoteScripts` - enable/disable remote script loading
- `allowAiIntegration` - enable/disable AI features
- `httpWhitelistedDomains` - domain whitelist
- `remoteScriptSources` - allowed remote script sources
- Script validation to block Java code patterns

### Changed
- ScriptManager now supports system vs user scripts
- KashubClient integrates all new services in tick loop
- ScriptCommand extended with task management subcommands

## [v0.2.0 beta] - 2025-12-05

### Added

#### New Script Editor
- **VSCode-like interface** with file panel, console, and debug panel
- **10+ color themes**: Dracula, One Dark, Monokai, Solarized, Nord, Gruvbox, Tokyo Night, Catppuccin, GitHub Dark
- **Syntax highlighting** for KHScript with command, keyword, string, number, and comment colors
- **Autocomplete** with command descriptions and parameter hints
- **Breakpoints** - click on line numbers to set/remove
- **Debug mode** with step-through execution and variable watching
- **Console panel** with colored output for errors, warnings, and info

#### New Commands
- `moveTo <x> <y> <z> [speed]` - Smooth movement to coordinates
- `chat <message>` - Send chat messages
- `attack [range] [type] [count]` - Attack nearby entities
- `eat [itemName]` - Eat food from inventory
- `equipArmor [type]` - Equip armor (diamond, iron, best, etc.)
- `lookAt <x> <y> <z>` or `lookAt entity [type]` - Look at position/entity
- `sneak [duration]` / `sprint [duration]` - Movement modifiers
- `drop [itemName] [count]` - Drop items
- `selectSlot <slot>` or `selectSlot item <name>` - Select hotbar slot
- `breakBlock [x y z]` - Break blocks
- `placeBlock <x> <y> <z> [blockName]` - Place blocks
- `getBlock <x> <y> <z>` - Get block information
- `tp <x> <y> <z>` - Teleport (requires cheats)
- `log [level] <message>` - Log messages with levels
- `onEvent <eventName> { script }` - Register event handlers
- `interact` / `interact entity` / `interact block` - Interact with world
- `swim [direction] [duration]` - Swimming control
- `stop [all/scripts/events/movement]` - Stop execution

#### Event System
- `onTick` - Fires every second
- `onDamage` - When player takes damage
- `onHeal` - When player heals
- `onHunger` - When hunger changes
- `onChat` - When receiving chat messages
- `onDeath` - When player dies
- Event variables accessible via `$event_*`

#### Configuration System
- JSON config file at `config/kashub/config.json`
- Editor settings: theme, font size, autocomplete toggle
- Script limits: max commands, timeout, loop iterations
- Security: sandbox mode, cheat whitelist
- Logging: file logging, chat logging, log levels

#### Logging System
- File logging to `logs/kashub/`
- In-game console output
- Log levels: DEBUG, INFO, WARN, ERROR, SUCCESS
- Colored output in console panel

#### Security (Sandbox Mode)
- Whitelisted safe commands
- Blacklisted cheat commands (require `allowCheats: true`)
- Loop iteration limits
- Command execution limits per tick

#### Chat Commands
- `/script load <name>` - Load script
- `/script run <name>` - Run script
- `/script stop` - Stop all scripts
- `/script list` - List saved scripts
- `/script reload` - Reload configuration
- `/script debug <name>` - Show script debug info

### Changed
- Completely redesigned editor UI
- Improved syntax highlighting accuracy
- Better autocomplete with descriptions
- Enhanced variable substitution in scripts

### Fixed
- Editor cursor positioning
- Scroll synchronization
- Command parsing edge cases
- Memory leaks in async commands

## [v0.1.4 beta] - Previous Version

### Features
- Basic script editor
- Core commands (print, wait, jump, run)
- Variable support
- Control flow (if, while, for)
- Function definitions

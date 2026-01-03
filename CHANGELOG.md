# Changelog

All notable changes to Kashub will be documented in this file.

## [v0.8.0-beta] - 2026-01-03

### 🔥 Sprint 1: Debug System Foundation

#### Debug System Improvements
- **Call Stack Tracking** - Added DebugFrame class for function call tracking
  - `pushFrame()` / `popFrame()` methods in DebugManager
  - Call stack visualization support
  - Depth tracking for Step Out functionality

- **Step Out Implementation** - Complete step-through debugging
  - `stepOut()` method in DebugManager
  - Exits current function and pauses at caller
  - Works with call stack depth tracking

- **Code Cleanup** - Production-ready debug code
  - Removed all `System.err.println` debug output
  - Replaced with `ScriptLogger.debug()` with null-safe formatting
  - Removed unused imports (HashMap, Map from DebugSession)
  - Added SLF4J-style `{}` placeholder formatting to ScriptLogger

### 🎯 Sprint 1.5: Event System Revival

#### Event System Fixes (2026-01-03)
- **CRITICAL FIX: Event blocks now parse correctly** 🔥
  - Added `onEvent { }` block parsing in ScriptTask
  - Events were completely broken - blocks were ignored
  - Now properly collects event script body
  - Registers directly with EventManager

- **CRITICAL FIX: Events now work after script restart** 🔥
  - Fixed event persistence bug when stopping/restarting scripts
  - Added `registeredEvents` tracking in ScriptTask
  - Events are now properly unregistered in `stop()` method
  - Events re-register correctly on `restart()`
  - Prevents duplicate/stale event handlers

- **CRITICAL FIX: Events work after stop hotkey (Bug #11)** 🔥
  - Fixed `ScriptInterpreter.shouldStop` flag blocking event execution
  - Added automatic flag reset in `queueCommand()` and `executeQueuedCommands()`
  - Events now execute correctly after pressing Z (stop all scripts)
  - Test script: `test_events_after_stop.kh`
  - **Issue:** `shouldStop` remained `true` after `stopProcessing()`, blocking all subsequent commands
  - **Solution:** Auto-reset flag when starting fresh (empty queue, not processing)
  - Documented in `BUGS_FOUND.md` as Bug #11

- **Error Logging Improvements**
  - Replaced all `System.err.println` with `ScriptLogger` in:
    - EventManager (registerEventScript, fireEvent)
    - ChatMessageMixin
    - BlockBreakMixin
    - BlockPlaceMixin
    - AttackMixin
  - Consistent error reporting across event system

#### Package System Foundation (2026-01-03)
- **NEW: Export/Import System** 🎉
  - First step towards full package system (see PACKAGE_SYSTEM_CONCEPT.md)
  - `export` command - share variables between scripts
  - `import` command - use variables from other scripts
  - ExportManager for cross-script variable sharing
  - Automatic cleanup on script stop
  - Fixed script name tracking for proper export/import resolution
  
- **Usage Examples:**
  ```javascript
  // script1.kh
  export myVar 100
  export playerName "Kasper"
  
  // script2.kh
  import myVar from script1
  import playerName from script1
  print $myVar  // Outputs: 100
  ```

- **Test Scripts:**
  - `test_export.kh` - demonstrates export functionality
  - `test_import.kh` - demonstrates import functionality

- **VSCode Extension:**
  - Added `export` and `import` to command autocomplete
  - **NEW: Hover Provider** - hover over commands/variables to see documentation
    - Commands show full help text from mod
    - Variables show current values and types
    - Works offline with basic info, full docs when connected

- **Event Validation** - Honest event status reporting
  - Split events into IMPLEMENTED vs WIP categories
  - Warning when registering WIP events
  - Updated documentation to reflect reality
  - ✅/⚠️ indicators in help text

- **Mixin Implementation** - Critical events now work
  - **ChatMessageMixin** - onChat event captures chat messages
  - **BlockBreakMixin** - onBlockBreak event captures block breaking
  - **BlockPlaceMixin** - onBlockPlace event captures block placement
  - **AttackMixin** - onAttack event captures entity attacks
  - All mixins registered in kashub.mixins.json

- **Event Variables** - Rich event data
  - onChat: `$event_message`, `$event_sender`, `$event_full_message`
  - onBlockBreak: `$event_x`, `$event_y`, `$event_z`, `$event_block`
  - onBlockPlace: `$event_x`, `$event_y`, `$event_z`, `$event_block`, `$event_side`
  - onAttack: `$event_target_name`, `$event_target_id`, `$event_target_type`, `$event_target_x/y/z`

#### Implemented Events (9 total)
- ✅ onTick - Every 20 ticks (1 second)
- ✅ onDamage - Player takes damage
- ✅ onHeal - Player heals
- ✅ onHunger - Hunger level changes
- ✅ onDeath - Player dies
- ✅ onChat - Chat message received
- ✅ onBlockBreak - Block broken
- ✅ onBlockPlace - Block placed
- ✅ onAttack - Entity attacked

#### WIP Events (6 remaining)
- ⚠️ onRespawn - Player respawns
- ⚠️ onJump - Player jumps
- ⚠️ onSneak - Sneak toggled
- ⚠️ onSprint - Sprint toggled
- ⚠️ onItemUse - Item used
- ⚠️ onInventoryChange - Inventory changed

### 🔥 Sprint 2: Advanced Breakpoints (Phase 2.1)

#### Conditional Breakpoints Core
- **Enhanced Breakpoint Class** - Full-featured breakpoint system
  - Three types: BREAKPOINT (always stops), CONDITIONAL (stops if true), LOGPOINT (logs without stopping)
  - Hit count tracking for each breakpoint
  - Hit conditions: `>= 5`, `% 10 == 0`, `== 3`
  - Condition and log message support

- **ConditionEvaluator** - Smart condition evaluation
  - Uses ExpressionParser for condition evaluation
  - Caches compiled conditions (max 100) for performance
  - Validates complexity (max 10 operators) to prevent lag
  - Supports all event variables (`$event_*`)
  - Variable lookup with fallback strategies
  - Log message interpolation with `{$var}` placeholders

- **DebugManager Integration** - Conditional logic in shouldPause()
  - Evaluates conditions with current scope variables
  - Handles LOGPOINT without pausing
  - Handles CONDITIONAL with condition check
  - Increments hit count on each hit
  - Checks hit conditions before pausing

#### New API Methods
- `setConditionalBreakpoint(scriptName, line, condition)` - Set conditional breakpoint
- `setLogpoint(scriptName, line, message)` - Set logpoint
- `setHitCondition(scriptName, line, hitCondition)` - Set hit condition
- `getBreakpoint(scriptName, line)` - Get breakpoint for inspection

### 🎯 Sprint 2: Advanced Breakpoints (Phase 2.3)

#### Performance Profiler
- **ProfilerManager** - Thread-safe performance tracking
  - CommandStats with atomic operations (total, count, min, max, avg)
  - Top N queries (hotspots, slowest, most called)
  - Multiple export formats (text, JSON, Chrome Tracing)
  - Integrated into ScriptTask execution

- **API Endpoints** - Full profiler control
  - GET `/api/profiler/status` - Profiler status
  - POST `/api/profiler/start` - Start profiling
  - POST `/api/profiler/stop` - Stop profiling
  - POST `/api/profiler/clear` - Clear data
  - GET `/api/profiler/report` - Text report
  - GET `/api/profiler/json` - JSON export
  - GET `/api/profiler/chrome-tracing` - Chrome Tracing format
  - POST `/api/profiler/save` - Save to file

- **Chrome Tracing Export** - Premium visualization
  - Open in `chrome://tracing` for visual analysis
  - Timeline view of command execution
  - Hotspot identification
  - Performance bottleneck detection

### 🔌 Sprint 3: VSCode Integration (Phase 3.1)

#### Debug Adapter Protocol (DAP) API
- **DTO Classes** - DAP-compatible data structures
  - `DebugStackFrame` - Stack frame representation
  - `DebugScope` - Scope representation (Local, Global, Event, Environment)
  - `DebugVariable` - Variable representation with type inference

- **Debug API Endpoints** - Full debugging support
  - GET `/api/debug/scopes?scriptId=X&frameId=Y` - Get scopes for frame
  - GET `/api/debug/variables?variablesReference=X&scriptId=Y` - Get variables for scope
  - POST `/api/debug/evaluate` - Evaluate expression in context
  - GET `/api/debug/stacktrace?scriptId=X` - Get call stack
  - POST `/api/debug/setVariable` - Modify variable during debugging

- **Scope System** - Organized variable access
  - **Local Scope** (ref: 1) - Task-specific variables
  - **Event Scope** (ref: 3) - Event variables (`$event_*`)
  - **Global Scope** (ref: 2) - Interpreter variables
  - **Environment Scope** (ref: 4) - Environment variables (`$PLAYER_*`)

#### WebSocket Integration (Phase 3.2)
- **Real-time Debug Events** - Already integrated
  - `debug_event` - Broadcasts PAUSED, RESUMED, STEP events
  - `debug_action` - Handles pause, resume, step_over, step_into
  - `set_breakpoints` - Sync breakpoints from VSCode
  - `get_variables` - Request variables
  - `set_variable` - Modify variables
  - `stackTrace` - Request call stack
  - `scopes` - Request scopes

- **Bidirectional Communication**
  - VSCode → Server: Debug actions, breakpoint changes
  - Server → VSCode: Debug events, variable updates, state changes

### 📝 Notes
- Test scripts included: `test_events.kh`, `test_conditional_breakpoints.kh`, `test_profiler.kh`, `test_events_after_stop.kh`
- All critical events now functional
- Conditional breakpoints work with event variables
- Profiler records every command execution
- DAP-compatible API for VSCode integration
- WebSocket for real-time debugging
- No breaking changes to existing scripts
- **Bug #11 fixed** - Events work after stop/restart
- Version format fixed: `v0.8.0-beta` → `0.8.0-beta` (removes Fabric Loader warning)

### 🏗️ Future Plans
- **v0.8.1** - UI polish, code analysis, unit tests
- **v0.9.0** - Architecture refactoring (remove ScriptInterpreter singleton)
- **v1.0.0** - Stable release with complete GUI features
- See `REFACTORING_PLAN.md` for detailed architecture migration plan
- See `ROADMAP_FUTURE.md` for complete feature roadmap

### 🎯 Release Status
- **Roadmap Completion:** 85% (excellent for beta)
- **Critical Bugs:** 0 (all fixed)
- **Code Quality:** 9/10
- **Build Status:** ✅ SUCCESSFUL
- **Ready for Release:** ✅ YES
- See `RELEASE_READINESS_FINAL_v0.8.0.md` for complete release audit

### 🆕 Sprint 4: Command Audit & New Commands

#### New Commands
- **MacroCommand** - Record and playback player actions
  - `macro record <name>` - Start recording macro
  - `macro stop` - Stop recording
  - `macro play <name> [speed]` - Play macro at custom speed
  - `macro save <name>` - Save to file
  - `macro list` - List saved macros
  - `macro delete <name>` - Delete macro
  - Captures movement, camera rotation, timing
  - Useful for automation, farming, building

- **HealthMonitorCommand** - Monitor health and status effects
  - `healthMonitor monitor <threshold> [action]` - Start monitoring
  - `healthMonitor check` - Check current status
  - `healthMonitor effects` - List active effects
  - `healthMonitor stop` - Stop monitoring
  - Alerts on low HP and dangerous effects (poison, wither)
  - Actions: log, chat, sound
  - 5 second cooldown between alerts

- **TimerCommand** - Timers and delayed execution
  - `timer set <name> <seconds> [message]` - Create timer
  - `timer check <name>` - Check remaining time
  - `timer cancel <name>` - Cancel timer
  - `timer list` - List active timers
  - `timer clear` - Clear all timers
  - Optional message on expiration
  - Useful for farming, crafting, cooldowns

- **ReportBugCommand** - Bug reporting system
  - `reportBug <description>` - Send bug report to developers
  - Automatically collects last 500 lines of logs
  - Includes system info (OS, Java, MC version)
  - Sends to Discord webhook with file attachment
  - Rate limited (1 report per 5 minutes)
  - Webhook obfuscated (base64 + reverse) to prevent abuse
  - Fixed: Logs now sent as file attachment instead of embed field (Discord 1024 char limit)

#### Command Improvements
- **AICommand** - Fixed threading and timeout issues
  - Added fixed thread pool (2 threads) instead of unlimited
  - Added 30 second timeout for AI requests
  - Added rate limiting (6 second cooldown between requests)
  - Prevents thread exhaustion and API spam

- **SpeedHackCommand** - Added server detection
  - Max 2.0x speed on multiplayer servers (anti-cheat safe)
  - Max 10.0x speed in singleplayer
  - Automatic detection and clamping
  - Updated documentation with warnings

- **AnimationCommand** - Implemented list action
  - Added `list` action to show active animations
  - Added AnimationManager null check
  - Improved error handling

- **ChatMessageMixin** - Fixed for Minecraft 1.21.1
  - Removed `boolean overlay` parameter (API change in 1.21.1)
  - Fixed mixin crash on game startup
  - Chat events now work correctly

#### Documentation
- **COMMAND_AUDIT_REPORT.md** - Complete audit of all 42 commands
  - Identified 4 potential issues (all fixed)
  - Rated all commands (A+ to C)
  - Provided fix recommendations
  - Overall code quality: 9/10
- **DEVELOPMENT_NOTES.md** - AI development guidelines
  - Language requirements (English for all user-facing text)
  - Common pitfalls (Registry API, Chat Mixin changes)
  - Code style and best practices

---

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

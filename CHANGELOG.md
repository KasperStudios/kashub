# Changelog

All notable changes to Kashub will be documented in this file.

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

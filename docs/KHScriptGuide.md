# KHScript Guide

A comprehensive guide for scripting with the Kashub (KHScript) automation system.

## Table of Contents
1. [Getting Started](#getting-started)
2. [Basics](#basics)
3. [Control Flow](#control-flow)
4. [Variables & Context](#variables--context)
5. [Movement & Navigation](#movement--navigation)
6. [Interactions & Combat](#interactions--combat)
7. [Utility Commands](#utility-commands)
8. [Advanced Features](#advanced-features)
9. [Inventory Management](#inventory-management)
10. [Pathfinding](#pathfinding)
11. [Examples](#examples)
12. [Tips & Best Practices](#tips--best-practices)

## Getting Started
### Requirements
- Kashub mod installed (Fabric 1.21.1)
- Open the KHScript editor (`K` by default)
- Scripts stored in `run/kashub/scripts` or bundled `assets/kashub/scripts`

### Editor Overview
- Left sidebar: script list, search, create new
- Toolbar buttons:
  - `▶ Run`, `⏹ Stop`, `💾 Save`, `⌨ Key` to bind scripts, `📚 Docs`, `📊 Tasks`, `⚙ Set`, `🎨 Theme`, `❌ Close`
- Editor supports syntax highlighting, vertical + horizontal scrolling, line numbers

### Keybinds
- Assign scripts to keys via `⌨ Key` button in editor
- Config stored in `kashub/config.json` > `scriptKeybinds`
- Scripts run when key pressed (only in-game, not in GUI)

## Basics
### Syntax
- One command per line
- Comments with `//`
- Variables via `$name`
- Strings use `"double quotes"`

### Core Commands
```
print <message>
log <message>
wait <ms>
chat <message>
```

### Running Scripts
- `run <script_name>` from scripts command
- Editor `▶ Run` button
- Keybinds (if configured)

## Control Flow
```
// Loop N times
loop 10
    print "Iteration"
end

// For loop with counter
for (i = 0; i < 10; i++) {
    print "Counter: $i"
}

// While loop
while ($health > 10) {
    attack
    wait 500
}

// If-else
if ($PLAYER_HEALTH < 10) {
    print "Low health!"
    eat
} else {
    print "Health OK"
}

// Logical operators
if ($x > 5 && $y < 10) {
    print "Both conditions true"
}

if ($health < 5 || $food < 3) {
    print "Need resources"
}
```
- `while`, `for`, `loop` supported
- `break`, `continue` for loop control
- `&&` (AND) and `||` (OR) logical operators
- Modulo operator `%` in conditions: `if ($i % 8 == 0)`

## Variables & Context
- Set variable: `myVar = value` (without $ on left side)
- Use variable: `$myVar` (with $ prefix)
- Use: `print Value: $myVar`
- Built-in context from commands:
  - `scan_*`, `inv_*`, `pathfind_*`, `player_*`, etc.
- `ScriptInterpreter.getContext()` accessible for advanced integration

## Movement & Navigation
### Movement Commands
```
input forward/back/left/right <ms>
input jump <ms>
input sprint on/off
input sneak on/off
input look <yaw> <pitch>
```

### Teleport & Movement
```
tp <x> <y> <z>
moveto <x> <y> <z>
lookat <x> <y> <z> | rel <dx> <dy> <dz>
selectslot <0-8>
```

### New Pathfinding Command
See [Pathfinding](#pathfinding)

## Interactions & Combat
```
attack
useitem
place <block>
break
interact
```
- Combine with `lookat`/`moveto`
- Example: automate mining, farming, combat behaviors

## Utility Commands
```
scan blocks|view|nearest|ores
vision player/entity/block
http get/post
sound play <id>
onevent <event> {...}
```
- `scan` now supports visibility checks when cheats disabled
- `vision` populates `$target_*` vars (block, entity, position)

## Advanced Features
### Events
```
onevent tick
    // commands each tick
end
```
- Available events: `tick`, `join`, `leave`, custom triggers

### HTTP Service
- `http get <url>` sets `$http_status`, `$http_body`
- Inline scripts via `http inline <code>`
- Requires API enabled in config

### AI Command
- `ai prompt <text>` uses configured AI service (Groq/OpenAI)

## Inventory Management
New `inventory` command:
```
inventory check             // sets $inv_full, $inv_empty_slots, $inv_used_slots
inventory count <item>      // sets $inv_item_count, $inv_has_item
inventory find <item>       // sets $inv_found_slot, $inv_item_found
inventory empty             // counts empty slots
inventory drop <slot>
inventory swap <slot1> <slot2>
```
- Slots 0-8: hotbar, 9-35: main inventory
- Use with `if $inv_full == true ...`

## Pathfinding
`pathfind` command uses A* navigation:
```
pathfind <x> <y> <z>
pathfind sethome
pathfind home
pathfind stop
```
Variables set:
- `$pathfind_active` (true/false)
- `$pathfind_complete`
- `$home_x`, `$home_y`, $home_z
- `$pathfind_length`

Pathfinding runs each tick (client-side). Handles stairs/jumps, avoids obstacles, basic parkour.

## Examples
### Area Clearer (4-block-high)
`assets/kashub/scripts/example_area_clearer.kh`
- Sweeps view up/down to clear 4-high tunnels
- Ideal for manual movement + auto-breaking

### Deepslate Miner
`assets/kashub/scripts/example_deepslate_miner.kh`
- Uses `inventory check`, `scan ores`, `pathfind sethome`
- Pattern-based strip mining with status prints

### HTTP, Vision, Tasks, Animations
See bundled scripts in `assets/kashub/scripts/`:
- `example_http.kh`, `example_vision.kh`, `example_tasks.kh`, etc.

## Tips & Best Practices
1. **Keybinds**: Use small scripts for hotkey automation (e.g., auto-eat, auto-jump)
2. **Variables**: Use meaningful names, reset when needed
3. **Delays**: Always `wait` between actions to prevent desync
4. **Safety**: Check `$player_health`, `$inv_full` before risky actions
5. **Logging**: Use `log` for console, `print` for chat UI
6. **Docs**: In-game docs (`📚`) now support horizontal scrolling
7. **Tasks**: Manage via `📊 Tasks` dialog or `/script tasks`
8. **Themes**: `🎨` button cycles editor themes (Dracula, Gruvbox, Tokyo, Catppuccin)

## README Integration
Add summary to `README.md`:
```
## KHScript Highlights
- Powerful in-game editor (keybinds, syntax highlight, scrolling)
- 40+ commands (movement, vision, HTTP, AI, inventory, pathfinding)
- Task manager & documentation dialogs
- Example scripts in assets/kashub/scripts/**
- Customizable themes & keybinds
```

## New Features (v2.1)

### Type System
KHScript now supports optional typing for variables:

```javascript
// Enable type checking
@type strict  // or 'loose' or 'off'

// Typed variable declaration
count: number = 10
name: string = "Player"
isActive: bool = true
pos: position = "100, 64, 200"
```

Available types: `number`, `string`, `bool`, `position`, `item`, `entity`, `block`, `any`

### Auto-Trading
Automate trading with villagers:

```javascript
// Configure auto-trade
autoTrade config 32 10 cheapest

// Set target items
autoTrade target diamond,enchanted_book,mending

// Scan for merchants
autoTrade scan

// View available trades
autoTrade offers

// Buy specific trade
autoTrade buy 0 5  // buy trade #0, quantity 5

// Start auto-trading
autoTrade start
autoTrade stop
```

Variables set:
- `$autoTrade_merchantCount`, `$autoTrade_offersCount`
- `$autoTrade_offer_N_sell`, `$autoTrade_offer_N_buy1`
- `$autoTrade_buySuccess`, `$autoTrade_active`

### Auto-Crafting
Automate crafting recipes:

```javascript
// Check if item can be crafted
autoCraft check diamond_pickaxe

// Craft items
autoCraft recipe diamond_pickaxe 5

// List craftable items
autoCraft list
autoCraft list pickaxe  // filter

// Show missing resources
autoCraft missing diamond_chestplate

// Stop crafting
autoCraft stop
```

Variables set:
- `$autoCraft_canCraft`, `$autoCraft_maxCraftable`
- `$autoCraft_crafted`, `$autoCraft_progress`
- `$autoCraft_missing_N_item`, `$autoCraft_missing_N_count`

### Enhanced Pathfinding
Improved navigation with danger avoidance:

```javascript
// Basic navigation
pathfind 100 64 200

// With options
pathfind 100 64 200 avoidDanger=true allowParkour=false maxFall=3 sprint=true

// Configure globally
pathfind config avoidDanger true
pathfind config maxIterations 3000

// Cache management
pathfind cache clear

// Home system
pathfind sethome
pathfind home
```

Features:
- **Danger avoidance**: Lava, fire, cactus, magma blocks
- **Climbing**: Ladders, vines, scaffolding
- **Swimming**: Water navigation
- **Parkour**: Jump over 2-block gaps
- **Async**: Non-blocking path calculation
- **Caching**: Reuse calculated paths

### Advanced Scanner
Powerful block and entity scanning:

```javascript
// Scan blocks with options
scanner blocks diamond_ore,ancient_debris radius=64 yMin=0 yMax=16 sortBy=distance limit=20

// Scan entities
scanner entities villager,wandering_trader radius=50 healthMin=20 hasAI=true

// Entity categories: hostile, passive, living, merchant, all
scanner entities hostile radius=30

// Cache management
scanner cache clear
```

Variables set:
- `$scanner_count`, `$scanner_found`
- `$scanner_N_x/y/z`, `$scanner_N_block/dist`
- `$scanner_nearest_x/y/z`, `$scanner_nearest_block`
- `$scanner_entity_count`, `$scanner_entity_N_type/health`

### CrashGuard - Crash Protection
Protects against script crashes:

```javascript
// Show status
crashGuard status

// Set strictness level
crashGuard config strictness=medium  // off, loose, medium, strict, paranoid

// Per-script strictness
crashGuard strictness loose my_script.kh

// Temporarily disable protection
crashGuard pause 5000  // 5 seconds

// Emergency stop script
crashGuard stop my_script.kh

// Clean up resources
crashGuard cleanup

// Detailed report
crashGuard report
```

**Strictness Levels:**
| Level | CPU Limit | Actions/sec | Loop Iterations |
|-------|-----------|-------------|-----------------|
| off | ∞ | ∞ | ∞ |
| loose | 50ms | 50 | 10000 |
| medium | 10ms | 20 | 5000 |
| strict | 5ms | 10 | 2000 |
| paranoid | 2ms | 5 | 500 |

**Script Directives:**
```javascript
#crashguard strictness=loose
#crashguard allowInfiniteLoops=true
```

**Variables:**
- `$crashGuard_strictness` - Current level
- `$crashGuard_cpuTotal` - CPU time (ms)
- `$crashGuard_actionsTotal` - Actions/sec

## Editor Improvements

### Undo/Redo
- `Ctrl+Z` - Undo last change
- `Ctrl+Y` or `Ctrl+Shift+Z` - Redo

### Draft System
- Auto-saves unsaved changes every 30 seconds
- Drafts stored in `config/kashub/drafts/`
- Recovery prompt on next editor open

### Autocomplete
- Trigger with `Tab` key
- Commands, variables, snippets
- Environment variables (`$PLAYER_*`)

### Scrolling
- **Vertical scroll**: Mouse wheel
- **Horizontal scroll**: `Shift` + Mouse wheel
- Works in editor and documentation dialog

## Eval Command (Advanced)

The `eval` command allows executing raw Java code within scripts. **This is a dangerous feature** and must be explicitly enabled in settings.

### Enabling Eval
1. Open Settings (`⚙ Set` button)
2. In Security section, enable "Allow Eval ⚠"
3. Save settings

### Usage
```javascript
// Simple Java code
eval System.out.println("Hello from Java!");

// Access Minecraft client
eval print("Player: " + mc.player.getName().getString());

// Access script variables
myVar = "test"
eval print("Variable: " + vars.get("myVar"));

// Modify player (if allowed)
eval player.setHealth(20.0f);

// Complex logic
eval {
    for (int i = 0; i < 5; i++) {
        print("Count: " + i);
        Thread.sleep(500);
    }
}
```

### Available Objects
| Object | Type | Description |
|--------|------|-------------|
| `mc` | MinecraftClient | Minecraft client instance |
| `player` | PlayerEntity | Current player |
| `world` | World | Current world |
| `interpreter` | ScriptInterpreter | Script interpreter |
| `vars` | Map<String, String> | Script variables |

### Helper Methods
- `print(String message)` - Send message to chat
- `getVar(vars, "name")` - Get variable value
- `setVar(vars, "name", "value")` - Set variable value

### Security Notes
- ⚠ **Eval can execute ANY Java code** - use with extreme caution
- Code runs with full mod permissions
- 10 second timeout per eval call
- Requires JDK (not JRE) for compilation
- Disabled by default for safety

## Future Extensions
- Full inventory auto-stash (pathfind to chest + deposit)
- Advanced pathfinding (multi-floor, nether portals)
- Script scheduler & macros
- Recursive crafting (auto-craft components)

---
Happy scripting ⚡

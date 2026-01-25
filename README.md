# 🎮 Kashub – In‑Game Scripting Engine for Minecraft

<p align="center">
  <img src="src/main/resources/assets/kashub/icon.png" alt="Kashub Logo" width="128"/>
</p>

<p align="center">
  <strong>Current Version: v0.9.0-beta</strong><br>
  <em>Modern scripting engine with VSCode-style editor</em>
</p>

### In‑game scripting and automation framework with a built‑in VSCode‑style editor
Tired of repetitive tasks? Want to automate farming, building, or grinding? **Kashub** adds a powerful scripting layer to Minecraft with a beautiful built‑in code editor inspired by VSCode.

---

## 🆕 What's New in v0.9.0-beta
- **🛡️ CrashGuard Command** – Protect your scripts from crashes, errors, and FPS drops with `crashguard { }`
- **🖥️ System Object** – New global object with `System.print()`, `System.log()`, `System.wait()`, `System.memory()`, and more
- **🎨 Enhanced Syntax Highlighting** – Object methods now have distinct colors (orange for objects, purple for methods)
- **✅ Improved Validation** – Object-oriented commands (player.attack, vision.nearest) are now properly recognized
- **🛑 Fixed Loop Stopping** – Scripts with while loops now stop immediately when pressing Stop button
- **🔧 Better Error Detection** – Real-time syntax validation with detailed error messages
- **📚 Complete V2 API** – All core objects (player, scanner, vision, inventory) fully implemented

---

## ⚡ Why Kashub?
Unlike other automation mods that require external tools or complex setups, Kashub gives you everything in‑game:
- **No external editors needed** – Open the editor with `K`
- **Easy to learn** – KHScript with Lua/JavaScript‑inspired syntax
- **Beautiful UI** – 10+ professional editor themes
- **Multiplayer‑proven** – Used on real servers in real scenarios
- **Open source** – MIT License, fully transparent

---

## 🔥 Key Features

### 🖥️ Built‑in Code Editor
- **Syntax highlighting** for KHScript
- **Auto‑completion** (`Ctrl+Space`)
- **10+ themes** – Dracula, One Dark Pro, Tokyo Night, Cyberpunk, and more
- **Search & script browser** – Find and manage scripts quickly
- **Keybind support** – Launch scripts with hotkeys

### ⚙️ Scripting Engine (V2)
- **Object-Oriented Syntax** – Modern API: `player.getHealth()`, `scanner.blocks()`, `inventory.check()`
- **CrashGuard Protection** – Wrap risky code in `crashguard { }` to prevent crashes and monitor performance
- **System Object** – Global utilities: `System.print()`, `System.log()`, `System.wait()`, `System.memory()`
- **Functions & Loops** – Full support for `fn`, `if/else`, `for`, `while`, `break`, `continue`
- **Variables** – Proper scoping with `let` and `const`
- **Type Safety** – Null checks, proper return types, method chaining
- **Rich Objects** – `player`, `scanner`, `vision`, `inventory` with 30+ methods

### 🛠️ Developer Tools
- **Deep Debugging** – Breakpoints, step-through, variable watching
- **Performance Profiler** – Track execution times and export to Chrome Tracing
- **Logging system** – Real-time debug output
- **Task manager** – Run and manage multiple scripts simultaneously

---

## 🛡️ Fair‑Play & Server Control
Kashub is designed to be server‑friendly:
- **Server‑authoritative control**: Server-side config can override client settings.
- **Editor Restrictions**: Admins can disable the editor on specific servers.
- **Command Blacklisting**: Block specific commands or namespaces from running.
- **Respectful Client**: The mod respects server decisions and does not bypass them.

---

## 📚 Quick Start
1. Install **Fabric 1.21.1** and **Fabric API**.
2. Drop **Kashub** into your `mods` folder.
3. Launch Minecraft and press `K` to open the editor.
4. Write your first script:
   ```javascript
   let health = player.getHealth()
   let name = player.getName()
   
   print "Hello, " + name + "!"
   print "Your health: " + health
   
   if (health < 10) {
       print "Low health! Be careful!"
   }
   ```
5. Press `F5` to run!

---

## 💡 Example Scripts

### Safe Mining with CrashGuard
```javascript
// Protected mining operation
crashguard(timeout=10000, minFps=25) {
    System.print("Starting safe mining...")
    
    let ores = scanner.blocks("diamond_ore,iron_ore", 64)
    
    if (ores.count > 0) {
        System.print("Found " + ores.count + " ores")
        
        let i = 0
        while (i < ores.count) {
            let ore = ores.blocks[i]
            player.moveTo(ore.x, ore.y, ore.z)
            System.wait(500)
            i = i + 1
        }
    }
    
    System.print("Mining complete!")
}
```

### Auto-Heal
```javascript
// Monitor health and auto-heal
while (true) {
    let health = player.getHealth()
    
    if (health < 10) {
        System.print("Low health! Healing...")
        inventory.use("golden_apple")
        System.wait(5000)
    }
    
    System.wait(1000)
}
```

### Ore Finder
```javascript
// Find and mine nearest diamond
let ores = scanner.blocks("diamond_ore", 64)

if (ores.length > 0) {
    let nearest = ores[0]
    System.print("Found diamond ore at " + nearest.x + ", " + nearest.y + ", " + nearest.z)
    
    player.moveTo(nearest.x, nearest.y, nearest.z)
    player.lookAt(nearest.x, nearest.y, nearest.z)
    
    System.print("Arrived at diamond ore!")
} else {
    System.print("No diamonds nearby")
}
```

### Combat Bot
```javascript
// Auto-attack nearest hostile mob
crashguard(minFps=30) {
    while (true) {
        let enemy = vision.nearest("hostile", 4, "head")
        
        if (enemy != null) {
            player.lookAt(enemy.pos.x, enemy.pos.y, enemy.pos.z)
            player.attack(enemy)
            System.print("Attacking " + enemy.type + " (HP: " + enemy.health + ")")
        }
        
        System.wait(100)
    }
}
```

---

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

---

## 🔌 VSCode Integration
Kashub includes a full VSCode Extension for a professional development experience!
- **DAP Implementation**: Full debugger support from VSCode.
- **IntelliSense**: Powered by the actual Kashub parser.
- **Kashub Console**: Live output stream directly in your IDE.

---

## 🛠️ Technical Details & Links
- **Loader**: Fabric 1.21.1
- **GitHub**: [KasperStudios/Kashub](https://github.com/KasperStudios/Kashub)
- **Discord**: [KasHub Community](https://discord.gg/gFeWtpEKN9)
- **Modrinth**: [kashub](https://modrinth.com/mod/kashub)
- **License**: MIT

---

## 📝 Full Documentation

**See [KHScriptGuide.md](docs/KHScriptGuide.md) for complete V2 syntax documentation.**

<details>
<summary><b>Quick Object Reference</b></summary>

### System Object
```javascript
System.print("message")    // Output to chat and console
System.log("message")      // Output to console only
System.chat("message")     // Send chat message
System.wait(ms)            // Sleep for milliseconds
System.time()              // Get current timestamp
System.exit()              // Stop script execution
System.gc()                // Request garbage collection
System.memory()            // Get memory usage info
```

### Player Object
```javascript
player.getHealth()        // Get health (0-20)
player.getHunger()        // Get hunger (0-20)
player.getName()          // Get player name
player.getPos()           // Get position {x, y, z}
player.moveTo(x, y, z)    // Move to coordinates
player.lookAt(x, y, z)    // Look at coordinates
player.attack()           // Attack entity at crosshair
player.attack(entity)     // Attack specific entity
player.breakBlock()       // Break block at crosshair
player.placeBlock(name)   // Place block by name
player.sprint(true/false) // Enable/disable sprint
player.chat("message")    // Send chat message
player.animation("play", "name", duration)  // Play animation
```

### Scanner Object
```javascript
scanner.blocks("diamond_ore", 32)  // Find blocks in radius
scanner.entities("zombie", 50)     // Find entities in radius
// Returns array of {x, y, z, id, dist, health}
```

### Vision Object
```javascript
vision.getTarget()                 // Get crosshair target
vision.getNearest("zombie", 30)    // Find nearest entity
vision.nearest("hostile", 40)      // Alias for getNearest
vision.count("zombie", 30)         // Count entities
vision.canSee("zombie", 20)        // Check visibility
vision.isLookingAt("zombie", 5)    // Check if looking at entity
// Returns {type, id, pos, x, y, z, distance, health}
```

### Inventory Object
```javascript
inventory.check()                  // Get inventory status
inventory.count("diamond")         // Count items
inventory.find("diamond_sword")    // Find item slot
inventory.getItems()               // Get all items
inventory.getEmptySlots()          // Count empty slots
inventory.drop(slot, dropAll)      // Drop item
inventory.swap(slot1, slot2)       // Swap items
inventory.equip(slot)              // Equip armor/shield
inventory.use("golden_apple")      // Use item by name
inventory.craft("diamond_sword", 1) // Craft item
```

**Full API documentation:** [KHScriptGuide.md](docs/KHScriptGuide.md)
</details>

<details>
<summary><b>Click to expand Configuration</b></summary>

Config file: `config/kashub/config.json`
```json
{
  "editorTheme": "dracula",
  "editorFontSize": 12,
  "sandboxMode": true,
  "allowCheats": false,
  "maxLoopIterations": 10000
}
```
</details>

---
<p align="center">Made with ❤️ by KasperStudios</p>

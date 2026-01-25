# V2 API Migration Guide

This guide helps you migrate from V1 command-style syntax to V2 object-oriented syntax.

## Overview

Kashub v0.9.0 introduces a new object-oriented API that's more intuitive and powerful. The old command-style syntax still works for backward compatibility, but we recommend migrating to V2.

## Key Changes

### 1. Print → System.print()

**V1 (Old):**
```javascript
print "Hello, World!"
```

**V2 (New):**
```javascript
System.print("Hello, World!")
```

### 2. wait → System.wait()

**V1 (Old):**
```javascript
wait 1000
```

**V2 (New):**
```javascript
System.wait(1000)
```

### 3. moveTo → player.moveTo()

**V1 (Old):**
```javascript
moveTo 100 64 200
```

**V2 (New):**
```javascript
player.moveTo(100, 64, 200)
```

### 4. attack → player.attack()

**V1 (Old):**
```javascript
attack hostile 5
```

**V2 (New):**
```javascript
let enemy = vision.nearest("hostile", 5)
if (enemy != null) {
    player.attack(enemy)
}
```

### 5. scanner → scanner.blocks()

**V1 (Old):**
```javascript
scanner blocks diamond_ore 32
```

**V2 (New):**
```javascript
let ores = scanner.blocks("diamond_ore", 32)
if (ores.length() > 0) {
    let nearest = ores[0]
    System.print("Found ore at " + nearest.x + ", " + nearest.y + ", " + nearest.z)
}
```

### 6. inventory → inventory.count()

**V1 (Old):**
```javascript
inventory check diamond
```

**V2 (New):**
```javascript
let diamonds = inventory.count("diamond")
System.print("You have " + diamonds + " diamonds")
```

## New Objects

### System Object
```javascript
System.print(message)    // Output to chat and console
System.log(message)      // Output to console only
System.chat(message)     // Send chat message
System.wait(ms)          // Sleep for milliseconds
System.time()            // Get current timestamp
System.exit()            // Stop script
System.gc()              // Garbage collection
System.memory()          // Memory usage info
```

### player Object
```javascript
player.getHealth()       // Get health (0-20)
player.getHunger()       // Get hunger (0-20)
player.getName()         // Get player name
player.getPos()          // Get position {x, y, z}
player.moveTo(x, y, z)   // Move to coordinates
player.lookAt(x, y, z)   // Look at coordinates
player.sprint(enabled)   // Enable/disable sprint
player.attack([entity])  // Attack entity
player.breakBlock()      // Break block
player.placeBlock(name)  // Place block
player.chat(message)     // Send chat message
```

### scanner Object
```javascript
scanner.blocks(type, radius)    // Find blocks
scanner.entities(type, radius)  // Find entities
```

### vision Object
```javascript
vision.getTarget()              // Get crosshair target
vision.nearest(type, maxDist)   // Find nearest entity
vision.count(type, maxDist)     // Count entities
vision.canSee(type, maxDist)    // Check visibility
```

### inventory Object
```javascript
inventory.check()               // Get inventory status
inventory.count(itemName)       // Count items
inventory.find(itemName)        // Find item slot
inventory.getItems()            // Get all items
inventory.use(itemName)         // Use item
inventory.craft(item, count)    // Craft item
```

### Math Object (NEW!)
```javascript
Math.sqrt(x)         // Square root
Math.abs(x)          // Absolute value
Math.min(a, b)       // Minimum
Math.max(a, b)       // Maximum
Math.floor(x)        // Round down
Math.ceil(x)         // Round up
Math.round(x)        // Round to nearest
Math.random()        // Random 0-1
Math.pow(base, exp)  // Power
```

## Complete Example Migration

### V1 Auto-Heal Script
```javascript
// V1 - Old style
loop {
    if $PLAYER_HEALTH < 10 {
        print "Low health!"
        inventory use golden_apple
        wait 5000
    }
    wait 1000
}
```

### V2 Auto-Heal Script
```javascript
// V2 - New style
while (true) {
    let health = player.getHealth()
    
    if (health < 10) {
        System.print("Low health!")
        inventory.use("golden_apple")
        System.wait(5000)
    }
    
    System.wait(1000)
}
```

## VSCode Extension Features

### Syntax Highlighting
- Objects are highlighted in **orange**
- Methods are highlighted in **purple**
- Keywords are highlighted in **blue**

### Auto-completion
Type object name and press `Ctrl+Space`:
```javascript
player.  // Shows: getHealth, moveTo, attack, etc.
System.  // Shows: print, log, wait, etc.
```

### Snippets
- `sysprint` → `System.print(message)`
- `pmove` → `player.moveTo(x, y, z)`
- `scanblocks` → `scanner.blocks("type", radius)`
- `autoheal` → Complete auto-heal template
- `combat` → Complete combat bot template
- `mining` → Complete mining bot template

### Hover Documentation
Hover over any method to see:
- Description
- Usage example
- Return type
- Parameters

## Tips

1. **Use let for variables**: `let health = player.getHealth()`
2. **Check for null**: Always check if `vision.nearest()` returns null
3. **Use .length()**: Arrays have `.length()` method, not `.length` property
4. **CrashGuard**: Wrap risky code in `crashguard { }` blocks
5. **System.log()**: Use for debugging without spamming chat

## Need Help?

- Check the [full documentation](../docs/README.md)
- Join our [Discord](https://discord.gg/gFeWtpEKN9)
- Report issues on [GitHub](https://github.com/KasperStudios/Kashub/issues)

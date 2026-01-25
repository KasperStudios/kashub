# API Overview

KasHub uses the **KHScript** language (based on simplified JavaScript). 

## Core Objects

These objects are available globally in all scripts.

| Object | Description | Documentation |
|:-------|:------------|:--------------|
| `System` | System utilities (print, wait, chat) | [api/system-object.md](api/system-object.md) |
| `player` | Player actions (move, attack, interact) | [api/player-object.md](api/player-object.md) |
| `inventory` | Inventory management | [api/inventory-object.md](api/inventory-object.md) |
| `world` | World information (blocks, time, biomes) | [api/world-object.md](api/world-object.md) |
| `vision` | Entity detection and raycasting | [api/vision-object.md](api/vision-object.md) |
| `scanner` | Block scanning and detection | [api/scanner-object.md](api/scanner-object.md) |
| `game` | Game settings and state (FPS, ping) | [api/game-object.md](api/game-object.md) |
| `w2p` | World-to-Player utilities (math, angles) | [api/w2p-object.md](api/w2p-object.md) |
| `Math` | Mathematical functions | [api/math-object.md](api/math-object.md) |
| `tag` | Item and block tag checks | [api/tag-object.md](api/tag-object.md) |

## Quick Reference

### Player Movement
```javascript
player.moveTo(x, y, z)      // Pathfind to coordinates
player.stopMove()           // Stop moving
player.lookAt(x, y, z)      // Look at coordinates
player.jump()               // Jump
player.attack(entity)       // Attack entity
player.interact()           // Right-click
```

### Entity Detection
```javascript
let enemy = vision.nearest("hostile", 32)
if (enemy != null) {
    player.attack(enemy)
}
```

### Block Scanning
```javascript
let diamonds = scanner.blocks("diamond_ore", 16)
if (diamonds.length() > 0) {
    System.print("Found diamond!")
}
```

### Inventory
```javascript
if (inventory.count("golden_apple") > 0) {
    inventory.use("golden_apple")
}
```

## See Also

- [Pathfinding Guide](pathfinding-guide.md)
- [KHScript Syntax](khscript-syntax.md)
- [Example Scripts](examples/README.md)

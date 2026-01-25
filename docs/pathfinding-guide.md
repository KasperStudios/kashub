# Pathfinding Guide

Complete guide for KasHub's advanced pathfinding system.

## Overview

KasHub features an A* pathfinding algorithm integrated into `player.moveTo()`. The pathfinding system automatically navigates your player around obstacles, handles jumps, falls, swimming, and climbing.

## Basic Usage

### Simple Navigation

```javascript
// Move to specific coordinates
let pos = player.getPos()
player.moveTo(pos.x + 50, pos.y, pos.z + 50)

// Wait until arrived
while (player.isMoveActive()) {
    System.print("Moving...")
    System.wait(1000)
}

System.print("Arrived!")
```

### Stop Mid-Navigation

```javascript
// Start moving
player.moveTo(100, 64, 200)

// Stop after 5 seconds
System.wait(5000)
player.stopMove()
```

### Check Movement Status

```javascript
if (player.isMoveActive()) {
    System.print("Currently navigating...")
} else {
    System.print("Idle")
}
```

## Advanced Features

### Automatic Obstacle Avoidance

The pathfinding automatically navigates around obstacles:

```javascript
// Automatically finds path around walls, trees, etc.
player.moveTo(targetX, targetY, targetZ)
```

### Jump Navigation

Pathfinding handles jumps up to 1 block height automatically:

```javascript
// Automatically jumps up stairs, ledges
player.moveTo(higherY_coordinate)
```

### Fall Navigation

Safe falling up to configured height (default: 3 blocks):

```javascript
// Automatically handles safe falls
player.moveTo(lowerY_coordinate)
```

### Swimming

Pathfinding can navigate through water:

```javascript
// Swims through water automatically
player.moveTo(coordinatesAcrossWater)
```

### Climbing

Supports ladders, vines, and scaffolding:

```javascript
// Climbs ladders automatically
player.moveTo(coordinatesUpLadder)
```

## Configuration

### Default Settings

- **Avoid Danger**: `true` - Avoids lava, fire, cactus, etc.
- **Allow Parkour**: `false` - Disables 2-block gap jumps
- **Max Fall Distance**: `3` - Maximum safe fall height
- **Use Sprint**: `true` - Sprints when far from target
- **Allow Swim**: `true` - Can navigate through water
- **Max Iterations**: `2000` - Maximum pathfinding iterations

## Path Caching

Paths are automatically cached for 30 seconds to improve performance when navigating to the same coordinates multiple times.

**Cache Behavior:**
- Max cache size: 50 paths
- Cache expiry: 30 seconds
- Automatically clears expired paths

## Performance

### Optimization Tips

1. **Use Radius Parameter**: Allow some tolerance to reach target faster
   ```javascript
   player.moveTo(x, y, z, 2.0) // Accept 2-block radius
   ```

2. **Check Distance First**: Avoid pathfinding for very close targets
   ```javascript
   let pos = player.getPos()
   let dist = Math.sqrt(
       Math.pow(targetX - pos.x, 2) + 
       Math.pow(targetZ - pos.z, 2)
   )
   
   if (dist < 3) {
       // Just move directly
       player.input("forward", "press")
   } else {
       // Use pathfinding
       player.moveTo(targetX, targetY, targetZ)
   }
   ```

3. **Use CrashGuard**: Protect against infinite loops
   ```javascript
   crashguard(timeout=30000, minFps=20) {
       player.moveTo(x, y, z)
       while (player.isMoveActive()) {
           System.wait(100)
       }
   }
   ```

## Common Patterns

### Waypoint Navigation

```javascript
let waypoints = [
    {x: 100, y: 64, z: 200},
    {x: 150, y: 64, z: 250},
    {x: 200, y: 64, z: 200}
]

let i = 0
while (i < 3) {
    let wp = waypoints[i]
    
    System.print("Moving to waypoint " + (i + 1))
    player.moveTo(wp.x, wp.y, wp.z)
    
    while (player.isMoveActive()) {
        System.wait(100)
    }
    
    System.print("Reached waypoint " + (i + 1))
    i = i + 1
}
```

### Mining Navigation

```javascript
let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    let i = 0
    while (i < ores.length()) {
        let ore = ores[i]
        
        // Navigate to ore
        player.moveTo(ore.x, ore.y, ore.z, 1.5)
        
        while (player.isMoveActive()) {
            System.wait(100)
        }
        
        // Mine it
        player.lookAt(ore.x, ore.y, ore.z)
        player.breakBlock()
        System.wait(2000)
        
        i = i + 1
    }
}
```

### Return Home Function

```javascript
let homeX = 0
let homeY = 64
let homeZ = 0

fn setHome() {
    let pos = player.getPos()
    homeX = pos.x
    homeY = pos.y
    homeZ = pos.z
    System.print("Home set!")
}

fn goHome() {
    System.print("Returning home...")
    player.moveTo(homeX, homeY, homeZ)
    
    while (player.isMoveActive()) {
        System.wait(500)
    }
    
    System.print("Home!")
}

// Usage
setHome()
// ... explore ...
goHome()
```

### Safe Navigation

```javascript
fn safeMoveTo(x, y, z) {
    let maxAttempts = 3
    let attempt = 0
    
    while (attempt < maxAttempts) {
        player.moveTo(x, y, z)
        
        let timeout = 0
        while (player.isMoveActive() && timeout < 60) {
            System.wait(1000)
            timeout = timeout + 1
        }
        
        if (!player.isMoveActive()) {
            System.print("Arrived successfully!")
            return true
        }
        
        System.print("Timeout, retrying...")
        player.stopMove()
        attempt = attempt + 1
        System.wait(2000)
    }
    
    System.print("Failed to reach destination")
    return false
}
```

## Troubleshooting

### Path Not Found

**Symptoms**: Player doesn't move, or pathfinding times out.

**Solutions**:
1. Target may be too far (>2000 iterations needed)
2. No valid path exists (blocked by walls, lava)
3. Increase max iterations (requires modifying PathfindingService)

### Player Stuck

**Symptoms**: Player stops moving mid-path.

**Solutions**:
```javascript
// Force stop and retry
player.stopMove()
System.wait(1000)
player.moveTo(x, y, z)
```

### Slow Performance

**Symptoms**: Lag when calculating paths.

**Solutions**:
1. Reduce navigation distance
2. Use radius parameter for faster completion
3. Avoid frequent recalculations

## API Reference

### player.moveTo(x, y, z, [radius])

Navigate to coordinates using pathfinding.

**Parameters:**
- `x` (number): Target X coordinate
- `y` (number): Target Y coordinate
- `z` (number): Target Z coordinate  
- `radius` (number, optional): Acceptable distance from target (default: 1.0)

**Returns:** `true` if pathfinding started, `false` otherwise

**Example:**
```javascript
player.moveTo(100, 64, 200)      // Navigate to exact coords
player.moveTo(100, 64, 200, 2.0) // Accept 2-block radius
```

### player.stopMove()

Stop current pathfinding/movement.

**Returns:** `true`

**Example:**
```javascript
player.stopMove()
```

### player.isMoveActive()

Check if pathfinding is currently active.

**Returns:** `true` if navigating, `false` if idle

**Example:**
```javascript
if (player.isMoveActive()) {
    System.print("Moving...")
}
```

## See Also

- [Player Object API](api/player-object.md)
- [KHScript Syntax](khscript-syntax.md)
- [Example Scripts](examples/README.md)

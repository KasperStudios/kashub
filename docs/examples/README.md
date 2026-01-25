# Example Scripts

Collection of ready-to-use Kashub scripts for common tasks.

## Getting Started Examples

### Hello World
```javascript
System.print("Hello, Kashub!")
```

### Player Info
```javascript
let health = player.getHealth()
let hunger = player.getHunger()
let name = player.getName()
let pos = player.getPos()

System.print("=== Player Info ===")
System.print("Name: " + name)
System.print("Health: " + health + "/20")
System.print("Hunger: " + hunger + "/20")
System.print("Position: " + pos.x + ", " + pos.y + ", " + pos.z)
```

## Automation Examples

### Auto-Heal
```javascript
System.print("Auto-heal starting...")

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

### Auto-Eat
```javascript
System.print("Auto-eat starting...")

while (true) {
    let hunger = player.getHunger()
    
    if (hunger < 14) {
        System.print("Hungry! Eating...")
        inventory.use("cooked_beef")
        System.wait(2000)
    }
    
    System.wait(1000)
}
```

### Health & Hunger Monitor
```javascript
System.print("Health monitor starting...")

while (true) {
    let health = player.getHealth()
    let hunger = player.getHunger()
    
    // Heal if low health
    if (health < 10) {
        System.print("Low health! Using golden apple...")
        inventory.use("golden_apple")
        System.wait(5000)
    }
    
    // Eat if hungry
    if (hunger < 14) {
        System.print("Hungry! Eating food...")
        inventory.use("cooked_beef")
        System.wait(2000)
    }
    
    System.wait(1000)
}
```

## Combat Examples

### Basic Combat Bot
```javascript
System.print("Combat bot starting...")

while (true) {
    let enemy = vision.nearest("hostile", 4)
    
    if (enemy != null) {
        player.lookAt(enemy.x, enemy.y, enemy.z)
        player.attack(enemy)
        System.print("Attacking " + enemy.type)
    }
    
    System.wait(100)
}
```

### Safe Combat Bot
```javascript
System.print("Safe combat bot starting...")

crashguard(minFps=30) {
    while (true) {
        // Safety check
        let health = player.getHealth()
        if (health < 8) {
            System.print("Low health! Retreating...")
            player.chat("/spawn")
            break
        }
        
        // Find and attack enemy
        let enemy = vision.nearest("hostile", 4, "head")
        
        if (enemy != null) {
            player.lookAt(enemy.x, enemy.y, enemy.z)
            player.attack(enemy)
            System.print("Attacking " + enemy.type + " (HP: " + enemy.health + ")")
        }
        
        System.wait(100)
    }
}
```

### Zombie Killer
```javascript
System.print("Zombie killer starting...")

let killCount = 0

while (true) {
    let zombie = vision.nearest("zombie", 5)
    
    if (zombie != null) {
        player.lookAt(zombie.x, zombie.y, zombie.z)
        player.attack(zombie)
        
        if (zombie.health <= 0) {
            killCount = killCount + 1
            System.print("Killed zombie! Total: " + killCount)
        }
    }
    
    System.wait(100)
}
```

## Mining Examples

### Diamond Finder
```javascript
System.print("Diamond finder starting...")

let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    System.print("Found " + ores.length() + " diamond ore blocks!")
    
    let nearest = ores[0]
    System.print("Nearest at: " + nearest.x + ", " + nearest.y + ", " + nearest.z)
    System.print("Distance: " + nearest.dist + " blocks")
} else {
    System.print("No diamonds found nearby")
}
```

### Auto-Miner
```javascript
System.print("Auto-miner starting...")

let targetOres = "diamond_ore,emerald_ore,gold_ore,iron_ore"
let mined = 0

while (mined < 20) {
    // Find nearest ore
    let ores = scanner.blocks(targetOres, 32)
    
    if (ores.length() == 0) {
        System.print("No ores found")
        break
    }
    
    let ore = ores[0]
    System.print("Found " + ore.id + " at " + ore.x + ", " + ore.y + ", " + ore.z)
    
    // Navigate to ore
    player.moveTo(ore.x, ore.y, ore.z)
    System.wait(1000)
    
    // Mine ore
    player.lookAt(ore.x, ore.y, ore.z)
    System.wait(200)
    player.breakBlock()
    System.wait(2000)
    
    mined = mined + 1
    System.print("Mined " + mined + " ores")
}

System.print("Mining complete!")
```

### Safe Mining
```javascript
System.print("Safe mining starting...")

crashguard(timeout=30000, minFps=25) {
    let ores = scanner.blocks("diamond_ore", 64)
    
    if (ores.count > 0) {
        System.print("Found " + ores.count + " ores")
        
        let i = 0
        while (i < ores.count) {
            let ore = ores.blocks[i]
            
            // Check safety
            let health = player.getHealth()
            if (health < 10) {
                System.print("Low health! Stopping.")
                break
            }
            
            // Mine ore
            player.moveTo(ore.x, ore.y, ore.z)
            System.wait(500)
            player.lookAt(ore.x, ore.y, ore.z)
            player.breakBlock()
            System.wait(2000)
            
            i = i + 1
        }
    }
    
    System.print("Mining complete!")
}
```

## Farming Examples

### Crop Harvester
```javascript
System.print("Crop harvester starting...")

let crops = scanner.blocks("wheat", 16)

if (crops.length() > 0) {
    System.print("Found " + crops.length() + " wheat crops")
    
    let i = 0
    while (i < crops.length()) {
        let crop = crops[i]
        
        // Move to crop
        player.moveTo(crop.x, crop.y, crop.z)
        System.wait(500)
        
        // Harvest
        player.lookAt(crop.x, crop.y, crop.z)
        player.breakBlock()
        System.wait(200)
        
        // Replant
        player.placeBlock("wheat_seeds")
        System.wait(200)
        
        i = i + 1
    }
    
    System.print("Harvesting complete!")
}
```

### Tree Chopper
```javascript
System.print("Tree chopper starting...")

let logs = scanner.blocks("oak_log", 16)

if (logs.length() > 0) {
    System.print("Found " + logs.length() + " logs")
    
    let i = 0
    while (i < logs.length()) {
        let log = logs[i]
        
        player.moveTo(log.x, log.y, log.z)
        System.wait(500)
        
        player.lookAt(log.x, log.y, log.z)
        player.breakBlock()
        System.wait(1000)
        
        i = i + 1
    }
    
    System.print("Tree chopping complete!")
}
```

## Utility Examples

### Position Logger
```javascript
System.print("Position logger starting...")

while (true) {
    let pos = player.getPos()
    System.print("Position: " + pos.x + ", " + pos.y + ", " + pos.z)
    System.wait(5000)
}
```

### Inventory Counter
```javascript
System.print("=== Inventory Count ===")

let diamonds = inventory.count("diamond")
let emeralds = inventory.count("emerald")
let gold = inventory.count("gold_ingot")
let iron = inventory.count("iron_ingot")

System.print("Diamonds: " + diamonds)
System.print("Emeralds: " + emeralds)
System.print("Gold: " + gold)
System.print("Iron: " + iron)
```

### Entity Counter
```javascript
System.print("=== Entity Count ===")

let zombies = vision.count("zombie", 50)
let skeletons = vision.count("skeleton", 50)
let creepers = vision.count("creeper", 50)
let hostiles = vision.count("hostile", 50)

System.print("Zombies: " + zombies)
System.print("Skeletons: " + skeletons)
System.print("Creepers: " + creepers)
System.print("Total hostiles: " + hostiles)
```

### Memory Monitor
```javascript
System.print("Memory monitor starting...")

while (true) {
    let mem = System.memory()
    
    System.print("=== Memory Usage ===")
    System.print("Used: " + mem.used + " MB")
    System.print("Free: " + mem.free + " MB")
    System.print("Total: " + mem.total + " MB")
    System.print("Max: " + mem.max + " MB")
    
    let usage = (mem.used / mem.max) * 100
    System.print("Usage: " + usage + "%")
    
    if (usage > 80) {
        System.print("High memory usage! Running GC...")
        System.gc()
    }
    
    System.wait(10000)
}
```

## Advanced Examples

### Pathfinding Demo
```javascript
System.print("Pathfinding demo starting...")

// Define waypoints
let waypoints = [
    {x: 100, y: 64, z: 200},
    {x: 150, y: 64, z: 250},
    {x: 200, y: 64, z: 200},
    {x: 150, y: 64, z: 150}
]

let i = 0
while (i < waypoints.length()) {
    let wp = waypoints[i]
    
    System.print("Moving to waypoint " + (i + 1))
    
    // player.moveTo() handles obstacle avoidance automatically
    player.moveTo(wp.x, wp.y, wp.z)
    
    // Wait until movement finishes
    while (player.isMoveActive()) {
        System.wait(100)
    }
    
    System.print("Arrived at " + (i + 1))
    i = i + 1
}

System.print("Pathfinding complete!")
```

### Mob Farm Bot
```javascript
System.print("Mob farm bot starting...")

crashguard(minFps=30) {
    let killCount = 0
    
    while (true) {
        // Check health
        let health = player.getHealth()
        if (health < 10) {
            inventory.use("golden_apple")
            System.wait(5000)
        }
        
        // Find and kill mobs
        let mob = vision.nearest("hostile", 4)
        
        if (mob != null) {
            player.lookAt(mob.x, mob.y, mob.z)
            player.attack(mob)
            
            if (mob.health <= 0) {
                killCount = killCount + 1
                System.print("Kills: " + killCount)
            }
        }
        
        // Check inventory
        let invCheck = inventory.check()
        if (invCheck.emptySlots < 5) {
            System.print("Inventory almost full!")
        }
        
        System.wait(100)
    }
}
```

### Resource Collector
```javascript
System.print("Resource collector starting...")

fn collectResource(resourceName, radius) {
    let items = scanner.blocks(resourceName, radius)
    
    if (items.length() > 0) {
        System.print("Found " + items.length() + " " + resourceName)
        
        let i = 0
        while (i < items.length() && i < 10) {
            let item = items[i]
            
            player.moveTo(item.x, item.y, item.z)
            System.wait(500)
            
            player.lookAt(item.x, item.y, item.z)
            player.breakBlock()
            System.wait(1000)
            
            i = i + 1
        }
    }
}

// Collect different resources
collectResource("diamond_ore", 32)
collectResource("emerald_ore", 32)
collectResource("gold_ore", 32)

System.print("Collection complete!")
```

## Tips for Writing Scripts

### 1. Always add delays
```javascript
while (true) {
    // Your code
    System.wait(100)  // Always add this!
}
```

### 2. Check for null
```javascript
let enemy = vision.nearest("zombie", 10)
if (enemy != null) {
    player.attack(enemy)
}
```

### 3. Use CrashGuard for safety
```javascript
crashguard(timeout=10000, minFps=30) {
    // Your risky code here
}
```

### 4. Monitor health
```javascript
let health = player.getHealth()
if (health < 10) {
    // Heal or escape
}
```

### 5. Check inventory space
```javascript
let invCheck = inventory.check()
if (invCheck.emptySlots < 5) {
    System.print("Inventory almost full!")
}
```

## See Also

- [Quick Start Guide](../quick-start.md)
- [KHScript Syntax](../khscript-syntax.md)
- [API Reference](../api/system-object.md)
- [Best Practices](best-practices.md)

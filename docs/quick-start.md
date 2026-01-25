# Quick Start Guide

Get started with Kashub in just 5 minutes! This guide will teach you the basics of writing and running scripts.

## Your First Script

Let's write a simple script that displays your player information.

### Step 1: Open the Editor

Press `K` to open the Kashub editor.

### Step 2: Create a New Script

1. Click the "New Script" button (or press `Ctrl+N`)
2. Name your script `my_first_script.kh`
3. The editor will open with an empty file

### Step 3: Write Your Code

Copy and paste this code into the editor:

```javascript
// My First Kashub Script
System.print("Hello, Kashub!")

// Get player information
let health = player.getHealth()
let hunger = player.getHunger()
let name = player.getName()
let pos = player.getPos()

// Display information
System.print("Player: " + name)
System.print("Health: " + health + "/20")
System.print("Hunger: " + hunger + "/20")
System.print("Position: X=" + pos.x + " Y=" + pos.y + " Z=" + pos.z)

System.print("Script complete!")
```

### Step 4: Run Your Script

Press `F5` or click the "Run" button. You should see output in your chat:

```
[KH] Hello, Kashub!
[KH] Player: YourName
[KH] Health: 20/20
[KH] Hunger: 20/20
[KH] Position: X=100 Y=64 Z=200
[KH] Script complete!
```

Congratulations! You've just run your first Kashub script! 🎉

## Understanding the Code

Let's break down what each part does:

### Comments

```javascript
// This is a comment - it's ignored by the script
```

Comments help you document your code. Use `//` for single-line comments.

### System.print()

```javascript
System.print("Hello, Kashub!")
```

`System.print()` displays messages in chat and the console. It's the main way to output information.

### Variables

```javascript
let health = player.getHealth()
```

Variables store values. Use `let` to declare a new variable. Variable names can contain letters, numbers, and underscores.

### Objects and Methods

```javascript
player.getHealth()
```

Kashub uses object-oriented syntax:
- `player` is an object representing your character
- `getHealth()` is a method that returns your health value
- Methods are called with parentheses `()`

### String Concatenation

```javascript
System.print("Health: " + health + "/20")
```

Use `+` to combine strings and values into a single message.

## Example 2: Auto-Heal Script

Let's create something more useful - a script that automatically heals you when health is low:

```javascript
System.print("Auto-Heal script starting...")

// Check health every second
while (true) {
    let health = player.getHealth()
    
    if (health < 10) {
        System.print("Low health detected! Healing...")
        
        // Try to use a golden apple
        inventory.use("golden_apple")
        
        // Wait 5 seconds before checking again
        System.wait(5000)
    }
    
    // Check again in 1 second
    System.wait(1000)
}
```

This script:
1. Runs forever (`while (true)`)
2. Checks your health every second
3. If health drops below 10, uses a golden apple
4. Waits 5 seconds after healing before checking again

### Running Background Scripts

To run this script in the background:
1. Press `F5` to start it
2. Press `Esc` to close the editor
3. The script continues running
4. Press `Z` to stop all scripts

## Example 3: Finding Diamonds

Let's use the scanner to find nearby diamond ore:

```javascript
System.print("Diamond scanner starting...")

// Scan for diamond ore within 32 blocks
let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    System.print("Found " + ores.length() + " diamond ore blocks!")
    
    // Get the nearest one
    let nearest = ores[0]
    
    System.print("Nearest diamond at:")
    System.print("  X: " + nearest.x)
    System.print("  Y: " + nearest.y)
    System.print("  Z: " + nearest.z)
    System.print("  Distance: " + nearest.dist + " blocks")
} else {
    System.print("No diamond ore found nearby")
}
```

This script:
1. Scans for diamond ore within 32 blocks
2. Displays how many were found
3. Shows the coordinates of the nearest one
4. Shows the distance to it

## Key Concepts

### 1. Objects

Kashub provides several built-in objects:

- `System` - Utilities and I/O
- `player` - Your character
- `scanner` - Find blocks and entities
- `vision` - Targeting and vision
- `inventory` - Manage items
- `world` - World information
- `game` - Game settings

### 2. Methods

Methods are functions attached to objects:

```javascript
player.getHealth()    // Get health
player.moveTo(x, y, z)  // Move to coordinates
inventory.count("diamond")  // Count diamonds
```

### 3. Variables

Store values for later use:

```javascript
let health = player.getHealth()
let pos = player.getPos()
let diamonds = inventory.count("diamond")
```

### 4. Conditionals

Make decisions in your code:

```javascript
if (health < 10) {
    System.print("Low health!")
} else {
    System.print("Health is good")
}
```

### 5. Loops

Repeat actions:

```javascript
// Repeat 5 times
let i = 0
while (i < 5) {
    System.print("Count: " + i)
    i = i + 1
}
```

### 6. Waiting

Add delays to your scripts:

```javascript
System.wait(1000)  // Wait 1 second (1000 milliseconds)
System.wait(500)   // Wait 0.5 seconds
```

## Editor Shortcuts

- `F5` - Run script
- `Ctrl+S` - Save script
- `Ctrl+N` - New script
- `Ctrl+Space` - Auto-complete
- `Ctrl+F` - Find
- `F9` - Toggle breakpoint
- `F10` - Step over (debug)
- `F11` - Step into (debug)

## Common Patterns

### Check before action

```javascript
let health = player.getHealth()
if (health < 10) {
    inventory.use("golden_apple")
}
```

### Loop with delay

```javascript
while (true) {
    // Do something
    System.print("Running...")
    
    // Wait before next iteration
    System.wait(1000)
}
```

### Find and use item

```javascript
let diamonds = inventory.count("diamond")
if (diamonds > 0) {
    System.print("You have " + diamonds + " diamonds!")
}
```

### Scan and navigate

```javascript
let ores = scanner.blocks("diamond_ore", 32)
if (ores.length() > 0) {
    let nearest = ores[0]
    player.moveTo(nearest.x, nearest.y, nearest.z)
}
```

## Safety Tips

### 1. Always add delays in loops

```javascript
// BAD - Will freeze the game!
while (true) {
    player.attack()
}

// GOOD - Adds delay
while (true) {
    player.attack()
    System.wait(100)
}
```

### 2. Use CrashGuard for risky code

```javascript
crashguard(timeout=10000, minFps=30) {
    // Your code here
    while (true) {
        player.attack()
        System.wait(100)
    }
}
```

### 3. Check for null values

```javascript
let enemy = vision.nearest("zombie", 10)
if (enemy != null) {
    player.attack(enemy)
}
```

### 4. Limit loop iterations

```javascript
let i = 0
let maxIterations = 100
while (i < maxIterations) {
    // Do something
    i = i + 1
}
```

## Next Steps

Now that you know the basics:

1. **Learn the full syntax** - Read [KHScript Syntax](khscript-syntax.md)
2. **Explore the API** - Check out [API Reference](api/system-object.md)
3. **Study examples** - Browse [Example Scripts](examples/README.md)
4. **Master the editor** - Read [Editor Guide](editor-guide.md)

## Getting Help

- **In-game help** - Type `/kashub help` in chat
- **Discord** - Join our [community](https://discord.gg/gFeWtpEKN9)
- **GitHub** - Report issues or ask questions

Happy scripting! 🚀

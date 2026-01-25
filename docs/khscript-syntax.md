# KHScript Syntax Reference

Complete reference for the KHScript language used in Kashub.

## Overview

KHScript is a modern scripting language with syntax inspired by JavaScript and Lua. It features:
- Object-oriented design
- Dynamic typing
- First-class functions
- Familiar control flow structures

## Comments

```javascript
// Single-line comment

// Multi-line comments are not supported yet
// Use multiple single-line comments instead
```

## Variables

### Declaration

Use `let` to declare variables:

```javascript
let health = 20
let name = "Steve"
let isAlive = true
let pos = player.getPos()
```

### Assignment

```javascript
let x = 10
x = 20  // Reassign value
x = x + 5  // Update value
```

### Naming Rules

- Must start with a letter or underscore
- Can contain letters, numbers, and underscores
- Case-sensitive (`health` ≠ `Health`)
- Cannot use reserved keywords

**Valid names:**
```javascript
let health = 20
let player_name = "Steve"
let _temp = 5
let value123 = 100
```

**Invalid names:**
```javascript
let 123value = 100  // Cannot start with number
let player-name = "Steve"  // Cannot use hyphens
let if = 5  // Cannot use keywords
```

## Data Types

### Numbers

```javascript
let integer = 42
let decimal = 3.14
let negative = -10
```

### Strings

```javascript
let name = "Steve"
let message = 'Hello'
let empty = ""
```

### Booleans

```javascript
let isAlive = true
let isDead = false
```

### Null

```javascript
let nothing = null
```

### Objects

```javascript
let pos = player.getPos()  // {x: 100, y: 64, z: 200}
let enemy = vision.nearest("zombie", 10)  // {type: "zombie", x: 105, ...}
```

### Arrays

```javascript
let ores = scanner.blocks("diamond_ore", 32)  // [{x: 100, y: 12, z: 200}, ...]
let items = inventory.getItems()  // [{slot: 0, id: "diamond", count: 5}, ...]
```

## Operators

### Arithmetic

```javascript
let sum = 5 + 3  // 8
let diff = 10 - 4  // 6
let product = 6 * 7  // 42
let quotient = 20 / 4  // 5
let remainder = 10 % 3  // 1
```

### Comparison

```javascript
let isEqual = (5 == 5)  // true
let isNotEqual = (5 != 3)  // true
let isGreater = (10 > 5)  // true
let isLess = (3 < 7)  // true
let isGreaterOrEqual = (5 >= 5)  // true
let isLessOrEqual = (3 <= 5)  // true
```

### Logical

```javascript
let and = (true && false)  // false
let or = (true || false)  // true
let not = !true  // false
```

### String Concatenation

```javascript
let greeting = "Hello, " + "World!"  // "Hello, World!"
let message = "Health: " + 20  // "Health: 20"
```

## Control Flow

### If Statement

```javascript
if (health < 10) {
    System.print("Low health!")
}
```

### If-Else

```javascript
if (health < 10) {
    System.print("Low health!")
} else {
    System.print("Health is good")
}
```

### If-Else If-Else

```javascript
if (health < 5) {
    System.print("Critical!")
} else if (health < 10) {
    System.print("Low health")
} else if (health < 15) {
    System.print("Moderate health")
} else {
    System.print("Full health")
}
```

### While Loop

```javascript
let i = 0
while (i < 5) {
    System.print("Count: " + i)
    i = i + 1
}
```

### Infinite Loop

```javascript
while (true) {
    System.print("Running...")
    System.wait(1000)
}
```

### Break

```javascript
let i = 0
while (true) {
    if (i >= 10) {
        break  // Exit loop
    }
    System.print(i)
    i = i + 1
}
```

### Continue

```javascript
let i = 0
while (i < 10) {
    i = i + 1
    
    if (i % 2 == 0) {
        continue  // Skip even numbers
    }
    
    System.print(i)  // Only prints odd numbers
}
```

## Functions

### Declaration

```javascript
fn greet(name) {
    System.print("Hello, " + name + "!")
}

greet("Steve")  // Output: Hello, Steve!
```

### Parameters

```javascript
fn add(a, b) {
    return a + b
}

let sum = add(5, 3)  // 8
```

### Return Values

```javascript
fn getHealth() {
    return player.getHealth()
}

let health = getHealth()
System.print("Health: " + health)
```

### Multiple Parameters

```javascript
fn moveTo(x, y, z) {
    player.moveTo(x, y, z)
    System.print("Moving to " + x + ", " + y + ", " + z)
}

moveTo(100, 64, 200)
```

### No Parameters

```javascript
fn checkHealth() {
    let health = player.getHealth()
    if (health < 10) {
        System.print("Low health!")
    }
}

checkHealth()
```

## Objects and Methods

### Accessing Properties

```javascript
let pos = player.getPos()
System.print("X: " + pos.x)
System.print("Y: " + pos.y)
System.print("Z: " + pos.z)
```

### Calling Methods

```javascript
player.getHealth()  // Call method
player.moveTo(100, 64, 200)  // Call with parameters
```

### Method Chaining

```javascript
// Not directly supported, but you can do:
let pos = player.getPos()
player.moveTo(pos.x + 10, pos.y, pos.z)
```

## Arrays

### Accessing Elements

```javascript
let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    let first = ores[0]  // First element
    let second = ores[1]  // Second element
    
    System.print("First ore at: " + first.x + ", " + first.y + ", " + first.z)
}
```

### Array Length

```javascript
let ores = scanner.blocks("diamond_ore", 32)
System.print("Found " + ores.length() + " ores")
```

### Iterating Arrays

```javascript
let items = inventory.getItems()
let i = 0

while (i < items.length()) {
    let item = items[i]
    System.print("Slot " + item.slot + ": " + item.id)
    i = i + 1
}
```

## Error Handling

### CrashGuard

```javascript
crashguard {
    // Protected code
    while (true) {
        player.attack()
        System.wait(100)
    }
}
```

### With Options

```javascript
crashguard(timeout=10000, minFps=30) {
    // Code with timeout and FPS monitoring
    let ores = scanner.blocks("diamond_ore", 64)
    // Process ores...
}
```

## Special Constructs

### Return Statement

```javascript
fn checkSafety() {
    let health = player.getHealth()
    
    if (health < 5) {
        System.print("Unsafe!")
        return false
    }
    
    return true
}

if (checkSafety()) {
    // Continue with main logic
}
```

### Early Exit

```javascript
let health = player.getHealth()

if (health < 5) {
    System.print("Critical health! Exiting.")
    System.exit()
}

System.print("Continuing...")
```

## Best Practices

### 1. Use meaningful variable names

```javascript
// Good
let playerHealth = player.getHealth()
let nearestEnemy = vision.nearest("zombie", 10)

// Bad
let h = player.getHealth()
let e = vision.nearest("zombie", 10)
```

### 2. Add comments

```javascript
// Check if player needs healing
let health = player.getHealth()
if (health < 10) {
    // Use golden apple for healing
    inventory.use("golden_apple")
}
```

### 3. Use functions for reusable code

```javascript
fn heal() {
    let health = player.getHealth()
    if (health < 10) {
        inventory.use("golden_apple")
        System.wait(5000)
    }
}

// Use the function multiple times
heal()
// ... do other stuff ...
heal()
```

### 4. Always add delays in loops

```javascript
// Good
while (true) {
    player.attack()
    System.wait(100)
}

// Bad - will freeze!
while (true) {
    player.attack()
}
```

### 5. Check for null values

```javascript
let enemy = vision.nearest("zombie", 10)

if (enemy != null) {
    player.attack(enemy)
} else {
    System.print("No enemies found")
}
```

### 6. Use constants for magic numbers

```javascript
let HEAL_THRESHOLD = 10
let SCAN_RADIUS = 32
let ATTACK_RANGE = 4

if (player.getHealth() < HEAL_THRESHOLD) {
    inventory.use("golden_apple")
}
```

## Common Patterns

### Health Monitor

```javascript
while (true) {
    let health = player.getHealth()
    
    if (health < 10) {
        inventory.use("golden_apple")
        System.wait(5000)
    }
    
    System.wait(1000)
}
```

### Resource Scanner

```javascript
let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    System.print("Found " + ores.length() + " diamonds!")
    
    let nearest = ores[0]
    player.moveTo(nearest.x, nearest.y, nearest.z)
}
```

### Combat Loop

```javascript
while (true) {
    let enemy = vision.nearest("hostile", 4)
    
    if (enemy != null) {
        player.lookAt(enemy.x, enemy.y, enemy.z)
        player.attack(enemy)
    }
    
    System.wait(100)
}
```

### Inventory Check

```javascript
let diamonds = inventory.count("diamond")

if (diamonds > 0) {
    System.print("You have " + diamonds + " diamonds!")
} else {
    System.print("No diamonds found")
}
```

## Reserved Keywords

These words cannot be used as variable names:

- `let`
- `fn`
- `if`
- `else`
- `while`
- `for`
- `break`
- `continue`
- `return`
- `true`
- `false`
- `null`
- `crashguard`

## Limitations

### No For Loops (Yet)

Use while loops instead:

```javascript
// Instead of: for (let i = 0; i < 10; i++)
let i = 0
while (i < 10) {
    System.print(i)
    i = i + 1
}
```

### No Multi-line Comments

Use multiple single-line comments:

```javascript
// This is a comment
// This is another comment
// And another one
```

### No String Interpolation

Use concatenation:

```javascript
// Instead of: `Health: ${health}`
let message = "Health: " + health
```

### No Array Literals

Arrays are returned by API methods:

```javascript
// You can't create: let arr = [1, 2, 3]
// But you can use: let ores = scanner.blocks("diamond_ore", 32)
```

## See Also

- [Quick Start Guide](quick-start.md)
- [API Reference](api/system-object.md)
- [Example Scripts](examples/README.md)

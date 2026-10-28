# KHScript Syntax Reference

Complete reference for the KHScript language used in Kashub v1.0.0-FSR.

## Overview

KHScript is a modern scripting language with syntax inspired by JavaScript and Lua. It features:
- Object-oriented design
- Dynamic typing
- First-class functions
- Familiar control flow structures
- Integrated **CrashGuard** safety system

## Comments

```javascript
// Single-line comment

/* 
   Multi-line comments are now supported 
   for documentation blocks.
*/
```

## Variables

### Declaration

Use `let` to declare variables:

```javascript
let health = 20
let name = "Steve"
let isAlive = true
let pos = player.pos()
```

## Data Types

### Basic Types
- **Numbers**: `42`, `3.14`, `-10`
- **Strings**: `"Hello"`, `'World'`
- **Booleans**: `true`, `false`
- **Null**: `null`

### Collections
- **Arrays (Lists)**: `[1, 2, "three"]`
- **Objects (Maps)**: `{key: "value", x: 10}`

### Reflection
Use the `typeof` operator to check the type of a value:
```javascript
let x = 10;
System.print(typeof x); // "number"
```

## Operators

### Arithmetic
`+`, `-`, `*`, `/`, `%` (modulo)

### Comparison
`==`, `!=`, `>`, `<`, `>=`, `<=`

### Logical
`&&` (AND), `||` (OR), `!` (NOT)

### Ternary
`condition ? trueValue : falseValue`

## Control Flow

### If / Else
```javascript
if (player.isCreative()) {
    System.print("Infinite power!");
} else {
    System.print("Survival mode.");
}
```

### While Loop
```javascript
let i = 0;
while (i < 5) {
    System.print(i);
    i = i + 1;
}
```

### Loops: Break & Continue
Standard `break` and `continue` keywords are supported within `while` loops.

## Functions

```javascript
fn greet(name) {
    return "Hello, " + name;
}

let msg = greet("Explorer");
System.print(msg);
```

## 🛡️ CrashGuard

The `crashguard` block protects your script from common runtime issues like lag, infinite loops, and API errors.

```javascript
crashguard(timeout=5000, minFps=20) {
    // This code will stop if it takes >5s or FPS drops below 20
    while(true) {
        player.attack();
        System.wait(100);
    }
}
```

## ⚡ Global Commands

The following commands are available globally without an object prefix:

- `print(msg)` : Send to console.
- `wait(ms)` : Pause execution.
- `chat(msg)` : Send to Minecraft chat.
- `time()` : Get system time in ms.

---

## Limitations (v1.0.0)

- **For Loops**: Use `while` loops instead.
- **Async Blocks**: Currently managed via the Terminal's background execution (`run script &`).
- **Classes**: KHScript is prototype-based; full class declarations are planned for v1.1.0.

# 🚀 Quick Start Guide

Get started with Kashub in just 5 minutes! This guide will teach you how to set up your hardware and write your first script.

## 🛠️ Step 0: The Neural Implant

Kashub is powered by a physical **Neural Implant**. Without it, the IDE and scripting engine will remain offline.

1. **Obtain an Implant**: In Creative mode, search for "Neural Implant". In Survival, craft a Standard Neural Implant.
2. **Equip it**: Open your inventory (`E`) and place the implant into the dedicated slot with the brain icon.
3. **Boot Up**: You will hear a startup sound, and the Kashub OS will begin initializing.

## 📝 Your First Script

Let's write a simple script that displays your player information.

### Step 1: Open the Editor
Press `K` to open the Kashub IDE.

### Step 2: Create a New Script
1. Click the **New Script** button (or press `Ctrl+N`).
2. Name your script `hello_world.kh`.

### Step 3: Write Your Code
Copy and paste this code into the editor:

```javascript
// My First Kashub Script
System.print("Initializing Neural Link...");

// Get player information
let pos = player.pos();
let health = player.isCreative() ? "Infinite" : 20;

// Display information
System.print("Position: " + pos.x + ", " + pos.y + ", " + pos.z);
System.print("Mode: " + (player.isCreative() ? "Creative" : "Survival"));

System.print("Neural Link Stable! 🎉");
```

### Step 4: Run Your Script
Press **F5** or click the **Run** button. You should see output in the console panel at the bottom.

## 🤖 Example: Auto-Defense

Here is a more advanced script that automatically attacks hostile mobs near you.

```javascript
System.print("Defense Matrix: ONLINE");

while (true) {
    // Find nearest hostile entity within 10 blocks
    let enemy = vision.nearest("hostile", 10);
    
    if (enemy != null) {
        System.print("Targeting: " + enemy.id);
        player.lookAt(enemy.pos.x, enemy.pos.y, enemy.pos.z);
        player.attack(enemy);
    }
    
    // Always add a delay in loops!
    System.wait(200);
}
```

## 🧠 Key Concepts

1. **Objects**: Access systems like `player`, `inventory`, `scanner`, and `Terminal`.
2. **Methods**: Perform actions using parentheses, e.g., `player.jump()`.
3. **Loops**: Use `while(true)` for continuous automation.
4. **Safety**: Use `System.wait(ms)` in every loop to prevent game lag.

---

## Next Steps
- **Full Syntax**: Read the [KHScript Syntax Reference](khscript-syntax.md).
- **Deep Scanning**: Learn about [Neural Implant & Scanning](neural-implant.md).
- **CC:T Bridge**: Connect to computers in the [CC:T Integration](cct-integration.md) guide.

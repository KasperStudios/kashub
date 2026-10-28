# 🔗 ComputerCraft: Tweaked Integration

Kashub v1.0.0-FSR features deep integration with the **ComputerCraft: Tweaked (CC:T)** mod. This allows you to use the Kashub IDE as a professional development environment for your CC:T computers, turtles, and peripherals.

## 📡 Establishing a Neural Link

To connect to a CC:T computer:
1. **Deep Scan** the computer block (Hold `Left Alt`).
2. Open the Kashub IDE.
3. Click the **Neural Link** button in the top-right toolbar.
4. Select the computer from the list of discovered devices.

Once linked, the icon will turn green, indicating a bidirectional connection is active.

## 💻 Remote Scripting

When a Neural Link is active, the Kashub IDE behaves as a remote frontend for the CC:T computer:

- **File Synchronization**: The `FilePanel` on the left will show the files stored on the CC:T computer under the `/remote/` or `/mnt/cc/` path.
- **Remote Editing**: Open, edit, and save `.lua` files directly on the CC:T computer.
- **Terminal Proxying**: The IDE Terminal sends commands directly to the CC:T Lua shell.
- **One-Click Run**: Press `F5` to execute the current Lua script on the linked computer.

## 📦 The `cc` Scripting Object

You can automate the integration itself using KHScript. This is useful for building "Hive Mind" networks of turtles or monitoring remote systems.

### `cc.scan()`
Returns a list of all reachable CC:T computers in the vicinity.

### `cc.connect(id, x, y, z)`
Connects the IDE to a specific computer.

### `cc.disconnect(id)`
Terminates the link.

### Example: Auto-Connecting to a Turtle
```javascript
let bots = cc.scan();
for (let bot of bots) {
    if (bot.label.contains("Miner")) {
        cc.connect(bot.id, bot.x, bot.y, bot.z);
        System.print("Linked to Miner #" + bot.id);
        break;
    }
}
```

## 💾 File Pushing

You can easily move files between your local Kashub storage and the CC:T computer. 
- Right-click a local file in the `FilePanel` and select **Push to CC:T**.
- This will automatically upload the script to the root directory of the connected computer.

---

> [!IMPORTANT]
> The CC:T bridge requires a **Tier 2 (Elite)** or **Tier 3 (Creative)** Neural Implant. Standard implants do not have the processing power to maintain a remote Neural Link.

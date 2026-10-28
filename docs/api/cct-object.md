# 🔗 CC Object

The `cc` object (lowercase) manages the integration with **ComputerCraft: Tweaked**, including discovery and Neural Link connections.

## Methods

### `cc.scan()`
Scans the surrounding area for reachable CC:T computers and turtles.
- **Returns:** List of `{id: number, label: string, x: number, y: number, z: number}`

### `cc.connect(id, x, y, z)`
Attempts to establish a Neural Link with a specific computer.
- **Parameters:** `id`, `x`, `y`, `z` (numbers)
- **Returns:** `boolean`

### `cc.disconnect(id)`
Terminates an active Neural Link with the specified computer.
- **Parameters:** `id` (number)
- **Returns:** `null`

---

## Example: Fleet Manager
```javascript
let bots = cc.scan();
System.print("Found " + bots.length() + " bots.");

for (let bot of bots) {
    if (bot.label == "Worker_1") {
        cc.connect(bot.id, bot.pos.x, bot.pos.y, bot.pos.z);
        System.print("Connected to Worker!");
    }
}
```

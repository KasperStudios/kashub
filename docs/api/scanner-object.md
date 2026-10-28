# 📡 Scanner Object

The `scanner` object allows for bulk detection of blocks and entities in a radius around the player. It is powered by the **Neural Implant**.

## Methods

### `scanner.blocks(id, radius, [limit])`
Searches for blocks matching the ID within the specified radius.
- **Parameters:** `id` (string), `radius` (number), `limit` (optional int).
- **Returns:** List of block data objects `{id, x, y, z}`.

### `scanner.entities(id, radius, [limit])`
Searches for entities matching the ID within the specified radius.
- **Parameters:** `id` (string), `radius` (number), `limit` (optional int).
- **Returns:** List of entity data objects.

### `scanner.peripherals(radius)`
Finds all blocks within range that support advanced interaction (e.g., inventories, machines, CC:T computers).
- **Returns:** List of peripheral objects.

### `scanner.deepScan()`
Performs a deep scan of the block or entity currently in crosshair, adding it to the **Compendium**.
- **Returns:** `boolean` (true if a new entry was discovered).

---

## Example: Diamond Hunter
```javascript
let ores = scanner.blocks("diamond_ore", 16);
if (ores.length() > 0) {
    System.print("Found " + ores.length() + " diamond ores nearby!");
    player.moveTo(ores.get(0).x, ores.get(0).y, ores.get(0).z);
}
```

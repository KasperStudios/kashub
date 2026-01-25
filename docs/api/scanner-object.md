# Scanner Object API

The `scanner` object provides methods for detecting blocks and entities around the player.

## Methods

### scanner.blocks([type], [radius])

Searches for blocks of a specific type within a given radius. Implementation includes a Performance/Fair Play filter (limited visibility check).

**Parameters:**
- `type` (string, optional): Block identifier or filter. Use `"*"` for all blocks. Defaults to `"*"` (though restricted by implementation to common ores/blocks if not specified usually).
- `radius` (number, optional): Search radius in blocks. Defaults to `32`.

**Returns:** Array of block objects, sorted by distance (nearest first).

**Block Object Properties:**
- `x`, `y`, `z` (number): Coordinates.
- `id` (string): Block identifier (e.g., `"minecraft:diamond_ore"`).
- `dist` (number): Distance from player.

**Example:**
```javascript
// Scan for diamond ore within 32 blocks
let ores = scanner.blocks("diamond_ore", 32)

if (ores.length() > 0) {
    let nearest = ores[0]
    System.print("Found diamond at " + nearest.x + ", " + nearest.y)
    player.lookAt(nearest.x, nearest.y, nearest.z)
}
```

### scanner.entities([type], [radius])

Searches for entities within a given radius.

**Parameters:**
- `type` (string, optional): Entity type filter. Supported: `"living"`, `"all"`, `"*"` or specific name like `"zombie"`. Defaults to `"living"`.
- `radius` (number, optional): Search radius in blocks. Defaults to `32`.

**Returns:** Array of entity objects, sorted by distance (nearest first).

**Entity Object Properties:**
- `type` (string): Entity identifier (e.g., `"zombie"`).
- `id` (number): Unique Entity ID.
- `x`, `y`, `z` (number): Current coordinates.
- `dist` (number): Distance from player.
- `health` (number, optional): Current health (for living entities).

**Example:**
```javascript
// Find nearest hostile mob
let mobs = scanner.entities("all", 16)

for (let i = 0; i < mobs.length(); i++) {
    let mob = mobs[i]
    if (mob.type.contains("zombie")) {
        System.print("Zombie detected: " + mob.id)
    }
}
```

## Internal Details

- **Distance Sorting**: All results from `scanner` methods are automatically sorted nearest-to-farthest.
- **Fair Play**: If enabled by the server, block scanning is filtered by visibility from the player's position (raycasting).

## See Also

- [Vision Object](vision-object.md)
- [World Object](world-object.md)
- [Tag Object](tag-object.md)

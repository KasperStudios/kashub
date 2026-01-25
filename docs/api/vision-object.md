# Vision Object API

The `vision` object provides utilities for detecting what the player is looking at and finding nearby entities.

## Methods

### vision.getTarget()

Returns information about the current crosshair target.

**Returns:** Object or `null` if nothing targeted.

**Object Properties:**
- `type` (string): `"block"`, `"entity"`, or `"miss"`.
- `x`, `y`, `z` (number): Coordinates of the hit target.
- `blockId` (string, for blocks): Identifier of the targeted block.
- `entityId` (string, for entities): Identifier of the targeted entity.
- `id` (number, for entities): Unique ID of the targeted entity.

**Example:**
```javascript
let target = vision.getTarget()
if (target != null && target.type == "block") {
    System.print("Looking at: " + target.blockId)
}
```

### vision.getNearest([type], [maxDist], [targetPart])

Finds the nearest entity matching the criteria.

**Parameters:**
- `type` (string, optional): Filter (e.g., `"hostile"`, `"living"`, `"all"`, or specific name like `"zombie"`). Defaults to `"living"`.
- `maxDist` (number, optional): Search radius. Defaults to `32.0`.
- `targetPart` (string, optional): Point on entity to target (`"head"`, `"body"`, `"legs"`). Defaults to `"body"`.

**Returns:** Object or `null`.

**Object Properties:**
- `type` (string): Entity type.
- `id` (number): Entity ID.
- `x`, `y`, `z` (number): Coordinates of the targeted part.
- `pos` (object): Position object with `x`, `y`, `z`.
- `distance` (number): Distance from player.
- `health` (number, for living): Current health.

**Example:**
```javascript
let enemy = vision.getNearest("hostile", 10, "head")
if (enemy != null) {
    player.lookAt(enemy.x, enemy.y, enemy.z)
    player.attack(enemy)
}
```

### vision.nearest([type], [maxDist], [targetPart])

Alias for `vision.getNearest()`.

### vision.isLookingAt(targetId, [maxDist])

Checks if the player is currently looking at a specific block or entity.

**Parameters:**
- `targetId` (string): Block or Entity ID to check for.
- `maxDist` (number, optional): Check distance. Defaults to `5.0`.

**Returns:** `true` if looking at target, `false` otherwise.

## See Also

- [Player Object](player-object.md)
- [Scanner Object](scanner-object.md)

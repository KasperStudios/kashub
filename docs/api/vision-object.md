# 👁️ Vision Object

The `vision` object provides utilities for querying the player's line of sight and detecting entities in the surrounding environment.

## Methods

### `vision.getTarget()`
Returns details about the block or entity currently in the player's crosshairs.
- **Returns:** `{type: string, id: string, x: number, y: number, z: number}`

### `vision.getNearest(type, [maxDist], [targetPart])`
Finds the nearest entity matching the specified criteria.
- **Parameters:** 
  - `type`: `"living"`, `"hostile"`, `"all"`, or a specific entity ID.
  - `maxDist`: (optional number) Maximum search distance.
  - `targetPart`: (optional string) `"head"`, `"body"`, `"legs"`.
- **Returns:** Entity object or `null`.

### `vision.nearest(type, ...)`
Alias for `vision.getNearest`.

### `vision.isLookingAt(targetId, [maxDist])`
Checks if the player is directly looking at a specific block or entity.
- **Parameters:** `targetId` (string), `maxDist` (optional number).
- **Returns:** `boolean`

### `vision.count(type, [maxDist])`
Returns the total number of entities of a certain type within range.
- **Returns:** `number`

### `vision.canSee(type, [maxDist])`
Returns true if at least one entity of the specified type is within the given distance.
- **Returns:** `boolean`

---

## Example: Targeting Mobs
```javascript
let target = vision.nearest("hostile", 20);
if (target != null) {
    System.print("Targeting " + target.id + " at " + target.pos);
    player.lookAt(target.pos.x, target.pos.y, target.pos.z);
    player.attack(target);
}
```

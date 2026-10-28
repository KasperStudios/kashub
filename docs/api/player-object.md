# 👤 Player Object

The `player` object provides methods for controlling the local player, querying their state, and performing actions like movement and interaction.

## Methods

### `player.pos()`
Returns the player's current world position.
- **Returns:** `{x: number, y: number, z: number}`

### `player.jump()`
Makes the player perform a single jump.
- **Returns:** `null`

### `player.moveTo(x, y, z)`
Starts the A* pathfinding engine to navigate to the specified coordinates.
- **Parameters:** `x, y, z` (numbers)
- **Returns:** `true` if pathfinding started successfully.

### `player.stopMove()`
Stops any active pathfinding task immediately.
- **Returns:** `null`

### `player.lookAt(x, y, z)`
Rotates the player's head to look directly at the target coordinates.
- **Parameters:** `x, y, z` (numbers)
- **Returns:** `null`

### `player.attack([entity])`
Attacks the specified entity or the entity currently in the player's crosshairs.
- **Parameters:** `entity` (optional object)
- **Returns:** `null`

### `player.interact()`
Simulates a right-click interaction with the block or entity in the player's crosshairs.
- **Returns:** `null`

### `player.isCreative()`
Checks if the player is currently in Creative mode.
- **Returns:** `boolean`

### `player.getYaw()`
Returns the player's current horizontal rotation.
- **Returns:** `number`

### `player.getPitch()`
Returns the player's current vertical rotation.
- **Returns:** `number`

### `player.swing()`
Plays the arm swing animation for the main hand.
- **Returns:** `null`

### `player.sneak(state)`
Sets the player's sneaking (crouching) state.
- **Parameters:** `state` (boolean)
- **Returns:** `null`

---

## Example: Combat & Movement
```javascript
let enemy = vision.nearest("zombie", 10);
if (enemy != null) {
    player.lookAt(enemy.pos.x, enemy.pos.y, enemy.pos.z);
    player.attack(enemy);
} else {
    player.moveTo(0, 64, 0); // Return to base
}
```

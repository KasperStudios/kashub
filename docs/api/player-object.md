# Player Object API

The `player` object provides methods for controlling the player and getting player state.

## Methods

### player.moveTo(x, y, z, [radius], [relative])

Starts A* pathfinding to the specified coordinates. Automatically avoids obstacles, climbs ladders, and handles jumps.

**Parameters:**
- `x`, `y`, `z` (number): Target coordinates.
- `radius` (number, optional): Acceptance radius. Defaults to `1.0`.
- `relative` (boolean, optional): If `true`, coordinates are relative to player's current position. Defaults to `false`.

**Returns:** `true` if pathfinding was successfully started.

**Example:**
```javascript
// Move to specific coordinates
player.moveTo(100, 64, 200)

// Move 10 blocks forward relatively with 2 block tolerance
player.moveTo(10, 0, 0, 2.0, true)
```

### player.moveBy(dx, dy, dz, [radius])

Convenience method for relative pathfinding. Same as `player.moveTo(dx, dy, dz, radius, true)`.

**Parameters:**
- `dx`, `dy`, `dz` (number): Relative offsets.
- `radius` (number, optional): Acceptance radius. Defaults to `1.0`.

**Returns:** `true`

**Example:**
```javascript
player.moveBy(5, 0, 5) // Move +5 on X and Z
```

### player.stopMove() / player.stopMoving()

Stops the current pathfinding task and clears all movement inputs.

**Returns:** `true`

### player.isMoveActive()

Checks if an A* pathfinding task is currently running.

**Returns:** `true` if moving, `false` otherwise.

### player.getPos()

Returns the player's current world position.

**Returns:** Object

**Properties:**
- `x`, `y`, `z` (number): Coordinates.

**Example:**
```javascript
let pos = player.getPos()
System.print("Currently at: " + pos.x + ", " + pos.y + ", " + pos.z)
```

### player.getHealth()

Returns current health including absorption (0-20+).

**Returns:** Number

### player.getHunger()

Returns current food level (0-20).

**Returns:** Number

### player.getName()

Returns the player's username.

**Returns:** String

### player.input(action, type)

Low-level simulation of player actions/inputs.

**Parameters:**
- `action` (string): Action to perform. Supported: `"forward"` (`"w"`), `"back"` (`"s"`), `"left"` (`"a"`), `"right"` (`"d"`), `"jump"` (`"space"`), `"sneak"` (`"shift"`), `"sprint"`, `"attack"` (`"leftclick"`), `"use"` (`"rightclick"`).
- `type` (string): Interaction type. Supported: `"press"` (hold down), `"release"`, `"tap"` (single click/press).

**Returns:** `true` if valid, `false` otherwise.

**Example:**
```javascript
// Sprint forward
player.input("forward", "press")
player.input("sprint", "press")

// Stop after 2 seconds
System.wait(2000)
player.input("forward", "release")
player.input("sprint", "release")

// Single jump
player.input("jump", "tap")
```

### player.lookAt(x, y, z)

Immediately turns the player to look at the specified coordinates.

**Parameters:**
- `x`, `y`, `z` (number): Coordinates to look at.

**Returns:** `true`

**Example:**
```javascript
player.lookAt(target.x, target.y + 1, target.z)
```

### player.attack([entity])

Attacks the specified entity or performs a left-click in the current direction.

**Parameters:**
- `entity` (object, optional): The entity to attack. If omitted, performs a generic attack/left-click.

**Returns:** `true`

### player.breakBlock()

Holds down left-click to break the block currently in crosshair.

**Returns:** `true`

### player.placeBlock(itemId)

Attempts to place a block from the hotbar.

**Parameters:**
- `itemId` (string): ID of block to place (e.g., `"stone"`).

**Returns:** `true` if successful.

### player.chat(message)

Sends a message or command to the server chat.

**Parameters:**
- `message` (string): Message or command starting with `/`.

**Returns:** `null`

## See Also

- [Pathfinding Guide](../pathfinding-guide.md)
- [Vision Object](vision-object.md)
- [Inventory Object](inventory-object.md)

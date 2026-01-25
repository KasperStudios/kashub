# World Object API

The `world` object provides access to world data and block information.

## Methods

### world.getBlock(x, y, z)

Returns the identifier of the block at the specified coordinates.

**Parameters:**
- `x`, `y`, `z` (number): Coordinates.

**Returns:** String (Block ID, e.g., `"minecraft:stone"`) or `null` if world not loaded.

**Example:**
```javascript
let blockAtFeet = world.getBlock(100, 64, 100)
System.print("Standing on: " + blockAtFeet)
```

### world.getTime()

Returns the current world time of day.

**Returns:** Number (0-24000)

### world.getWeather()

Returns current world weather state.

**Returns:** String (`"clear"`, `"rain"`, or `"thunder"`) or `"unknown"` if world not loaded.

**Example:**
```javascript
if (world.getWeather() == "thunder") {
    System.print("Watch out for lightning!")
}
```

### world.defineRecipe(type, id, ...)

Defines a custom client-side crafting recipe. This allows the built-in crafting logic to understand how to craft specific items.

**Parameters for `"shaped"`:**
- `type`: `"shaped"`
- `id`: Recipe identifier string.
- `row1`, `row2`, `row3`: 3-character pattern strings (e.g., `"AAA"`).
- `ingredients`: Multi-arg key-value pairs (e.g., `"A:stick"`, `"B:diamond"`).
- `result`: Result string (e.g., `"result:diamond_shovel"`).
- `count` (optional): Output count.

**Parameters for `"shapeless"`:**
- `type`: `"shapeless"`
- `id`: Recipe identifier string.
- `ingredients`: Multi-arg list of item IDs.
- `result`: Result string (e.g., `"result:sword"`).
- `count` (optional): Output count.

**Returns:** `true` if registered, `false` otherwise.

**Example:**
```javascript
// Define recipe for a diamond shovel
world.defineRecipe("shaped", "my_shovel", " A ", " S ", " S ", "A:diamond", "S:stick", "result:diamond_shovel")
```

## See Also

- [Scanner Object](scanner-object.md)
- [Tag Object](tag-object.md)

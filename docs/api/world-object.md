# 🌍 World Object

The `world` object provides access to environmental data, block information, and server-side configurations like custom recipes.

## Methods

### `world.getBlock(x, y, z)`
Returns the unique identifier (ID) of the block at the specified world coordinates.
- **Returns:** `string` (e.g., `"minecraft:stone"`)

### `world.getTime()`
Returns the current world time in ticks (0-24000).
- **Returns:** `number`

### `world.getWeather()`
Returns the current world weather status.
- **Returns:** `string` (`"clear"`, `"rain"`, or `"thunder"`)

### `world.defineRecipe(type, id, ...)`
Defines a custom server-side crafting recipe. Requires **SERVER** origin privileges.
- **Parameters:** `type` (`"shaped"`, `"shapeless"`, or `"remove"`), `id` (recipe identifier), and recipe-specific arguments.
- **Returns:** `boolean`

---

## Example: Checking Environment
```javascript
if (world.getWeather() == "thunder") {
    System.print("Warning: Storm approaching. Finding shelter...");
    player.moveTo(home.x, home.y, home.z);
}

let block = world.getBlock(player.pos().x, player.pos().y - 1, player.pos().z);
System.print("Standing on: " + block);
```

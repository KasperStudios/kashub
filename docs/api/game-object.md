# 🎮 Game Object

The `game` object allows scripts to modify client-side settings and rendering parameters.

## Methods

### `game.setGamma(value)`
Directly sets the game's brightness (gamma) level. This can exceed the normal 100% range in some implementations.
- **Parameters:** `value` (number)
- **Returns:** `null`

### `game.fullBright()`
Immediately sets the gamma to a high value (100.0) for maximum visibility in dark areas.
- **Returns:** `null`

### `game.setFov(value)`
Changes the player's Field of View (FOV).
- **Parameters:** `value` (number, typical range 30-110)
- **Returns:** `null`

---

## Example: Night Vision Toggle
```javascript
let time = world.getTime();
if (time > 13000 || world.getWeather() == "thunder") {
    game.fullBright();
} else {
    game.setGamma(1.0);
}
```

# Game Object API

The `game` object provides control over client-side game settings.

## Methods

### game.setGamma(value)

Sets the client brightness (gamma) level.

**Parameters:**
- `value` (number): Gamma value (e.g., `1.0`, `100.0`).

**Returns:** `true`

**Example:**
```javascript
game.setGamma(5.0)
```

### game.fullBright()

Sets the brightness to maximum (100.0).

**Returns:** `true`

**Example:**
```javascript
game.fullBright()
```

### game.setFov(value)

Sets the client Field of View (FOV).

**Parameters:**
- `value` (number): FOV value (e.g., `70`, `110`).

**Returns:** `true`

**Example:**
```javascript
game.setFov(90)
```

## See Also

- [System Object](system-object.md)
- [World Object](world-object.md)

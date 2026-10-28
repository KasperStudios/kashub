# ⏺️ W2P (Write-to-Play) Object

The `w2p` object controls the **Macro Recording** system, allowing scripts to record and replay player actions.

## Methods

### `w2p.record([name])`
Starts recording the player's inputs into a macro.
- **Parameters:** `name` (optional string, defaults to `"default"`)
- **Returns:** `boolean`

### `w2p.stop()`
Stops the current recording or playback process.
- **Returns:** `boolean`

### `w2p.play([name])`
Starts replaying a previously recorded macro.
- **Parameters:** `name` (optional string, defaults to `"default"`)
- **Returns:** `boolean`

---

## Example: Macro Control
```javascript
// Record a 5-second action
w2p.record("mine_pattern");
System.wait(5000);
w2p.stop();

// Replay it 3 times
for (let i = 0; i < 3; i++) {
    w2p.play("mine_pattern");
    System.wait(5000);
}
```

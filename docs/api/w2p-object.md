# W2P Object API (Write to Play)

The `w2p` (Write to Play) object provides a macro system for recording and replaying player actions.

IN DEVELOPING

## Methods

### w2p.record(name)

Starts recording player actions into a macro with the specified name.

**Parameters:**
- `name` (string, optional): Name of the macro. Defaults to `"default"`.

**Returns:** `true`

**Example:**
```javascript
// Start recording actions to "mining_run"
w2p.record("mining_run")
System.print("Recording started. Perform actions now.")
```

### w2p.stop()

Stops the current macro recording or playback.

**Returns:** `true`

**Example:**
```javascript
// Stop whatever is happening
w2p.stop()
System.print("Recording/Playback stopped.")
```

### w2p.play(name)

Plays back a previously recorded macro.

**Parameters:**
- `name` (string, optional): Name of the macro to play. Defaults to `"default"`.

**Returns:** `true`

**Example:**
```javascript
// Replay the "mining_run" macro
w2p.play("mining_run")
System.print("Playing macro...")
```

## See Also

- [Player Object](player-object.md)
- [System Object](system-object.md)

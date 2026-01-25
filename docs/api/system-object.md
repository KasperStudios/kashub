# System Object API

The `System` object provides system-level utilities for scripts.

## Methods

### System.print(message)

Outputs a message to the in-game chat (prefixed with `§7[KH]§r`) and the console.

**Parameters:**
- `message` (string): The message to output.

**Returns:** `null`

**Example:**
```javascript
System.print("Hello from KasHub!")
```

### System.log(message)

Outputs a message to the console only (no in-game chat). Use this for debugging to avoid spamming chat.

**Parameters:**
- `message` (string): The message to log.

**Returns:** `null`

**Example:**
```javascript
System.log("Internal status update: OK")
```

### System.wait(ms)

Pauses the script execution for a specified duration.

**Parameters:**
- `ms` (number): Duration in milliseconds.

**Returns:** `null`

**Example:**
```javascript
System.print("Waiting for 5 seconds...")
System.wait(5000)
System.print("Done!")
```

### System.time()

Returns the current system timestamp in milliseconds.

**Returns:** Number (timestamp)

**Example:**
```javascript
let start = System.time()
// ... do something ...
let elapsed = System.time() - start
System.print("Action took " + elapsed + "ms")
```

### System.exit()

Immediately stops the execution of the current script.

**Returns:** `null`

**Example:**
```javascript
if (player.getHealth() < 1) {
    System.print("Player died, stopping script.")
    System.exit()
}
```

### System.gc()

Requests immediate Java Garbage Collection. 

**Returns:** `null`

### System.memory()

Returns an object with current memory usage information.

**Returns:** Object

**Properties:**
- `used` (number): Used memory in MB.
- `free` (number): Free memory in MB.
- `total` (number): Total allocated memory in MB.
- `max` (number): Maximum possible memory in MB.

**Example:**
```javascript
let mem = System.memory()
System.print("Memory usage: " + mem.used + " / " + mem.max + " MB")
```

## See Also

- [Game Object](game-object.md)
- [Player Object](player-object.md)

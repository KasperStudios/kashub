# 🖥️ System Object

The `System` object (capital 'S') provides core engine utilities, script lifecycle management, and hardware status information.

## Methods

### `System.print(message)`
Outputs a message to the in-game script console.
- **Parameters:** `message` (string or object)
- **Returns:** `null`

### `System.wait(ms)`
Pauses the script execution for a specific duration.
- **Parameters:** `ms` (number, milliseconds)
- **Returns:** `null`

### `System.time()`
Returns the current system epoch time in milliseconds.
- **Returns:** `number`

### `System.exit()`
Immediately terminates the current script.
- **Returns:** `null`

### `System.gc()`
Requests a garbage collection to free up memory (use sparingly).
- **Returns:** `null`

### `System.getMemory()`
Returns current memory usage information.
- **Returns:** `{total: number, free: number, used: number}`

---

## Example: Execution Control
```javascript
System.print("Starting process...");
let start = System.time();

System.wait(1000); // Simulate work

let end = System.time();
System.print("Finished in " + (end - start) + "ms");
System.exit();
```

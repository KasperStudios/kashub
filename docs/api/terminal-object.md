# 📟 Terminal Object

The `Terminal` object (capital 'T') provides a DSL (Domain Specific Language) for interacting with the **Khinux Terminal** environment and managing system state.

## Methods

### `Terminal.currentDir()`
Returns the current working directory of the Khinux environment.
- **Returns:** `string` (e.g., `"~/scripts"`)

### `Terminal.setCurrentDir(path)`
Changes the current working directory.
- **Parameters:** `path` (string)
- **Returns:** `boolean`

### `Terminal.hasSudo()`
Checks if the current terminal session has active `sudo` privileges.
- **Returns:** `boolean`

### `Terminal.hasSu()`
Checks if the session is currently logged in as the SuperUser (`root`).
- **Returns:** `boolean`

### `Terminal.execute(command)`
Executes a shell command in the terminal. Requires **TERMINAL** or **SERVER** origin.
- **Parameters:** `command` (string)
- **Returns:** `boolean`

### `Terminal.get(key)`
Retrieves the value of a terminal environment variable.
- **Parameters:** `key` (string)
- **Returns:** `string` or `null`

### `Terminal.set(key, value)`
Sets a terminal environment variable. Requires `sudo` privileges.
- **Parameters:** `key` (string), `value` (string)
- **Returns:** `boolean`

### `Terminal.getPrompt()`
Returns the current prompt format string.
- **Returns:** `string`

### `Terminal.setPrompt(prompt)`
Sets a custom prompt format for the terminal. Requires `sudo` privileges.
- **Parameters:** `prompt` (string)
- **Returns:** `boolean`

---

## Example: Shell Integration
```javascript
if (!Terminal.hasSudo()) {
    System.print("Elevated access required.");
} else {
    Terminal.execute("mkdir ~/backups");
    Terminal.execute("cp ~/main.kh ~/backups/main_bak.kh");
}
```

# 📁 VFS Object

The `VFS` object (capital 'V') provides direct access to the **Virtual File System**, allowing scripts to navigate and inspect the internal directory structure.

## Methods

### `VFS.list([path])`
Lists all files and subdirectories in the specified virtual path. If no path is provided, it lists the current directory.
- **Parameters:** `path` (optional string)
- **Returns:** List of `{name: string, isDirectory: boolean, virtualPath: string}`

### `VFS.exists(path)`
Checks if a file or directory exists at the given virtual path.
- **Parameters:** `path` (string)
- **Returns:** `boolean`

### `VFS.isDirectory(path)`
Checks if the target at the given virtual path is a directory.
- **Parameters:** `path` (string)
- **Returns:** `boolean`

---

## Example: Directory Traversal
```javascript
let files = VFS.list("~/my_projects");
for (let file of files) {
    if (file.isDirectory) {
        System.print("[DIR] " + file.name);
    } else {
        System.print("[FILE] " + file.name);
    }
}
```

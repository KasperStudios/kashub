# 📟 Khinux Terminal System

The Khinux Terminal is the command-line interface for the Kashub ecosystem. It provides a Unix-like environment inside Minecraft to manage files, scripts, and system processes.

## 🛠️ Dual-Mode Operation

The Terminal in the KasHub IDE operates in two distinct modes:

### 1. Local Mode (Khinux)
Used for interacting with the player's internal OS layer. 
- Manage local `.kh` scripts.
- Control background tasks.
- Modify system settings.

### 2. Neural Link Mode (Remote)
When connected to a ComputerCraft: Tweaked (CC:T) computer, the terminal becomes a proxy.
- All input is sent to the remote computer.
- Remote output is displayed in the IDE.
- Full access to the CC:T Lua environment directly from the Kashub editor.

## 📁 Virtual File System (VFS)

Khinux uses a Virtual File System to unify different storage locations:

- `~/` : Your local scripts folder (Minecraft config directory).
- `/mnt/world/` : (Read-only) Access to world data and block coordinates.
- `/mnt/cc/` : (Linked) Access to the filesystem of a connected CC:T computer.

### Common File Commands
```bash
ls          # List files in current directory
cd <dir>    # Change directory
cat <file>  # View file content
rm <file>   # Remove file
mv <a] [b]  # Move/Rename file
```

## 🔐 Permissions: Sudo & Su

Khinux implements a basic permission system to protect your game and server.

- **`sudo <command>`**: Execute a command with temporary elevated privileges. Required for modifying sensitive system settings or deleting protected files.
- **`su`**: Switch to the Root user. This provides permanent elevated access but is dangerous if used incorrectly.

> [!WARNING]
> Dangerous commands like `rm -rf /` or `format` are blocked by the Fair Play System unless `allowDangerousCommands` is enabled in the server config.

## ⚙️ System Settings

You can modify the environment using the `set` command:

```bash
set PROMPT "[Khinux] %u@%p:%w$ "
set THEME dark_premium
```

## 🔄 Background Processes

Scripts can be run in the background, allowing you to automate tasks while you continue playing or editing.

- `run <script> &` : Starts a script in the background.
- `ps` : Lists all running background tasks.
- `kill <pid>` : Terminates a specific process.

---

> [!NOTE]
> The Terminal is accessible by clicking the Terminal tab at the bottom of the IDE or by pressing `Ctrl + \` (default).

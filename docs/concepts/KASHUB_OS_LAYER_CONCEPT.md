# Kashub OS Layer Concept v2

Mini-OS on top of KasHub: terminal, sandbox, permissions, packages, and drivers.

## 1. General Idea

KasHub is a scripting engine with a `.kh` DSL and a VS Code-style editor inside Minecraft.

On top of this engine, a **terminal OS layer** is added:
- console in KHGui (bottom TERMINAL panel),
- sandbox file system,
- built-in commands (`cd`, `ls`, `open`, `kh`, `sudo`, `su`),
- permission system (normal, sudo, su),
- packages as apps/installers/libraries/drivers,
- eventually, "drivers" and mod-to-mod communication running at the kernel level.

Goal: Create a "mini-Linux inside Minecraft" feel, but safely and under the engine's control.

***

## 2. Terminal and Sandbox File System

### 2.1. Terminal (ConsolePanel + TerminalManager)

- KHGui already has `ConsolePanel` and the `TERMINAL` tab.
- A logical layer `TerminalManager` is added, which:
  - stores the terminal context (current directory, permission flags);
  - accepts input strings;
  - parses and routes commands (`cd`, `ls`, `open`, `kh`, `sudo`, `su`, package commands);
  - invokes the KasHub interpreter with the correct flags.

### 2.2. Sandbox FS

- The user sees a **virtual root** (sandbox), mounted to the KasHub data directory:
  - e.g.: `~/` → `config/kashub/scripts` and user data;
  - `/system` → `config/kashub/system` and packages.
- Navigation:
  - `cd path` operates within the sandbox, `..` prevents escaping the virtual root.
- Access:
  - normal mode: can only see the user area;
  - with `sudo`: can see and enter `/system` and other system directories.

Implemented via a dedicated `VirtualFileSystem`, which:
- resolves virtual paths (`~/`, `/system`, relative);
- prevents escaping the root boundary;
- checks `ctx.hasSudo()` when accessing system paths.

***

## 3. Built-in Terminal Commands

The terminal supports **native commands** processed on the Java side:

### 3.1. `cd <path>`

- Changes the current directory relative to the sandbox:
  - supports `.` and `..`;
  - prevents escaping the virtual root.
- Requires `sudo` when entering system directories (`/system/...`).

### 3.2. `ls`

- Shows the contents of the current directory.
- Normal mode:
  - only user files/folders.
- With `sudo`:
  - additionally shows system files and internal directories.

### 3.3. `open <path>`

- Opens the specified file in the KHGui editor.
- Normal mode:
  - files from the user area of the sandbox.
- With `sudo`:
  - can open system files, package files, configs.

### 3.4. `kh ...` (Package Manager)

- Terminal frontend for the package manager:
  - `kh install/i <pkg>`
  - `kh list`
  - `kh uninstall/uni <pkg>`
  - later: `kh info`, `kh update`, etc.
- Operates on top of the new package concept (see below).
- The engine decides whether `sudo`/`su` is needed based on the APIs the package uses (not the JSON).

### 3.5. `sudo` and `su`

- Commands **exclusive to the terminal** (not DSL):

`sudo`:
- `sudo <command>`:
  - executes a single terminal command with `sudo` privileges;
  - injects `ctx.hasSudo() = true` into the interpreter.
- conditionally, `sudo` without arguments:
  - elevates to `sudo` for the entire terminal session.

`su`:
- enables **driver/kernel-mode** for the current session:
  - sets the `suMode` flag in the terminal context;
  - anything executed afterwards receives `ctx.hasSu() = true` (and effectively `hasSudo()` as true).
- completely unavailable from code:
  - no `su` function exists in the DSL;
  - invoking via `System.console("su")` is blocked (see below).
- may have an empty default password, but is only documented in the dev-docs.

***

## 4. Permission System: normal, sudo, su

Permissions live at the **terminal session layer** and are passed to the interpreter `Context` when running scripts/commands.

### 4.1. Normal Mode

- `sudo=false`, `su=false`.
- Allowed to:
  - interact with user-scripts and sandbox data;
  - use safe DSL commands and functions.
- NOT allowed to:
  - enter `/system` directories;
  - access dangerous APIs (kernel, eval, low-level package management).

### 4.2. `sudo` (System Level)

- `hasSudo() == true`, `hasSu() == false`.
- Allowed to:
  - view and interact with system directories;
  - install/uninstall system packages;
  - modify system KasHub configurations.
- NOT allowed to:
  - use kernel eval/Java-hooks,
  - access the lowest-level driver-APIs.

### 4.3. `su` (Driver/Kernel Level)

- `hasSu() == true` (`hasSudo()` is automatically treated as true).
- Allowed to:
  - call DSL eval-functions capable of executing Java code;
  - manage internal KasHub state and other packages at a "mod-on-mod" level;
  - write drivers/extensions that run close to the core.

Obtaining `su` is only possible through the interactive `su` terminal command. Scripts have zero access to it.

***

## 5. Security: System.console and Dangerous APIs

### 5.1. Blocking sudo/su via DSL

There are no built-in `sudo` or `su` commands/functions in the DSL. Scripts can only communicate with the terminal through **controlled bridges**, for example:

- `System.console("command")`
- or other high-level APIs to be defined.

Inside the `System.console(...)` implementation:

- if the string starts with `sudo `, `"su"`, or `su `:
  - the call is rejected with a `SecurityException` or a logged error;
  - scripts cannot initiate sudo/su-mode.

### 5.2. Dangerous Commands and Functions

At the Java level:

- commands (`Command`) and internal objects can have flags:
  - `requiresSudo()`
  - `requiresSu()`
- before executing commands, the interpreter checks these flags against the `Context`:
  - if permissions are missing -> throws a controlled error without crashing the session.

Inside DSL-APIs (e.g., `kernel.*`, `fs.*`, driver objects):

- each method independently checks `ctx.hasSudo()/hasSu()`,
- if lacking permissions -> denies execution and provides a clear result (error in console).

***

## 6. Packages: Apps, Libs, Installers, Drivers

A package is a **meta-wrapper around `.kh` scripts** alongside a `kashub.json` manifest.

### 6.1. Package Types (New Vision)

- `app` — terminal applications:
  - registers commands in the terminal (`hw`, `inv.setup`, `radar.run`, etc.);
  - they act as CLI programs inside your mini-OS.

- `lib` — libraries:
  - contain `export fn` / `export var` within `.kh`;
  - accessed via `use "..."` and imports.

- `installer` — installers and orchestrators:
  - manage the installation of other packages/scripts;
  - pull dependencies via `kh` + marketplace.

- `driver` / `core` — low-level:
  - require `su` to operate;
  - manage daemons, autostart, deep integrations (e.g., a future `kashub-systemd`).

- `hybrid` — combinations (app + installer + lib).

### 6.2. `kashub.json` Manifest (Simplified)

The manifest describes **structure**, not permissions:

```json
{
  "name": "hello_wallet",
  "version": "1.0.0",
  "author": "KasperStudios",
  "description": "Test app that prints hey wallet",
  "type": "app",
  "entry": "main.kh",
  "commands": [
    {
      "name": "hw",
      "description": "Prints hey wallet",
      "handler": "hw_handler"
    }
  ]
}
```

For a library:

```json
{
  "name": "some_lib",
  "version": "1.0.0",
  "author": "KasperStudios",
  "description": "Utility library",
  "type": "lib",
  "entry": "main.kh"
}
```

Crucial constraint:
- **No `permissions/sudo_required/su_required` in the JSON.**
- Exports (`export fn/var`) stay **in the `.kh` code**, they are not duplicated in the manifest.

***

## 7. Package Manager `kh` and Marketplace

### 7.1. `kh` inside Terminal

The `kh` command in the terminal acts as the interface to the package system:

- `kh install <name>` — installation:
  - queries your marketplace (Vercel API) or local registry;
  - downloads and extracts the package to `config/kashub/packages/...`.
- `kh list` — prints installed packages.
- `kh uninstall <name>` — removal.

Permission constraints:

- regular packages can be installed without `sudo`;
- if a package requires dangerous APIs (determined by code/command analysis):
  - the engine strictly demands `sudo kh install ...` or `su kh install ...`.

### 7.2. Marketplace

- Central package repository:
  - packages arrive via issues/PRs (moderation).
- Web-Marketplace (Vercel):
  - browsing packages, descriptions, versions;
  - separate sections for packages and scripts.
- API:
  - `GET /packages/...` → provides registry, meta, download links;
  - KasHub ping ONLY this API, not GitHub directly.

***

## 8. su, Passwords, and Dev-Documentation

- `su`:
  - exists solely as a terminal command;
  - defaults to an empty password;
  - can be optionally configured:
    - password can be enabled in settings for servers/advanced security.
- Standard terminal documentation:
  - explains `cd`, `ls`, `open`, `kh`, `sudo`.
- At the end:
  - a link like "Advanced developer console features" → dev-docs:
    - covers `su`,
    - driver-level, eval, constraints,
    - risks and recommendations.

***

## 9. Hardware Integrations (ComputerCraft)

KasHub provides optional, deep integration with hardware mods like **ComputerCraft: Tweaked (CC:T)**. 
- **Virtual File System Mounting**: Remote CC computers can be mounted into the local KasHub sandbox (e.g., `/mnt/cc/1/`), enabling seamless file editing across the network.
- **Terminal Tunneling**: The KasHub terminal can proxy I/O directly to a remote computer, supporting full keystroke injection, `paste` events, and `Ctrl+C` termination.
- **Neural Scan**: Tier 2 Neural Implants can dynamically scan CC:T peripherals using reflection, extracting their type and Lua methods, and compiling them into an interactive local documentation compendium.

# Kashub DevNotes

> IMPORTANT: This file contains critical project rules, implementation context, and constraints for the author and AI assistants.
> Treat this file as the operational contract of the project.
> If something is unclear, prefer existing code/project patterns over invention.

---

## 0. DevNotes Writing Rules

**DevNotes is an internal operational document, not a marketing page.
Do not add hype, “wow-effect”, promotional language, or exaggerated claims.
Write only what is useful for implementation, review, maintenance, and AI execution.**

This file is operational, not promotional. No marketing language, hype, or “wow-effect” wording. Do not beautify the project or exaggerate maturity/stability. Prefer precise facts over impressive wording.

If a sentence does not change how the project is implemented, reviewed, or maintained — it does not belong in this file.

## 1. Project Overview

### What this project is
Kashub is a scriptable automation and modding framework for Minecraft, designed to provide advanced debugging, profiling, and event-based scripting capabilities within the game.

### Core goal
To create a robust, extensible environment for Minecraft modding and automation that is accessible to a global audience through English-first interfaces.

### Success criteria
- Functional command system with English help text.
- Stable event system for script triggers.
- Reliable cross-platform execution (Windows/Linux).
- High performance with minimal impact on game TPS.

### Project type
- [ ] Web app
- [ ] Website / landing
- [ ] Desktop app
- [ ] Library / framework
- [ ] Bot / automation
- [x] Game / mod
- [ ] API / backend
- [ ] Other: 

---

## 2. Current Status

### Status summary
v1.0.0-FSR (First Stable Release) achieved.

### Implemented
- [x] **Stabilized Core Engine (v1.0.0-FSR)**.
- [x] Unified Value system with prototype method support.
- [x] Event system for game events (mixin-based).
- [x] Debug system (breakpoints, profiler).
- [x] Fabric mixins for core game hooks (1.21.1).
- [x] Scripting engine core (KHScript) with recursion protection.
- [x] REST API & DTOs for external integration.

### In progress
- [x] **Core Milestone: Mojang Mappings & Multiloader Baseline** (Fabric + NeoForge compile) ✅
- [x] **FSR Milestone: ComputerCraft Bridge & VFS Integration** ✅
- [x] **FSR Milestone: Access Policy Manager & Sandbox Hardening** ✅
- [x] Stabilizing KHGui integrations (NeoForge parity achieved). ✅

### Removed from v1.0.0-FSR scope
- ~~W2PObject (macro recording/playback)~~ → Deferred to v1.1+
- ~~AutoCraftService~~ → Deferred to v1.1+
- ~~BugReporter~~ → Removed (dead code)
- ~~Package System (import/export, kashub.json, kh install)~~ → Deferred to v1.1+

### Research & Proposals (Not yet canonical)
- [ ] **Dynamic Documentation** (Under discussion) - Advanced implant functionality.

### Planned / TODO
- [ ] **Neuro-Implant** (Phase 1) - Survival access gating via inventory slot.
- [ ] **Advanced Interaction Reach** - Hand-reach based detection (max 10-15 blocks).
- [ ] **ComputerCraft Integrated Documentation** - KHScript tutorials inside CC terminal.
- [ ] Marketplace & Community features (v1.1.0).
- [x] Full stable release (v1.0.0-FSR). ✅

### Not in scope right now
- Major UI overhauls not related to core functionality.
- Support for Minecraft versions older than 1.21.1.
- Skill trees/RPG mechanics for KasHub (planned for later versions).

---

## 3. Tech Stack

### Core stack
- Runtime: Minecraft 1.21.1 (Fabric)
- Language(s): Java 21
- Framework(s): Fabric API, NeoForge API, Architectury API, KHGui, KHWrapper
- Build tools: Gradle
- Storage / DB: Local files (VFS) / Configs
- External services: REST API support

### Versions
- Minecraft: 1.21.1
- Fabric Loader: >=0.18.4 (Crucial for KHGui/Architectury compatibility)
- Architectury API: 13.0.8+
- Java: 21

### Versioning Scheme (v1.1+)
- **`vX.Y.Z-N`** — build N of version X.Y.Z, released as GitHub **pre-release** (e.g. `v1.1.0-1`, `v1.1.0-2`)
- **`vX.Y.Z`** — final release without build number, published to all distribution channels
- Commits are frequent and incremental (no single mega-commit). Successful builds are tagged and pre-released.
- v1.0.0-FSR is a special case: first stable release, no build numbers applied retroactively.

---

## 4. Architecture Notes

### High-level architecture
Fabric mod with a custom internal engine for commands, events, and scripting. Uses Mixins for deep integration with Minecraft internals.

### Main modules / layers
1. `core/` - Engine core (Parser, Registry, Context, Value).
2. `core/commands/` - Script command implementations.
3. `core/objects/` - Script-exposed OO wrappers (Player, System, etc.).
4. `api/` - REST API and data transfer objects.
5. `mixin/` - Fabric mixins for bytecode manipulation.
6. `debug/` - Debugger and profiler logic.

### Platform Abstraction Layer (KHWrapper / KHGui)

Kashub core **MUST NOT** depend directly on Fabric, NeoForge or any loader‑specific APIs.

All platform‑specific code is isolated behind `KHWrapper` and UI is isolated behind `KHGui`. Architectury's `ExpectPlatform` implementations must reside in platform-specific subpackages (e.g., `.neoforge` or `.fabric`) for correct runtime discovery.

#### Platform boundaries

- Core code (`kasperstudios.kashub.core`, `debug`, `services`):
  - NO direct imports of `net.fabricmc.*`, `net.neoforge.*` or other loader‑specific classes.
  - All platform operations (paths, lifecycle, input, networking, commands) go through `KHWrapper`.

- Platform adapters:
  - `khwrapper` provides `IPlatformWrapper` interface plus concrete implementations per loader.
  - Fabric and NeoForge specific bootstraps live **outside** `kashub` core module.

#### KHWrapper responsibilities (stable contract)

`IPlatformWrapper` is the only allowed entry point for:

1. Loader & lifecycle:
   - `onModInit(Runnable callback)`
   - `onClientInit(Runnable callback)`
   - `onClientTick(Runnable callback)`
   - `onClientStopping(Runnable callback)`

2. Paths & filesystem:
   - `Path getConfigDir()`
   - `Path getGameDir()`
   - `Path getModsDir()`
   - `Collection<ModInfo> getAllMods()`
   - `boolean isModLoaded(String modId)`

3. Input / keybindings:
   - Keybinding registration via wrapper (no direct `KeyBindingHelper` / `KeyMapping` in core).
   - Key state checks via wrapper (no direct GLFW / InputUtil calls).

4. Networking:
   - Packet registration, send/receive and client join events through wrapper abstraction only.
   - `playBidirectional` is the standard for high-performance cross-platform packet handling.
   - No direct `ClientPlayNetworking` / `PayloadTypeRegistry` / NeoForge equivalents in core.

5. Commands / client commands:
   - Client command registration via wrapper‑level abstraction (no direct Brigadier / Fabric client command APIs in core).

#### KHGui responsibilities

- All editor/UI code must go through `KHGui` abstractions.
- Core logic must not depend on concrete rendering or widget implementations.
- Any GUI behaviour that requires platform APIs must be implemented inside KHGui, not inside Kashub.

#### Platform review rule

Any new usage of loader APIs (`Fabric`, `NeoForge`, `MinecraftClient` client hooks) MUST be checked:
- if it belongs to core logic → it must be moved behind `KHWrapper` or `KHGui`.
- direct imports are allowed only in platform bootstrap or adapter modules.

### Access Philosophy & ComputerCraft Integration
The access model is based on three main modes:
- **Survival**: Gated by **Neuro-Implant** (required to see the UI). Interaction radius is based on player's effective hand reach (`min(reach, 10-15 blocks)`).
- **Casual**: Configurable toggle to skip progression gates (immediate UI access, hand reach radius).
- **Creative**: Remote access to all computers in loaded chunks via a dropdown selection list.

#### ComputerCraft Bridge
- **KHinux Integration**: CC File System is mounted to `/mnt/cc/<id>/`.
- **Dual Runtime**: CC capabilities exposed to both Lua and KHScript.
- **Connection UX**: Contextual "Connect/Exit" button instead of magic overlays.
- **Built-in Docs**: Integrated documentation for KHScript inside the CC environment.

### Dependency direction
```txt
[core/commands] -> [core]
[core/objects] -> [core]
[core] -> [util/VirtualFileSystem]
[Mixin] -> [core/Minecraft internals]
```

### Important boundaries
- `ScriptLogger` must be used for ALL user-facing messages.
- Async operations must be confined to managed thread pools (executor in `TerminalManager`).
- Value objects must be immutable or handle mutability explicitly (List/Map).

---

## 5. Project Structure

```txt
kashub/
├── src/main/java/kasperstudios/kashub/
│   ├── core/                  # Core Engine
│   │   ├── commands/          # Script commands (if, while, let...)
│   │   ├── objects/           # Script API objects (Player, System...)
│   │   ├── Parser.java        # Expression & Script parser
│   │   ├── Registry.java      # Command registry
│   │   └── Value.java         # Unified type system
│   ├── debug/                 # Debug system (profiler, breakpoints)
│   ├── api/                   # REST API & DTOs
│   ├── mixin/                 # Fabric mixins
│   ├── util/                  # Utilities (VFS, Logger)
│   └── services/              # High-level services (Pathfinding, Integration)
├── docs/                      # User documentation (English)
└── config/kashub/             # Config files
```

### Structure rules
- New script commands must be registered in `Registry.java`.
- API objects should inherit from `Value` and implement `getMember`.

---

## 6. Language & Text Rules

### User-facing language
- **ALL user-facing text MUST be in ENGLISH.**

---

## 7. Code Rules

### General style
- Follow the `Command` interface for interpreter extensions.
- Add null checks for `MinecraftClient.getInstance().player`.

### Logging
- In KHScript: `print` (System.print) for chat, `log` (System.log) for console only.

### Async / threading / concurrency
- Use `CompletableFuture.runAsync()` ONLY with the mod's specific executor or `client.execute()` for main thread tasks.

---

## 8. Critical Rules

1. **Language:** All user-facing text must be English.
2. **Registry:** Use `Registry.java` for script commands.
3. **Files:** Do not create any new `.md` files; only edit existing ones.

---

## 9. AI Assistant Context (Local Repositories)

These paths describe the **local layout on the author’s machine** and are provided
for AI assistants as context only. They MUST NOT be used as hardcoded paths in code
or build scripts.

### 9.1 Related local projects

The following directories contain adjacent projects used by Kashub:

- `C:\Users\kasperenok\Desktop\projects\experiments\khwrapper`
  - Local working copy of **KHWrapper** (platform abstraction / loader bridge).
  - Used as the source for the `khwrapper` library consumed by Kashub.

- `C:\Users\kasperenok\Desktop\projects\KHGui`
  - Local working copy of **KHGui** (UI abstraction and editor components).
  - Used as the source for the `khgui` library consumed by Kashub.

### 9.2 Rules for AI assistants

- These paths are **hints only** for reading/editing code during local sessions.
- Do NOT:
  - hardcode these paths into Java code, configs, or Gradle scripts;
  - assume they exist on other machines or CI.
- When referring to KHWrapper or KHGui in code:
  - use Gradle dependencies and module names,
  - treat the local folders as implementation detail, not part of the public contract.

---

## 23. Stable Runtime Surface (v1.0.0-FSR)

The following API surface is considered stable and normalized as of Phase 0.5 stabilization.

### Core Commands
- `let`, `var` : Declaration. Supports `let x = 5` or `let x`.
- `if (condition) { block }` : Conditional block. Requires braces.
- `while (condition) { block }` : Loop. Safe against infinite spinning (Thread interrupt check).
- `fn name(args) { block }` : Function declaration.
- `assignment` : Supports deep updates: `obj.prop = value`.

### Verified Global Objects
- **System** : `print`, `log`, `wait(ms)`, `time()`, `exit()`, `openFile(path)`, `memory()`, `gc()`, `console(cmd)`.
- **player** : `getPos()`, `getHealth()`, `getHunger()`, `getName()`, `moveTo(x,y,z)`, `input(action, type)`, `attack(entity?)`, `breakBlock()`, `placeBlock(name)`, `sprint(bool)`, `lookAt(x,y,z)`, `chat(msg)`.
- **VFS** : `read(path)`, `write(path, data)`, `list(path)`, `exists(path)`, `delete(path)`, `mkdir(path)`.

### Variable Scoping
- Variables starting with `$` are redirected to the `Environment` singleton (read-only for scripts unless set via specific commands).
- Local variables (let/var) take precedence over parent scope.

### Dynamic Documentation (Not yet canonical)
- **Proposed**: Advanced Neuro-Implant feature to display block methods and code examples in real-time.
- **Toggle**: Keybind-based overlay to prevent UI clutter.

---

**Last Updated:** 2026-09-19
**Project Status:** v1.0.0-FSR released (First Stable Release — Fabric + NeoForge 1.21.1)
**DevNotes Version:** v1.0.0-FSR

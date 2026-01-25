# Development Notes for Kashub Mod

**IMPORTANT:** This file contains critical guidelines for AI assistants working on this project.

---

## 🌍 LANGUAGE REQUIREMENTS

### Code & User-Facing Text
**ALL user-facing text MUST be in ENGLISH:**
- Command descriptions
- Error messages
- Log messages
- Help text
- Documentation strings
- Chat messages
- UI text
- Comments in code (preferred English, but Russian acceptable)

**Reason:** The mod will be promoted internationally and must be accessible to global audience.

### Documentation
- **README.md** - English (primary)
- **CHANGELOG.md** - English
- **Guides (docs/)** - English
- **Code comments** - English preferred, Russian acceptable
- **Internal notes** - Any language

---

## 📝 CODE STYLE

### Logging
- Use `ScriptLogger.getInstance()` for user-facing messages
- Use `LOGGER` (Log4j) for internal debug/error logging
- Never use `System.out.println()` or `System.err.println()`
- **In KHScript**: Use `print` for chat messages, `log` for console/file only

### Error Messages
```java
// ✅ GOOD
ScriptLogger.getInstance().error("Invalid argument: " + arg);

// ❌ BAD
System.out.println("Неверный аргумент: " + arg);
```

### Command Help Text
```java
@Override
public String getDetailedHelp() {
    return "Command description in English.\n\n" +
           "Usage:\n" +
           "  command <arg> - Description\n\n" +
           "Examples:\n" +
           "  command example\n\n" +
           "Notes:\n" +
           "  - Important note";
}
```

---

## 🔧 TECHNICAL GUIDELINES

### Threading
- Use `ExecutorService` with fixed thread pool, NOT `CompletableFuture.runAsync()` without executor
- Always add timeouts to async operations
- Add rate limiting for external API calls

### Minecraft Version
- Target: Minecraft 1.21.1
- Mod Loader: Fabric
- Use `Registries` API, not deprecated `Registry`
- Use `RegistryEntry<T>.value()` to get actual value from registry entries

### Command Registration
- Register all commands in `CommandRegistry.java`
- Use `safeRegister()` wrapper for graceful failure handling
- Commands must implement `Command` interface

### Performance
- Cache expensive operations
- Limit search radius (max 64 blocks)
- Add cooldowns to prevent spam
- Use `CompletableFuture` for async operations

---

## 🐛 COMMON PITFALLS

### 1. Registry API Changes (Minecraft 1.21+)
```java
// ❌ OLD (doesn't work in 1.21+)
StatusEffect type = effect.getEffectType();
String name = Registries.STATUS_EFFECT.getId(type).getPath();

// ✅ NEW (correct for 1.21+)
String name = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).getPath();
```

### 2. Chat Message Mixin (Minecraft 1.21.1)
```java
// ❌ OLD (Minecraft 1.20.x and earlier)
@Inject(method = "onGameMessage", at = @At("HEAD"))
private void onChatMessage(GameMessageS2CPacket packet, boolean overlay, CallbackInfo ci) {
    if (overlay) return;
    // ...
}

// ✅ NEW (Minecraft 1.21.1+)
@Inject(method = "onGameMessage", at = @At("HEAD"))
private void onChatMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
    // overlay parameter removed in 1.21.1
    // ...
}
```

### 3. Thread Safety
```java
// ❌ BAD - unlimited threads
CompletableFuture.runAsync(() -> { ... });

// ✅ GOOD - fixed thread pool
private static final ExecutorService executor = Executors.newFixedThreadPool(2);
CompletableFuture.runAsync(() -> { ... }, executor)
    .orTimeout(30, TimeUnit.SECONDS);
```

### 3. Server vs Client
```java
// Check if on server
boolean isMultiplayer = !MinecraftClient.getInstance().isInSingleplayer();

// Adjust behavior accordingly
double maxAllowed = isMultiplayer ? MAX_SERVER : MAX_SINGLEPLAYER;
```

---

## 📦 PROJECT STRUCTURE

```
kashub/
├── src/main/java/kasperstudios/kashub/
│   ├── algorithm/
│   │   ├── commands/          # All commands here
│   │   ├── events/            # Event system
│   │   └── CommandRegistry.java
│   ├── debug/                 # Debug system
│   ├── api/                   # REST API & DTOs
│   ├── mixin/                 # Fabric mixins
│   └── util/                  # Utilities
├── docs/                      # User documentation (English)
└── config/kashub/             # Config files
```

---

## 📚 USEFUL REFERENCES

- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Minecraft Wiki](https://minecraft.wiki/)
- [Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol/)
- [ROADMAP_FUTURE.md](./ROADMAP_FUTURE.md) - Full roadmap

---

## 🔍 TESTING

### Before Committing
1. Run `./gradlew build -x test`
2. Check diagnostics with IDE
3. Test in-game if possible
4. Update CHANGELOG.md
5. Update documentation if needed

### Test Scripts
- `test_events.kh` - Event system
- `test_conditional_breakpoints.kh` - Breakpoints
- `test_profiler.kh` - Profiler

---

## 💡 TIPS FOR AI ASSISTANTS

1. **Always check existing code** before creating new files
2. **Use `getDiagnostics`** before finalizing changes
3. **Keep code minimal** - avoid verbose implementations
4. **Follow existing patterns** in the codebase
5. **Test critical changes** with build command
6. **Update documentation** when adding features
7. **Use English** for all user-facing text
8. **Add null checks** for Minecraft client/player
9. **Handle async operations** properly with timeouts
10. **Consider server compatibility** for multiplayer features
11. **Dont create any .md files**, only edit

## Roadmap Change Policy

⚠️ IMPORTANT: The roadmap and versions are considered the author's area of responsibility.

- Any changes to:
- `ROADMAP_FUTURE.md`
- `REFACTORING_PLAN.md`
- the "Next Steps", "Future Features", "Planned for vX.Y.Z" sections

**PROHIBITED:**
- moving tasks between versions (e.g., from v0.9.0 to v1.1.0+);
- changing the target version of a feature;
- marking a feature as *Moved*, *Deferred*, *Out of scope*.

- **ALLOWED without the author's consent:**
- correcting typos and formatting;
- update the completion status (check the box) if the feature is actually implemented in the code;
- add technical implementation details to existing items.

- **ANY CHANGES TO THE PLAN (transfer, defer, removal of features)**
must be made only by the project author.

### Custom Version Scheme (Project-Specific)

This project uses a **cyclic versioning scheme**: beta → hotfix → stable, then repeat.

```
MAJOR.MINOR.PATCH
```

**Version Cycle:**

1. **X.Y.0** → Beta release (new features, testing phase)
   - Example: `0.9.0` = beta for 0.9 line
   - All planned work goes here
   - May have bugs, breaking changes

2. **X.Y.1+** → Hotfixes for beta (quick fixes, small improvements)
   - Example: `0.9.1`, `0.9.2` = hotfixes for 0.9 beta
   - Bug fixes, critical issues
   - No new features

3. **X+1.0.0** → Stable release (major milestone)
   - Example: `1.0.0` = first stable release
   - Architecture complete, tested, production-ready
   - Breaking changes allowed (major version bump)

4. **X+1.Y.0** → Next beta cycle
   - Example: `1.1.0` = new beta with next feature set
   - Cycle repeats: beta → hotfix → stable

**Example Timeline:**
```
0.9.0 → beta (current)
0.9.1 → hotfix (if needed)
1.0.0 → stable (ScriptInterpreter removed, architecture complete)
1.1.0 → beta (Marketplace & Community features)
1.1.1 → hotfix (if needed)
2.0.0 → stable (next major milestone)
```

**Key Differences from Classic SemVer:**
- Beta versions use `.0` patch (not `-beta` suffix)
- Hotfixes increment patch for beta (not just for stable)
- Major version bump = stable milestone (not just breaking changes)
- Odd MINOR versions (0.9, 1.1, 1.3) = beta phases
- Even MAJOR versions (1.0, 2.0) = stable releases

**Rules:**
- Do NOT:
  - auto-upgrade `X.Y.0` to `X.Y.1` (only owner decides)
  - create `X.Y.2+` versions for internal phases
  - change version numbers without explicit approval
- Only the project owner decides:
  - when to release hotfixes (`X.Y.1+`)
  - when to move to stable (`X+1.0.0`)
  - when to start next beta cycle (`X+1.Y.0`)

### Versioning Rules (VERY IMPORTANT)

These rules apply to ALL versions in this project.

1. **One main release per minor line**
   - `A.B.0` is the **only main release** of the `A.B` line.
   - All work completed **before the actual `A.B.0` release** is considered part of `A.B.0`, not separate `A.B.1`, `A.B.2`, etc.
   - Do NOT invent intermediate patch versions before release just to label internal phases.

2. **Patch versions ONLY after release**
   - Versions like `A.B.1`, `A.B.2`, ... are used ONLY for updates made **after `A.B.0` has been released**.
   - Patch versions are for:
     - bug fixes
     - small improvements
     - minor, non-breaking changes.
   - Internal refactoring/feature phases are **NOT** separate versions.

3. **Phases for planning, not versions**
   - When planning work, use neutral phase names instead of new version numbers, for example:
     - `Phase A: Before A.B.0 release`
     - `Phase B: After A.B.0, towards A+1.0.0`
   - Do NOT:
     - invent new version numbers;
     - move features between versions on your own;
     - mark features as “moved to A.B+1.0” without explicit approval from the project owner.

4. **SemVer meaning (simplified for this project)**
   - `A.B.0` – major release for the `A.B` line:
     - new features
     - significant refactors
     - architectural changes (not necessarily breaking).
   - `A.B.x` (`x > 0`) – patch releases:
     - small updates after `A.B.0`
     - fixes, optimizations, minor improvements.
   - `A+1.0.0` – major milestone:
     - completion of key architectural work
     - may include **breaking changes** that cannot go into `A.B.x`.

5. **Roadmap and version ownership**
   - Only the project owner may:
     - move features between versions (`A.B.0` → `A+1.0.0`, etc.);
     - mark items as *Moved*, *Deferred*, *Out of scope*;
     - introduce new target versions.
   - Any assistants/tools are **NOT ALLOWED** to:
     - change target versions for features on their own;
     - move roadmap items to future releases without explicit approval from the project owner.

### File Edit Restrictions (CRITICAL)

The following files are **READ-ONLY** for assistants/tools:

- `DEVELOPMENT_NOTES.md`

Rules:

- Assistants/tools MUST NOT:
  - edit, overwrite, or reformat `DEVELOPMENT_NOTES.md`;
  - insert new sections into this file;
  - remove or change any existing rules in this file.

- Assistants/tools MAY:
  - read `DEVELOPMENT_NOTES.md` to understand project rules and guidelines.

Only the project owner is allowed to modify `DEVELOPMENT_NOTES.md`.

---

**Last Updated:** 2026-01-13  
**Version:** v0.9.0-beta
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

---

**Last Updated:** 2026-01-03  
**Version:** v0.8.0-beta
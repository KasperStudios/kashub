# Kashub v0.6.0 Beta - Hot-Reload & Autorun Update 🔥

## What's New?

This release focuses on **developer experience** and **automation**. Two major features that will change how you work with Kashub scripts!

---

## 🔥 Hot-Reload: Edit Scripts Without Stopping Them!

Never stop your scripts to edit them again! Hot-reload automatically detects file changes and reloads your scripts instantly.

### How it works:
1. Start your script in Kashub
2. Enable Hot-Reload in Settings
3. Open the script file in your favorite editor (VSCode, IntelliJ, etc.)
4. Make changes and save
5. Script automatically reloads with your changes!

### Example workflow:
```kashub
// Initial script
loop 100 {
    print "Mining..."
    mine
    walk forward 1
    wait 10
}

// You realize it's too slow, so you:
// 1. Alt+Tab to VSCode
// 2. Change "wait 10" to "wait 5"
// 3. Ctrl+S (save)
// 4. Alt+Tab back to game
// 5. Script already reloaded! Now it's faster!
```

### Technical details:
- **Dual monitoring**: WatchService + polling for maximum compatibility
- **Works with ANY text editor**: VSCode, IntelliJ, Notepad++, Sublime, etc.
- **Configurable check interval**: Default 1000ms (1 second)
- **Thread-safe reloading**: No crashes or race conditions
- **Error handling**: Broken scripts don't crash the game

---

## 🚀 Autorun: Scripts That Start Themselves!

Tired of manually starting your utility scripts every time you join a world? Autorun solves this!

### How it works:
1. Create your utility scripts (anti-AFK, auto-farm, etc.)
2. Add them to Autorun list in Task Manager
3. Enable Autorun in Settings
4. Next time you join a world - they start automatically!

### Example use cases:
- ✅ Anti-AFK script (keeps you active)
- ✅ Resource monitoring (alerts on low health/food)
- ✅ Auto-farming scripts
- ✅ Server-specific automation
- ✅ Custom HUD overlays

### Configuration:
```json
{
  "autorunEnabled": true,
  "autorunScripts": [
    "utility/anti_afk.kh",
    "automation/auto_farm.kh"
  ]
}
```

---

## 🛠️ Task Manager Improvements

Task Manager now has a tabbed interface like Windows Task Manager:

### Processes Tab:
- View all running scripts
- Pause/Resume/Stop controls
- Real-time status updates

### Autorun Tab (NEW!):
- Left panel: Scripts in autorun
- Right panel: Available scripts
- → button: Add to autorun
- ← button: Remove from autorun

---

## 🐛 Major Bug Fixes

- **If/Else If/Else Blocks** - Now correctly detects all blocks
- **While (true) Loops** - Fixed infinite loop execution
- **Environment Variables** - Real-time updates before condition evaluation
- **Example Scripts** - All examples now 100% working

---

## 🎯 Migration Guide

**No breaking changes!** Your existing scripts work as-is.

**To use new features:**
1. Open Settings → Enable "Hot-Reload"
2. Open Task Manager → Autorun tab → Add scripts
3. Done! 🎉

---

## ⚠️ Known Issues

- Hot-reload: 1-2s delay for external editors (by design)
- Windows: Rare file locks (working on fix)
- Large scripts (>1000 lines): Slower reload time

**These will be addressed in v0.6.1!**

---

## 📝 Full Changelog

See [CHANGELOG.md](CHANGELOG.md) for complete list of changes.

---

**Happy Scripting! 🚀**

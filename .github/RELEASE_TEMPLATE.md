# 🎮 Kashub v{VERSION}

## What's New

### 🛡️ CrashGuard Command
Protect your scripts from crashes, errors, and performance issues:
```javascript
crashguard(timeout=10000, minFps=25) {
    // Your risky code here
    let blocks = scanner.blocks("diamond_ore", 64)
}
```

### 🖥️ System Object
New global object with essential utilities:
```javascript
System.print("Hello!")      // Output to chat and console
System.log("Debug info")    // Console only
System.wait(1000)           // Sleep
System.memory()             // Memory usage
System.time()               // Current timestamp
```

### ✨ Other Improvements
- Enhanced syntax highlighting for object methods
- Improved validation for object-oriented commands
- Fixed loop stopping issues
- Better error detection and reporting
- Complete V2 API implementation

## Installation

1. Install [Fabric 1.21.1](https://fabricmc.net/use/)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download `kashub-{VERSION}.jar` below
4. Place in your `mods` folder
5. Launch Minecraft and press `K` to open the editor!

## Quick Start

```javascript
// Your first script
crashguard {
    System.print("Hello from Kashub!")
    
    let health = player.getHealth()
    System.print("Health: " + health)
    
    if (health < 10) {
        System.print("Low health! Be careful!")
    }
}
```

## Links

- 📚 [Full Documentation](https://github.com/KasperStudios/Kashub/tree/main/docs)
- 💬 [Discord Community](https://discord.gg/gFeWtpEKN9)
- 🐛 [Report Issues](https://github.com/KasperStudios/Kashub/issues)
- 🌐 [GitHub Repository](https://github.com/KasperStudios/Kashub)

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.16.0+
- Fabric API 0.100.0+

## Changelog

See [CHANGELOG.md](https://github.com/KasperStudios/Kashub/blob/main/CHANGELOG.md) for full changelog.

---

Made with ❤️ by KasperStudios

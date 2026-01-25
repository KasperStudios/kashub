# Installation Guide

This guide will walk you through installing Kashub and getting it running on your Minecraft client.

## Prerequisites

Before installing Kashub, make sure you have:

- **Minecraft Java Edition 1.21.1**
- **Fabric Loader 0.16.0 or higher**
- **Fabric API 0.100.0 or higher**
- **Java 21 or higher**

## Step 1: Install Fabric Loader

If you don't have Fabric installed yet:

1. Download the Fabric installer from [fabricmc.net](https://fabricmc.net/use/)
2. Run the installer and select Minecraft version **1.21.1**
3. Click "Install" and wait for it to complete
4. Launch Minecraft and select the "Fabric" profile

## Step 2: Install Fabric API

Fabric API is required for Kashub to work:

1. Download Fabric API from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Make sure to download the version for **Minecraft 1.21.1**
3. Place the downloaded `.jar` file in your `mods` folder

**Mods folder location:**
- Windows: `%APPDATA%\.minecraft\mods`
- macOS: `~/Library/Application Support/minecraft/mods`
- Linux: `~/.minecraft/mods`

## Step 3: Install Kashub

1. Download the latest Kashub release from:
   - [Modrinth](https://modrinth.com/mod/kashub)
   - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kashub)
   - [GitHub Releases](https://github.com/KasperStudios/Kashub/releases)

2. Place the `kashub-x.x.x.jar` file in your `mods` folder

3. Launch Minecraft with the Fabric profile

## Step 4: Verify Installation

1. Launch Minecraft
2. Press `K` to open the Kashub editor
3. If the editor opens, installation was successful!

You should see:
- A code editor with syntax highlighting
- A script browser on the left
- Example scripts in the list

## First Launch

On first launch, Kashub will:
- Create a config folder at `config/kashub/`
- Generate a default configuration file
- Create a scripts folder for your custom scripts
- Load example scripts from the mod resources

## Configuration

The main config file is located at `config/kashub/config.json`:

```json
{
  "editorTheme": "dracula",
  "editorFontSize": 12,
  "sandboxMode": true,
  "allowCheats": false,
  "maxLoopIterations": 10000,
  "enableDebugger": true,
  "enableProfiler": true
}
```

### Configuration Options

- **editorTheme** - Editor color theme (dracula, onedark, tokyonight, etc.)
- **editorFontSize** - Font size for the editor (8-24)
- **sandboxMode** - Enable safety restrictions
- **allowCheats** - Allow cheat-like commands (single-player only)
- **maxLoopIterations** - Maximum loop iterations before timeout
- **enableDebugger** - Enable debugging features
- **enableProfiler** - Enable performance profiling

## Troubleshooting

### Editor doesn't open when pressing K

1. Check that Kashub is in your mods folder
2. Verify Fabric API is installed
3. Check the Minecraft logs for errors
4. Try rebinding the key in Minecraft settings

### Scripts won't run

1. Check the script console for error messages
2. Verify your script syntax is correct
3. Make sure you're not in a restricted server environment
4. Check if Fair Play mode is blocking certain actions

### Performance issues

1. Reduce `maxLoopIterations` in config
2. Use CrashGuard with FPS monitoring
3. Avoid infinite loops without delays
4. Close unused scripts in the task manager

### Mod conflicts

Kashub is generally compatible with most mods, but conflicts may occur with:
- Other scripting/automation mods
- Mods that modify the HUD extensively
- Mods that override keybindings

If you experience conflicts:
1. Try removing other automation mods
2. Rebind conflicting keys
3. Report the issue on GitHub

## Uninstallation

To remove Kashub:

1. Delete `kashub-x.x.x.jar` from your mods folder
2. (Optional) Delete the `config/kashub/` folder to remove all settings
3. (Optional) Delete the `logs/kashub/` folder to remove logs

## Next Steps

Now that Kashub is installed:

1. Read the [Quick Start Guide](quick-start.md) to write your first script
2. Explore the [Editor Guide](editor-guide.md) to learn the editor features
3. Check out [Example Scripts](examples/README.md) for inspiration

---

**Need help?** Join our [Discord Community](https://discord.gg/gFeWtpEKN9)

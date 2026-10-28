# Installation Guide

This guide will walk you through installing Kashub and getting it running on your Minecraft client.

## Prerequisites

Before installing Kashub, make sure you have:

- **Minecraft Java Edition 1.21.1**
- **Java 21 or higher**
- One of the following mod loaders:
  - **Fabric Loader 0.16.0+** (with Fabric API 0.100.0+)
  - **NeoForge 21.1.x+**

## Step 1: Install Mod Loader

### Fabric
1. Download the Fabric installer from [fabricmc.net](https://fabricmc.net/use/)
2. Run the installer and select Minecraft version **1.21.1**
3. Install **Fabric API** into your `mods` folder.

### NeoForge
1. Download the NeoForge installer from [neoforged.net](https://neoforged.net/)
2. Run the installer and select the target version.

## Step 2: Install Kashub

1. Download the latest Kashub release for your loader from [Modrinth](https://modrinth.com/mod/kashub) or [GitHub](https://github.com/KasperStudios/Kashub/releases).
2. Place the `kashub-x.x.x.jar` file in your `mods` folder.
3. Launch Minecraft.

## Step 3: Getting Started In-Game

Unlike previous versions, Kashub is now a hardware-driven system.

1. **Obtain an Implant**: In creative mode, search for "Neural Implant" in the search tab. In survival, craft a Standard Neural Implant.
2. **Equip the Implant**: Open your inventory and place the implant into the dedicated **Neural Implant Slot**.
3. **Open the Editor**: Press `K` (default) to open the professional IDE.

## Step 4: Verify Installation

If the editor opens and you see the `System`, `player`, and `Terminal` objects in the documentation panel, installation was successful!

## Configuration

The main config file is located at `config/kashub/config.json`:

```json
{
  "editorTheme": "dracula",
  "editorFontSize": 12,
  "allowCCTBridge": true,
  "allowInventoryControl": true,
  "allowDangerousCommands": false,
  "sandboxMode": true
}
```

### Key Options
- **allowCCTBridge**: Enables integration with ComputerCraft: Tweaked.
- **allowDangerousCommands**: Allows use of `rm -rf` and other high-risk commands.
- **sandboxMode**: Enforces strict API limits to prevent server-side detection.

---

**Need help?** Join our [Discord Community](https://discord.gg/gFeWtpEKN9)

# 🧠 Neural Implant & Peripheral Scanning

The Neural Implant is the core piece of hardware that enables the Kashub OS and scripting capabilities. Unlike previous versions where the system was always active, Kashub v1.0.0-FSR requires a physical **Neural Implant** item to be equipped in your inventory.

## 🕹️ The Implant Slot

Kashub adds a dedicated **Neural Implant Slot** to the player's inventory screen. 

### How to Access
1. Open your inventory (`E`).
2. Look for the custom slot with a brain/chip icon (typically near the armor slots).
3. Place a **Neural Implant** item into this slot to boot the Kashub system.

### Why use a dedicated slot?
- **Equip-on-Use**: The system only boots when the hardware is physically connected.
- **Tiers**: Higher-tier implants (Creative, Elite, Standard) provide more features like increased scanning range, faster execution, or access to the CC:T bridge.
- **Immersive Feedback**: If the implant is removed, scripts will automatically pause and the IDE will enter "Offline Mode".

## 🔍 Peripheral Scanning

One of the most powerful features of the Neural Implant is the **Deep Scan** utility. This allows you to discover and interact with blocks that have advanced functionality (peripherals).

### How to Scan
1. Look at a functional block (e.g., a Chest, Furnace, or a ComputerCraft Computer).
2. Hold the **Left Alt** key (default).
3. You will see a scanning animation and sound.
4. Once completed, the block is added to your **Compendium**.

### Scanning Tiers
- **Standard Implant**: 5-block reach, basic block data.
- **Elite Implant**: 15-block reach, reveals NBT data and hidden methods.
- **Creative Implant**: Infinite reach, instant scanning.

## 🎯 Scanned Peripherals Compendium

Once a block is scanned, it is registered in your internal database. This unlocks:
1. **IDE Documentation**: The specific methods for that block type appear in your Documentation panel.
2. **Scripting Access**: You can now target that block using the `scanner` or `cc` API objects.
3. **Remote Control**: For CC:T computers, scanning is required before you can establish a "Neural Link".

## 🛠️ Scripting with Scanning

You can query your scanned peripherals directly in KHScript:

```javascript
// Get all scanned chests within 10 blocks
let chests = scanner.blocks("chest", 10);

// Print the inventory status of the nearest one
if (chests.length() > 0) {
    let nearest = chests.get(0);
    System.print("Chest at " + nearest.pos + " contains " + nearest.itemCount + " items.");
}
```

---

> [!TIP]
> Always keep your Neural Implant updated to the highest tier available to maximize your automation reach and scanning depth.

# Inventory Object API

The `inventory` object provides methods to manage the player inventory and equipment.

## Methods

### inventory.count(searchString)

Counts items in the inventory whose identifier contains the specified search string.

**Parameters:**
- `searchString` (string): Part of item ID to count (e.g., `"diamond"`, `"ore"`, `"sword"`).

**Returns:** Number (total count of all matching items).

**Example:**
```javascript
let totalDiamonds = inventory.count("diamond")
System.print("Total items containing 'diamond': " + totalDiamonds)
```

### inventory.getItems()

Returns a list of all non-empty stack in the player's inventory.

**Returns:** Array of item objects.

**Item Object Properties:**
- `slot` (number): Inventory slot index (0-45).
- `id` (string): Item identifier (e.g., `"minecraft:stone"`).
- `count` (number): Stack size.
- `name` (string): Display name.

**Example:**
```javascript
let items = inventory.getItems()
for (let i = 0; i < items.length(); i++) {
    System.print("Slot " + items[i].slot + ": " + items[i].name)
}
```

### inventory.getEmptySlots()

Returns the number of empty slots in the main inventory part (slots 0-35).

**Returns:** Number

### inventory.drop(slot, [dropAll])

Drops the item stack at the specified slot.

**Parameters:**
- `slot` (number): Slot index.
- `dropAll` (boolean, optional): If `true`, drops the whole stack. If `false`, drops a single item. Defaults to `false`.

**Returns:** `true`

**Example:**
```javascript
inventory.drop(9, true) // Drop everything in slot 9
```

### inventory.swap(slot1, slot2)

Swaps the item stacks between two inventory slots.

**Parameters:**
- `slot1`, `slot2` (number): Slot indices to swap.

**Returns:** `true`

**Example:**
```javascript
inventory.swap(0, 9) // Move item from hotbar 0 to slot 9
```

### inventory.equip(slot)

Equips an item from the specified slot into the appropriate armor slot or offhand (simulates Shift-Click).

**Parameters:**
- `slot` (number): Slot index.

**Returns:** `true`

**Example:**
```javascript
inventory.equip(9) // Automatically wear armor from slot 9
```

### inventory.use(itemName)

Finds an item with the specified name in the inventory and uses it (simulates Right-Click).

**Parameters:**
- `itemName` (string): Item ID or part of it to use (e.g., `"cooked_beef"`, `"golden_apple"`).

**Returns:** `true` if item was found and used, `false` otherwise.

**Example:**
```javascript
if (player.getHealth() < 10) {
    inventory.use("golden_apple")
}
```

## See Also

- [Player Object](player-object.md)
- [Tag Object](tag-object.md)

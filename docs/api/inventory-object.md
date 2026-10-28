# 🎒 Inventory Object

The `inventory` object manages the player's items, armor, crafting, and interactions with external containers like chests.

## Methods

### `inventory.count(itemName)`
Returns the total number of items matching the name in the player's inventory.
- **Returns:** `number`

### `inventory.getItems()`
Returns a list of all item stacks currently in the inventory.
- **Returns:** List of `{slot: int, id: string, count: int, name: string, data: map}`

### `inventory.getEmptySlots()`
Returns the count of empty slots in the main inventory (0-35).
- **Returns:** `number`

### `inventory.drop(slot, [dropAll])`
Drops items from a specific slot.
- **Parameters:** `slot` (int), `dropAll` (optional boolean, default: false)
- **Returns:** `boolean`

### `inventory.swap(slot1, slot2)`
Swaps the contents of two inventory slots.
- **Returns:** `boolean`

### `inventory.equip(slot)`
Quickly moves an item from the specified slot to an appropriate armor or offhand slot.
- **Returns:** `boolean`

### `inventory.use(itemName)`
Finds an item by name, selects it in the hotbar, and simulates a right-click.
- **Returns:** `boolean`

### `inventory.craft(itemName, count)`
Attempts to craft the specified amount of an item using the AutoCraft service.
- **Returns:** `boolean`

### `inventory.check()`
Returns a summary of the inventory status.
- **Returns:** `{totalItems: int, emptySlots: int, usedSlots: int}`

### `inventory.find(itemName)`
Finds the first slot containing an item matching the search string.
- **Returns:** `int` (slot index or -1 if not found)

### `inventory.isContainerOpen()`
Checks if the player currently has an external container (e.g., chest) open.
- **Returns:** `boolean`

### `inventory.containerTransferAll()`
Quickly moves all items from an open container into the player's inventory.
- **Returns:** `boolean`

### `inventory.closeContainer()`
Closes the currently open container GUI.
- **Returns:** `boolean`

### `inventory.has(itemName)`
Checks if the player has at least one of the specified items.
- **Returns:** `boolean`

### `inventory.offhand(itemName)`
Finds the item and moves it to the offhand (shield) slot.
- **Returns:** `boolean`

### `inventory.select(itemName)`
Selects the specified item in the hotbar, swapping it from the main inventory if necessary.
- **Returns:** `boolean`

---

## Example: Auto-Eating
```javascript
if (player.getHunger() < 15) {
    if (inventory.has("cooked_beef")) {
        inventory.use("cooked_beef");
    } else if (inventory.has("apple")) {
        inventory.use("apple");
    }
}
```

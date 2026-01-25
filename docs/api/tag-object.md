# Tag Object API

The `tag` object helps working with Minecraft item and block tags (categories).

## Methods

### tag.itemHas(itemId, tagId)

Check if an item has a specific tag.

**Parameters:**
- `itemId` (string): Item identifier (e.g., `"diamond"`, `"oak_log"`)
- `tagId` (string): Tag identifier (e.g., `"minecraft:logs"`)

**Returns:** `true` if item has the tag, `false` otherwise

**Example:**
```javascript
if (tag.itemHas("oak_log", "minecraft:logs")) {
    System.print("It's a log!")
}
```

### tag.itemsWith(tagId)

Get a list of all items that have a specific tag.

**Parameters:**
- `tagId` (string): Tag identifier

**Returns:** List of strings (item IDs)

**Example:**
```javascript
let wools = tag.itemsWith("minecraft:wool")
System.print("Found " + wools.length() + " types of wool")
```

### tag.blockHas(blockId, tagId)

Check if a block has a specific tag.

**Parameters:**
- `blockId` (string): Block identifier (e.g., `"stone"`)
- `tagId` (string): Tag identifier

**Returns:** `true` if block has the tag, `false` otherwise

**Example:**
```javascript
if (tag.blockHas("stone", "minecraft:base_stone_overworld")) {
    System.print("Base stone detected")
}
```

## See Also

- [Inventory Object](inventory-object.md)
- [World Object](world-object.md)

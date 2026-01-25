package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * TagManager - Work with item/block tags from other mods.
 * 
 * Allows scripts to check if items/blocks belong to specific tags,
 * enabling compatibility with mod-added content.
 * 
 * Part of v0.9.0 Universal Integration System feature.
 * 
 * @since 0.9.0
 */
public class TagManager {
    
    private static volatile TagManager instance;
    private static final Object LOCK = new Object();
    
    private TagManager() {
    }
    
    public static TagManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new TagManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Check if item has a tag.
     * 
     * @param itemId Item identifier (e.g., "minecraft:diamond")
     * @param tagId Tag identifier (e.g., "c:gems")
     * @return true if item has the tag
     */
    public boolean itemHasTag(String itemId, String tagId) {
        try {
            Identifier itemIdentifier = Identifier.tryParse(itemId);
            if (itemIdentifier == null) {
                itemIdentifier = Identifier.of("minecraft", itemId);
            }
            
            Item item = Registries.ITEM.get(itemIdentifier);
            if (item == null) {
                return false;
            }
            
            Identifier tagIdentifier = Identifier.tryParse(tagId);
            if (tagIdentifier == null) {
                return false;
            }
            
            TagKey<Item> tag = TagKey.of(Registries.ITEM.getKey(), tagIdentifier);
            return item.getRegistryEntry().isIn(tag);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to check item tag: " + itemId + " -> " + tagId, e);
            return false;
        }
    }
    
    /**
     * Check if block has a tag.
     * 
     * @param blockId Block identifier (e.g., "minecraft:stone")
     * @param tagId Tag identifier (e.g., "minecraft:mineable/pickaxe")
     * @return true if block has the tag
     */
    public boolean blockHasTag(String blockId, String tagId) {
        try {
            Identifier blockIdentifier = Identifier.tryParse(blockId);
            if (blockIdentifier == null) {
                blockIdentifier = Identifier.of("minecraft", blockId);
            }
            
            Block block = Registries.BLOCK.get(blockIdentifier);
            if (block == null) {
                return false;
            }
            
            Identifier tagIdentifier = Identifier.tryParse(tagId);
            if (tagIdentifier == null) {
                return false;
            }
            
            TagKey<Block> tag = TagKey.of(Registries.BLOCK.getKey(), tagIdentifier);
            return block.getRegistryEntry().isIn(tag);
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to check block tag: " + blockId + " -> " + tagId, e);
            return false;
        }
    }
    
    /**
     * Get all items with a specific tag.
     * 
     * @param tagId Tag identifier
     * @return List of item identifiers
     */
    public List<String> getItemsWithTag(String tagId) {
        List<String> items = new ArrayList<>();
        
        try {
            Identifier tagIdentifier = Identifier.tryParse(tagId);
            if (tagIdentifier == null) {
                return items;
            }
            
            TagKey<Item> tag = TagKey.of(Registries.ITEM.getKey(), tagIdentifier);
            
            Registries.ITEM.stream()
                .filter(item -> item.getRegistryEntry().isIn(tag))
                .forEach(item -> {
                    Identifier id = Registries.ITEM.getId(item);
                    if (id != null) {
                        items.add(id.toString());
                    }
                });
                
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to get items with tag: " + tagId, e);
        }
        
        return items;
    }
    
    /**
     * Get all blocks with a specific tag.
     * 
     * @param tagId Tag identifier
     * @return List of block identifiers
     */
    public List<String> getBlocksWithTag(String tagId) {
        List<String> blocks = new ArrayList<>();
        
        try {
            Identifier tagIdentifier = Identifier.tryParse(tagId);
            if (tagIdentifier == null) {
                return blocks;
            }
            
            TagKey<Block> tag = TagKey.of(Registries.BLOCK.getKey(), tagIdentifier);
            
            Registries.BLOCK.stream()
                .filter(block -> block.getRegistryEntry().isIn(tag))
                .forEach(block -> {
                    Identifier id = Registries.BLOCK.getId(block);
                    if (id != null) {
                        blocks.add(id.toString());
                    }
                });
                
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to get blocks with tag: " + tagId, e);
        }
        
        return blocks;
    }
}

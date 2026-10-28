package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;

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
            ResourceLocation itemIdentifier = ResourceLocation.tryParse(itemId);
            if (itemIdentifier == null) {
                itemIdentifier = ResourceLocation.fromNamespaceAndPath("minecraft", itemId);
            }
            
            Item item = BuiltInRegistries.ITEM.get(itemIdentifier);
            if (item == null) {
                return false;
            }
            
            ResourceLocation tagIdentifier = ResourceLocation.tryParse(tagId);
            if (tagIdentifier == null) {
                return false;
            }
            
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagIdentifier);
            return item.builtInRegistryHolder().is(tag);
            
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
            ResourceLocation blockIdentifier = ResourceLocation.tryParse(blockId);
            if (blockIdentifier == null) {
                blockIdentifier = ResourceLocation.fromNamespaceAndPath("minecraft", blockId);
            }
            
            Block block = BuiltInRegistries.BLOCK.get(blockIdentifier);
            if (block == null) {
                return false;
            }
            
            ResourceLocation tagIdentifier = ResourceLocation.tryParse(tagId);
            if (tagIdentifier == null) {
                return false;
            }
            
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagIdentifier);
            return block.builtInRegistryHolder().is(tag);
            
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
            ResourceLocation tagIdentifier = ResourceLocation.tryParse(tagId);
            if (tagIdentifier == null) {
                return items;
            }
            
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), tagIdentifier);
            
            BuiltInRegistries.ITEM.stream()
                .filter(item -> item.builtInRegistryHolder().is(tag))
                .forEach(item -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
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
            ResourceLocation tagIdentifier = ResourceLocation.tryParse(tagId);
            if (tagIdentifier == null) {
                return blocks;
            }
            
            TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), tagIdentifier);
            
            BuiltInRegistries.BLOCK.stream()
                .filter(block -> block.builtInRegistryHolder().is(tag))
                .forEach(block -> {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
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

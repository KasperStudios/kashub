package kasperstudios.kashub.services.modpack;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CustomCraftingManager - Manages custom crafting recipes defined by scripts.
 * 
 * Allows modpack scripts to define custom crafting recipes that work
 * alongside vanilla and mod recipes.
 * 
 * Part of v0.9.0 Server & Modpack Scripts feature.
 * 
 * @since 0.9.0
 */
public class CustomCraftingManager {
    
    private static volatile CustomCraftingManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, CustomRecipe> recipes;
    
    private CustomCraftingManager() {
        this.recipes = new ConcurrentHashMap<>();
    }
    
    public static CustomCraftingManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CustomCraftingManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a shaped crafting recipe.
     * 
     * @param recipeId Unique recipe identifier
     * @param pattern 3x3 crafting pattern (use ' ' for empty slots)
     * @param ingredients Map of pattern characters to item identifiers
     * @param result Result item identifier
     * @param resultCount Result item count
     * @return true if recipe was registered successfully
     */
    public boolean registerShapedRecipe(String recipeId, String[] pattern, 
                                       Map<Character, String> ingredients,
                                       String result, int resultCount) {
        try {
            if (pattern.length != 3) {
                ScriptLogger.getInstance().error("Shaped recipe pattern must have 3 rows");
                return false;
            }
            
            // Validate result item
            Item resultItem = getItemFromId(result);
            if (resultItem == null) {
                ScriptLogger.getInstance().error("Invalid result item: " + result);
                return false;
            }
            
            // Validate ingredient items
            Map<Character, Item> itemIngredients = new HashMap<>();
            for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
                Item item = getItemFromId(entry.getValue());
                if (item == null) {
                    ScriptLogger.getInstance().error("Invalid ingredient item: " + entry.getValue());
                    return false;
                }
                itemIngredients.put(entry.getKey(), item);
            }
            
            CustomRecipe recipe = new CustomRecipe(
                recipeId,
                RecipeType.SHAPED,
                pattern,
                itemIngredients,
                new ItemStack(resultItem, resultCount)
            );
            
            recipes.put(recipeId, recipe);
            ScriptLogger.getInstance().info("Registered shaped recipe: " + recipeId);
            return true;
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to register shaped recipe: " + recipeId, e);
            return false;
        }
    }
    
    /**
     * Register a shapeless crafting recipe.
     * 
     * @param recipeId Unique recipe identifier
     * @param ingredients List of ingredient item identifiers
     * @param result Result item identifier
     * @param resultCount Result item count
     * @return true if recipe was registered successfully
     */
    public boolean registerShapelessRecipe(String recipeId, List<String> ingredients,
                                          String result, int resultCount) {
        try {
            // Validate result item
            Item resultItem = getItemFromId(result);
            if (resultItem == null) {
                ScriptLogger.getInstance().error("Invalid result item: " + result);
                return false;
            }
            
            // Validate ingredient items
            List<Item> itemIngredients = new ArrayList<>();
            for (String ingredientId : ingredients) {
                Item item = getItemFromId(ingredientId);
                if (item == null) {
                    ScriptLogger.getInstance().error("Invalid ingredient item: " + ingredientId);
                    return false;
                }
                itemIngredients.add(item);
            }
            
            CustomRecipe recipe = new CustomRecipe(
                recipeId,
                RecipeType.SHAPELESS,
                itemIngredients,
                new ItemStack(resultItem, resultCount)
            );
            
            recipes.put(recipeId, recipe);
            ScriptLogger.getInstance().info("Registered shapeless recipe: " + recipeId);
            return true;
            
        } catch (Exception e) {
            Kashub.LOGGER.error("Failed to register shapeless recipe: " + recipeId, e);
            return false;
        }
    }
    
    /**
     * Unregister a custom recipe.
     */
    public boolean unregisterRecipe(String recipeId) {
        CustomRecipe removed = recipes.remove(recipeId);
        if (removed != null) {
            ScriptLogger.getInstance().info("Unregistered recipe: " + recipeId);
            return true;
        }
        return false;
    }
    
    /**
     * Get a custom recipe by ID.
     */
    public CustomRecipe getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }
    
    /**
     * Get all registered custom recipes.
     */
    public Collection<CustomRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }
    
    /**
     * Clear all custom recipes.
     */
    public void clearAllRecipes() {
        recipes.clear();
        ScriptLogger.getInstance().info("Cleared all custom recipes");
    }
    
    /**
     * Match a custom recipe against a crafting inventory.
     * 
     * @param inventory The crafting inventory (3x3 grid)
     * @return The result ItemStack if a recipe matches, or ItemStack.EMPTY
     */
    public ItemStack matchRecipe(net.minecraft.inventory.RecipeInputInventory inventory) {
        // Try shaped recipes first
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.getType() == RecipeType.SHAPED) {
                if (matchesShapedRecipe(recipe, inventory)) {
                    return recipe.getResult().copy();
                }
            }
        }
        
        // Try shapeless recipes
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.getType() == RecipeType.SHAPELESS) {
                if (matchesShapelessRecipe(recipe, inventory)) {
                    return recipe.getResult().copy();
                }
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Check if a shaped recipe matches the crafting grid.
     */
    private boolean matchesShapedRecipe(CustomRecipe recipe, net.minecraft.inventory.RecipeInputInventory inventory) {
        String[] pattern = recipe.getPattern();
        Map<Character, Item> ingredients = recipe.getShapedIngredients();
        
        if (pattern == null || ingredients == null) {
            return false;
        }
        
        // Check 3x3 pattern
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                ItemStack stackInSlot = inventory.getStack(slotIndex);
                
                char patternChar = pattern[row].length() > col ? pattern[row].charAt(col) : ' ';
                
                if (patternChar == ' ') {
                    // Empty slot expected
                    if (!stackInSlot.isEmpty()) {
                        return false;
                    }
                } else {
                    // Item expected
                    Item expectedItem = ingredients.get(patternChar);
                    if (expectedItem == null) {
                        return false;
                    }
                    
                    if (stackInSlot.isEmpty() || stackInSlot.getItem() != expectedItem) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Check if a shapeless recipe matches the crafting grid.
     */
    private boolean matchesShapelessRecipe(CustomRecipe recipe, net.minecraft.inventory.RecipeInputInventory inventory) {
        List<Item> requiredItems = recipe.getShapelessIngredients();
        
        if (requiredItems == null) {
            return false;
        }
        
        // Count items in inventory
        Map<Item, Integer> inventoryItems = new HashMap<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                inventoryItems.put(stack.getItem(), inventoryItems.getOrDefault(stack.getItem(), 0) + 1);
            }
        }
        
        // Count required items
        Map<Item, Integer> requiredCounts = new HashMap<>();
        for (Item item : requiredItems) {
            requiredCounts.put(item, requiredCounts.getOrDefault(item, 0) + 1);
        }
        
        // Check if inventory has exactly the required items
        if (inventoryItems.size() != requiredCounts.size()) {
            return false;
        }
        
        for (Map.Entry<Item, Integer> entry : requiredCounts.entrySet()) {
            Integer inventoryCount = inventoryItems.get(entry.getKey());
            if (inventoryCount == null || !inventoryCount.equals(entry.getValue())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get item from identifier string.
     */
    private Item getItemFromId(String itemId) {
        try {
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) {
                // Try with minecraft namespace
                id = Identifier.of("minecraft", itemId);
            }
            return Registries.ITEM.get(id);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Recipe type enum.
     */
    public enum RecipeType {
        SHAPED,
        SHAPELESS
    }
    
    /**
     * Custom recipe class.
     */
    public static class CustomRecipe {
        private final String id;
        private final RecipeType type;
        private final String[] pattern; // For shaped recipes
        private final Map<Character, Item> shapedIngredients; // For shaped recipes
        private final List<Item> shapelessIngredients; // For shapeless recipes
        private final ItemStack result;
        
        // Shaped recipe constructor
        public CustomRecipe(String id, RecipeType type, String[] pattern,
                          Map<Character, Item> ingredients, ItemStack result) {
            this.id = id;
            this.type = type;
            this.pattern = pattern;
            this.shapedIngredients = ingredients;
            this.shapelessIngredients = null;
            this.result = result;
        }
        
        // Shapeless recipe constructor
        public CustomRecipe(String id, RecipeType type, List<Item> ingredients, ItemStack result) {
            this.id = id;
            this.type = type;
            this.pattern = null;
            this.shapedIngredients = null;
            this.shapelessIngredients = ingredients;
            this.result = result;
        }
        
        public String getId() {
            return id;
        }
        
        public RecipeType getType() {
            return type;
        }
        
        public String[] getPattern() {
            return pattern;
        }
        
        public Map<Character, Item> getShapedIngredients() {
            return shapedIngredients;
        }
        
        public List<Item> getShapelessIngredients() {
            return shapelessIngredients;
        }
        
        public ItemStack getResult() {
            return result;
        }
    }
}

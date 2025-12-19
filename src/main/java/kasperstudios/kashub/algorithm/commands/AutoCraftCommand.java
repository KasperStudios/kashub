package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Команда авто-крафтинга
 * 
 * Синтаксис:
 *   autoCraft recipe <item> [count] - скрафтить предмет
 *   autoCraft check <item> - проверить возможность крафта
 *   autoCraft list - показать доступные рецепты
 *   autoCraft missing <item> - показать недостающие ресурсы
 *   autoCraft stop - остановить крафт
 */
public class AutoCraftCommand implements Command {
    
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean isCrafting = new AtomicBoolean(false);
    private static CompletableFuture<Void> currentTask = null;
    
    // Кэш рецептов
    private static Map<String, RecipeEntry<?>> recipeCache = new HashMap<>();
    private static boolean recipeCacheInitialized = false;
    
    @Override
    public String getName() {
        return "autoCraft";
    }
    
    @Override
    public String getDescription() {
        return "Automated crafting of items";
    }
    
    @Override
    public String getParameters() {
        return "recipe|check|list|missing|stop <args>";
    }

    @Override
    public String getCategory() {
        return "Automation";
    }

    @Override
    public String getDetailedHelp() {
        return "Automated crafting of items.\n\n" +
               "Usage:\n" +
               "  autoCraft recipe <item> [count]\n" +
               "  autoCraft check <item>\n" +
               "  autoCraft list [filter]\n" +
               "  autoCraft missing <item>\n" +
               "  autoCraft stop\n\n" +
               "Actions:\n" +
               "  recipe  - Craft specified item\n" +
               "  check   - Check if item can be crafted\n" +
               "  list    - List craftable items\n" +
               "  missing - Show missing resources\n" +
               "  stop    - Stop crafting\n\n" +
               "Examples:\n" +
               "  autoCraft recipe stick 64\n" +
               "  autoCraft recipe diamond_pickaxe\n" +
               "  autoCraft check iron_sword\n" +
               "  autoCraft list sword\n" +
               "  autoCraft missing diamond_chestplate\n" +
               "  autoCraft stop\n\n" +
               "Variables set:\n" +
               "  $autoCraft_success     - true/false\n" +
               "  $autoCraft_canCraft    - Can craft item\n" +
               "  $autoCraft_maxCraftable - Max craftable count\n" +
               "  $autoCraft_needsTable  - Requires crafting table\n" +
               "  $autoCraft_crafted     - Items crafted\n" +
               "  $autoCraft_active      - Is crafting active\n" +
               "  $autoCraft_progress    - Current progress\n\n" +
               "Notes:\n" +
               "  - 3x3 recipes require open crafting table\n" +
               "  - Uses recipe book for quick crafting\n" +
               "  - Partial names work (e.g., 'pick' for pickaxe)";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        // Инициализируем кэш рецептов при первом использовании
        if (!recipeCacheInitialized) {
            initializeRecipeCache(client);
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "recipe":
                handleRecipe(player, args, interpreter);
                break;
            case "check":
                handleCheck(player, args, interpreter);
                break;
            case "list":
                handleList(player, args, interpreter);
                break;
            case "missing":
                handleMissing(player, args, interpreter);
                break;
            case "stop":
                handleStop(interpreter);
                break;
            default:
                printHelp();
        }
    }
    
    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        MinecraftClient.getInstance().execute(() -> {
            try {
                execute(args);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    private void initializeRecipeCache(MinecraftClient client) {
        if (client.world == null) return;
        
        RecipeManager recipeManager = client.world.getRecipeManager();
        
        // Получаем все рецепты крафта
        for (RecipeEntry<?> entry : recipeManager.values()) {
            Recipe<?> recipe = entry.value();
            if (recipe instanceof CraftingRecipe) {
                ItemStack result = recipe.getResult(client.world.getRegistryManager());
                String itemName = Registries.ITEM.getId(result.getItem()).getPath();
                recipeCache.put(itemName, entry);
            }
        }
        
        recipeCacheInitialized = true;
        System.out.println("Loaded " + recipeCache.size() + " crafting recipes");
    }
    
    private void handleRecipe(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Usage: autoCraft recipe <item> [count]");
            return;
        }
        
        String itemName = args[1].toLowerCase();
        int count = 1;
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }
        
        // Ищем рецепт
        RecipeEntry<?> recipeEntry = findRecipe(itemName);
        if (recipeEntry == null) {
            System.out.println("Recipe not found for: " + itemName);
            interpreter.setVariable("autoCraft_success", "false");
            interpreter.setVariable("autoCraft_error", "recipe_not_found");
            return;
        }
        
        Recipe<?> recipe = recipeEntry.value();
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Проверяем, можем ли крафтить
        CraftabilityResult result = checkCraftability(player, recipe, count);
        
        if (!result.canCraft) {
            System.out.println("Cannot craft " + itemName + ": " + result.reason);
            interpreter.setVariable("autoCraft_success", "false");
            interpreter.setVariable("autoCraft_error", result.reason);
            return;
        }
        
        // Определяем тип крафта (2x2 или 3x3)
        boolean needs3x3 = needsCraftingTable(recipe);
        
        if (needs3x3 && !(player.currentScreenHandler instanceof CraftingScreenHandler)) {
            System.out.println("This recipe requires a crafting table. Open one first.");
            interpreter.setVariable("autoCraft_success", "false");
            interpreter.setVariable("autoCraft_error", "needs_crafting_table");
            return;
        }
        
        // Выполняем крафт
        isCrafting.set(true);
        interpreter.setVariable("autoCraft_active", "true");
        
        currentTask = performCrafting(player, recipe, count, interpreter);
    }
    
    private void handleCheck(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Usage: autoCraft check <item>");
            return;
        }
        
        String itemName = args[1].toLowerCase();
        RecipeEntry<?> recipeEntry = findRecipe(itemName);
        
        if (recipeEntry == null) {
            interpreter.setVariable("autoCraft_canCraft", "false");
            interpreter.setVariable("autoCraft_recipeExists", "false");
            System.out.println("No recipe found for: " + itemName);
            return;
        }
        
        Recipe<?> recipe = recipeEntry.value();
        CraftabilityResult result = checkCraftability(player, recipe, 1);
        
        interpreter.setVariable("autoCraft_recipeExists", "true");
        interpreter.setVariable("autoCraft_canCraft", String.valueOf(result.canCraft));
        interpreter.setVariable("autoCraft_maxCraftable", String.valueOf(result.maxCraftable));
        interpreter.setVariable("autoCraft_needsTable", String.valueOf(needsCraftingTable(recipe)));
        
        if (result.canCraft) {
            System.out.println("Can craft " + itemName + " (max: " + result.maxCraftable + ")");
        } else {
            System.out.println("Cannot craft " + itemName + ": " + result.reason);
        }
    }
    
    private void handleList(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        String filter = args.length >= 2 ? args[1].toLowerCase() : "";
        
        List<String> craftable = new ArrayList<>();
        
        for (Map.Entry<String, RecipeEntry<?>> entry : recipeCache.entrySet()) {
            String itemName = entry.getKey();
            if (!filter.isEmpty() && !itemName.contains(filter)) {
                continue;
            }
            
            Recipe<?> recipe = entry.getValue().value();
            CraftabilityResult result = checkCraftability(player, recipe, 1);
            
            if (result.canCraft) {
                craftable.add(itemName + " (x" + result.maxCraftable + ")");
            }
        }
        
        interpreter.setVariable("autoCraft_craftableCount", String.valueOf(craftable.size()));
        
        System.out.println("Craftable items" + (filter.isEmpty() ? "" : " matching '" + filter + "'") + ":");
        int shown = 0;
        for (String item : craftable) {
            System.out.println("  - " + item);
            if (++shown >= 20) {
                System.out.println("  ... and " + (craftable.size() - 20) + " more");
                break;
            }
        }
    }
    
    private void handleMissing(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            System.out.println("Usage: autoCraft missing <item>");
            return;
        }
        
        String itemName = args[1].toLowerCase();
        RecipeEntry<?> recipeEntry = findRecipe(itemName);
        
        if (recipeEntry == null) {
            System.out.println("No recipe found for: " + itemName);
            return;
        }
        
        Recipe<?> recipe = recipeEntry.value();
        Map<Item, Integer> required = getRequiredItems(recipe);
        Map<Item, Integer> missing = new HashMap<>();
        
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int have = countItemInInventory(player, entry.getKey());
            if (have < entry.getValue()) {
                missing.put(entry.getKey(), entry.getValue() - have);
            }
        }
        
        if (missing.isEmpty()) {
            System.out.println("You have all required items for " + itemName);
            interpreter.setVariable("autoCraft_hasMissing", "false");
        } else {
            System.out.println("Missing items for " + itemName + ":");
            int index = 0;
            for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
                String missingItem = Registries.ITEM.getId(entry.getKey()).getPath();
                System.out.println("  - " + missingItem + " x" + entry.getValue());
                interpreter.setVariable("autoCraft_missing_" + index + "_item", missingItem);
                interpreter.setVariable("autoCraft_missing_" + index + "_count", String.valueOf(entry.getValue()));
                index++;
            }
            interpreter.setVariable("autoCraft_hasMissing", "true");
            interpreter.setVariable("autoCraft_missingCount", String.valueOf(missing.size()));
        }
    }
    
    private void handleStop(ScriptInterpreter interpreter) {
        isCrafting.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
        interpreter.setVariable("autoCraft_active", "false");
        System.out.println("AutoCraft stopped");
    }
    
    private RecipeEntry<?> findRecipe(String itemName) {
        // Точное совпадение
        if (recipeCache.containsKey(itemName)) {
            return recipeCache.get(itemName);
        }
        
        // Частичное совпадение
        for (Map.Entry<String, RecipeEntry<?>> entry : recipeCache.entrySet()) {
            if (entry.getKey().contains(itemName)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    private CraftabilityResult checkCraftability(ClientPlayerEntity player, Recipe<?> recipe, int count) {
        Map<Item, Integer> required = getRequiredItems(recipe);
        int maxCraftable = Integer.MAX_VALUE;
        
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int have = countItemInInventory(player, entry.getKey());
            int canMake = have / entry.getValue();
            maxCraftable = Math.min(maxCraftable, canMake);
        }
        
        if (maxCraftable == 0) {
            return new CraftabilityResult(false, 0, "not_enough_resources");
        }
        
        if (maxCraftable < count) {
            return new CraftabilityResult(true, maxCraftable, "partial");
        }
        
        return new CraftabilityResult(true, maxCraftable, "ok");
    }
    
    private Map<Item, Integer> getRequiredItems(Recipe<?> recipe) {
        Map<Item, Integer> required = new HashMap<>();
        
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            
            ItemStack[] stacks = ingredient.getMatchingStacks();
            if (stacks.length > 0) {
                Item item = stacks[0].getItem();
                required.merge(item, 1, Integer::sum);
            }
        }
        
        return required;
    }
    
    private boolean needsCraftingTable(Recipe<?> recipe) {
        // Проверяем размер рецепта
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getWidth() > 2 || shaped.getHeight() > 2;
        }
        // Shapeless рецепты с более чем 4 ингредиентами требуют верстак
        return recipe.getIngredients().size() > 4;
    }
    
    private CompletableFuture<Void> performCrafting(ClientPlayerEntity player, Recipe<?> recipe, 
                                                     int count, ScriptInterpreter interpreter) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        final int[] remaining = {count};
        
        Runnable craftTask = new Runnable() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    if (!isCrafting.get() || remaining[0] <= 0) {
                        isCrafting.set(false);
                        interpreter.setVariable("autoCraft_active", "false");
                        interpreter.setVariable("autoCraft_crafted", String.valueOf(count - remaining[0]));
                        future.complete(null);
                        return;
                    }
                    
                    ClientPlayerEntity p = MinecraftClient.getInstance().player;
                    if (p == null) {
                        future.complete(null);
                        return;
                    }
                    
                    // Проверяем, можем ли ещё крафтить
                    CraftabilityResult result = checkCraftability(p, recipe, 1);
                    if (!result.canCraft) {
                        isCrafting.set(false);
                        interpreter.setVariable("autoCraft_active", "false");
                        interpreter.setVariable("autoCraft_crafted", String.valueOf(count - remaining[0]));
                        interpreter.setVariable("autoCraft_stoppedReason", result.reason);
                        System.out.println("Crafting stopped: " + result.reason);
                        future.complete(null);
                        return;
                    }
                    
                    // Выполняем один крафт
                    boolean success = executeSingleCraft(p, recipe);
                    
                    if (success) {
                        remaining[0]--;
                        interpreter.setVariable("autoCraft_progress", 
                            String.valueOf(count - remaining[0]) + "/" + count);
                    }
                    
                    if (remaining[0] > 0 && isCrafting.get()) {
                        scheduler.schedule(this, 200, TimeUnit.MILLISECONDS);
                    } else {
                        isCrafting.set(false);
                        interpreter.setVariable("autoCraft_active", "false");
                        interpreter.setVariable("autoCraft_crafted", String.valueOf(count - remaining[0]));
                        interpreter.setVariable("autoCraft_success", "true");
                        future.complete(null);
                    }
                });
            }
        };
        
        scheduler.schedule(craftTask, 0, TimeUnit.MILLISECONDS);
        return future;
    }
    
    private boolean executeSingleCraft(ClientPlayerEntity player, Recipe<?> recipe) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Используем рецептную книгу для крафта
        if (client.interactionManager != null && player.currentScreenHandler != null) {
            // Для простоты используем quick craft через recipe book
            // В реальной реализации нужно размещать предметы в сетке
            
            Identifier recipeId = null;
            for (Map.Entry<String, RecipeEntry<?>> entry : recipeCache.entrySet()) {
                if (entry.getValue().value() == recipe) {
                    recipeId = entry.getValue().id();
                    break;
                }
            }
            
            if (recipeId != null) {
                // Отправляем пакет крафта через recipe book
                client.interactionManager.clickRecipe(
                    player.currentScreenHandler.syncId,
                    (RecipeEntry<CraftingRecipe>) recipeCache.values().stream()
                        .filter(e -> e.value() == recipe)
                        .findFirst()
                        .orElse(null),
                    true // shift-click для быстрого крафта
                );
                return true;
            }
        }
        
        return false;
    }
    
    private int countItemInInventory(ClientPlayerEntity player, Item item) {
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private void printHelp() {
        System.out.println("AutoCraft Command:");
        System.out.println("  autoCraft recipe <item> [count]");
        System.out.println("    - Craft specified item");
        System.out.println("  autoCraft check <item>");
        System.out.println("    - Check if item can be crafted");
        System.out.println("  autoCraft list [filter]");
        System.out.println("    - List craftable items");
        System.out.println("  autoCraft missing <item>");
        System.out.println("    - Show missing resources for recipe");
        System.out.println("  autoCraft stop");
        System.out.println("    - Stop crafting");
    }
    
    public static boolean isActive() {
        return isCrafting.get();
    }
    
    public static void stop() {
        isCrafting.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
    }
    
    private static class CraftabilityResult {
        final boolean canCraft;
        final int maxCraftable;
        final String reason;
        
        CraftabilityResult(boolean canCraft, int maxCraftable, String reason) {
            this.canCraft = canCraft;
            this.maxCraftable = maxCraftable;
            this.reason = reason;
        }
    }
}

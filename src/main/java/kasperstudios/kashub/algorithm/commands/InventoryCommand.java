package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Inventory management command
 * Syntax:
 *   inventory check - Check if inventory is full
 *   inventory count <item> - Count specific item
 *   inventory find <item> - Find slot with item
 *   inventory drop <slot> - Drop item from slot
 *   inventory swap <slot1> <slot2> - Swap items between slots
 */
public class InventoryCommand implements Command {
    
    @Override
    public String getName() {
        return "inventory";
    }
    
    @Override
    public String getDescription() {
        return "Manage player inventory";
    }
    
    @Override
    public String getParameters() {
        return "check | count <item> | find <item> | drop <slot> | swap <s1> <s2>";
    }

    @Override
    public String getCategory() {
        return "Inventory";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Inventory management commands.\n\n" +
               "Actions:\n" +
               "  inventory check      - Check if full\n" +
               "  inventory count <item> - Count items\n" +
               "  inventory find <item>  - Find slot\n" +
               "  inventory empty      - Count empty slots\n" +
               "  inventory drop <slot> - Drop from slot\n" +
               "  inventory swap <s1> <s2> - Swap slots\n" +
               "  inventory open/close\n\n" +
               "Variables set:\n" +
               "  $inv_full        - Is inventory full\n" +
               "  $inv_empty_slots - Empty slot count\n" +
               "  $inv_item_count  - Item count\n" +
               "  $inv_found_slot  - Found slot (-1 if none)";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        PlayerInventory inv = player.getInventory();
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "check":
                checkInventory(inv, interpreter);
                break;
                
            case "count":
                if (args.length < 2) {
                    System.out.println("Usage: inventory count <item_name>");
                    return;
                }
                countItem(inv, args[1], interpreter);
                break;
                
            case "find":
                if (args.length < 2) {
                    System.out.println("Usage: inventory find <item_name>");
                    return;
                }
                findItem(inv, args[1], interpreter);
                break;
                
            case "drop":
                if (args.length < 2) {
                    System.out.println("Usage: inventory drop <slot>");
                    return;
                }
                dropItem(player, Integer.parseInt(args[1]));
                break;
                
            case "swap":
                if (args.length < 3) {
                    System.out.println("Usage: inventory swap <slot1> <slot2>");
                    return;
                }
                swapItems(player, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                break;
                
            case "empty":
                countEmptySlots(inv, interpreter);
                break;
                
            default:
                printHelp();
        }
    }
    
    private void checkInventory(PlayerInventory inv, ScriptInterpreter interpreter) {
        int emptySlots = 0;
        int totalSlots = 36; // Main inventory + hotbar
        
        for (int i = 0; i < totalSlots; i++) {
            if (inv.getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        
        boolean isFull = emptySlots == 0;
        interpreter.setVariable("inv_full", String.valueOf(isFull));
        interpreter.setVariable("inv_empty_slots", String.valueOf(emptySlots));
        interpreter.setVariable("inv_used_slots", String.valueOf(totalSlots - emptySlots));
        
        System.out.println("Inventory: " + (totalSlots - emptySlots) + "/" + totalSlots + " slots used" + 
                          (isFull ? " (FULL!)" : ""));
    }
    
    private void countEmptySlots(PlayerInventory inv, ScriptInterpreter interpreter) {
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isEmpty()) {
                emptySlots++;
            }
        }
        interpreter.setVariable("inv_empty_slots", String.valueOf(emptySlots));
        System.out.println("Empty slots: " + emptySlots);
    }
    
    private void countItem(PlayerInventory inv, String itemName, ScriptInterpreter interpreter) {
        int count = 0;
        String searchName = itemName.toLowerCase();
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                String stackName = stack.getItem().toString().toLowerCase();
                if (stackName.contains(searchName)) {
                    count += stack.getCount();
                }
            }
        }
        
        interpreter.setVariable("inv_item_count", String.valueOf(count));
        interpreter.setVariable("inv_has_item", count > 0 ? "true" : "false");
        System.out.println("Found " + count + " " + itemName);
    }
    
    private void findItem(PlayerInventory inv, String itemName, ScriptInterpreter interpreter) {
        String searchName = itemName.toLowerCase();
        int foundSlot = -1;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                String stackName = stack.getItem().toString().toLowerCase();
                if (stackName.contains(searchName)) {
                    foundSlot = i;
                    break;
                }
            }
        }
        
        interpreter.setVariable("inv_found_slot", String.valueOf(foundSlot));
        interpreter.setVariable("inv_item_found", foundSlot >= 0 ? "true" : "false");
        
        if (foundSlot >= 0) {
            System.out.println("Found " + itemName + " in slot " + foundSlot);
        } else {
            System.out.println(itemName + " not found in inventory");
        }
    }
    
    private void dropItem(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot >= 36) {
            System.out.println("Invalid slot: " + slot);
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null) {
            // Drop entire stack from slot
            client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                slot,
                1, // Right click = drop one, use Q key action instead
                SlotActionType.THROW,
                player
            );
            System.out.println("Dropped item from slot " + slot);
        }
    }
    
    private void swapItems(ClientPlayerEntity player, int slot1, int slot2) {
        if (slot1 < 0 || slot1 >= 36 || slot2 < 0 || slot2 >= 36) {
            System.out.println("Invalid slots");
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null) {
            // Pick up from slot1
            client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                slot1,
                0,
                SlotActionType.PICKUP,
                player
            );
            // Place in slot2
            client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                slot2,
                0,
                SlotActionType.PICKUP,
                player
            );
            // Place remainder back in slot1
            client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                slot1,
                0,
                SlotActionType.PICKUP,
                player
            );
            System.out.println("Swapped slots " + slot1 + " and " + slot2);
        }
    }
    
    private void printHelp() {
        System.out.println("Inventory Command:");
        System.out.println("  inventory check - Check if full, sets $inv_full, $inv_empty_slots");
        System.out.println("  inventory count <item> - Count items, sets $inv_item_count");
        System.out.println("  inventory find <item> - Find item slot, sets $inv_found_slot");
        System.out.println("  inventory empty - Count empty slots");
        System.out.println("  inventory drop <slot> - Drop item from slot");
        System.out.println("  inventory swap <s1> <s2> - Swap items");
    }
}

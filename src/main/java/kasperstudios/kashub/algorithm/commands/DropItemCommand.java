package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Команда для выбрасывания предметов
 * Синтаксис: drop [itemName] [count] или drop all
 */
public class DropItemCommand implements Command {

    @Override
    public String getName() {
        return "drop";
    }

    @Override
    public String getDescription() {
        return "Drops items from inventory";
    }

    @Override
    public String getParameters() {
        return "[item] [count] | all | stack";
    }
    
    @Override
    public String getCategory() {
        return "Inventory";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Drops items from player's inventory.\n\n" +
               "Usage:\n" +
               "  drop                    - Drop 1 item from hand\n" +
               "  drop stack              - Drop entire stack from hand\n" +
               "  drop all                - Drop ALL items (empties inventory!)\n" +
               "  drop <item>             - Drop 1 of specified item\n" +
               "  drop <item> <count>     - Drop specific amount\n\n" +
               "Parameters:\n" +
               "  item   - Item name (partial match, e.g. 'diamond')\n" +
               "  count  - Number of items to drop\n\n" +
               "Details:\n" +
               "  - Item names use partial matching\n" +
               "  - 'diamond' matches 'diamond', 'diamond_sword', etc.\n" +
               "  - Items are dropped in front of player\n" +
               "  - 'all' drops EVERYTHING - use with caution!\n\n" +
               "Examples:\n" +
               "  drop                    // Drop 1 from hand\n" +
               "  drop stack              // Drop full stack\n" +
               "  drop cobblestone 64     // Drop 64 cobblestone\n" +
               "  drop diamond            // Drop 1 diamond\n" +
               "  drop all                // Empty inventory\n\n" +
               "Warning: 'drop all' cannot be undone!";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        if (args.length == 0) {
            // Выбросить предмет в руке
            client.execute(() -> {
                player.dropSelectedItem(false);
            });
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            // Выбросить все предметы
            client.execute(() -> {
                for (int i = 0; i < player.getInventory().size(); i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (!stack.isEmpty()) {
                        player.dropItem(stack.copy(), false);
                        player.getInventory().setStack(i, ItemStack.EMPTY);
                    }
                }
            });
            return;
        }

        if (args[0].equalsIgnoreCase("stack")) {
            // Выбросить весь стак в руке
            client.execute(() -> {
                player.dropSelectedItem(true);
            });
            return;
        }

        String itemName = args[0].toLowerCase();
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        client.execute(() -> {
            int dropped = 0;
            for (int i = 0; i < player.getInventory().size() && dropped < count; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String stackName = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
                    if (stackName.contains(itemName)) {
                        int toDrop = Math.min(stack.getCount(), count - dropped);
                        ItemStack dropStack = stack.copy();
                        dropStack.setCount(toDrop);
                        player.dropItem(dropStack, false);
                        stack.decrement(toDrop);
                        if (stack.isEmpty()) {
                            player.getInventory().setStack(i, ItemStack.EMPTY);
                        }
                        dropped += toDrop;
                    }
                }
            }
            if (dropped == 0) {
                System.out.println("Предмет не найден: " + itemName);
            }
        });
    }
}

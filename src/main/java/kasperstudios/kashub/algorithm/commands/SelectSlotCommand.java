package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Команда для выбора слота в хотбаре
 * Синтаксис: selectSlot <slot> или selectSlot item <itemName>
 */
public class SelectSlotCommand implements Command {

    @Override
    public String getName() {
        return "selectSlot";
    }

    @Override
    public String getDescription() {
        return "Selects hotbar slot by number or item";
    }

    @Override
    public String getParameters() {
        return "<0-8> | item <name>";
    }
    
    @Override
    public String getCategory() {
        return "Inventory";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Selects a hotbar slot by number or item name.\n\n" +
               "Usage:\n" +
               "  selectSlot <0-8>        - Select slot by number\n" +
               "  selectSlot item <name>  - Select slot containing item\n\n" +
               "Parameters:\n" +
               "  0-8   - Hotbar slot number (0 = leftmost)\n" +
               "  name  - Item name (partial match)\n\n" +
               "Details:\n" +
               "  - Slot numbers: 0-8 (left to right)\n" +
               "  - Item search uses partial matching\n" +
               "  - Only searches hotbar (not full inventory)\n" +
               "  - 'sword' matches 'diamond_sword', 'iron_sword', etc.\n\n" +
               "Examples:\n" +
               "  selectSlot 0            // First slot\n" +
               "  selectSlot 8            // Last slot\n" +
               "  selectSlot item sword   // Find any sword\n" +
               "  selectSlot item diamond_pickaxe\n" +
               "  selectSlot item food    // Find food item\n\n" +
               "Common pattern:\n" +
               "  selectSlot item pickaxe\n" +
               "  breakBlock\n" +
               "  wait 100";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || args.length == 0) return;

        if (args[0].equalsIgnoreCase("item") && args.length > 1) {
            String itemName = args[1].toLowerCase();
            client.execute(() -> {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (!stack.isEmpty()) {
                        String stackName = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
                        if (stackName.contains(itemName)) {
                            player.getInventory().selectedSlot = i;
                            return;
                        }
                    }
                }
                System.out.println("Предмет не найден в хотбаре: " + itemName);
            });
            return;
        }

        try {
            int slot = Integer.parseInt(args[0]);
            if (slot >= 0 && slot <= 8) {
                client.execute(() -> {
                    player.getInventory().selectedSlot = slot;
                });
            } else {
                System.out.println("Номер слота должен быть от 0 до 8");
            }
        } catch (NumberFormatException e) {
            System.out.println("Неверный номер слота: " + args[0]);
        }
    }
}

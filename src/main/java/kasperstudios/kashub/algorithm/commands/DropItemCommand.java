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
        return "Выбрасывает предметы из инвентаря";
    }

    @Override
    public String getParameters() {
        return "[itemName] [count] или all - имя предмета и количество, или все предметы";
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

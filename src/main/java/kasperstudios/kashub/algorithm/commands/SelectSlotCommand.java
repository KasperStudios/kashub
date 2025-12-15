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
        return "<slot> (0-8) or item <itemName> - slot number or item name";
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

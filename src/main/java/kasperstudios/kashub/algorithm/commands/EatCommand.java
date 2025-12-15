package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.registry.Registries;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Команда для употребления еды
 * Синтаксис: eat [itemName]
 */
public class EatCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "eat";
    }

    @Override
    public String getDescription() {
        return "Eats specified food or best available";
    }

    @Override
    public String getParameters() {
        return "[itemName] - food name (optional, eats best available if not specified)";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        String targetFood = args.length > 0 ? String.join("_", args).toLowerCase() : null;

        client.execute(() -> {
            int foodSlot = findFoodSlot(player, targetFood);
            if (foodSlot != -1) {
                if (foodSlot < 9) {
                    player.getInventory().selectedSlot = foodSlot;
                } else {
                    swapToHotbar(player, foodSlot);
                }
                
                // Начинаем есть
                client.options.useKey.setPressed(true);
            } else {
                System.out.println("Еда не найдена" + (targetFood != null ? ": " + targetFood : ""));
            }
        });
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            future.complete(null);
            return future;
        }

        String targetFood = args.length > 0 ? String.join("_", args).toLowerCase() : null;

        client.execute(() -> {
            int foodSlot = findFoodSlot(player, targetFood);
            if (foodSlot != -1) {
                if (foodSlot < 9) {
                    player.getInventory().selectedSlot = foodSlot;
                } else {
                    swapToHotbar(player, foodSlot);
                }
                
                client.options.useKey.setPressed(true);
                
                // Ждём пока игрок поест (примерно 1.6 секунды)
                scheduler.schedule(() -> {
                    client.execute(() -> {
                        client.options.useKey.setPressed(false);
                    });
                    future.complete(null);
                }, 1700, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("Еда не найдена" + (targetFood != null ? ": " + targetFood : ""));
                future.complete(null);
            }
        });

        return future;
    }

    private int findFoodSlot(ClientPlayerEntity player, String targetFood) {
        int bestSlot = -1;
        int bestHunger = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (!stack.isEmpty() && food != null) {
                String itemName = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
                    
                    if (targetFood != null) {
                        if (itemName.contains(targetFood) || targetFood.contains(itemName)) {
                            return i;
                        }
                    } else {
                        // Ищем лучшую еду
                        int hunger = food.nutrition();
                        if (hunger > bestHunger) {
                            bestHunger = hunger;
                            bestSlot = i;
                        }
                    }
            }
        }
        return bestSlot;
    }

    private void swapToHotbar(ClientPlayerEntity player, int inventorySlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        int hotbarSlot = player.getInventory().selectedSlot;
        
        if (client.interactionManager != null) {
            client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                inventorySlot,
                hotbarSlot,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                player
            );
        }
    }
}

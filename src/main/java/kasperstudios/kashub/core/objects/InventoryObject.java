package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.services.automation.AutoCraftService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryObject - OO wrapper for inventory management.
 * Usage: inventory.count("diamond"), inventory.drop(0)
 */
public class InventoryObject extends Value {

    public InventoryObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("count", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.of(0);
            String searchString = args.get(0).asString().toLowerCase();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return Value.of(0);
            PlayerInventory inv = client.player.getInventory();

            int count = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains(searchString)) {
                    count += stack.getCount();
                }
            }
            return Value.of(count);
        }));

        methods.put("getItems", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return Value.of(new ArrayList<>());
            PlayerInventory inv = client.player.getInventory();
            List<Value> items = new ArrayList<>();

            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty()) {
                    Map<String, Value> item = new HashMap<>();
                    item.put("slot", Value.of(i));
                    item.put("id", Value.of(stack.getItem().toString()));
                    item.put("count", Value.of(stack.getCount()));
                    item.put("name", Value.of(stack.getName().getString()));
                    items.add(Value.of(item));
                }
            }
            return Value.of(items);
        }));

        methods.put("getEmptySlots", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return Value.of(0);
            PlayerInventory inv = client.player.getInventory();

            int empty = 0;
            for (int i = 0; i < 36; i++) { // Main inventory only
                if (inv.getStack(i).isEmpty())
                    empty++;
            }
            return Value.of(empty);
        }));

        methods.put("drop", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            int slot = args.get(0).asInt();
            boolean dropAll = args.size() > 1 && args.get(1).asBoolean();

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot, dropAll ? 1 : 0,
                    SlotActionType.THROW, player);
            return Value.TRUE;
        }));

        methods.put("swap", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 2)
                return Value.FALSE;
            int slot1 = args.get(0).asInt();
            int slot2 = args.get(1).asInt();

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP,
                    player);
            client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP,
                    player);
            client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP,
                    player);
            return Value.TRUE;
        }));

        methods.put("equip", Value.of((Callable) (ctx, args) -> {
            // equip(slot) -> moves to appropriate armor slot or offhand
            if (args.isEmpty())
                return Value.FALSE;
            int slot = args.get(0).asInt();
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            // Allow shift-click to equip (simulates quick move to armor/shield)
            client.interactionManager.clickSlot(player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE,
                    player);
            return Value.TRUE;
        }));

        methods.put("use", Value.of((Callable) (ctx, args) -> {
            // use(itemName) -> finds and uses item (right-click)
            if (args.isEmpty())
                return Value.FALSE;
            String itemName = args.get(0).asString().toLowerCase();

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            PlayerInventory inv = player.getInventory();
            int foundSlot = -1;

            // Search for item in inventory
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains(itemName)) {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot == -1)
                return Value.FALSE;

            // Save current slot
            int originalSlot = inv.selectedSlot;

            // If item is in hotbar (0-8), select it
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selectedSlot = foundSlot;
            } else {
                // Swap to hotbar first
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, foundSlot, 0,
                        SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, 36 + originalSlot, 0,
                        SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, foundSlot, 0,
                        SlotActionType.PICKUP, player);
            }

            // Use item (right-click)
            client.interactionManager.interactItem(player, net.minecraft.util.Hand.MAIN_HAND);

            // Restore original slot if needed
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selectedSlot = originalSlot;
            }

            return Value.TRUE;
        }));

        methods.put("craft", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 2)
                return Value.FALSE;
            String item = args.get(0).asString();
            int count = args.get(1).asInt();

            boolean success = AutoCraftService.getInstance().craft(item, count);
            return Value.of(success);
        }));

        methods.put("check", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return Value.NULL;
            
            PlayerInventory inv = client.player.getInventory();
            Map<String, Value> status = new HashMap<>();
            
            int totalItems = 0;
            int emptySlots = 0;
            
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isEmpty()) {
                    emptySlots++;
                } else {
                    totalItems += stack.getCount();
                }
            }
            
            status.put("totalItems", Value.of(totalItems));
            status.put("emptySlots", Value.of(emptySlots));
            status.put("usedSlots", Value.of(36 - emptySlots));
            
            return Value.of(status);
        }));

        methods.put("find", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.of(-1);
            String searchString = args.get(0).asString().toLowerCase();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null)
                return Value.of(-1);
            PlayerInventory inv = client.player.getInventory();

            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains(searchString)) {
                    return Value.of(i);
                }
            }
            return Value.of(-1);
        }));

        return methods;
    }
}

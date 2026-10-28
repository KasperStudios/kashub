package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kasperstudios.kashub.core.ExecutionOrigin;

/**
 * InventoryObject - OO wrapper for inventory management.
 * Usage: inventory.count("diamond"), inventory.drop(0)
 */
public class InventoryObject extends Value implements Documentable {

    public InventoryObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("count", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.of(0);
            String searchString = args.get(0).asString().toLowerCase();

            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return Value.of(0);
            Inventory inv = client.player.getInventory();

            int count = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem()).contains(searchString)) {
                    count += stack.getCount();
                }
            }
            return Value.of(count);
        }));

        methods.put("getItems", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return Value.of(new ArrayList<>());
            Inventory inv = client.player.getInventory();
            List<Value> items = new ArrayList<>();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    Map<String, Value> item = new HashMap<>();
                    item.put("slot", Value.of(i));
                    item.put("id", Value.of(com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem())));
                    item.put("count", Value.of(stack.getCount()));
                    item.put("name", Value.of(stack.getHoverName().getString()));
                    item.put("data", Value.of(com.kasperstudios.khwrapper.KHWrapper.get().getItemData(stack)));
                    items.add(Value.of(item));
                }
            }
            return Value.of(items);
        }));

        methods.put("getEmptySlots", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return Value.of(0);
            Inventory inv = client.player.getInventory();

            int empty = 0;
            for (int i = 0; i < 36; i++) { // Main inventory only
                if (inv.getItem(i).isEmpty())
                    empty++;
            }
            return Value.of(empty);
        }));

        methods.put("drop", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Inventory modification requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowInventoryControl) {
                throw new SecurityException("Access Denied: Inventory modification is disabled by server/config.");
            }
            if (args.isEmpty()) return Value.FALSE;
            int slot = args.get(0).asInt();
            boolean dropAll = args.size() > 1 && args.get(1).asBoolean();

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null)
                return Value.FALSE;

            client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot, dropAll ? 1 : 0,
                    ClickType.THROW, player);
            return Value.TRUE;
        }));

        methods.put("swap", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Inventory modification requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowInventoryControl) {
                throw new SecurityException("Access Denied: Inventory modification is disabled by server/config.");
            }
            if (args.size() < 2)
                return Value.FALSE;
            int slot1 = args.get(0).asInt();
            int slot2 = args.get(1).asInt();

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null)
                return Value.FALSE;

            client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot1, 0, ClickType.PICKUP,
                    player);
            client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot2, 0, ClickType.PICKUP,
                    player);
            client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot1, 0, ClickType.PICKUP,
                    player);
            return Value.TRUE;
        }));

        methods.put("equip", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Inventory modification (equip) requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowInventoryControl) {
                throw new SecurityException("Access Denied: Inventory modification is disabled by server/config.");
            }
            // equip(slot) -> moves to appropriate armor slot or offhand
            if (args.isEmpty())
                return Value.FALSE;
            int slot = args.get(0).asInt();
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null)
                return Value.FALSE;

            // Allow shift-click to equip (simulates quick move to armor/shield)
            client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE,
                    player);
            return Value.TRUE;
        }));

        methods.put("use", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Inventory interaction (use) requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowInventoryControl) {
                throw new SecurityException("Access Denied: Inventory modification is disabled by server/config.");
            }
            // use(itemName) -> finds and uses item (right-click)
            if (args.isEmpty())
                return Value.FALSE;
            String itemName = args.get(0).asString().toLowerCase();

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null)
                return Value.FALSE;

            Inventory inv = player.getInventory();
            int foundSlot = -1;

            // Search for item in inventory
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem()).contains(itemName)) {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot == -1)
                return Value.FALSE;

            // Save current slot
            int originalSlot = inv.selected;

            // If item is in hotbar (0-8), select it
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selected = foundSlot;
            } else {
                // Swap to hotbar first
                client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, foundSlot, 0,
                        ClickType.PICKUP, player);
                client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, 36 + originalSlot, 0,
                        ClickType.PICKUP, player);
                client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, foundSlot, 0,
                        ClickType.PICKUP, player);
            }

            // Use item (right-click)
            client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);

            // Restore original slot if needed
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selected = originalSlot;
            }

            return Value.TRUE;
        }));

        methods.put("craft", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Crafting requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowInventoryControl) {
                throw new SecurityException("Access Denied: Inventory modification (crafting) is disabled by server/config.");
            }
            if (args.size() < 2) return Value.FALSE;
            String item = args.get(0).asString();
            int count = args.get(1).asInt();

            boolean success = false; // AutoCraftService removed in v1.0.0-FSR
            return Value.of(success);
        }));

        methods.put("check", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return Value.NULL;
            
            Inventory inv = client.player.getInventory();
            Map<String, Value> status = new HashMap<>();
            
            int totalItems = 0;
            int emptySlots = 0;
            
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
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

            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return Value.of(-1);
            Inventory inv = client.player.getInventory();

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem()).contains(searchString)) {
                    return Value.of(i);
                }
            }
            return Value.of(-1);
        }));

        // v1.1 - Container (Chest) methods merged into Inventory
        methods.put("isContainerOpen", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            return Value.of(client.player != null && client.player.containerMenu != client.player.inventoryMenu);
        }));

        methods.put("containerTransferAll", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null || player.containerMenu == player.inventoryMenu) return Value.FALSE;
            
            net.minecraft.world.inventory.AbstractContainerMenu menu = player.containerMenu;
            int containerSize = 0;
            for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                if (slot.container != player.getInventory()) containerSize++;
            }

            for (int i = containerSize; i < menu.slots.size(); i++) {
                if (!menu.getSlot(i).getItem().isEmpty()) {
                    client.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, player);
                }
            }
            return Value.TRUE;
        }));

        methods.put("closeContainer", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.closeContainer();
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("has", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String search = args.get(0).asString().toLowerCase();
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return Value.FALSE;
            Inventory inv = client.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) continue;
                String id = com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem());
                String name = stack.getHoverName().getString().toLowerCase();
                if (id.contains(search) || name.contains(search)) return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("offhand", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String itemName = args.get(0).asString().toLowerCase();
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) return Value.FALSE;

            Inventory inv = player.getInventory();
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty() && (com.kasperstudios.khwrapper.KHWrapper.get().getItemId(offhand.getItem()).contains(itemName) || offhand.getHoverName().getString().toLowerCase().contains(itemName))) {
                return Value.TRUE;
            }

            int foundSlot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && (com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem()).contains(itemName) || stack.getHoverName().getString().toLowerCase().contains(itemName))) {
                    foundSlot = i; break;
                }
            }

            if (foundSlot == -1) return Value.FALSE;

            // Map Inventory slot to Container slot (InventoryMenu)
            int menuSlot = foundSlot;
            if (foundSlot < 9) menuSlot = 36 + foundSlot; // Hotbar
            // Main inventory slots 9-35 are same in both
            
            // Offhand is slot 45 in InventoryMenu
            client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, menuSlot, 40, ClickType.SWAP, player);
            return Value.TRUE;
        }));

        methods.put("select", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            String itemName = args.get(0).asString().toLowerCase();
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) return Value.FALSE;

            Inventory inv = player.getInventory();
            int foundSlot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && (com.kasperstudios.khwrapper.KHWrapper.get().getItemId(stack.getItem()).contains(itemName) || stack.getHoverName().getString().toLowerCase().contains(itemName))) {
                    foundSlot = i; break;
                }
            }

            if (foundSlot == -1) return Value.FALSE;

            if (foundSlot < 9) {
                inv.selected = foundSlot;
            } else {
                client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, foundSlot, inv.selected, ClickType.SWAP, player);
            }
            return Value.TRUE;
        }));

        return methods;
    }

    @Override
    public String getObjectName() {
        return "inventory";
    }

    @Override
    public String getObjectDescription() {
        return "Provides methods for managing the player's inventory, crafting, and container interactions.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("count(itemName)", "Returns the total count of the specified item in the inventory.");
        docs.put("getItems()", "Returns a list of all items currently in the inventory, containing slot, id, count, name, and data.");
        docs.put("getEmptySlots()", "Returns the number of empty slots in the main inventory.");
        docs.put("drop(slot, [dropAll])", "Drops the item from the specified slot. If dropAll is true, drops the entire stack.");
        docs.put("swap(slot1, slot2)", "Swaps the items in slot1 and slot2.");
        docs.put("equip(slot)", "Quickly moves the item in the specified slot to an appropriate armor/shield slot.");
        docs.put("use(itemName)", "Finds the specified item, selects it, and simulates a right-click use action.");
        docs.put("craft(itemName, count)", "Attempts to automatically craft the specified item using the AutoCraft service.");
        docs.put("check()", "Returns a status map of the inventory: totalItems, emptySlots, and usedSlots.");
        docs.put("find(itemName)", "Returns the first slot index containing the specified item, or -1 if not found.");
        docs.put("isContainerOpen()", "Returns true if the player currently has a chest or other container open.");
        docs.put("containerTransferAll()", "Quick-moves all items from an open container into the player's inventory.");
        docs.put("closeContainer()", "Closes the currently open container.");
        docs.put("has(itemName)", "Returns true if the inventory contains the specified item.");
        docs.put("offhand(itemName)", "Finds the specified item and moves it to the offhand slot (shield slot).");
        docs.put("select(itemName)", "Finds the specified item and selects it in the hotbar, swapping it if necessary.");
        return docs;
    }
}

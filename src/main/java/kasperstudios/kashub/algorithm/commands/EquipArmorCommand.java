package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Команда для экипировки брони
 * Синтаксис: equipArmor [type] - diamond, iron, gold, netherite, leather, chainmail
 */
public class EquipArmorCommand implements Command {

    @Override
    public String getName() {
        return "equipArmor";
    }

    @Override
    public String getDescription() {
        return "Equips armor of specified type from inventory";
    }

    @Override
    public String getParameters() {
        return "[type] - armor material or 'best'";
    }
    
    @Override
    public String getCategory() {
        return "Inventory";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Automatically equips armor from inventory.\n\n" +
               "Usage:\n" +
               "  equipArmor              - Equip best armor available\n" +
               "  equipArmor best         - Same as above\n" +
               "  equipArmor <type>       - Equip specific armor type\n\n" +
               "Armor Types:\n" +
               "  netherite  - Best protection\n" +
               "  diamond    - High protection\n" +
               "  iron       - Medium protection\n" +
               "  chainmail  - Medium protection\n" +
               "  gold       - Low protection (high enchantability)\n" +
               "  leather    - Lowest protection (dyeable)\n\n" +
               "Details:\n" +
               "  - Equips all 4 pieces: helmet, chestplate, leggings, boots\n" +
               "  - 'best' mode selects highest protection value\n" +
               "  - Uses shift-click for quick equip\n" +
               "  - Searches entire inventory, not just hotbar\n\n" +
               "Examples:\n" +
               "  equipArmor              // Best available\n" +
               "  equipArmor diamond      // Diamond set\n" +
               "  equipArmor netherite    // Netherite set\n" +
               "  equipArmor iron         // Iron set\n\n" +
               "Note: Only equips armor found in inventory.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        String armorType = args.length > 0 ? args[0].toLowerCase() : "best";

        client.execute(() -> {
            equipArmorPiece(player, EquipmentSlot.HEAD, armorType);
            equipArmorPiece(player, EquipmentSlot.CHEST, armorType);
            equipArmorPiece(player, EquipmentSlot.LEGS, armorType);
            equipArmorPiece(player, EquipmentSlot.FEET, armorType);
        });
    }

    private void equipArmorPiece(ClientPlayerEntity player, EquipmentSlot slot, String armorType) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        int bestSlot = -1;
        int bestProtection = 0;
        
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) stack.getItem();
                if (armor.getSlotType() == slot) {
                    String itemName = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
                    
                    if (armorType.equals("best")) {
                        int protection = armor.getProtection();
                        if (protection > bestProtection) {
                            bestProtection = protection;
                            bestSlot = i;
                        }
                    } else if (itemName.contains(armorType)) {
                        bestSlot = i;
                        break;
                    }
                }
            }
        }
        
        if (bestSlot != -1) {
            int armorSlotIndex = getArmorSlotIndex(slot);
            if (client.interactionManager != null) {
                // Shift-click для быстрой экипировки
                client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    bestSlot < 9 ? bestSlot + 36 : bestSlot,
                    0,
                    net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
                    player
                );
            }
        }
    }

    private int getArmorSlotIndex(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return 5;
            case CHEST: return 6;
            case LEGS: return 7;
            case FEET: return 8;
            default: return -1;
        }
    }
}

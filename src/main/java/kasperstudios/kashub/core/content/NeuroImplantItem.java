package kasperstudios.kashub.core.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import java.util.List;

public class NeuroImplantItem extends Item {
    private final int tier;

    public NeuroImplantItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (tier == 2 && !com.kasperstudios.khwrapper.KHWrapper.get().isModLoaded("computercraft")) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("item.kashub.neuro_implant.t2_requires_cc").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return net.minecraft.world.InteractionResultHolder.fail(stack);
        }

        // v1.0.0-FSR Patch: Try to install into Curios slot first if available
        try {
            if (com.kasperstudios.khwrapper.KHWrapper.get().isModLoaded("curios")) {
                kasperstudios.kashub.Kashub.debug("NeuroImplantItem: Attempting Curios installation for Tier {}", tier);
                Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");

                // NeoForge 1.21.1: CuriosApi.getCuriosInventory(LivingEntity) -> Optional<ICuriosItemHandler>
                java.lang.reflect.Method getInventory = apiClass.getMethod("getCuriosInventory",
                        net.minecraft.world.entity.LivingEntity.class);
                java.util.Optional<?> inventoryResult = (java.util.Optional<?>) getInventory.invoke(null, player);

                if (inventoryResult.isPresent()) {
                    Object curioInventory = inventoryResult.get();
                    
                    // Use findCurio(String identifier, int index) -> Optional<SlotResult>
                    java.lang.reflect.Method findCurio = curioInventory.getClass()
                            .getMethod("findCurio", String.class, int.class);
                    java.util.Optional<?> slotResult = (java.util.Optional<?>) findCurio.invoke(curioInventory, "neural", 0);

                    // Case-insensitive lookup for "neural"
                    Object neuralSlotHandler = null;
                    try {
                        java.lang.reflect.Method getCurios = curioInventory.getClass().getMethod("getCurios");
                        java.util.Map<String, ?> slotsMap = (java.util.Map<String, ?>) getCurios.invoke(curioInventory);
                        
                        for (String key : slotsMap.keySet()) {
                            if (key.equalsIgnoreCase("neural")) {
                                neuralSlotHandler = slotsMap.get(key);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        kasperstudios.kashub.Kashub.LOGGER.error("NeuroImplantItem: Failed to access Curios slots map: {}", e.getMessage());
                    }

                    if (neuralSlotHandler != null) {
                        java.lang.reflect.Method getStacks = neuralSlotHandler.getClass().getMethod("getStacks");
                        Object stackHandler = getStacks.invoke(neuralSlotHandler);
                        java.lang.reflect.Method getStackInSlot = stackHandler.getClass().getMethod("getStackInSlot", int.class);
                        ItemStack currentStack = (ItemStack) getStackInSlot.invoke(stackHandler, 0);

                        if (currentStack.isEmpty()) {
                            if (!level.isClientSide()) {
                                java.lang.reflect.Method setStackInSlot = stackHandler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);
                                setStackInSlot.invoke(stackHandler, 0, stack.copy());
                                
                                kasperstudios.kashub.Kashub.LOGGER.info("NeuroImplantItem: Successfully equipped Tier {} in Curios slot", tier);
                                stack.setCount(0);
                                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                                    kasperstudios.kashub.services.InjectionService.getInstance().startInjection(serverPlayer, tier);
                                }
                            } else {
                                player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 1.0f);
                            }
                            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                        } else {
                            if (level.isClientSide()) {
                                player.displayClientMessage(Component.translatable("message.kashub.slot_occupied").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                            }
                            return net.minecraft.world.InteractionResultHolder.pass(stack);
                        }
                    } else {
                        kasperstudios.kashub.Kashub.LOGGER.warn("NeuroImplantItem: Slot 'neural' not found in Curios.");
                    }
                }
            }
        } catch (Exception e) {
            kasperstudios.kashub.Kashub.LOGGER.warn("NeuroImplantItem: Curios installation failed (will fallback): {}", e.getMessage());
        }

        // Fallback to internal inventory (Duck interface)
        if (player instanceof kasperstudios.kashub.core.IImplantInventoryAccess access) {
            net.minecraft.world.Container inv = access.kashub$getImplantInventory();
            ItemStack existing = inv.getItem(0);

            if (existing.isEmpty()) {
                if (!level.isClientSide()) {
                    inv.setItem(0, stack.copy());
                    stack.setCount(0);
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        kasperstudios.kashub.services.InjectionService.getInstance().startInjection(serverPlayer, tier);
                    }
                } else {
                    player.playSound(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 1.0f);
                }
                return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                if (level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.kashub.slot_occupied").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                }
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
        }

        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kashub.neuro_implant.tier", tier).withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.kashub.neuro_implant.instruction").withStyle(net.minecraft.ChatFormatting.YELLOW));
        if (tier == 1) {
            tooltip.add(Component.translatable("item.kashub.neuro_implant.desc.t1").withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.kashub.neuro_implant.desc.t2").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.kashub.neuro_implant.warning").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.ITALIC));
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.registry.Registries;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Команда авто-торговли с жителями
 * 
 * Синтаксис:
 *   autoTrade config <radius> <maxEmeralds> <mode>
 *   autoTrade target <item1,item2,...>
 *   autoTrade start
 *   autoTrade stop
 *   autoTrade scan - сканировать жителей
 *   autoTrade offers - показать доступные трейды
 *   autoTrade buy <index> [count] - купить конкретный трейд
 */
public class AutoTradeCommand implements Command {
    
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean isTrading = new AtomicBoolean(false);
    private static CompletableFuture<Void> currentTask = null;
    
    // Конфигурация
    private static int searchRadius = 32;
    private static int maxEmeraldsPerTrade = 64;
    private static String priorityMode = "cheapest"; // cheapest, fastest, custom
    private static Set<String> targetItems = new HashSet<>();
    private static MerchantEntity currentMerchant = null;
    
    @Override
    public String getName() {
        return "autoTrade";
    }
    
    @Override
    public String getDescription() {
        return "Automated trading with villagers";
    }
    
    @Override
    public String getParameters() {
        return "config|target|start|stop|scan|offers|buy <args>";
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "config":
                handleConfig(args, interpreter);
                break;
            case "target":
                handleTarget(args, interpreter);
                break;
            case "start":
                handleStart(player, interpreter);
                break;
            case "stop":
                handleStop(interpreter);
                break;
            case "scan":
                handleScan(player, interpreter);
                break;
            case "offers":
                handleOffers(player, interpreter);
                break;
            case "buy":
                handleBuy(player, args, interpreter);
                break;
            default:
                printHelp();
        }
    }
    
    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        MinecraftClient.getInstance().execute(() -> {
            try {
                execute(args);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    private void handleConfig(String[] args, ScriptInterpreter interpreter) {
        if (args.length >= 2) {
            try {
                searchRadius = Integer.parseInt(args[1]);
                searchRadius = Math.min(Math.max(searchRadius, 1), 64);
            } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 3) {
            try {
                maxEmeraldsPerTrade = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 4) {
            priorityMode = args[3].toLowerCase();
            if (!priorityMode.equals("cheapest") && !priorityMode.equals("fastest") && !priorityMode.equals("custom")) {
                priorityMode = "cheapest";
            }
        }
        
        interpreter.setVariable("autoTrade_radius", String.valueOf(searchRadius));
        interpreter.setVariable("autoTrade_maxEmeralds", String.valueOf(maxEmeraldsPerTrade));
        interpreter.setVariable("autoTrade_mode", priorityMode);
        
        System.out.println("AutoTrade config: radius=" + searchRadius + 
                          ", maxEmeralds=" + maxEmeraldsPerTrade + 
                          ", mode=" + priorityMode);
    }
    
    private void handleTarget(String[] args, ScriptInterpreter interpreter) {
        targetItems.clear();
        if (args.length >= 2) {
            String[] items = args[1].split(",");
            for (String item : items) {
                targetItems.add(item.trim().toLowerCase());
            }
        }
        
        interpreter.setVariable("autoTrade_targets", String.join(",", targetItems));
        System.out.println("AutoTrade targets: " + targetItems);
    }
    
    private void handleScan(ClientPlayerEntity player, ScriptInterpreter interpreter) {
        List<MerchantEntity> merchants = findMerchants(player, searchRadius);
        
        interpreter.setVariable("autoTrade_merchantCount", String.valueOf(merchants.size()));
        
        int index = 0;
        for (MerchantEntity merchant : merchants) {
            String type = merchant instanceof VillagerEntity ? "villager" : "wandering_trader";
            double dist = player.distanceTo(merchant);
            
            interpreter.setVariable("autoTrade_merchant_" + index + "_type", type);
            interpreter.setVariable("autoTrade_merchant_" + index + "_x", String.valueOf((int) merchant.getX()));
            interpreter.setVariable("autoTrade_merchant_" + index + "_y", String.valueOf((int) merchant.getY()));
            interpreter.setVariable("autoTrade_merchant_" + index + "_z", String.valueOf((int) merchant.getZ()));
            interpreter.setVariable("autoTrade_merchant_" + index + "_dist", String.format("%.1f", dist));
            
            if (merchant instanceof VillagerEntity villager) {
                interpreter.setVariable("autoTrade_merchant_" + index + "_profession", 
                    villager.getVillagerData().getProfession().id());
                interpreter.setVariable("autoTrade_merchant_" + index + "_level", 
                    String.valueOf(villager.getVillagerData().getLevel()));
            }
            
            index++;
            if (index >= 10) break; // Лимит на 10 жителей
        }
        
        System.out.println("Found " + merchants.size() + " merchants within " + searchRadius + " blocks");
    }
    
    private void handleOffers(ClientPlayerEntity player, ScriptInterpreter interpreter) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Проверяем, открыт ли экран торговли
        if (!(client.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            // Пытаемся найти ближайшего торговца и открыть торговлю
            List<MerchantEntity> merchants = findMerchants(player, 5);
            if (merchants.isEmpty()) {
                System.out.println("No merchants nearby. Move closer to a villager.");
                interpreter.setVariable("autoTrade_offersCount", "0");
                return;
            }
            
            MerchantEntity nearest = merchants.get(0);
            currentMerchant = nearest;
            
            // Взаимодействуем с торговцем
            client.interactionManager.interactEntity(player, nearest, Hand.MAIN_HAND);
            
            System.out.println("Opening trade with merchant. Run 'autoTrade offers' again.");
            return;
        }
        
        TradeOfferList offers = handler.getRecipes();
        interpreter.setVariable("autoTrade_offersCount", String.valueOf(offers.size()));
        
        int index = 0;
        for (TradeOffer offer : offers) {
            ItemStack sellItem = offer.getSellItem();
            ItemStack buyItem1 = offer.getDisplayedFirstBuyItem();
            ItemStack buyItem2 = offer.getDisplayedSecondBuyItem();
            
            String sellName = Registries.ITEM.getId(sellItem.getItem()).getPath();
            String buy1Name = Registries.ITEM.getId(buyItem1.getItem()).getPath();
            String buy2Name = buyItem2.isEmpty() ? "" : Registries.ITEM.getId(buyItem2.getItem()).getPath();
            
            interpreter.setVariable("autoTrade_offer_" + index + "_sell", sellName);
            interpreter.setVariable("autoTrade_offer_" + index + "_sellCount", String.valueOf(sellItem.getCount()));
            interpreter.setVariable("autoTrade_offer_" + index + "_buy1", buy1Name);
            interpreter.setVariable("autoTrade_offer_" + index + "_buy1Count", String.valueOf(buyItem1.getCount()));
            interpreter.setVariable("autoTrade_offer_" + index + "_buy2", buy2Name);
            interpreter.setVariable("autoTrade_offer_" + index + "_buy2Count", 
                buyItem2.isEmpty() ? "0" : String.valueOf(buyItem2.getCount()));
            interpreter.setVariable("autoTrade_offer_" + index + "_disabled", 
                String.valueOf(offer.isDisabled()));
            interpreter.setVariable("autoTrade_offer_" + index + "_uses", 
                String.valueOf(offer.getUses()) + "/" + offer.getMaxUses());
            
            // Проверяем стоимость в изумрудах
            int emeraldCost = 0;
            if (buyItem1.getItem() == Items.EMERALD) {
                emeraldCost = buyItem1.getCount();
            }
            interpreter.setVariable("autoTrade_offer_" + index + "_emeraldCost", String.valueOf(emeraldCost));
            
            System.out.println("[" + index + "] " + sellName + " x" + sellItem.getCount() + 
                             " <- " + buy1Name + " x" + buyItem1.getCount() +
                             (buy2Name.isEmpty() ? "" : " + " + buy2Name + " x" + buyItem2.getCount()) +
                             (offer.isDisabled() ? " [DISABLED]" : ""));
            
            index++;
        }
    }
    
    private void handleBuy(ClientPlayerEntity player, String[] args, ScriptInterpreter interpreter) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (!(client.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            System.out.println("No trade screen open. Use 'autoTrade offers' first.");
            interpreter.setVariable("autoTrade_buySuccess", "false");
            interpreter.setVariable("autoTrade_error", "no_trade_screen");
            return;
        }
        
        if (args.length < 2) {
            System.out.println("Usage: autoTrade buy <index> [count]");
            return;
        }
        
        int offerIndex;
        int count = 1;
        
        try {
            offerIndex = Integer.parseInt(args[1]);
            if (args.length >= 3) {
                count = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid index or count");
            return;
        }
        
        TradeOfferList offers = handler.getRecipes();
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            System.out.println("Invalid offer index: " + offerIndex);
            interpreter.setVariable("autoTrade_buySuccess", "false");
            interpreter.setVariable("autoTrade_error", "invalid_index");
            return;
        }
        
        TradeOffer offer = offers.get(offerIndex);
        
        if (offer.isDisabled()) {
            System.out.println("This trade is disabled (out of stock)");
            interpreter.setVariable("autoTrade_buySuccess", "false");
            interpreter.setVariable("autoTrade_error", "trade_disabled");
            return;
        }
        
        // Проверяем наличие ресурсов
        ItemStack required1 = offer.getDisplayedFirstBuyItem();
        ItemStack required2 = offer.getDisplayedSecondBuyItem();
        
        int available1 = countItemInInventory(player, required1.getItem());
        int available2 = required2.isEmpty() ? Integer.MAX_VALUE : countItemInInventory(player, required2.getItem());
        
        int maxTrades = Math.min(
            available1 / required1.getCount(),
            required2.isEmpty() ? Integer.MAX_VALUE : available2 / required2.getCount()
        );
        maxTrades = Math.min(maxTrades, offer.getMaxUses() - offer.getUses());
        
        if (maxTrades <= 0) {
            System.out.println("Not enough resources for this trade");
            interpreter.setVariable("autoTrade_buySuccess", "false");
            interpreter.setVariable("autoTrade_error", "not_enough_resources");
            return;
        }
        
        int actualCount = Math.min(count, maxTrades);
        
        // Выполняем торговлю
        handler.setRecipeIndex(offerIndex);
        handler.switchTo(offerIndex);
        
        for (int i = 0; i < actualCount; i++) {
            // Симулируем клик по слоту результата
            if (client.interactionManager != null) {
                client.interactionManager.clickSlot(
                    handler.syncId,
                    2, // Result slot
                    0,
                    net.minecraft.screen.slot.SlotActionType.QUICK_MOVE,
                    player
                );
            }
        }
        
        String sellName = Registries.ITEM.getId(offer.getSellItem().getItem()).getPath();
        System.out.println("Bought " + actualCount + "x " + sellName);
        
        interpreter.setVariable("autoTrade_buySuccess", "true");
        interpreter.setVariable("autoTrade_buyCount", String.valueOf(actualCount));
        interpreter.setVariable("autoTrade_buyItem", sellName);
    }
    
    private void handleStart(ClientPlayerEntity player, ScriptInterpreter interpreter) {
        if (isTrading.get()) {
            System.out.println("AutoTrade is already running");
            return;
        }
        
        if (targetItems.isEmpty()) {
            System.out.println("No target items set. Use 'autoTrade target <items>' first.");
            interpreter.setVariable("autoTrade_active", "false");
            return;
        }
        
        isTrading.set(true);
        interpreter.setVariable("autoTrade_active", "true");
        
        System.out.println("AutoTrade started. Looking for: " + targetItems);
        
        currentTask = startAutoTrading(player, interpreter);
    }
    
    private void handleStop(ScriptInterpreter interpreter) {
        isTrading.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
        interpreter.setVariable("autoTrade_active", "false");
        System.out.println("AutoTrade stopped");
    }
    
    private CompletableFuture<Void> startAutoTrading(ClientPlayerEntity player, ScriptInterpreter interpreter) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        Runnable tradeTask = new Runnable() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    if (!isTrading.get()) {
                        future.complete(null);
                        return;
                    }
                    
                    ClientPlayerEntity p = MinecraftClient.getInstance().player;
                    if (p == null) {
                        future.complete(null);
                        return;
                    }
                    
                    // Ищем торговцев
                    List<MerchantEntity> merchants = findMerchants(p, searchRadius);
                    if (merchants.isEmpty()) {
                        interpreter.setVariable("autoTrade_status", "no_merchants");
                        scheduler.schedule(this, 2000, TimeUnit.MILLISECONDS);
                        return;
                    }
                    
                    // Находим торговца с нужными товарами
                    for (MerchantEntity merchant : merchants) {
                        if (!isTrading.get()) break;
                        
                        TradeOfferList offers = merchant.getOffers();
                        for (int i = 0; i < offers.size(); i++) {
                            TradeOffer offer = offers.get(i);
                            if (offer.isDisabled()) continue;
                            
                            String sellItem = Registries.ITEM.getId(offer.getSellItem().getItem()).getPath();
                            
                            // Проверяем, нужен ли нам этот предмет
                            boolean isTarget = false;
                            for (String target : targetItems) {
                                if (sellItem.contains(target)) {
                                    isTarget = true;
                                    break;
                                }
                            }
                            
                            if (!isTarget) continue;
                            
                            // Проверяем стоимость
                            ItemStack cost = offer.getDisplayedFirstBuyItem();
                            if (cost.getItem() == Items.EMERALD && cost.getCount() > maxEmeraldsPerTrade) {
                                continue;
                            }
                            
                            // Нашли подходящий трейд
                            interpreter.setVariable("autoTrade_foundItem", sellItem);
                            interpreter.setVariable("autoTrade_foundMerchant_x", String.valueOf((int) merchant.getX()));
                            interpreter.setVariable("autoTrade_foundMerchant_y", String.valueOf((int) merchant.getY()));
                            interpreter.setVariable("autoTrade_foundMerchant_z", String.valueOf((int) merchant.getZ()));
                            interpreter.setVariable("autoTrade_status", "found_trade");
                            
                            System.out.println("Found trade: " + sellItem + " at merchant " + 
                                             (int) merchant.getX() + ", " + (int) merchant.getY() + ", " + (int) merchant.getZ());
                        }
                    }
                    
                    if (isTrading.get()) {
                        scheduler.schedule(this, 1000, TimeUnit.MILLISECONDS);
                    } else {
                        future.complete(null);
                    }
                });
            }
        };
        
        scheduler.schedule(tradeTask, 0, TimeUnit.MILLISECONDS);
        return future;
    }
    
    private List<MerchantEntity> findMerchants(ClientPlayerEntity player, int radius) {
        List<MerchantEntity> result = new ArrayList<>();
        Box searchBox = player.getBoundingBox().expand(radius);
        
        for (Entity entity : player.getWorld().getOtherEntities(player, searchBox)) {
            if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
                result.add((MerchantEntity) entity);
            }
        }
        
        // Сортируем по расстоянию
        result.sort(Comparator.comparingDouble(e -> player.squaredDistanceTo(e)));
        
        return result;
    }
    
    private int countItemInInventory(ClientPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private void printHelp() {
        System.out.println("AutoTrade Command:");
        System.out.println("  autoTrade config <radius> [maxEmeralds] [mode]");
        System.out.println("    - Configure search radius, max emerald cost, priority mode");
        System.out.println("    - Modes: cheapest, fastest, custom");
        System.out.println("  autoTrade target <item1,item2,...>");
        System.out.println("    - Set target items to buy");
        System.out.println("  autoTrade scan");
        System.out.println("    - Scan for nearby merchants");
        System.out.println("  autoTrade offers");
        System.out.println("    - Show available trades (must be near merchant)");
        System.out.println("  autoTrade buy <index> [count]");
        System.out.println("    - Buy specific trade by index");
        System.out.println("  autoTrade start");
        System.out.println("    - Start auto-trading");
        System.out.println("  autoTrade stop");
        System.out.println("    - Stop auto-trading");
    }
    
    public static boolean isActive() {
        return isTrading.get();
    }
    
    public static void stop() {
        isTrading.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
    }
}

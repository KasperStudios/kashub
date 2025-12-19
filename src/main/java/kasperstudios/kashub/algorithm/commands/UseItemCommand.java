package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.mixin.KeyBindingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;

public class UseItemCommand implements Command {
    private int holdTicks = 0;
    private boolean isHolding = false;

    @Override
    public String getName() {
        return "useItem";
    }

    @Override
    public String getDescription() {
        return "Uses the item in hand (right-click action)";
    }

    @Override
    public String getParameters() {
        return "[hold <ticks>|release|offhand]";
    }
    
    @Override
    public String getCategory() {
        return "Interaction";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Uses item in hand (simulates right-click).\n\n" +
               "Usage:\n" +
               "  useItem                 - Single right-click\n" +
               "  useItem hold            - Hold right-click indefinitely\n" +
               "  useItem hold <ticks>    - Hold for specified ticks\n" +
               "  useItem release         - Release held right-click\n" +
               "  useItem offhand         - Use item in offhand\n\n" +
               "Parameters:\n" +
               "  ticks  - Duration in game ticks (20 ticks = 1 second)\n\n" +
               "Details:\n" +
               "  - Single use: one right-click action\n" +
               "  - Hold mode: keeps right-click pressed\n" +
               "  - Useful for bows, shields, eating, fishing\n" +
               "  - 'offhand' uses item in left hand slot\n\n" +
               "Item-specific behaviors:\n" +
               "  - Bow: hold to charge, release to shoot\n" +
               "  - Shield: hold to block\n" +
               "  - Food: hold to eat (use 'eat' command instead)\n" +
               "  - Fishing rod: cast/reel\n" +
               "  - Ender pearl: throw\n" +
               "  - Bucket: place/collect liquid\n\n" +
               "Examples:\n" +
               "  useItem                 // Single use\n" +
               "  useItem hold 40         // Hold for 2 seconds\n" +
               "  useItem release         // Stop holding\n" +
               "  useItem offhand         // Use shield/totem\n\n" +
               "Bow shooting pattern:\n" +
               "  selectSlot item bow\n" +
               "  useItem hold 20         // Charge bow\n" +
               "  useItem release         // Shoot";
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        if (args.length == 0) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "hold":
                if (args.length > 1) {
                    try {
                        holdTicks = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        holdTicks = 20;
                    }
                } else {
                    holdTicks = -1;
                }
                ((KeyBindingMixin) client.options.useKey).setPressed(true);
                isHolding = true;
                break;
            case "release":
                ((KeyBindingMixin) client.options.useKey).setPressed(false);
                isHolding = false;
                holdTicks = 0;
                break;
            case "offhand":
                client.interactionManager.interactItem(player, Hand.OFF_HAND);
                break;
            default:
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                break;
        }
    }
}

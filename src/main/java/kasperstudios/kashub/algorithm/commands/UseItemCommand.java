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

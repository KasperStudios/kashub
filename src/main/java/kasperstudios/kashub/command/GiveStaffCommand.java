package kasperstudios.kashub.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class GiveStaffCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("givestaff")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                ServerCommandSource source = context.getSource();
                ServerPlayerEntity player = source.getPlayer();
                
                if (player != null) {
                    ItemStack staff = new ItemStack(Items.BLAZE_ROD);
                    staff.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("§6Teleport Staff"));
                    player.giveItemStack(staff);
                    source.sendFeedback(() -> Text.literal("Given Teleport Staff to " + player.getName().getString()), true);
                    return 1;
                }
                
                return 0;
            }));
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Command for setting health (creative/cheats only)
 * Syntax: setHealth <value>
 */
public class SetHealthCommand implements Command {
    
    private static final float MIN_HEALTH = 0.0f;
    private static final float MAX_HEALTH = 20.0f;
    private static final float DEFAULT_HEALTH = 20.0f;

    @Override
    public String getName() {
        return "setHealth";
    }

    @Override
    public String getDescription() {
        return "Sets player health (requires creative/cheats)";
    }

    @Override
    public String getParameters() {
        return "<value> - health value (0.0-20.0)";
    }
    
    @Override
    public String getCategory() {
        return "Player";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Sets player health.\n\n" +
               "Parameters:\n" +
               "  <value> - Health value (0.0 - 20.0)\n\n" +
               "Examples:\n" +
               "  setHealth 20    - Full health\n" +
               "  setHealth 10    - Half health\n" +
               "  setHealth 1     - Critical health\n\n" +
               "Note: Works only in creative mode or with cheats enabled.\n" +
               "May not work on servers due to server-side checks.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            throw new IllegalStateException("Player is null");
        }
        
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: setHealth <value>");
        }

        try {
            float health = Float.parseFloat(args[0]);
            
            // Validate and clamp health value
            if (health < MIN_HEALTH || health > MAX_HEALTH) {
                ScriptLogger.getInstance().warn("Health value " + health + " out of range, clamping to " + MIN_HEALTH + "-" + MAX_HEALTH);
                health = Math.max(MIN_HEALTH, Math.min(MAX_HEALTH, health));
            }
            
            final float finalHealth = health;
            client.execute(() -> {
                try {
                    player.setHealth(finalHealth);
                    ScriptLogger.getInstance().info("Health set to: " + finalHealth);
                    player.sendMessage(Text.literal("§7[Health] Set to §c" + String.format("%.1f", finalHealth) + " §7/ §c20.0"), false);
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Failed to set health: " + e.getMessage());
                    player.sendMessage(Text.literal("§c[Health] Error: " + e.getMessage()), false);
                }
            });

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid health value: " + args[0] + ". Must be a number between " + MIN_HEALTH + " and " + MAX_HEALTH);
        }
    }
}

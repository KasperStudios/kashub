package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Команда для установки здоровья (только в креативе/с читами)
 * Синтаксис: setHealth <value>
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
        return "Устанавливает здоровье игрока (требует креатив/читы)";
    }

    @Override
    public String getParameters() {
        return "<value> - значение здоровья (0.0-20.0)";
    }
    
    @Override
    public String getCategory() {
        return "Player";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Устанавливает здоровье игрока.\n\n" +
               "Параметры:\n" +
               "  <value> - Значение здоровья (0.0 - 20.0)\n\n" +
               "Примеры:\n" +
               "  setHealth 20    - Полное здоровье\n" +
               "  setHealth 10    - Половина здоровья\n" +
               "  setHealth 1     - Критическое здоровье\n\n" +
               "Примечание: Работает только в креативе или с включенными читами.\n" +
               "На серверах может не работать из-за проверок на стороне сервера.";
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

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public class HealthMonitorCommand implements Command {

    private static boolean isMonitoring = false;
    private static float healthThreshold = 10.0f;
    private static String alertAction = "log";
    private static Map<String, Long> lastAlerts = new HashMap<>();
    private static final long ALERT_COOLDOWN = 5000;

    @Override
    public String getName() {
        return "healthMonitor";
    }

    @Override
    public String getDescription() {
        return "Monitor player health and status effects";
    }

    @Override
    public String getParameters() {
        return "monitor|check|effects|stop <args>";
    }

    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public String getDetailedHelp() {
        return "Monitor player health and status effects.\n\n" +
               "Usage:\n" +
               "  healthMonitor monitor <threshold> [action]\n" +
               "  healthMonitor check\n" +
               "  healthMonitor effects\n" +
               "  healthMonitor stop\n\n" +
               "Actions:\n" +
               "  monitor - Start monitoring health\n" +
               "  check   - Check current health/hunger/effects\n" +
               "  effects - List active status effects\n" +
               "  stop    - Stop monitoring\n\n" +
               "Alert Actions:\n" +
               "  log     - Log to console (default)\n" +
               "  chat    - Send chat message\n" +
               "  sound   - Play alert sound\n" +
               "  script  - Execute script (future)\n\n" +
               "Examples:\n" +
               "  healthMonitor monitor 10 chat\n" +
               "  healthMonitor check\n" +
               "  healthMonitor effects\n" +
               "  healthMonitor stop\n\n" +
               "Variables set:\n" +
               "  $health          - Current health\n" +
               "  $health_max      - Max health\n" +
               "  $health_percent  - Health percentage\n" +
               "  $hunger          - Hunger level\n" +
               "  $saturation      - Saturation level\n" +
               "  $air             - Air level (underwater)\n" +
               "  $armor           - Armor points\n" +
               "  $effects_count   - Number of active effects\n" +
               "  $effect_<name>   - Effect duration (ticks)\n\n" +
               "Notes:\n" +
               "  - Monitoring runs every tick\n" +
               "  - Alerts have 5 second cooldown\n" +
               "  - Useful for auto-healing scripts";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "monitor":
                handleMonitor(args, interpreter);
                break;
            case "check":
                handleCheck(interpreter);
                break;
            case "effects":
                handleEffects(interpreter);
                break;
            case "stop":
                handleStop(interpreter);
                break;
            default:
                printHelp();
        }
    }

    private void handleMonitor(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: healthMonitor monitor <threshold> [action]");
            return;
        }

        try {
            healthThreshold = Float.parseFloat(args[1]);
            alertAction = args.length >= 3 ? args[2].toLowerCase() : "log";

            isMonitoring = true;
            interpreter.setVariable("health_monitoring", "true");
            interpreter.setVariable("health_threshold", String.valueOf(healthThreshold));

            ScriptLogger.getInstance().info("Health monitoring enabled: threshold=" + healthThreshold +
                                           ", action=" + alertAction);
        } catch (NumberFormatException e) {
            ScriptLogger.getInstance().error("Invalid threshold: " + args[1]);
        }
    }

    private void handleCheck(ScriptInterpreter interpreter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            ScriptLogger.getInstance().error("Player is null");
            return;
        }

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float healthPercent = (health / maxHealth) * 100;
        int hunger = player.getHungerManager().getFoodLevel();
        float saturation = player.getHungerManager().getSaturationLevel();
        int air = player.getAir();
        int maxAir = player.getMaxAir();
        int armor = player.getArmor();

        interpreter.setVariable("health", String.format("%.1f", health));
        interpreter.setVariable("health_max", String.format("%.1f", maxHealth));
        interpreter.setVariable("health_percent", String.format("%.1f", healthPercent));
        interpreter.setVariable("hunger", String.valueOf(hunger));
        interpreter.setVariable("saturation", String.format("%.1f", saturation));
        interpreter.setVariable("air", String.valueOf(air));
        interpreter.setVariable("air_max", String.valueOf(maxAir));
        interpreter.setVariable("armor", String.valueOf(armor));

        ScriptLogger.getInstance().info("=== Player Status ===");
        ScriptLogger.getInstance().info("Health: " + health + "/" + maxHealth + " (" + String.format("%.1f", healthPercent) + "%)");
        ScriptLogger.getInstance().info("Hunger: " + hunger + "/20 (Saturation: " + String.format("%.1f", saturation) + ")");
        ScriptLogger.getInstance().info("Air: " + air + "/" + maxAir);
        ScriptLogger.getInstance().info("Armor: " + armor);

        handleEffects(interpreter);
    }

    private void handleEffects(ScriptInterpreter interpreter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            ScriptLogger.getInstance().error("Player is null");
            return;
        }

        var effects = player.getStatusEffects();
        interpreter.setVariable("effects_count", String.valueOf(effects.size()));

        if (effects.isEmpty()) {
            ScriptLogger.getInstance().info("No active effects");
            return;
        }

        ScriptLogger.getInstance().info("=== Active Effects ===");
        int index = 0;
        for (StatusEffectInstance effect : effects) {
            String name = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).getPath();
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();

            String durationStr = formatDuration(duration);
            String amplifierStr = amplifier > 0 ? " " + (amplifier + 1) : "";

            interpreter.setVariable("effect_" + name, String.valueOf(duration));
            interpreter.setVariable("effect_" + name + "_level", String.valueOf(amplifier + 1));
            interpreter.setVariable("effect_" + index + "_name", name);
            interpreter.setVariable("effect_" + index + "_duration", String.valueOf(duration));
            interpreter.setVariable("effect_" + index + "_level", String.valueOf(amplifier + 1));

            ScriptLogger.getInstance().info("  " + name + amplifierStr + " (" + durationStr + ")");
            index++;
        }
    }

    private void handleStop(ScriptInterpreter interpreter) {
        isMonitoring = false;
        interpreter.setVariable("health_monitoring", "false");
        ScriptLogger.getInstance().info("Health monitoring stopped");
    }

    public static void tick() {
        if (!isMonitoring) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        float health = player.getHealth();

        if (health <= healthThreshold) {
            String alertKey = "low_health";
            long now = System.currentTimeMillis();
            Long lastAlert = lastAlerts.get(alertKey);

            if (lastAlert == null || (now - lastAlert) > ALERT_COOLDOWN) {
                triggerAlert("Low health: " + String.format("%.1f", health) + "/" + player.getMaxHealth());
                lastAlerts.put(alertKey, now);
            }
        }

        for (StatusEffectInstance effect : player.getStatusEffects()) {
            String name = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).getPath();

            if (isDangerousEffect(name)) {
                String alertKey = "effect_" + name;
                long now = System.currentTimeMillis();
                Long lastAlert = lastAlerts.get(alertKey);

                if (lastAlert == null || (now - lastAlert) > ALERT_COOLDOWN) {
                    triggerAlert("Dangerous effect: " + name + " (" + formatDuration(effect.getDuration()) + ")");
                    lastAlerts.put(alertKey, now);
                }
            }
        }
    }

    private static void triggerAlert(String message) {
        switch (alertAction) {
            case "log":
                ScriptLogger.getInstance().warn("[HEALTH ALERT] " + message);
                break;
            case "chat":
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(net.minecraft.text.Text.literal("§c[ALERT] §r" + message), false);
                        }
                    });
                }
                break;
            case "sound":

                ScriptLogger.getInstance().warn("[HEALTH ALERT] " + message);
                break;
        }
    }

    private static boolean isDangerousEffect(String effectName) {
        return effectName.equals("poison") ||
               effectName.equals("wither") ||
               effectName.equals("instant_damage") ||
               effectName.equals("hunger") ||
               effectName.equals("weakness") ||
               effectName.equals("slowness") ||
               effectName.equals("mining_fatigue");
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    private void printHelp() {
        ScriptLogger.getInstance().info("HealthMonitor Command:");
        ScriptLogger.getInstance().info("  healthMonitor monitor <threshold> [action]");
        ScriptLogger.getInstance().info("    - Start monitoring (actions: log, chat, sound)");
        ScriptLogger.getInstance().info("  healthMonitor check");
        ScriptLogger.getInstance().info("    - Check current status");
        ScriptLogger.getInstance().info("  healthMonitor effects");
        ScriptLogger.getInstance().info("    - List active effects");
        ScriptLogger.getInstance().info("  healthMonitor stop");
        ScriptLogger.getInstance().info("    - Stop monitoring");
    }

    public static boolean isMonitoring() {
        return isMonitoring;
    }

    public static void stop() {
        isMonitoring = false;
    }
}

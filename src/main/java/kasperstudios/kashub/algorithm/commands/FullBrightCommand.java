package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class FullBrightCommand implements Command {
    private static boolean isEnabled = false;
    private static double originalGamma = 1.0;
    private static boolean hasStoredOriginal = false;

    @Override
    public String getName() {
        return "fullbright";
    }

    @Override
    public String getDescription() {
        return "Toggles fullbright mode (max gamma)";
    }

    @Override
    public String getParameters() {
        return "[on|off|toggle] - Enable, disable, or toggle fullbright";
    }
    
    @Override
    public String getCategory() {
        return "Visual";
    }

    @Override
    public String getDetailedHelp() {
        return "Toggles fullbright mode (maximum gamma).\n\n" +
               "Usage:\n" +
               "  fullbright          - Toggle on/off\n" +
               "  fullbright on       - Enable fullbright\n" +
               "  fullbright off      - Disable fullbright\n" +
               "  fullbright toggle   - Toggle state\n\n" +
               "Aliases:\n" +
               "  on, enable, true    - Enable\n" +
               "  off, disable, false - Disable\n\n" +
               "Examples:\n" +
               "  fullbright\n" +
               "  fullbright on\n" +
               "  fullbright off\n\n" +
               "Variables set:\n" +
               "  $fullbright - Current state (true/false)\n\n" +
               "Notes:\n" +
               "  - Sets gamma to maximum (100.0)\n" +
               "  - Original gamma is restored on disable\n" +
               "  - Client-side only, safe on servers\n" +
               "  - Useful for cave exploration\n" +
               "  - Auto-disabled when script stops";
    }

    @Override
    public void execute(String[] args) {
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.options == null) {
            ScriptLogger.getInstance().error("FullBright: Options not available");
            return;
        }

        String mode = args.length > 0 ? args[0].toLowerCase() : "toggle";

        try {
            switch (mode) {
                case "on", "enable", "true" -> enableFullbright(client);
                case "off", "disable", "false" -> disableFullbright(client);
                case "toggle" -> {
                    if (isEnabled) {
                        disableFullbright(client);
                    } else {
                        enableFullbright(client);
                    }
                }
                default -> {
                    ScriptLogger.getInstance().warn("FullBright: Unknown mode '" + mode + "', using toggle");
                    if (isEnabled) {
                        disableFullbright(client);
                    } else {
                        enableFullbright(client);
                    }
                }
            }
            
            // Update variable and send feedback
            interpreter.setVariable("fullbright", String.valueOf(isEnabled));
            
            String status = isEnabled ? "§aON" : "§cOFF";
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[FullBright] " + status), false);
                }
            });
            
        } catch (Exception e) {
            ScriptLogger.getInstance().error("FullBright error: " + e.getMessage());
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[FullBright] Error: " + e.getMessage()), false);
                }
            });
        }
    }

    private void enableFullbright(MinecraftClient client) {
        if (!isEnabled) {
            client.execute(() -> {
                // Store original gamma only once
                if (!hasStoredOriginal) {
                    originalGamma = client.options.getGamma().getValue();
                    hasStoredOriginal = true;
                    ScriptLogger.getInstance().info("FullBright: Stored original gamma: " + originalGamma);
                }
                
                // Use reflection to bypass SimpleOption validation
                // Minecraft's validator rejects values > 1.0, but we need higher for fullbright
                setGammaDirectly(client.options.getGamma(), 100.0);
                ScriptLogger.getInstance().info("FullBright enabled, gamma set to: " + client.options.getGamma().getValue());
            });
            isEnabled = true;
        }
    }

    private void disableFullbright(MinecraftClient client) {
        if (isEnabled) {
            client.execute(() -> {
                // Restore original gamma
                double gammaToRestore = hasStoredOriginal ? originalGamma : 1.0;
                client.options.getGamma().setValue(gammaToRestore);
                ScriptLogger.getInstance().info("FullBright disabled, restored gamma to: " + gammaToRestore);
            });
            isEnabled = false;
        }
    }
    
    /**
     * Force disable (for cleanup on script stop)
     */
    public static void forceDisable() {
        if (isEnabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options != null) {
                client.execute(() -> {
                    double gammaToRestore = hasStoredOriginal ? originalGamma : 1.0;
                    client.options.getGamma().setValue(gammaToRestore);
                    ScriptLogger.getInstance().info("FullBright force disabled");
                });
                isEnabled = false;
            }
        }
    }
    
    /**
     * Set gamma value directly using reflection to bypass SimpleOption validation
     * This is necessary because Minecraft's validator rejects values > 1.0
     */
    private void setGammaDirectly(SimpleOption<Double> gammaOption, double value) {
        try {
            // Access the private 'value' field in SimpleOption
            Field valueField = SimpleOption.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(gammaOption, value);
            ScriptLogger.getInstance().info("Gamma set directly via reflection to: " + value);
        } catch (NoSuchFieldException e) {
            ScriptLogger.getInstance().error("Failed to find 'value' field in SimpleOption: " + e.getMessage());
            // Fallback to normal setValue (will be clamped)
            gammaOption.setValue(1.0);
        } catch (IllegalAccessException e) {
            ScriptLogger.getInstance().error("Failed to access 'value' field in SimpleOption: " + e.getMessage());
            gammaOption.setValue(1.0);
        }
    }
}

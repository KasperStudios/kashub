package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class SpeedHackCommand implements Command {
  private static boolean enabled = false;
  private static double currentMultiplier = 1.0;
  private static double originalSpeed = 0.1; // Default Minecraft walking speed
  private static boolean hasStoredOriginal = false;
  
  // Safety limits
  private static final double MIN_MULTIPLIER = 0.1;
  private static final double MAX_MULTIPLIER = 10.0;
  private static final double DEFAULT_SPEED = 0.1;

  @Override
  public String getName() {
    return "speed";
  }

  @Override
  public String getDescription() {
    return "Changes player movement speed";
  }

  @Override
  public String getParameters() {
    return "<multiplier|off> - speed multiplier (0.1-10.0) or 'off' to disable";
  }
  
  @Override
  public String getCategory() {
    return "Movement";
  }
  
  @Override
  public String getDetailedHelp() {
    return "Changes player movement speed.\n\n" +
           "Parameters:\n" +
           "  <multiplier> - Speed multiplier (0.1 - 10.0)\n" +
           "  off          - Disable and restore normal speed\n\n" +
           "Examples:\n" +
           "  speed 2.0    - Double speed\n" +
           "  speed 0.5    - Half speed\n" +
           "  speed off    - Restore normal speed\n\n" +
           "Note: Too high speed may cause issues on servers.";
  }

  @Override
  public void execute(String[] args) throws Exception {
    MinecraftClient client = MinecraftClient.getInstance();
    ClientPlayerEntity player = client.player;

    if (player == null) {
      throw new IllegalStateException("Player is null");
    }

    if (args.length > 0) {
      if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
        disable(player);
        client.execute(() -> {
          if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[Speed] §cOFF - Speed restored"), false);
          }
        });
        return;
      }
      
      try {
        double multiplier = Double.parseDouble(args[0]);
        
        // Clamp to safe limits
        if (multiplier < MIN_MULTIPLIER || multiplier > MAX_MULTIPLIER) {
          ScriptLogger.getInstance().warn("Speed multiplier " + multiplier + " out of range, clamping to " + MIN_MULTIPLIER + "-" + MAX_MULTIPLIER);
          multiplier = Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, multiplier));
        }
        
        currentMultiplier = multiplier;
        enable(player);
        
        final double finalMultiplier = multiplier;
        client.execute(() -> {
          if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[Speed] §aON - Multiplier: §f" + String.format("%.1f", finalMultiplier) + "x"), false);
          }
        });
        
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid speed multiplier: " + args[0] + ". Must be a number between " + MIN_MULTIPLIER + " and " + MAX_MULTIPLIER);
      }
    } else {
      throw new IllegalArgumentException("Usage: speed <multiplier|off>");
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

  public static void enable(ClientPlayerEntity player) {
    EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    if (attribute != null) {
      // Store original speed only once
      if (!hasStoredOriginal) {
        originalSpeed = attribute.getBaseValue();
        hasStoredOriginal = true;
        ScriptLogger.getInstance().info("Speed: Stored original speed: " + originalSpeed);
      }
      
      // Apply multiplier to original speed
      double newSpeed = originalSpeed * currentMultiplier;
      attribute.setBaseValue(newSpeed);
      enabled = true;
      ScriptLogger.getInstance().info("Speed enabled: " + currentMultiplier + "x (speed: " + newSpeed + ")");
    }
  }

  public static void disable(ClientPlayerEntity player) {
    if (enabled) {
      EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
      if (attribute != null) {
        // Restore original speed
        double speedToRestore = hasStoredOriginal ? originalSpeed : DEFAULT_SPEED;
        attribute.setBaseValue(speedToRestore);
        enabled = false;
        ScriptLogger.getInstance().info("Speed disabled, restored to: " + speedToRestore);
      }
    }
  }
  
  /**
   * Force disable (for cleanup on script stop)
   */
  public static void forceDisable() {
    if (enabled) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null) {
        disable(client.player);
        ScriptLogger.getInstance().info("Speed force disabled");
      }
    }
  }
  
  /**
   * Get current status
   */
  public static boolean isEnabled() {
    return enabled;
  }
  
  public static double getCurrentMultiplier() {
    return currentMultiplier;
  }
}
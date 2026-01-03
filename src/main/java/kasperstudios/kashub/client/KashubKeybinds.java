package kasperstudios.kashub.client;

import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.MinecraftClient;
import kasperstudios.kashub.algorithm.commands.*;

public class KashubKeybinds {
  public static KeyBinding openMenuKey;
  public static KeyBinding stopScriptsKey;
  private static boolean initialized = false;

  public static void register() {
    if (!initialized) {
      try {

        openMenuKey = new KeyBinding(
            "key.kashub.openmenu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.kashub.main"
        );

        stopScriptsKey = new KeyBinding(
            "key.kashub.stopscripts",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "category.kashub.main"
        );

        initialized = true;
        ScriptLogger.getInstance().info("Keybinds registered successfully");
      } catch (Exception e) {
        ScriptLogger.getInstance().error("Failed to register keybindings: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public static void tick() {

    if (stopScriptsKey != null && isKeyPressed(stopScriptsKey)) {
      RunToCommand.stopRunning();
      if (MinecraftClient.getInstance().player != null) {
        SpeedHackCommand.disable(MinecraftClient.getInstance().player);
      }
    }
  }

  public static boolean isKeyPressed(KeyBinding key) {
    if (key == null) return false;

    try {

      return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(),
          ((InputUtil.Key)key.getDefaultKey()).getCode());
    } catch (Exception e) {
      return false;
    }
  }

  public static KeyBinding getOpenMenuKey() {
    return openMenuKey;
  }

  public static KeyBinding getStopScriptsKey() {
    return stopScriptsKey;
  }
}
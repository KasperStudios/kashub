package kasperstudios.kashub.client;

import com.mojang.blaze3d.platform.InputConstants;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KashubKeybinds {
  private static final Logger LOGGER = LoggerFactory.getLogger(KashubKeybinds.class);
  public static KeyMapping openMenuKey;
  public static KeyMapping stopScriptsKey;
  private static boolean initialized = false;

  public static void register() {
    if (!initialized) {
      try {

        openMenuKey = new KeyMapping(
            "key.kashub.openmenu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "category.kashub.main");

        stopScriptsKey = new KeyMapping(
            "key.kashub.stopscripts",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "category.kashub.main");

        initialized = true;
        ScriptLogger.getInstance().info("Keybinds registered successfully");
      } catch (Exception e) {
        ScriptLogger.getInstance().error("Failed to register keybindings: " + e.getMessage());
        LOGGER.error("Failed to register keybindings", e);
      }
    }
  }

  public static void tick() {
    // V1 commands removed - keybind handling now in KashubClient
  }

  public static boolean isKeyPressed(KeyMapping key) {
    if (key == null)
      return false;

    try {

      return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
          ((InputConstants.Key) key.getDefaultKey()).getValue());
    } catch (Exception e) {
      return false;
    }
  }

  public static KeyMapping getOpenMenuKey() {
    return openMenuKey;
  }

  public static KeyMapping getStopScriptsKey() {
    return stopScriptsKey;
  }
}

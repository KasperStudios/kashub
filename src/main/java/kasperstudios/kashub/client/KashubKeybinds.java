package kasperstudios.kashub.client;

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
        // Создаем клавиши без регистрации через Fabric API
        openMenuKey = new KeyBinding(
            "key.kashub.openmenu", // ID кнопки
            InputUtil.Type.KEYSYM, // Тип ввода
            GLFW.GLFW_KEY_K, // Клавиша K по умолчанию
            "category.kashub.main" // Категория в настройках
        );

        stopScriptsKey = new KeyBinding(
            "key.kashub.stopscripts", // ID кнопки
            InputUtil.Type.KEYSYM, // Тип ввода
            GLFW.GLFW_KEY_Z, // Клавиша Z по умолчанию
            "category.kashub.main" // Категория в настройках
        );

        // Не используем рефлексию, так как она может вызывать ошибки
        // Просто создаем клавиши, но не регистрируем их в системе Minecraft
        // Мы будем проверять их состояние вручную

        initialized = true;
        System.out.println("Keybinds registered successfully");
      } catch (Exception e) {
        System.err.println("Failed to register keybindings: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public static void tick() {
    // Проверяем нажатие клавиш
    if (stopScriptsKey != null && isKeyPressed(stopScriptsKey)) {
      RunToCommand.stopRunning();
      if (MinecraftClient.getInstance().player != null) {
        SpeedHackCommand.disable(MinecraftClient.getInstance().player);
      }
    }
  }
  
  // Метод для проверки нажатия клавиши без использования Fabric API
  public static boolean isKeyPressed(KeyBinding key) {
    if (key == null) return false;
    
    try {
      // Проверяем, нажата ли клавиша в данный момент
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
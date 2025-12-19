package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.services.KasHubAiClient;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Команда для взаимодействия с ИИ-ассистентом
 * Использует новый KasHubAiClient с поддержкой tools
 */
public class AICommand implements Command {
    private static boolean enabled = false;
    private static final Logger LOGGER = LogManager.getLogger(AICommand.class);

    @Override
    public String getName() {
        return "ai";
    }

    @Override
    public String getDescription() {
        return "AI assistant with script manipulation tools";
    }

    @Override
    public String getParameters() {
        return "[on|off|test] or <message> - Enable/disable AI, test connection, or send message";
    }

    @Override
    public String getCategory() {
        return "AI";
    }

    @Override
    public String getDetailedHelp() {
        return "AI assistant with script manipulation tools.\n\n" +
               "Usage:\n" +
               "  ai on       - Enable AI assistant\n" +
               "  ai off      - Disable AI assistant\n" +
               "  ai test     - Test AI connection\n" +
               "  ai <message> - Send message to AI\n\n" +
               "Features:\n" +
               "  - Natural language script generation\n" +
               "  - Context-aware responses (health, position, dimension)\n" +
               "  - Script manipulation tools\n" +
               "  - In-game chat integration\n\n" +
               "Examples:\n" +
               "  ai on\n" +
               "  ai test\n" +
               "  ai How do I mine diamonds?\n" +
               "  ai Create a script to farm wheat\n" +
               "  ai off\n\n" +
               "Requirements:\n" +
               "  - AI integration must be enabled in config\n" +
               "  - Valid API key configured\n" +
               "  - Internet connection required\n\n" +
               "Notes:\n" +
               "  - Responses appear in game chat\n" +
               "  - AI can execute scripts on your behalf\n" +
               "  - Use responsibly on servers";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            KashubConfig config = KashubConfig.getInstance();
			if (!config.allowAiIntegration) {
				sendMessage("§cAI integration is disabled in config");
				return;
			}
			throw new IllegalArgumentException("Usage: ai [on|off] or ai <message>");
        }
        
        if (args[0].equalsIgnoreCase("on")) {
            enabled = true;
            sendMessage("§a[AI] Enabled");
        } else if (args[0].equalsIgnoreCase("off")) {
            enabled = false;
            sendMessage("§c[AI] Disabled");
        } else if (args[0].equalsIgnoreCase("test")) {
            testAiConnection();
        } else {
            String message = String.join(" ", args);
            processMessage(message, MinecraftClient.getInstance().player.getName().getString());
        }
    }

    /**
     * Проверяет, включен ли ИИ-ассистент
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Обрабатывает сообщение и отправляет его ИИ-ассистенту
     */
    public static void processMessage(String message, String sender) {
        CompletableFuture.runAsync(() -> {
            try {
                if (MinecraftClient.getInstance().player == null) {
                    LOGGER.error("Player is null when trying to process AI message");
                    return;
                }
                
                Map<String, String> context = new HashMap<>();
                context.put("sender", sender);
                
                // Добавляем информацию о текущем состоянии игры
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    try {
                        context.put("health", String.valueOf(client.player.getHealth()));
                        context.put("position", client.player.getPos().toString());
                        context.put("dimension", client.player.getWorld().getRegistryKey().getValue().toString());
                    } catch (Exception e) {
                        LOGGER.error("Error getting player context", e);
                    }
                }
                
                // Получаем ответ от ИИ с поддержкой tools
                try {
                    KasHubAiClient aiClient = KasHubAiClient.getInstance();
                    String response = aiClient.generateResponseWithTools(message, context);
                    if (response != null && !response.isEmpty()) {
                        // Отправляем ответ в чат (разбиваем на строки если длинный)
                        String[] lines = response.split("\n");
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                sendMessage("§b[AI] §r" + line);
                            }
                        }
                    } else {
                        sendMessage("§c[AI] §rNo response from assistant");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error generating AI response", e);
                    sendMessage("§c[AI Error] §r" + e.getMessage());
                }
            } catch (Exception e) {
                LOGGER.error("Critical error in AI processing", e);
            }
        });
    }

    /**
     * Test AI connection
     */
    private static void testAiConnection() {
        CompletableFuture.runAsync(() -> {
            try {
                sendMessage("§7[AI] Testing connection...");
                KasHubAiClient aiClient = KasHubAiClient.getInstance();
                boolean success = aiClient.testConnection();
                if (success) {
                    sendMessage("§a[AI] Connection successful!");
                } else {
                    sendMessage("§c[AI] Connection failed!");
                }
            } catch (Exception e) {
                LOGGER.error("Error testing AI connection", e);
                sendMessage("§c[AI] Connection test error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Отправляет сообщение в чат
     */
    private static void sendMessage(String message) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.execute(() -> {
                    try {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal(message), false);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error sending message to chat", e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("Critical error sending message", e);
        }
    }
} 
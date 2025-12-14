package kasperstudios.kashub.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.Map;
import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;

public class AIService {
    private static AIService instance;
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    private AIService() {
        this.apiKey = ""; // Замените на ваш ключ
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    public static AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }
    
    private String buildCommandsList() {
        StringBuilder sb = new StringBuilder();
        for (Command cmd : CommandRegistry.getCommands()) {
            sb.append("- ").append(cmd.getName())
              .append(" ").append(cmd.getParameters())
              .append(" - ").append(cmd.getDescription())
              .append("\n");
        }
        return sb.toString();
    }
    
    private String buildVariablesList() {
        StringBuilder sb = new StringBuilder();
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        for (Map.Entry<String, String> entry : interpreter.getContext().entrySet()) {
            sb.append("- $").append(entry.getKey())
              .append(" - ").append(entry.getValue())
              .append("\n");
        }
        return sb.toString();
    }
    
    public String generateResponse(String message, Map<String, String> context) {
        try {
            // Формируем промпт с контекстом
            StringBuilder prompt = new StringBuilder();
            prompt.append("Ты - ИИ-ассистент в Minecraft с возможностью писать и выполнять код на KHScript.\n\n");
            prompt.append("Правила:\n");
            prompt.append("1. Отвечай кратко и по существу\n");
            prompt.append("2. Используй информацию о состоянии игры для контекстных ответов\n");
            prompt.append("3. Если игрок просит написать код - напиши его на KHScript\n");
            prompt.append("4. Если игрок просит выполнить код - выполни его\n");
            prompt.append("5. Не используй нецензурную лексику\n");
            prompt.append("6. Если не знаешь ответа - честно признайся\n\n");
            
            prompt.append("Доступные команды KHScript:\n");
            prompt.append(buildCommandsList()).append("\n");
            
            prompt.append("Переменные окружения:\n");
            prompt.append(buildVariablesList()).append("\n");
            
            prompt.append("Информация о текущем состоянии:\n");
            prompt.append("Игрок: ").append(context.get("sender")).append("\n");
            prompt.append("Сообщение: ").append(message).append("\n");
            prompt.append("Контекст игры:\n");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                if (!entry.getKey().equals("sender") && !entry.getKey().equals("message")) {
                    prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            prompt.append("\nОтветь на сообщение игрока. Если нужно написать или выполнить код - сделай это.");
            
            // Формируем JSON запрос
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "mixtral-8x7b-32768");
            
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("role", "user");
            messageObj.addProperty("content", prompt.toString());
            
            JsonObject[] messages = new JsonObject[]{messageObj};
            requestBody.add("messages", gson.toJsonTree(messages));
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 1000);
            
            // Создаем HTTP запрос
            Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                    MediaType.parse("application/json"),
                    gson.toJson(requestBody)
                ))
                .build();
            
            // Отправляем запрос
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response);
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                // Извлекаем ответ из JSON
                return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Произошла ошибка при генерации ответа: " + e.getMessage();
        }
    }
} 
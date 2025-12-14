package kasperstudios.kashub.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kasperstudios.kashub.algorithm.CommandRegistry;
import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptLogger;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Multi-provider AI client for KasHub Agent.
 * Supports Groq, MegaLLM, and custom OpenAI-compatible endpoints.
 */
public class KasHubAiClient {
    private static KasHubAiClient instance;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    // Provider base URLs
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String MEGALLM_BASE_URL = "https://ai.megallm.io/v1";
    
    // Cached models list
    private final List<String> availableModels = new ArrayList<>();
    private long modelsLastFetched = 0;
    private static final long MODELS_CACHE_TTL = 3600000; // 1 hour
    
    private KasHubAiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    public static KasHubAiClient getInstance() {
        if (instance == null) {
            instance = new KasHubAiClient();
        }
        return instance;
    }
    
    /**
     * Get base URL for current provider
     */
    private String getBaseUrl() {
        KashubConfig config = KashubConfig.getInstance();
        
        // If custom URL is set, use it
        if (!config.aiBaseUrl.isEmpty()) {
            return config.aiBaseUrl;
        }
        
        // Otherwise use provider default
        return switch (config.aiProvider) {
            case GROQ -> GROQ_BASE_URL;
            case MEGALLM -> MEGALLM_BASE_URL;
            case CUSTOM -> config.aiBaseUrl;
            default -> "";
        };
    }
    
    /**
     * Fetch available models from provider
     */
    public List<String> fetchAvailableModels() throws IOException {
        KashubConfig config = KashubConfig.getInstance();
        
        if (config.aiProvider == KashubConfig.AiProvider.OFF) {
            return Collections.emptyList();
        }
        
        // Check cache
        long now = System.currentTimeMillis();
        if (!availableModels.isEmpty() && (now - modelsLastFetched) < MODELS_CACHE_TTL) {
            return new ArrayList<>(availableModels);
        }
        
        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            throw new IOException("AI provider base URL not configured");
        }
        
        Request request = new Request.Builder()
                .url(baseUrl + "/models")
                .addHeader("Authorization", "Bearer " + config.aiApiKey)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch models: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            availableModels.clear();
            JsonArray data = jsonResponse.getAsJsonArray("data");
            for (int i = 0; i < data.size(); i++) {
                JsonObject model = data.get(i).getAsJsonObject();
                String modelId = model.get("id").getAsString();
                availableModels.add(modelId);
            }
            
            modelsLastFetched = now;
            ScriptLogger.getInstance().info("Fetched " + availableModels.size() + " models from " + config.aiProvider);
            
            return new ArrayList<>(availableModels);
        }
    }
    
    /**
     * Get model to use (from config or first available)
     */
    private String getModel() throws IOException {
        KashubConfig config = KashubConfig.getInstance();
        
        if (config.aiModel != null && !config.aiModel.isEmpty()) {
            return config.aiModel;
        }
        
        List<String> models = fetchAvailableModels();
        if (models.isEmpty()) {
            throw new IOException("No models available from provider. Please configure aiModel manually.");
        }
        
        String selected = models.get(0);
        ScriptLogger.getInstance().info("Auto-selected model: " + selected);
        return selected;
    }
    
    /**
     * Build system prompt with comprehensive KHScript context
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are KasHub Agent - a specialized AI assistant for the Kashub Minecraft mod.\n");
        prompt.append("Your mission: Help players create, debug, and optimize KHScript automation scripts.\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## YOUR CAPABILITIES\n\n");
        
        prompt.append("✅ SCRIPT CREATION & GENERATION\n");
        prompt.append("- Write complete, production-ready KHScript automation scripts\n");
        prompt.append("- Generate mining bots, farming automation, auto-trade systems\n");
        prompt.append("- Create PvP assist scripts, monster hunters, resource collectors\n");
        prompt.append("- Build complex multi-tool automation workflows\n\n");
        
        prompt.append("✅ DEBUGGING & OPTIMIZATION\n");
        prompt.append("- Identify logic errors and syntax mistakes\n");
        prompt.append("- Optimize script performance (pathfinding, scanning efficiency)\n");
        prompt.append("- Explain why scripts fail and how to fix them\n");
        prompt.append("- Suggest better approaches and design patterns\n\n");
        
        prompt.append("✅ COMMAND & SYNTAX EXPERTISE\n");
        prompt.append("- Know all available commands and their parameters\n");
        prompt.append("- Explain environment variables and how to use them\n");
        prompt.append("- Understand complex control flow and logic\n");
        prompt.append("- Create reusable functions and modular code\n\n");
        
        prompt.append("✅ PRACTICAL EXAMPLES\n");
        prompt.append("- Mining automation with avoidDanger=true pathfinding\n");
        prompt.append("- Auto-farming mobs, shulkers, specific creatures\n");
        prompt.append("- Auto-crafting with inventory management\n");
        prompt.append("- PvP scripts with target tracking and arrow prediction\n");
        prompt.append("- Auto-trading and item sorting systems\n");
        prompt.append("- Shulker bullet dodging and auto-shooting\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## KHScript LANGUAGE REFERENCE\n\n");
        
        prompt.append("### Variables (Rust/JS style)\n");
        prompt.append("  let x = 5              // Mutable variable\n");
        prompt.append("  const MAX = 100        // Immutable constant\n");
        prompt.append("  x = 10                 // Legacy assignment\n");
        prompt.append("  x = x + 1              // Math operations\n\n");
        
        prompt.append("### Control Flow\n");
        prompt.append("  if (condition) { ... } else if (cond2) { ... } else { ... }\n");
        prompt.append("  while (condition) { ... }\n");
        prompt.append("  for (let i = 0; i < 10; i++) { ... }\n");
        prompt.append("  loop 5 { ... }         // Repeat exactly 5 times\n");
        prompt.append("  condition ? true_val : false_val  // Ternary\n\n");
        
        prompt.append("### Operators\n");
        prompt.append("  Arithmetic: + - * / %\n");
        prompt.append("  Comparison: == != < > <= >=\n");
        prompt.append("  Logical: && || !\n");
        prompt.append("  Priority: * / % > + - > comparisons > &&/||\n\n");
        
        prompt.append("### Functions\n");
        prompt.append("  function hunterLoop(radius, delay) {\n");
        prompt.append("    // function body\n");
        prompt.append("    vision nearest hostile \" + radius + \"\n");
        prompt.append("    ...\n");
        prompt.append("  }\n");
        prompt.append("  hunterLoop(64, 500)  // Call with arguments\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## AVAILABLE COMMANDS\n\n");
        
        for (Command cmd : CommandRegistry.getAllCommands()) {
            prompt.append("◆ ").append(cmd.getName());
            
            String params = cmd.getParameters();
            if (params != null && !params.isEmpty()) {
                prompt.append(" ").append(params);
            }
            prompt.append("\n");
            
            String desc = cmd.getDescription();
            if (desc != null && !desc.isEmpty()) {
                prompt.append("  └─ ").append(desc).append("\n");
            }
            
            String detailed = cmd.getDetailedHelp();
            if (detailed != null && !detailed.isEmpty()) {
                prompt.append("  ").append(detailed.replace("\n", "\n  ")).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## ENVIRONMENT VARIABLES (Read-only - updated every tick)\n\n");
        
        prompt.append("Player Status:\n");
        prompt.append("  $PLAYER_NAME, $PLAYER_X, $PLAYER_Y, $PLAYER_Z\n");
        prompt.append("  $PLAYER_YAW, $PLAYER_PITCH (Head rotation)\n");
        prompt.append("  $PLAYER_HEALTH, $PLAYER_FOOD, $PLAYER_LEVEL\n");
        prompt.append("  $PLAYER_SPEED, $PLAYER_XP, $GAME_MODE\n");
        prompt.append("  $IS_SNEAKING, $IS_SPRINTING\n\n");
        
        prompt.append("World Status:\n");
        prompt.append("  $WORLD_TIME, $WORLD_DAY, $WORLD_WEATHER\n");
        prompt.append("  $WORLD_DIFFICULTY, $DIMENSION\n\n");
        
        prompt.append("Vision/Scan Results:\n");
        prompt.append("  $scan_count, $nearest_found, $nearest_x/y/z\n");
        prompt.append("  $vision_count, $distance, $health (for targets)\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## COMMON PATTERNS & EXAMPLES\n\n");
        
        prompt.append("### Pattern 1: Hunt nearest mob in radius\n");
        prompt.append("  vision scan 80 zombie\n");
        prompt.append("  if (scan_count > 0) {\n");
        prompt.append("    vision nearest zombie 80\n");
        prompt.append("    pathfind $nearest_x $nearest_y $nearest_z\n");
        prompt.append("    attack 6\n");
        prompt.append("  }\n\n");
        
        prompt.append("### Pattern 2: Auto-farm with safety check\n");
        prompt.append("  while (PLAYER_HEALTH > 5) {\n");
        prompt.append("    vision nearest shulker 40\n");
        prompt.append("    if (nearest_found) {\n");
        prompt.append("      attack 6\n");
        prompt.append("    } else {\n");
        prompt.append("      wait 500\n");
        prompt.append("    }\n");
        prompt.append("  }\n\n");
        
        prompt.append("### Pattern 3: Multi-command sequence\n");
        prompt.append("  selectSlot 1\n");
        prompt.append("  wait 100\n");
        prompt.append("  attack 6\n");
        prompt.append("  wait 500\n");
        prompt.append("  input attack\n");
        prompt.append("  wait 200\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## RULES & CONSTRAINTS\n\n");
        
        prompt.append("⚠️  ONE COMMAND PER LINE - No multiple commands on same line\n");
        prompt.append("⚠️  Comments use // and count as 1 line - Put comments alone\n");
        prompt.append("⚠️  Pathfinding needs time - Always use wait() between pathfind calls\n");
        prompt.append("⚠️  Vision updates tick-based - Scan, wait, then read $nearest_*\n");
        prompt.append("⚠️  Coordinates are doubles - Use $PLAYER_X format, not hardcoded\n");
        prompt.append("⚠️  Health check before combat - if ($PLAYER_HEALTH > 5)\n\n");
        
        prompt.append("═══════════════════════════════════════════════════════════════\n");
        prompt.append("## WHEN RESPONDING\n\n");
        
        prompt.append("✓ Explain WHAT the script does (human-readable)\n");
        prompt.append("✓ Explain WHY it works that way\n");
        prompt.append("✓ Provide working, tested code examples\n");
        prompt.append("✓ Include safety checks and error handling\n");
        prompt.append("✓ Suggest optimizations and best practices\n");
        prompt.append("✓ Answer follow-up questions about syntax and logic\n\n");
        
        prompt.append("✗ Don't just dump code without explanation\n");
        prompt.append("✗ Don't create untested or broken scripts\n");
        prompt.append("✗ Don't ignore player safety (health checks, avoidDanger)\n");
        prompt.append("✗ Don't forget about timing and delays\n");
        prompt.append("✗ Don't mix up command names or parameters\n\n");
        
        return prompt.toString();
    }
    
    /**
     * Generate AI response with chat completion
     */
    public String generateResponse(String userMessage, Map<String, String> context) throws IOException {
        KashubConfig config = KashubConfig.getInstance();
        
        if (config.aiProvider == KashubConfig.AiProvider.OFF) {
            throw new IllegalStateException("AI provider is disabled");
        }
        
        if (config.aiApiKey.isEmpty()) {
            throw new IllegalStateException("AI API key not configured");
        }
        
        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            throw new IllegalStateException("AI base URL not configured");
        }
        
        // Build messages
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", buildSystemPrompt());
        messages.add(systemMsg);
        
        // Add context if provided
        if (context != null && !context.isEmpty()) {
            StringBuilder contextStr = new StringBuilder("Current context:\n");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                contextStr.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            JsonObject contextMsg = new JsonObject();
            contextMsg.addProperty("role", "system");
            contextMsg.addProperty("content", contextStr.toString());
            messages.add(contextMsg);
        }
        
        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        
        // Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", getModel());
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", config.aiTemperature);
        requestBody.addProperty("max_tokens", config.aiMaxTokens);
        
        // Make request
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + config.aiApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        gson.toJson(requestBody)
                ))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("AI request failed: " + response.code() + " " + response.message() + "\n" + errorBody);
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            // Extract response
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                throw new IOException("No response from AI");
            }
            
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            String content = message.get("content").getAsString();
            
            ScriptLogger.getInstance().info("AI response generated (" + content.length() + " chars)");
            
            return content;
        }
    }
    
    /**
     * Test connection to AI provider
     */
    public boolean testConnection() {
        try {
            fetchAvailableModels();
            return true;
        } catch (Exception e) {
            ScriptLogger.getInstance().error("AI connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== AI Tools ====================
    
    /**
     * Generate response with tool support and iterative tool calling
     * AI can call tools multiple times to complete complex tasks
     */
    public String generateResponseWithTools(String userMessage, Map<String, String> context) throws IOException {
        KashubConfig config = KashubConfig.getInstance();
        
        if (!config.aiEnableTools) {
            return generateResponse(userMessage, context);
        }
        
        JsonArray tools = buildToolsDefinition();
        JsonArray messages = new JsonArray();
        
        // System message with enhanced tool usage instructions
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        String enhancedPrompt = buildSystemPrompt() + "\n\n" +
            "## CRITICAL: Tool Usage Instructions\n" +
            "You have access to tools for script manipulation. YOU MUST USE THESE TOOLS.\n\n" +
            "IMPORTANT RULES:\n" +
            "1. NEVER output code directly to the user - they CANNOT copy it from chat\n" +
            "2. ALWAYS use write_script tool to create or modify scripts\n" +
            "3. Use read_script to check existing scripts before modifying\n" +
            "4. Use list_scripts to see available scripts\n" +
            "5. Use run_script to execute scripts after creating them\n" +
            "6. You can call multiple tools in sequence to complete complex tasks\n" +
            "7. After using tools, explain what you did in simple terms\n\n" +
            "Example workflow:\n" +
            "User: 'Create a mining script'\n" +
            "1. Call write_script with the script content\n" +
            "2. Respond: 'I created mining.kh script that does X, Y, Z'\n\n" +
            "DO NOT just describe what the script should contain - CREATE IT using tools!";
        systemMsg.addProperty("content", enhancedPrompt);
        messages.add(systemMsg);
        
        // Add context
        if (context != null && !context.isEmpty()) {
            StringBuilder contextStr = new StringBuilder("Current context:\n");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                contextStr.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            JsonObject contextMsg = new JsonObject();
            contextMsg.addProperty("role", "system");
            contextMsg.addProperty("content", contextStr.toString());
            messages.add(contextMsg);
        }
        
        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        
        // Iterative tool calling (max 5 iterations)
        StringBuilder allToolResults = new StringBuilder();
        int maxIterations = 5;
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", getModel());
            requestBody.add("messages", messages);
            requestBody.add("tools", tools);
            requestBody.addProperty("temperature", config.aiTemperature);
            requestBody.addProperty("max_tokens", config.aiMaxTokens);
            
            String baseUrl = getBaseUrl();
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.aiApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json"),
                            gson.toJson(requestBody)
                    ))
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("AI request failed: " + response.code() + " " + response.message() + "\n" + errorBody);
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                
                if (choices == null || choices.size() == 0) {
                    throw new IOException("No response from AI");
                }
                
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                
                // Add assistant message to history
                messages.add(message);
                
                // Check if AI wants to call tools
                if (message.has("tool_calls")) {
                    JsonArray toolCalls = message.getAsJsonArray("tool_calls");
                    
                    for (int i = 0; i < toolCalls.size(); i++) {
                        JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
                        String toolCallId = toolCall.get("id").getAsString();
                        JsonObject function = toolCall.getAsJsonObject("function");
                        String functionName = function.get("name").getAsString();
                        String arguments = function.get("arguments").getAsString();
                        
                        ScriptLogger.getInstance().info("AI calling tool: " + functionName);
                        String result = executeTool(functionName, arguments);
                        
                        allToolResults.append("🔧 Used tool: ").append(functionName).append("\n");
                        allToolResults.append("   Result: ").append(result).append("\n\n");
                        
                        // Add tool result to messages
                        JsonObject toolResultMsg = new JsonObject();
                        toolResultMsg.addProperty("role", "tool");
                        toolResultMsg.addProperty("tool_call_id", toolCallId);
                        toolResultMsg.addProperty("content", result);
                        messages.add(toolResultMsg);
                    }
                    
                    // Continue to next iteration to let AI respond to tool results
                    continue;
                }
                
                // No more tool calls, AI has final response
                String finalContent = message.has("content") && !message.get("content").isJsonNull() 
                    ? message.get("content").getAsString() 
                    : "";
                
                // Combine tool results with final response
                if (allToolResults.length() > 0) {
                    return allToolResults.toString() + "\n" + finalContent;
                }
                return finalContent;
            }
        }
        
        // Max iterations reached
        return allToolResults.toString() + "\n(Reached maximum tool iterations)";
    }
    
    /**
     * Build tools definition for AI
     */
    private JsonArray buildToolsDefinition() {
        JsonArray tools = new JsonArray();
        
        // Tool 1: list_scripts
        JsonObject listScriptsTool = new JsonObject();
        listScriptsTool.addProperty("type", "function");
        JsonObject listScriptsFunc = new JsonObject();
        listScriptsFunc.addProperty("name", "list_scripts");
        listScriptsFunc.addProperty("description", "List all available KHScript files");
        JsonObject listScriptsParams = new JsonObject();
        listScriptsParams.addProperty("type", "object");
        listScriptsParams.add("properties", new JsonObject());
        listScriptsFunc.add("parameters", listScriptsParams);
        listScriptsTool.add("function", listScriptsFunc);
        tools.add(listScriptsTool);
        
        // Tool 2: read_script
        JsonObject readScriptTool = new JsonObject();
        readScriptTool.addProperty("type", "function");
        JsonObject readScriptFunc = new JsonObject();
        readScriptFunc.addProperty("name", "read_script");
        readScriptFunc.addProperty("description", "Read contents of a KHScript file");
        JsonObject readScriptParams = new JsonObject();
        readScriptParams.addProperty("type", "object");
        JsonObject readScriptProps = new JsonObject();
        JsonObject scriptNameProp = new JsonObject();
        scriptNameProp.addProperty("type", "string");
        scriptNameProp.addProperty("description", "Name of the script file (without .kh extension)");
        readScriptProps.add("script_name", scriptNameProp);
        readScriptParams.add("properties", readScriptProps);
        JsonArray readScriptRequired = new JsonArray();
        readScriptRequired.add("script_name");
        readScriptParams.add("required", readScriptRequired);
        readScriptFunc.add("parameters", readScriptParams);
        readScriptTool.add("function", readScriptFunc);
        tools.add(readScriptTool);
        
        // Tool 3: write_script
        JsonObject writeScriptTool = new JsonObject();
        writeScriptTool.addProperty("type", "function");
        JsonObject writeScriptFunc = new JsonObject();
        writeScriptFunc.addProperty("name", "write_script");
        writeScriptFunc.addProperty("description", "Write or update a KHScript file");
        JsonObject writeScriptParams = new JsonObject();
        writeScriptParams.addProperty("type", "object");
        JsonObject writeScriptProps = new JsonObject();
        JsonObject scriptNameProp2 = new JsonObject();
        scriptNameProp2.addProperty("type", "string");
        scriptNameProp2.addProperty("description", "Name of the script file (without .kh extension)");
        writeScriptProps.add("script_name", scriptNameProp2);
        JsonObject contentProp = new JsonObject();
        contentProp.addProperty("type", "string");
        contentProp.addProperty("description", "Content of the script");
        writeScriptProps.add("content", contentProp);
        writeScriptParams.add("properties", writeScriptProps);
        JsonArray writeScriptRequired = new JsonArray();
        writeScriptRequired.add("script_name");
        writeScriptRequired.add("content");
        writeScriptParams.add("required", writeScriptRequired);
        writeScriptFunc.add("parameters", writeScriptParams);
        writeScriptTool.add("function", writeScriptFunc);
        tools.add(writeScriptTool);
        
        // Tool 4: run_script
        JsonObject runScriptTool = new JsonObject();
        runScriptTool.addProperty("type", "function");
        JsonObject runScriptFunc = new JsonObject();
        runScriptFunc.addProperty("name", "run_script");
        runScriptFunc.addProperty("description", "Execute a KHScript file");
        JsonObject runScriptParams = new JsonObject();
        runScriptParams.addProperty("type", "object");
        JsonObject runScriptProps = new JsonObject();
        JsonObject scriptNameProp3 = new JsonObject();
        scriptNameProp3.addProperty("type", "string");
        scriptNameProp3.addProperty("description", "Name of the script file (without .kh extension)");
        runScriptProps.add("script_name", scriptNameProp3);
        runScriptParams.add("properties", runScriptProps);
        JsonArray runScriptRequired = new JsonArray();
        runScriptRequired.add("script_name");
        runScriptParams.add("required", runScriptRequired);
        runScriptFunc.add("parameters", runScriptParams);
        runScriptTool.add("function", runScriptFunc);
        tools.add(runScriptTool);
        
        return tools;
    }
    
    /**
     * Execute a tool call from AI
     */
    private String executeTool(String toolName, String argumentsJson) {
        try {
            JsonObject args = gson.fromJson(argumentsJson, JsonObject.class);
            
            switch (toolName) {
                case "list_scripts":
                    return toolListScripts();
                case "read_script":
                    String scriptName = args.get("script_name").getAsString();
                    return toolReadScript(scriptName);
                case "write_script":
                    String name = args.get("script_name").getAsString();
                    String content = args.get("content").getAsString();
                    return toolWriteScript(name, content);
                case "run_script":
                    String runName = args.get("script_name").getAsString();
                    return toolRunScript(runName);
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            return "Tool execution error: " + e.getMessage();
        }
    }
    
    private String toolListScripts() {
        try {
            java.nio.file.Path scriptsDir = java.nio.file.Paths.get("config/kashub/scripts");
            if (!java.nio.file.Files.exists(scriptsDir)) {
                return "No scripts directory found";
            }
            
            StringBuilder result = new StringBuilder("Available scripts:\n");
            java.nio.file.Files.list(scriptsDir)
                    .filter(p -> p.toString().endsWith(".kh"))
                    .forEach(p -> result.append("- ").append(p.getFileName().toString().replace(".kh", "")).append("\n"));
            
            return result.toString();
        } catch (Exception e) {
            return "Error listing scripts: " + e.getMessage();
        }
    }
    
    private String toolReadScript(String scriptName) {
        try {
            java.nio.file.Path scriptPath = java.nio.file.Paths.get("config/kashub/scripts/" + scriptName + ".kh");
            if (!java.nio.file.Files.exists(scriptPath)) {
                return "Script not found: " + scriptName;
            }
            
            String content = new String(java.nio.file.Files.readAllBytes(scriptPath));
            return "Script content:\n```\n" + content + "\n```";
        } catch (Exception e) {
            return "Error reading script: " + e.getMessage();
        }
    }
    
    private String toolWriteScript(String scriptName, String content) {
        try {
            java.nio.file.Path scriptsDir = java.nio.file.Paths.get("config/kashub/scripts");
            java.nio.file.Files.createDirectories(scriptsDir);
            
            java.nio.file.Path scriptPath = scriptsDir.resolve(scriptName + ".kh");
            java.nio.file.Files.write(scriptPath, content.getBytes());
            
            return "Script saved: " + scriptName + ".kh";
        } catch (Exception e) {
            return "Error writing script: " + e.getMessage();
        }
    }
    
    private String toolRunScript(String scriptName) {
        try {
            kasperstudios.kashub.runtime.ScriptTaskManager taskManager = kasperstudios.kashub.runtime.ScriptTaskManager.getInstance();
            
            // Start script from file
            kasperstudios.kashub.runtime.ScriptTask task = taskManager.startScriptFromFile(scriptName);
            
            if (task == null) {
                return "Failed to start script: " + scriptName + " (script execution may be disabled)";
            }
            
            return "Script started: " + scriptName + " (Task ID: " + task.getId() + ")";
        } catch (Exception e) {
            return "Error running script: " + e.getMessage();
        }
    }
}

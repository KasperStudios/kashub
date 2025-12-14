package kasperstudios.kashub.gui;

import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.services.KasHubAiClient;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI Agent Chat GUI Screen with Settings
 * Accessible via keybind, allows chat with AI and configuration
 */
public class AiAgentScreen extends Screen {
    private int chatWidth;
    private int chatHeight;
    private static final int INPUT_HEIGHT = 30;
    private static final int PADDING = 10;
    private static final int TAB_HEIGHT = 25;
    
    // Tabs
    private enum Tab { CHAT, SETTINGS }
    private Tab currentTab = Tab.CHAT;
    
    // Chat tab
    private TextFieldWidget inputField;
    private ButtonWidget sendButton;
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private boolean isLoading = false;
    private String loadingMessage = "";
    private int chatScrollOffset = 0;
    
    // Settings tab
    private TextFieldWidget apiKeyField;
    private TextFieldWidget modelField;
    private TextFieldWidget customUrlField;
    private ButtonWidget saveSettingsButton;
    private ButtonWidget baseUrlLabel;
    private String selectedProvider = "";
    private List<String> availableModels = new ArrayList<>();
    private int modelScrollOffset = 0;
    
    public AiAgentScreen() {
        super(Text.literal("AI Agent"));
    }
    
    @Override
    protected void init() {
        this.clearChildren();
        
        // Calculate adaptive sizes (80% of screen, but max 600x400)
        this.chatWidth = Math.min((int)(this.width * 0.8), 600);
        this.chatHeight = Math.min((int)(this.height * 0.8), 400);
        
        // Ensure minimum size
        this.chatWidth = Math.max(this.chatWidth, 400);
        this.chatHeight = Math.max(this.chatHeight, 300);
        
        if (currentTab == Tab.CHAT) {
            initChatTab();
        } else {
            initSettingsTab();
        }
        
        // Tab buttons (always visible)
        int windowX = this.width / 2 - chatWidth / 2;
        int windowY = this.height / 2 - chatHeight / 2;
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Chat"), button -> switchTab(Tab.CHAT))
            .dimensions(windowX + PADDING, 
                       windowY - TAB_HEIGHT - PADDING,
                       60, TAB_HEIGHT)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), button -> switchTab(Tab.SETTINGS))
            .dimensions(windowX + 80, 
                       windowY - TAB_HEIGHT - PADDING,
                       70, TAB_HEIGHT)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
            .dimensions(windowX + chatWidth - PADDING - 60,
                       windowY - TAB_HEIGHT - PADDING,
                       60, TAB_HEIGHT)
            .build());
    }
    
    private void initChatTab() {
        int windowX = this.width / 2 - chatWidth / 2;
        int windowY = this.height / 2 - chatHeight / 2;
        
        // Input field
        this.inputField = new TextFieldWidget(this.textRenderer,
            windowX + PADDING,
            windowY + chatHeight - INPUT_HEIGHT - PADDING,
            chatWidth - 2 * PADDING - 80,
            INPUT_HEIGHT - 4,
            Text.literal("Message..."));
        this.inputField.setMaxLength(500);
        this.addDrawableChild(this.inputField);
        
        // Send button
        this.sendButton = ButtonWidget.builder(Text.literal("Send"), button -> sendMessage())
            .dimensions(
                windowX + chatWidth - PADDING - 70,
                windowY + chatHeight - INPUT_HEIGHT - PADDING,
                65,
                INPUT_HEIGHT - 4
            )
            .build();
        this.addDrawableChild(this.sendButton);
        
        // Add initial greeting
        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatMessage("AI Agent", "Hello! I'm your KHScript AI assistant. Ask me anything about scripting or use me to create and manage scripts.", false));
        }
    }
    
    private void initSettingsTab() {
        KashubConfig config = KashubConfig.getInstance();
        int windowX = this.width / 2 - chatWidth / 2;
        int windowY = this.height / 2 - chatHeight / 2;
        int startY = windowY + 30;
        int labelWidth = 80;
        int fieldWidth = chatWidth - labelWidth - 3 * PADDING;
        
        // Provider selection label
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Provider: " + config.aiProvider), button -> {})
            .dimensions(windowX + PADDING, startY, 150, 20)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Groq"), button -> {
            config.aiProvider = KashubConfig.AiProvider.GROQ;
            config.save();
            updateUrlFieldState();
        })
            .dimensions(windowX + 160, startY, 50, 20)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("MegaLLM"), button -> {
            config.aiProvider = KashubConfig.AiProvider.MEGALLM;
            config.save();
            updateUrlFieldState();
        })
            .dimensions(windowX + 220, startY, 60, 20)
            .build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Custom"), button -> {
            config.aiProvider = KashubConfig.AiProvider.CUSTOM;
            config.save();
            updateUrlFieldState();
        })
            .dimensions(windowX + 290, startY, 60, 20)
            .build());
        
        // API Key field
        startY += 35;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("API Key:"), button -> {})
            .dimensions(windowX + PADDING, startY, labelWidth, 20)
            .build());
        
        this.apiKeyField = new TextFieldWidget(this.textRenderer,
            windowX + labelWidth + PADDING,
            startY,
            fieldWidth,
            20,
            Text.literal("API Key"));
        this.apiKeyField.setText(config.aiApiKey);
        this.apiKeyField.setMaxLength(500);
        this.addDrawableChild(this.apiKeyField);
        
        // Model field
        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Model:"), button -> {})
            .dimensions(windowX + PADDING, startY, labelWidth, 20)
            .build());
        
        this.modelField = new TextFieldWidget(this.textRenderer,
            windowX + labelWidth + PADDING,
            startY,
            fieldWidth,
            20,
            Text.literal("Model"));
        this.modelField.setText(config.aiModel);
        this.modelField.setMaxLength(100);
        this.addDrawableChild(this.modelField);
        
        // Custom URL field (for CUSTOM provider only)
        startY += 30;
        boolean isCustomProvider = config.aiProvider == KashubConfig.AiProvider.CUSTOM;
        
        if (isCustomProvider) {
            this.baseUrlLabel = this.addDrawableChild(ButtonWidget.builder(Text.literal("Base URL:"), button -> {})
                .dimensions(windowX + PADDING, startY, labelWidth, 20)
                .build());
            
            this.customUrlField = new TextFieldWidget(this.textRenderer,
                windowX + labelWidth + PADDING,
                startY,
                fieldWidth,
                20,
                Text.literal("https://api.example.com/v1"));
            this.customUrlField.setText(config.aiBaseUrl);
            this.customUrlField.setMaxLength(500);
            this.customUrlField.setEditable(true);
            this.addDrawableChild(this.customUrlField);
        } else {
            this.customUrlField = null;
            this.baseUrlLabel = null;
            startY -= 30; // Compensate for skipped field
        }
        
        // Model selection buttons
        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fetch Models"), button -> fetchModels())
            .dimensions(windowX + PADDING, startY, 100, 20)
            .build());
        
        // Display available models
        if (!availableModels.isEmpty()) {
            startY += 25;
            int maxModelsToShow = 3;
            for (int i = modelScrollOffset; i < Math.min(availableModels.size(), modelScrollOffset + maxModelsToShow); i++) {
                String model = availableModels.get(i);
                this.addDrawableChild(ButtonWidget.builder(Text.literal(model), button -> {
                    this.modelField.setText(model);
                })
                    .dimensions(windowX + PADDING, startY, chatWidth - 2 * PADDING, 20)
                    .build());
                startY += 22;
            }
        }
        
        // Save button
        this.saveSettingsButton = ButtonWidget.builder(Text.literal("Save Settings"), button -> saveSettings())
            .dimensions(windowX + chatWidth - PADDING - 120, startY, 120, 20)
            .build();
        this.addDrawableChild(this.saveSettingsButton);
    }
    
    private void switchTab(Tab tab) {
        this.currentTab = tab;
        this.init();
    }
    
    private void updateUrlFieldState() {
        // Re-initialize settings tab to show/hide Base URL field
        if (currentTab == Tab.SETTINGS) {
            this.init();
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent dark background (no blur)
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        
        // Draw window background
        int x = this.width / 2 - chatWidth / 2;
        int y = this.height / 2 - chatHeight / 2;
        
        context.fill(x, y - TAB_HEIGHT - PADDING, x + chatWidth, y + chatHeight, 0xFF1a1a1a);
        context.drawBorder(x, y - TAB_HEIGHT - PADDING, chatWidth, chatHeight + TAB_HEIGHT + PADDING, 0xFF00ff00);
        
        // Draw tab title
        context.drawText(this.textRenderer, "AI Agent - " + currentTab.name(), x + PADDING, y - TAB_HEIGHT - PADDING + 5, 0xFF00ff00, false);
        
        super.render(context, mouseX, mouseY, delta);
        
        if (currentTab == Tab.CHAT) {
            drawChatMessages(context, x, y);
            if (isLoading) {
                drawLoadingIndicator(context, x + chatWidth / 2, y + chatHeight - 50);
            }
        }
    }
    
    private void drawChatMessages(DrawContext context, int x, int y) {
        int messageY = y + PADDING;
        int maxY = y + chatHeight - INPUT_HEIGHT - 2 * PADDING;
        
        // Calculate total messages to show with scroll
        int startIndex = Math.max(0, chatScrollOffset);
        int endIndex = Math.min(chatHistory.size(), startIndex + 20);
        
        for (int i = startIndex; i < endIndex; i++) {
            ChatMessage msg = chatHistory.get(i);
            
            if (messageY > maxY) break;
            
            int color = msg.isUser ? 0xFF00ff00 : (msg.isSystem ? 0xFFffaa00 : 0xFF00ccff);
            context.drawText(this.textRenderer, msg.sender + ":", x + PADDING, messageY, color, false);
            messageY += 10;
            
            List<String> lines = wrapText(msg.content, chatWidth - 2 * PADDING - 10);
            for (String line : lines) {
                if (messageY > maxY) break;
                context.drawText(this.textRenderer, line, x + PADDING + 10, messageY, 0xFFcccccc, false);
                messageY += 10;
            }
            
            messageY += 5;
        }
        
        // Draw scroll indicator if needed
        if (chatHistory.size() > 20) {
            String scrollInfo = "[" + (startIndex + 1) + "-" + endIndex + "/" + chatHistory.size() + "]";
            context.drawText(this.textRenderer, scrollInfo, x + chatWidth - PADDING - 60, y + PADDING, 0xFF888888, false);
        }
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (this.textRenderer.getWidth(currentLine + word) > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(word);
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    private void drawLoadingIndicator(DrawContext context, int x, int y) {
        context.drawText(this.textRenderer, "⏳ " + loadingMessage, x - 50, y, 0xFFffff00, false);
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        
        chatHistory.add(new ChatMessage("You", message, true));
        inputField.setText("");
        
        isLoading = true;
        loadingMessage = "Thinking...";
        
        CompletableFuture.runAsync(() -> {
            try {
                KashubConfig config = KashubConfig.getInstance();
                if (config.aiProvider == KashubConfig.AiProvider.OFF) {
                    chatHistory.add(new ChatMessage("AI Agent", "AI is disabled. Enable it in settings.", false));
                    return;
                }
                
                KasHubAiClient client = KasHubAiClient.getInstance();
                Map<String, String> context = new HashMap<>();
                context.put("source", "ai_agent_gui");
                
                String response = client.generateResponseWithTools(message, context);
                chatHistory.add(new ChatMessage("AI Agent", response, false));
                
            } catch (Exception e) {
                ScriptLogger.getInstance().error("AI error: " + e.getMessage());
                chatHistory.add(new ChatMessage("AI Agent", "Error: " + e.getMessage(), false));
            } finally {
                isLoading = false;
            }
        });
    }
    
    private void fetchModels() {
        CompletableFuture.runAsync(() -> {
            try {
                KasHubAiClient client = KasHubAiClient.getInstance();
                availableModels = client.fetchAvailableModels();
                chatHistory.add(new ChatMessage("System", "Fetched " + availableModels.size() + " models", false));
                // Re-init to show model buttons
                if (currentTab == Tab.SETTINGS) {
                    this.client.execute(() -> this.init());
                }
            } catch (Exception e) {
                chatHistory.add(new ChatMessage("System", "Failed to fetch models: " + e.getMessage(), false));
            }
        });
    }
    
    private void saveSettings() {
        KashubConfig config = KashubConfig.getInstance();
        config.aiApiKey = apiKeyField.getText();
        config.aiModel = modelField.getText();
        if (customUrlField != null) {
            config.aiBaseUrl = customUrlField.getText();
        }
        config.save();
        
        chatHistory.add(new ChatMessage("System", "Settings saved!", false));
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        
        if (currentTab == Tab.CHAT) {
            if (keyCode == GLFW.GLFW_KEY_ENTER && inputField != null && !inputField.getText().isEmpty()) {
                sendMessage();
                return true;
            }
            
            // Scroll with Page Up/Down
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                chatScrollOffset = Math.max(0, chatScrollOffset - 5);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                chatScrollOffset = Math.min(Math.max(0, chatHistory.size() - 20), chatScrollOffset + 5);
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == Tab.CHAT) {
            chatScrollOffset = Math.max(0, Math.min(Math.max(0, chatHistory.size() - 20), chatScrollOffset - (int)(verticalAmount * 3)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public void close() {
        this.client.setScreen(null);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    private static class ChatMessage {
        String sender;
        String content;
        boolean isUser;
        boolean isSystem;
        
        ChatMessage(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
            this.isSystem = false;
        }
        
        ChatMessage(String sender, String content, boolean isUser, boolean isSystem) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
            this.isSystem = isSystem;
        }
    }
}

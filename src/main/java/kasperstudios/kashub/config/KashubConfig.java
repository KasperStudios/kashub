package kasperstudios.kashub.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Конфигурация мода Kashub
 */
public class KashubConfig {
    private static KashubConfig instance;
    private static final Path CONFIG_PATH = Paths.get("config", "kashub", "config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Настройки редактора
    public String editorTheme = "dracula";
    public int editorFontSize = 12;
    public boolean editorLineNumbers = true;
    public boolean editorAutoComplete = true;
    public boolean editorSyntaxHighlight = true;
    public int editorTabSize = 4;

    // Настройки скриптов
    public int maxScriptsPerTick = 10;
    public int maxCommandsPerScript = 1000;
    public int scriptTimeout = 30000; // мс
    public boolean allowCheats = false;
    public List<String> whitelistedCommands = new ArrayList<>();
    public List<String> blacklistedCommands = new ArrayList<>();

    // Настройки безопасности
    public boolean sandboxMode = true;
    public int maxLoopIterations = 10000;
    public boolean allowFileAccess = false;
    public boolean allowNetworkAccess = false;
    public boolean allowEval = false; // Разрешить выполнение Java-кода через eval

    // Настройки HTTP/API
    public boolean allowHttpRequests = false;
    public boolean allowRemoteScripts = false;
    public boolean allowAiIntegration = false;
    public List<String> httpWhitelistedDomains = new ArrayList<>();
    public List<String> remoteScriptSources = new ArrayList<>();
    
    // AI Settings
    public enum AiProvider { OFF, GROQ, MEGALLM, CUSTOM }
    public AiProvider aiProvider = AiProvider.OFF;
    public String aiApiKey = "";
    public String aiBaseUrl = ""; // Empty = use default for selected provider
    public String aiModel = ""; // Empty = use first available model from API
    public boolean aiEnableTools = true; // Enable script manipulation tools
    public boolean aiEnableGui = true; // Enable AI chat GUI
    public int aiMaxTokens = 2000;
    public double aiTemperature = 0.7;
    public int aiContextMaxLength = 8000; // Max characters for context

    // Настройки прав доступа
    public boolean allowUserScriptsEdit = true;
    public boolean allowDangerousCommands = false;
    public boolean allowSystemScriptsCopy = true;

    // Настройки UI
    public float guiScale = 1.0f;
    public int openEditorKey = 75; // K
    public int stopScriptsKey = 90; // Z
    public int toggleScriptingKey = 293; // F6

    // Настройки логирования
    public boolean enableLogging = true;
    public String logLevel = "INFO";
    public boolean logToFile = true;
    public boolean logToChat = false;
    
    // Последний открытый скрипт
    public String lastOpenedScript = null;
    
    // Привязки клавиш к скриптам (keyCode -> scriptName)
    public Map<Integer, String> scriptKeybinds = new HashMap<>();

    // Темы редактора
    public Map<String, EditorTheme> themes = new HashMap<>();

    private KashubConfig() {
        initializeDefaultThemes();
        initializeDefaultWhitelist();
    }

    public static KashubConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private void initializeDefaultThemes() {
        // Dracula theme
        EditorTheme dracula = new EditorTheme();
        dracula.name = "Dracula";
        dracula.background = 0xFF282A36;
        dracula.foreground = 0xFFF8F8F2;
        dracula.keyword = 0xFFFF79C6;
        dracula.string = 0xFFF1FA8C;
        dracula.comment = 0xFF6272A4;
        dracula.number = 0xFFBD93F9;
        dracula.command = 0xFF8BE9FD;
        dracula.variable = 0xFF50FA7B;
        dracula.lineNumber = 0xFF6272A4;
        dracula.selection = 0x4044475A;
        dracula.currentLine = 0x20FFFFFF;
        themes.put("dracula", dracula);

        // One Dark theme
        EditorTheme oneDark = new EditorTheme();
        oneDark.name = "One Dark";
        oneDark.background = 0xFF282C34;
        oneDark.foreground = 0xFFABB2BF;
        oneDark.keyword = 0xFFC678DD;
        oneDark.string = 0xFF98C379;
        oneDark.comment = 0xFF5C6370;
        oneDark.number = 0xFFD19A66;
        oneDark.command = 0xFF61AFEF;
        oneDark.variable = 0xFFE5C07B;
        oneDark.lineNumber = 0xFF4B5263;
        oneDark.selection = 0x403E4451;
        oneDark.currentLine = 0x202C313C;
        themes.put("onedark", oneDark);

        // Monokai theme
        EditorTheme monokai = new EditorTheme();
        monokai.name = "Monokai";
        monokai.background = 0xFF272822;
        monokai.foreground = 0xFFF8F8F2;
        monokai.keyword = 0xFFF92672;
        monokai.string = 0xFFE6DB74;
        monokai.comment = 0xFF75715E;
        monokai.number = 0xFFAE81FF;
        monokai.command = 0xFF66D9EF;
        monokai.variable = 0xFFA6E22E;
        monokai.lineNumber = 0xFF90908A;
        monokai.selection = 0x4049483E;
        monokai.currentLine = 0x203E3D32;
        themes.put("monokai", monokai);

        // Solarized Dark theme
        EditorTheme solarizedDark = new EditorTheme();
        solarizedDark.name = "Solarized Dark";
        solarizedDark.background = 0xFF002B36;
        solarizedDark.foreground = 0xFF839496;
        solarizedDark.keyword = 0xFF859900;
        solarizedDark.string = 0xFF2AA198;
        solarizedDark.comment = 0xFF586E75;
        solarizedDark.number = 0xFFD33682;
        solarizedDark.command = 0xFF268BD2;
        solarizedDark.variable = 0xFFB58900;
        solarizedDark.lineNumber = 0xFF586E75;
        solarizedDark.selection = 0x40073642;
        solarizedDark.currentLine = 0x20073642;
        themes.put("solarized_dark", solarizedDark);

        // Solarized Light theme
        EditorTheme solarizedLight = new EditorTheme();
        solarizedLight.name = "Solarized Light";
        solarizedLight.background = 0xFFFDF6E3;
        solarizedLight.foreground = 0xFF657B83;
        solarizedLight.keyword = 0xFF859900;
        solarizedLight.string = 0xFF2AA198;
        solarizedLight.comment = 0xFF93A1A1;
        solarizedLight.number = 0xFFD33682;
        solarizedLight.command = 0xFF268BD2;
        solarizedLight.variable = 0xFFB58900;
        solarizedLight.lineNumber = 0xFF93A1A1;
        solarizedLight.selection = 0x40EEE8D5;
        solarizedLight.currentLine = 0x20EEE8D5;
        themes.put("solarized_light", solarizedLight);

        // GitHub Dark theme
        EditorTheme githubDark = new EditorTheme();
        githubDark.name = "GitHub Dark";
        githubDark.background = 0xFF0D1117;
        githubDark.foreground = 0xFFC9D1D9;
        githubDark.keyword = 0xFFFF7B72;
        githubDark.string = 0xFFA5D6FF;
        githubDark.comment = 0xFF8B949E;
        githubDark.number = 0xFF79C0FF;
        githubDark.command = 0xFFD2A8FF;
        githubDark.variable = 0xFFFFA657;
        githubDark.lineNumber = 0xFF484F58;
        githubDark.selection = 0x40388BFD;
        githubDark.currentLine = 0x20161B22;
        themes.put("github_dark", githubDark);

        // Nord theme
        EditorTheme nord = new EditorTheme();
        nord.name = "Nord";
        nord.background = 0xFF2E3440;
        nord.foreground = 0xFFD8DEE9;
        nord.keyword = 0xFF81A1C1;
        nord.string = 0xFFA3BE8C;
        nord.comment = 0xFF616E88;
        nord.number = 0xFFB48EAD;
        nord.command = 0xFF88C0D0;
        nord.variable = 0xFFEBCB8B;
        nord.lineNumber = 0xFF4C566A;
        nord.selection = 0x403B4252;
        nord.currentLine = 0x203B4252;
        themes.put("nord", nord);

        // Gruvbox Dark theme
        EditorTheme gruvbox = new EditorTheme();
        gruvbox.name = "Gruvbox Dark";
        gruvbox.background = 0xFF282828;
        gruvbox.foreground = 0xFFEBDBB2;
        gruvbox.keyword = 0xFFFB4934;
        gruvbox.string = 0xFFB8BB26;
        gruvbox.comment = 0xFF928374;
        gruvbox.number = 0xFFD3869B;
        gruvbox.command = 0xFF83A598;
        gruvbox.variable = 0xFFFABD2F;
        gruvbox.lineNumber = 0xFF665C54;
        gruvbox.selection = 0x403C3836;
        gruvbox.currentLine = 0x203C3836;
        themes.put("gruvbox", gruvbox);

        // Tokyo Night theme
        EditorTheme tokyoNight = new EditorTheme();
        tokyoNight.name = "Tokyo Night";
        tokyoNight.background = 0xFF1A1B26;
        tokyoNight.foreground = 0xFFA9B1D6;
        tokyoNight.keyword = 0xFF9D7CD8;
        tokyoNight.string = 0xFF9ECE6A;
        tokyoNight.comment = 0xFF565F89;
        tokyoNight.number = 0xFFFF9E64;
        tokyoNight.command = 0xFF7AA2F7;
        tokyoNight.variable = 0xFFE0AF68;
        tokyoNight.lineNumber = 0xFF3B4261;
        tokyoNight.selection = 0x40283457;
        tokyoNight.currentLine = 0x201E2030;
        themes.put("tokyo_night", tokyoNight);

        // Catppuccin Mocha theme
        EditorTheme catppuccin = new EditorTheme();
        catppuccin.name = "Catppuccin Mocha";
        catppuccin.background = 0xFF1E1E2E;
        catppuccin.foreground = 0xFFCDD6F4;
        catppuccin.keyword = 0xFFCBA6F7;
        catppuccin.string = 0xFFA6E3A1;
        catppuccin.comment = 0xFF6C7086;
        catppuccin.number = 0xFFFAB387;
        catppuccin.command = 0xFF89B4FA;
        catppuccin.variable = 0xFFF9E2AF;
        catppuccin.lineNumber = 0xFF45475A;
        catppuccin.selection = 0x40313244;
        catppuccin.currentLine = 0x20313244;
        themes.put("catppuccin", catppuccin);
    }

    private void initializeDefaultWhitelist() {
        // Безопасные команды по умолчанию
        whitelistedCommands.addAll(Arrays.asList(
            "print", "wait", "jump", "run", "moveTo", "lookAt",
            "chat", "eat", "useItem", "selectSlot", "drop",
            "sneak", "sprint", "getBlock", "loop", "onEvent"
        ));
        
        // Потенциально опасные команды (требуют allowCheats)
        blacklistedCommands.addAll(Arrays.asList(
            "tp", "setHealth", "attack", "speedhack", "fullbright"
        ));
    }

    public EditorTheme getCurrentTheme() {
        return themes.getOrDefault(editorTheme, themes.get("dracula"));
    }

    public boolean isCommandAllowed(String command) {
        if (!sandboxMode) return true;
        
        String cmd = command.toLowerCase();
        
        // Проверяем черный список
        if (blacklistedCommands.contains(cmd)) {
            return allowCheats;
        }
        
        // Если белый список пуст, разрешаем все кроме черного списка
        if (whitelistedCommands.isEmpty()) {
            return true;
        }
        
        // Проверяем белый список
        return whitelistedCommands.contains(cmd);
    }

    public static KashubConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = new String(Files.readAllBytes(CONFIG_PATH));
                KashubConfig config = GSON.fromJson(json, KashubConfig.class);
                if (config != null) {
                    return config;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
        
        KashubConfig config = new KashubConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.write(CONFIG_PATH, json.getBytes());
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void reload() {
        KashubConfig loaded = load();
        this.editorTheme = loaded.editorTheme;
        this.editorFontSize = loaded.editorFontSize;
        this.editorLineNumbers = loaded.editorLineNumbers;
        this.editorAutoComplete = loaded.editorAutoComplete;
        this.editorSyntaxHighlight = loaded.editorSyntaxHighlight;
        this.maxScriptsPerTick = loaded.maxScriptsPerTick;
        this.maxCommandsPerScript = loaded.maxCommandsPerScript;
        this.scriptTimeout = loaded.scriptTimeout;
        this.allowCheats = loaded.allowCheats;
        this.sandboxMode = loaded.sandboxMode;
        this.maxLoopIterations = loaded.maxLoopIterations;
        this.guiScale = loaded.guiScale;
        this.enableLogging = loaded.enableLogging;
        this.logLevel = loaded.logLevel;
        this.logToFile = loaded.logToFile;
        this.logToChat = loaded.logToChat;
        if (loaded.scriptKeybinds != null) {
            this.scriptKeybinds = new HashMap<>(loaded.scriptKeybinds);
        }
    }
    
    // Методы для работы с кейбиндами скриптов
    public void setScriptKeybind(int keyCode, String scriptName) {
        // Удаляем старую привязку для этого скрипта
        scriptKeybinds.entrySet().removeIf(e -> e.getValue().equals(scriptName));
        if (keyCode > 0) {
            scriptKeybinds.put(keyCode, scriptName);
        }
        save();
    }
    
    public void removeScriptKeybind(String scriptName) {
        scriptKeybinds.entrySet().removeIf(e -> e.getValue().equals(scriptName));
        save();
    }
    
    public String getScriptForKey(int keyCode) {
        return scriptKeybinds.get(keyCode);
    }
    
    public int getKeyForScript(String scriptName) {
        for (Map.Entry<Integer, String> entry : scriptKeybinds.entrySet()) {
            if (entry.getValue().equals(scriptName)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * Класс темы редактора
     */
    public static class EditorTheme {
        public String name;
        public int background;
        public int foreground;
        public int keyword;
        public int string;
        public int comment;
        public int number;
        public int command;
        public int variable;
        public int lineNumber;
        public int selection;
        public int currentLine;
    }
}

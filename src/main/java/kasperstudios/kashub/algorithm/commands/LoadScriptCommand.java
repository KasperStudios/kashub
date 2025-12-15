package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.runtime.ScriptTaskManager;
import kasperstudios.kashub.runtime.ScriptType;
import kasperstudios.kashub.services.HttpService;
import kasperstudios.kashub.util.ScriptLogger;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Команда для динамической загрузки и выполнения скриптов
 * Аналог eval/loadstring, но только для .kh формата
 * 
 * Синтаксис:
 *   loadscript <code> - выполнить код из строки
 *   loadscriptFromUrl <url> - загрузить и выполнить скрипт по URL
 */
public class LoadScriptCommand implements Command {

    @Override
    public String getName() {
        return "loadscript";
    }

    @Override
    public String getDescription() {
        return "Loads and executes .kh script from string or URL";
    }

    @Override
    public String getParameters() {
        return "<code> or fromUrl <url> - script code or URL";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("fromurl") || subcommand.equals("url")) {
            executeFromUrl(args);
        } else {
            executeFromString(args);
        }
    }

    private void executeFromString(String[] args) {
        // Собираем код из аргументов
        StringBuilder code = new StringBuilder();
        for (String arg : args) {
            code.append(arg).append(" ");
        }
        
        String scriptCode = code.toString().trim();
        
        // Убираем кавычки если есть
        if (scriptCode.startsWith("\"") && scriptCode.endsWith("\"")) {
            scriptCode = scriptCode.substring(1, scriptCode.length() - 1);
        }
        
        // Заменяем escape-последовательности
        scriptCode = scriptCode.replace("\\n", "\n").replace("\\t", "\t");
        
        if (scriptCode.isEmpty()) {
            System.out.println("Empty script code");
            return;
        }

        ScriptLogger.getInstance().info("Executing inline script (" + scriptCode.length() + " chars)");
        
        try {
            ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
            interpreter.parseCommands(scriptCode);
            interpreter.executeQueuedCommands();
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Inline script error: " + e.getMessage());
        }
    }

    private void executeFromUrl(String[] args) {
        KashubConfig config = KashubConfig.getInstance();
        
        if (!config.allowRemoteScripts) {
            System.out.println("Remote scripts are disabled. Enable allowRemoteScripts in config.");
            ScriptLogger.getInstance().warn("Remote script execution blocked by config");
            return;
        }

        if (!config.allowHttpRequests) {
            System.out.println("HTTP requests are disabled. Enable allowHttpRequests in config.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Usage: loadscript fromUrl <url>");
            return;
        }

        String url = args[1];

        // Проверяем, разрешён ли источник
        if (!isSourceAllowed(url, config)) {
            System.out.println("Script source not allowed: " + url);
            ScriptLogger.getInstance().warn("Blocked remote script from: " + url);
            return;
        }

        ScriptLogger.getInstance().info("Loading remote script from: " + url);

        HttpService.getInstance().loadScriptFromUrl(url).thenAccept(scriptCode -> {
            if (scriptCode == null || scriptCode.isEmpty()) {
                ScriptLogger.getInstance().error("Failed to load remote script or empty content");
                return;
            }

            // Выполняем в главном потоке
            HttpService.getInstance().runOnMainThread(() -> {
                try {
                    // Валидация: проверяем, что это текстовый .kh скрипт
                    if (!isValidKhScript(scriptCode)) {
                        ScriptLogger.getInstance().error("Invalid .kh script content");
                        return;
                    }

                    ScriptLogger.getInstance().info("Executing remote script (" + scriptCode.length() + " chars)");
                    
                    // Запускаем как отдельную задачу с типом REMOTE
                    Set<String> tags = new HashSet<>();
                    tags.add("remote");
                    ScriptTaskManager.getInstance().startScript(
                        "remote_" + System.currentTimeMillis(),
                        scriptCode,
                        tags,
                        ScriptType.REMOTE
                    );
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Remote script execution error: " + e.getMessage());
                }
            });
        });
    }

    private boolean isSourceAllowed(String url, KashubConfig config) {
        if (config.remoteScriptSources == null || config.remoteScriptSources.isEmpty()) {
            // Если список пуст, используем httpWhitelistedDomains
            if (config.httpWhitelistedDomains == null || config.httpWhitelistedDomains.isEmpty()) {
                return true; // Разрешаем все если нет ограничений
            }
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            // Проверяем remoteScriptSources
            if (config.remoteScriptSources != null) {
                for (String allowed : config.remoteScriptSources) {
                    if (host.equals(allowed) || host.endsWith("." + allowed) || url.startsWith(allowed)) {
                        return true;
                    }
                }
            }

            // Проверяем httpWhitelistedDomains
            if (config.httpWhitelistedDomains != null) {
                for (String allowed : config.httpWhitelistedDomains) {
                    if (host.equals(allowed) || host.endsWith("." + allowed)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidKhScript(String content) {
        if (content == null || content.isEmpty()) return false;
        
        // Проверяем, что это текст, а не бинарные данные
        if (content.contains("\0")) return false;
        
        // Проверяем на наличие опасных паттернов
        String lower = content.toLowerCase();
        
        // Запрещаем любые попытки выполнить Java-код
        if (lower.contains("class ") || lower.contains("import ") || 
            lower.contains("package ") || lower.contains("java.") ||
            lower.contains("javax.") || lower.contains("reflect")) {
            ScriptLogger.getInstance().warn("Blocked script with Java code patterns");
            return false;
        }
        
        // Проверяем на попытки десериализации
        if (lower.contains("objectinputstream") || lower.contains("deserialize") ||
            lower.contains("classloader")) {
            ScriptLogger.getInstance().warn("Blocked script with deserialization patterns");
            return false;
        }
        
        return true;
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            execute(args);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    private void printHelp() {
        System.out.println("LoadScript command:");
        System.out.println("  loadscript <code> - execute inline .kh code");
        System.out.println("  loadscript fromUrl <url> - load and execute .kh from URL");
        System.out.println("");
        System.out.println("Security:");
        System.out.println("  - Only .kh syntax is allowed, no Java code");
        System.out.println("  - Remote scripts require allowRemoteScripts=true");
        System.out.println("  - URLs must be in httpWhitelistedDomains or remoteScriptSources");
    }
}

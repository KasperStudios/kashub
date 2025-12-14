package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.services.HttpService;
import kasperstudios.kashub.util.ScriptLogger;

import java.util.concurrent.CompletableFuture;

/**
 * Команды для работы с HTTP API
 * Синтаксис:
 *   http.get <url> - GET запрос
 *   http.post <url> <body> - POST запрос
 *   http.status - статус последнего запроса
 */
public class HttpCommand implements Command {

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public String getDescription() {
        return "Выполняет HTTP-запросы (GET/POST)";
    }

    @Override
    public String getParameters() {
        return "<subcommand> [args] - get/post/status";
    }

    @Override
    public String getCategory() {
        return "Network";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Make HTTP requests (requires allowHttpRequests).\n\n" +
               "Usage:\n" +
               "  http get <url>\n" +
               "  http post <url> <body>\n" +
               "  http put <url> <body>\n" +
               "  http delete <url>\n\n" +
               "Examples:\n" +
               "  http get https://api.example.com/data\n" +
               "  http post https://api.example.com {\"key\":\"value\"}\n\n" +
               "Variables set:\n" +
               "  $http_status - Response status code\n" +
               "  $http_body   - Response body\n" +
               "  $http_error  - Error message (if failed)";
    }

    @Override
    public void execute(String[] args) throws Exception {
        KashubConfig config = KashubConfig.getInstance();
        
        if (!config.allowHttpRequests) {
            System.out.println("HTTP requests are disabled. Enable in config.");
            return;
        }

        if (args.length == 0) {
            printHelp();
            return;
        }

        String subcommand = args[0].toLowerCase();
        HttpService http = HttpService.getInstance();
        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();

        switch (subcommand) {
            case "get": {
                if (args.length < 2) {
                    System.out.println("Usage: http get <url>");
                    return;
                }
                String url = args[1];
                
                interpreter.setVariable("http_status", "pending");
                interpreter.setVariable("http_body", "");
                interpreter.setVariable("http_error", "");
                
                http.get(url).thenAccept(result -> {
                    http.runOnMainThread(() -> {
                        interpreter.setVariable("http_status", result.success ? "success" : "error");
                        interpreter.setVariable("http_code", String.valueOf(result.statusCode));
                        interpreter.setVariable("http_body", result.body != null ? result.body : "");
                        interpreter.setVariable("http_error", result.error != null ? result.error : "");
                        
                        if (result.success) {
                            ScriptLogger.getInstance().info("HTTP GET success: " + url);
                        } else {
                            ScriptLogger.getInstance().error("HTTP GET failed: " + result.error);
                        }
                    });
                });
                break;
            }

            case "post": {
                if (args.length < 3) {
                    System.out.println("Usage: http post <url> <body>");
                    return;
                }
                String url = args[1];
                StringBuilder body = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    body.append(args[i]);
                    if (i < args.length - 1) body.append(" ");
                }
                
                interpreter.setVariable("http_status", "pending");
                interpreter.setVariable("http_body", "");
                interpreter.setVariable("http_error", "");
                
                http.post(url, body.toString()).thenAccept(result -> {
                    http.runOnMainThread(() -> {
                        interpreter.setVariable("http_status", result.success ? "success" : "error");
                        interpreter.setVariable("http_code", String.valueOf(result.statusCode));
                        interpreter.setVariable("http_body", result.body != null ? result.body : "");
                        interpreter.setVariable("http_error", result.error != null ? result.error : "");
                        
                        if (result.success) {
                            ScriptLogger.getInstance().info("HTTP POST success: " + url);
                        } else {
                            ScriptLogger.getInstance().error("HTTP POST failed: " + result.error);
                        }
                    });
                });
                break;
            }

            case "status": {
                String status = interpreter.getVariable("http_status");
                String code = interpreter.getVariable("http_code");
                System.out.println("HTTP Status: " + (status != null ? status : "none"));
                System.out.println("HTTP Code: " + (code != null ? code : "none"));
                break;
            }

            default:
                printHelp();
        }
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            execute(args);
            // Для HTTP команд сразу завершаем, результат придёт асинхронно
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    private void printHelp() {
        System.out.println("HTTP API commands:");
        System.out.println("  http get <url> - perform GET request");
        System.out.println("  http post <url> <body> - perform POST request");
        System.out.println("  http status - show last request status");
        System.out.println("");
        System.out.println("Results are stored in variables:");
        System.out.println("  $http_status - pending/success/error");
        System.out.println("  $http_code - HTTP status code");
        System.out.println("  $http_body - response body");
        System.out.println("  $http_error - error message");
    }
}

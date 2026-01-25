package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Metadata;
import kasperstudios.kashub.core.Registry;

import java.util.*;
import java.util.stream.Collectors;

public class AutocompleteEndpoint {

    private static final List<String> KEYWORDS = Arrays.asList(
            "if", "else", "while", "for", "loop", "break", "continue", "return",
            "let", "const", "fn", "function", "true", "false");

    public static void handle(HttpExchange exchange, Gson gson) {
        try {
            String body = KashubAPIServer.readRequestBody(exchange);
            Map<String, Object> request = gson.fromJson(body, Map.class);

            String code = (String) request.getOrDefault("code", "");
            String prefix = (String) request.getOrDefault("prefix", "");
            int line = ((Number) request.getOrDefault("line", 0)).intValue();
            int column = ((Number) request.getOrDefault("column", 0)).intValue();

            List<CompletionItem> items = getCompletions(code, prefix, line, column);

            Map<String, Object> response = new HashMap<>();
            response.put("items", items);

            String json = gson.toJson(response);
            KashubAPIServer.sendResponse(exchange, 200, json);

        } catch (Exception e) {
            Kashub.LOGGER.error("Error in autocomplete endpoint", e);
            KashubAPIServer.sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    public static List<CompletionItem> getCompletions(String code, String prefix, int line, int column) {
        List<CompletionItem> items = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        if (prefix.startsWith("$")) {
            String varPrefix = prefix.substring(1).toUpperCase();
            for (kasperstudios.kashub.core.Environment.Variable envVar : kasperstudios.kashub.core.Environment
                    .getInstance().getVariableDefinitions().values()) {
                if (envVar.getName().startsWith(varPrefix)) {
                    items.add(new CompletionItem(
                            "$" + envVar.getName(),
                            "Variable",
                            envVar.getDescription(),
                            "Environment variable: " + envVar.getValue(),
                            "$" + envVar.getName()));
                }
            }
            return items;
        }

        for (Command cmd : Registry.getAllCommands()) {
            if (cmd.getName().toLowerCase().startsWith(lowerPrefix)) {
                items.add(new CompletionItem(
                        cmd.getName(),
                        "Function",
                        cmd.getMetadata().description,
                        formatCommandHelp(cmd),
                        cmd.getName() + " "));
            }
        }

        for (String keyword : KEYWORDS) {
            if (keyword.startsWith(lowerPrefix)) {
                String insertText = keyword;
                String detail = "Keyword";

                switch (keyword) {
                    case "if":
                        insertText = "if ${1:condition} {\n\t$0\n}";
                        detail = "If statement";
                        break;
                    case "while":
                        insertText = "while ${1:condition} {\n\t$0\n}";
                        detail = "While loop";
                        break;
                    case "for":
                        insertText = "for (${1:i} = ${2:0}; ${1:i} < ${3:10}; ${1:i}++) {\n\t$0\n}";
                        detail = "For loop";
                        break;
                    case "loop":
                        insertText = "loop ${1:10} {\n\t$0\n}";
                        detail = "Loop N times";
                        break;
                    case "fn":
                    case "function":
                        insertText = "fn ${1:name}(${2:params}) {\n\t$0\n}";
                        detail = "Function definition";
                        break;
                    case "let":
                        insertText = "let ${1:name} = ${2:value}";
                        detail = "Variable declaration";
                        break;
                    case "const":
                        insertText = "const ${1:NAME} = ${2:value}";
                        detail = "Constant declaration";
                        break;
                }

                items.add(new CompletionItem(
                        keyword,
                        "Keyword",
                        detail,
                        "",
                        insertText));
            }
        }

        Set<String> userVars = extractVariables(code);
        for (String varName : userVars) {
            if (varName.toLowerCase().startsWith(lowerPrefix)) {
                items.add(new CompletionItem(
                        varName,
                        "Variable",
                        "User variable",
                        "",
                        varName));
            }
        }

        items.sort((a, b) -> {
            boolean aExact = a.label.toLowerCase().equals(lowerPrefix);
            boolean bExact = b.label.toLowerCase().equals(lowerPrefix);
            if (aExact && !bExact)
                return -1;
            if (!aExact && bExact)
                return 1;
            return a.label.compareToIgnoreCase(b.label);
        });

        return items;
    }

    private static Set<String> extractVariables(String code) {
        Set<String> vars = new HashSet<>();
        String[] lines = code.split("\\r?\\n");

        for (String line : lines) {

            if (line.matches(".*\\b(let|const)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=.*")) {
                String[] parts = line.split("(let|const)\\s+")[1].split("\\s*=");
                if (parts.length > 0) {
                    vars.add(parts[0].trim());
                }
            }

            else if (line.matches("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=.*")) {
                String varName = line.trim().split("\\s*=")[0].trim();
                vars.add(varName);
            }
        }

        return vars;
    }

    private static String formatCommandHelp(Command cmd) {
        Metadata meta = cmd.getMetadata();
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(meta.name).append("**\n\n");
        sb.append(meta.description).append("\n\n");

        if (meta.syntax != null && !meta.syntax.isEmpty()) {
            sb.append("**Syntax:** `").append(meta.syntax).append("`\n\n");
        }

        if (meta.examples != null && !meta.examples.isEmpty()) {
            sb.append("**Examples:**\n");
            for (String example : meta.examples) {
                sb.append("- `").append(example).append("`\n");
            }
        }

        return sb.toString();
    }

    public static class CompletionItem {
        public String label;
        public String kind;
        public String detail;
        public String documentation;
        public String insertText;

        public CompletionItem(String label, String kind, String detail, String documentation, String insertText) {
            this.label = label;
            this.kind = kind;
            this.detail = detail;
            this.documentation = documentation;
            this.insertText = insertText;
        }
    }
}

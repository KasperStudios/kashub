package kasperstudios.kashub.util;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BugReporter {
    private static final Logger LOGGER = LogManager.getLogger(BugReporter.class);
    private static final Map<String, Long> lastReportTimes = new HashMap<>();
    private static final long REPORT_COOLDOWN = 300000;
    private static final int MAX_LOG_LINES = 500;

    private static final String ENCODED_WEBHOOK = "Smo2Uy1FVkV5Ymx4WTFDUEhNcjNHRGJkNTRPb2E0LUo1SXE1UjQ0NDROT2NSU3FtMGJIOXpyZUJ3MVl2alJTNUxDYVAvNTQ4MDk4Mjg4ODI2NDMwNzU0MS9za29vaGJldy9pcGEvbW9jLmRyb2NzaWQvLzpzcHR0aA==";

    public static CompletableFuture<Boolean> sendBugReport(String description, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = username != null ? username : "anonymous";
                long now = System.currentTimeMillis();
                Long lastReport = lastReportTimes.get(key);

                if (lastReport != null && (now - lastReport) < REPORT_COOLDOWN) {
                    long remainingSeconds = (REPORT_COOLDOWN - (now - lastReport)) / 1000;
                    LOGGER.warn("Bug report rate limited. Try again in {} seconds", remainingSeconds);
                    return false;
                }

                if (ENCODED_WEBHOOK == null || ENCODED_WEBHOOK.isEmpty()) {
                    LOGGER.warn("Bug report webhook not configured");
                    return false;
                }

                String logs = collectLatestLogs();
                String systemInfo = collectSystemInfo();
                boolean success = sendToWebhook(description, logs, systemInfo, username);

                if (success) {
                    lastReportTimes.put(key, now);
                    LOGGER.info("Bug report sent successfully");
                }

                return success;

            } catch (Exception e) {
                LOGGER.error("Failed to send bug report", e);
                return false;
            }
        });
    }

    private static String collectLatestLogs() {
        try {
            Path logPath = Paths.get("logs/latest.log");
            if (!Files.exists(logPath)) {
                return "No logs found";
            }

            List<String> allLines = Files.readAllLines(logPath);
            List<String> lastLines = allLines.stream()
                .skip(Math.max(0, allLines.size() - MAX_LOG_LINES))
                .collect(Collectors.toList());

            return String.join("\n", lastLines);

        } catch (IOException e) {
            LOGGER.error("Failed to read logs", e);
            return "Failed to read logs: " + e.getMessage();
        }
    }

    private static String collectSystemInfo() {
        StringBuilder info = new StringBuilder();

        try {
            MinecraftClient client = MinecraftClient.getInstance();

            info.append("Kashub Version: v0.8.0-beta\n");
            info.append("Minecraft Version: ").append(client.getGameVersion()).append("\n");
            info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
            info.append("OS: ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append("\n");
            info.append("Timestamp: ").append(Instant.now().toString()).append("\n");

        } catch (Exception e) {
            info.append("Failed to collect system info: ").append(e.getMessage()).append("\n");
        }

        return info.toString();
    }

    private static boolean sendToWebhook(String description, String logs, String systemInfo, String username) {
        try {
            String webhookUrl = decodeWebhook(ENCODED_WEBHOOK);
            if (webhookUrl.isEmpty()) {
                return false;
            }

            String boundary = "----KashubBugReport" + System.currentTimeMillis();
            String LINE = "\r\n";

            JsonObject payload = new JsonObject();
            payload.addProperty("username", "Kashub Bug Reporter");
            payload.addProperty("avatar_url", "https://i.imgur.com/4M34hi2.png");

            JsonObject embed = new JsonObject();
            embed.addProperty("title", "🐛 Bug Report");
            embed.addProperty("description", description);
            embed.addProperty("color", 15158332);
            embed.addProperty("timestamp", Instant.now().toString());

            JsonObject author = new JsonObject();
            author.addProperty("name", username != null ? username : "Anonymous");
            embed.add("author", author);

            com.google.gson.JsonArray fields = new com.google.gson.JsonArray();

            JsonObject sysField = new JsonObject();
            sysField.addProperty("name", "📊 System Info");
            sysField.addProperty("value", "```\n" + systemInfo + "```");
            sysField.addProperty("inline", false);
            fields.add(sysField);

            embed.add("fields", fields);

            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            StringBuilder multipart = new StringBuilder();

            multipart.append("--").append(boundary).append(LINE);
            multipart.append("Content-Disposition: form-data; name=\"payload_json\"").append(LINE);
            multipart.append("Content-Type: application/json").append(LINE).append(LINE);
            multipart.append(payload.toString()).append(LINE);

            multipart.append("--").append(boundary).append(LINE);
            multipart.append("Content-Disposition: form-data; name=\"file\"; filename=\"kashub_logs.txt\"").append(LINE);
            multipart.append("Content-Type: text/plain").append(LINE).append(LINE);
            multipart.append(logs).append(LINE);

            multipart.append("--").append(boundary).append("--").append(LINE);

            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = multipart.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                LOGGER.info("Bug report sent successfully. Response code: {}", responseCode);
            } else {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errorResponse = br.lines().collect(Collectors.joining("\n"));
                    LOGGER.error("Discord webhook error. Code: {}, Response: {}", responseCode, errorResponse);
                }
            }

            conn.disconnect();

            return responseCode >= 200 && responseCode < 300;

        } catch (Exception e) {
            LOGGER.error("Failed to send webhook", e);
            return false;
        }
    }

    private static String decodeWebhook(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return "";
        }

        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
            String reversed = new String(decoded, StandardCharsets.UTF_8);
            return new StringBuilder(reversed).reverse().toString();
        } catch (Exception e) {
            LOGGER.error("Failed to decode webhook", e);
            return "";
        }
    }

    public static String encodeWebhook(String webhookUrl) {
        String reversed = new StringBuilder(webhookUrl).reverse().toString();
        return java.util.Base64.getEncoder().encodeToString(reversed.getBytes(StandardCharsets.UTF_8));
    }
}

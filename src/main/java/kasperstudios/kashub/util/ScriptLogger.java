package kasperstudios.kashub.util;

import kasperstudios.kashub.config.KashubConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScriptLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptLogger.class);
    private static ScriptLogger instance;
    private static final Path LOG_PATH = Paths.get("logs", "kashub");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final List<LogEntry> logBuffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000;

    public enum LogLevel {
        DEBUG(0, ChatFormatting.GRAY),
        INFO(1, ChatFormatting.WHITE),
        WARN(2, ChatFormatting.YELLOW),
        ERROR(3, ChatFormatting.RED),
        SUCCESS(1, ChatFormatting.GREEN);

        public final int priority;
        public final ChatFormatting color;

        LogLevel(int priority, ChatFormatting color) {
            this.priority = priority;
            this.color = color;
        }
    }

    private ScriptLogger() {
        try {
            Files.createDirectories(LOG_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to create log directory: {}", e.getMessage());
        }
    }

    public static ScriptLogger getInstance() {
        if (instance == null) {
            instance = new ScriptLogger();
        }
        return instance;
    }

    public void log(LogLevel level, String message) {
        log(level, message, null);
    }

    public void log(LogLevel level, String message, String scriptName) {
        KashubConfig config = KashubConfig.getInstance();

        if (!config.enableLogging)
            return;

        LogLevel configLevel;
        try {
            configLevel = LogLevel.valueOf(config.logLevel.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            configLevel = LogLevel.INFO;
        }

        if (level.priority < configLevel.priority)
            return;

        LocalDateTime now = LocalDateTime.now();
        LogEntry entry = new LogEntry(now, level, message, scriptName);

        if (config.logToTerminal) {
            synchronized (logBuffer) {
                logBuffer.add(entry);
                if (logBuffer.size() > MAX_BUFFER_SIZE) {
                    logBuffer.remove(0);
                }
            }
        }

        if (config.logToFile) {
            writeToFile(entry);
        }

        if (config.logToChat) {
            sendToChat(entry);
        }

        // Broadcast telemetry to connected AI agents & external tools via WebSocket
        try {
            kasperstudios.kashub.api.server.KashubAPIServer.broadcast(
                new kasperstudios.kashub.api.server.events.ScriptOutputEvent(
                    entry.scriptName != null ? Math.abs(entry.scriptName.hashCode()) : -1,
                    entry.message,
                    entry.level.name().toLowerCase(),
                    System.currentTimeMillis()
                )
            );
        } catch (Throwable ignored) {}

        LOGGER.info(formatLogEntry(entry));
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, formatMessage(message, args));
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void info(String message, Object... args) {
        log(LogLevel.INFO, formatMessage(message, args));
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void warn(String message, Object... args) {
        log(LogLevel.WARN, formatMessage(message, args));
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void error(String message, Object... args) {
        log(LogLevel.ERROR, formatMessage(message, args));
    }

    public void error(String message, Throwable t) {
        log(LogLevel.ERROR, message + ": " + t.getMessage());
        LOGGER.error(message, t);
    }

    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    public void scriptLog(String scriptName, LogLevel level, String message) {
        log(level, message, scriptName);
    }

    public void scriptLogAppend(String scriptName, LogLevel level, String message) {
        KashubConfig config = KashubConfig.getInstance();
        if (!config.enableLogging || !config.logToTerminal) return;

        synchronized (logBuffer) {
            if (message.contains("\n")) {
                String[] parts = message.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i == 0) {
                        // Append first part to last entry if it matches
                        if (!logBuffer.isEmpty()) {
                            LogEntry last = logBuffer.get(logBuffer.size() - 1);
                            if (last.level == level && java.util.Objects.equals(last.scriptName, scriptName) && !last.message.endsWith("\n")) {
                                logBuffer.set(logBuffer.size() - 1, new LogEntry(last.timestamp, level, last.message + parts[i], scriptName));
                            } else {
                                log(level, parts[i], scriptName);
                            }
                        } else {
                            log(level, parts[i], scriptName);
                        }
                    } else {
                        // All subsequent parts start a new line
                        log(level, parts[i], scriptName);
                    }
                }
                return;
            }

            if (!logBuffer.isEmpty()) {
                LogEntry last = logBuffer.get(logBuffer.size() - 1);
                // Only append if it's the same script/level AND the last entry didn't just end with a newline (optional, but safer)
                if (last.level == level && java.util.Objects.equals(last.scriptName, scriptName)) {
                    logBuffer.set(logBuffer.size() - 1, new LogEntry(last.timestamp, level, last.message + message, scriptName));
                    return;
                }
            }
            log(level, message, scriptName);
        }
    }

    public void printOut(String message) {
        KashubConfig config = KashubConfig.getInstance();
        
        if (config.printToTerminal) {
            LocalDateTime now = LocalDateTime.now();
            LogEntry entry = new LogEntry(now, LogLevel.INFO, message, "PRINT");
            synchronized (logBuffer) {
                logBuffer.add(entry);
                if (logBuffer.size() > MAX_BUFFER_SIZE) {
                    logBuffer.remove(0);
                }
            }
        }
        
        if (config.printToChat) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                net.minecraft.network.chat.Component chatMessage = net.minecraft.network.chat.Component.literal("[Print] " + message).withStyle(ChatFormatting.WHITE);
                client.player.displayClientMessage(chatMessage, false);
            }
        }
        
        LOGGER.info("[Print] {}", message);
    }

    private void writeToFile(LogEntry entry) {
        String fileName = "kashub_" + entry.timestamp.format(DATE_FORMAT) + ".log";
        Path logFile = LOG_PATH.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(formatLogEntry(entry));
            writer.newLine();
        } catch (IOException e) {
            LOGGER.error("Failed to write to log file: {}", e.getMessage());
        }
    }

    private void sendToChat(LogEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String prefix = "[KH] ";
            String text = prefix + entry.message;

            Component chatMessage = Component.literal(text).withStyle(entry.level.color);
            client.player.displayClientMessage(chatMessage, false);
        }
    }

    private String formatLogEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.timestamp.format(TIME_FORMAT)).append("] ");
        sb.append("[").append(entry.level.name()).append("] ");

        if (entry.scriptName != null && !entry.scriptName.isEmpty()) {
            sb.append("[").append(entry.scriptName).append("] ");
        }

        sb.append(entry.message);
        return sb.toString();
    }

    public List<LogEntry> getRecentLogs(int count) {
        synchronized (logBuffer) {
            int start = Math.max(0, logBuffer.size() - count);
            return new ArrayList<>(logBuffer.subList(start, logBuffer.size()));
        }
    }

    public List<LogEntry> getLogsByLevel(LogLevel level) {
        synchronized (logBuffer) {
            List<LogEntry> filtered = new ArrayList<>();
            for (LogEntry entry : logBuffer) {
                if (entry.level == level) {
                    filtered.add(entry);
                }
            }
            return filtered;
        }
    }

    public void clearBuffer() {
        synchronized (logBuffer) {
            logBuffer.clear();
        }
    }

    public void clear() {
        clearBuffer();
    }

    private String formatMessage(String message, Object... args) {
        if (message == null) return "null";
        if (args == null || args.length == 0) return message;

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;

        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    result.append(args[argIndex] != null ? args[argIndex].toString() : "null");
                    argIndex++;
                }
                i += 2;
            } else {
                result.append(message.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    public static class LogEntry {
        public final LocalDateTime timestamp;
        public final LogLevel level;
        public final String message;
        public final String scriptName;

        public LogEntry(LocalDateTime timestamp, LogLevel level, String message, String scriptName) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
            this.scriptName = scriptName;
        }
    }
}

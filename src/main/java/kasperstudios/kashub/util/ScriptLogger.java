package kasperstudios.kashub.util;

import kasperstudios.kashub.config.KashubConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Система логирования для скриптов
 */
public class ScriptLogger {
    private static ScriptLogger instance;
    private static final Path LOG_PATH = Paths.get("logs", "kashub");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private final List<LogEntry> logBuffer = new ArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000;
    
    public enum LogLevel {
        DEBUG(0, Formatting.GRAY),
        INFO(1, Formatting.WHITE),
        WARN(2, Formatting.YELLOW),
        ERROR(3, Formatting.RED),
        SUCCESS(1, Formatting.GREEN);
        
        public final int priority;
        public final Formatting color;
        
        LogLevel(int priority, Formatting color) {
            this.priority = priority;
            this.color = color;
        }
    }

    private ScriptLogger() {
        try {
            Files.createDirectories(LOG_PATH);
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
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
        
        if (!config.enableLogging) return;
        
        // Проверяем уровень логирования
        LogLevel configLevel = LogLevel.valueOf(config.logLevel.toUpperCase());
        if (level.priority < configLevel.priority) return;
        
        LocalDateTime now = LocalDateTime.now();
        LogEntry entry = new LogEntry(now, level, message, scriptName);
        
        // Добавляем в буфер
        synchronized (logBuffer) {
            logBuffer.add(entry);
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.remove(0);
            }
        }
        
        // Логируем в файл
        if (config.logToFile) {
            writeToFile(entry);
        }
        
        // Логируем в чат
        if (config.logToChat) {
            sendToChat(entry);
        }
        
        // Всегда выводим в консоль
        System.out.println(formatLogEntry(entry));
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }

    public void success(String message) {
        log(LogLevel.SUCCESS, message);
    }

    public void scriptLog(String scriptName, LogLevel level, String message) {
        log(level, message, scriptName);
    }

    private void writeToFile(LogEntry entry) {
        String fileName = "kashub_" + entry.timestamp.format(DATE_FORMAT) + ".log";
        Path logFile = LOG_PATH.resolve(fileName);
        
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(formatLogEntry(entry));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    private void sendToChat(LogEntry entry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            String prefix = "[KH] ";
            String text = prefix + entry.message;
            
            Text chatMessage = Text.literal(text).formatted(entry.level.color);
            client.player.sendMessage(chatMessage, false);
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

    /**
     * Запись лога
     */
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

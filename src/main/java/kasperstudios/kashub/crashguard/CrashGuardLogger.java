package kasperstudios.kashub.crashguard;

import kasperstudios.kashub.crashguard.CrashGuard.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Логгер для CrashGuard - записывает события в per-script лог-файлы
 */
public class CrashGuardLogger {
    private static final Logger LOGGER = LogManager.getLogger("CrashGuardLogger");
    private static final Path LOGS_DIR = Paths.get("logs", "crashguard");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static CrashGuardLogger instance;
    
    // Буферы для записи (чтобы не писать на каждое событие)
    private final Map<String, List<LogEntry>> buffers = new ConcurrentHashMap<>();
    private static final int BUFFER_SIZE = 10;
    private static final long FLUSH_INTERVAL_MS = 5000;
    
    private long lastFlush = System.currentTimeMillis();
    private boolean enabled = true;
    
    private CrashGuardLogger() {
        ensureLogsDirectory();
        
        // Подписываемся на события CrashGuard
        CrashGuard.getInstance().onWarning(this::logWarning);
    }
    
    public static CrashGuardLogger getInstance() {
        if (instance == null) {
            instance = new CrashGuardLogger();
        }
        return instance;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    private void ensureLogsDirectory() {
        try {
            Files.createDirectories(LOGS_DIR);
        } catch (IOException e) {
            LOGGER.error("Failed to create crashguard logs directory", e);
        }
    }
    
    /**
     * Логирует предупреждение CrashGuard
     */
    private void logWarning(WarningType type, WarningData data) {
        if (!enabled) return;
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now(),
            type,
            data.context,
            data.value,
            data.limit
        );
        
        List<LogEntry> buffer = buffers.computeIfAbsent(data.scriptName, k -> new ArrayList<>());
        synchronized (buffer) {
            buffer.add(entry);
            
            if (buffer.size() >= BUFFER_SIZE) {
                flushBuffer(data.scriptName, buffer);
            }
        }
        
        // Периодический flush
        if (System.currentTimeMillis() - lastFlush > FLUSH_INTERVAL_MS) {
            flushAll();
        }
    }
    
    /**
     * Записывает произвольное сообщение в лог скрипта
     */
    public void log(String scriptName, String level, String message) {
        if (!enabled) return;
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now(),
            null,
            level + ": " + message,
            0,
            0
        );
        
        List<LogEntry> buffer = buffers.computeIfAbsent(scriptName, k -> new ArrayList<>());
        synchronized (buffer) {
            buffer.add(entry);
        }
    }
    
    /**
     * Сбрасывает буфер в файл
     */
    private void flushBuffer(String scriptName, List<LogEntry> buffer) {
        if (buffer.isEmpty()) return;
        
        Path logFile = LOGS_DIR.resolve(sanitizeFileName(scriptName) + ".log");
        
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            for (LogEntry entry : buffer) {
                writer.write(formatEntry(entry));
                writer.newLine();
            }
            
            buffer.clear();
            
        } catch (IOException e) {
            LOGGER.error("Failed to write crashguard log for: {}", scriptName, e);
        }
    }
    
    /**
     * Сбрасывает все буферы
     */
    public void flushAll() {
        for (Map.Entry<String, List<LogEntry>> entry : buffers.entrySet()) {
            synchronized (entry.getValue()) {
                flushBuffer(entry.getKey(), entry.getValue());
            }
        }
        lastFlush = System.currentTimeMillis();
    }
    
    /**
     * Форматирует запись лога
     */
    private String formatEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(entry.timestamp.format(TIME_FORMAT)).append("] ");
        
        if (entry.type != null) {
            sb.append(entry.type.name()).append(" ");
        }
        
        sb.append(entry.context);
        
        if (entry.value > 0 || entry.limit > 0) {
            sb.append(": ").append(entry.value);
            if (entry.limit > 0) {
                sb.append(" > ").append(entry.limit);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Читает лог скрипта
     */
    public List<String> readLog(String scriptName, int maxLines) {
        Path logFile = LOGS_DIR.resolve(sanitizeFileName(scriptName) + ".log");
        
        if (!Files.exists(logFile)) {
            return Collections.emptyList();
        }
        
        try {
            List<String> allLines = Files.readAllLines(logFile);
            if (allLines.size() <= maxLines) {
                return allLines;
            }
            return allLines.subList(allLines.size() - maxLines, allLines.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to read crashguard log for: {}", scriptName, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Очищает лог скрипта
     */
    public void clearLog(String scriptName) {
        Path logFile = LOGS_DIR.resolve(sanitizeFileName(scriptName) + ".log");
        try {
            Files.deleteIfExists(logFile);
        } catch (IOException e) {
            LOGGER.error("Failed to clear crashguard log for: {}", scriptName, e);
        }
    }
    
    /**
     * Получает статистику по логам
     */
    public Map<String, LogStats> getLogStats() {
        Map<String, LogStats> stats = new HashMap<>();
        
        try {
            if (Files.exists(LOGS_DIR)) {
                Files.list(LOGS_DIR)
                    .filter(p -> p.toString().endsWith(".log"))
                    .forEach(path -> {
                        try {
                            String name = path.getFileName().toString().replace(".log", "");
                            long size = Files.size(path);
                            long lines = Files.lines(path).count();
                            stats.put(name, new LogStats(size, (int) lines));
                        } catch (IOException ignored) {}
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get log stats", e);
        }
        
        return stats;
    }
    
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    // Вспомогательные классы
    
    private static class LogEntry {
        final LocalDateTime timestamp;
        final WarningType type;
        final String context;
        final long value;
        final long limit;
        
        LogEntry(LocalDateTime timestamp, WarningType type, String context, long value, long limit) {
            this.timestamp = timestamp;
            this.type = type;
            this.context = context;
            this.value = value;
            this.limit = limit;
        }
    }
    
    public static class LogStats {
        public final long sizeBytes;
        public final int lineCount;
        
        public LogStats(long sizeBytes, int lineCount) {
            this.sizeBytes = sizeBytes;
            this.lineCount = lineCount;
        }
    }
}

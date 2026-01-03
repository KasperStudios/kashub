package kasperstudios.kashub.debug;

import kasperstudios.kashub.util.ScriptLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ProfilerManager {
    private static ProfilerManager instance;
    private final Map<String, CommandStats> stats = new ConcurrentHashMap<>();
    private volatile boolean isEnabled = true;
    private volatile long startTime = System.nanoTime();
    private final AtomicLong totalExecutionTime = new AtomicLong(0);

    private ProfilerManager() {
    }

    public static ProfilerManager getInstance() {
        if (instance == null) {
            instance = new ProfilerManager();
        }
        return instance;
    }

    public void start() {
        isEnabled = true;
        stats.clear();
        startTime = System.nanoTime();
        totalExecutionTime.set(0);
        ScriptLogger.getInstance().info("Profiler started");
    }

    public void stop() {
        isEnabled = false;
        ScriptLogger.getInstance().info("Profiler stopped");
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (enabled && startTime == 0) {
            startTime = System.nanoTime();
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void recordCommand(String command, long durationNs) {
        if (!isEnabled) return;

        stats.computeIfAbsent(command, k -> new CommandStats(command))
            .addExecution(durationNs);
        totalExecutionTime.addAndGet(durationNs);
    }

    public void clear() {
        stats.clear();
        totalExecutionTime.set(0);
        startTime = System.nanoTime();
        ScriptLogger.getInstance().debug("Profiler data cleared");
    }

    public List<Map.Entry<String, CommandStats>> getTopCommands(int limit) {
        return stats.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().getTotalTime(), e1.getValue().getTotalTime()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<Map.Entry<String, CommandStats>> getTopSlowest(int limit) {
        return stats.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getAverageMs(), e1.getValue().getAverageMs()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<Map.Entry<String, CommandStats>> getTopMostCalled(int limit) {
        return stats.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().getExecutions(), e1.getValue().getExecutions()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public Map<String, CommandStats> getAllStats() {
        return new HashMap<>(stats);
    }

    public double getTotalExecutionTimeMs() {
        return totalExecutionTime.get() / 1_000_000.0;
    }

    public double getProfilingDurationMs() {
        if (startTime == 0) return 0;
        return (System.nanoTime() - startTime) / 1_000_000.0;
    }

    public int getCommandCount() {
        return stats.size();
    }

    public long getTotalExecutions() {
        return stats.values().stream()
            .mapToLong(CommandStats::getExecutions)
            .sum();
    }

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== KHScript Profiler Report ===\n\n");

        sb.append(String.format("Profiling Duration: %.2f ms\n", getProfilingDurationMs()));
        sb.append(String.format("Total Execution Time: %.2f ms\n", getTotalExecutionTimeMs()));
        sb.append(String.format("Commands Profiled: %d\n", getCommandCount()));
        sb.append(String.format("Total Executions: %d\n\n", getTotalExecutions()));

        sb.append("Top 10 Hotspots (by total time):\n");
        sb.append(String.format("%-20s %10s %12s %12s %12s %8s\n",
            "Command", "Count", "Total (ms)", "Avg (ms)", "Max (ms)", "%"));
        sb.append("-".repeat(80)).append("\n");

        long totalTime = totalExecutionTime.get();
        for (Map.Entry<String, CommandStats> entry : getTopCommands(10)) {
            CommandStats stat = entry.getValue();
            sb.append(String.format("%-20s %10d %12.3f %12.3f %12.3f %7.2f%%\n",
                entry.getKey(),
                stat.getExecutions(),
                stat.getTotalTime() / 1_000_000.0,
                stat.getAverageMs(),
                stat.getMaxTime() / 1_000_000.0,
                (stat.getTotalTime() * 100.0) / totalTime));
        }

        sb.append("\n");

        sb.append("Top 10 Slowest Commands (by average time):\n");
        sb.append(String.format("%-20s %10s %12s %12s %12s\n",
            "Command", "Count", "Avg (ms)", "Max (ms)", "Min (ms)"));
        sb.append("-".repeat(70)).append("\n");

        for (Map.Entry<String, CommandStats> entry : getTopSlowest(10)) {
            CommandStats stat = entry.getValue();
            sb.append(String.format("%-20s %10d %12.3f %12.3f %12.3f\n",
                entry.getKey(),
                stat.getExecutions(),
                stat.getAverageMs(),
                stat.getMaxTime() / 1_000_000.0,
                stat.getMinTime() / 1_000_000.0));
        }

        return sb.toString();
    }

    public String exportToJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"profilingDurationMs\": ").append(getProfilingDurationMs()).append(",\n");
        json.append("  \"totalExecutionTimeMs\": ").append(getTotalExecutionTimeMs()).append(",\n");
        json.append("  \"commandCount\": ").append(getCommandCount()).append(",\n");
        json.append("  \"totalExecutions\": ").append(getTotalExecutions()).append(",\n");
        json.append("  \"entries\": [\n");

        List<Map.Entry<String, CommandStats>> allEntries = new ArrayList<>(stats.entrySet());
        long totalTime = totalExecutionTime.get();

        for (int i = 0; i < allEntries.size(); i++) {
            Map.Entry<String, CommandStats> entry = allEntries.get(i);
            CommandStats stat = entry.getValue();
            json.append("    {\n");
            json.append("      \"command\": \"").append(entry.getKey()).append("\",\n");
            json.append("      \"count\": ").append(stat.getExecutions()).append(",\n");
            json.append("      \"totalTimeMs\": ").append(stat.getTotalTime() / 1_000_000.0).append(",\n");
            json.append("      \"averageTimeMs\": ").append(stat.getAverageMs()).append(",\n");
            json.append("      \"maxTimeMs\": ").append(stat.getMaxTime() / 1_000_000.0).append(",\n");
            json.append("      \"minTimeMs\": ").append(stat.getMinTime() / 1_000_000.0).append(",\n");
            json.append("      \"percentage\": ").append((stat.getTotalTime() * 100.0) / totalTime).append("\n");
            json.append("    }");
            if (i < allEntries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    public String exportToChromeTracing() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");

        List<Map.Entry<String, CommandStats>> allEntries = new ArrayList<>(stats.entrySet());
        long timestamp = startTime / 1000;

        for (int i = 0; i < allEntries.size(); i++) {
            Map.Entry<String, CommandStats> entry = allEntries.get(i);
            CommandStats stat = entry.getValue();

            json.append("  {\n");
            json.append("    \"name\": \"").append(entry.getKey()).append("\",\n");
            json.append("    \"cat\": \"command\",\n");
            json.append("    \"ph\": \"X\",\n");
            json.append("    \"ts\": ").append(timestamp).append(",\n");
            json.append("    \"dur\": ").append((long)(stat.getAverageMs() * 1000)).append(",\n");
            json.append("    \"pid\": 1,\n");
            json.append("    \"tid\": 1,\n");
            json.append("    \"args\": {\n");
            json.append("      \"count\": ").append(stat.getExecutions()).append(",\n");
            json.append("      \"totalMs\": ").append(stat.getTotalTime() / 1_000_000.0).append(",\n");
            json.append("      \"avgMs\": ").append(stat.getAverageMs()).append(",\n");
            json.append("      \"maxMs\": ").append(stat.getMaxTime() / 1_000_000.0).append("\n");
            json.append("    }\n");
            json.append("  }");

            if (i < allEntries.size() - 1) {
                json.append(",");
            }
            json.append("\n");

            timestamp += (long)(stat.getAverageMs() * 1000);
        }

        json.append("]\n");
        return json.toString();
    }

    public void saveReport(String filename) throws IOException {
        Path logsDir = Paths.get("logs", "kashub", "profiler");
        Files.createDirectories(logsDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path reportPath = logsDir.resolve(filename + "_" + timestamp + ".txt");

        Files.writeString(reportPath, generateReport());
        ScriptLogger.getInstance().info("Profile report saved to: {}", reportPath);
    }

    public void saveJSON(String filename) throws IOException {
        Path logsDir = Paths.get("logs", "kashub", "profiler");
        Files.createDirectories(logsDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path jsonPath = logsDir.resolve(filename + "_" + timestamp + ".json");

        Files.writeString(jsonPath, exportToJSON());
        ScriptLogger.getInstance().info("Profile JSON saved to: {}", jsonPath);
    }

    public void saveChromeTracing(String filename) throws IOException {
        Path logsDir = Paths.get("logs", "kashub", "profiler");
        Files.createDirectories(logsDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path tracePath = logsDir.resolve(filename + "_" + timestamp + "_trace.json");

        Files.writeString(tracePath, exportToChromeTracing());
        ScriptLogger.getInstance().info("Chrome Tracing saved to: {}", tracePath);
        ScriptLogger.getInstance().info("Open in Chrome: chrome://tracing");
    }

    public static class CommandStats {
        private final String commandName;
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong executions = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);

        public CommandStats(String commandName) {
            this.commandName = commandName;
        }

        public synchronized void addExecution(long durationNs) {
            totalTime.addAndGet(durationNs);
            executions.incrementAndGet();

            long currentMax = maxTime.get();
            while (durationNs > currentMax) {
                if (maxTime.compareAndSet(currentMax, durationNs)) {
                    break;
                }
                currentMax = maxTime.get();
            }

            long currentMin = minTime.get();
            while (durationNs < currentMin) {
                if (minTime.compareAndSet(currentMin, durationNs)) {
                    break;
                }
                currentMin = minTime.get();
            }
        }

        public String getCommandName() {
            return commandName;
        }

        public long getTotalTime() {
            return totalTime.get();
        }

        public int getExecutions() {
            return (int) executions.get();
        }

        public long getMaxTime() {
            return maxTime.get();
        }

        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public double getAverageMs() {
            long exec = executions.get();
            return exec == 0 ? 0 : (totalTime.get() / 1_000_000.0) / exec;
        }
    }
}

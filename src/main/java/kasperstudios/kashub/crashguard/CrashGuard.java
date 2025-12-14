package kasperstudios.kashub.crashguard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * CrashGuard - система защиты от крашей для KHScript
 * 
 * Детекторы:
 * 1. CPU Time Limit — лимит CPU времени на тик
 * 2. Action Rate Limiter — лимиты на действия
 * 3. Infinite Loop Detector — обнаружение зацикливания
 * 4. Memory Guard — контроль памяти
 * 5. Recursion Depth Guard — защита от рекурсии
 */
public class CrashGuard {
    private static final Logger LOGGER = LogManager.getLogger("CrashGuard");
    private static CrashGuard instance;
    
    // Глобальные настройки
    private Strictness globalStrictness = Strictness.MEDIUM;
    private long cpuLimitMs = 10;
    private int actionsPerSecond = 20;
    private long memoryGrowthLimitBytes = 1024 * 1024; // 1MB
    private int maxRecursionDepth = 100;
    private int maxLoopIterations = 5000;
    
    // Per-script состояние
    private final Map<String, ScriptGuardState> scriptStates = new ConcurrentHashMap<>();
    
    // Глобальные счётчики
    private long globalCpuTimeThisTick = 0;
    private int globalActionsThisSecond = 0;
    private long lastSecondReset = System.currentTimeMillis();
    
    // Обработчики событий
    private final List<BiConsumer<WarningType, WarningData>> warningHandlers = new ArrayList<>();
    
    // Временная пауза защиты
    private long pauseUntil = 0;
    
    public enum Strictness {
        OFF(0),
        LOOSE(1),
        MEDIUM(2),
        STRICT(3),
        PARANOID(4);
        
        public final int level;
        Strictness(int level) { this.level = level; }
        
        public static Strictness fromString(String s) {
            try {
                return valueOf(s.toUpperCase());
            } catch (Exception e) {
                return MEDIUM;
            }
        }
    }
    
    public enum WarningType {
        CPU_LIMIT,
        ACTION_RATE,
        INFINITE_LOOP,
        MEMORY_GROWTH,
        RECURSION_DEPTH,
        SCRIPT_PAUSED,
        SCRIPT_STOPPED
    }
    
    private CrashGuard() {}
    
    public static CrashGuard getInstance() {
        if (instance == null) {
            instance = new CrashGuard();
        }
        return instance;
    }
    
    // ==================== Конфигурация ====================
    
    public void setGlobalStrictness(Strictness strictness) {
        this.globalStrictness = strictness;
        applyStrictnessDefaults(strictness);
        LOGGER.info("CrashGuard strictness set to: {}", strictness);
    }
    
    private void applyStrictnessDefaults(Strictness strictness) {
        switch (strictness) {
            case OFF:
                cpuLimitMs = Long.MAX_VALUE;
                actionsPerSecond = Integer.MAX_VALUE;
                maxLoopIterations = Integer.MAX_VALUE;
                break;
            case LOOSE:
                cpuLimitMs = 50;
                actionsPerSecond = 50;
                maxLoopIterations = 10000;
                break;
            case MEDIUM:
                cpuLimitMs = 10;
                actionsPerSecond = 20;
                maxLoopIterations = 5000;
                break;
            case STRICT:
                cpuLimitMs = 5;
                actionsPerSecond = 10;
                maxLoopIterations = 2000;
                break;
            case PARANOID:
                cpuLimitMs = 2;
                actionsPerSecond = 5;
                maxLoopIterations = 500;
                break;
        }
    }
    
    public void configure(long cpuLimitMs, int actionsPerSecond, long memoryLimitBytes, int maxRecursion) {
        this.cpuLimitMs = cpuLimitMs;
        this.actionsPerSecond = actionsPerSecond;
        this.memoryGrowthLimitBytes = memoryLimitBytes;
        this.maxRecursionDepth = maxRecursion;
    }
    
    public Strictness getGlobalStrictness() {
        return globalStrictness;
    }
    
    // ==================== Per-Script настройки ====================
    
    public void setScriptStrictness(String scriptName, Strictness strictness) {
        getOrCreateState(scriptName).strictness = strictness;
    }
    
    public void setScriptConfig(String scriptName, ScriptGuardConfig config) {
        ScriptGuardState state = getOrCreateState(scriptName);
        state.config = config;
    }
    
    private ScriptGuardState getOrCreateState(String scriptName) {
        return scriptStates.computeIfAbsent(scriptName, k -> new ScriptGuardState(scriptName));
    }
    
    // ==================== CPU Time Monitor ====================
    
    public void startCpuMeasure(String scriptName) {
        if (isPaused()) return;
        ScriptGuardState state = getOrCreateState(scriptName);
        state.cpuStartTime = System.nanoTime();
    }
    
    public boolean endCpuMeasure(String scriptName) {
        if (isPaused()) return true;
        
        ScriptGuardState state = scriptStates.get(scriptName);
        if (state == null || state.cpuStartTime == 0) return true;
        
        long elapsed = (System.nanoTime() - state.cpuStartTime) / 1_000_000; // ms
        state.cpuStartTime = 0;
        state.cpuTimeThisTick += elapsed;
        globalCpuTimeThisTick += elapsed;
        
        long limit = getEffectiveCpuLimit(state);
        
        if (elapsed > limit) {
            WarningData data = new WarningData(scriptName, "cpu_time", elapsed, limit);
            fireWarning(WarningType.CPU_LIMIT, data);
            
            if (getEffectiveStrictness(state).level >= Strictness.STRICT.level) {
                pauseScript(scriptName, 1000);
                return false;
            }
        }
        
        return true;
    }
    
    private long getEffectiveCpuLimit(ScriptGuardState state) {
        if (state.config != null && state.config.cpuLimitMs > 0) {
            return state.config.cpuLimitMs;
        }
        return cpuLimitMs;
    }
    
    // ==================== Action Rate Limiter ====================
    
    public boolean checkActionRate(String scriptName, String actionType) {
        if (isPaused()) return true;
        
        resetSecondCountersIfNeeded();
        
        ScriptGuardState state = getOrCreateState(scriptName);
        state.actionsThisSecond++;
        globalActionsThisSecond++;
        
        int limit = getEffectiveActionLimit(state);
        
        if (state.actionsThisSecond > limit) {
            WarningData data = new WarningData(scriptName, actionType, state.actionsThisSecond, limit);
            fireWarning(WarningType.ACTION_RATE, data);
            
            if (getEffectiveStrictness(state).level >= Strictness.MEDIUM.level) {
                state.throttledUntil = System.currentTimeMillis() + 100; // throttle 100ms
                return false;
            }
        }
        
        return !isThrottled(state);
    }
    
    private int getEffectiveActionLimit(ScriptGuardState state) {
        if (state.config != null && state.config.actionsPerSecond > 0) {
            return state.config.actionsPerSecond;
        }
        return actionsPerSecond;
    }
    
    private boolean isThrottled(ScriptGuardState state) {
        return System.currentTimeMillis() < state.throttledUntil;
    }
    
    private void resetSecondCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastSecondReset >= 1000) {
            globalActionsThisSecond = 0;
            lastSecondReset = now;
            
            for (ScriptGuardState state : scriptStates.values()) {
                state.actionsThisSecond = 0;
            }
        }
    }
    
    // ==================== Infinite Loop Detector ====================
    
    public boolean checkLoopIteration(String scriptName, String loopId) {
        if (isPaused()) return true;
        
        ScriptGuardState state = getOrCreateState(scriptName);
        LoopState loop = state.loops.computeIfAbsent(loopId, k -> new LoopState());
        
        loop.iterations++;
        
        // Проверяем время с последнего sleep/yield
        long timeSinceYield = System.currentTimeMillis() - loop.lastYieldTime;
        
        int limit = getEffectiveLoopLimit(state);
        
        // Если много итераций без yield
        if (loop.iterations > limit && timeSinceYield > 1000) {
            WarningData data = new WarningData(scriptName, loopId, loop.iterations, limit);
            fireWarning(WarningType.INFINITE_LOOP, data);
            
            if (getEffectiveStrictness(state).level >= Strictness.STRICT.level) {
                stopScript(scriptName, "Infinite loop detected");
                return false;
            } else if (getEffectiveStrictness(state).level >= Strictness.MEDIUM.level) {
                pauseScript(scriptName, 1000);
                loop.iterations = 0;
                return false;
            }
        }
        
        // Warning при приближении к лимиту
        if (loop.iterations > limit / 2 && timeSinceYield > 500) {
            WarningData data = new WarningData(scriptName, loopId, loop.iterations, limit);
            fireWarning(WarningType.INFINITE_LOOP, data);
        }
        
        return true;
    }
    
    public void markLoopYield(String scriptName, String loopId) {
        ScriptGuardState state = scriptStates.get(scriptName);
        if (state == null) return;
        
        LoopState loop = state.loops.get(loopId);
        if (loop != null) {
            loop.lastYieldTime = System.currentTimeMillis();
            loop.iterations = 0;
        }
    }
    
    private int getEffectiveLoopLimit(ScriptGuardState state) {
        if (state.config != null && state.config.maxLoopIterations > 0) {
            return state.config.maxLoopIterations;
        }
        return maxLoopIterations;
    }
    
    // ==================== Recursion Depth Guard ====================
    
    public boolean checkRecursion(String scriptName, String functionName) {
        if (isPaused()) return true;
        
        ScriptGuardState state = getOrCreateState(scriptName);
        state.recursionDepth++;
        
        int limit = getEffectiveRecursionLimit(state);
        
        if (state.recursionDepth > limit) {
            WarningData data = new WarningData(scriptName, functionName, state.recursionDepth, limit);
            fireWarning(WarningType.RECURSION_DEPTH, data);
            
            if (getEffectiveStrictness(state).level >= Strictness.MEDIUM.level) {
                return false; // Прерываем рекурсию
            }
        }
        
        return true;
    }
    
    public void exitRecursion(String scriptName) {
        ScriptGuardState state = scriptStates.get(scriptName);
        if (state != null && state.recursionDepth > 0) {
            state.recursionDepth--;
        }
    }
    
    private int getEffectiveRecursionLimit(ScriptGuardState state) {
        if (state.config != null && state.config.maxRecursionDepth > 0) {
            return state.config.maxRecursionDepth;
        }
        return maxRecursionDepth;
    }
    
    // ==================== Memory Guard ====================
    
    public void trackMemoryUsage(String scriptName, long bytesUsed) {
        if (isPaused()) return;
        
        ScriptGuardState state = getOrCreateState(scriptName);
        long growth = bytesUsed - state.lastMemoryUsage;
        state.lastMemoryUsage = bytesUsed;
        state.memoryGrowthThisMinute += Math.max(0, growth);
        
        if (state.memoryGrowthThisMinute > memoryGrowthLimitBytes) {
            WarningData data = new WarningData(scriptName, "memory", 
                state.memoryGrowthThisMinute, memoryGrowthLimitBytes);
            fireWarning(WarningType.MEMORY_GROWTH, data);
            
            if (getEffectiveStrictness(state).level >= Strictness.STRICT.level) {
                cleanupScript(scriptName);
            }
        }
    }
    
    // ==================== Управление скриптами ====================
    
    public void pauseScript(String scriptName, long durationMs) {
        ScriptGuardState state = getOrCreateState(scriptName);
        state.pausedUntil = System.currentTimeMillis() + durationMs;
        state.isPaused = true;
        
        WarningData data = new WarningData(scriptName, "pause", durationMs, 0);
        fireWarning(WarningType.SCRIPT_PAUSED, data);
        
        LOGGER.warn("Script {} paused for {}ms", scriptName, durationMs);
    }
    
    public void stopScript(String scriptName, String reason) {
        ScriptGuardState state = getOrCreateState(scriptName);
        state.isStopped = true;
        
        WarningData data = new WarningData(scriptName, reason, 0, 0);
        fireWarning(WarningType.SCRIPT_STOPPED, data);
        
        LOGGER.error("Script {} stopped: {}", scriptName, reason);
    }
    
    public void cleanupScript(String scriptName) {
        ScriptGuardState state = scriptStates.get(scriptName);
        if (state != null) {
            state.loops.clear();
            state.memoryGrowthThisMinute = 0;
            state.recursionDepth = 0;
        }
        LOGGER.info("Script {} resources cleaned up", scriptName);
    }
    
    public boolean isScriptAllowed(String scriptName) {
        ScriptGuardState state = scriptStates.get(scriptName);
        if (state == null) return true;
        
        if (state.isStopped) return false;
        
        if (state.isPaused) {
            if (System.currentTimeMillis() >= state.pausedUntil) {
                state.isPaused = false;
                return true;
            }
            return false;
        }
        
        return true;
    }
    
    // ==================== Глобальная пауза ====================
    
    public void pauseGlobal(long durationMs) {
        pauseUntil = System.currentTimeMillis() + durationMs;
        LOGGER.info("CrashGuard paused for {}ms", durationMs);
    }
    
    private boolean isPaused() {
        return globalStrictness == Strictness.OFF || System.currentTimeMillis() < pauseUntil;
    }
    
    // ==================== События ====================
    
    public void onWarning(BiConsumer<WarningType, WarningData> handler) {
        warningHandlers.add(handler);
    }
    
    private void fireWarning(WarningType type, WarningData data) {
        for (BiConsumer<WarningType, WarningData> handler : warningHandlers) {
            try {
                handler.accept(type, data);
            } catch (Exception e) {
                LOGGER.error("Error in warning handler", e);
            }
        }
        
        // Логируем
        LOGGER.warn("[{}] {}: {} (value={}, limit={})", 
            data.scriptName, type, data.context, data.value, data.limit);
    }
    
    // ==================== Tick обработка ====================
    
    public void onTickStart() {
        globalCpuTimeThisTick = 0;
        for (ScriptGuardState state : scriptStates.values()) {
            state.cpuTimeThisTick = 0;
        }
    }
    
    public void onTickEnd() {
        // Проверяем общее CPU время
        if (globalCpuTimeThisTick > cpuLimitMs * 2) {
            LOGGER.warn("High global CPU usage: {}ms", globalCpuTimeThisTick);
        }
    }
    
    // ==================== Статистика ====================
    
    public Map<String, ScriptStats> getStats() {
        Map<String, ScriptStats> stats = new HashMap<>();
        for (Map.Entry<String, ScriptGuardState> entry : scriptStates.entrySet()) {
            ScriptGuardState state = entry.getValue();
            stats.put(entry.getKey(), new ScriptStats(
                state.cpuTimeThisTick,
                state.actionsThisSecond,
                state.memoryGrowthThisMinute,
                state.isPaused,
                state.isStopped,
                getEffectiveStrictness(state)
            ));
        }
        return stats;
    }
    
    public GlobalStats getGlobalStats() {
        return new GlobalStats(
            globalCpuTimeThisTick,
            globalActionsThisSecond,
            scriptStates.size(),
            globalStrictness
        );
    }
    
    private Strictness getEffectiveStrictness(ScriptGuardState state) {
        if (state.strictness != null) {
            return state.strictness;
        }
        return globalStrictness;
    }
    
    // ==================== Вспомогательные классы ====================
    
    public static class ScriptGuardState {
        final String scriptName;
        Strictness strictness = null;
        ScriptGuardConfig config = null;
        
        long cpuStartTime = 0;
        long cpuTimeThisTick = 0;
        int actionsThisSecond = 0;
        long throttledUntil = 0;
        
        Map<String, LoopState> loops = new ConcurrentHashMap<>();
        int recursionDepth = 0;
        
        long lastMemoryUsage = 0;
        long memoryGrowthThisMinute = 0;
        
        boolean isPaused = false;
        long pausedUntil = 0;
        boolean isStopped = false;
        
        ScriptGuardState(String scriptName) {
            this.scriptName = scriptName;
        }
    }
    
    public static class LoopState {
        int iterations = 0;
        long lastYieldTime = System.currentTimeMillis();
    }
    
    public static class ScriptGuardConfig {
        public long cpuLimitMs = 0;
        public int actionsPerSecond = 0;
        public int maxLoopIterations = 0;
        public int maxRecursionDepth = 0;
        public boolean allowInfiniteLoops = false;
        public boolean autoCleanup = true;
    }
    
    public static class WarningData {
        public final String scriptName;
        public final String context;
        public final long value;
        public final long limit;
        public final long timestamp;
        
        public WarningData(String scriptName, String context, long value, long limit) {
            this.scriptName = scriptName;
            this.context = context;
            this.value = value;
            this.limit = limit;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class ScriptStats {
        public final long cpuTimeMs;
        public final int actionsPerSec;
        public final long memoryGrowth;
        public final boolean isPaused;
        public final boolean isStopped;
        public final Strictness strictness;
        
        public ScriptStats(long cpuTimeMs, int actionsPerSec, long memoryGrowth,
                          boolean isPaused, boolean isStopped, Strictness strictness) {
            this.cpuTimeMs = cpuTimeMs;
            this.actionsPerSec = actionsPerSec;
            this.memoryGrowth = memoryGrowth;
            this.isPaused = isPaused;
            this.isStopped = isStopped;
            this.strictness = strictness;
        }
        
        public String getStatus() {
            if (isStopped) return "STOPPED";
            if (isPaused) return "PAUSED";
            return "OK";
        }
    }
    
    public static class GlobalStats {
        public final long totalCpuTimeMs;
        public final int totalActionsPerSec;
        public final int activeScripts;
        public final Strictness strictness;
        
        public GlobalStats(long totalCpuTimeMs, int totalActionsPerSec, 
                          int activeScripts, Strictness strictness) {
            this.totalCpuTimeMs = totalCpuTimeMs;
            this.totalActionsPerSec = totalActionsPerSec;
            this.activeScripts = activeScripts;
            this.strictness = strictness;
        }
    }
}

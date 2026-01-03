# Refactoring Plan: Remove ScriptInterpreter Singleton

## 🎯 Goal
Migrate from global `ScriptInterpreter` singleton to per-script `ScriptTask` architecture for better isolation, thread safety, and maintainability.

## 📊 Current Architecture Problems

### 1. Global State Issues
```java
// Current: ONE interpreter for ALL scripts
ScriptInterpreter.getInstance() // Shared by everyone!
```

**Problems:**
- Variables leak between scripts
- `shouldStop` flag affects all scripts globally
- Race conditions when multiple scripts run
- Hard to debug which script owns what

### 2. Dual Variable Storage
```java
// ScriptTask has its own variables
private final Map<String, String> variables = new ConcurrentHashMap<>();

// But also syncs with ScriptInterpreter
ScriptInterpreter.getInstance().setVariable(name, value);
```

**Problems:**
- Data duplication
- Synchronization overhead
- Potential inconsistencies

### 3. Event System Confusion
```java
// EventManager uses global interpreter
ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
interpreter.parseCommands(script);
```

**Problems:**
- Events from different scripts share state
- Hard to track which script registered which event

## 🏗️ Target Architecture

### New Design: ScriptTask is Self-Contained

```java
public class ScriptTask {
    // Each task has its own execution context
    private final ScriptExecutionContext context;
    
    // No more ScriptInterpreter dependency!
    public void parseAndQueue() {
        context.parseCommands(code);
    }
    
    public void tick() {
        context.executeNextCommand();
    }
}

public class ScriptExecutionContext {
    // Per-script state
    private final Map<String, String> variables;
    private final Map<String, Function> functions;
    private final Deque<CommandEntry> commandQueue;
    private boolean shouldStop;
    
    // Environment variables (shared, read-only)
    private final EnvironmentVariableProvider envProvider;
}
```

## 📋 Migration Plan

### Phase 1: Create New Classes (v0.9.0)
**Complexity:** Medium  
**Time:** 2-3 days  
**Risk:** Low (no breaking changes)

1. **Create `ScriptExecutionContext`**
   - Move parsing logic from `ScriptInterpreter`
   - Move command queue management
   - Keep per-script state isolated

2. **Create `EnvironmentVariableProvider`**
   - Extract environment variable logic
   - Make it shared/singleton (read-only)
   - Update values globally, read per-script

3. **Update `ScriptTask`**
   - Add `ScriptExecutionContext` field
   - Delegate to context instead of interpreter
   - Keep backward compatibility

**Files to create:**
- `src/main/java/kasperstudios/kashub/services/runtime/ScriptExecutionContext.java`
- `src/main/java/kasperstudios/kashub/services/runtime/EnvironmentVariableProvider.java`

### Phase 2: Migrate Core Features (v0.9.1)
**Complexity:** High  
**Time:** 1 week  
**Risk:** Medium (changes execution flow)

1. **Migrate Variable System**
   ```java
   // Old
   ScriptInterpreter.getInstance().setVariable(name, value);
   
   // New
   context.setVariable(name, value);
   ```

2. **Migrate Function System**
   ```java
   // Old
   ScriptInterpreter.getInstance().functions.put(name, func);
   
   // New
   context.defineFunction(name, func);
   ```

3. **Migrate Command Execution**
   ```java
   // Old
   ScriptInterpreter.getInstance().queueCommand(cmd, args);
   
   // New
   context.queueCommand(cmd, args);
   ```

**Files to modify:**
- `src/main/java/kasperstudios/kashub/services/runtime/ScriptTask.java`
- `src/main/java/kasperstudios/kashub/algorithm/events/EventManager.java`

### Phase 3: Update Event System (v0.9.2)
**Complexity:** Medium  
**Time:** 2-3 days  
**Risk:** Medium (events are critical)

1. **Events Create Temporary Context**
   ```java
   // EventManager.java
   public void fireEvent(String eventName, Map<String, Object> data) {
       String script = eventScripts.get(eventName);
       if (script != null) {
           // Create temporary context for event
           ScriptExecutionContext eventContext = new ScriptExecutionContext();
           
           // Set event variables
           for (Map.Entry<String, Object> entry : data.entrySet()) {
               eventContext.setVariable("event_" + entry.getKey(), 
                                       String.valueOf(entry.getValue()));
           }
           
           // Execute event script
           eventContext.parseCommands(script);
           eventContext.executeQueuedCommands();
       }
   }
   ```

2. **Track Event Ownership**
   ```java
   // Each script tracks its events
   private final Map<String, ScriptExecutionContext> eventContexts;
   ```

**Files to modify:**
- `src/main/java/kasperstudios/kashub/algorithm/events/EventManager.java`

### Phase 4: Update Commands (v0.9.3)
**Complexity:** Low-Medium  
**Time:** 3-4 days  
**Risk:** Low (mostly mechanical changes)

1. **Add Context Parameter to Commands**
   ```java
   // Old
   public interface Command {
       void execute(String[] args) throws Exception;
   }
   
   // New
   public interface Command {
       void execute(String[] args, ScriptExecutionContext context) throws Exception;
   }
   ```

2. **Update All Commands**
   - ~50 command files to update
   - Mostly find-replace operations
   - Test each command

**Files to modify:**
- `src/main/java/kasperstudios/kashub/algorithm/Command.java`
- All command implementations (~50 files)

### Phase 5: Update Debug/API (v0.9.4)
**Complexity:** Medium  
**Time:** 2-3 days  
**Risk:** Low (external interfaces)

1. **Debug Adapter Protocol**
   ```java
   // DebugManager.java
   public Map<String, String> getVariables(int scriptId) {
       ScriptTask task = ScriptTaskManager.getInstance().getTask(scriptId);
       if (task != null) {
           return task.getContext().getVariables();
       }
       return Collections.emptyMap();
   }
   ```

2. **REST API**
   ```java
   // VariablesEndpoint.java
   // Return per-script variables instead of global
   ```

**Files to modify:**
- `src/main/java/kasperstudios/kashub/debug/DebugManager.java`
- `src/main/java/kasperstudios/kashub/api/server/DebugEndpoint.java`
- `src/main/java/kasperstudios/kashub/api/server/VariablesEndpoint.java`

### Phase 6: Remove ScriptInterpreter (v1.0.0)
**Complexity:** Low  
**Time:** 1 day  
**Risk:** Low (cleanup)

1. **Delete Old Code**
   - Remove `ScriptInterpreter.java`
   - Remove all `getInstance()` calls
   - Clean up imports

2. **Update Documentation**
   - Update architecture docs
   - Update API docs
   - Update examples

**Files to delete:**
- `src/main/java/kasperstudios/kashub/algorithm/ScriptInterpreter.java`

## 📈 Benefits After Refactoring

### 1. Better Isolation
```java
// Each script has its own context
ScriptTask task1 = new ScriptTask(1, "script1", code1);
ScriptTask task2 = new ScriptTask(2, "script2", code2);

// Variables don't leak
task1.context.setVariable("x", "10");
task2.context.getVariable("x"); // null - isolated!
```

### 2. Thread Safety
```java
// No more global state = no race conditions
// Each task can run in parallel safely
```

### 3. Easier Testing
```java
@Test
public void testScript() {
    ScriptExecutionContext context = new ScriptExecutionContext();
    context.parseCommands("let x = 5\nprint $x");
    context.executeQueuedCommands();
    
    assertEquals("5", context.getVariable("x"));
}
```

### 4. Better Debugging
```java
// Clear ownership of variables/functions
// Easy to see which script is doing what
// No mysterious global state changes
```

### 5. Cleaner Code
```java
// Before: 15+ getInstance() calls per file
ScriptInterpreter.getInstance().setVariable(...)
ScriptInterpreter.getInstance().getVariable(...)
ScriptInterpreter.getInstance().updateEnvironmentVariables()

// After: Clean dependency injection
context.setVariable(...)
context.getVariable(...)
envProvider.update()
```

## ⚠️ Risks & Mitigation

### Risk 1: Breaking Changes
**Mitigation:**
- Keep backward compatibility during migration
- Deprecate old APIs gradually
- Provide migration guide

### Risk 2: Performance Impact
**Mitigation:**
- Profile before/after
- Optimize hot paths
- Use object pooling if needed

### Risk 3: Bugs During Migration
**Mitigation:**
- Migrate incrementally (6 phases)
- Test each phase thoroughly
- Keep old code until new code is stable

## 📊 Estimated Timeline

| Phase | Version | Time | Complexity | Risk |
|-------|---------|------|------------|------|
| 1. Create New Classes | v0.9.0 | 2-3 days | Medium | Low |
| 2. Migrate Core | v0.9.1 | 1 week | High | Medium |
| 3. Update Events | v0.9.2 | 2-3 days | Medium | Medium |
| 4. Update Commands | v0.9.3 | 3-4 days | Low-Medium | Low |
| 5. Update Debug/API | v0.9.4 | 2-3 days | Medium | Low |
| 6. Remove Old Code | v1.0.0 | 1 day | Low | Low |
| **TOTAL** | | **3-4 weeks** | | |

## 🎯 Quick Win: Minimal Refactoring (Alternative)

If full refactoring is too complex, here's a minimal approach:

### Option A: Keep ScriptInterpreter, Fix Issues
**Time:** 2-3 days  
**Complexity:** Low

1. **Make ScriptInterpreter Thread-Local**
   ```java
   private static final ThreadLocal<ScriptInterpreter> instance = 
       ThreadLocal.withInitial(ScriptInterpreter::new);
   
   public static ScriptInterpreter getInstance() {
       return instance.get();
   }
   ```

2. **Add Context ID**
   ```java
   public class ScriptInterpreter {
       private int contextId = -1; // Which script owns this?
       
       public void setContextId(int id) {
           this.contextId = id;
       }
   }
   ```

3. **Clear State Between Scripts**
   ```java
   public void reset() {
       shouldStop = false;
       commandQueue.clear();
       variables.clear();
       functions.clear();
   }
   ```

**Pros:**
- Quick fix
- Minimal changes
- Low risk

**Cons:**
- Still has global state issues
- Doesn't solve all problems
- Technical debt remains

## 💡 Recommendation

**For v0.9.0:** Start with Phase 1 (Create New Classes)
- Low risk
- No breaking changes
- Foundation for future work
- Can be done incrementally

**For v1.0.0:** Complete full refactoring
- Clean architecture
- Better maintainability
- Worth the investment

## 📚 Related Issues

- Bug #11: `shouldStop` blocks events (FIXED, but symptom of global state)
- Bug #10: Events don't work after restart (FIXED, but related to global state)
- Future: Multi-threaded script execution
- Future: Script sandboxing/security

## 🔗 References

- `BUGS_FOUND.md` - Current bugs related to global state
- `DEVELOPMENT_NOTES.md` - Architecture guidelines
- `ROADMAP_FUTURE.md` - Future features that need this refactoring

---

**Status:** 📋 Planning  
**Priority:** 🟡 Medium (not urgent, but important for v1.0)  
**Assigned:** Future development  
**Created:** 2026-01-03

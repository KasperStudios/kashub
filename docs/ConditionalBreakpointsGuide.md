# Conditional Breakpoints Guide

Advanced debugging features for Kashub scripts.

## 📋 Breakpoint Types

### 1. Regular Breakpoint (BREAKPOINT)
Always stops execution when hit.

**Usage:**
- Click on line number in editor
- Or use F9 key

**Example:**
```javascript
onEvent onDamage {
    log Health: $event_health  // ← Set breakpoint here
}
```

---

### 2. Conditional Breakpoint (CONDITIONAL)
Stops execution only if condition evaluates to true.

**Usage:**
- Right-click on breakpoint
- Select "Edit Breakpoint"
- Enter condition

**Condition Syntax:**
- Uses KHScript expression syntax
- Access all variables in current scope
- Supports: `==`, `!=`, `<`, `>`, `<=`, `>=`, `&&`, `||`

**Examples:**

**Health-based:**
```javascript
onEvent onDamage {
    log Health: $event_health
    // Condition: $event_health < 10
    // Pauses only when health drops below 10
}
```

**Block-based:**
```javascript
onEvent onBlockBreak {
    log Broke: $event_block
    // Condition: $event_block == "diamond_ore"
    // Pauses only when breaking diamond ore
}
```

**Complex conditions:**
```javascript
onEvent onAttack {
    log Target: $event_target_name
    // Condition: $event_target_type == "zombie" && $event_target_health < 5
    // Pauses only for low-health zombies
}
```

**Variable-based:**
```javascript
let counter = 0
loop 100 {
    counter++
    log Counter: $counter
    // Condition: $counter >= 50
    // Pauses when counter reaches 50
}
```

---

### 3. Logpoint (LOGPOINT)
Logs message without stopping execution.

**Usage:**
- Right-click on line
- Select "Add Logpoint"
- Enter message template

**Message Syntax:**
- Use `{$var}` to interpolate variables
- All variables in scope are available

**Examples:**

**Simple log:**
```javascript
onEvent onHunger {
    // Logpoint: "Food level is {$event_food}"
    // Logs without pausing
}
```

**Multiple variables:**
```javascript
onEvent onDamage {
    // Logpoint: "Health: {$event_health}, Damage: {$event_damage}"
}
```

**With calculations:**
```javascript
let x = 10
let y = 20
// Logpoint: "Position: X={$x}, Y={$y}"
```

---

## 🎯 Hit Conditions

Control when breakpoint triggers based on hit count.

**Syntax:**
- `>= N` - Pause after Nth hit
- `== N` - Pause only on Nth hit
- `% N == 0` - Pause every Nth hit
- `<= N` - Pause until Nth hit

**Examples:**

**Pause after 5th hit:**
```javascript
onEvent onTick {
    log Tick
    // Hit condition: >= 5
    // Pauses on 5th tick and after
}
```

**Pause every 10th hit:**
```javascript
loop 100 {
    log Iteration
    // Hit condition: % 10 == 0
    // Pauses on 10th, 20th, 30th... iterations
}
```

**Pause only on 3rd hit:**
```javascript
onEvent onAttack {
    log Attack
    // Hit condition: == 3
    // Pauses only on 3rd attack
}
```

---

## 🔧 API Usage

### Set Conditional Breakpoint
```java
DebugManager.getInstance().setConditionalBreakpoint(
    "script.kh",  // Script name
    15,           // Line number
    "$event_health < 10"  // Condition
);
```

### Set Logpoint
```java
DebugManager.getInstance().setLogpoint(
    "script.kh",
    20,
    "Health is {$event_health}"
);
```

### Set Hit Condition
```java
DebugManager.getInstance().setHitCondition(
    "script.kh",
    25,
    ">= 5"  // Pause after 5th hit
);
```

### Get Breakpoint Info
```java
Breakpoint bp = DebugManager.getInstance().getBreakpoint("script.kh", 15);
System.out.println("Type: " + bp.getType());
System.out.println("Condition: " + bp.getCondition());
System.out.println("Hit count: " + bp.getHitCount());
```

---

## 💡 Best Practices

### 1. Keep Conditions Simple
❌ Bad: `$event_health < 10 && $event_food > 5 && $event_x > 100 && $event_y < 64`
✅ Good: `$event_health < 10`

**Why:** Complex conditions slow down execution. Max 10 operators allowed.

### 2. Use Logpoints for Frequent Events
❌ Bad: Regular breakpoint in onTick (pauses 20 times/second)
✅ Good: Logpoint in onTick (logs without pausing)

### 3. Combine with Hit Conditions
```javascript
onEvent onTick {
    // Condition: $event_tick % 100 == 0
    // Hit condition: >= 5
    // Pauses every 100 ticks, starting from 5th occurrence
}
```

### 4. Use Event Variables
Conditional breakpoints shine with event variables:
```javascript
onEvent onDamage {
    // Condition: $event_damage > 5
    // Catches only significant damage
}
```

---

## 🧪 Testing

Run the test script to validate conditional breakpoints:
```
test_conditional_breakpoints.kh
```

This script demonstrates:
- Health-based conditions
- Block-based conditions
- Attack-based conditions
- Hit conditions
- Logpoints

---

## ⚡ Performance

### Condition Evaluation
- Conditions are cached (max 100)
- Complexity limited to 10 operators
- Uses optimized ExpressionParser

### Hit Count Tracking
- Minimal overhead (single integer increment)
- Reset when breakpoint is removed

### Logpoints
- No pause overhead
- Message interpolation is fast
- Use for high-frequency events

---

## 🐛 Troubleshooting

### Condition Not Working
1. Check syntax: `$event_health < 10` (not `event_health < 10`)
2. Verify variable exists in scope
3. Check logs for evaluation errors

### Logpoint Not Logging
1. Verify message template: `{$var}` (not `$var`)
2. Check if variable exists
3. Ensure logpoint is enabled

### Hit Condition Not Triggering
1. Verify syntax: `>= 5` (not `>= 5.0`)
2. Check hit count: `getBreakpoint().getHitCount()`
3. Reset if needed: `breakpoint.resetHitCount()`

---

## 📊 Examples by Use Case

### Debugging Event Handlers
```javascript
onEvent onDamage {
    // Condition: $event_health < 5
    // Catches critical health situations
}
```

### Debugging Loops
```javascript
loop 1000 {
    // Hit condition: % 100 == 0
    // Pauses every 100 iterations
}
```

### Debugging Rare Events
```javascript
onEvent onBlockBreak {
    // Condition: $event_block == "ancient_debris"
    // Pauses only for rare blocks
}
```

### Performance Monitoring
```javascript
onEvent onTick {
    // Logpoint: "Tick {$event_tick} at {$event_time}"
    // Logs without impacting performance
}
```

---

## 🚀 Advanced Patterns

### Conditional Logpoint
Combine condition with logpoint for filtered logging:
```javascript
onEvent onDamage {
    // Condition: $event_damage > 5
    // Logpoint: "Significant damage: {$event_damage}"
    // Logs only when damage > 5
}
```

### Multi-Stage Debugging
```javascript
let stage = 0

onEvent onTick {
    stage++
    // Breakpoint 1: Condition: $stage == 10
    // Breakpoint 2: Condition: $stage == 50
    // Breakpoint 3: Condition: $stage == 100
}
```

### Event Correlation
```javascript
let lastDamage = 0

onEvent onDamage {
    lastDamage = $event_damage
}

onEvent onHeal {
    // Condition: $lastDamage > 5
    // Pauses if healing after significant damage
}
```

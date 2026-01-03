# Event System Guide

Kashub's event system allows scripts to react to game events in real-time.

## 📋 Available Events

### ✅ Implemented Events

#### onTick
Fires every 20 ticks (1 second).

**Variables:**
- `$event_tick` - Current tick counter
- `$event_time` - Timestamp in milliseconds

**Example:**
```javascript
onEvent onTick {
    print Tick: $event_tick
}
```

---

#### onDamage
Fires when player takes damage.

**Variables:**
- `$event_damage` - Amount of damage taken
- `$event_health` - Current health after damage
- `$event_maxHealth` - Maximum health

**Example:**
```javascript
onEvent onDamage {
    print Took $event_damage damage! Health: $event_health
    if ($event_health < 10) {
        eat
    }
}
```

---

#### onHeal
Fires when player heals.

**Variables:**
- `$event_healed` - Amount healed
- `$event_health` - Current health after healing
- `$event_maxHealth` - Maximum health

**Example:**
```javascript
onEvent onHeal {
    print Healed $event_healed! Health: $event_health
}
```

---

#### onHunger
Fires when hunger level changes.

**Variables:**
- `$event_food` - Current food level
- `$event_previousFood` - Previous food level
- `$event_saturation` - Current saturation level

**Example:**
```javascript
onEvent onHunger {
    if ($event_food < 6) {
        print Low hunger! Eating...
        eat
    }
}
```

---

#### onDeath
Fires when player dies.

**Variables:**
- `$event_position_x` - X coordinate of death
- `$event_position_y` - Y coordinate of death
- `$event_position_z` - Z coordinate of death

**Example:**
```javascript
onEvent onDeath {
    print Died at X:$event_position_x Y:$event_position_y Z:$event_position_z
    chat /home
}
```

---

#### onChat
Fires when chat message is received.

**Variables:**
- `$event_message` - Message content (without sender)
- `$event_sender` - Message sender (or "System")
- `$event_full_message` - Full message with formatting

**Example:**
```javascript
onEvent onChat {
    if ($event_sender == "Admin") {
        print Admin said: $event_message
    }
}
```

---

#### onBlockBreak
Fires when player breaks a block.

**Variables:**
- `$event_x` - Block X coordinate
- `$event_y` - Block Y coordinate
- `$event_z` - Block Z coordinate
- `$event_block` - Block name

**Example:**
```javascript
onEvent onBlockBreak {
    print Broke $event_block at X:$event_x Y:$event_y Z:$event_z
}
```

---

#### onBlockPlace
Fires when player places a block.

**Variables:**
- `$event_x` - Block X coordinate
- `$event_y` - Block Y coordinate
- `$event_z` - Block Z coordinate
- `$event_block` - Block name
- `$event_side` - Side placed on (up, down, north, south, east, west)

**Example:**
```javascript
onEvent onBlockPlace {
    print Placed $event_block at X:$event_x Y:$event_y Z:$event_z
}
```

---

#### onAttack
Fires when player attacks an entity.

**Variables:**
- `$event_target_name` - Entity name
- `$event_target_id` - Entity ID
- `$event_target_type` - Entity type
- `$event_target_x` - Entity X coordinate
- `$event_target_y` - Entity Y coordinate
- `$event_target_z` - Entity Z coordinate

**Example:**
```javascript
onEvent onAttack {
    print Attacked $event_target_name (Type: $event_target_type)
}
```

---

### ⚠️ Work In Progress

These events are declared but not yet implemented:
- `onRespawn` - Player respawns
- `onJump` - Player jumps
- `onSneak` - Sneak toggled
- `onSprint` - Sprint toggled
- `onItemUse` - Item used
- `onInventoryChange` - Inventory changed

Registering handlers for WIP events will show a warning.

---

## 🔧 Usage

### Basic Syntax
```javascript
onEvent <eventName> {
    <script>
}
```

### Multiple Handlers
You can register multiple handlers for the same event:
```javascript
onEvent onDamage {
    print Took damage!
}

onEvent onDamage {
    sound play alert
}
```

### Clearing Events
Events persist until the script stops. Use `stop events` to clear all handlers:
```javascript
stop events
```

---

## 💡 Best Practices

1. **Use sparingly** - Events fire frequently and can cause lag
2. **Filter conditions** - Use if statements to filter unwanted events
3. **Avoid heavy operations** - Keep event handlers lightweight
4. **Test thoroughly** - Use `test_events.kh` to validate behavior

---

## 🧪 Testing

Run the included test script to validate all events:
```
test_events.kh
```

This script registers handlers for all implemented events and provides instructions for testing.

---

## 📝 Implementation Details

Events are implemented using Fabric Mixins that inject into Minecraft's code:
- `ChatMessageMixin` - Captures chat messages
- `BlockBreakMixin` - Captures block breaking
- `BlockPlaceMixin` - Captures block placement
- `AttackMixin` - Captures entity attacks

The `EventManager` processes events and executes registered scripts with event variables injected into the scope.

# Export/Import System Guide

**Share variables between scripts - the first step towards a full package system!**

---

## Overview

The export/import system allows scripts to share variables with each other. This is the foundation for the future package system described in `PACKAGE_SYSTEM_CONCEPT.md`.

## Commands

### `export` - Share a variable

Export a variable so other scripts can import it.

**Syntax:**
```javascript
export <variableName> <value>
```

**Examples:**
```javascript
// Export simple values
export myNumber 42
export myString "Hello World"
export coords "100,64,200"

// Export calculated values
let result = 10 + 20
export calculatedValue $result

// Export environment variables
export currentHealth $PLAYER_HEALTH
export playerPos "$PLAYER_X,$PLAYER_Y,$PLAYER_Z"
```

### `import` - Use exported variables

Import variables that were exported by other scripts.

**Syntax:**
```javascript
import <variableName> from <scriptName>
import <var1>, <var2>, <var3> from <scriptName>
```

**Examples:**
```javascript
// Import single variable
import myNumber from script1
print $myNumber  // Outputs: 42

// Import multiple variables
import myString, coords from script1
print $myString
print $coords

// Use imported values in calculations
import calculatedValue from script1
let doubled = $calculatedValue * 2
print $doubled
```

---

## How It Works

1. **Script A exports variables** using the `export` command
2. **Script B imports variables** using the `import` command
3. **Variables are shared** between running scripts
4. **Exports are cleared** when the exporting script stops

---

## Example: Shared Configuration

### config.kh (Configuration Script)
```javascript
// Shared configuration values
export farmRadius 10
export farmCrop "wheat"
export farmSpeed 500

print "Configuration loaded:"
print "  Radius: $farmRadius"
print "  Crop: $farmCrop"
print "  Speed: $farmSpeed"

// Keep running to maintain exports
loop {
    wait 1000
}
```

### farmer.kh (Farming Script)
```javascript
// Import configuration
import farmRadius, farmCrop, farmSpeed from config

print "Starting farm with config:"
print "  Radius: $farmRadius"
print "  Crop: $farmCrop"
print "  Speed: $farmSpeed"

// Use imported values
for (x = 0; x < $farmRadius; x++) {
    for (z = 0; z < $farmRadius; z++) {
        placeBlock $x 64 $z $farmCrop
        wait $farmSpeed
    }
}
```

---

## Example: Shared State

### monitor.kh (Monitoring Script)
```javascript
// Monitor and export player state
loop {
    export playerHealth $PLAYER_HEALTH
    export playerFood $PLAYER_FOOD
    export playerPos "$PLAYER_X,$PLAYER_Y,$PLAYER_Z"
    
    wait 100
}
```

### healer.kh (Auto-Heal Script)
```javascript
// Auto-heal based on monitored health
loop {
    import playerHealth from monitor
    
    if ($playerHealth < 10) {
        print "Low health detected: $playerHealth"
        eat golden_apple
        wait 1000
    }
    
    wait 500
}
```

---

## Important Notes

### Script Names
- Use the script name without the `.kh` extension
- Example: `import myVar from script1` (not `script1.kh`)

### Script Lifecycle
- Exports are **cleared when the script stops**
- The exporting script must be **running** for imports to work
- If you stop and restart a script, it will re-export its variables

### Variable Scope
- Imported variables become **local variables** in the importing script
- Changes to imported variables **don't affect** the original export
- Each script has its own copy of imported values

### Error Handling
```javascript
// If the variable doesn't exist, you'll get an error:
import nonExistent from script1
// Error: Variable 'nonExistent' not exported by script 'script1'

// If the script isn't running, you'll get an error:
import myVar from stoppedScript
// Error: Variable 'myVar' not exported by script 'stoppedScript'
```

---

## Testing

Two test scripts are provided:

### test_export.kh
Demonstrates exporting various types of values:
- Simple numbers and strings
- Calculated values
- Environment variables

### test_import.kh
Demonstrates importing and using exported values:
- Single variable import
- Multiple variable import
- Using imported values in calculations

**To test:**
1. Run `test_export.kh` first (it will keep running)
2. Run `test_import.kh` to see the imports work
3. Stop `test_export.kh` and try running `test_import.kh` again (it will fail)

---

## Future: Full Package System

This export/import system is the **first step** towards a full package system. Future features will include:

- **Namespaces:** `kasper.math.distance()`
- **Package manager:** `kashub install kasper.math`
- **Function exports:** `export fn myFunction() { ... }`
- **Module system:** `use "kasper.math" { distance, round }`
- **Dependencies:** Automatic dependency resolution
- **Marketplace:** Share and discover packages

See `PACKAGE_SYSTEM_CONCEPT.md` for the full vision!

---

## Tips

1. **Keep config scripts running** - Use `loop { wait 1000 }` to keep them alive
2. **Export early** - Export variables at the start of your script
3. **Import once** - Import at the start, not in loops
4. **Use descriptive names** - `farmRadius` is better than `r`
5. **Document exports** - Add comments explaining what each export is for

---

**Happy scripting! 🚀**

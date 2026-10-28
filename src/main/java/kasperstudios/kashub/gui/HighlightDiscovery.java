package kasperstudios.kashub.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.objects.GameObject;
import kasperstudios.kashub.core.objects.InventoryObject;
import kasperstudios.kashub.core.objects.PlayerObject;
import kasperstudios.kashub.core.objects.ScannerObject;
import kasperstudios.kashub.core.objects.SystemObject;
import kasperstudios.kashub.core.objects.TerminalObject;
import kasperstudios.kashub.core.objects.VfsObject;
import kasperstudios.kashub.core.objects.VisionObject;
import kasperstudios.kashub.core.objects.WorldObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to discover all highlighting tokens from the current OS registry and environment.
 */
public class HighlightDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighlightDiscovery.class);

    public static Map<String, Integer> getDynamicKeywords() {
        Map<String, Integer> keywords = new HashMap<>();

        // 1. Collect from Command Registry
        Registry.getHighlightingKeywords().forEach((k, v) -> {
            keywords.put(k, parseColor(v, 0x56B6C2)); // Default Cyan
        });

        // 2. Collect from Global Objects
        addFromValue(keywords, new SystemObject());
        addFromValue(keywords, new PlayerObject());
        addFromValue(keywords, new VfsObject());
        addFromValue(keywords, new TerminalObject());
        addFromValue(keywords, new GameObject());
        addFromValue(keywords, new WorldObject());
        addFromValue(keywords, new ScannerObject());
        addFromValue(keywords, new VisionObject());
        addFromValue(keywords, new InventoryObject());

        // 3. Add global object names themselves
        keywords.put("System", 0xC678DD); // Purple
        keywords.put("player", 0xC678DD);
        keywords.put("Terminal", 0xC678DD);
        keywords.put("VFS", 0xC678DD);
        keywords.put("Math", 0xC678DD);
        keywords.put("scanner", 0xC678DD);
        keywords.put("vision", 0xC678DD);
        keywords.put("inventory", 0xC678DD);
        keywords.put("world", 0xC678DD);
        keywords.put("game", 0xC678DD);
        
        // Built-in functions
        keywords.put("print", 0x61AFEF); // Blue
        keywords.put("wait", 0x61AFEF);
        keywords.put("chat", 0x61AFEF);

        return keywords;
    }

    private static void addFromValue(Map<String, Integer> keywords, Value v) {
        v.getHighlighting().forEach((k, val) -> {
            keywords.put(k, parseColor(val, 0x98C379)); // Default Green (Functions)
        });
    }

    private static int parseColor(Object o, int defaultColor) {
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof String) {
            String s = (String) o;
            if (s.startsWith("#")) {
                try {
                    return Integer.parseInt(s.substring(1), 16) | 0xFF000000;
                } catch (Exception ignored) {
                    LOGGER.debug("Failed to parse color string '{}': {}", s, ignored.getMessage());
                }
            }
            // Add named color mapping here if needed
        }
        return defaultColor;
    }
}

package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.Command;
import kasperstudios.kashub.core.Registry;
import kasperstudios.kashub.core.types.KHType;
import kasperstudios.kashub.core.objects.Documentable;
import com.kasperstudios.khwrapper.KHWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DocumentationManager - Manages rich dynamic documentation sections,
 * loading from files and generating directly from code and live registries.
 */
public class DocumentationManager {
    private static final Map<String, DocSection> staticSections = new LinkedHashMap<>();
    private static final Map<String, ObjectDoc> objectDocs = new LinkedHashMap<>();
    private static final Path DOCS_DIR = KHWrapper.get().getConfigDir().resolve("kashub/docs");

    public static void initialize() {
        staticSections.clear();
        objectDocs.clear();

        // 1. Built-in Core Guides (First-class citizens in sidebar)
        registerCoreGuides();

        // 2. Register all Documentable objects with their live methods
        registerBuiltInObjects();

        // 3. Load user / bundled Markdown files from config/kashub/docs/
        loadFromDisk();
    }

    private static void registerCoreGuides() {
        // Quick Start & Syntax Guide
        registerSection("syntax", "Language Syntax", "⚡", List.of(
                "# ⚡ KHScript Syntax Guide",
                "",
                "KHScript is a modern, lightweight automation language for Minecraft.",
                "",
                "## Variable Declarations",
                "• `let x = 10` - Declare a local mutable variable",
                "• `let name = \"Player\"` - Declare a string variable",
                "• `let flag = true` - Declare a boolean variable",
                "",
                "## Control Flow",
                "• `if (condition) { ... } else { ... }` - Conditional branching",
                "• `while (condition) { ... }` - While loop",
                "• `for (let i = 0; i < 10; i = i + 1) { ... }` - For loop",
                "• `break` - Exit loop immediately",
                "• `continue` - Skip to next iteration",
                "",
                "## Functions",
                "• `fn myFunction(arg1, arg2) { return arg1 + arg2 }` - Declare function",
                "• `return value` - Return value from function",
                "",
                "## Execution Control",
                "• `wait 1000` - Pause script execution for milliseconds (non-blocking)",
                "• `print \"Hello World\"` - Output text to chat and console",
                "",
                "## Safety & Limits",
                "• `crashguard { ... }` - Sandboxed execution with tick/instruction budget"
        ));

        // Editor Shortcuts Guide
        registerSection("shortcuts", "Editor Shortcuts", "⌨", List.of(
                "# ⌨ Editor Shortcuts",
                "",
                "## File Operations",
                "• `Ctrl + S` - Save current script to disk",
                "• `Ctrl + N` - Create a new untitled script",
                "• `F5` - Run active script",
                "• `Ctrl + ~` or `Ctrl + ` ` - Toggle in-game terminal console",
                "",
                "## Editing",
                "• `Ctrl + Space` - Open Autocompletion Popup",
                "• `Ctrl + A` - Select entire document",
                "• `Ctrl + C` / `Ctrl + X` / `Ctrl + V` - Copy, Cut, Paste",
                "• `Ctrl + Z` - Undo change",
                "• `Ctrl + Y` - Redo change",
                "• `Ctrl + D` - Duplicate current line",
                "",
                "## Navigation",
                "• `Ctrl + Home` / `Ctrl + End` - Jump to beginning / end of file",
                "• `Double Click` - Select word under cursor",
                "• `Click + Drag` - Highlight text selection"
        ));
    }

    private static void loadFromDisk() {
        if (!Files.exists(DOCS_DIR)) return;
        try (var stream = Files.list(DOCS_DIR)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".md") && !name.equalsIgnoreCase("README.md") && !name.equalsIgnoreCase("DOCUMENTATION_SUMMARY.md");
            })
            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
            .forEach(DocumentationManager::loadDocFile);
        } catch (IOException e) {
            Kashub.LOGGER.error("[Docs] Failed to list docs directory: {}", e.getMessage());
        }
    }

    private static void loadDocFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            String fileName = path.getFileName().toString();
            String id = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // Clean title: convert "api-overview" -> "Api Overview"
            String title = Arrays.stream(id.split("[-_]"))
                    .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
            String icon = "📋";

            // Extract real title and icon from first markdown header if present
            if (!lines.isEmpty() && lines.get(0).startsWith("# ")) {
                String header = lines.get(0).substring(2).trim();
                if (header.length() > 2 && isEmoji(header.substring(0, 2))) {
                    icon = header.substring(0, 2).trim();
                    title = header.substring(2).trim();
                } else if (!header.isEmpty()) {
                    title = header;
                }
            }

            // Custom friendly icons
            if (id.contains("pathfind")) icon = "🧭";
            else if (id.contains("cct") || id.contains("computercraft")) icon = "💻";
            else if (id.contains("implant") || id.contains("neural")) icon = "🧠";
            else if (id.contains("terminal")) icon = "📟";
            else if (id.contains("editor")) icon = "📝";
            else if (id.contains("install")) icon = "📦";

            staticSections.put(id, new DocSection(title, id, icon, lines));
            Kashub.LOGGER.info("[Docs] Loaded section: {} ({})", id, title);
        } catch (IOException e) {
            Kashub.LOGGER.error("[Docs] Failed to read doc file {}: {}", path, e.getMessage());
        }
    }

    private static boolean isEmoji(String s) {
        if (s == null || s.isEmpty()) return false;
        int codePoint = s.codePointAt(0);
        return Character.getType(codePoint) == Character.OTHER_SYMBOL || codePoint > 0x2000;
    }

    private static void registerBuiltInObjects() {
        Documentable[] objs = new Documentable[] {
            new kasperstudios.kashub.core.objects.PlayerObject(),
            new kasperstudios.kashub.core.objects.GameObject(),
            new kasperstudios.kashub.core.objects.WorldObject(),
            new kasperstudios.kashub.core.objects.VisionObject(),
            new kasperstudios.kashub.core.objects.ScannerObject(),
            new kasperstudios.kashub.core.objects.InventoryObject(),
            new kasperstudios.kashub.core.objects.SystemObject(),
            new kasperstudios.kashub.core.objects.TerminalObject(),
            new kasperstudios.kashub.core.objects.VfsObject()
        };

        for (Documentable doc : objs) {
            registerObject(doc.getObjectName(), doc.getObjectDescription(), doc.getMethodDescriptions());
        }

        // Global helpers defined inline in Interpreter
        registerObject("math", "Standard mathematical functions and utilities.", Map.of(
                "sqrt(value)", "Returns the square root of a number.",
                "abs(value)", "Returns the absolute value of a number.",
                "min(a, b)", "Returns the smaller of two numbers.",
                "max(a, b)", "Returns the larger of two numbers.",
                "floor(value)", "Rounds a decimal value down to the nearest integer.",
                "ceil(value)", "Rounds a decimal value up to the nearest integer.",
                "round(value)", "Rounds a value to the nearest integer.",
                "random([max])", "Returns a random integer between 0 and max-1, or a 0.0-1.0 float.",
                "pow(base, exp)", "Calculates base raised to the power of exp."));
                
        registerObject("tag", "Item and block Minecraft tag queries.", Map.of(
                "itemHas(item, tag)", "Checks whether an item stack possesses the specified tag.",
                "itemsWith(tag)", "Lists all registered items matching the given tag.",
                "blockHas(block, tag)", "Checks whether a block in the world possesses the specified tag."));
    }

    public static void registerSection(String id, String title, String icon, List<String> lines) {
        staticSections.put(id, new DocSection(title, id, icon, lines));
    }

    public static void registerObject(String name, String description, Map<String, String> methods) {
        objectDocs.put(name, new ObjectDoc(name, description, methods));
    }

    public static String getMemberDoc(String objectName, String methodName) {
        ObjectDoc doc = objectDocs.get(objectName);
        if (doc != null) {
            if (doc.methods.containsKey(methodName)) return doc.methods.get(methodName);
            for (Map.Entry<String, String> entry : doc.methods.entrySet()) {
                if (entry.getKey().startsWith(methodName + "(")) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public static kasperstudios.khgui.api.IHoverProvider.HoverInfo getHoverDoc(String word) {
        if (objectDocs.containsKey(word)) {
            ObjectDoc doc = objectDocs.get(word);
            return new kasperstudios.khgui.api.IHoverProvider.HoverInfo(doc.name, doc.description);
        }

        for (ObjectDoc doc : objectDocs.values()) {
            if (doc.methods.containsKey(word)) {
                return new kasperstudios.khgui.api.IHoverProvider.HoverInfo(doc.name + "." + word, doc.methods.get(word));
            }
            for (String key : doc.methods.keySet()) {
                if (key.startsWith(word + "(")) {
                    return new kasperstudios.khgui.api.IHoverProvider.HoverInfo(doc.name + "." + word, doc.methods.get(key));
                }
            }
        }
        return null;
    }

    public static List<DocSection> getAllSections() {
        List<DocSection> all = new ArrayList<>();

        // 1. Language basics
        if (staticSections.containsKey("syntax")) all.add(staticSections.get("syntax"));
        if (staticSections.containsKey("shortcuts")) all.add(staticSections.get("shortcuts"));

        // 2. Core Objects (First-class citizens right in sidebar)
        for (ObjectDoc obj : objectDocs.values()) {
            all.add(generateSingleObjectSection(obj));
        }

        // 3. Live Commands Registry (Grouped by Category)
        all.add(generateCommandsSection());

        // 4. Types section
        all.add(generateTypesSection());

        // 5. In-depth Markdown Guides
        for (Map.Entry<String, DocSection> entry : staticSections.entrySet()) {
            if (!entry.getKey().equals("syntax") && !entry.getKey().equals("shortcuts")) {
                all.add(entry.getValue());
            }
        }

        // 6. ComputerCraft Integration & Scanned Peripherals — strictly when CC:T is available
        if (kasperstudios.kashub.services.integration.CCTBridge.getInstance().isAvailable()) {
            all.add(generateCCSection());
            all.add(generateScannedPeripheralsSection());
        }

        return all;
    }

    private static DocSection generateSingleObjectSection(ObjectDoc obj) {
        List<String> lines = new ArrayList<>();
        lines.add("# 📦 " + obj.name.toUpperCase() + " Object");
        lines.add("");
        lines.add(obj.description);
        lines.add("");
        lines.add("## Available Methods");
        lines.add("");

        for (Map.Entry<String, String> method : obj.methods.entrySet()) {
            lines.add("### " + obj.name + "." + method.getKey());
            lines.add(method.getValue());
            lines.add("Usage: " + obj.name + "." + method.getKey());
            lines.add("");
        }

        return new DocSection(obj.name, "obj_" + obj.name, "📦", lines);
    }

    private static DocSection generateCommandsSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 📚 All Registered Commands");
        lines.add("");
        lines.add("Full list of syntax commands registered in the live KasHub engine:");
        lines.add("");

        Map<String, Command> uniqueCommands = new LinkedHashMap<>();
        for (Command cmd : Registry.getAllCommands()) {
            uniqueCommands.put(cmd.getMetadata().syntax, cmd);
        }

        Map<String, List<Command>> byCategory = uniqueCommands.values().stream()
                .collect(Collectors.groupingBy(c -> c.getMetadata().category, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<Command>> entry : byCategory.entrySet()) {
            lines.add("## Category: " + entry.getKey());
            for (Command cmd : entry.getValue()) {
                lines.add("• `" + cmd.getMetadata().syntax + "` - " + cmd.getMetadata().description);
                if (cmd.getMetadata().syntax != null && !cmd.getMetadata().syntax.isEmpty()) {
                    lines.add("Usage: " + cmd.getMetadata().syntax);
                }
            }
            lines.add("");
        }

        return new DocSection("All Commands", "all_commands", "📚", lines);
    }

    private static DocSection generateCCSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 💻 ComputerCraft: Tweaked Bridge");
        lines.add("");
        lines.add("KasHub provides a bidirectional VFS and scripting bridge for CC:T computers.");
        lines.add("");
        lines.add("## Standard Peripheral Modules");
        lines.add("• `turtle` - Movement, digging, item placement, and crafting.");
        lines.add("• `fs` - In-world virtual computer filesystem operations.");
        lines.add("• `peripheral` - Direct invocation of wrapped peripheral blocks.");
        lines.add("• `term` / `window` - Screen rendering and color buffer control.");
        lines.add("• `redstone` - Bundled and analog redstone wire management.");
        lines.add("");
        lines.add("## Connecting in Editor");
        lines.add("1. Target a Computer or Turtle in-world.");
        lines.add("2. Hold `Left Alt` to scan and wrap the peripheral.");
        lines.add("3. Open the editor terminal to manipulate files via `/mnt/cc/...`.");

        return new DocSection("CC:T Bridge", "cc_api", "💻", lines);
    }

    private static DocSection generateTypesSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 📐 KHScript Type System");
        lines.add("");
        lines.add("Every value in KHScript has a concrete runtime type:");
        lines.add("");
        for (KHType type : KHType.values()) {
            lines.add("• **" + type.getName() + "** - " + type.getDescription());
        }
        return new DocSection("Type System", "types", "📐", lines);
    }

    private static DocSection generateScannedPeripheralsSection() {
        List<String> lines = new ArrayList<>();
        lines.add("# 🔍 Scanned Peripherals");
        lines.add("");
        lines.add("Deep-scanned peripherals in your current world:");
        lines.add("");

        Map<String, List<String>> scanned = kasperstudios.kashub.services.integration.ScannedPeripheralManager.getInstance().getAllScanned();
        
        if (scanned.isEmpty()) {
            lines.add("You haven't scanned any peripherals yet.");
            lines.add("Aim at an active CC peripheral and hold `Left Alt` to analyze its methods.");
            lines.add("");
            lines.add("§eNote: Deep peripheral analysis requires a Neuro-Implant Tier 2.");
        } else {
            for (String type : scanned.keySet()) {
                lines.add("[SCAN:" + type + "]");
            }
        }

        return new DocSection("Scanned Peripherals", "scanned_peripherals", "🔍", lines);
    }

    public static class DocSection {
        public final String title;
        public final String id;
        public final String icon;
        public final List<String> lines;

        public DocSection(String title, String id, String icon, List<String> lines) {
            this.title = title;
            this.id = id;
            this.icon = icon;
            this.lines = lines;
        }
    }

    public static class ObjectDoc {
        public final String name;
        public final String description;
        public final Map<String, String> methods;

        public ObjectDoc(String name, String description, Map<String, String> methods) {
            this.name = name;
            this.description = description;
            this.methods = methods;
        }
    }
}

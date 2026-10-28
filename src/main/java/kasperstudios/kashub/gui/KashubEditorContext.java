package kasperstudios.kashub.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kasperstudios.khgui.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Collections;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Type;
import kasperstudios.kashub.util.ScriptManager;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.khgui.api.dialogs.DialogBuilder;
import kasperstudios.khgui.api.dialogs.elements.DialogTextInputElement;
import kasperstudios.khgui.core.ThemeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.DocumentationManager;
import kasperstudios.kashub.util.VirtualFileSystem;
import kasperstudios.kashub.gui.dialogs.TaskManagerDialog;
import kasperstudios.kashub.gui.dialogs.DocsDialog;
import kasperstudios.kashub.gui.dialogs.SettingsDialog;

/**
 * Multi-tab editor context for KHGui integration.
 * Tracks open files, dirty state, and active tab index.
 */
public class KashubEditorContext implements IEditorContext, IConsoleProvider, IHoverProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KashubEditorContext.class);

    private final java.util.LinkedHashMap<String, String> openFiles = new java.util.LinkedHashMap<>();
    private final java.util.Set<String> dirtyFiles = new java.util.HashSet<>();
    private int activeTabIndex = 0;
    private int untitledCounter = 1;

    // ─── CC mode ──────────────────────────────────────────────────────────────
    /** -1 means not in CC mode. */
    private int ccComputerId = -1;
    /** Static instance reference so NetworkingManager can call back. */
    private static KashubEditorContext activeInstance;

    public boolean isInCCMode() { return ccComputerId >= 0; }
    public int getCCComputerId() { return ccComputerId; }

    private boolean needsRefresh = false;

    @Override
    public boolean shouldRefreshLayout() {
        if (needsRefresh) {
            needsRefresh = false;
            return true;
        }
        return false;
    }

    public void requestRefresh() {
        this.needsRefresh = true;
    }

    public static void onCCConnected(int computerId) {
        if (activeInstance != null) activeInstance.enterCCMode(computerId);
    }

    public static void onCCDisconnected() {
        if (activeInstance != null) activeInstance.exitCCMode();
    }

    public static void onCCFileListReceived(int computerId) {
        if (activeInstance != null && activeInstance.ccComputerId == computerId) {
            activeInstance.requestRefresh();
        }
    }

    public void enterCCMode(int computerId) {
        this.ccComputerId = computerId;
        ScriptLogger.getInstance().clear(); // Clear local terminal for CC context
        ScriptLogger.getInstance().info("§b[CC] Connected to Computer #" + computerId + ". Context switched.");
        
        // Force refresh tabs for remote context
        openFiles.clear();
        openFile("/mnt/cc/" + computerId + "/startup.lua");
        requestRefresh();
    }

    public void exitCCMode() {
        int prev = this.ccComputerId;
        this.ccComputerId = -1;
        if (prev >= 0) ScriptLogger.getInstance().info("§7[CC] Disconnected from Computer #" + prev + ".");
        requestRefresh();
    }

    public KashubEditorContext(String initialFile) {
        activeInstance = this;
        if (initialFile != null) {
            String loadedContent = ScriptManager.loadScript(initialFile);
            openFiles.put(initialFile, loadedContent != null ? loadedContent : "");
        }
        this.activeTabIndex = 0;

        // Register Terminal File Opener
        kasperstudios.kashub.core.TerminalManager.getInstance().setFileOpener(this::openFile);
        
        // Restore session
        restoreSession();

        // Restore CC Session if bridge is already connected
        int activeCC = kasperstudios.kashub.client.CCTClientBridge.getInstance().getActiveSessionId();
        if (activeCC >= 0) {
            this.ccComputerId = activeCC;
            // Note: we don't clear files or open startup.lua here because we want to preserve 
            // whatever files were open in the editor tabs from restoreSession()
        }

        // If still empty, open welcome.kh
        if (openFiles.isEmpty()) {
            String welcome = ScriptManager.loadScript("welcome.kh");
            if (welcome != null && !welcome.isBlank()) {
                openFiles.put("welcome.kh", welcome);
            } else {
                openFiles.put("welcome.kh", "// ==========================================\n" +
                        "// Welcome to KasHub (v1.0.0-FSR)\n" +
                        "// ==========================================\n" +
                        "System.print(\"§a[KasHub] Welcome to KasHub Scripting Engine!\")\n\n" +
                        "let name = player.getName()\n" +
                        "let health = player.getHealth()\n" +
                        "let pos = player.getPos()\n\n" +
                        "System.print(\"Hello, §e\" + name + \"§f!\")\n" +
                        "System.print(\"Your Health: §c\" + health + \" HP\")\n" +
                        "System.print(\"Coordinates: §b\" + pos.x + \", \" + pos.y + \", \" + pos.z)\n");
            }
        }

        // Push dynamic keywords to syntax highlighter
        try {
            kasperstudios.khgui.editor.SyntaxHighlighter.setDynamicKeywords(HighlightDiscovery.getDynamicKeywords());
        } catch (Exception e) {
            ScriptLogger.getInstance().error("Failed to initialize dynamic highlighting: " + e.getMessage());
        }
    }

    private void restoreSession() {
        var config = KashubConfig.getInstance();
        if (config.openTabs != null && !config.openTabs.isEmpty()) {
            for (String script : config.openTabs) {
                if (script.equals("Untitled")) continue; // Don't restore ephemeral Untitled
                
                // If it's a CC file, we need to load it differently or skip it
                if (script.startsWith("/mnt/cc/")) continue;

                String content = ScriptManager.loadScript(script);
                if (content == null) {
                    content = ScriptManager.loadServerScript(script);
                }
                if (content != null) {
                    openFiles.put(script, content);
                }
            }
            activeTabIndex = Math.min(config.activeTabIndex, openFiles.size() - 1);
            if (activeTabIndex < 0 && !openFiles.isEmpty()) activeTabIndex = 0;
        }
    }

    private void saveSession() {
        var config = KashubConfig.getInstance();
        config.openTabs = new ArrayList<>(openFiles.keySet());
        config.activeTabIndex = activeTabIndex;
        config.save();
    }

    // --- IEditorContext ---

    @Override
    public IHoverProvider getHoverProvider() {
        return this;
    }

    @Override
    public IHoverProvider.HoverInfo getHoverInfo(String line, int column, String word) {
        // 1. Try global objects (System, player, etc.)
        String fullWord = word;
        
        // Check if there is a prefix (e.g., System.print -> word is print, prefix is System)
        String prefix = null;
        int dotIdx = line.lastIndexOf('.', column);
        if (dotIdx != -1) {
            int start = dotIdx;
            while (start > 0 && (Character.isLetterOrDigit(line.charAt(start - 1)) || line.charAt(start - 1) == '_')) {
                start--;
            }
            if (start < dotIdx) {
                prefix = line.substring(start, dotIdx);
            }
        }

        if (prefix != null) {
            String doc = DocumentationManager.getMemberDoc(prefix, word);
            if (doc != null) {
                return new IHoverProvider.HoverInfo(prefix + "." + word, doc);
            }
        }

        // Try direct object lookup
        var info = DocumentationManager.getHoverDoc(word);
        if (info != null) return info;

        return null;
    }

    @Override
    public String getTitleText() {
        return "v1.0.0-FSR";
    }

    @Override
    public String getTitleIcon() {
        return "";
    }

    @Override
    public String getTitleLogoTexture() {
        return "kashub:textures/gui/logo.png";
    }

    @Override
    public int getTitleLogoWidth() {
        return 981;
    }

    @Override
    public int getTitleLogoHeight() {
        return 265;
    }

    @Override
    public List<IEditorMenu> getTitleMenus() {
        List<IEditorMenu> menus = new ArrayList<>();

        // File Menu
        List<IMenuItem> fileItems = new ArrayList<>();

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "New File"; }
            @Override public void onClick() {
                createNewUntitled();
            }
        });

        if (isInCCMode()) {
            fileItems.add(new IMenuItem() {
                @Override public String getLabel() { return "§cDisconnect CC#" + ccComputerId; }
                @Override public void onClick() {
                    kasperstudios.kashub.client.CCTClientBridge.getInstance().disconnect(ccComputerId);
                    exitCCMode();
                }
            });
        }

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "Open..."; }
            @Override public void onClick() {
                Screen parent = Minecraft.getInstance().screen;
                var theme = ThemeManager.getCurrentTheme();
                DialogBuilder builder = new DialogBuilder(parent, theme);
                builder.setTitle("Open File");
                
                List<String> files = ScriptManager.getUserScripts();
                if (files.isEmpty()) {
                    builder.addElement(new kasperstudios.khgui.api.dialogs.elements.DialogTextElement("No user scripts found.", theme.getPrimaryText(), false));
                } else {
                    builder.addElement(new kasperstudios.khgui.api.dialogs.elements.DialogListElement(files, selectedFile -> {
                        openFile(selectedFile);
                        Minecraft.getInstance().setScreen(parent);
                    }));
                }
                
                builder.addButton("Cancel", theme.getBorder(), () -> Minecraft.getInstance().setScreen(parent));
                Minecraft.getInstance().setScreen(builder.build());
            }
        });

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "Save"; }
            @Override public void onClick() { save(getContent()); }
        });

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "Save As..."; }
            @Override public void onClick() {
                Screen parent = Minecraft.getInstance().screen;
                var theme = ThemeManager.getCurrentTheme();
                DialogBuilder builder = new DialogBuilder(parent, theme);
                String defaultName = getCurrentFile() != null ? getCurrentFile() : "untitled";
                DialogTextInputElement fileNameField = new DialogTextInputElement("File name", defaultName, theme);
                builder.addElement(fileNameField);
                builder.addButton("Save", theme.getPrimaryAccent(), () -> {
                    String rawName = fileNameField.getValue();
                    String trimmedName = rawName != null ? rawName.trim() : "";
                    if (trimmedName.isEmpty()) {
                        ScriptLogger.getInstance().error("File name cannot be empty.");
                        return;
                    }
                    String fullName = trimmedName.endsWith(".kh") ? trimmedName : trimmedName + ".kh";
                    boolean success = ScriptManager.saveScript(fullName, getContent());
                    if (success) {
                        String baseName = fullName.endsWith(".kh") ? fullName.substring(0, fullName.length() - 3) : fullName;
                        String reloaded = ScriptManager.loadScript(baseName);
                        openFiles.put(baseName, reloaded != null ? reloaded : getContent());
                        activeTabIndex = openFiles.size() - 1;
                        dirtyFiles.remove(baseName);
                        saveSession();
                        ScriptLogger.getInstance().success("Saved " + fullName);
                    } else {
                        ScriptLogger.getInstance().error("Failed to save " + fullName);
                    }
                    Minecraft.getInstance().setScreen(parent);
                });
                builder.addButton("Cancel", theme.getBorder(), () -> Minecraft.getInstance().setScreen(parent));
                Minecraft.getInstance().setScreen(builder.build());
            }
        });

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "Close Editor"; }
            @Override public void onClick() {
                var mc = Minecraft.getInstance();
                if (mc.screen != null) mc.screen.onClose();
            }
        });

        fileItems.add(new IMenuItem() {
            @Override public String getLabel() { return "Recent Files"; }
            @Override public void onClick() { /* TODO */ }
        });

        // ComputerCraft Menu
        List<IMenuItem> ccItems = new ArrayList<>();
        var bridge = kasperstudios.kashub.client.CCTClientBridge.getInstance();
        var computers = bridge.scan();

        if (isInCCMode()) {
            ccItems.add(new IMenuItem() {
                @Override public String getLabel() { return "§cDisconnect (CC#" + ccComputerId + ")"; }
                @Override public void onClick() {
                    bridge.disconnect(ccComputerId);
                    exitCCMode();
                }
            });
            ccItems.add(new IMenuItem() { @Override public String getLabel() { return "---"; } @Override public void onClick() {} });
        }

        if (computers.isEmpty()) {
            ccItems.add(new IMenuItem() {
                @Override public String getLabel() { return "No computers found"; }
                @Override public void onClick() {}
            });
        } else {
            for (var comp : computers) {
                ccItems.add(new IMenuItem() {
                    @Override public String getLabel() { 
                        String label = comp.label != null ? comp.label : "Computer #" + comp.id;
                        return (ccComputerId == comp.id ? "§b● " : "  ") + label; 
                    }
                    @Override public void onClick() {
                        if (ccComputerId == comp.id) return;
                        bridge.connect(comp.id, comp.pos);
                    }
                });
            }
        }

        menus.add(new IEditorMenu() {
            @Override public String getLabel() { return "File"; }
            @Override public List<IMenuItem> getItems() { return fileItems; }
        });

        if (!isInCCMode()) {
            menus.add(new IEditorMenu() {
                @Override public String getLabel() { return "Computer"; }
                @Override public List<IMenuItem> getItems() { return ccItems; }
            });
        }

        return menus;
    }

    @Override
    public List<ITitleAction> getTitleActions() {
        List<ITitleAction> actions = new ArrayList<>();

        if (isInCCMode()) {
            actions.add(new ITitleAction() {
                @Override public String getId() { return "cc_disconnect"; }
                @Override public String getLabel() { return "§cDisconnect CC#" + ccComputerId; }
                @Override public void onClick() {
                    kasperstudios.kashub.client.CCTClientBridge.getInstance().disconnect(ccComputerId);
                    exitCCMode();
                }
            });
        }

        actions.add(new ITitleAction() {
            @Override public String getId() { return "tasks"; }
            @Override public String getLabel() { return "Tasks"; }
            @Override public String getIcon() { return ""; }
            @Override public void onClick() {
                Minecraft.getInstance().setScreen(new TaskManagerDialog(Minecraft.getInstance().screen));
            }
        });

        actions.add(new ITitleAction() {
            @Override public String getId() { return "docs"; }
            @Override public String getLabel() { return "Docs"; }
            @Override public String getIcon() { return ""; }
            @Override public void onClick() {
                Minecraft.getInstance().setScreen(new DocsDialog(Minecraft.getInstance().screen));
            }
        });

        actions.add(new ITitleAction() {
            @Override public String getId() { return "settings"; }
            @Override public String getLabel() { return "Settings"; }
            @Override public String getIcon() { return ""; }
            @Override public void onClick() {
                Minecraft.getInstance().setScreen(new SettingsDialog(Minecraft.getInstance().screen));
            }
        });

        actions.add(new ITitleAction() {
            @Override public String getId() { return "stop_all"; }
            @Override public String getLabel() { return "Stop All"; }
            @Override public String getIcon() { return ""; } // TODO: Use stop icon
            @Override public void onClick() {
                TaskManager.getInstance().stopAll();
                kasperstudios.kashub.services.PathfindingService.getInstance().stop();
                ScriptLogger.getInstance().info("Terminated all processes and navigation.");
            }
        });

        return actions;
    }

    @Override
    public Map<String, String> getCustomPanelIconTextures() {
        return Map.of(
            "explorer", "kashub:textures/icons/explorer.png",
            "search", "kashub:textures/icons/search.png",
            "market", "kashub:textures/icons/market.png",
            "run", "kashub:textures/icons/run.png"
        );
    }

    @Override
    public int getExecutionLine() {
        String currentFile = getCurrentFile();
        if (currentFile == null) return -1;
        
        for (kasperstudios.kashub.core.Task task : TaskManager.getInstance().getRunningTasks()) {
            if (task.getName().equals(currentFile)) {
                return task.getCurrentLine();
            }
        }
        return -1;
    }

    @Override
    public java.util.Set<Integer> getErrorLines() {
        java.util.Set<Integer> errors = new java.util.HashSet<>();
        String currentFile = getCurrentFile();
        if (currentFile == null) return errors;

        for (kasperstudios.kashub.core.Task task : TaskManager.getInstance().getTasksByState(kasperstudios.kashub.core.State.ERROR)) {
            if (task.getName().equals(currentFile)) {
                errors.add(task.getCurrentLine());
            }
        }
        return errors;
    }

    @Override
    public java.util.Set<Integer> getBreakpoints() {
        // Breakpoints not yet persisted in Task
        return new java.util.HashSet<>();
    }

    @Override
    public List<String> getOpenTabs() {
        return new ArrayList<>(openFiles.keySet());
    }

    @Override
    public int getActiveTabIndex() {
        return Math.max(0, Math.min(activeTabIndex, openFiles.size() - 1));
    }

    @Override
    public String getSidebarTitle() {
        return "EXPLORER";
    }

    @Override
    public String getStatusBarText() {
        int taskCount = TaskManager.getInstance().getRunningTasks().size();
        boolean pathfinding = kasperstudios.kashub.services.PathfindingService.getInstance().isActive();
        
        StringBuilder sb = new StringBuilder();
        sb.append(taskCount).append(" Tasks Running");
        if (pathfinding) {
            sb.append(" | NAVIGATION ACTIVE");
        }
        return sb.toString();
    }

    @Override
    public String getLanguage() {
        return "khscript";
    }

    @Override
    public String getContent() {
        List<String> tabs = getOpenTabs();
        int idx = getActiveTabIndex();
        if (idx < 0 || idx >= tabs.size()) return "\n";
        String content = openFiles.getOrDefault(tabs.get(idx), "");
        return (content == null || content.isEmpty()) ? "\n" : content;
    }

    @Override
    public void updateContent(String content) {
        updateContent(getActiveTabIndex(), content);
    }

    @Override
    public void updateContent(int idx, String content) {
        List<String> tabs = getOpenTabs();
        if (tabs.isEmpty() || idx < 0 || idx >= tabs.size()) return;
        String fileName = tabs.get(idx);
        String previous = openFiles.get(fileName);
        if (previous != null && !previous.equals(content)) {
            dirtyFiles.add(fileName);
        }
        openFiles.put(fileName, content != null ? content : "");
    }

    @Override
    public boolean isTabDirty(int index) {
        List<String> tabs = getOpenTabs();
        if (index < 0 || index >= tabs.size()) return false;
        return dirtyFiles.contains(tabs.get(index));
    }

    @Override
    public void save(String content) {
        List<String> tabs = getOpenTabs();
        int idx = getActiveTabIndex();
        if (tabs.isEmpty() || idx >= tabs.size()) return;
        String fileName = tabs.get(idx);
        openFiles.put(fileName, content);
        dirtyFiles.remove(fileName);
        
        if (fileName.startsWith("/mnt/cc/")) {
            // Save to remote VFS
            try {
                VirtualFileSystem.getInstance().write(fileName, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ScriptLogger.getInstance().success("Saved remote file: " + fileName);
            } catch (java.io.IOException e) {
                ScriptLogger.getInstance().error("Failed to save remote file: " + e.getMessage());
            }
            return;
        }

        String fullFileName = fileName.endsWith(".kh") ? fileName : fileName + ".kh";
        if (ScriptManager.saveScript(fullFileName, content)) {
            ScriptLogger.getInstance().success("Saved " + fullFileName);
        } else {
            ScriptLogger.getInstance().error("Failed to save " + fullFileName);
        }
    }

    @Override
    public void closeTab(int index) {
        closeFile(index);
    }
    
    public void closeFile(int index) {
        List<String> tabs = getOpenTabs();
        if (index >= 0 && index < tabs.size()) {
            String fileName = tabs.get(index);
            if (dirtyFiles.contains(fileName)) {
                ScriptLogger.getInstance().warn("Please save " + fileName + " before closing.");
                return;
            }
            openFiles.remove(fileName);
            if (activeTabIndex >= openFiles.size()) {
                activeTabIndex = Math.max(0, openFiles.size() - 1);
            }
            saveSession();
        }
        if (openFiles.isEmpty()) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    // --- Multi-tab helpers ---

    public void openFile(String fileName) {
        if (fileName.equals("Untitled")) {
             // Handle untitled separately or just open it
             if (!openFiles.containsKey("Untitled")) {
                 openFiles.put("Untitled", "// Untitled Script\n\n");
             }
             activeTabIndex = getTabIndex("Untitled");
             saveSession();
             return;
        }

        if (fileName.startsWith("/mnt/cc/")) {
            openRemoteFile(fileName);
            return;
        }

        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        String currentDir = kasperstudios.kashub.core.TerminalManager.getInstance().getCurrentDir();
        java.nio.file.Path physical = vfs.resolve(fileName, currentDir);
        
        // Final name for the tab
        String virtualName = (physical != null) ? vfs.toVirtual(physical) : fileName;
        
        if (openFiles.containsKey(virtualName)) {
            activeTabIndex = getTabIndex(virtualName);
            saveSession();
            return;
        }

        // Check aliases
        for (String open : openFiles.keySet()) {
             java.nio.file.Path openPhys = vfs.resolve(open, "/");
             if (openPhys != null && physical != null && openPhys.equals(physical)) {
                 activeTabIndex = getTabIndex(open);
                 saveSession();
                 return;
             }
        }

        String loadedContent = null;
        
        // 1. Try loading via Physical Path if it exists
        if (physical != null && java.nio.file.Files.exists(physical)) {
            try {
                loadedContent = java.nio.file.Files.readString(physical, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException ignored) {
                LOGGER.warn("Failed to read file from physical path: {}", physical, ignored);
            }
        }
        
        // 2. Fallback to ScriptManager (handles system/server scripts by logical name)
        if (loadedContent == null) {
            String logicalName = virtualName;
            if (logicalName.startsWith("~/")) logicalName = logicalName.substring(2);
            else if (logicalName.startsWith("/")) {
                String[] parts = logicalName.split("/");
                logicalName = parts[parts.length - 1];
            }
            
            loadedContent = ScriptManager.loadScript(logicalName);
            if (loadedContent == null) {
                loadedContent = ScriptManager.loadServerScript(logicalName);
            }
        }

        if (loadedContent != null) {
            if (loadedContent.isEmpty()) loadedContent = "\n";
            openFiles.put(virtualName, loadedContent);
            activeTabIndex = openFiles.size() - 1;
            saveSession();
            ScriptLogger.getInstance().success("Opened script: " + virtualName);
        } else {
            ScriptLogger.getInstance().error("Failed to load script: " + fileName);
        }
    }

    private void openRemoteFile(String fileName) {
        if (openFiles.containsKey(fileName)) {
            activeTabIndex = getTabIndex(fileName);
            return;
        }

        // Show loading state
        openFiles.put(fileName, "-- Loading remote file... --");
        activeTabIndex = openFiles.size() - 1;
        
        String mountPrefix = "/mnt/cc/" + ccComputerId + "/";
        String subPath = fileName.startsWith(mountPrefix) ? fileName.substring(mountPrefix.length()) : fileName;
        
        kasperstudios.kashub.client.CCTClientBridge.getInstance().readFileAsync(ccComputerId, subPath, (data) -> {
            Minecraft.getInstance().execute(() -> {
                if (data != null) {
                    String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    openFiles.put(fileName, content);
                    ScriptLogger.getInstance().success("Opened remote script: " + fileName);
                } else {
                    openFiles.put(fileName, "-- File Not Found or Empty --\n");
                    ScriptLogger.getInstance().error("Failed to load remote script: " + fileName);
                }
            });
        });
    }

    private int getTabIndex(String name) {
        int i = 0;
        for (String key : openFiles.keySet()) {
            if (key.equals(name)) return i;
            i++;
        }
        return 0;
    }

    public void switchTab(int index) {
        if (index >= 0 && index < openFiles.size()) {
            activeTabIndex = index;
        }
    }

    public String getCurrentFile() {
        List<String> tabs = getOpenTabs();
        int idx = getActiveTabIndex();
        if (idx < 0 || idx >= tabs.size()) return null;
        return tabs.get(idx);
    }

    // --- Custom panels ---

    private String searchQuery = "";
    private final List<SearchResult> searchResults = new ArrayList<>();

    private static class SearchResult {
        final String fileName;
        final int line;
        final String content;
        SearchResult(String f, int l, String c) { this.fileName = f; this.line = l; this.content = c; }
    }

    private void performSearch(String query) {
        searchQuery = query;
        searchResults.clear();
        if (query.isEmpty()) return;

        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        List<String> filesToSearch = new ArrayList<>();
        vfs.list("~", "/").forEach(node -> { if (!node.isDirectory) filesToSearch.add("~/" + node.name); });
        vfs.list("/system/bin", "/").forEach(node -> { if (!node.isDirectory) filesToSearch.add("/system/bin/" + node.name); });

        for (String filePath : filesToSearch) {
            String content = ScriptManager.loadScript(filePath);
            if (content == null) continue;
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(new SearchResult(filePath, i + 1, lines[i].trim()));
                }
            }
        }
    }

    @Override
    public List<IEditorPanel> getCustomSidePanels() {
        List<IEditorPanel> panels = new ArrayList<>();
        panels.add(new IEditorPanel() {
            @Override public String getId() { return "search"; }
            @Override public String getDisplayName() { return "SEARCH"; }
            @Override public String getIcon() { return ""; }


            @Override
            public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int h) {
                var mc = Minecraft.getInstance();
                // Relative to x, y
                if (mx >= x + 10 && mx < x + w - 10 && my >= y + 10 && my < y + 30) {
                    // Open search dialog
                    var theme = ThemeManager.getCurrentTheme();
                    Screen parent = mc.screen;
                    DialogBuilder builder = new DialogBuilder(parent, theme);
                    DialogTextInputElement input = new DialogTextInputElement("Search Expression", searchQuery, theme);
                    builder.addElement(input);
                    builder.addButton("Search", theme.getPrimaryAccent(), () -> {
                        performSearch(input.getValue());
                        mc.setScreen(parent);
                    });
                    builder.addButton("Clear", theme.getBorder(), () -> {
                        performSearch("");
                        mc.setScreen(parent);
                    });
                    mc.setScreen(builder.build());
                    return true;
                }
                
                // Check results
                int ry = y + 45;
                for (SearchResult res : searchResults) {
                    if (mx >= x + 5 && mx < x + w - 5 && my >= ry && my < ry + 30) {
                        openFile(res.fileName);
                        // Jump to line logic
                        // Jump to line logic (reflection to avoid platform-specific dependencies)
                        try {
                            String screenName = mc.screen.getClass().getName();
                            if (screenName.contains("CodeEditorScreen")) {
                                java.lang.reflect.Method m = mc.screen.getClass().getMethod("jumpToLine", int.class);
                                m.invoke(mc.screen, res.line);
                            }
                        } catch (Exception ignored) {
                            LOGGER.debug("Failed to jump to line via reflection: {}", ignored.getMessage());
                        }
                        return true;
                    }
                    ry += 34;
                }
                return false;
            }

            @Override public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h, ITheme theme) {
                var mc = Minecraft.getInstance();
                var font = mc.font;
                
                // Search "Button" area (Top Input)
                int btnX = x + 10;
                int btnY = y + 10;
                int btnW = w - 20;
                int btnH = 24;
                boolean btnHovered = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
                
                // Glassmorphic background for search box
                g.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHovered ? 0x449D4EDD : 0x22FFFFFF);
                g.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, theme.getPrimaryAccent()); // Focus line
                
                String searchLabel = searchQuery.isEmpty() ? " 🔍  Search project..." : " 🔍  " + searchQuery;
                int textColor = searchQuery.isEmpty() ? theme.getMutedText() : theme.getPrimaryText();
                g.drawString(font, Component.literal(searchLabel), btnX + 8, btnY + 8, textColor, false);

                // Results list
                if (searchResults.isEmpty() && !searchQuery.isEmpty()) {
                    g.drawString(font, Component.literal("No matches found."), x + 15, y + 50, theme.getMutedText(), false);
                }

                int ry = y + 45;
                for (SearchResult res : searchResults) {
                    if (ry + 34 > y + h) {
                         g.drawString(font, Component.literal("... " + (searchResults.size() - searchResults.indexOf(res)) + " more"), x + 15, ry, theme.getMutedText(), false);
                         break; 
                    }
                    
                    boolean resHovered = mx >= x + 8 && mx < x + w - 8 && my >= ry && my < ry + 30;
                    
                    // Result background
                    if (resHovered) {
                        g.fill(x + 5, ry, x + w - 5, ry + 30, theme.getHover());
                    } else {
                        g.fill(x + 5, ry, x + w - 5, ry + 30, 0x11FFFFFF);
                    }

                    // File icon/path
                    String icon = res.fileName.endsWith(".kh") ? "📄 " : "📝 ";
                    String fileNameOnly = res.fileName.substring(res.fileName.lastIndexOf('/') + 1);
                    String title = icon + fileNameOnly + ":" + res.line;
                    g.drawString(font, Component.literal(title), x + 12, ry + 4, theme.getPrimaryAccent(), false);
                    
                    // Match content (syntax-like coloring for preview)
                    String snippet = res.content.trim();
                    if (font.width(snippet) > w - 30) {
                        snippet = font.plainSubstrByWidth(snippet, w - 40) + "...";
                    }
                    g.drawString(font, Component.literal(snippet), x + 12, ry + 16, 0xFFCCCCCC, false);
                    
                    ry += 34;
                }
            }
        });
        panels.add(new IEditorPanel() {
            @Override public String getId() { return "market"; }
            @Override public String getDisplayName() { return "MARKETPLACE"; }
            @Override public String getIcon() { return ""; }
            @Override public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int h) { return false; }
            @Override public void render(GuiGraphics g, int mx, int my, float pt, int x, int y, int w, int h, ITheme theme) {
                var mc = Minecraft.getInstance();
                var font = mc.font;
                String msg = "Marketplace coming soon...";
                String sub = "v1.1.0 Roadmap Feature";
                g.drawString(font, Component.literal(msg), x + 10, y + 10, theme.getPrimaryText(), false);
                g.drawString(font, Component.literal(sub), x + 10, y + 22, theme.getMutedText(), false);
            }
        });
        return panels;
    }

    @Override
    public List<IToolbarButton> getCustomToolbarButtons() {
        List<IToolbarButton> buttons = new ArrayList<>();

        buttons.add(new IToolbarButton() {
            @Override public String getId() { return "run"; }
            @Override public String getLabel() { return "Run"; }
            @Override public String getIconPath() { return "kashub:textures/icons/run.png"; }
            @Override public void onClick() {
                try {
                    String file = getCurrentFile() != null ? getCurrentFile() : "Untitled";
                    String content = getContent();
                    
                    if (isInCCMode()) {
                        // Derive remote path: use the actual filename from the tab.
                        // /mnt/cc/<id>/startup.lua -> startup.lua
                        // Any non-mnt path (local name opened before CC mode) -> keep as-is
                        String remotePath;
                        if (file.startsWith("/mnt/cc/")) {
                            remotePath = file.substring(file.lastIndexOf("/") + 1);
                        } else {
                            // Strip .kh extension if present for Lua files
                            remotePath = file.endsWith(".kh") ? file.substring(0, file.length() - 3) + ".lua" : file;
                            // Strip any leading path separators
                            if (remotePath.contains("/")) remotePath = remotePath.substring(remotePath.lastIndexOf("/") + 1);
                        }
                        
                        com.kasperstudios.khwrapper.KHWrapper.sendPacket(
                            new kasperstudios.kashub.network.payload.NetworkingPayloads.CCScriptPushPayload(ccComputerId, remotePath, content)
                        );
                        
                        // Send 'run' command to terminal proxy
                        com.kasperstudios.khwrapper.KHWrapper.sendPacket(
                            new kasperstudios.kashub.network.payload.NetworkingPayloads.CCTerminalInputPayload(ccComputerId, "run " + remotePath)
                        );
                        
                        ScriptLogger.getInstance().success("Pushed \"" + remotePath + "\" and executing on CC#" + ccComputerId);
                    } else {
                        TaskManager.getInstance().startScript(file, content, null, Type.USER);
                        ScriptLogger.getInstance().success("Script started locally!");
                    }
                } catch (Exception e) {
                    ScriptLogger.getInstance().error("Execution failed: " + e.getMessage());
                }
            }
            @Override public String getTooltip() { return "Run Script (F5)"; }
        });

        return buttons;
    }

    // --- File Provider ---

    @Override
    public IFileProvider getFileProvider() {
        return new IFileProvider() {
            @Override
            public List<IFileProvider.FileCategory> getCategories() {
                List<IFileProvider.FileCategory> categories = new ArrayList<>();
                // User scripts category
                List<String> userFiles = ScriptManager.getUserScripts();
                categories.add(new IFileProvider.FileCategory("user", "USER SCRIPTS", "", userFiles));
                
                // System scripts category (modpack)
                List<String> systemFiles = ScriptManager.getSystemScripts();
                categories.add(new IFileProvider.FileCategory("system", "MODPACK SCRIPTS", "", systemFiles));
                
                // ComputerCraft category
                if (isInCCMode()) {
                    List<String> ccPaths = new ArrayList<>();
                    var vfs = VirtualFileSystem.getInstance();
                    vfs.list("/mnt/cc/" + ccComputerId, "/").forEach(node -> {
                        ccPaths.add("/mnt/cc/" + ccComputerId + "/" + node.name);
                    });
                    categories.add(new IFileProvider.FileCategory("cc", "REMOTE DISK (CC#" + ccComputerId + ")", "", ccPaths));
                    
                    // In CC mode, we only show the CC disk for "Full replacement" feel
                    return List.of(categories.get(categories.size() - 1));
                }
                
                return categories;
            }

            @Override
            public String getFileIcon(String fileName, String categoryId) {
                if ("cc".equals(categoryId)) {
                    return "kashub:textures/icons/cc_file.png";
                }
                if ("user".equals(categoryId) || "system".equals(categoryId)) {
                    return "kashub:textures/icons/khicon.png";
                }
                return null;
            }

            @Override
            public void onFileSelected(String fileName, String categoryId) {
                openFile(fileName);
            }

            @Override public boolean canCreateFiles(String categoryId) { return "user".equals(categoryId) || "cc".equals(categoryId); }
            @Override public List<String> getCreatableCategories() { 
                if (isInCCMode()) return List.of("user", "cc");
                return List.of("user"); 
            }

            @Override
            public void createFile(String fileName, String categoryId, java.util.function.Consumer<Boolean> callback) {
                if (!"user".equals(categoryId) && !"cc".equals(categoryId)) {
                    callback.accept(false);
                    return;
                }
                
                String fullName = fileName.endsWith(".kh") || fileName.endsWith(".lua") ? fileName : (categoryId.equals("cc") ? fileName + ".lua" : fileName + ".kh");
                if ("cc".equals(categoryId)) {
                    // Create on remote disk
                    String fullPath = "/mnt/cc/" + ccComputerId + "/" + fullName;
                    try {
                        VirtualFileSystem.getInstance().write(fullPath, "".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        openFile(fullPath);
                        callback.accept(true);
                    } catch (java.io.IOException e) {
                        ScriptLogger.getInstance().error("Failed to create remote file: " + e.getMessage());
                        callback.accept(false);
                    }
                    return;
                }

                VirtualFileSystem vfs = VirtualFileSystem.getInstance();
                java.nio.file.Path path = vfs.resolve("~/" + fullName, "/");
                
                try {
                    if (path != null) {
                        if (path.getParent() != null) {
                            java.nio.file.Files.createDirectories(path.getParent());
                        }
                        java.nio.file.Files.writeString(path, "// New Khinux Script\n\n");
                        ScriptLogger.getInstance().success("Created file: " + fullName);
                        openFile("~/" + fullName);
                        callback.accept(true);
                        return;
                    }
                } catch (java.io.IOException e) {
                    ScriptLogger.getInstance().error("Failed to create file: " + e.getMessage());
                }
                callback.accept(false);
            }

            @Override
            public void deleteFile(String fileName, String categoryId, java.util.function.Consumer<Boolean> callback) {
                if ("system".equals(categoryId)) {
                    ScriptLogger.getInstance().error("Cannot delete system scripts!");
                    callback.accept(false);
                    return;
                }

                if ("cc".equals(categoryId)) {
                    try {
                        VirtualFileSystem.getInstance().delete(fileName);
                        ScriptLogger.getInstance().success("Deleted remote file: " + fileName);
                        List<String> tabs = getOpenTabs();
                        for (int i = 0; i < tabs.size(); i++) {
                            if (tabs.get(i).equals(fileName)) {
                                closeTab(i);
                                break;
                            }
                        }
                        callback.accept(true);
                        return;
                    } catch (Exception e) {
                        ScriptLogger.getInstance().error("Failed to delete remote file: " + e.getMessage());
                        callback.accept(false);
                        return;
                    }
                }
                
                // Resolve name to relative path for user scripts
                String relPath = fileName;
                if (fileName.startsWith("~/")) relPath = fileName.substring(2);
                else if (fileName.startsWith("/")) relPath = fileName.substring(1);

                boolean success = ScriptManager.deleteScript(relPath);
                if (success) {
                    ScriptLogger.getInstance().success("Deleted script: " + fileName);
                    // Close tab if open
                    List<String> tabs = getOpenTabs();
                    for (int i = 0; i < tabs.size(); i++) {
                        if (tabs.get(i).equals(fileName)) {
                            closeTab(i);
                            break;
                        }
                    }
                } else {
                    ScriptLogger.getInstance().error("Failed to delete script: " + fileName);
                }
                callback.accept(success);
            }
        };
    }

    @Override
    public void createNewUntitled() {
        String name = "Untitled_" + (untitledCounter++);
        openFiles.put(name, "// " + name + "\n\n");
        activeTabIndex = openFiles.size() - 1;
        saveSession();
    }

    // --- Providers ---

    @Override
    public ICompletionProvider getCompletionProvider() {
        return (line, column, prefix) -> {
            // Find full prefix including previous object (e.g. "System.")
            String fullPrefix = prefix;
            int start = column - prefix.length();
            if (start > 0 && line.charAt(start - 1) == '.') {
                // Extract object name
                int objEnd = start - 1;
                int objStart = objEnd - 1;
                while (objStart >= 0 && (Character.isLetterOrDigit(line.charAt(objStart)) || line.charAt(objStart) == '_' || line.charAt(objStart) == '$')) {
                    objStart--;
                }
                objStart++;
                String objectName = line.substring(objStart, objEnd);
                if (!objectName.isEmpty()) {
                    fullPrefix = objectName + "." + prefix;
                }
            }

            List<String> rawCompletions = CodeCompletionManager.getCompletions(fullPrefix);
            List<ICompletionProvider.CompletionItem> items = new ArrayList<>();
            for (String completion : rawCompletions) {
                ICompletionProvider.CompletionKind kind;

                if (completion.contains(".")) {
                    // Object member: player.getHealth, System.print, etc.
                    kind = ICompletionProvider.CompletionKind.FUNCTION;
                } else {
                    String snippet = CodeCompletionManager.getSnippet(completion);
                    if (snippet != null && !snippet.isEmpty()) {
                        kind = ICompletionProvider.CompletionKind.SNIPPET;
                    } else if (CodeCompletionManager.hasObjectSuggestions(completion)) {
                        // It's a top-level object name (player, System, VFS...)
                        kind = ICompletionProvider.CompletionKind.FUNCTION;
                    } else if (completion.equals("if") || completion.equals("else") || completion.equals("while")
                            || completion.equals("for") || completion.equals("fn") || completion.equals("return")
                            || completion.equals("break") || completion.equals("continue") || completion.equals("let")) {
                        kind = ICompletionProvider.CompletionKind.KEYWORD;
                    } else {
                        // User variable or command
                        kind = ICompletionProvider.CompletionKind.VARIABLE;
                    }
                }

                String cleanName = completion.contains(".")
                        ? completion.substring(completion.lastIndexOf('.') + 1)
                        : completion;

                String display = cleanName;
                String detail = CodeCompletionManager.getCommandParameters(completion);
                String doc = CodeCompletionManager.getCommandDescription(completion);
                items.add(new ICompletionProvider.CompletionItem(display, display, kind, detail, doc));
            }
            return items;
        };
    }

    @Override
    public List<String> getUserVariables() {
        return CodeCompletionManager.getUserVariables();
    }

    @Override
    public List<String> getObjectMembers(String objectName) {
        return CodeCompletionManager.getObjectMembers(objectName);
    }

    // --- IConsoleProvider ---

    @Override
    public List<ProblemEntry> getProblems() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getOutputLines() {
        return ScriptLogger.getInstance().getRecentLogs(200).stream()
                .filter(entry -> entry.level.priority >= ScriptLogger.LogLevel.INFO.priority)
                .map(this::formatLogEntry)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> getTerminalLines() {
        // For terminal, we usually want all logs (including DEBUG if enabled)
        return ScriptLogger.getInstance().getRecentLogs(500).stream()
                .map(this::formatLogEntry)
                .collect(java.util.stream.Collectors.toList());
    }

    private String formatLogEntry(ScriptLogger.LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        // Color codes based on level
        if (entry.level == ScriptLogger.LogLevel.ERROR) sb.append("§c");
        else if (entry.level == ScriptLogger.LogLevel.SUCCESS) sb.append("§a");
        else if (entry.level == ScriptLogger.LogLevel.WARN) sb.append("§e");
        else if (entry.level == ScriptLogger.LogLevel.DEBUG) sb.append("§7");
        
        // Don't show timestamp/script name in terminal to keep it clean, 
        // unless it's a specific script log
        if (entry.scriptName != null && !entry.scriptName.equals("PRINT") && !entry.scriptName.startsWith("CC#")) {
            sb.append("[").append(entry.scriptName).append("] ");
        }
        
        sb.append(entry.message);
        return sb.toString();
    }

    @Override
    public void submitTerminalCommand(String command) {
        if (isInCCMode()) {
            // Echo input locally so the user sees it with the correct prefix
            ScriptLogger.getInstance().info(getTerminalPrompt() + command);
            kasperstudios.kashub.network.NetworkingManager.getInstance()
                    .sendCCTerminalInput(ccComputerId, command);
        } else {
            kasperstudios.kashub.core.TerminalManager.getInstance().execute(command);
        }
    }

    @Override
    public String getTerminalPrompt() {
        if (isInCCMode()) {
            return "cc#" + ccComputerId + "> ";
        }
        return kasperstudios.kashub.core.TerminalManager.getInstance().getPrompt();
    }
}

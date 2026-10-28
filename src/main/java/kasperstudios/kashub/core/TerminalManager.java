package kasperstudios.kashub.core;

import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.VirtualFileSystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.kasperstudios.khwrapper.KHWrapper;

/**
 * TerminalManager - Handlers terminal input for the KasHub OS Layer.
 */
public class TerminalManager {
    private static final Path CONFIG_FILE = KHWrapper.get().getConfigDir()
            .resolve("kashub").resolve("system").resolve("terminal.khc");
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private static final TerminalManager instance = new TerminalManager();

    private String currentDir = "~/";
    private boolean hasSudo = false;
    private boolean hasSu = false;
    private final Map<String, String> config = new HashMap<>();
    private FileOpener fileOpener;

    public interface FileOpener {
        void openFile(String virtualPath);
    }

    private TerminalManager() {
        loadConfig();
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    public static TerminalManager getInstance() {
        return instance;
    }

    public void setFileOpener(FileOpener fileOpener) {
        this.fileOpener = fileOpener;
    }

    public void execute(String input) {
        if (input == null || input.trim().isEmpty()) return;

        List<String> tokens = tokenize(input.trim());
        if (tokens.isEmpty()) return;

        String cmd = tokens.get(0);
        List<String> args = tokens.subList(1, tokens.size());

        // Builtin Commands (Core OS)
        if (cmd.equals("sudo")) {
            if (args.isEmpty()) {
                hasSudo = true;
                ScriptLogger.getInstance().info("§e[KasHub] Elevated to SUDO Mode");
            } else {
                String fullCommand = String.join(" ", args);
                boolean prevSudo = hasSudo;
                hasSudo = true;
                execute(fullCommand);
                hasSudo = prevSudo;
            }
            return;
        }

        if (cmd.equals("su")) {
            hasSu = true;
            hasSudo = true;
            ScriptLogger.getInstance().info("§c[KasHub] Elevated to SUPERUSER (Driver/Kernel Mode)");
            return;
        }

        if (cmd.equals("exit")) {
            if (hasSu || hasSudo) {
                hasSu = false;
                hasSudo = false;
                ScriptLogger.getInstance().info("§a[KasHub] Permission downgrade to NORMAL Mode");
            } else {
                ScriptLogger.getInstance().info("§7[KasHub] Use 'exit' to escape su/sudo, or close the editor.");
            }
            return;
        }

        if (cmd.equals("clear")) {
            ScriptLogger.getInstance().clear();
            return;
        }

        if (cmd.equals("pwd")) {
            ScriptLogger.getInstance().info("§a" + currentDir);
            return;
        }

        // Native Commands (for stability)
        if (cmd.equals("ls")) {
            handleLs(args);
            return;
        }

        if (cmd.equals("cd")) {
            handleCd(args);
            return;
        }

        if (cmd.equals("open")) {
            handleOpen(args);
            return;
        }

        if (cmd.equals("help")) {
            handleHelp(args);
            return;
        }

        if (cmd.equals("whoami")) {
            handleWhoAmI(args);
            return;
        }

        // Script-based commands (Linux concept)
        // 1. Search in /system/bin/
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        Path binPath = vfs.resolve("/system/bin/" + cmd + ".kh", "/");
        
        if (binPath != null && Files.exists(binPath)) {
            runScriptCommand(binPath, args);
            return;
        }

        // 2. Search in current directory (if dot-slash specified)
        if (cmd.startsWith("./")) {
            String scriptName = cmd.substring(2);
            Path localPath = vfs.resolve(scriptName.endsWith(".kh") ? scriptName : scriptName + ".kh", currentDir);
            if (localPath != null && Files.exists(localPath)) {
                runScriptCommand(localPath, args);
                return;
            }
        }

        // 3. Fallback: maybe it's just a file in current dir
        Path fallbackPath = vfs.resolve(cmd.endsWith(".kh") ? cmd : cmd + ".kh", currentDir);
        if (fallbackPath != null && Files.exists(fallbackPath)) {
            runScriptCommand(fallbackPath, args);
            return;
        }

        ScriptLogger.getInstance().error("[KasHub] Command not found: " + cmd);
    }

    private void handleLs(List<String> args) {
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        String path = args.isEmpty() ? currentDir : args.get(0);
        List<VirtualFileSystem.VfsNode> nodes = vfs.list(path, currentDir);

        if (nodes.isEmpty()) {
            ScriptLogger.getInstance().info("§7[ls] Directory is empty or not found: " + path);
            return;
        }

        ScriptLogger.getInstance().info("§6[ls] Contents of " + path + ":");
        for (VirtualFileSystem.VfsNode node : nodes) {
            String prefix = node.isDirectory ? "§b[DIR]§f " : "      ";
            ScriptLogger.getInstance().info(prefix + node.name);
        }
    }

    private void handleCd(List<String> args) {
        if (args.isEmpty()) {
            currentDir = "~/";
            ScriptLogger.getInstance().info("§aWorking directory: " + currentDir);
            return;
        }

        String target = args.get(0);
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        Path physical = vfs.resolve(target, currentDir);

        if (physical != null && Files.exists(physical) && Files.isDirectory(physical)) {
            String virtual = vfs.toVirtual(physical);
            if (vfs.isSystemPath(virtual, "/") && !hasSudo) {
                ScriptLogger.getInstance().error("[cd] Access Denied: sudo required for " + virtual);
                return;
            }
            currentDir = virtual;
            ScriptLogger.getInstance().info("§aWorking directory: " + currentDir);
        } else {
            ScriptLogger.getInstance().error("[cd] Directory not found: " + target);
        }
    }

    private void handleWhoAmI(List<String> args) {
        String user = hasSu ? "root" : (hasSudo ? "sudo-user" : "user");
        String origin = "TERMINAL"; // TerminalManager always acts on TERMINAL origin for manual input
        ScriptLogger.getInstance().info("§aUser: §f" + user);
        ScriptLogger.getInstance().info("§aOrigin: §f" + origin);
        ScriptLogger.getInstance().info("§aPermissions: §f" + (hasSu ? "§c[KERNEL/DRIVER]" : (hasSudo ? "§e[SUDO]" : "§7[NORMAL]")));
    }

    private void handleHelp(List<String> args) {
        ScriptLogger.getInstance().info("§6=== KasHub Terminal Help ===");
        ScriptLogger.getInstance().info("§fBuilt-in commands:");
        ScriptLogger.getInstance().info("  §bhelp§f      - Show this help message");
        ScriptLogger.getInstance().info("  §bwhoami§f    - Show current user and origin");
        ScriptLogger.getInstance().info("  §bls§f [path] - List directory contents");
        ScriptLogger.getInstance().info("  §bcd§f <path> - Change current directory");
        ScriptLogger.getInstance().info("  §bopen§f <file>- Open file in editor");
        ScriptLogger.getInstance().info("  §bclear§f     - Clear terminal output");
        ScriptLogger.getInstance().info("  §bsudo§f [cmd]- Elevate permissions");
        ScriptLogger.getInstance().info("  §bsu§f        - Switch to superuser mode");
        ScriptLogger.getInstance().info("  §bexit§f      - Exit sudo/su or terminal");
        
        ScriptLogger.getInstance().info("§fSystem binaries (/system/bin):");
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        List<VirtualFileSystem.VfsNode> nodes = vfs.list("/system/bin", "/");
        if (!nodes.isEmpty()) {
            StringBuilder bins = new StringBuilder("  ");
            for (int i = 0; i < nodes.size(); i++) {
                String name = nodes.get(i).name;
                if (name.endsWith(".kh")) name = name.substring(0, name.length() - 3);
                bins.append("§e").append(name).append("§f");
                if (i < nodes.size() - 1) bins.append(", ");
            }
            ScriptLogger.getInstance().info(bins.toString());
        } else {
            ScriptLogger.getInstance().info("  §7(No system binaries found)");
        }
        ScriptLogger.getInstance().info("§6============================");
    }

    private void handleOpen(List<String> args) {
        if (args.isEmpty()) {
            ScriptLogger.getInstance().error("[open] Usage: open <file>");
            return;
        }

        String target = args.get(0);
        VirtualFileSystem vfs = VirtualFileSystem.getInstance();
        Path physical = vfs.resolve(target, currentDir);
        
        // Auto-extension .kh
        if (physical == null || !Files.exists(physical)) {
            String targetWithExt = target.endsWith(".kh") ? target : target + ".kh";
            Path p2 = vfs.resolve(targetWithExt, currentDir);
            if (p2 != null && Files.exists(p2)) {
                physical = p2;
            }
        }

        // Fallback: check in user home (~/) if relative failed
        if ((physical == null || !Files.exists(physical)) && !target.startsWith("/") && !target.startsWith("~")) {
            String targetWithExt = target.endsWith(".kh") ? target : target + ".kh";
            Path pHome = vfs.resolve("~/" + targetWithExt, "/");
            if (pHome != null && Files.exists(pHome)) {
                physical = pHome;
            }
        }

        // Fallback: check bundled / system scripts via ScriptManager
        if (physical == null || !Files.exists(physical)) {
            String baseName = target.endsWith(".kh") ? target.substring(0, target.length() - 3) : target;
            if (kasperstudios.kashub.util.ScriptManager.scriptExists(baseName)) {
                if (fileOpener != null) {
                    fileOpener.openFile(baseName + ".kh");
                    ScriptLogger.getInstance().success("Opening " + baseName + ".kh");
                }
                return;
            }
        }

        if (physical != null && Files.exists(physical) && !Files.isDirectory(physical)) {
            String virtual = vfs.toVirtual(physical);
            if (vfs.isSystemPath(virtual, "/") && !hasSudo) {
                ScriptLogger.getInstance().error("[open] Access Denied: sudo required for " + virtual);
                return;
            }
            
            if (fileOpener != null) {
                fileOpener.openFile(virtual);
                ScriptLogger.getInstance().success("Opening " + virtual);
            } else {
                ScriptLogger.getInstance().error("[open] File opener callback not registered.");
            }
        } else {
            ScriptLogger.getInstance().error("[open] File not found: " + target);
        }
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens;
    }


    private void runScriptCommand(Path path, List<String> args) {
        executor.execute(() -> {
            try {
                String content = Files.readString(path);
                String name = path.getFileName().toString();
                
                // Create context with permissions
                // Note: v0.9.0 Context/Interpreter setup
                
                // [!] Permissions are now passed correctly
                TaskManager.getInstance().startScript(name, content, null, 
                    VirtualFileSystem.getInstance().isSystemPath(name, currentDir) ? Type.SYSTEM : Type.USER,
                    ExecutionOrigin.TERMINAL,
                    args, hasSudo, hasSu);
            } catch (IOException e) {
                ScriptLogger.getInstance().error("[KasHub] Failed to read script: " + e.getMessage());
            } catch (Exception e) {
                ScriptLogger.getInstance().error("[KasHub] Execution error: " + e.getMessage());
            }
        });
    }

    public String getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(String currentDir) {
        this.currentDir = currentDir;
    }

    public boolean hasSudo() {
        return hasSudo;
    }

    public boolean hasSu() {
        return hasSu;
    }

    public String getPrompt() {
        return config.getOrDefault("PROMPT", "> ");
    }

    public String getSetting(String key) {
        return config.get(key);
    }

    public void setSetting(String key, String value) {
        config.put(key, value);
        saveConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                Files.createDirectories(CONFIG_FILE.getParent());
                config.put("PROMPT", "user@kashub:$ ");
                saveConfig();
                return;
            }

            List<String> lines = Files.readAllLines(CONFIG_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    config.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            ScriptLogger.getInstance().error("[KasHub] Failed to load terminal config: " + e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# KasHub Terminal Configuration\n");
            for (Map.Entry<String, String> entry : config.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            Files.writeString(CONFIG_FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ScriptLogger.getInstance().error("[KasHub] Failed to save terminal config: " + e.getMessage());
        }
    }
}

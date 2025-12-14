package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.config.KashubConfig;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Command for executing Java code dynamically.
 * WARNING: This is a dangerous command that can execute arbitrary Java code.
 * Must be explicitly enabled in config with allowEval = true.
 * 
 * Usage:
 *   eval System.out.println("Hello");
 *   eval $code  (where $code is a variable containing Java code)
 * 
 * The code has access to:
 *   - MinecraftClient mc
 *   - ScriptInterpreter interpreter
 *   - Map<String, String> vars (script variables)
 */
public class EvalCommand implements Command {
    private static final Path EVAL_DIR = Paths.get("config", "kashub", "eval_cache");
    private static int evalCounter = 0;
    private static int evalCalls = 0;
    private static int evalTimeouts = 0;
    private static int evalErrors = 0;
    
    // Security blacklist - dangerous operations
    private static final String[] BLACKLIST = {
        "Runtime.getRuntime()",
        "System.exit",
        "ProcessBuilder",
        "Files.delete",
        "Files.write",
        "Files.move",
        "Files.copy",
        "FileOutputStream",
        "FileWriter",
        "Runtime.exec",
        "Class.forName",
        "System.setSecurityManager",
        "System.setProperty",
        "Unsafe",
        "sun.misc",
        "java.lang.reflect.Method.invoke"
    };
    
    // Safe mode - only allow these packages
    private static final String[] SAFE_PACKAGES = {
        "net.minecraft",
        "java.util",
        "java.lang.Math",
        "java.lang.String",
        "kasperstudios.kashub"
    };
    
    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public String getDescription() {
        return "Executes Java code dynamically (DANGEROUS - must be enabled in config)";
    }

    @Override
    public String getParameters() {
        return "<java_code> - Java code to execute";
    }

    @Override
    public String getCategory() {
        return "Other";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Execute Java code (DANGEROUS - requires allowEval).\n\n" +
               "Available objects:\n" +
               "  mc          - MinecraftClient\n" +
               "  player      - ClientPlayerEntity\n" +
               "  world       - ClientWorld\n" +
               "  interpreter - ScriptInterpreter\n" +
               "  vars        - Script variables Map\n\n" +
               "Helper methods:\n" +
               "  print(msg)              - Send to chat\n" +
               "  getVar(vars, name)      - Get variable\n" +
               "  setVar(vars, name, val) - Set variable\n\n" +
               "Examples:\n" +
               "  eval print(\"Hello!\");\n" +
               "  eval player.setHealth(20.0f);\n" +
               "  eval print(player.getX());\n\n" +
               "Security: 10 second timeout, requires JDK";
    }

    @Override
    public void execute(String[] args) throws Exception {
        evalCalls++;
        
        // Check if eval is enabled
        if (!KashubConfig.getInstance().allowEval) {
            throw new SecurityException("eval command is disabled. Enable it in config with allowEval = true");
        }
        
        if (args.length == 0) {
            throw new IllegalArgumentException("No code provided. Usage: eval <java_code>");
        }
        
        String code = String.join(" ", args);
        
        // Security check - blacklist dangerous operations
        if (!checkSecurity(code)) {
            evalErrors++;
            throw new SecurityException("Code contains blacklisted operations. Eval blocked for security.");
        }
        
        // Log the eval attempt
        ScriptLogger.getInstance().warn("EVAL executing: " + code.substring(0, Math.min(code.length(), 100)) + "...");
        
        try {
            executeJavaCode(code);
        } catch (TimeoutException e) {
            evalTimeouts++;
            ScriptLogger.getInstance().error("EVAL timeout: " + code.substring(0, Math.min(code.length(), 50)));
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[EVAL TIMEOUT] Code execution exceeded 10 seconds"), false);
                }
            });
            throw e;
        } catch (Exception e) {
            evalErrors++;
            ScriptLogger.getInstance().error("EVAL error: " + e.getMessage());
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[EVAL ERROR] " + e.getMessage()), false);
                }
            });
            throw e;
        }
    }
    
    private void executeJavaCode(String code) throws Exception {
        // Create eval directory if not exists
        Files.createDirectories(EVAL_DIR);
        
        // Generate unique class name
        String className = "EvalScript_" + (++evalCounter) + "_" + System.currentTimeMillis();
        
        // Wrap user code in a class
        String fullCode = generateClassCode(className, code);
        
        // Write source file
        Path sourceFile = EVAL_DIR.resolve(className + ".java");
        Files.write(sourceFile, fullCode.getBytes());
        
        try {
            // Compile the code
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new RuntimeException("Java compiler not available. Make sure you're running with JDK, not JRE.");
            }
            
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
            
            // Set classpath to include Minecraft and mod classes
            String classpath = System.getProperty("java.class.path");
            List<String> options = Arrays.asList("-classpath", classpath, "-d", EVAL_DIR.toString());
            
            Iterable<? extends JavaFileObject> compilationUnits = 
                fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile.toFile()));
            
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
            
            boolean success = task.call();
            fileManager.close();
            
            if (!success) {
                StringBuilder errors = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errors.append(String.format("Line %d: %s%n", 
                        diagnostic.getLineNumber(), 
                        diagnostic.getMessage(null)));
                }
                throw new RuntimeException(errors.toString());
            }
            
            // Load and execute the compiled class
            URL[] urls = new URL[] { EVAL_DIR.toUri().toURL() };
            try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {
                Class<?> evalClass = classLoader.loadClass(className);
                Method runMethod = evalClass.getMethod("run", MinecraftClient.class, ScriptInterpreter.class, Map.class);
                
                // Execute with timeout
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<?> future = executor.submit(() -> {
                    try {
                        runMethod.invoke(null, 
                            MinecraftClient.getInstance(), 
                            ScriptInterpreter.getInstance(),
                            ScriptInterpreter.getInstance().getVariables());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                
                try {
                    future.get(10, TimeUnit.SECONDS); // 10 second timeout
                } catch (TimeoutException e) {
                    future.cancel(true);
                    executor.shutdownNow();
                    throw new TimeoutException("Eval execution timed out (10 seconds)");
                } catch (ExecutionException e) {
                    executor.shutdownNow();
                    Throwable cause = e.getCause();
                    throw new RuntimeException("Eval execution failed: " + (cause != null ? cause.getMessage() : e.getMessage()), cause);
                } finally {
                    executor.shutdownNow();
                }
            }
            
            // Success message
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§a[EVAL] Code executed successfully"), false);
                }
            });

        } finally {
            // Cleanup source file
            try {
                Files.deleteIfExists(sourceFile);
                Files.deleteIfExists(EVAL_DIR.resolve(className + ".class"));
            } catch (Exception ignored) {}
        }
    }
    
    private String generateClassCode(String className, String userCode) {
        return String.format("""
            import net.minecraft.client.MinecraftClient;
            import net.minecraft.text.Text;
            import net.minecraft.entity.player.PlayerEntity;
            import net.minecraft.item.ItemStack;
            import net.minecraft.util.math.BlockPos;
            import net.minecraft.util.math.Vec3d;
            import net.minecraft.world.World;
            import net.minecraft.block.Block;
            import net.minecraft.entity.Entity;
            import kasperstudios.kashub.algorithm.ScriptInterpreter;
            import java.util.*;
            
            public class %s {
                public static void run(MinecraftClient mc, ScriptInterpreter interpreter, Map<String, String> vars) throws Exception {
                    // Helper variables for convenience
                    var player = mc.player;
                    var world = mc.world;
                    
                    // User code starts here
                    %s
                    // User code ends here
                }
                
                // Helper method to send chat message
                private static void print(String message) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.sendMessage(Text.literal("§d[EVAL] §f" + message), false);
                        }
                    });
                }
                
                // Helper method to get variable
                private static String getVar(Map<String, String> vars, String name) {
                    return vars.getOrDefault(name, "");
                }
                
                // Helper method to set variable
                private static void setVar(Map<String, String> vars, String name, String value) {
                    vars.put(name, value);
                }
            }
            """, className, userCode);
    }
    
    /**
     * Security check - validates code against blacklist
     */
    private boolean checkSecurity(String code) {
        KashubConfig config = KashubConfig.getInstance();
        
        // Check blacklist
        for (String forbidden : BLACKLIST) {
            if (code.contains(forbidden)) {
                ScriptLogger.getInstance().error("SECURITY: Eval blocked - contains forbidden operation: " + forbidden);
                return false;
            }
        }
        
        // If safe eval mode is enabled in config, check package whitelist
        if (config.sandboxMode) {
            // Allow basic operations without package names
            String codeNormalized = code.replaceAll("\\s+", " ");
            
            // Check for import statements or fully qualified class names
            if (code.contains("import ") || code.matches(".*\\b[a-z]+\\.[a-z]+\\..*")) {
                boolean hasSafePackage = false;
                for (String safePackage : SAFE_PACKAGES) {
                    if (code.contains(safePackage)) {
                        hasSafePackage = true;
                        break;
                    }
                }
                
                // If using packages but none are safe, block it
                if (!hasSafePackage && (code.contains("import ") || code.matches(".*\\b(java|javax|sun|com|org)\\.[a-z]+\\..*"))) {
                    ScriptLogger.getInstance().error("SECURITY: Eval blocked - uses non-whitelisted packages in sandbox mode");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get eval statistics
     */
    public static Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("calls", evalCalls);
        stats.put("timeouts", evalTimeouts);
        stats.put("errors", evalErrors);
        return stats;
    }
}

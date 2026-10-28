package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import com.kasperstudios.khwrapper.KHWrapper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

/**
 * ResourceManager — Extracts bundled system scripts from mod resources to the
 * user's config directory on first launch (or when they are missing).
 *
 * System scripts bundled at: assets/kashub/system/bin/*.kh
 * Target on disk:            config/kashub/system/bin/*.kh
 *
 * Scripts are never overwritten if they already exist, so users can customize them.
 */
public class ResourceManager {

    private static final String RESOURCE_PREFIX = "assets/kashub/system/bin/";
    private static final Path BIN_DIR = KHWrapper.get()
            .getConfigDir().resolve("kashub/system/bin");
    private static final Path SCRIPTS_DIR = KHWrapper.get()
            .getConfigDir().resolve("kashub/scripts");
    private static final Path DOCS_DIR = KHWrapper.get()
            .getConfigDir().resolve("kashub/docs");

    /** System scripts that must always be present. */
    private static final List<String> SYSTEM_SCRIPTS = List.of(
            "cd.kh", "ls.kh", "open.kh", "kh.kh"
    );

    /** Embedded documentation resources to seed into config/kashub/docs/ */
    private static final String DOCS_RESOURCE_PREFIX = "assets/kashub/docs/";
    private static final List<String> EMBEDDED_DOCS = List.of(
            "README.md",
            "api-overview.md",
            "cct-integration.md",
            "DOCUMENTATION_SUMMARY.md",
            "editor-guide.md",
            "installation.md",
            "khscript-syntax.md",
            "khinux-terminal.md",
            "neural-implant.md",
            "pathfinding-guide.md",
            "quick-start.md"
    );

    private static boolean initialized = false;

    /**
     * Call once during mod initialization.
     * Extracts all missing system scripts to config/kashub/system/bin/
     * and seeds welcome.kh in user scripts if missing.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;

        try {
            Files.createDirectories(BIN_DIR);
            Files.createDirectories(SCRIPTS_DIR);
            Files.createDirectories(DOCS_DIR);
        } catch (IOException e) {
            Kashub.LOGGER.error("[ResourceManager] Failed to create directories: {}", e.getMessage());
            return;
        }

        // Seed welcome.kh in user scripts on first launch
        Path welcomeTarget = SCRIPTS_DIR.resolve("welcome.kh");
        if (!Files.exists(welcomeTarget)) {
            try (InputStream in = getResource("assets/kashub/scripts/welcome.kh")) {
                if (in != null) {
                    Files.copy(in, welcomeTarget, StandardCopyOption.REPLACE_EXISTING);
                    Kashub.LOGGER.info("[ResourceManager] Seeded welcome.kh into user scripts directory.");
                }
            } catch (IOException e) {
                Kashub.LOGGER.warn("[ResourceManager] Failed to seed welcome.kh: {}", e.getMessage());
            }
        }

        // Seed test_all.kh in user scripts
        Path testTarget = SCRIPTS_DIR.resolve("test_all.kh");
        try (InputStream in = getResource("assets/kashub/scripts/test_all.kh")) {
            if (in != null) {
                Files.copy(in, testTarget, StandardCopyOption.REPLACE_EXISTING);
                Kashub.LOGGER.info("[ResourceManager] Seeded test_all.kh into user scripts directory.");
            }
        } catch (IOException e) {
            Kashub.LOGGER.warn("[ResourceManager] Failed to seed test_all.kh: {}", e.getMessage());
        }

        int extracted = 0;
        for (String script : SYSTEM_SCRIPTS) {
            Path target = BIN_DIR.resolve(script);
            if (Files.exists(target)) {
                // Already on disk — skip (preserve user customization)
                continue;
            }

            String resourcePath = RESOURCE_PREFIX + script;
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    Kashub.LOGGER.warn("[ResourceManager] Bundled script not found: {}", resourcePath);
                    continue;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                extracted++;
                Kashub.LOGGER.info("[ResourceManager] Extracted {} -> {}", script, target);
            } catch (IOException e) {
                Kashub.LOGGER.error("[ResourceManager] Failed to extract {}: {}", script, e.getMessage());
            }
        }

        if (extracted > 0) {
            Kashub.LOGGER.info("[ResourceManager] Extracted {} system script(s) on first launch.", extracted);
        } else {
            Kashub.LOGGER.debug("[ResourceManager] All system scripts already present.");
        }

        // Extract bundled documentation
        try {
            Files.createDirectories(DOCS_DIR);
            int docsExtracted = 0;
            for (String docFile : EMBEDDED_DOCS) {
                Path target = DOCS_DIR.resolve(docFile);
                if (!Files.exists(target)) {
                    String resourcePath = DOCS_RESOURCE_PREFIX + docFile;
                    try (InputStream in = getResource(resourcePath)) {
                        if (in != null) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            docsExtracted++;
                        }
                    } catch (IOException e) {
                        Kashub.LOGGER.warn("[ResourceManager] Failed to extract doc {}: {}", docFile, e.getMessage());
                    }
                }
            }
            if (docsExtracted > 0) {
                Kashub.LOGGER.info("[ResourceManager] Extracted {} doc file(s) to config/kashub/docs/.", docsExtracted);
            }
        } catch (IOException e) {
            Kashub.LOGGER.error("[ResourceManager] Failed to initialize docs directory: {}", e.getMessage());
        }
    }

    /**
     * Forces re-extraction of ALL system scripts, overwriting existing files.
     * Called when the user runs 'kh reload' or equivalent.
     */
    public static void forceReload() {
        initialized = false;
        for (String script : SYSTEM_SCRIPTS) {
            Path target = BIN_DIR.resolve(script);
            String resourcePath = RESOURCE_PREFIX + script;
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) continue;
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                Kashub.LOGGER.info("[ResourceManager] Reloaded: {}", script);
            } catch (IOException e) {
                Kashub.LOGGER.warn("[ResourceManager] Could not reload {}: {}", script, e.getMessage());
            }
        }
        initialized = true;
    }

    /** Checks whether a system script is present on disk. */
    public static boolean isPresent(String scriptName) {
        return Files.exists(BIN_DIR.resolve(scriptName));
    }

    private static InputStream getResource(String path) {
        // Try class loader (works in both dev and production)
        InputStream in = ResourceManager.class.getClassLoader().getResourceAsStream(path);
        if (in != null) return in;
        // Fallback via URL
        URL url = ResourceManager.class.getClassLoader().getResource(path);
        if (url == null) return null;
        try {
            return url.openStream();
        } catch (IOException e) {
            return null;
        }
    }
}

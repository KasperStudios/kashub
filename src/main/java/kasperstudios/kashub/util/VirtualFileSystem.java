package kasperstudios.kashub.util;

import kasperstudios.kashub.Kashub;
import com.kasperstudios.khwrapper.KHWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.HashMap;
import java.util.Map;

/**
 * VirtualFileSystem (VFS) for the Khinux OS Layer.
 * Maps virtual paths like ~/ and /system to physical paths on disk.
 * Supports external mounts (e.g. for ComputerCraft).
 */
public class VirtualFileSystem {

    private static final Path ROOT_DIR = KHWrapper.getConfigDir().resolve("kashub");
    private static final Path USER_HOME = ROOT_DIR.resolve("scripts");      // ~/
    private static final Path SYSTEM_DIR = ROOT_DIR.resolve("system");     // /system
    private static final Path BIN_DIR = SYSTEM_DIR.resolve("bin");         // /system/bin
    private static final Path MNT_DIR = ROOT_DIR.resolve("mnt");           // /mnt

    private final Map<String, IMount> mounts = new HashMap<>();

    private static VirtualFileSystem instance;

    private VirtualFileSystem() {
        try {
            Files.createDirectories(ROOT_DIR);
            Files.createDirectories(USER_HOME);
            Files.createDirectories(SYSTEM_DIR);
            Files.createDirectories(BIN_DIR);
            Files.createDirectories(MNT_DIR);
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to initialize VirtualFileSystem directories", e);
        }
    }

    public static VirtualFileSystem getInstance() {
        if (instance == null) {
            instance = new VirtualFileSystem();
        }
        return instance;
    }

    public void mount(String virtualPath, IMount mount) {
        String normalized = normalizeVirtualPath(virtualPath);
        mounts.put(normalized, mount);
        Kashub.LOGGER.info("VFS: Mounted " + mount.getName() + " at " + normalized);
    }

    public void unmount(String virtualPath) {
        String normalized = normalizeVirtualPath(virtualPath);
        mounts.remove(normalized);
        Kashub.LOGGER.info("VFS: Unmounted " + normalized);
    }

    private String normalizeVirtualPath(String path) {
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }

    /**
     * Resolves a virtual path to a physical path on disk.
     * 
     * @param virtualPath The path (e.g., "~/test.kh", "/system/bin", "local_file")
     * @param currentDir  The current working directory (virtual)
     * @return The physical path, or null if it escapes the root
     */
    public Path resolve(String virtualPath, String currentDir) {
        if (virtualPath == null) return null;
        String path = virtualPath;

        // Check for mounts first
        String absoluteVirtual = toAbsoluteVirtual(virtualPath, currentDir);
        for (Map.Entry<String, IMount> entry : mounts.entrySet()) {
            if (absoluteVirtual.startsWith(entry.getKey())) {
                // This path is handled by a mount, we don't resolve it to a physical path here
                // Note: IMount handles its own internal resolution
                return null; 
            }
        }

        // Handle home shortcut
        if (path.startsWith("~/")) {
            path = USER_HOME.resolve(path.substring(2)).toString();
        } else if (path.equals("~")) {
            path = USER_HOME.toString();
        } else if (path.startsWith("/")) {
            // Absolute path from virtual root
            path = ROOT_DIR.resolve(path.substring(1).replace("/", java.io.File.separator)).toString();
        } else {
            // Relative path
            String normalizedCurrent = currentDir.startsWith("/") ? currentDir.substring(1) : currentDir;
            if (normalizedCurrent.startsWith("~")) {
                Path base = USER_HOME.resolve(normalizedCurrent.length() > 1 ? normalizedCurrent.substring(2) : "");
                path = base.resolve(path.replace("/", java.io.File.separator)).toString();
            } else {
                Path base = ROOT_DIR.resolve(normalizedCurrent.replace("/", java.io.File.separator));
                path = base.resolve(path.replace("/", java.io.File.separator)).toString();
            }
        }

        try {
            Path resolved = Paths.get(path).normalize();

            // Security check: ensure it's within ROOT_DIR
            if (!resolved.toAbsolutePath().startsWith(ROOT_DIR.toAbsolutePath())) {
                Kashub.LOGGER.warn("Blocked VFS escape attempt: " + virtualPath + " (resolved to: " + resolved + ")");
                return null;
            }

            return resolved;
        } catch (Exception e) {
            Kashub.LOGGER.error("Error resolving VFS path: " + virtualPath, e);
            return null;
        }
    }

    /**
     * Converts a physical path back to a virtual path.
     */
    public String toVirtual(Path physicalPath) {
        Path relative = ROOT_DIR.relativize(physicalPath.normalize());
        String virtual = "/" + relative.toString().replace("\\", "/");
        
        // Special case for home
        Path homeRel = ROOT_DIR.relativize(USER_HOME);
        String homePart = homeRel.toString().replace("\\", "/");
        if (virtual.startsWith("/" + homePart)) {
            return "~" + virtual.substring(homePart.length() + 1);
        }
        
        return virtual;
    }

    private String toAbsoluteVirtual(String virtualPath, String currentDir) {
        if (virtualPath.startsWith("/") || virtualPath.startsWith("~")) return virtualPath;
        
        String normalizedCurrent = normalizeVirtualPath(currentDir);
        if (normalizedCurrent.equals("/")) return "/" + virtualPath;
        return normalizedCurrent + "/" + virtualPath;
    }

    /**
     * Lists files in a virtual directory.
     */
    public List<VfsNode> list(String virtualPath, String currentDir) {
        String absoluteVirtual = toAbsoluteVirtual(virtualPath, currentDir);
        
        // Check for mount list
        for (Map.Entry<String, IMount> entry : mounts.entrySet()) {
            String mountPoint = entry.getKey();
            if (absoluteVirtual.startsWith(mountPoint)) {
                String subPath = absoluteVirtual.substring(mountPoint.length());
                if (subPath.startsWith("/")) subPath = subPath.substring(1);
                return entry.getValue().list(subPath);
            }
        }

        Path physical = resolve(virtualPath, currentDir);
        if (physical == null || !Files.exists(physical) || !Files.isDirectory(physical)) {
            // Check if we are listing /mnt to show available mounts
            if (absoluteVirtual.equals("/mnt")) {
                return mounts.keySet().stream()
                        .filter(k -> k.startsWith("/mnt/"))
                        .map(k -> k.substring(5)) // Get name after /mnt/
                        .map(name -> new VfsNode(name, true, "/mnt/" + name))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

        try (Stream<Path> stream = Files.list(physical)) {
            return stream.map(p -> new VfsNode(
                    p.getFileName().toString(),
                    Files.isDirectory(p),
                    toVirtual(p)
            )).collect(Collectors.toList());
        } catch (IOException e) {
            Kashub.LOGGER.error("Failed to list VFS directory: " + virtualPath, e);
            return new ArrayList<>();
        }
    }

    /**
     * Reads file content from a virtual path.
     * Dispatches to mounts or physical disk.
     */
    public byte[] read(String virtualPath) throws IOException {
        String absoluteVirtual = toAbsoluteVirtual(virtualPath, "/");
        for (Map.Entry<String, IMount> entry : mounts.entrySet()) {
            if (absoluteVirtual.startsWith(entry.getKey())) {
                String subPath = absoluteVirtual.substring(entry.getKey().length());
                if (subPath.startsWith("/")) subPath = subPath.substring(1);
                return entry.getValue().read(subPath);
            }
        }

        Path physical = resolve(virtualPath, "/");
        if (physical == null || !Files.exists(physical)) throw new IOException("File not found: " + virtualPath);
        return Files.readAllBytes(physical);
    }

    /**
     * Writes file content to a virtual path.
     * Dispatches to mounts or physical disk.
     */
    public void write(String virtualPath, byte[] data) throws IOException {
        String absoluteVirtual = toAbsoluteVirtual(virtualPath, "/");
        for (Map.Entry<String, IMount> entry : mounts.entrySet()) {
            if (absoluteVirtual.startsWith(entry.getKey())) {
                String subPath = absoluteVirtual.substring(entry.getKey().length());
                if (subPath.startsWith("/")) subPath = subPath.substring(1);
                entry.getValue().write(subPath, data);
                return;
            }
        }

        Path physical = resolve(virtualPath, "/");
        if (physical == null) throw new IOException("Access Denied: " + virtualPath);
        Files.createDirectories(physical.getParent());
        Files.write(physical, data);
    }

    /**
     * Deletes a file at the given virtual path.
     * Dispatches to mounts or physical disk.
     */
    public void delete(String virtualPath) throws IOException {
        String absoluteVirtual = toAbsoluteVirtual(virtualPath, "/");
        for (Map.Entry<String, IMount> entry : mounts.entrySet()) {
            if (absoluteVirtual.startsWith(entry.getKey())) {
                String subPath = absoluteVirtual.substring(entry.getKey().length());
                if (subPath.startsWith("/")) subPath = subPath.substring(1);
                entry.getValue().delete(subPath);
                return;
            }
        }

        Path physical = resolve(virtualPath, "/");
        if (physical == null) throw new IOException("Access Denied: " + virtualPath);
        if (Files.exists(physical)) {
            Files.delete(physical);
        }
    }

    public boolean isSystemPath(String virtualPath, String currentDir) {
        Path physical = resolve(virtualPath, currentDir);
        return physical != null && physical.startsWith(SYSTEM_DIR);
    }

    public static class VfsNode {
        public final String name;
        public final boolean isDirectory;
        public final String virtualPath;

        public VfsNode(String name, boolean isDirectory, String virtualPath) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.virtualPath = virtualPath;
        }
    }

    public Path getBinDir() {
        return BIN_DIR;
    }

    public Path getUserHome() {
        return USER_HOME;
    }

    /**
     * Interface for external VFS mounts.
     */
    public interface IMount {
        String getName();
        List<VfsNode> list(String path);
        byte[] read(String path) throws IOException;
        void write(String path, byte[] data) throws IOException;
        boolean exists(String path);
        boolean isDirectory(String path);
        void delete(String path) throws IOException;
        void mkdir(String path) throws IOException;
    }
}

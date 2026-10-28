package kasperstudios.kashub.client;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.services.integration.CCTBridge;
import kasperstudios.kashub.services.AccessPolicy;
import kasperstudios.kashub.util.VirtualFileSystem;
import com.kasperstudios.khwrapper.KHWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Client-side bridge for CC:T integration.
 * Handles scanning for computers and local VFS mounts.
 */
public class CCTClientBridge {
    private static final CCTClientBridge INSTANCE = new CCTClientBridge();
    
    private final Map<Integer, List<String>> cachedFiles = new ConcurrentHashMap<>();
    private final Map<Integer, String> pendingReads = new ConcurrentHashMap<>(); // computerId -> filename
    private final Map<String, byte[]> readResults = new ConcurrentHashMap<>(); // filename -> content
    
    private final Map<Integer, ComputerSession> activeSessions = new ConcurrentHashMap<>();

    public static CCTClientBridge getInstance() {
        return INSTANCE;
    }

    private static final long SCAN_CACHE_TTL_MS = 2000;
    private List<CCTBridge.DiscoveredComputer> cachedScanResult = new ArrayList<>();
    private long lastScanTime = 0;

    public List<CCTBridge.DiscoveredComputer> scan() {
        long now = System.currentTimeMillis();
        if (now - lastScanTime < SCAN_CACHE_TTL_MS && !cachedScanResult.isEmpty()) {
            return cachedScanResult;
        }

        List<CCTBridge.DiscoveredComputer> found = new ArrayList<>();
        
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            cachedScanResult = found;
            lastScanTime = now;
            return found;
        }

        double reach = AccessPolicy.getInstance().getEffectiveReach(client.player);
        int radius = (int) Math.ceil(reach);
        
        if (client.player.isCreative()) {
            radius = 32;
        }

        BlockPos playerPos = client.player.blockPosition();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    
                    if (!client.player.isCreative() && playerPos.distSqr(pos) > reach * reach) continue;

                    BlockEntity be = client.level.getBlockEntity(pos);
                    if (be != null && isComputer(be)) {
                        int id = getComputerId(be);
                        String label = getComputerLabel(be);
                        found.add(new CCTBridge.DiscoveredComputer(id, label, pos));
                    }
                }
            }
        }

        cachedScanResult = found;
        lastScanTime = now;
        return found;
    }

    private boolean isComputer(BlockEntity be) {
        String className = be.getClass().getName();
        return className.contains("computercraft") && className.contains("Computer");
    }

    private int getComputerId(BlockEntity be) {
        if (be == null) return -1;
        try {
            Object id = kasperstudios.kashub.services.integration.ModBridgeManager.getInstance().callModMethod(be, "getComputerID");
            if (id == null) {
                id = kasperstudios.kashub.services.integration.ModBridgeManager.getInstance().callModMethod(be, "getComputerId");
            }
            return id instanceof Integer ? (Integer) id : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String getComputerLabel(BlockEntity be) {
        if (be == null) return "Computer";
        try {
            Object label = kasperstudios.kashub.services.integration.ModBridgeManager.getInstance().callModMethod(be, "getLabel");
            return label instanceof String ? (String) label : "Computer";
        } catch (Exception e) {
            return "Computer";
        }
    }

    public boolean connect(int computerId, BlockPos pos) {
        // Clear all other sessions to ensure one active link
        for (Integer id : new ArrayList<>(activeSessions.keySet())) {
            if (id != computerId) disconnect(id);
        }

        if (!AccessPolicy.getInstance().canInteractWith(Minecraft.getInstance().player, pos)) {
            Kashub.LOGGER.error("Access Denied: Too far from Computer #{}", computerId);
            return false;
        }

        if (!activeSessions.containsKey(computerId)) {
            ComputerSession session = new ComputerSession(computerId, pos);
            activeSessions.put(computerId, session);
            VirtualFileSystem.getInstance().mount("/mnt/cc/" + computerId, session.getMount());
            Kashub.LOGGER.info("CCTClientBridge: Local mount established for Computer #{}", computerId);
        }
        
        // Notify server to establish terminal link (Always send to ensure sync)
        KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.ComputerLinkPayload(computerId, pos, true));
        
        return true;
    }

    public int getActiveSessionId() {
        return activeSessions.keySet().stream().findFirst().orElse(-1);
    }

    public void disconnect(int computerId) {
        ComputerSession session = activeSessions.remove(computerId);
        if (session != null) {
            VirtualFileSystem.getInstance().unmount("/mnt/cc/" + computerId);
            Kashub.LOGGER.info("CCTClientBridge: Local mount removed for Computer #{}", computerId);
            
            // Notify server to terminate terminal link
            KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.ComputerLinkPayload(computerId, session.pos, false));
        }
    }

    public void onPeripheralScanResultReceived(String type, List<String> methods) {
        Kashub.LOGGER.info("CCTClientBridge: Received Scan Result: {} ({} methods)", type, methods.size());
        kasperstudios.kashub.services.integration.ScannedPeripheralManager.getInstance().add(type, methods);
        
        // Use KHWrapper to safely set screen (PeripheralScanScreen is a client-only class)
        KHWrapper.get().setScreen(new kasperstudios.kashub.gui.screen.PeripheralScanScreen(type, methods, null));
    }

    public void onFileListReceived(int computerId, List<String> filenames) {
        cachedFiles.put(computerId, filenames);
    }

    private final java.util.Map<String, java.util.function.Consumer<byte[]>> readCallbacks = new ConcurrentHashMap<>();

    public void readFileAsync(int computerId, String path, java.util.function.Consumer<byte[]> callback) {
        readCallbacks.put(path, callback);
        KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.CCFileReadRequestPayload(computerId, path));
    }

    public void onFileContentReceived(int computerId, String filename, String content, boolean success) {
        byte[] data = (success && content != null) ? content.getBytes(java.nio.charset.StandardCharsets.UTF_8) : null;
        
        java.util.function.Consumer<byte[]> cb = readCallbacks.remove(filename);
        if (cb != null) {
            cb.accept(data);
        } else if (data != null) {
            readResults.put(filename, data);
        } else {
            // Mark as not found to unblock sync reads
            readResults.put(filename, new byte[0]); 
        }
    }

    private static class ComputerSession {
        private final int id;
        private final BlockPos pos;
        private final VirtualFileSystem.IMount mount;

        public ComputerSession(int id, BlockPos pos) {
            this.id = id;
            this.pos = pos;
            this.mount = new CCTMount(id);
        }

        public VirtualFileSystem.IMount getMount() {
            return mount;
        }
    }

    private static class CCTMount implements VirtualFileSystem.IMount {
        private final int computerId;

        public CCTMount(int computerId) {
            this.computerId = computerId;
        }

        @Override public String getName() { return "CC#" + computerId; }

        @Override
        public List<VirtualFileSystem.VfsNode> list(String path) {
            List<String> files = CCTClientBridge.getInstance().cachedFiles.getOrDefault(computerId, new ArrayList<>());
            return files.stream()
                .map(f -> new VirtualFileSystem.VfsNode(f, false, "/mnt/cc/" + computerId + "/" + f))
                .collect(Collectors.toList());
        }

        @Override
        public byte[] read(String path) throws IOException {
            byte[] cached = CCTClientBridge.getInstance().readResults.remove(path);
            if (cached != null) {
                if (cached.length == 0) throw new IOException("File not found or empty: " + path);
                return cached;
            }

            if (Minecraft.getInstance().isSameThread()) {
                Kashub.LOGGER.warn("CCTMount.read() called from render thread — use readFileAsync() for CC file reads");
                throw new IOException("CC file read must be async from render thread. Use readFileAsync().");
            }

            KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.CCFileReadRequestPayload(computerId, path));

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 2000) {
                byte[] data = CCTClientBridge.getInstance().readResults.remove(path);
                if (data != null) {
                    if (data.length == 0) throw new IOException("File not found or empty: " + path);
                    return data;
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            throw new IOException("Read timeout for " + path + " (Computer #" + computerId + ")");
        }

        @Override
        public void write(String path, byte[] data) throws IOException {
            String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.CCScriptPushPayload(computerId, path, content));
        }

        @Override public boolean exists(String path) { return true; } // Simplified
        @Override public boolean isDirectory(String path) { return false; } // Simplified
        @Override
        public void delete(String path) throws IOException {
            KHWrapper.sendPacket(new kasperstudios.kashub.network.payload.NetworkingPayloads.CCScriptPushPayload(computerId, path, "__DELETE__"));
        }
        @Override public void mkdir(String path) throws IOException {}
    }
}

package kasperstudios.kashub.services.integration;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.util.ScriptLogger;
import kasperstudios.kashub.util.VirtualFileSystem;
import com.kasperstudios.khwrapper.KHWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ComputerCraft (CC:T) Bridge Service.
 * Implements Phase 1 Concept: Terminal-level integration with CC:T computers.
 */
public class CCTBridge {

    private static final String CCT_MOD_ID = "computercraft";
    private static volatile CCTBridge instance;
    private static final Object LOCK = new Object();

    private final Map<Integer, List<String>>        cachedFiles     = new ConcurrentHashMap<>();
    // Cache for peripheral methods: key = "blockId:className" or "peripheralType"
    private final Map<String, List<String>>         cachedMethods   = new ConcurrentHashMap<>();
    private boolean isCctLoaded = false;

    private CCTBridge() {
        this.isCctLoaded = ModBridgeManager.getInstance().isModLoaded(CCT_MOD_ID);
        if (isCctLoaded) {
            Kashub.LOGGER.info("CCTBridge: ComputerCraft detected, enabling integration.");
        }
    }

    public static CCTBridge getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new CCTBridge();
                }
            }
        }
        return instance;
    }

    public boolean isAvailable() {
        return isCctLoaded;
    }

    public List<DiscoveredComputer> scan() {
        if (KHWrapper.get().isClientSide()) {
            return kasperstudios.kashub.client.CCTClientBridge.getInstance().scan();
        }
        return new ArrayList<>();
    }

    public boolean connect(int computerId, BlockPos pos) {
        if (KHWrapper.get().isClientSide()) {
            return kasperstudios.kashub.client.CCTClientBridge.getInstance().connect(computerId, pos);
        }
        return false;
    }

    public void disconnect(int computerId) {
        if (KHWrapper.get().isClientSide()) {
            kasperstudios.kashub.client.CCTClientBridge.getInstance().disconnect(computerId);
        }
    }

    public static class DiscoveredComputer {
        public final int id;
        public final String label;
        public final BlockPos pos;

        public DiscoveredComputer(int id, String label, BlockPos pos) {
            this.id = id;
            this.label = label;
            this.pos = pos;
        }
    }

    // ComputerSession and CCTMount logic simplified for Side-Safety.
    private final Map<UUID, DiscoveredComputer> activePlayerLinks = new java.util.concurrent.ConcurrentHashMap<>();

    public void establishLink(ServerPlayer player, int computerId, BlockPos pos) {
        Kashub.LOGGER.info("CCTBridge: Establishing Neural Link for {} with Computer #{} at {}", player.getScoreboardName(), computerId, pos);
        DiscoveredComputer computer = new DiscoveredComputer(computerId, null, pos);
        activePlayerLinks.put(player.getUUID(), computer);
        
        player.displayClientMessage(Component.literal("§bNeural Link established with Computer #" + computerId), true);
        
        // Notify client to update UI context
        KHWrapper.get().sendPacketToPlayer(player, new kasperstudios.kashub.network.payload.NetworkingPayloads.InjectionStatusPayload(true, "CONNECTED:" + computerId));

        // Send initial file list so the client's FilePanel can populate immediately
        sendFileList(player, computerId);
    }

    public void terminateLink(ServerPlayer player) {
        DiscoveredComputer removed = activePlayerLinks.remove(player.getUUID());
        if (removed != null) {
            Kashub.LOGGER.info("CCTBridge: Terminating Neural Link for {}", player.getScoreboardName());
            player.displayClientMessage(Component.literal("§eNeural Link terminated."), true);
            KHWrapper.get().sendPacketToPlayer(player, new kasperstudios.kashub.network.payload.NetworkingPayloads.InjectionStatusPayload(true, "DISCONNECTED"));
        }
    }

    // ─── Server-side handlers ───────────────────────────────────────────────────

    /**
     * Called on the server when the client pushes a file to a CC computer disk.
     * Writes the file via NIO, then sends the updated file list back to the client.
     */
    public void handleFilePush(net.minecraft.server.level.ServerPlayer player,
                               int computerId, String filename, String content) {
        DiscoveredComputer session = getActiveSession(player);
        if (session == null || session.id != computerId) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cNo active link to CC#" + computerId), false);
            return;
        }

        try {
            Path ccRoot = player.server.getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("computercraft/computer/" + computerId);
            Files.createDirectories(ccRoot);

            // Clean relative path, prevent escaping root
            String cleanRel = filename.replace("\\", "/").replaceAll("^/+", "");
            if (cleanRel.startsWith("mnt/cc/" + computerId + "/")) {
                cleanRel = cleanRel.substring(("mnt/cc/" + computerId + "/").length());
            }
            Path targetFile = ccRoot.resolve(cleanRel).normalize();
            if (!targetFile.startsWith(ccRoot)) {
                player.displayClientMessage(Component.literal("§cInvalid file path (escapes CC root)"), false);
                return;
            }

            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }

            if ("__DELETE__".equals(content)) {
                if (Files.exists(targetFile)) {
                    Files.delete(targetFile);
                    Kashub.LOGGER.info("CCTBridge: deleted {} on CC#{}", cleanRel, computerId);
                }
            } else {
                Files.writeString(targetFile, content, java.nio.charset.StandardCharsets.UTF_8);
                Kashub.LOGGER.info("CCTBridge: wrote {} to CC#{}", cleanRel, computerId);
            }

            // Notify client with fresh file list
            sendFileList(player, computerId);
        } catch (IOException e) {
            Kashub.LOGGER.error("CCTBridge: failed to write CC file", e);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§cFailed to write file: " + e.getMessage()), false);
        }
    }

    public Object findServerComputer(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be == null) return null;
        Object serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getServerComputer");
        if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getComputer");
        if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getComputerEntity");
        if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().getModField(be, "computer");
        if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().getModField(be, "serverComputer");
        return serverComputer;
    }

    /**
     * Called on the server when the client sends a terminal input line.
     */
    public void handleTerminalInput(net.minecraft.server.level.ServerPlayer player, 
                                     int computerId, String input) {
        if (input == null) return;
        String cmd = input.trim();
        if (cmd.isEmpty()) return;

        // Alias 'run' to direct execution for user convenience
        if (cmd.startsWith("run ")) {
            cmd = cmd.substring(4).trim();
        }

        DiscoveredComputer session = getActiveSession(player);
        if (session == null || session.id != computerId) return;

        // Proxy input to CC:T computer
        try {
            net.minecraft.world.level.Level level = player.level();
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(session.pos);
            if (be == null) return;

            // Use robust cascade lookup for computer instance
            Object serverComputer = findServerComputer(be);
            if (serverComputer == null) return;

            // Ensure computer is turned on
            try {
                Object isOn = ModBridgeManager.getInstance().callModMethod(serverComputer, "isOn");
                if (Boolean.FALSE.equals(isOn)) {
                    ModBridgeManager.getInstance().callModMethod(serverComputer, "turnOn");
                }
            } catch (Exception ignored) {
                Kashub.LOGGER.debug("Failed to check/start CC computer: {}", ignored.getMessage());
            }

            if (cmd.equals("\u0003")) {
                // Ctrl+C -> terminate
                ModBridgeManager.getInstance().callModMethod(serverComputer, "queueEvent", "terminate", new Object[0]);
                return;
            }

            // Proxy as a 'paste' event followed by 'key' Enter
            ModBridgeManager.getInstance().callModMethod(serverComputer, "queueEvent", "paste", new Object[]{cmd});
            
            // 257 is Enter. Send key down and key up events.
            ModBridgeManager.getInstance().callModMethod(serverComputer, "queueEvent", "key", new Object[]{257, false});
            ModBridgeManager.getInstance().callModMethod(serverComputer, "queueEvent", "key_up", new Object[]{257});

        } catch (Exception e) {
            Kashub.LOGGER.error("CCTBridge: failed to proxy terminal input to CC#{}", computerId, e);
        }
    }

    private final Map<UUID, Integer> lastCursorX = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastCursorY = new ConcurrentHashMap<>();
    private final Map<UUID, String>  lastLineText = new ConcurrentHashMap<>();

    /**
     * Polled every server tick to capture terminal output and proxy it back to linked players.
     */
    public void tick() {
        if (!isCctLoaded) return;
        
        if (Kashub.DEBUG_MODE && !activePlayerLinks.isEmpty()) {
            Kashub.LOGGER.info("CCTBridge: Tick running, {} active links", activePlayerLinks.size());
        }
        
        if (activePlayerLinks.isEmpty()) return;

        for (Map.Entry<UUID, DiscoveredComputer> entry : activePlayerLinks.entrySet()) {
            UUID playerUuid = entry.getKey();
            DiscoveredComputer session = entry.getValue();
            
            net.minecraft.server.level.ServerPlayer player = com.kasperstudios.khwrapper.KHWrapper.get().getServer().getPlayerList().getPlayer(playerUuid);
            if (player == null) continue;

            try {
                net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(session.pos);
                if (be == null) continue;

                Object serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getServerComputer");
                if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getComputer");
                if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().callModMethod(be, "getComputerEntity");
                
                // Field fallback
                if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().getModField(be, "computer");
                if (serverComputer == null) serverComputer = ModBridgeManager.getInstance().getModField(be, "serverComputer");

                if (serverComputer == null) {
                    if (Kashub.DEBUG_MODE) {
                        Kashub.LOGGER.warn("CCTBridge: Could not find computer instance on BlockEntity at {}. Methods: {}, Fields: {}", 
                            session.pos, 
                            java.util.Arrays.toString(be.getClass().getDeclaredMethods()),
                            java.util.Arrays.toString(be.getClass().getDeclaredFields()));
                    }
                    continue;
                }

                Object terminal = ModBridgeManager.getInstance().callModMethod(serverComputer, "getTerminal");
                if (terminal == null) {
                    Object executor = ModBridgeManager.getInstance().callModMethod(serverComputer, "getComputer");
                    if (executor != null) terminal = ModBridgeManager.getInstance().callModMethod(executor, "getTerminal");
                }
                
                // Field fallback for terminal
                if (terminal == null) terminal = ModBridgeManager.getInstance().getModField(serverComputer, "terminal");
                if (terminal == null) terminal = ModBridgeManager.getInstance().getModField(serverComputer, "m_terminal");

                if (terminal == null) {
                    if (Kashub.DEBUG_MODE) {
                        Kashub.LOGGER.warn("CCTBridge: Could not find terminal on computer {}. Methods: {}, Fields: {}", 
                            serverComputer, 
                            java.util.Arrays.toString(serverComputer.getClass().getDeclaredMethods()),
                            java.util.Arrays.toString(serverComputer.getClass().getDeclaredFields()));
                    }
                    continue;
                }

                Object ox = ModBridgeManager.getInstance().callModMethod(terminal, "getCursorX");
                Object oy = ModBridgeManager.getInstance().callModMethod(terminal, "getCursorY");
                if (ox == null || oy == null) continue;

                int cx = (int) ox;
                int cy = (int) oy;
                
                int lx = lastCursorX.getOrDefault(playerUuid, 0);
                int ly = lastCursorY.getOrDefault(playerUuid, 0);
                String lt = lastLineText.getOrDefault(playerUuid, "");

                // Get current line text for comparison
                Object currentBuffer = ModBridgeManager.getInstance().callModMethod(terminal, "getLine", cy);
                String currentLineText = extractLineText(currentBuffer);

                // Case 1: Cursor moved or line changed
                if (cy != ly || cx != lx || !currentLineText.equals(lt)) {
                    if (cy < ly) {
                        // Potential clear screen or significant scroll up - just sync to current position
                        if (cx > 0 && cx <= currentLineText.length()) {
                            sendTerminalPacket(player, session.id, currentLineText.substring(0, cx));
                        }
                    } else {
                        // Normal forward progress (same line or new lines)
                        StringBuilder sb = new StringBuilder();
                        
                        // Handle the case where content changed but cursor stayed same (e.g. scrolling)
                        if (cy == ly && cx == lx && !currentLineText.equals(lt)) {
                            // Something changed on this line without cursor moving. 
                            // Usually this is an overwrite or a scroll-up.
                            if (cx > 0 && cx <= currentLineText.length()) {
                                sb.append(currentLineText.substring(0, cx));
                            }
                        } else {
                            for (int y = ly; y <= cy; y++) {
                                Object lineBuffer = ModBridgeManager.getInstance().callModMethod(terminal, "getLine", y);
                                String lineText = extractLineText(lineBuffer);
                                
                                if (y == ly) {
                                    if (cy == ly) {
                                        // Same line, just more text
                                        if (cx > lx && cx <= lineText.length()) {
                                            sb.append(lineText.substring(lx, cx));
                                        } else if (cx < lx) {
                                            // Overwrite/Jump back - send from start
                                            if (cx > 0 && cx <= lineText.length()) {
                                                sb.append("\n").append(lineText.substring(0, cx));
                                            }
                                        }
                                    } else {
                                        // Multiple lines, start with the rest of the current line
                                        if (lx < lineText.length()) {
                                            String remainder = lineText.substring(lx).stripTrailing();
                                            sb.append(remainder);
                                        }
                                        sb.append("\n");
                                    }
                                } else if (y < cy) {
                                    // Intermediate full lines - strip trailing to avoid empty-looking terminal
                                    sb.append(lineText.stripTrailing()).append("\n");
                                } else {
                                    // Last line, up to cursor
                                    if (cx > 0 && cx <= lineText.length()) {
                                        sb.append(lineText.substring(0, cx));
                                    }
                                }
                            }
                        }
                        
                        String toSend = sb.toString();
                        if (!toSend.isEmpty()) {
                            if (Kashub.DEBUG_MODE) Kashub.LOGGER.info("CCTBridge: Sending terminal output to {}: '{}'", player.getScoreboardName(), toSend.replace("\n", "\\n"));
                            sendTerminalPacket(player, session.id, toSend);
                        }
                    }

                    // Update state
                    lastCursorX.put(playerUuid, cx);
                    lastCursorY.put(playerUuid, cy);
                    lastLineText.put(playerUuid, currentLineText);
                }

            } catch (Exception e) {
                if (Kashub.DEBUG_MODE) {
                    Kashub.LOGGER.error("CCTBridge: Error in tick loop for player " + player.getScoreboardName(), e);
                }
            }
        }
    }

    private String extractLineText(Object textBuffer) {
        if (textBuffer == null) return "";
        Object textObj = ModBridgeManager.getInstance().callModMethod(textBuffer, "getText");
        String text;
        if (textObj instanceof String) text = (String) textObj;
        else if (textObj instanceof char[]) text = new String((char[]) textObj);
        else if (textObj != null) text = textObj.toString();
        else text = textBuffer.toString();
        // Do NOT strip trailing spaces, they might be part of a prompt waiting for input
        return text;
    }

    private void sendTerminalPacket(net.minecraft.server.level.ServerPlayer player, int computerId, String text) {
        KHWrapper.get().sendPacketToPlayer(player, 
            new kasperstudios.kashub.network.payload.NetworkingPayloads.CCTerminalOutputPayload(computerId, text));
    }

    /**
     * Called on the server to handle a file read request from a client.
     */
    public void handleFileReadRequest(net.minecraft.server.level.ServerPlayer player, int computerId, String filename) {
        DiscoveredComputer session = getActiveSession(player);
        if (session == null || session.id != computerId) return;

        try {
            Path ccRoot = player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("computercraft/computer/" + computerId);
            
            String cleanRel = filename.replace("\\", "/").replaceAll("^/+", "");
            if (cleanRel.startsWith("mnt/cc/" + computerId + "/")) {
                cleanRel = cleanRel.substring(("mnt/cc/" + computerId + "/").length());
            }
            Path filePath = ccRoot.resolve(cleanRel).normalize();

            if (filePath.startsWith(ccRoot) && Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String content = Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
                KHWrapper.get().sendPacketToPlayer(player,
                        new kasperstudios.kashub.network.payload.NetworkingPayloads.CCFileContentPayload(computerId, filename, content, true));
            } else {
                // File not found, still respond to unblock client
                KHWrapper.get().sendPacketToPlayer(player,
                        new kasperstudios.kashub.network.payload.NetworkingPayloads.CCFileContentPayload(computerId, filename, null, false));
            }
        } catch (IOException e) {
            Kashub.LOGGER.error("CCTBridge: failed to read CC file", e);
            KHWrapper.get().sendPacketToPlayer(player,
                    new kasperstudios.kashub.network.payload.NetworkingPayloads.CCFileContentPayload(computerId, filename, null, false));
        }
    }

    /**
     * Called on the server to scan a block and resolve its CC:T peripheral methods.
     * Uses caching to avoid repeated reflection overhead.
     */
    public void handlePeripheralScanRequest(ServerPlayer player, BlockPos pos) {
        Kashub.LOGGER.info("CCTBridge: Received Peripheral Scan Request from {} at {}", player.getScoreboardName(), pos);
        
        // Enforce T2 requirement unless creative or casual
        int tier = kasperstudios.kashub.services.AccessPolicy.getInstance().getImplantTier(player);
        Kashub.LOGGER.info("CCTBridge: Player {} tier is {}", player.getScoreboardName(), tier);

        if (!kasperstudios.kashub.services.AccessPolicy.getInstance().hasAccess(player) || 
            (!player.isCreative() && !"CASUAL".equalsIgnoreCase(kasperstudios.kashub.config.KashubConfig.getInstance().accessPolicy) && 
             tier < 2)) {
            Kashub.LOGGER.warn("CCTBridge: Access Denied for peripheral scan (Tier: {}, Access: {})", tier, kasperstudios.kashub.services.AccessPolicy.getInstance().hasAccess(player));
            player.displayClientMessage(Component.literal("§c[Scanner] Requires Neuro-Implant Tier 2 for deep scanning."), true);
            return;
        }

        net.minecraft.world.level.Level level = player.level();
        String type = "unknown";
        List<String> methods;

        try {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            
            // Use KHWrapper abstraction for cross-platform peripheral detection
            Object peripheral = KHWrapper.get().getComputerCraftPeripheral(level, pos, null);

            if (peripheral != null) {
                Class<?> peripheralClass = peripheral.getClass();
                type = extractPeripheralType(peripheral, peripheralClass);
                
                // If type is still default or unknown, use blockId
                if (type == null || type.isEmpty() || type.equals("unknown") || type.equals(peripheralClass.getSimpleName())) {
                    type = blockId;
                }

                Kashub.LOGGER.info("CCTBridge: Found peripheral {} at {}: {}", type, pos, peripheralClass.getName());
                
                String cacheKey = type + ":" + peripheralClass.getName();
                List<String> cachedResult = cachedMethods.get(cacheKey);
                if (cachedResult != null) {
                    methods = new ArrayList<>(cachedResult);
                } else {
                    methods = scanPeripheralMethods(peripheral, peripheralClass);
                    List<String> formattedMethods = new ArrayList<>();
                    for (String m : methods) {
                        if (!m.contains("(")) {
                            formattedMethods.add(m + "()");
                        } else {
                            formattedMethods.add(m);
                        }
                    }
                    methods = formattedMethods;
                    cachedMethods.put(cacheKey, new ArrayList<>(methods));
                    ScannedPeripheralManager.getInstance().add(type, methods);
                }
            } else {
                // Fallback for vanilla blocks or ANY block with a BE
                type = blockId;
                List<String> cachedResult = cachedMethods.get(blockId);
                if (cachedResult != null) {
                    methods = new ArrayList<>(cachedResult);
                } else {
                    methods = new ArrayList<>();
                    String vanillaType = resolveVanillaBlockMethods(blockId, methods);
                    if (!vanillaType.equals(blockId)) {
                        type = vanillaType;
                    }
                    
                    // Deep Scan even for non-peripherals if they have a BE
                    net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
                    if (be != null && methods.size() <= 1) { // 1 because resolveVanilla adds a header if nothing found
                        List<String> deepMethods = scanPeripheralMethods(be, be.getClass());
                        for (String m : deepMethods) {
                            if (!methods.contains(m)) methods.add(m);
                        }
                    }

                    List<String> formattedMethods = new ArrayList<>();
                    for (String m : methods) {
                        if (!m.startsWith("§") && !m.contains("(")) {
                            formattedMethods.add(m + "()");
                        } else {
                            formattedMethods.add(m);
                        }
                    }
                    methods = formattedMethods;
                    
                    cachedMethods.put(blockId, new ArrayList<>(methods));
                    ScannedPeripheralManager.getInstance().add(type, methods);
                }
            }
        } catch (Exception e) {
            Kashub.LOGGER.error("CCTBridge: Peripheral scan failed at " + pos, e);
            
            // Enhanced fallback: Show BlockState properties for debugging
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            type = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            
            methods = new ArrayList<>();
            methods.add("§c--- Scan Error ---");
            methods.add("§7" + e.getClass().getSimpleName() + ": " + e.getMessage());
            methods.add("§8--- Block Properties ---");
            final List<String> finalMethods = methods;
            state.getValues().forEach((property, value) -> {
                finalMethods.add("§e" + property.getName() + "§7: §f" + value.toString());
            });
        }

        KHWrapper.get().sendPacketToPlayer(player,
                new kasperstudios.kashub.network.payload.NetworkingPayloads.PeripheralScanResultPayload(type, methods));
    }

    /** Send the current file list of a CC computer to the player (including subfolders). */
    private void sendFileList(net.minecraft.server.level.ServerPlayer player, int computerId) {
        try {
            Path ccRoot = player.server.getWorldPath(
                    net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("computercraft/computer/" + computerId);

            List<String> files = new ArrayList<>();
            if (Files.isDirectory(ccRoot)) {
                try (Stream<Path> stream = Files.walk(ccRoot)) {
                    files = stream
                            .filter(Files::isRegularFile)
                            .map(p -> ccRoot.relativize(p).toString().replace("\\", "/"))
                            .sorted()
                            .collect(Collectors.toList());
                }
            }

            KHWrapper.get().sendPacketToPlayer(player,
                    new kasperstudios.kashub.network.payload.NetworkingPayloads
                            .CCFileListPayload(computerId, files));
        } catch (IOException e) {
            Kashub.LOGGER.error("CCTBridge: failed to list CC files", e);
        }
    }

    // Client-side cache handlers moved to CCTClientBridge.

    public DiscoveredComputer getActiveSession(ServerPlayer player) {
        return activePlayerLinks.get(player.getUUID());
    }

    public boolean isConnected(ServerPlayer player) {
        return activePlayerLinks.containsKey(player.getUUID());
    }

    // ─── Peripheral Method Extraction Helpers ───────────────────────────────────

    /**
     * Extracts the peripheral type from a CC:T peripheral object.
     * Handles both single type (String) and multiple types (String[]) returns.
     */
    private String extractPeripheralType(Object peripheral, Class<?> peripheralClass) {
        try {
            java.lang.reflect.Method getTypeMethod = peripheralClass.getMethod("getType");
            Object result = getTypeMethod.invoke(peripheral);
            
            if (result instanceof String str) {
                return str;
            } else if (result instanceof String[] types && types.length > 0) {
                // CC:T peripherals can have multiple types - return the primary one
                return types[0];
            }
        } catch (Exception e) {
            Kashub.LOGGER.debug("CCTBridge: Could not extract type via getType(), using class name");
        }
        return peripheralClass.getSimpleName();
    }

    /**
     * Scans a peripheral object for Lua-callable methods using multiple strategies.
     * 1. IDynamicPeripheral.getMethodNames() (legacy)
     * 2. @LuaFunction annotations (modern CC:T 1.20+)
     * 3. Fallback to public methods
     */
    private List<String> scanPeripheralMethods(Object peripheral, Class<?> peripheralClass) {
        List<String> methods = new ArrayList<>();
        Kashub.LOGGER.info("CCTBridge: Scanning methods for peripheral class: {}", peripheralClass.getName());

        // Strategy 1: Try IPeripheral.getMethodNames() or IDynamicPeripheral.getMethodNames()
        try {
            // Scan all methods including inherited ones for getMethodNames
            java.lang.reflect.Method getMethodNamesMethod = null;
            try {
                getMethodNamesMethod = peripheralClass.getMethod("getMethodNames");
            } catch (NoSuchMethodException e) {
                // Try to find it in any interface (some mods use proxy objects)
                for (Class<?> iface : peripheralClass.getInterfaces()) {
                    try {
                        getMethodNamesMethod = iface.getMethod("getMethodNames");
                        break;
                    } catch (NoSuchMethodException ignored) {
                        Kashub.LOGGER.debug("getMethodNames not found on interface {}: {}", iface.getName(), ignored.getMessage());
                    }
                }
            }

            if (getMethodNamesMethod != null) {
                Object result = getMethodNamesMethod.invoke(peripheral);
                if (result instanceof String[] methodArray) {
                    methods.addAll(java.util.Arrays.asList(methodArray));
                    Kashub.LOGGER.info("CCTBridge: Found {} methods via getMethodNames()", methods.size());
                    return methods;
                }
            }
        } catch (Exception e) {
            Kashub.LOGGER.debug("CCTBridge: getMethodNames() scan failed: {}", e.getMessage());
        }

        // Strategy 2: Scan for @LuaFunction annotations (modern CC:T 1.20+)
        methods = scanLuaFunctionAnnotations(peripheralClass);
        if (!methods.isEmpty()) {
            Kashub.LOGGER.info("CCTBridge: Found {} methods via @LuaFunction scan", methods.size());
            return methods;
        }

        // Strategy 3: Exhaustive interface scan (for Create and complex mods)
        for (Class<?> iface : peripheralClass.getInterfaces()) {
            List<String> ifaceMethods = scanLuaFunctionAnnotations(iface);
            for (String m : ifaceMethods) {
                if (!methods.contains(m)) methods.add(m);
            }
        }
        
        if (!methods.isEmpty()) {
            Kashub.LOGGER.info("CCTBridge: Found {} methods via exhaustive interface scan", methods.size());
            return methods;
        }

        // Strategy 4: Fallback to all members (DEEP SCAN)
        Kashub.LOGGER.warn("CCTBridge: Deep scanning all members for class: {}", peripheralClass.getName());
        
        Class<?> current = peripheralClass;
        while (current != null && !current.equals(Object.class)) {
            // Scan methods (including non-public)
            for (java.lang.reflect.Method m : current.getDeclaredMethods()) {
                try {
                    m.setAccessible(true);
                    String name = m.getName();
                    // Filter out very common/internal methods to keep it readable
                    if (name.contains("$") || name.equals("wait") || name.equals("notify") || name.equals("toString")) continue;
                    
                    String displayName = name;
                    if (m.getParameterCount() > 0) {
                        displayName += "(" + m.getParameterCount() + ")";
                    }
                    if (!methods.contains(displayName)) {
                        methods.add(displayName);
                        Kashub.LOGGER.debug("CCTBridge: Found method: {}", displayName);
                    }
                } catch (Exception ignored) {
                    Kashub.LOGGER.debug("CCTBridge: Could not access method in deep scan: {}", ignored.getMessage());
                }
            }

            // Scan fields (Properties / State)
            for (java.lang.reflect.Field f : current.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    String fieldName = "§7" + f.getName() + " §8(val)";
                    if (!methods.contains(fieldName)) {
                        methods.add(fieldName);
                        Kashub.LOGGER.debug("CCTBridge: Found field: {}", f.getName());
                    }
                } catch (Exception ignored) {
                    Kashub.LOGGER.debug("CCTBridge: Could not access field in deep scan: {}", ignored.getMessage());
                }
            }
            current = current.getSuperclass();
        }

        if (methods.isEmpty()) {
            Kashub.LOGGER.error("CCTBridge: TOTAL FAILURE. No members found for class {}", peripheralClass.getName());
        } else {
            Kashub.LOGGER.info("CCTBridge: Deep scan found {} total members", methods.size());
        }
        
        return methods;
    }

    /**
     * Scans a class hierarchy for @LuaFunction annotations.
     */
    private List<String> scanLuaFunctionAnnotations(Class<?> startClass) {
        List<String> methods = new ArrayList<>();
        Class<?> currentClass = startClass;
        
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (java.lang.reflect.Method m : currentClass.getDeclaredMethods()) {
                if (m.isSynthetic() || m.isBridge()) continue;
                
                for (java.lang.annotation.Annotation anno : m.getAnnotations()) {
                    String annoName = anno.annotationType().getName();
                    if (annoName.equals("dan200.computercraft.api.lua.LuaFunction")) {
                        String methodName = extractLuaFunctionName(anno, m);
                        if (!methods.contains(methodName)) {
                            methods.add(methodName);
                        }
                        break;
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return methods;
    }

    /**
     * Extracts the Lua function name from a @LuaFunction annotation.
     * If the annotation has a custom value, uses that; otherwise uses the method name.
     */
    private String extractLuaFunctionName(java.lang.annotation.Annotation anno, java.lang.reflect.Method method) {
        try {
            java.lang.reflect.Method valueMethod = anno.annotationType().getMethod("value");
            String[] customNames = (String[]) valueMethod.invoke(anno);
            if (customNames != null && customNames.length > 0 && !customNames[0].isEmpty()) {
                return customNames[0];
            }
        } catch (Exception ignored) {
            // Use method name as fallback
        }
        return method.getName();
    }

    /**
     * Resolves methods for vanilla blocks that CC:T wraps via GenericSource system.
     */
    private String resolveVanillaBlockMethods(String blockId, List<String> methods) {
        // CC:T GenericSource provides standard inventory methods for many blocks
        if (blockId.contains("chest") || blockId.contains("barrel") || blockId.contains("hopper") || 
            blockId.contains("shulker") || blockId.contains("furnace") || blockId.contains("blast_furnace") ||
            blockId.contains("smoker") || blockId.contains("brewing_stand") || blockId.contains("dropper") || 
            blockId.contains("dispenser")) {
            methods.addAll(java.util.Arrays.asList("size", "list", "getItemDetail", "pushItems", "pullItems"));
            return "inventory";
        }
        
        if (blockId.contains("note_block")) {
            methods.addAll(java.util.Arrays.asList("playNote"));
            return "note_block";
        }
        
        if (blockId.contains("jukebox")) {
            methods.addAll(java.util.Arrays.asList("playDisc", "stopDisc", "ejectDisc"));
            return "jukebox";
        }
        
        if (blockId.contains("lectern")) {
            methods.addAll(java.util.Arrays.asList("getPage", "setPage", "getPageCount"));
            return "lectern";
        }
        
        // Beacon, structure block, etc. could be added here
        
        methods.add("§8--- Not a CC:T Peripheral ---");
        return blockId;
    }

    /**
     * Clears the peripheral methods cache. Call this when mod configurations change.
     */
    public void clearMethodCache() {
        cachedMethods.clear();
        Kashub.LOGGER.debug("CCTBridge: Peripheral methods cache cleared");
    }
}

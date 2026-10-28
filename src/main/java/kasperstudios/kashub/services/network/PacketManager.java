package kasperstudios.kashub.services.network;

import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.network.payload.NetworkingPayloads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * PacketManager - Network packet handling for scripts.
 * 
 * Allows scripts to send/receive custom packets for multiplayer communication.
 * 
 * @since 0.9.0
 */
public class PacketManager {
    
    private static volatile PacketManager instance;
    private static final Object LOCK = new Object();
    
    private final Map<String, Consumer<byte[]>> packetHandlers = new ConcurrentHashMap<>();
    private boolean initialized = false;

    private PacketManager() {}
    
    public static PacketManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PacketManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize packet handling.
     */
    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        // Unified registration for both directions
        KHWrapper.register(com.kasperstudios.khwrapper.NetworkRegistration.bidirectional(
            NetworkingPayloads.ScriptPacketPayload.ID,
            NetworkingPayloads.ScriptPacketPayload::read,
            (packet, context) -> {
                NetworkingPayloads.ScriptPacketPayload payload = (NetworkingPayloads.ScriptPacketPayload) packet;
                String channel = payload.channel();
                byte[] data = payload.data();
                
                Consumer<byte[]> handler = packetHandlers.get(channel);
                if (handler != null) {
                    context.execute(() -> {
                        try {
                            handler.accept(data);
                        } catch (Exception e) {
                            Kashub.LOGGER.error("Error handling packet on channel " + channel, e);
                        }
                    });
                }
            }
        ));
        
        Kashub.LOGGER.info("PacketManager initialized");
    }
    
    /**
     * Send packet to server.
     */
    public void sendPacket(String channel, byte[] data) {
        NetworkingPayloads.ScriptPacketPayload packet = new NetworkingPayloads.ScriptPacketPayload(channel, data);
        KHWrapper.sendPacket(packet);
    }
    
    /**
     * Send string packet to server.
     */
    public void sendPacket(String channel, String message) {
        sendPacket(channel, message.getBytes());
    }
    
    /**
     * Register packet handler for channel.
     */
    public void registerHandler(String channel, Consumer<byte[]> handler) {
        packetHandlers.put(channel, handler);
    }
    
    /**
     * Unregister packet handler.
     */
    public void unregisterHandler(String channel) {
        packetHandlers.remove(channel);
    }
    
    /**
     * Clear all handlers.
     */
    public void clearHandlers() {
        packetHandlers.clear();
    }
}

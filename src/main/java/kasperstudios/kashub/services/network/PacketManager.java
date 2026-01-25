package kasperstudios.kashub.services.network;

import kasperstudios.kashub.Kashub;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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
    public void initialize() {
        // Register custom payload type
        PayloadTypeRegistry.playC2S().register(ScriptPacket.ID, ScriptPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ScriptPacket.ID, ScriptPacket.CODEC);
        
        // Register packet receiver
        ClientPlayNetworking.registerGlobalReceiver(ScriptPacket.ID, (payload, context) -> {
            String channel = payload.channel();
            byte[] data = payload.data();
            
            Consumer<byte[]> handler = packetHandlers.get(channel);
            if (handler != null) {
                context.client().execute(() -> {
                    try {
                        handler.accept(data);
                    } catch (Exception e) {
                        Kashub.LOGGER.error("Error handling packet on channel " + channel, e);
                    }
                });
            }
        });
        
        Kashub.LOGGER.info("PacketManager initialized");
    }
    
    /**
     * Send packet to server.
     */
    public void sendPacket(String channel, byte[] data) {
        if (!ClientPlayNetworking.canSend(ScriptPacket.ID)) {
            Kashub.LOGGER.warn("Cannot send packet - not connected to server");
            return;
        }
        
        ScriptPacket packet = new ScriptPacket(channel, data);
        ClientPlayNetworking.send(packet);
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
    
    /**
     * Custom packet payload for script communication.
     */
    public record ScriptPacket(String channel, byte[] data) implements CustomPayload {
        
        public static final CustomPayload.Id<ScriptPacket> ID = 
            new CustomPayload.Id<>(Identifier.of("kashub", "script_packet"));
        
        public static final PacketCodec<RegistryByteBuf, ScriptPacket> CODEC = 
            new PacketCodec<RegistryByteBuf, ScriptPacket>() {
                @Override
                public ScriptPacket decode(RegistryByteBuf buf) {
                    return read(buf);
                }
                
                @Override
                public void encode(RegistryByteBuf buf, ScriptPacket packet) {
                    write(buf, packet);
                }
            };
        
        private static void write(RegistryByteBuf buf, ScriptPacket packet) {
            buf.writeString(packet.channel);
            buf.writeByteArray(packet.data);
        }
        
        private static ScriptPacket read(RegistryByteBuf buf) {
            String channel = buf.readString();
            byte[] data = buf.readByteArray();
            return new ScriptPacket(channel, data);
        }
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

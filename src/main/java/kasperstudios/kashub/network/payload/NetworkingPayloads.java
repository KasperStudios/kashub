package kasperstudios.kashub.network.payload;

import kasperstudios.kashub.network.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public class NetworkingPayloads {

    public record HandshakePayload(String json) implements CustomPayload {
        public static final CustomPayload.Id<HandshakePayload> ID = new CustomPayload.Id<>(
                NetworkingConstants.CHANNEL_HANDSHAKE);
        public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC = CustomPayload.codecOf(
                (value, buf) -> buf.writeString(value.json),
                buf -> new HandshakePayload(buf.readString()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ConfigPayload(String json) implements CustomPayload {
        public static final CustomPayload.Id<ConfigPayload> ID = new CustomPayload.Id<>(
                NetworkingConstants.CHANNEL_CONFIG);
        public static final PacketCodec<RegistryByteBuf, ConfigPayload> CODEC = CustomPayload.codecOf(
                (value, buf) -> buf.writeString(value.json),
                buf -> new ConfigPayload(buf.readString()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record LogPayload(String json) implements CustomPayload {
        public static final CustomPayload.Id<LogPayload> ID = new CustomPayload.Id<>(NetworkingConstants.CHANNEL_LOG);
        public static final PacketCodec<RegistryByteBuf, LogPayload> CODEC = CustomPayload.codecOf(
                (value, buf) -> buf.writeString(value.json),
                buf -> new LogPayload(buf.readString()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ScriptExecutionPayload(String scriptName, String content, boolean force) implements CustomPayload {
        public static final CustomPayload.Id<ScriptExecutionPayload> ID = new CustomPayload.Id<>(
                net.minecraft.util.Identifier.of("kashub", "script_exec"));
        public static final PacketCodec<RegistryByteBuf, ScriptExecutionPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ScriptExecutionPayload::scriptName,
                PacketCodecs.STRING, ScriptExecutionPayload::content,
                PacketCodecs.BOOL, ScriptExecutionPayload::force,
                ScriptExecutionPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ScriptMgmtPayload(byte action, String data) implements CustomPayload {
        public static final CustomPayload.Id<ScriptMgmtPayload> ID = new CustomPayload.Id<>(
                net.minecraft.util.Identifier.of("kashub", "script_mgmt"));
        public static final PacketCodec<RegistryByteBuf, ScriptMgmtPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.BYTE, ScriptMgmtPayload::action,
                PacketCodecs.STRING, ScriptMgmtPayload::data,
                ScriptMgmtPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Since plugin sends on same channel name "kashub:script_mgmt" for both
    // directions,
    // handling it in Fabric requires careful payload registration.
    // Fabric 1.21 distinguishes C2S and S2C registries.
    // So we can reuse the ID "kashub:script_mgmt" for both, but we need separate
    // classes if we want separate types,
    // OR just use one class for both if the structure is the same.
    // Structure IS the same: [Byte action] [String data].
    // So we can reuse ScriptMgmtPayload for both C2S and S2C!

}

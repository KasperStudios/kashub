package kasperstudios.kashub.network.payload;

import com.kasperstudios.khwrapper.IPacketBuffer;
import com.kasperstudios.khwrapper.IPacketPayload;
import kasperstudios.kashub.network.NetworkingConstants;

/**
 * Platform-agnostic networking payloads for KasHub.
 */
public class NetworkingPayloads {

    public record HandshakePayload(String json) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_HANDSHAKE;

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeString(json);
        }

        public static HandshakePayload read(IPacketBuffer buffer) {
            return new HandshakePayload(buffer.readString());
        }
    }

    public record ConfigPayload(String json) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CONFIG;

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeString(json);
        }

        public static ConfigPayload read(IPacketBuffer buffer) {
            return new ConfigPayload(buffer.readString());
        }
    }

    public record LogPayload(String json) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_LOG;

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeString(json);
        }

        public static LogPayload read(IPacketBuffer buffer) {
            return new LogPayload(buffer.readString());
        }
    }

    public record ScriptExecutionPayload(String scriptName, String content, boolean force) implements IPacketPayload {
        public static final String ID = "kashub:script_exec";

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeString(scriptName);
            buffer.writeString(content);
            buffer.writeBoolean(force);
        }

        public static ScriptExecutionPayload read(IPacketBuffer buffer) {
            return new ScriptExecutionPayload(buffer.readString(), buffer.readString(), buffer.readBoolean());
        }
    }

    public record ScriptMgmtPayload(byte action, String data) implements IPacketPayload {
        public static final String ID = "kashub:script_mgmt";

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeBytes(new byte[]{action});
            buffer.writeString(data);
        }

        public static ScriptMgmtPayload read(IPacketBuffer buffer) {
            byte action = buffer.readBytes()[0];
            return new ScriptMgmtPayload(action, buffer.readString());
        }
    }

    public record ScriptPacketPayload(String channel, byte[] data) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_SCRIPT_PACKET;

        @Override
        public String getPacketId() {
            return ID;
        }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeString(channel);
            buffer.writeBytes(data);
        }

        public static ScriptPacketPayload read(IPacketBuffer buffer) {
            return new ScriptPacketPayload(buffer.readString(), buffer.readBytes());
        }
    }

    public record BlackoutPayload(int durationSeconds, String message) implements IPacketPayload {
        public static final String ID = "kashub:blackout";

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(durationSeconds);
            buffer.writeString(message);
        }

        public static BlackoutPayload read(IPacketBuffer buffer) {
            return new BlackoutPayload(buffer.readInt(), buffer.readString());
        }
    }

    public record InjectionStatusPayload(boolean success, String message) implements IPacketPayload {
        public static final String ID = "kashub:injection_status";

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeBoolean(success);
            buffer.writeString(message);
        }

        public static InjectionStatusPayload read(IPacketBuffer buffer) {
            return new InjectionStatusPayload(buffer.readBoolean(), buffer.readString());
        }
    }

    public record ImplantSyncPayload(int tier) implements IPacketPayload {
        public static final String ID = "kashub:implant_sync";

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(tier);
        }

        public static ImplantSyncPayload read(IPacketBuffer buffer) {
            return new ImplantSyncPayload(buffer.readInt());
        }
    }

    public record InjectionRequestPayload(net.minecraft.core.BlockPos pos, int tier) implements IPacketPayload {
        public static final String ID = "kashub:injection_request";

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeLong(pos.asLong());
            buffer.writeInt(tier);
        }

        public static InjectionRequestPayload read(IPacketBuffer buffer) {
            return new InjectionRequestPayload(net.minecraft.core.BlockPos.of(buffer.readLong()), buffer.readInt());
        }
    }
    public record VisualEffectPayload(byte type, float intensity, int durationTicks) implements IPacketPayload {
        public static final String ID = "kashub:visual_effect";
        public static final byte TYPE_SHAKE = 0;
        public static final byte TYPE_FLASH = 1;

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeBytes(new byte[]{type});
            buffer.writeFloat(intensity);
            buffer.writeInt(durationTicks);
        }

        public static VisualEffectPayload read(IPacketBuffer buffer) {
            byte type = buffer.readBytes()[0];
            return new VisualEffectPayload(type, buffer.readFloat(), buffer.readInt());
        }
    }
    public record ComputerLinkPayload(int computerId, net.minecraft.core.BlockPos pos, boolean connect) implements IPacketPayload {
        public static final String ID = "kashub:computer_link";

        @Override
        public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeLong(pos.asLong());
            buffer.writeBoolean(connect);
        }

        public static ComputerLinkPayload read(IPacketBuffer buffer) {
            return new ComputerLinkPayload(buffer.readInt(), net.minecraft.core.BlockPos.of(buffer.readLong()), buffer.readBoolean());
        }
    }

    // ─── CC:T Integration ───────────────────────────────────────────────────────

    /** C2S — create or overwrite a file on a CC computer's disk. */
    public record CCScriptPushPayload(int computerId, String filename, String content) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_PUSH;

        @Override public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeString(filename);
            buffer.writeString(content);
        }

        public static CCScriptPushPayload read(IPacketBuffer buffer) {
            return new CCScriptPushPayload(buffer.readInt(), buffer.readString(), buffer.readString());
        }
    }

    /** S2C — file listing for a CC computer disk (sent on connect or after push). */
    public record CCFileListPayload(int computerId, java.util.List<String> filenames) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_FILE_LIST;

        @Override public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeInt(filenames.size());
            for (String f : filenames) buffer.writeString(f);
        }

        public static CCFileListPayload read(IPacketBuffer buffer) {
            int id    = buffer.readInt();
            int count = buffer.readInt();
            java.util.List<String> files = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) files.add(buffer.readString());
            return new CCFileListPayload(id, files);
        }
    }

    /** C2S — raw terminal input line sent to a CC computer. */
    public record CCTerminalInputPayload(int computerId, String input) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_TERM_INPUT;

        @Override public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeString(input);
        }

        public static CCTerminalInputPayload read(IPacketBuffer buffer) {
            return new CCTerminalInputPayload(buffer.readInt(), buffer.readString());
        }
    }

    /** S2C — terminal output from a CC computer (may contain §-codes). */
    public record CCTerminalOutputPayload(int computerId, String text) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_TERM_OUTPUT;

        @Override public String getPacketId() { return ID; }

        @Override
        public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeString(text);
        }

        public static CCTerminalOutputPayload read(IPacketBuffer buffer) {
            return new CCTerminalOutputPayload(buffer.readInt(), buffer.readString());
        }
    }

    /** C2S — request content of a file on a CC computer. */
    public record CCFileReadRequestPayload(int computerId, String filename) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_READ_REQ;
        @Override public String getPacketId() { return ID; }
        @Override public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeString(filename);
        }
        public static CCFileReadRequestPayload read(IPacketBuffer buffer) {
            return new CCFileReadRequestPayload(buffer.readInt(), buffer.readString());
        }
    }

    /** S2C — file content from a CC computer. */
    public record CCFileContentPayload(int computerId, String filename, String content, boolean success) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_CC_FILE_DATA;
        @Override public String getPacketId() { return ID; }
        @Override public void write(IPacketBuffer buffer) {
            buffer.writeInt(computerId);
            buffer.writeString(filename);
            buffer.writeBoolean(success);
            if (success) {
                buffer.writeString(content);
            }
        }
        public static CCFileContentPayload read(IPacketBuffer buffer) {
            int id = buffer.readInt();
            String name = buffer.readString();
            boolean success = buffer.readBoolean();
            String content = success ? buffer.readString() : null;
            return new CCFileContentPayload(id, name, content, success);
        }
    }

    /** C2S — request peripheral deep scan for a block. */
    public record PeripheralScanRequestPayload(net.minecraft.core.BlockPos pos) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_PERIPHERAL_SCAN_REQ;
        @Override public String getPacketId() { return ID; }
        @Override public void write(IPacketBuffer buffer) {
            buffer.writeLong(pos.asLong());
        }
        public static PeripheralScanRequestPayload read(IPacketBuffer buffer) {
            return new PeripheralScanRequestPayload(net.minecraft.core.BlockPos.of(buffer.readLong()));
        }
    }

    /** S2C — result of a peripheral deep scan. */
    public record PeripheralScanResultPayload(String type, java.util.List<String> methods) implements IPacketPayload {
        public static final String ID = NetworkingConstants.CHANNEL_PERIPHERAL_SCAN_RES;
        @Override public String getPacketId() { return ID; }
        @Override public void write(IPacketBuffer buffer) {
            buffer.writeString(type);
            buffer.writeInt(methods.size());
            for (String method : methods) {
                buffer.writeString(method);
            }
        }
        public static PeripheralScanResultPayload read(IPacketBuffer buffer) {
            String type = buffer.readString();
            int count = buffer.readInt();
            java.util.List<String> methods = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                methods.add(buffer.readString());
            }
            return new PeripheralScanResultPayload(type, methods);
        }
    }
}

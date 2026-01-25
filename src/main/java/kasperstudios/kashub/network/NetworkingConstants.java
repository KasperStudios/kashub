package kasperstudios.kashub.network;

import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final String MOD_ID = "kashub";

    // Channels
    public static final Identifier CHANNEL_HANDSHAKE = Identifier.of(MOD_ID, "handshake");
    public static final Identifier CHANNEL_CONFIG = Identifier.of(MOD_ID, "config");
    public static final Identifier CHANNEL_LOG = Identifier.of(MOD_ID, "log");
}

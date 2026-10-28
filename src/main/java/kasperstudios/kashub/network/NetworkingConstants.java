package kasperstudios.kashub.network;

public class NetworkingConstants {
    public static final String MOD_ID = "kashub";

    // Channels
    public static final String CHANNEL_HANDSHAKE = "kashub:handshake";
    public static final String CHANNEL_CONFIG = "kashub:config";
    public static final String CHANNEL_LOG = "kashub:log";
    public static final String CHANNEL_SCRIPT_PACKET   = "kashub:script_packet";

    // CC:T bridge channels
    public static final String CHANNEL_CC_PUSH         = "kashub:cc_push";      // C2S: push file to CC disk
    public static final String CHANNEL_CC_FILE_LIST    = "kashub:cc_file_list"; // S2C: list of files on CC disk
    public static final String CHANNEL_CC_TERM_INPUT   = "kashub:cc_term_in";   // C2S: terminal input
    public static final String CHANNEL_CC_TERM_OUTPUT  = "kashub:cc_term_out";  // S2C: terminal output
    public static final String CHANNEL_CC_READ_REQ     = "kashub:cc_read_req";  // C2S: request file content
    public static final String CHANNEL_CC_FILE_DATA    = "kashub:cc_file_data"; // S2C: send file content

    // Peripheral Scan channels
    public static final String CHANNEL_PERIPHERAL_SCAN_REQ = "kashub:peripheral_scan_req"; // C2S
    public static final String CHANNEL_PERIPHERAL_SCAN_RES = "kashub:peripheral_scan_res"; // S2C
}

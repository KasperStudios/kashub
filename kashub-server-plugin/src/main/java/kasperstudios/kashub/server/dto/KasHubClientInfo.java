package kasperstudios.kashub.server.dto;

import java.util.List;

public class KasHubClientInfo {
    private String version;
    private List<String> capabilities;
    private String platform = "Unknown";
    private String modLoader = "Unknown";
    private String deviceSpec = "Unknown";

    public KasHubClientInfo(String version, List<String> capabilities, String platform, String modLoader) {
        this.version = version;
        this.capabilities = capabilities;
        this.platform = platform;
        this.modLoader = modLoader;
        this.lastSeen = System.currentTimeMillis();
    }

    // Default constructor for Gson
    public KasHubClientInfo() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getModLoader() {
        return modLoader;
    }

    public void setModLoader(String modLoader) {
        this.modLoader = modLoader;
    }

    public String getDeviceSpec() {
        return deviceSpec;
    }

    public void setDeviceSpec(String deviceSpec) {
        this.deviceSpec = deviceSpec;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
}

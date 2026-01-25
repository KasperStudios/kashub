package kasperstudios.kashub.server;

import com.google.gson.Gson;
import kasperstudios.kashub.server.dto.ServerConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    private final KasHubServerPlugin plugin;
    private ServerConfig serverConfig;
    private final Gson gson = new Gson();

    public ConfigManager(KasHubServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        serverConfig = new ServerConfig();
        serverConfig.setEnabled(config.getBoolean("enabled", true));

        String modeStr = config.getString("mode", "FULL");
        try {
            serverConfig.setMode(ServerConfig.Mode.valueOf(modeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid mode in config: " + modeStr + ". Defaulting to FULL.");
            serverConfig.setMode(ServerConfig.Mode.FULL);
        }

        serverConfig.setAllowEditor(config.getBoolean("allowEditor", true));
        serverConfig.setAllowExternalEditor(config.getBoolean("allowExternalEditor", false));
        serverConfig.setAllowCheatMode(config.getBoolean("allowCheatMode", false));
        serverConfig.setDisabledCommands(config.getStringList("disabledCommands"));
        serverConfig.setAllowedNamespaces(config.getStringList("allowedNamespaces"));
        serverConfig.setLogBlockedCommands(config.getBoolean("logBlockedCommands", true));

        serverConfig.setMessageOnBlocked(
                config.getString("messages.blocked", "KasHub: This command is blocked by the server."));
        serverConfig.setMessageOnDisabled(config.getString("messages.disabled", "KasHub is disabled on this server."));

        plugin.getLogger().info("Configuration loaded: " + gson.toJson(serverConfig));
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public String getServerConfigJson() {
        return gson.toJson(serverConfig);
    }
}

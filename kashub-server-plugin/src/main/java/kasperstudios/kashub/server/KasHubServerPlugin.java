package kasperstudios.kashub.server;

import org.bukkit.plugin.java.JavaPlugin;

public class KasHubServerPlugin extends JavaPlugin {

    private static KasHubServerPlugin instance;
    private ConfigManager configManager;
    private HandshakeListener handshakeListener;
    private kasperstudios.kashub.server.data.StorageManager storageManager;
    private kasperstudios.kashub.server.script.ServerScriptManager scriptManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize Config Manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize Data Managers
        storageManager = new kasperstudios.kashub.server.data.StorageManager(this);
        scriptManager = new kasperstudios.kashub.server.script.ServerScriptManager(this);

        // Register Channels
        this.handshakeListener = new HandshakeListener(this);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "kashub:handshake", this.handshakeListener);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "kashub:log", new LogListener(this));

        // Script Management
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "kashub:script_mgmt",
                new kasperstudios.kashub.server.network.ScriptNetworkHandler(this));
        // For sending scripts to client
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "kashub:script_mgmt");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "kashub:script_exec");

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "kashub:config");

        // Register Commands
        this.getCommand("kashub").setExecutor(new KasHubCommand(this));

        getLogger().info("KasHub Server Plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KasHub Server Plugin disabled.");
    }

    public static KasHubServerPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HandshakeListener getHandshakeListener() {
        return handshakeListener;
    }

    public kasperstudios.kashub.server.data.StorageManager getStorageManager() {
        return storageManager;
    }

    public kasperstudios.kashub.server.script.ServerScriptManager getScriptManager() {
        return scriptManager;
    }
}

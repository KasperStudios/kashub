package kasperstudios.kashub.network;

import kasperstudios.kashub.network.dto.ServerConfig;
import org.jetbrains.annotations.Nullable;

public class ServerModeManager {
    private static final ServerModeManager INSTANCE = new ServerModeManager();

    public enum ControlMode {
        LOCAL,
        SERVER_CONTROLLED
    }

    private ControlMode currentMode = ControlMode.LOCAL;
    private ServerConfig serverConfig = null;

    private ServerModeManager() {
    }

    public static ServerModeManager getInstance() {
        return INSTANCE;
    }

    public void switchToServerControlled(ServerConfig config) {
        this.currentMode = ControlMode.SERVER_CONTROLLED;
        this.serverConfig = config;
    }

    public void switchToLocal() {
        this.currentMode = ControlMode.LOCAL;
        this.serverConfig = null;
    }

    public ControlMode getCurrentMode() {
        return currentMode;
    }

    public boolean isServerControlled() {
        return currentMode == ControlMode.SERVER_CONTROLLED;
    }

    @Nullable
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public boolean isEditorAllowed() {
        if (currentMode == ControlMode.LOCAL)
            return true;
        return serverConfig != null && serverConfig.isAllowEditor();
    }

    public boolean isExternalEditorAllowed() {
        if (currentMode == ControlMode.LOCAL)
            return true;
        return serverConfig != null && serverConfig.isAllowExternalEditor();
    }

    public boolean isCheatModeAllowed() {
        if (currentMode == ControlMode.LOCAL)
            return true;
        return serverConfig != null && serverConfig.isAllowCheatMode();
    }
}

package kasperstudios.kashub.network.dto;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
    public enum Mode {
        FULL, RESTRICTED, DISABLED
    }

    private boolean enabled = true;
    private Mode mode = Mode.FULL;
    private boolean allowEditor = true;
    private List<String> disabledCommands = new ArrayList<>();
    private List<String> allowedNamespaces = new ArrayList<>();
    private boolean logBlockedCommands = true;
    private String messageOnBlocked = "KasHub: This command is blocked by the server.";
    private String messageOnDisabled = "KasHub is disabled on this server.";
    private boolean allowExternalEditor = false;
    private boolean allowCheatMode = false;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isAllowEditor() {
        return allowEditor;
    }

    public void setAllowEditor(boolean allowEditor) {
        this.allowEditor = allowEditor;
    }

    public boolean isAllowExternalEditor() {
        return allowExternalEditor;
    }

    public void setAllowExternalEditor(boolean allowExternalEditor) {
        this.allowExternalEditor = allowExternalEditor;
    }

    public boolean isAllowCheatMode() {
        return allowCheatMode;
    }

    public void setAllowCheatMode(boolean allowCheatMode) {
        this.allowCheatMode = allowCheatMode;
    }

    public List<String> getDisabledCommands() {
        return disabledCommands;
    }

    public void setDisabledCommands(List<String> disabledCommands) {
        this.disabledCommands = disabledCommands;
    }

    public List<String> getAllowedNamespaces() {
        return allowedNamespaces;
    }

    public void setAllowedNamespaces(List<String> allowedNamespaces) {
        this.allowedNamespaces = allowedNamespaces;
    }

    public boolean isLogBlockedCommands() {
        return logBlockedCommands;
    }

    public void setLogBlockedCommands(boolean logBlockedCommands) {
        this.logBlockedCommands = logBlockedCommands;
    }

    public String getMessageOnBlocked() {
        return messageOnBlocked;
    }

    public void setMessageOnBlocked(String messageOnBlocked) {
        this.messageOnBlocked = messageOnBlocked;
    }

    public String getMessageOnDisabled() {
        return messageOnDisabled;
    }

    public void setMessageOnDisabled(String messageOnDisabled) {
        this.messageOnDisabled = messageOnDisabled;
    }

    @Override
    public String toString() {
        return "ServerConfig{enabled=" + enabled + ", mode=" + mode + ", allowEditor=" + allowEditor + "}";
    }
}

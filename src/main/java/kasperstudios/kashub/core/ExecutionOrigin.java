package kasperstudios.kashub.core;

/**
 * ExecutionOrigin - Defines the functional role and privilege level of a script.
 */
public enum ExecutionOrigin {
    /**
     * Root/System level. Can modify world state, recipes, and global settings.
     */
    SERVER,

    /**
     * Player level. Controls character movement, inventory, and interactions.
     * Cannot modify global world properties or recipes.
     */
    USER,

    /**
     * OS/Shell level. Executes terminal utilities and shell commands.
     * Access is restricted to OS-layer bridges and mounts.
     */
    TERMINAL,

    /**
     * Orchestrator level. Used for modpack initialization and script routing.
     * Can spawn sub-contexts with different origins.
     */
    MODPACK
}

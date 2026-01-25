package kasperstudios.kashub.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;
import kasperstudios.kashub.gui.CodeCompletionManager;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Environment - Singleton provider for read-only environment variables.
 */
public class Environment {

    private static volatile Environment instance;
    private static final Object LOCK = new Object();

    private final Map<String, Variable> variables;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL_MS = 50; // Update at most every 50ms

    private Environment() {
        this.variables = new ConcurrentHashMap<>();
        this.lastUpdateTime = 0;
        initializeVariables();
    }

    public static Environment getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new Environment();
                }
            }
        }
        return instance;
    }

    private void initializeVariables() {
        // Player variables
        registerVariable("PLAYER_NAME", "", "Current player name");
        registerVariable("PLAYER_X", "0", "Player X coordinate");
        registerVariable("PLAYER_Y", "0", "Player Y coordinate");
        registerVariable("PLAYER_Z", "0", "Player Z coordinate");
        registerVariable("PLAYER_YAW", "0", "Player horizontal rotation (yaw)");
        registerVariable("PLAYER_PITCH", "0", "Player vertical rotation (pitch)");
        registerVariable("PLAYER_HEALTH", "20", "Player current health (0-20)");
        registerVariable("PLAYER_MAX_HEALTH", "20", "Player maximum health");
        registerVariable("PLAYER_FOOD", "20", "Player food level (0-20)");
        registerVariable("PLAYER_SATURATION", "5", "Player saturation level");
        registerVariable("PLAYER_XP", "0", "Player experience level");
        registerVariable("PLAYER_LEVEL", "0", "Player experience level (alias)");
        registerVariable("PLAYER_SPEED", "0.1", "Player movement speed");
        registerVariable("PLAYER_AIR", "300", "Player air supply (for swimming)");

        // Player state
        registerVariable("IS_SNEAKING", "false", "Is player sneaking");
        registerVariable("IS_SPRINTING", "false", "Is player sprinting");
        registerVariable("IS_SWIMMING", "false", "Is player swimming");
        registerVariable("IS_FLYING", "false", "Is player flying");
        registerVariable("IS_RIDING", "false", "Is player riding an entity");
        registerVariable("IS_ON_GROUND", "true", "Is player on ground");
        registerVariable("IS_IN_WATER", "false", "Is player in water");
        registerVariable("IS_IN_LAVA", "false", "Is player in lava");
        registerVariable("IS_BURNING", "false", "Is player on fire");
        registerVariable("IS_DEAD", "false", "Is player dead");

        // World variables
        registerVariable("WORLD_TIME", "0", "Current world time in ticks");
        registerVariable("WORLD_DAY", "0", "Current world day");
        registerVariable("WORLD_WEATHER", "clear", "Current weather (clear/rain/thunder)");
        registerVariable("WORLD_DIFFICULTY", "normal", "World difficulty");
        registerVariable("WORLD_IS_DAY", "true", "Is it daytime");
        registerVariable("WORLD_IS_NIGHT", "false", "Is it nighttime");
        registerVariable("WORLD_MOON_PHASE", "0", "Moon phase (0-7)");

        // Game state
        registerVariable("GAME_MODE", "survival", "Current game mode");
        registerVariable("DIMENSION", "minecraft:overworld", "Current dimension");
        registerVariable("IS_SINGLEPLAYER", "true", "Is singleplayer world");
        registerVariable("IS_MULTIPLAYER", "false", "Is multiplayer server");
        registerVariable("SERVER_NAME", "", "Server name (if multiplayer)");

        // Held item
        registerVariable("HELD_ITEM", "", "Currently held item name");
        registerVariable("HELD_ITEM_COUNT", "0", "Currently held item stack count");
        registerVariable("HELD_SLOT", "0", "Currently selected hotbar slot (0-8)");

        // Target (what player is looking at)
        registerVariable("TARGET_BLOCK", "", "Block player is looking at");
        registerVariable("TARGET_BLOCK_X", "0", "Target block X coordinate");
        registerVariable("TARGET_BLOCK_Y", "0", "Target block Y coordinate");
        registerVariable("TARGET_BLOCK_Z", "0", "Target block Z coordinate");
        registerVariable("TARGET_ENTITY", "", "Entity player is looking at");
        registerVariable("TARGET_DISTANCE", "0", "Distance to target");
    }

    private void registerVariable(String name, String defaultValue, String description) {
        variables.put(name, new Variable(name, defaultValue, description));
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return; // Throttle updates
        }
        lastUpdateTime = now;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
            return;

        ClientPlayerEntity player = mc.player;
        World world = mc.world;

        if (player == null || world == null)
            return;

        try {
            // Player position
            setValue("PLAYER_NAME", player.getName().getString());
            setValue("PLAYER_X", String.format("%.2f", player.getX()));
            setValue("PLAYER_Y", String.format("%.2f", player.getY()));
            setValue("PLAYER_Z", String.format("%.2f", player.getZ()));
            setValue("PLAYER_YAW", String.format("%.2f", player.getYaw()));
            setValue("PLAYER_PITCH", String.format("%.2f", player.getPitch()));

            // Player stats
            setValue("PLAYER_HEALTH", String.format("%.1f", player.getHealth()));
            setValue("PLAYER_MAX_HEALTH", String.format("%.1f", player.getMaxHealth()));
            setValue("PLAYER_FOOD", String.valueOf(player.getHungerManager().getFoodLevel()));
            setValue("PLAYER_SATURATION", String.format("%.1f", player.getHungerManager().getSaturationLevel()));
            setValue("PLAYER_XP", String.valueOf(player.experienceLevel));
            setValue("PLAYER_LEVEL", String.valueOf(player.experienceLevel));
            setValue("PLAYER_SPEED", String.format("%.3f", player.getMovementSpeed()));
            setValue("PLAYER_AIR", String.valueOf(player.getAir()));

            // Player state
            setValue("IS_SNEAKING", String.valueOf(player.isSneaking()));
            setValue("IS_SPRINTING", String.valueOf(player.isSprinting()));
            setValue("IS_SWIMMING", String.valueOf(player.isSwimming()));
            setValue("IS_FLYING", String.valueOf(player.getAbilities().flying));
            setValue("IS_RIDING", String.valueOf(player.isRiding()));
            setValue("IS_ON_GROUND", String.valueOf(player.isOnGround()));
            setValue("IS_IN_WATER", String.valueOf(player.isTouchingWater()));
            setValue("IS_IN_LAVA", String.valueOf(player.isInLava()));
            setValue("IS_BURNING", String.valueOf(player.isOnFire()));
            setValue("IS_DEAD", String.valueOf(player.isDead()));

            // World state
            long worldTime = world.getTimeOfDay();
            setValue("WORLD_TIME", String.valueOf(worldTime));
            setValue("WORLD_DAY", String.valueOf(worldTime / 24000L));
            setValue("WORLD_WEATHER", world.isRaining() ? (world.isThundering() ? "thunder" : "rain") : "clear");
            setValue("WORLD_DIFFICULTY", world.getDifficulty().getName());
            setValue("WORLD_IS_DAY", String.valueOf(worldTime % 24000 < 12000));
            setValue("WORLD_IS_NIGHT", String.valueOf(worldTime % 24000 >= 12000));
            setValue("WORLD_MOON_PHASE", String.valueOf(world.getMoonPhase()));

            // Game state
            setValue("GAME_MODE", player.isCreative() ? "creative" : (player.isSpectator() ? "spectator" : "survival"));
            setValue("DIMENSION", world.getRegistryKey().getValue().toString());
            setValue("IS_SINGLEPLAYER", String.valueOf(mc.isInSingleplayer()));
            setValue("IS_MULTIPLAYER", String.valueOf(!mc.isInSingleplayer()));

            if (mc.getCurrentServerEntry() != null) {
                setValue("SERVER_NAME", mc.getCurrentServerEntry().name);
            } else {
                setValue("SERVER_NAME", "");
            }

            // Held item
            var heldStack = player.getMainHandStack();
            if (!heldStack.isEmpty()) {
                setValue("HELD_ITEM", heldStack.getItem().toString());
                setValue("HELD_ITEM_COUNT", String.valueOf(heldStack.getCount()));
            } else {
                setValue("HELD_ITEM", "");
                setValue("HELD_ITEM_COUNT", "0");
            }
            setValue("HELD_SLOT", String.valueOf(player.getInventory().selectedSlot));

            // Update CodeCompletionManager
            try {
                CodeCompletionManager.updateEnvironmentVariables();
            } catch (Exception ignored) {
            }

            // Update Timers from TimerManager
            TimerManager tm = TimerManager.getInstance();
            setValue("timer_count", String.valueOf(tm.getTimers().size()));

            for (TimerManager.Timer t : tm.getTimers().values()) {
                String remKey = "timer_" + t.name + "_remaining";
                String expKey = "timer_" + t.name + "_expired";
                setOrRegister(remKey, String.valueOf(t.getRemainingSeconds()));
                setOrRegister(expKey, String.valueOf(t.expired));
            }

        } catch (Exception e) {
            // Silently ignore errors during update
        }
    }

    private void setOrRegister(String name, String value) {
        synchronized (variables) {
            if (!variables.containsKey(name)) {
                variables.put(name, new Variable(name, value, "Dynamic variable"));
            } else {
                Variable var = variables.get(name);
                var.setValue(value);
            }
        }
    }

    private void setValue(String name, String value) {
        Variable var = variables.get(name);
        if (var != null) {
            var.setValue(value);
        }
    }

    public String getValue(String name) {
        Variable var = variables.get(name);
        return var != null ? var.getValue() : null;
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public Map<String, String> getAllVariables() {
        Map<String, String> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, Variable> entry : variables.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }
        return result;
    }

    public Map<String, Variable> getVariableDefinitions() {
        return Collections.unmodifiableMap(variables);
    }

    public String getDescription(String name) {
        Variable var = variables.get(name);
        return var != null ? var.getDescription() : null;
    }

    public static class Variable {
        private final String name;
        private volatile String value;
        private final String description;

        public Variable(String name, String value, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }
    }
}

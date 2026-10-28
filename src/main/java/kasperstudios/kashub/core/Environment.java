package kasperstudios.kashub.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kasperstudios.kashub.gui.CodeCompletionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Environment - Singleton provider for read-only environment variables.
 */
public class Environment {
    private static final Logger LOGGER = LoggerFactory.getLogger(Environment.class);

    private static volatile Environment instance;
    private static final Object LOCK = new Object();

    private final Map<String, Value> variables;
    private final Map<String, Variable> variableDefinitions;
    private long lastUpdateTime;
    private static final long UPDATE_INTERVAL_MS = 50; // Update at most every 50ms

    private Environment() {
        this.variables = new ConcurrentHashMap<>();
        this.variableDefinitions = new ConcurrentHashMap<>();
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

    private int implantTier = 0;

    private void initializeVariables() {
        // Player variables
        registerVariable("IMPLANT_TIER", Value.of(0), "Neuro-implant tier level (0 = none)");
        registerVariable("PLAYER_NAME", Value.of(""), "Current player name");
        registerVariable("PLAYER_X", Value.of(0), "Player X coordinate");
        registerVariable("PLAYER_Y", Value.of(0), "Player Y coordinate");
        registerVariable("PLAYER_Z", Value.of(0), "Player Z coordinate");
        registerVariable("PLAYER_YAW", Value.of(0), "Player horizontal rotation (yaw)");
        registerVariable("PLAYER_PITCH", Value.of(0), "Player vertical rotation (pitch)");
        registerVariable("PLAYER_HEALTH", Value.of(20), "Player current health (0-20)");
        registerVariable("PLAYER_MAX_HEALTH", Value.of(20), "Player maximum health");
        registerVariable("PLAYER_FOOD", Value.of(20), "Player food level (0-20)");
        registerVariable("PLAYER_SATURATION", Value.of(5), "Player saturation level");
        registerVariable("PLAYER_XP", Value.of(0), "Player experience level");
        registerVariable("PLAYER_LEVEL", Value.of(0), "Player experience level (alias)");
        registerVariable("PLAYER_SPEED", Value.of(0.1), "Player movement speed");
        registerVariable("PLAYER_AIR", Value.of(300), "Player air supply (for swimming)");

        // Player state
        registerVariable("IS_SNEAKING", Value.FALSE, "Is player sneaking");
        registerVariable("IS_SPRINTING", Value.FALSE, "Is player sprinting");
        registerVariable("IS_SWIMMING", Value.FALSE, "Is player swimming");
        registerVariable("IS_FLYING", Value.FALSE, "Is player flying");
        registerVariable("IS_RIDING", Value.FALSE, "Is player riding an entity");
        registerVariable("IS_ON_GROUND", Value.TRUE, "Is player on ground");
        registerVariable("IS_IN_WATER", Value.FALSE, "Is player in water");
        registerVariable("IS_IN_LAVA", Value.FALSE, "Is player in lava");
        registerVariable("IS_BURNING", Value.FALSE, "Is player on fire");
        registerVariable("IS_DEAD", Value.FALSE, "Is player dead");

        // World variables
        registerVariable("WORLD_TIME", Value.of(0), "Current world time in ticks");
        registerVariable("WORLD_DAY", Value.of(0), "Current world day");
        registerVariable("WORLD_WEATHER", Value.of("clear"), "Current weather (clear/rain/thunder)");
        registerVariable("WORLD_DIFFICULTY", Value.of("normal"), "World difficulty");
        registerVariable("WORLD_IS_DAY", Value.TRUE, "Is it daytime");
        registerVariable("WORLD_IS_NIGHT", Value.FALSE, "Is it nighttime");
        registerVariable("WORLD_MOON_PHASE", Value.of(0), "Moon phase (0-7)");

        // Game state
        registerVariable("GAME_MODE", Value.of("survival"), "Current game mode");
        registerVariable("DIMENSION", Value.of("minecraft:overworld"), "Current dimension");
        registerVariable("IS_SINGLEPLAYER", Value.TRUE, "Is singleplayer world");
        registerVariable("IS_MULTIPLAYER", Value.FALSE, "Is multiplayer server");
        registerVariable("SERVER_NAME", Value.of(""), "Server name (if multiplayer)");

        // Held item
        registerVariable("HELD_ITEM", Value.of(""), "Currently held item name");
        registerVariable("HELD_ITEM_COUNT", Value.of(0), "Currently held item stack count");
        registerVariable("HELD_SLOT", Value.of(0), "Currently selected hotbar slot (0-8)");

        // Target (what player is looking at)
        registerVariable("TARGET_BLOCK", Value.of(""), "Block player is looking at");
        registerVariable("TARGET_BLOCK_X", Value.of(0), "Target block X coordinate");
        registerVariable("TARGET_BLOCK_Y", Value.of(0), "Target block Y coordinate");
        registerVariable("TARGET_BLOCK_Z", Value.of(0), "Target block Z coordinate");
        registerVariable("TARGET_ENTITY", Value.of(""), "Entity player is looking at");
        registerVariable("TARGET_DISTANCE", Value.of(0), "Distance to target");
    }

    private void registerVariable(String name, Value defaultValue, String description) {
        variables.put(name, defaultValue);
        variableDefinitions.put(name, new Variable(name, description));
    }

    public Map<String, Variable> getVariableDefinitions() {
        return Collections.unmodifiableMap(variableDefinitions);
    }

    public static class Variable {
        private final String name;
        private final String description;

        public Variable(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public Value getValue() { return Environment.getInstance().getValue(name); }
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return; // Throttle updates
        }
        lastUpdateTime = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null)
            return;

        LocalPlayer player = mc.player;
        net.minecraft.world.level.Level world = mc.level;

        if (player == null || world == null)
            return;

        try {
            // Player position
            setValue("PLAYER_NAME", Value.of(player.getName().getString()));
            setValue("PLAYER_X", Value.of(player.getX()));
            setValue("PLAYER_Y", Value.of(player.getY()));
            setValue("PLAYER_Z", Value.of(player.getZ()));
            setValue("PLAYER_YAW", Value.of(player.getYRot()));
            setValue("PLAYER_PITCH", Value.of(player.getXRot()));

            // Player stats
            setValue("PLAYER_HEALTH", Value.of(player.getHealth()));
            setValue("PLAYER_MAX_HEALTH", Value.of(player.getMaxHealth()));
            setValue("PLAYER_FOOD", Value.of(player.getFoodData().getFoodLevel()));
            setValue("PLAYER_SATURATION", Value.of(player.getFoodData().getSaturationLevel()));
            setValue("PLAYER_XP", Value.of(player.experienceLevel));
            setValue("PLAYER_LEVEL", Value.of(player.experienceLevel));
            setValue("PLAYER_SPEED", Value.of(player.getSpeed()));
            setValue("PLAYER_AIR", Value.of(player.getAirSupply()));

            // Player state
            setValue("IS_SNEAKING", Value.of(player.isShiftKeyDown()));
            setValue("IS_SPRINTING", Value.of(player.isSprinting()));
            setValue("IS_SWIMMING", Value.of(player.isSwimming()));
            setValue("IS_FLYING", Value.of(player.getAbilities().flying));
            setValue("IS_RIDING", Value.of(player.isHandsBusy()));
            setValue("IS_ON_GROUND", Value.of(player.onGround()));
            setValue("IS_IN_WATER", Value.of(player.isInWater()));
            setValue("IS_IN_LAVA", Value.of(player.isInLava()));
            setValue("IS_BURNING", Value.of(player.isOnFire()));
            setValue("IS_DEAD", Value.of(player.isDeadOrDying()));

            // World state
            long worldTime = world.getDayTime();
            setValue("WORLD_TIME", Value.of(worldTime));
            setValue("WORLD_DAY", Value.of(worldTime / 24000L));
            setValue("WORLD_WEATHER", Value.of(world.isRaining() ? (world.isThundering() ? "thunder" : "rain") : "clear"));
            setValue("WORLD_DIFFICULTY", Value.of(world.getDifficulty().getKey()));
            setValue("WORLD_IS_DAY", Value.of(worldTime % 24000 < 12000));
            setValue("WORLD_IS_NIGHT", Value.of(worldTime % 24000 >= 12000));
            setValue("WORLD_MOON_PHASE", Value.of(world.getMoonPhase()));

            // Game state
            setValue("GAME_MODE", Value.of(player.isCreative() ? "creative" : (player.isSpectator() ? "spectator" : "survival")));
            setValue("DIMENSION", Value.of(world.dimension().location().toString()));
            setValue("IS_SINGLEPLAYER", Value.of(mc.isLocalServer()));
            setValue("IS_MULTIPLAYER", Value.of(!mc.isLocalServer()));

            if (mc.getCurrentServer() != null) {
                setValue("SERVER_NAME", Value.of(mc.getCurrentServer().name));
            } else {
                setValue("SERVER_NAME", Value.of(""));
            }

            // Held item
            var heldStack = player.getMainHandItem();
            if (!heldStack.isEmpty()) {
                setValue("HELD_ITEM", Value.of(heldStack.getItem().toString()));
                setValue("HELD_ITEM_COUNT", Value.of(heldStack.getCount()));
            } else {
                setValue("HELD_ITEM", Value.of(""));
                setValue("HELD_ITEM_COUNT", Value.of(0));
            }
            setValue("HELD_SLOT", Value.of(player.getInventory().selected));



            // Update Timers from TimerManager
            TimerManager tm = TimerManager.getInstance();
            setValue("timer_count", Value.of(tm.getTimers().size()));

            for (TimerManager.Timer t : tm.getTimers().values()) {
                String remKey = "timer_" + t.name + "_remaining";
                String expKey = "timer_" + t.name + "_expired";
                variables.put(remKey, Value.of(t.getRemainingSeconds()));
                variables.put(expKey, Value.of(t.expired));
            }

            if (player instanceof IImplantData access) {
                this.implantTier = access.getImplantTier();
            }
            // Creative mode / AccessPolicy bypass logic:
            if (this.implantTier == 0 && kasperstudios.kashub.services.AccessPolicy.getInstance().hasAccess(player)) {
                this.implantTier = 3; // Virtual maximum tier for creative access
            }
            setValue("IMPLANT_TIER", Value.of(implantTier));

        } catch (Exception e) {
            LOGGER.warn("Error during environment update", e);
        }
    }

    public void setImplantTier(int tier) {
        this.implantTier = tier;
        setValue("IMPLANT_TIER", Value.of(tier));
    }

    public int getImplantTier() {
        return implantTier;
    }

    private void setValue(String name, Value value) {
        variables.put(name, value);
    }

    public Value getValue(String name) {
        return variables.getOrDefault(name, Value.NULL);
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public Map<String, String> getAllVariables() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Value> entry : variables.entrySet()) {
            result.put(entry.getKey(), entry.getValue().asString());
        }
        return result;
    }

    public Map<String, Value> getVariableValues() {
        return Collections.unmodifiableMap(variables);
    }

    public String getDescription(String name) {
        return "Environment variable"; // Simplified
    }
}

package kasperstudios.kashub.algorithm;

import java.util.*;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kasperstudios.kashub.algorithm.commands.*;
import kasperstudios.kashub.crashguard.CrashGuardCommand;

public class CommandRegistry {
    private static final Map<String, Command> COMMANDS = new HashMap<>();
    private static final List<Command> COMMAND_LIST = new ArrayList<>();
    public static final String MOD_ID = "kashub";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized)
            return;
        initialized = true;

        LOGGER.info("Initializing CommandRegistry...");

        try {
            Class<?> bootstrapClass = Class.forName("net.minecraft.Bootstrap");
            java.lang.reflect.Method method = bootstrapClass.getDeclaredMethod("isBootstrapped");
            method.setAccessible(true);
            Boolean isBootstrapped = (Boolean) method.invoke(null);
            if (!isBootstrapped) {

                LOGGER.warn("Minecraft not bootstrapped - proceeding anyway (test environment)");

            }
        } catch (Exception e) {
            LOGGER.warn("Could not check bootstrap status - proceeding anyway: " + e.getMessage());

        }

        safeRegister(() -> new AICommand());
        safeRegister(() -> new AnimationCommand());
        safeRegister(() -> new AttackCommand());
        safeRegister(() -> new AutoTradeCommand());
        safeRegister(() -> new AutoCraftCommand());
        safeRegister(() -> new BreakBlockCommand());
        safeRegister(() -> new ChatCommand());
        safeRegister(() -> new CrashGuardCommand());
        safeRegister(() -> new DropItemCommand());
        safeRegister(() -> new EatCommand());
        safeRegister(() -> new EvalCommand());
        safeRegister(() -> new EquipArmorCommand());
        safeRegister(() -> new ExportCommand());
        safeRegister(() -> new ImportCommand());
        safeRegister(() -> new FullBrightCommand());
        safeRegister(() -> new GetBlockCommand());
        safeRegister(() -> new HealthMonitorCommand());
        safeRegister(() -> new InputCommand());
        safeRegister(() -> new InteractCommand());
        safeRegister(() -> new InventoryCommand());
        safeRegister(() -> new JumpCommand());
        safeRegister(() -> new LogCommand());
        safeRegister(() -> new LookAtCommand());
        safeRegister(() -> new LoopCommand());
        safeRegister(() -> new MacroCommand());
        safeRegister(() -> new MoveToCommand());
        safeRegister(() -> new OnEventCommand());
        safeRegister(() -> new PathfindCommand());
        safeRegister(() -> new PlaceBlockCommand());
        safeRegister(() -> new PrintCommand());
        safeRegister(() -> new ReportBugCommand());
        safeRegister(() -> new RunToCommand());
        safeRegister(() -> new ScanCommand());
        safeRegister(() -> new ScannerCommand());
        safeRegister(() -> new ScriptsCommand());
        safeRegister(() -> new SelectSlotCommand());
        safeRegister(() -> new SetHealthCommand());
        safeRegister(() -> new SneakCommand());
        safeRegister(() -> new SoundCommand());
        safeRegister(() -> new SpeedHackCommand());
        safeRegister(() -> new SprintCommand());
        safeRegister(() -> new StopCommand());
        safeRegister(() -> new SwimCommand());
        safeRegister(() -> new TeleportCommand());
        safeRegister(() -> new TimerCommand());
        safeRegister(() -> new UseItemCommand());
        safeRegister(() -> new VisionCommand());
        safeRegister(() -> new WaitCommand());

        LOGGER.info("Registered " + COMMANDS.size() + " commands");
    }

    private static void safeRegister(Supplier<Command> commandSupplier) {
        try {
            registerCommand(commandSupplier.get());
        } catch (Throwable t) {

            LOGGER.warn("Failed to register command: " + t.toString());
        }
    }

    public static void registerCommand(Command command) {
        String name = command.getName().toLowerCase();
        COMMANDS.put(name, command);
        COMMAND_LIST.add(command);
    }

    public static Command getCommand(String name) {
        if (!initialized)
            initialize();
        return COMMANDS.get(name.toLowerCase());
    }

    public static boolean hasCommand(String name) {
        if (!initialized)
            initialize();
        return COMMANDS.containsKey(name.toLowerCase());
    }

    public static Set<String> getAllCommandNames() {
        if (!initialized)
            initialize();
        return new HashSet<>(COMMANDS.keySet());
    }

    public static List<Command> getAllCommands() {
        if (!initialized)
            initialize();
        return new ArrayList<>(COMMAND_LIST);
    }

    public static List<Command> getCommands() {
        return new ArrayList<>(COMMANDS.values());
    }
}

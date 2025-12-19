package kasperstudios.kashub.algorithm;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kasperstudios.kashub.algorithm.commands.*;
import kasperstudios.kashub.crashguard.CrashGuardCommand;

/**
 * Реестр всех доступных команд в KHScript
 */
public class CommandRegistry {
    private static final Map<String, Command> COMMANDS = new HashMap<>();
    private static final List<Command> COMMAND_LIST = new ArrayList<>();
    public static final String MOD_ID = "kashub";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        LOGGER.info("Initializing CommandRegistry...");
        
        // Register all commands explicitly (works in both IDE and JAR)
        registerCommand(new AICommand());
        registerCommand(new AnimationCommand());
        registerCommand(new AttackCommand());
        registerCommand(new AutoTradeCommand());
        registerCommand(new AutoCraftCommand());
        registerCommand(new BreakBlockCommand());
        registerCommand(new ChatCommand());
        registerCommand(new CrashGuardCommand());
        registerCommand(new DropItemCommand());
        registerCommand(new EatCommand());
        registerCommand(new EvalCommand());
        registerCommand(new EquipArmorCommand());
        registerCommand(new FullBrightCommand());
        registerCommand(new GetBlockCommand());
        registerCommand(new InputCommand());
        registerCommand(new InteractCommand());
        registerCommand(new InventoryCommand());
        registerCommand(new JumpCommand());
        registerCommand(new LogCommand());
        registerCommand(new LookAtCommand());
        registerCommand(new LoopCommand());
        registerCommand(new MoveToCommand());
        registerCommand(new OnEventCommand());
        registerCommand(new PathfindCommand());
        registerCommand(new PlaceBlockCommand());
        registerCommand(new PrintCommand());
        registerCommand(new RunToCommand());
        registerCommand(new ScanCommand());
        registerCommand(new ScannerCommand());
        registerCommand(new ScriptsCommand());
        registerCommand(new SelectSlotCommand());
        registerCommand(new SetHealthCommand());
        registerCommand(new SneakCommand());
        registerCommand(new SoundCommand());
        registerCommand(new SpeedHackCommand());
        registerCommand(new SprintCommand());
        registerCommand(new StopCommand());
        registerCommand(new SwimCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new UseItemCommand());
        registerCommand(new VisionCommand());
        registerCommand(new WaitCommand());
        
        LOGGER.info("Registered " + COMMANDS.size() + " commands");
    }

    /**
     * Регистрирует новую команду
     */
    public static void registerCommand(Command command) {
        String name = command.getName().toLowerCase();
        COMMANDS.put(name, command);
        COMMAND_LIST.add(command);
    }

    /**
     * Возвращает команду по имени
     */
    public static Command getCommand(String name) {
        if (!initialized) initialize();
        return COMMANDS.get(name.toLowerCase());
    }

    /**
     * Проверяет, существует ли команда
     */
    public static boolean hasCommand(String name) {
        if (!initialized) initialize();
        return COMMANDS.containsKey(name.toLowerCase());
    }

    public static Set<String> getAllCommandNames() {
        if (!initialized) initialize();
        return new HashSet<>(COMMANDS.keySet());
    }

    public static List<Command> getAllCommands() {
        if (!initialized) initialize();
        return new ArrayList<>(COMMAND_LIST);
    }

    public static List<Command> getCommands() {
        return new ArrayList<>(COMMANDS.values());
    }
}

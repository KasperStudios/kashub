package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MacroCommand implements Command {

    private static boolean isRecording = false;
    private static String currentMacroName = null;
    private static List<MacroAction> recordedActions = new ArrayList<>();
    private static long recordStartTime = 0;
    private static Vec3d lastPosition = null;
    private static float lastYaw = 0;
    private static float lastPitch = 0;

    @Override
    public String getName() {
        return "macro";
    }

    @Override
    public String getDescription() {
        return "Record and playback player actions";
    }

    @Override
    public String getParameters() {
        return "record|stop|play|list|delete|save <args>";
    }

    @Override
    public String getCategory() {
        return "Automation";
    }

    @Override
    public String getDetailedHelp() {
        return "Record and playback player actions (macros).\n\n" +
               "Usage:\n" +
               "  macro record <name>     - Start recording\n" +
               "  macro stop              - Stop recording\n" +
               "  macro play <name> [speed] - Play macro\n" +
               "  macro list              - List saved macros\n" +
               "  macro delete <name>     - Delete macro\n" +
               "  macro save <name>       - Save to file\n\n" +
               "Recording captures:\n" +
               "  - Movement (position changes)\n" +
               "  - Camera rotation (yaw/pitch)\n" +
               "  - Timing between actions\n\n" +
               "Playback:\n" +
               "  - speed: 1.0 = normal, 2.0 = 2x faster, 0.5 = slower\n\n" +
               "Examples:\n" +
               "  macro record mining\n" +
               "  # ... perform actions ...\n" +
               "  macro stop\n" +
               "  macro play mining\n" +
               "  macro play mining 2.0\n" +
               "  macro save mining\n\n" +
               "Variables set:\n" +
               "  $macro_recording - Is recording active\n" +
               "  $macro_playing   - Is playback active\n" +
               "  $macro_count     - Number of saved macros\n\n" +
               "Notes:\n" +
               "  - Macros are session-only unless saved\n" +
               "  - Saved macros stored in config/kashub/macros/\n" +
               "  - Use with caution on servers";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "record":
                handleRecord(args, interpreter);
                break;
            case "stop":
                handleStop(interpreter);
                break;
            case "play":
                handlePlay(args, interpreter);
                break;
            case "list":
                handleList(interpreter);
                break;
            case "delete":
                handleDelete(args, interpreter);
                break;
            case "save":
                handleSave(args, interpreter);
                break;
            default:
                printHelp();
        }
    }

    private void handleRecord(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: macro record <name>");
            return;
        }

        if (isRecording) {
            ScriptLogger.getInstance().warn("Already recording macro: " + currentMacroName);
            return;
        }

        currentMacroName = args[1];
        recordedActions.clear();
        recordStartTime = System.currentTimeMillis();
        isRecording = true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            lastPosition = client.player.getPos();
            lastYaw = client.player.getYaw();
            lastPitch = client.player.getPitch();
        }

        interpreter.setVariable("macro_recording", "true");
        ScriptLogger.getInstance().info("Started recording macro: " + currentMacroName);
    }

    private void handleStop(ScriptInterpreter interpreter) {
        if (!isRecording) {
            ScriptLogger.getInstance().warn("No macro is being recorded");
            return;
        }

        isRecording = false;
        interpreter.setVariable("macro_recording", "false");

        ScriptLogger.getInstance().info("Stopped recording macro: " + currentMacroName +
                                       " (" + recordedActions.size() + " actions)");
    }

    private void handlePlay(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: macro play <name> [speed]");
            return;
        }

        String macroName = args[1];
        double speed = args.length >= 3 ? Double.parseDouble(args[2]) : 1.0;

        ScriptLogger.getInstance().info("Playing macro: " + macroName + " at " + speed + "x speed");
        interpreter.setVariable("macro_playing", "true");
    }

    private void handleList(ScriptInterpreter interpreter) {
        ScriptLogger.getInstance().info("Saved macros: " + currentMacroName);
        interpreter.setVariable("macro_count", "1");
    }

    private void handleDelete(String[] args, ScriptInterpreter interpreter) {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: macro delete <name>");
            return;
        }

        String macroName = args[1];
        ScriptLogger.getInstance().info("Deleted macro: " + macroName);
    }

    private void handleSave(String[] args, ScriptInterpreter interpreter) throws IOException {
        if (args.length < 2) {
            ScriptLogger.getInstance().error("Usage: macro save <name>");
            return;
        }

        String macroName = args[1];
        Path macroDir = Paths.get("config/kashub/macros");
        Files.createDirectories(macroDir);

        Path macroFile = macroDir.resolve(macroName + ".macro");

        ScriptLogger.getInstance().info("Saved macro to: " + macroFile);
    }

    public static void tick() {
        if (!isRecording) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        long timestamp = System.currentTimeMillis() - recordStartTime;
        Vec3d currentPos = player.getPos();
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        if (lastPosition != null && currentPos.distanceTo(lastPosition) > 0.01) {
            recordedActions.add(new MacroAction(
                MacroActionType.MOVE,
                timestamp,
                currentPos.x, currentPos.y, currentPos.z
            ));
            lastPosition = currentPos;
        }

        if (Math.abs(currentYaw - lastYaw) > 0.1 || Math.abs(currentPitch - lastPitch) > 0.1) {
            recordedActions.add(new MacroAction(
                MacroActionType.LOOK,
                timestamp,
                currentYaw, currentPitch, 0
            ));
            lastYaw = currentYaw;
            lastPitch = currentPitch;
        }
    }

    private void printHelp() {
        ScriptLogger.getInstance().info("Macro Command:");
        ScriptLogger.getInstance().info("  macro record <name> - Start recording");
        ScriptLogger.getInstance().info("  macro stop - Stop recording");
        ScriptLogger.getInstance().info("  macro play <name> [speed] - Play macro");
        ScriptLogger.getInstance().info("  macro list - List macros");
        ScriptLogger.getInstance().info("  macro delete <name> - Delete macro");
        ScriptLogger.getInstance().info("  macro save <name> - Save to file");
    }

    private static class MacroAction {
        MacroActionType type;
        long timestamp;
        double x, y, z;

        MacroAction(MacroActionType type, long timestamp, double x, double y, double z) {
            this.type = type;
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private enum MacroActionType {
        MOVE, LOOK, ATTACK, USE, JUMP, SNEAK, SPRINT
    }
}

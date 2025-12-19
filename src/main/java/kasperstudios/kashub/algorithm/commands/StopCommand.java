package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;
import kasperstudios.kashub.algorithm.events.EventManager;
import net.minecraft.client.MinecraftClient;

/**
 * Команда для остановки всех скриптов и действий
 * Синтаксис: stop [all/scripts/events/movement]
 */
public class StopCommand implements Command {

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Stops script execution and player actions";
    }

    @Override
    public String getParameters() {
        return "[all|scripts|events|movement]";
    }
    
    @Override
    public String getCategory() {
        return "Other";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Emergency stop for scripts and player actions.\n\n" +
               "Usage:\n" +
               "  stop                    - Stop everything (default)\n" +
               "  stop all                - Stop everything\n" +
               "  stop scripts            - Stop script execution only\n" +
               "  stop events             - Clear event handlers only\n" +
               "  stop movement           - Stop player movement only\n\n" +
               "What each mode stops:\n\n" +
               "  'all' (default):\n" +
               "    - Script interpreter\n" +
               "    - Event handlers\n" +
               "    - All movement keys\n" +
               "    - Attack/use actions\n" +
               "    - moveTo/runTo commands\n\n" +
               "  'scripts':\n" +
               "    - Script interpreter only\n\n" +
               "  'events':\n" +
               "    - Clears all onEvent handlers\n\n" +
               "  'movement':\n" +
               "    - Releases all movement keys\n" +
               "    - Stops moveTo/runTo\n" +
               "    - Zeroes player velocity\n\n" +
               "Examples:\n" +
               "  stop                    // Emergency stop all\n" +
               "  stop movement           // Just stop moving\n" +
               "  stop events             // Clear event handlers\n\n" +
               "Tip: Bind to hotkey (Z by default) for quick stop.";
    }

    @Override
    public void execute(String[] args) throws Exception {
        String target = args.length > 0 ? args[0].toLowerCase() : "all";
        MinecraftClient client = MinecraftClient.getInstance();

        switch (target) {
            case "scripts":
                ScriptInterpreter.getInstance().stopProcessing();
                System.out.println("Scripts stopped");
                break;
                
            case "events":
                EventManager.getInstance().clear();
                System.out.println("Event handlers cleared");
                break;
                
            case "movement":
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.setVelocity(0, 0, 0);
                    }
                    client.options.forwardKey.setPressed(false);
                    client.options.backKey.setPressed(false);
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                    client.options.sprintKey.setPressed(false);
                });
                MoveToCommand.stopMoving();
                RunToCommand.stopRunning();
                System.out.println("Movement stopped");
                break;
                
            case "all":
            default:
                ScriptInterpreter.getInstance().stopProcessing();
                EventManager.getInstance().clear();
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.setVelocity(0, 0, 0);
                    }
                    client.options.forwardKey.setPressed(false);
                    client.options.backKey.setPressed(false);
                    client.options.leftKey.setPressed(false);
                    client.options.rightKey.setPressed(false);
                    client.options.jumpKey.setPressed(false);
                    client.options.sneakKey.setPressed(false);
                    client.options.sprintKey.setPressed(false);
                    client.options.attackKey.setPressed(false);
                    client.options.useKey.setPressed(false);
                });
                MoveToCommand.stopMoving();
                RunToCommand.stopRunning();
                AttackCommand.stopAttacking();
                System.out.println("All stopped");
                break;
        }
    }
}

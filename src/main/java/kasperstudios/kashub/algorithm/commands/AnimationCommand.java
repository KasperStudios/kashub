package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.network.AnimationManager;

public class AnimationCommand implements Command {
    @Override
    public String getName() {
        return "animation";
    }

    @Override
    public String getDescription() {
        return "Plays or controls animations";
    }

    @Override
    public String getParameters() {
        return "<play|stop|list> [name] [duration]";
    }

    @Override
    public String getCategory() {
        return "Visual";
    }

    @Override
    public String getDetailedHelp() {
        return "Plays or controls player animations.\n\n" +
               "Usage:\n" +
               "  animation play <name> [duration]\n" +
               "  animation stop [name]\n" +
               "  animation list\n\n" +
               "Parameters:\n" +
               "  <name>     - Animation name to play\n" +
               "  [duration] - Duration in ticks (default: 20)\n\n" +
               "Actions:\n" +
               "  play  - Start playing animation\n" +
               "  stop  - Stop animation (or all if no name)\n" +
               "  list  - Show active animations\n\n" +
               "Examples:\n" +
               "  animation play wave 40\n" +
               "  animation play dance\n" +
               "  animation stop wave\n" +
               "  animation stop\n" +
               "  animation list\n\n" +
               "Notes:\n" +
               "  - Animations are client-side only\n" +
               "  - Other players won't see custom animations\n" +
               "  - Duration is in game ticks (20 = 1 second)";
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) return;

        AnimationManager manager = AnimationManager.getInstance();
        String action = args[0].toLowerCase();

        switch (action) {
            case "play":
                if (args.length >= 2) {
                    String name = args[1];
                    int duration = args.length >= 3 ? Integer.parseInt(args[2]) : 20;
                    manager.playAnimation(name, duration);
                }
                break;
            case "stop":
                if (args.length >= 2) {
                    manager.stopAnimation(args[1]);
                } else {
                    manager.stopAll();
                }
                break;
            case "list":
                // Return list of active animations
                break;
        }
    }
}

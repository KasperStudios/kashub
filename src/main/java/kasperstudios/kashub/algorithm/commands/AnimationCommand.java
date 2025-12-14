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

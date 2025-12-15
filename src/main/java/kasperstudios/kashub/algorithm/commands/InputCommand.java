package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.api.InputAPI;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Команды для работы с Input API
 * Синтаксис:
 *   input.jump - прыжок
 *   input.sneak <true/false/toggle> - присед
 *   input.sprint <true/false/toggle> - бег
 *   input.attack - атака
 *   input.use - использовать предмет
 *   input.drop [stack] - выбросить предмет
 *   input.hotbar <slot> - выбрать слот
 *   input.look <yaw> <pitch> - повернуть камеру
 *   input.lookAt <x> <y> <z> - посмотреть на координаты
 *   input.move <direction> <true/false> - движение
 *   input.stop - остановить всё
 */
public class InputCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "input";
    }

    @Override
    public String getDescription() {
        return "High-level player input control";
    }

    @Override
    public String getParameters() {
        return "<action> [args] - jump/sneak/sprint/attack/use/drop/hotbar/look/lookAt/move/stop";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            printHelp();
            return;
        }

        InputAPI input = InputAPI.getInstance();
        String action = args[0].toLowerCase();

        switch (action) {
            case "jump":
                input.jump();
                break;

            case "sneak":
                if (args.length > 1) {
                    String value = args[1].toLowerCase();
                    if (value.equals("toggle")) {
                        input.toggleSneak();
                    } else {
                        input.sneak(Boolean.parseBoolean(value));
                    }
                } else {
                    input.toggleSneak();
                }
                break;

            case "sprint":
                if (args.length > 1) {
                    String value = args[1].toLowerCase();
                    if (value.equals("toggle")) {
                        input.toggleSprint();
                    } else {
                        input.sprint(Boolean.parseBoolean(value));
                    }
                } else {
                    input.toggleSprint();
                }
                break;

            case "attack":
                input.attack();
                break;

            case "use":
            case "useitem":
                if (args.length > 1 && args[1].equalsIgnoreCase("offhand")) {
                    input.useItemOffhand();
                } else {
                    input.useItem();
                }
                break;

            case "drop":
                if (args.length > 1 && args[1].equalsIgnoreCase("stack")) {
                    input.dropStack();
                } else {
                    input.dropItem();
                }
                break;

            case "swap":
            case "swaphands":
                input.swapHands();
                break;

            case "hotbar":
            case "slot":
                if (args.length > 1) {
                    String slotArg = args[1].toLowerCase();
                    if (slotArg.equals("next")) {
                        input.nextSlot();
                    } else if (slotArg.equals("prev") || slotArg.equals("previous")) {
                        input.prevSlot();
                    } else {
                        int slot = Integer.parseInt(args[1]);
                        input.selectHotbarSlot(slot);
                    }
                }
                break;

            case "look":
                if (args.length >= 3) {
                    float yaw = Float.parseFloat(args[1]);
                    float pitch = Float.parseFloat(args[2]);
                    input.look(yaw, pitch);
                }
                break;

            case "lookat":
                if (args.length >= 4) {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    input.lookAt(x, y, z);
                }
                break;

            case "lookrelative":
                if (args.length >= 3) {
                    float deltaYaw = Float.parseFloat(args[1]);
                    float deltaPitch = Float.parseFloat(args[2]);
                    input.lookRelative(deltaYaw, deltaPitch);
                }
                break;

            case "move":
                if (args.length >= 2) {
                    String direction = args[1].toLowerCase();
                    boolean enabled = args.length < 3 || Boolean.parseBoolean(args[2]);
                    
                    switch (direction) {
                        case "forward": input.moveForward(enabled); break;
                        case "back": case "backward": input.moveBack(enabled); break;
                        case "left": input.moveLeft(enabled); break;
                        case "right": input.moveRight(enabled); break;
                    }
                }
                break;

            case "stop":
                input.stopMovement();
                break;

            case "hold":
                if (args.length >= 2) {
                    String holdAction = args[1].toLowerCase();
                    boolean hold = args.length < 3 || Boolean.parseBoolean(args[2]);
                    
                    switch (holdAction) {
                        case "attack": input.holdAttack(hold); break;
                        case "use": input.holdUse(hold); break;
                    }
                }
                break;

            default:
                printHelp();
        }
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            execute(args);
            
            // Для некоторых действий добавляем небольшую задержку
            if (args.length > 0) {
                String action = args[0].toLowerCase();
                if (action.equals("attack") || action.equals("use")) {
                    scheduler.schedule(() -> {
                        InputAPI.getInstance().holdAttack(false);
                        InputAPI.getInstance().holdUse(false);
                        future.complete(null);
                    }, 100, TimeUnit.MILLISECONDS);
                    return future;
                }
            }
            
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    private void printHelp() {
        System.out.println("Input API commands:");
        System.out.println("  input jump - make player jump");
        System.out.println("  input sneak <true/false/toggle> - sneak control");
        System.out.println("  input sprint <true/false/toggle> - sprint control");
        System.out.println("  input attack - attack once");
        System.out.println("  input use [offhand] - use item");
        System.out.println("  input drop [stack] - drop item");
        System.out.println("  input swap - swap hands");
        System.out.println("  input hotbar <0-8/next/prev> - select slot");
        System.out.println("  input look <yaw> <pitch> - set camera angle");
        System.out.println("  input lookAt <x> <y> <z> - look at position");
        System.out.println("  input lookRelative <dYaw> <dPitch> - relative look");
        System.out.println("  input move <forward/back/left/right> [true/false]");
        System.out.println("  input hold <attack/use> [true/false]");
        System.out.println("  input stop - stop all movement");
    }
}

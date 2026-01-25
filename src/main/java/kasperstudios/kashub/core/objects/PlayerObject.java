package kasperstudios.kashub.core.objects;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.services.PathfindingService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * PlayerObject - OO wrapper for player functionality.
 * Usage: player.moveTo(100, 64, 100)
 */
public class PlayerObject extends Value {

    public PlayerObject() {
        super(createMethods());
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("moveTo", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3)
                return Value.FALSE;

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.FALSE;

            double x = args.get(0).asDouble();
            double y = args.get(1).asDouble();
            double z = args.get(2).asDouble();
            float radius = args.size() > 3 ? (float) args.get(3).asDouble() : 1.0f;

            // Check if coordinates are relative
            boolean relative = args.size() > 4 && args.get(4).asBoolean();

            if (relative) {
                net.minecraft.util.math.Vec3d pos = player.getPos();
                x += pos.x;
                y += pos.y;
                z += pos.z;
            }

            // Use PathfindingService for A* navigation
            PathfindingService.getInstance().navigateTo(x, y, z, radius);
            return Value.TRUE;
        }));

        methods.put("stopMove", Value.of((Callable) (ctx, args) -> {
            PathfindingService.getInstance().stop();
            return Value.TRUE;
        }));

        // Alias for backward compatibility
        methods.put("stopMoving", Value.of((Callable) (ctx, args) -> {
            PathfindingService.getInstance().stop();
            return Value.TRUE;
        }));

        methods.put("isMoveActive", Value.of((Callable) (ctx, args) -> {
            return Value.of(PathfindingService.getInstance().isActive());
        }));

        methods.put("moveBy", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3)
                return Value.FALSE;

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.FALSE;

            double dx = args.get(0).asDouble();
            double dy = args.get(1).asDouble();
            double dz = args.get(2).asDouble();
            float radius = args.size() > 3 ? (float) args.get(3).asDouble() : 1.0f;

            // Get current position and add offset
            net.minecraft.util.math.Vec3d pos = player.getPos();
            double x = pos.x + dx;
            double y = pos.y + dy;
            double z = pos.z + dz;

            PathfindingService.getInstance().navigateTo(x, y, z, radius);
            return Value.TRUE;
        }));

        methods.put("input", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 2)
                return Value.FALSE;

            String action = args.get(0).asString().toLowerCase();
            String type = args.get(1).asString().toLowerCase();

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || player.input == null)
                return Value.FALSE;

            // Actions: forward, back, left, right, jump, sneak, sprint, attack, use
            // Types: press (hold), release, tap (single press)

            boolean value = type.equals("press") || type.equals("tap");

            switch (action) {
                case "forward":
                case "w":
                    player.input.pressingForward = value;
                    if (value)
                        player.input.movementForward = 1.0f;
                    else
                        player.input.movementForward = 0.0f;
                    break;

                case "back":
                case "s":
                    player.input.pressingBack = value;
                    if (value)
                        player.input.movementForward = -1.0f;
                    else
                        player.input.movementForward = 0.0f;
                    break;

                case "left":
                case "a":
                    player.input.pressingLeft = value;
                    if (value)
                        player.input.movementSideways = -1.0f;
                    else
                        player.input.movementSideways = 0.0f;
                    break;

                case "right":
                case "d":
                    player.input.pressingRight = value;
                    if (value)
                        player.input.movementSideways = 1.0f;
                    else
                        player.input.movementSideways = 0.0f;
                    break;

                case "jump":
                case "space":
                    player.input.jumping = value;
                    break;

                case "sneak":
                case "shift":
                    player.input.sneaking = value;
                    break;

                case "sprint":
                    player.setSprinting(value);
                    break;

                case "attack":
                case "leftclick":
                    if (value && client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr) {
                        net.minecraft.entity.Entity target = ehr.getEntity();
                        client.interactionManager.attackEntity(player, target);
                        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                    break;

                case "use":
                case "rightclick":
                    if (value) {
                        client.interactionManager.interactItem(player, net.minecraft.util.Hand.MAIN_HAND);
                    }
                    break;

                default:
                    return Value.FALSE;
            }

            // Handle tap - release after a short delay
            if (type.equals("tap")) {
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                        client.execute(() -> {
                            switch (action) {
                                case "forward":
                                case "w":
                                    player.input.pressingForward = false;
                                    player.input.movementForward = 0.0f;
                                    break;
                                case "back":
                                case "s":
                                    player.input.pressingBack = false;
                                    player.input.movementForward = 0.0f;
                                    break;
                                case "left":
                                case "a":
                                    player.input.pressingLeft = false;
                                    player.input.movementSideways = 0.0f;
                                    break;
                                case "right":
                                case "d":
                                    player.input.pressingRight = false;
                                    player.input.movementSideways = 0.0f;
                                    break;
                                case "jump":
                                case "space":
                                    player.input.jumping = false;
                                    break;
                                case "sneak":
                                case "shift":
                                    player.input.sneaking = false;
                                    break;
                            }
                        });
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }).start();
            }

            return Value.TRUE;
        }));

        methods.put("attack", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.FALSE;

            net.minecraft.entity.Entity target = null;
            boolean skipReachCheck = false;

            // If entity object passed as argument, use it
            if (!args.isEmpty()) {
                Value entityValue = args.get(0);

                // Check if it's a map (entity object from vision.nearest)
                if (entityValue.getValue() instanceof Map) {
                    Map<?, ?> entityData = (Map<?, ?>) entityValue.getValue();

                    // Try to get _entity field
                    if (entityData.containsKey("_entity")) {
                        Object entityObj = entityData.get("_entity");

                        // Handle both Value-wrapped and direct Entity
                        if (entityObj instanceof Value) {
                            Object unwrapped = ((Value) entityObj).getValue();
                            if (unwrapped instanceof net.minecraft.entity.Entity) {
                                target = (net.minecraft.entity.Entity) unwrapped;
                                skipReachCheck = true;
                            }
                        } else if (entityObj instanceof net.minecraft.entity.Entity) {
                            target = (net.minecraft.entity.Entity) entityObj;
                            skipReachCheck = true;
                        }
                    }
                }
            }

            // Otherwise use crosshair target
            if (target == null && client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr) {
                target = ehr.getEntity();
            }

            if (target != null) {
                // Only check reach if using crosshair target
                if (!skipReachCheck
                        && !kasperstudios.kashub.core.fairplay.FairPlayGuard.validateEntityReach(player, target)) {
                    return Value.FALSE;
                }
                client.interactionManager.attackEntity(player, target);
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                return Value.TRUE;
            }

            return Value.FALSE;
        }));

        // ... existing getPos/getHealth ...
        methods.put("getPos", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.NULL;

            Map<String, Value> pos = new HashMap<>();
            pos.put("x", Value.of(player.getX()));
            pos.put("y", Value.of(player.getY()));
            pos.put("z", Value.of(player.getZ()));
            return Value.of(pos);
        }));

        methods.put("getHealth", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.of(0);
            return Value.of(player.getHealth());
        }));

        methods.put("getHunger", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.of(0);
            return Value.of(player.getHungerManager().getFoodLevel());
        }));

        methods.put("getName", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.of("");
            return Value.of(player.getName().getString());
        }));

        methods.put("breakBlock", Value.of((Callable) (ctx, args) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
                net.minecraft.util.math.BlockPos pos = blockHit.getBlockPos();
                net.minecraft.util.math.Direction direction = blockHit.getSide();

                if (!kasperstudios.kashub.core.fairplay.FairPlayGuard.validateBlockReach(player, pos)) {
                    return Value.FALSE;
                }

                client.interactionManager.attackBlock(pos, direction);
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("placeBlock", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            String blockName = args.get(0).asString().toLowerCase();

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null || client.interactionManager == null)
                return Value.FALSE;

            // Find block in inventory
            net.minecraft.entity.player.PlayerInventory inv = player.getInventory();
            int foundSlot = -1;

            for (int i = 0; i < inv.size(); i++) {
                net.minecraft.item.ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains(blockName)) {
                    foundSlot = i;
                    break;
                }
            }

            if (foundSlot == -1)
                return Value.FALSE;

            // Save and switch to slot
            int originalSlot = inv.selectedSlot;
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selectedSlot = foundSlot;
            } else {
                // Swap to hotbar
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, foundSlot, 0,
                        net.minecraft.screen.slot.SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, 36 + originalSlot, 0,
                        net.minecraft.screen.slot.SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(player.currentScreenHandler.syncId, foundSlot, 0,
                        net.minecraft.screen.slot.SlotActionType.PICKUP, player);
            }

            // Place block
            if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
                net.minecraft.util.math.BlockPos pos = blockHit.getBlockPos();
                net.minecraft.util.math.Direction direction = blockHit.getSide();

                if (!kasperstudios.kashub.core.fairplay.FairPlayGuard.validateBlockReach(player, pos)) {
                    if (foundSlot >= 0 && foundSlot <= 8) {
                        inv.selectedSlot = originalSlot;
                    }
                    return Value.FALSE;
                }

                client.interactionManager.interactBlock(player, net.minecraft.util.Hand.MAIN_HAND, blockHit);
                player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                // Restore slot
                if (foundSlot >= 0 && foundSlot <= 8) {
                    inv.selectedSlot = originalSlot;
                }
                return Value.TRUE;
            }

            // Restore slot
            if (foundSlot >= 0 && foundSlot <= 8) {
                inv.selectedSlot = originalSlot;
            }
            return Value.FALSE;
        }));

        methods.put("sprint", Value.of((Callable) (ctx, args) -> {
            boolean enable = !args.isEmpty() && args.get(0).asBoolean();
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null)
                return Value.FALSE;
            player.setSprinting(enable);
            return Value.TRUE;
        }));

        methods.put("animation", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            String action = args.get(0).asString();
            kasperstudios.kashub.network.AnimationManager mgr = kasperstudios.kashub.network.AnimationManager
                    .getInstance();

            if (action.equals("play") && args.size() >= 2) {
                String name = args.get(1).asString();
                int duration = args.size() > 2 ? args.get(2).asInt() : 20;
                mgr.playAnimation(name, duration);
                return Value.TRUE;
            } else if (action.equals("stop")) {
                if (args.size() >= 2)
                    mgr.stopAnimation(args.get(1).asString());
                else
                    mgr.stopAll();
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("lookAt", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3)
                return Value.FALSE;
            double x = args.get(0).asDouble();
            double y = args.get(1).asDouble();
            double z = args.get(2).asDouble();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES,
                        new net.minecraft.util.math.Vec3d(x, y, z));
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("chat", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty())
                return Value.FALSE;
            String message = args.get(0).asString();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.networkHandler != null) {
                client.player.networkHandler.sendChatMessage(message);
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        return methods;
    }
}

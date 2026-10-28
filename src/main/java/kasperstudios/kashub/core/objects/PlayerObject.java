package kasperstudios.kashub.core.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Callable;
import kasperstudios.kashub.services.PathfindingService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.InteractionHand;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.component.DataComponents;
import kasperstudios.kashub.core.ExecutionOrigin;
import java.util.concurrent.CompletableFuture;

/**
 * PlayerObject - OO wrapper for player functionality.
 * Usage: player.moveTo(100, 64, 100)
 */
public class PlayerObject extends Value implements Documentable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerObject.class);

    public PlayerObject() {
        super(createMethods());
    }

    @Override
    public Map<String, Object> getHighlighting() {
        Map<String, Object> highlights = new HashMap<>();
        String moveColor = "#C678DD"; // Purple
        String stateColor = "#98C379"; // Green
        
        highlights.put("moveTo", moveColor);
        highlights.put("moveBy", moveColor);
        highlights.put("stopMove", moveColor);
        highlights.put("isMoveActive", moveColor);
        
        highlights.put("getPos", stateColor);
        highlights.put("getHealth", stateColor);
        highlights.put("getHunger", stateColor);
        highlights.put("getName", stateColor);
        
        return highlights;
    }

    private static Map<String, Value> createMethods() {
        Map<String, Value> methods = new HashMap<>();

        methods.put("moveTo", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Player movement requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) {
                throw new SecurityException("Access Denied: Player control is disabled by server/config.");
            }
            if (args.size() < 3)
                return Value.FALSE;

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.FALSE;

            double x = args.get(0).asDouble();
            double y = args.get(1).asDouble();
            double z = args.get(2).asDouble();
            float radius = args.size() > 3 ? (float) args.get(3).asDouble() : 1.0f;

            // Check if coordinates are relative
            boolean relative = args.size() > 4 && args.get(4).asBoolean();

            if (relative) {
                net.minecraft.world.phys.Vec3 pos = player.position();
                x += pos.x;
                y += pos.y;
                z += pos.z;
            }

            // Use PathfindingService for A* navigation
            PathfindingService.getInstance().navigateTo(x, y, z, radius);
            return Value.TRUE;
        }));

        methods.put("stopMove", Value.of((Callable) (ctx, args) -> {
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) return Value.FALSE;
            PathfindingService.getInstance().stop();
            return Value.TRUE;
        }));

        methods.put("isMoveActive", Value.of((Callable) (ctx, args) -> {
            return Value.of(PathfindingService.getInstance().isActive());
        }));

        methods.put("moveBy", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3)
                return Value.FALSE;

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null)
                return Value.FALSE;

            double dx = args.get(0).asDouble();
            double dy = args.get(1).asDouble();
            double dz = args.get(2).asDouble();
            float radius = args.size() > 3 ? (float) args.get(3).asDouble() : 1.0f;

            // Get current position and add offset
            net.minecraft.world.phys.Vec3 pos = player.position();
            double x = pos.x + dx;
            double y = pos.y + dy;
            double z = pos.z + dz;

            PathfindingService.getInstance().navigateTo(x, y, z, radius);
            return Value.TRUE;
        }));

        methods.put("input", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Player input requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) {
                throw new SecurityException("Access Denied: Player input is disabled by server/config.");
            }
            if (args.size() < 2)
                return Value.FALSE;

            String action = args.get(0).asString().toLowerCase();
            String type = args.get(1).asString().toLowerCase();

            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || player.input == null)
                return Value.FALSE;

            boolean value = type.equals("press") || type.equals("tap");

            switch (action) {
                case "forward": case "w":
                    player.input.up = value;
                    player.input.forwardImpulse = value ? 1.0f : 0.0f;
                    break;
                case "back": case "s":
                    player.input.down = value;
                    player.input.forwardImpulse = value ? -1.0f : 0.0f;
                    break;
                case "left": case "a":
                    player.input.left = value;
                    player.input.leftImpulse = value ? -1.0f : 0.0f;
                    break;
                case "right": case "d":
                    player.input.right = value;
                    player.input.leftImpulse = value ? 1.0f : 0.0f;
                    break;
                case "jump": case "space":
                    player.input.jumping = value;
                    break;
                case "sneak": case "shift":
                    player.input.shiftKeyDown = value;
                    break;
                case "sprint":
                    player.setSprinting(value);
                    break;
                case "attack": case "leftclick":
                    if (value && client.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                        client.gameMode.attack(player, ehr.getEntity());
                        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                    break;
                case "use": case "rightclick":
                    if (value) {
                        client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                    break;
                default:
                    return Value.FALSE;
            }

            if (type.equals("tap")) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(50);
                        client.execute(() -> {
                            if (client.player != null && client.player.input != null) {
                                switch (action) {
                                    case "forward": case "w": player.input.up = false; player.input.forwardImpulse = 0.0f; break;
                                    case "back": case "s": player.input.down = false; player.input.forwardImpulse = 0.0f; break;
                                    case "left": case "a": player.input.left = false; player.input.leftImpulse = 0.0f; break;
                                    case "right": case "d": player.input.right = false; player.input.leftImpulse = 0.0f; break;
                                    case "jump": case "space": player.input.jumping = false; break;
                                    case "sneak": case "shift": player.input.shiftKeyDown = false; break;
                                }
                            }
                        });
                    } catch (InterruptedException ignored) {
                        LOGGER.debug("Player input release interrupted for action: {}", action);
                        Thread.currentThread().interrupt();
                    }
                });
            }

            return Value.TRUE;
        }));

        methods.put("attack", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Player interaction (attack) requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) {
                throw new SecurityException("Access Denied: Player interaction (attack) is disabled by server/config.");
            }
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null) return Value.FALSE;

            net.minecraft.world.entity.Entity target = null;
            boolean skipReachCheck = false;

            if (!args.isEmpty()) {
                Value entityValue = args.get(0);
                if (entityValue.getValue() instanceof Map) {
                    Map<?, ?> entityData = (Map<?, ?>) entityValue.getValue();
                    if (entityData.containsKey("_entity")) {
                        Object entityObj = entityData.get("_entity");
                        if (entityObj instanceof Value) entityObj = ((Value) entityObj).getValue();
                        if (entityObj instanceof net.minecraft.world.entity.Entity) {
                            target = (net.minecraft.world.entity.Entity) entityObj;
                            skipReachCheck = true;
                        }
                    }
                }
            }

            if (target == null && client.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                target = ehr.getEntity();
            }

            if (target != null && target.isAttackable()) {
                if (!skipReachCheck && !kasperstudios.kashub.core.fairplay.FairPlayGuard.validateEntityReach(player, target)) {
                    return Value.FALSE;
                }
                // Automatically face the target when attacking!
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

                client.gameMode.attack(player, target);
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("getPos", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return Value.NULL;
            Map<String, Value> pos = new HashMap<>();
            pos.put("x", Value.of(client.player.getX()));
            pos.put("y", Value.of(client.player.getY()));
            pos.put("z", Value.of(client.player.getZ()));
            return Value.of(pos);
        }));

        methods.put("getX", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getX()) : Value.of(0);
        }));

        methods.put("getY", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getY()) : Value.of(0);
        }));

        methods.put("getZ", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getZ()) : Value.of(0);
        }));

        methods.put("distanceTo", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty() || clientPlayer() == null) return Value.of(0);
            if (args.size() == 1) {
                Value arg = args.get(0);
                if (arg.getValue() instanceof Map map) {
                    Object ex = map.get("x");
                    Object ey = map.get("y");
                    Object ez = map.get("z");
                    if (ex instanceof Number nx && ey instanceof Number ny && ez instanceof Number nz) {
                        return Value.of(Math.sqrt(clientPlayer().distanceToSqr(nx.doubleValue(), ny.doubleValue(), nz.doubleValue())));
                    }
                }
            }
            if (args.size() < 3) return Value.of(0);
            double x = args.get(0).asDouble();
            double y = args.get(1).asDouble();
            double z = args.get(2).asDouble();
            return Value.of(clientPlayer().distanceToSqr(x, y, z) > 0 ? Math.sqrt(clientPlayer().distanceToSqr(x, y, z)) : 0);
        }));

        methods.put("jump", Value.of((Callable) (ctx, args) -> {
            if (clientPlayer() != null) {
                clientPlayer().jumpFromGround();
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("getHealth", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getHealth()) : Value.of(0);
        }));

        methods.put("getHunger", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getFoodData().getFoodLevel()) : Value.of(0);
        }));

        methods.put("getName", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getName().getString()) : Value.of("");
        }));

        methods.put("getYaw", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getYRot()) : Value.of(0);
        }));

        methods.put("getPitch", Value.of((Callable) (ctx, args) -> {
            return clientPlayer() != null ? Value.of(clientPlayer().getXRot()) : Value.of(0);
        }));

        methods.put("mine", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3 || clientPlayer() == null) return Value.FALSE;
            int x = args.get(0).asInt();
            int y = args.get(1).asInt();
            int z = args.get(2).asInt();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.gameMode == null) return Value.FALSE;

            // 0. Skip if already air
            if (client.level.getBlockState(pos).isAir()) return Value.TRUE;

            // 1. Look at block center
            client.player.lookAt(EntityAnchorArgument.Anchor.EYES, new net.minecraft.world.phys.Vec3(x + 0.5, y + 0.5, z + 0.5));
            
            // 2. Start breaking
            client.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.UP);
            client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            
            // 3. Wait loop
            long startTime = System.currentTimeMillis();
            while (!client.level.getBlockState(pos).isAir() && (System.currentTimeMillis() - startTime) < 8000) {
                if (ctx.shouldStop) break;
                client.gameMode.continueDestroyBlock(pos, net.minecraft.core.Direction.UP);
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
            client.gameMode.stopDestroyBlock();
            return Value.of(client.level.getBlockState(pos).isAir());
        }));

        methods.put("moveWait", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3) return Value.FALSE;
            double x = args.get(0).asDouble();
            double y = args.get(1).asDouble();
            double z = args.get(2).asDouble();
            
            kasperstudios.kashub.services.PathfindingService pf = kasperstudios.kashub.services.PathfindingService.getInstance();
            pf.navigateTo(x, y, z, 0.05f);
            
            // Wait until navigation finishes or is cancelled
            while (pf.isActive() && !ctx.shouldStop) {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
            return Value.TRUE;
        }));

        methods.put("eat", Value.of((Callable) (ctx, args) -> {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player == null || client.gameMode == null) return Value.FALSE;

            net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;

            // 1. Check offhand first
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty() && offhand.has(DataComponents.FOOD)) {
                hand = InteractionHand.OFF_HAND;
            } else {
                // 2. Search inventory
                Inventory inv = player.getInventory();
                int foodSlot = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                        foodSlot = i; break;
                    }
                }

                if (foodSlot == -1) return Value.FALSE;

                // Select food to main hand
                if (foodSlot < 9) inv.selected = foodSlot;
                else client.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, foodSlot, inv.selected, ClickType.SWAP, player);
            }

            // Eat (Hold right click)
            client.gameMode.useItem(player, hand);
            long start = System.currentTimeMillis();
            while ((System.currentTimeMillis() - start) < 2000 && player.getFoodData().needsFood()) {
                if (ctx.shouldStop) break;
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
            }
            return Value.TRUE;
        }));

        methods.put("breakBlock", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Block breaking requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) {
                throw new SecurityException("Access Denied: Player interaction (breakBlock) is disabled by server/config.");
            }
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.gameMode == null) return Value.FALSE;

            net.minecraft.core.BlockPos pos = null;
            net.minecraft.core.Direction dir = net.minecraft.core.Direction.UP;

            if (args.size() >= 3) {
                pos = new net.minecraft.core.BlockPos(args.get(0).asInt(), args.get(1).asInt(), args.get(2).asInt());
            } else if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                pos = blockHit.getBlockPos();
                dir = blockHit.getDirection();
            }

            if (pos != null) {
                if (!kasperstudios.kashub.core.fairplay.FairPlayGuard.validateBlockReach(client.player, pos)) return Value.FALSE;
                client.gameMode.startDestroyBlock(pos, dir);
                client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("interact", Value.of((Callable) (ctx, args) -> {
            if (args.size() < 3) return Value.FALSE;
            int x = args.get(0).asInt();
            int y = args.get(1).asInt();
            int z = args.get(2).asInt();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.gameMode == null) return Value.FALSE;

            client.player.lookAt(EntityAnchorArgument.Anchor.EYES, new net.minecraft.world.phys.Vec3(x + 0.5, y + 0.5, z + 0.5));
            net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(x + 0.5, y + 0.5, z + 0.5), net.minecraft.core.Direction.UP, pos, false);
            
            client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            return Value.TRUE;
        }));

        methods.put("placeBlock", Value.of((Callable) (ctx, args) -> {
            if (ctx.getOrigin() != ExecutionOrigin.USER && ctx.getOrigin() != ExecutionOrigin.SERVER) {
                throw new SecurityException("Access Denied: Block placing requires USER or SERVER origin.");
            }
            if (!kasperstudios.kashub.config.KashubConfig.getInstance().allowPlayerControl) {
                throw new SecurityException("Access Denied: Player interaction (placeBlock) is disabled by server/config.");
            }
            if (args.isEmpty()) return Value.FALSE;
            String blockName = args.get(0).asString().toLowerCase();
            
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.gameMode == null) return Value.FALSE;

            net.minecraft.world.entity.player.Inventory inv = client.player.getInventory();
            int foundSlot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i).getItem().toString().toLowerCase().contains(blockName)) {
                    foundSlot = i; break;
                }
            }
            if (foundSlot == -1) return Value.FALSE;

            net.minecraft.world.phys.BlockHitResult hit = null;
            if (args.size() >= 4) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(args.get(1).asInt(), args.get(2).asInt(), args.get(3).asInt());
                hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY(), pos.getZ()), net.minecraft.core.Direction.UP, pos, false);
            } else if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                hit = blockHit;
            }

            if (hit != null) {
                if (!kasperstudios.kashub.core.fairplay.FairPlayGuard.validateBlockReach(client.player, hit.getBlockPos())) return Value.FALSE;
                
                int originalSlot = inv.selected;
                if (foundSlot <= 8) inv.selected = foundSlot;
                else {
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, foundSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 36 + originalSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, foundSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                }

                client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                
                if (foundSlot <= 8) inv.selected = originalSlot;
                return Value.TRUE;
            }
            return Value.FALSE;
        }));

        methods.put("sprint", Value.of((Callable) (ctx, args) -> {
            if (clientPlayer() != null) clientPlayer().setSprinting(!args.isEmpty() && args.get(0).asBoolean());
            return Value.TRUE;
        }));

        methods.put("lookAt", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty() || clientPlayer() == null) return Value.FALSE;
            if (args.size() == 1) {
                Value arg = args.get(0);
                if (arg.getValue() instanceof Map map) {
                    if (map.containsKey("_entity")) {
                        Object eo = map.get("_entity");
                        if (eo instanceof Value) eo = ((Value) eo).getValue();
                        if (eo instanceof net.minecraft.world.entity.Entity ent) {
                            clientPlayer().lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, ent.getEyePosition());
                            return Value.TRUE;
                        }
                    }
                    Object ex = map.get("x");
                    Object ey = map.get("y");
                    Object ez = map.get("z");
                    if (ex instanceof Number nx && ey instanceof Number ny && ez instanceof Number nz) {
                        clientPlayer().lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                                new net.minecraft.world.phys.Vec3(nx.doubleValue(), ny.doubleValue() + 1.2, nz.doubleValue()));
                        return Value.TRUE;
                    }
                }
            }
            if (args.size() < 3) return Value.FALSE;
            clientPlayer().lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    new net.minecraft.world.phys.Vec3(args.get(0).asDouble(), args.get(1).asDouble(), args.get(2).asDouble()));
            return Value.TRUE;
        }));

        methods.put("chat", Value.of((Callable) (ctx, args) -> {
            if (args.isEmpty()) return Value.FALSE;
            com.kasperstudios.khwrapper.KHWrapper.get().sendChat(args.get(0).asString());
            return Value.TRUE;
        }));

        return methods;
    }

    private static LocalPlayer clientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public String getObjectName() {
        return "player";
    }

    @Override
    public String getObjectDescription() {
        return "Represents the local player. Contains methods for movement, interaction, inventory, and status checks.";
    }

    @Override
    public Map<String, String> getMethodDescriptions() {
        Map<String, String> docs = new java.util.LinkedHashMap<>();
        docs.put("moveTo(x, y, z, [radius], [relative])", "Navigates to the specified coordinates using pathfinding.");
        docs.put("moveBy(dx, dy, dz, [radius])", "Navigates to a location relative to the current position.");
        docs.put("stopMove()", "Stops current pathfinding navigation.");
        docs.put("isMoveActive()", "Returns true if currently navigating.");
        docs.put("moveWait(x, y, z)", "Navigates to the coordinates and blocks execution until reached.");
        docs.put("input(action, type)", "Simulates player input. Action: forward/back/left/right/jump/sneak/sprint/attack/use. Type: press/tap.");
        docs.put("attack([entity])", "Attacks the specified entity, or the one currently in the crosshair.");
        docs.put("getPos()", "Returns the current position as a map {x, y, z}.");
        docs.put("getX()", "Returns current X coordinate.");
        docs.put("getY()", "Returns current Y coordinate.");
        docs.put("getZ()", "Returns current Z coordinate.");
        docs.put("distanceTo(x, y, z)", "Returns the distance from the player to the specified coordinates.");
        docs.put("jump()", "Makes the player jump.");
        docs.put("getHealth()", "Returns the player's current health.");
        docs.put("getHunger()", "Returns the player's current hunger level.");
        docs.put("getName()", "Returns the player's name.");
        docs.put("getYaw()", "Returns the head rotation yaw.");
        docs.put("getPitch()", "Returns the head rotation pitch.");
        docs.put("mine(x, y, z)", "Mines the block at the specified coordinates. Blocks execution until broken.");
        docs.put("eat()", "Automatically finds food in the inventory and eats it.");
        docs.put("breakBlock([x, y, z])", "Breaks the specified block or the one in the crosshair.");
        docs.put("interact(x, y, z)", "Interacts (right-clicks) with the block at the specified coordinates.");
        docs.put("placeBlock(blockName, [x, y, z])", "Places a block from the inventory at the target coordinates or crosshair.");
        docs.put("sprint(boolean)", "Enables or disables sprinting.");
        docs.put("lookAt(x, y, z)", "Rotates the player's head to look at the exact coordinates.");
        docs.put("chat(message)", "Sends a chat message or command.");
        return docs;
    }
}

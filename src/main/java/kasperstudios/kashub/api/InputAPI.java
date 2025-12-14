package kasperstudios.kashub.api;

import kasperstudios.kashub.mixin.KeyBindingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class InputAPI {
    private static InputAPI instance;
    private final MinecraftClient client;

    private InputAPI() {
        this.client = MinecraftClient.getInstance();
    }

    public static InputAPI getInstance() {
        if (instance == null) {
            instance = new InputAPI();
        }
        return instance;
    }

    public void jump() {
        client.execute(() -> {
            if (client.player != null && client.player.isOnGround()) {
                client.player.jump();
            }
        });
    }

    public void setSneak(boolean sneak) {
        client.execute(() -> setKeyPressed(client.options.sneakKey, sneak));
    }

    public void toggleSneak() {
        client.execute(() -> {
            KeyBinding key = client.options.sneakKey;
            setKeyPressed(key, !isKeyPressed(key));
        });
    }

    public void setSprint(boolean sprint) {
        client.execute(() -> {
            setKeyPressed(client.options.sprintKey, sprint);
            if (client.player != null) {
                client.player.setSprinting(sprint);
            }
        });
    }

    public void setForward(boolean forward) {
        client.execute(() -> setKeyPressed(client.options.forwardKey, forward));
    }

    public void setBack(boolean back) {
        client.execute(() -> setKeyPressed(client.options.backKey, back));
    }

    public void setLeft(boolean left) {
        client.execute(() -> setKeyPressed(client.options.leftKey, left));
    }

    public void setRight(boolean right) {
        client.execute(() -> setKeyPressed(client.options.rightKey, right));
    }

    public void stopMovement() {
        client.execute(() -> {
            setKeyPressed(client.options.forwardKey, false);
            setKeyPressed(client.options.backKey, false);
            setKeyPressed(client.options.leftKey, false);
            setKeyPressed(client.options.rightKey, false);
            setKeyPressed(client.options.jumpKey, false);
            setKeyPressed(client.options.sneakKey, false);
            setKeyPressed(client.options.sprintKey, false);
        });
    }

    public void attack() {
        client.execute(() -> {
            if (client.interactionManager != null && client.player != null && client.targetedEntity != null) {
                client.interactionManager.attackEntity(client.player, client.targetedEntity);
            }
        });
    }

    public void use() {
        client.execute(() -> {
            if (client.interactionManager != null && client.player != null) {
                client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
            }
        });
    }

    public void holdAttack(boolean hold) {
        client.execute(() -> setKeyPressed(client.options.attackKey, hold));
    }

    public void holdUse(boolean hold) {
        client.execute(() -> setKeyPressed(client.options.useKey, hold));
    }

    public void swapHands() {
        client.execute(() -> {
            if (client.player != null && client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ORIGIN,
                    Direction.DOWN
                ));
            }
        });
    }

    public void dropItem() {
        client.execute(() -> {
            if (client.player != null) {
                client.player.dropSelectedItem(false);
            }
        });
    }

    public void dropStack() {
        client.execute(() -> {
            if (client.player != null) {
                client.player.dropSelectedItem(true);
            }
        });
    }

    public void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        client.execute(() -> {
            if (client.player != null) {
                client.player.getInventory().selectedSlot = slot;
            }
        });
    }

    public void nextHotbarSlot() {
        client.execute(() -> {
            if (client.player != null) {
                int current = client.player.getInventory().selectedSlot;
                client.player.getInventory().selectedSlot = (current + 1) % 9;
            }
        });
    }

    public void previousHotbarSlot() {
        client.execute(() -> {
            if (client.player != null) {
                int current = client.player.getInventory().selectedSlot;
                client.player.getInventory().selectedSlot = (current + 8) % 9;
            }
        });
    }

    public void setLook(float yaw, float pitch) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.setYaw(yaw);
                client.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
            }
        });
    }

    public void lookAt(double x, double y, double z) {
        client.execute(() -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            Vec3d playerPos = player.getEyePos();
            double dx = x - playerPos.x;
            double dy = y - playerPos.y;
            double dz = z - playerPos.z;

            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));

            player.setYaw(yaw);
            player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
        });
    }

    public void rotateLook(float deltaYaw, float deltaPitch) {
        client.execute(() -> {
            if (client.player != null) {
                float newYaw = client.player.getYaw() + deltaYaw;
                float newPitch = MathHelper.clamp(client.player.getPitch() + deltaPitch, -90.0f, 90.0f);
                client.player.setYaw(newYaw);
                client.player.setPitch(newPitch);
            }
        });
    }

    // Alias methods for compatibility
    public void sneak(boolean enabled) { setSneak(enabled); }
    public void sprint(boolean enabled) { setSprint(enabled); }
    public void toggleSprint() {
        client.execute(() -> {
            KeyBinding key = client.options.sprintKey;
            setKeyPressed(key, !isKeyPressed(key));
        });
    }
    public void useItem() { use(); }
    public void useItemOffhand() { use(); }
    public void nextSlot() { nextHotbarSlot(); }
    public void prevSlot() { previousHotbarSlot(); }
    public void look(float yaw, float pitch) { setLook(yaw, pitch); }
    public void lookRelative(float deltaYaw, float deltaPitch) { rotateLook(deltaYaw, deltaPitch); }
    public void moveForward(boolean enabled) { setForward(enabled); }
    public void moveBack(boolean enabled) { setBack(enabled); }
    public void moveLeft(boolean enabled) { setLeft(enabled); }
    public void moveRight(boolean enabled) { setRight(enabled); }

    private void setKeyPressed(KeyBinding key, boolean pressed) {
        ((KeyBindingMixin) key).setPressed(pressed);
    }

    private boolean isKeyPressed(KeyBinding key) {
        return ((KeyBindingMixin) key).isPressed();
    }
}

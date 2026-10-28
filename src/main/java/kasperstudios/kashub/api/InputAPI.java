package kasperstudios.kashub.api;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class InputAPI {
    private static InputAPI instance;
    private final Minecraft client;

    private InputAPI() {
        this.client = Minecraft.getInstance();
    }

    public static InputAPI getInstance() {
        if (instance == null) {
            instance = new InputAPI();
        }
        return instance;
    }

    public void jump() {
        client.execute(() -> {
            if (client.player != null && client.player.onGround()) {
                client.player.jumpFromGround();
            }
        });
    }

    public void setSneak(boolean sneak) {
        client.execute(() -> setKeyPressed(client.options.keyShift, sneak));
    }

    public void toggleSneak() {
        client.execute(() -> {
            KeyMapping key = client.options.keyShift;
            setKeyPressed(key, !isKeyPressed(key));
        });
    }

    public void setSprint(boolean sprint) {
        client.execute(() -> {
            setKeyPressed(client.options.keySprint, sprint);
            if (client.player != null) {
                client.player.setSprinting(sprint);
            }
        });
    }

    public void setForward(boolean forward) {
        client.execute(() -> setKeyPressed(client.options.keyUp, forward));
    }

    public void setBack(boolean back) {
        client.execute(() -> setKeyPressed(client.options.keyDown, back));
    }

    public void setLeft(boolean left) {
        client.execute(() -> setKeyPressed(client.options.keyLeft, left));
    }

    public void setRight(boolean right) {
        client.execute(() -> setKeyPressed(client.options.keyRight, right));
    }

    public void stopMovement() {
        client.execute(() -> {
            setKeyPressed(client.options.keyUp, false);
            setKeyPressed(client.options.keyDown, false);
            setKeyPressed(client.options.keyLeft, false);
            setKeyPressed(client.options.keyRight, false);
            setKeyPressed(client.options.keyJump, false);
            setKeyPressed(client.options.keyShift, false);
            setKeyPressed(client.options.keySprint, false);
        });
    }

    public void attack() {
        client.execute(() -> {
            if (client.gameMode != null && client.player != null && client.crosshairPickEntity != null) {
                client.gameMode.attack(client.player, client.crosshairPickEntity);
            }
        });
    }

    public void use() {
        client.execute(() -> {
            if (client.gameMode != null && client.player != null) {
                client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        });
    }

    public void holdAttack(boolean hold) {
        client.execute(() -> setKeyPressed(client.options.keyAttack, hold));
    }

    public void holdUse(boolean hold) {
        client.execute(() -> setKeyPressed(client.options.keyUse, hold));
    }

    public void swapHands() {
        client.execute(() -> {
            if (client.player != null && client.getConnection() != null) {
                client.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                    BlockPos.ZERO,
                    Direction.DOWN
                ));
            }
        });
    }

    public void dropItem() {
        client.execute(() -> {
            if (client.player != null) {
                client.player.drop(false);
            }
        });
    }

    public void dropStack() {
        client.execute(() -> {
            if (client.player != null) {
                client.player.drop(true);
            }
        });
    }

    public void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        client.execute(() -> {
            if (client.player != null) {
                client.player.getInventory().selected = slot;
            }
        });
    }

    public void nextHotbarSlot() {
        client.execute(() -> {
            if (client.player != null) {
                int current = client.player.getInventory().selected;
                client.player.getInventory().selected = (current + 1) % 9;
            }
        });
    }

    public void previousHotbarSlot() {
        client.execute(() -> {
            if (client.player != null) {
                int current = client.player.getInventory().selected;
                client.player.getInventory().selected = (current + 8) % 9;
            }
        });
    }

    public void setLook(float yaw, float pitch) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.setYRot(yaw);
                client.player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
            }
        });
    }

    public void lookAt(double x, double y, double z) {
        client.execute(() -> {
            LocalPlayer player = client.player;
            if (player == null) return;
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, new Vec3(x, y, z));
        });
    }

    public void rotateLook(float deltaYaw, float deltaPitch) {
        client.execute(() -> {
            if (client.player != null) {
                float newYaw = client.player.getYRot() + deltaYaw;
                float newPitch = Mth.clamp(client.player.getXRot() + deltaPitch, -90.0f, 90.0f);
                client.player.setYRot(newYaw);
                client.player.setXRot(newPitch);
            }
        });
    }

    public void sneak(boolean enabled) { setSneak(enabled); }
    public void sprint(boolean enabled) { setSprint(enabled); }
    public void toggleSprint() {
        client.execute(() -> {
            KeyMapping key = client.options.keySprint;
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

    private void setKeyPressed(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }

    private boolean isKeyPressed(KeyMapping key) {
        return key.isDown();
    }
}

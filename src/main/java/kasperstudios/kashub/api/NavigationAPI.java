package kasperstudios.kashub.api;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class NavigationAPI {
    private static NavigationAPI instance;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean moving = new AtomicBoolean(false);
    private BlockPos target = null;
    private float acceptableRadius = 1.0f;
    private CompletableFuture<Void> currentTask = null;

    private NavigationAPI() {
    }

    public static NavigationAPI getInstance() {
        if (instance == null) {
            instance = new NavigationAPI();
        }
        return instance;
    }

    public static void navigateTo(double x, double y, double z, float radius) {
        getInstance().startMovement(new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z)), radius);
    }

    public synchronized void startMovement(BlockPos targetPos, float radius) {
        stopMovement();
        this.target = targetPos;
        this.acceptableRadius = radius;
        this.moving.set(true);
        this.currentTask = new CompletableFuture<>();

        runMovementLoop();
    }

    public synchronized void stopMovement() {
        moving.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
        
        // Clear all movement keys
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.execute(() -> {
                client.options.keyUp.setDown(false);
                client.options.keyDown.setDown(false);
                client.options.keyLeft.setDown(false);
                client.options.keyRight.setDown(false);
            });
        }
    }

    private void runMovementLoop() {
        Runnable moveTask = new Runnable() {
            @Override
            public void run() {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    LocalPlayer player = client.player;
                    if (player == null || !moving.get() || target == null) {
                        stopMovement();
                        return;
                    }

                    Vec3 playerPos = player.position();
                    Vec3 targetVec = Vec3.atCenterOf(target);
                    double distance = playerPos.distanceTo(targetVec);

                    // Check if reached target
                    if (distance <= acceptableRadius) {
                        stopMovement();
                        return;
                    }

                    // Calculate direction
                    Vec3 direction = targetVec.subtract(playerPos).normalize();
                    
                    // Look towards target
                    double dx = targetVec.x - playerPos.x;
                    double dz = targetVec.z - playerPos.z;
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    player.setYRot(targetYaw);
                    
                    // Calculate forward/backward movement based on current yaw
                    double yaw = Math.toRadians(player.getYRot());
                    Vec3 lookDir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                    double forwardDot = direction.dot(lookDir);
                    
                    // Calculate strafe movement
                    Vec3 strafeDir = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                    double strafeDot = direction.dot(strafeDir);
                    
                    // Press keys based on direction
                    client.options.keyUp.setDown(forwardDot > 0.1);
                    client.options.keyDown.setDown(forwardDot < -0.1);
                    client.options.keyLeft.setDown(strafeDot < -0.1);
                    client.options.keyRight.setDown(strafeDot > 0.1);

                    if (moving.get()) {
                        scheduler.schedule(this, 50, TimeUnit.MILLISECONDS);
                    } else {
                        // Clear all keys when stopped
                        client.options.keyUp.setDown(false);
                        client.options.keyDown.setDown(false);
                        client.options.keyLeft.setDown(false);
                        client.options.keyRight.setDown(false);
                    }
                });
            }
        };
        scheduler.schedule(moveTask, 0, TimeUnit.MILLISECONDS);
    }

    public boolean isMoving() {
        return moving.get();
    }
}

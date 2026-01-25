package kasperstudios.kashub.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.execute(() -> {
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
            });
        }
    }

    private void runMovementLoop() {
        Runnable moveTask = new Runnable() {
            @Override
            public void run() {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    ClientPlayerEntity player = client.player;
                    if (player == null || !moving.get() || target == null) {
                        stopMovement();
                        return;
                    }

                    Vec3d playerPos = player.getPos();
                    Vec3d targetVec = Vec3d.ofCenter(target);
                    double distance = playerPos.distanceTo(targetVec);

                    // Check if reached target
                    if (distance <= acceptableRadius) {
                        stopMovement();
                        return;
                    }

                    // Calculate direction
                    Vec3d direction = targetVec.subtract(playerPos).normalize();
                    
                    // Look towards target
                    double dx = targetVec.x - playerPos.x;
                    double dz = targetVec.z - playerPos.z;
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    player.setYaw(targetYaw);
                    
                    // Calculate forward/backward movement based on current yaw
                    double yaw = Math.toRadians(player.getYaw());
                    Vec3d lookDir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
                    double forwardDot = direction.dotProduct(lookDir);
                    
                    // Calculate strafe movement
                    Vec3d strafeDir = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
                    double strafeDot = direction.dotProduct(strafeDir);
                    
                    // Press keys based on direction
                    client.options.forwardKey.setPressed(forwardDot > 0.1);
                    client.options.backKey.setPressed(forwardDot < -0.1);
                    client.options.leftKey.setPressed(strafeDot < -0.1);
                    client.options.rightKey.setPressed(strafeDot > 0.1);

                    if (moving.get()) {
                        scheduler.schedule(this, 50, TimeUnit.MILLISECONDS);
                    } else {
                        // Clear all keys when stopped
                        client.options.forwardKey.setPressed(false);
                        client.options.backKey.setPressed(false);
                        client.options.leftKey.setPressed(false);
                        client.options.rightKey.setPressed(false);
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

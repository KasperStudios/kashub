package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Command for attacking nearest mobs
 * Syntax: attack [range] [type] [count]
 */
public class AttackCommand implements Command {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicBoolean attacking = new AtomicBoolean(false);
    private static CompletableFuture<Void> currentTask = null;

    @Override
    public String getName() {
        return "attack";
    }

    @Override
    public String getDescription() {
        return "Attacks nearest mobs within specified radius";
    }

    @Override
    public String getParameters() {
        return "[range] [type] [count] - search radius, mob type (hostile/passive/player/all), attack count";
    }

    @Override
    public String getCategory() {
        return "Interaction";
    }
    
    @Override
    public String getDetailedHelp() {
        return "Performs attack action with current item.\n\n" +
               "Modes:\n" +
               "  attack       - Single attack\n" +
               "  attack once  - Single attack\n" +
               "  attack hold  - Hold attack button\n" +
               "  attack release - Release attack\n\n" +
               "Use with lookAt for targeting:\n" +
               "  lookAt entity zombie\n" +
               "  attack";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) return;

        double range = 5.0;
        String targetType = "hostile";
        int attackCount = 1;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                stopAttacking();
                return;
            }
            try {
                range = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                targetType = args[0];
            }
        }
        if (args.length > 1) {
            targetType = args[1];
        }
        if (args.length > 2) {
            try {
                attackCount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        final double finalRange = range;
        final String finalTargetType = targetType;

        LivingEntity target = findNearestTarget(player, finalRange, finalTargetType);
        if (target != null) {
            attackEntity(player, target);
        } else {
            System.out.println("Цель не найдена в радиусе " + range);
        }
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        MinecraftClient.getInstance().execute(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                ClientPlayerEntity player = client.player;

                if (player == null) {
                    future.complete(null);
                    return;
                }

                double range = 5.0;
                String targetType = "hostile";
                int attackCount = 1;

                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("stop")) {
                        stopAttacking();
                        future.complete(null);
                        return;
                    }
                    try {
                        range = Double.parseDouble(args[0]);
                    } catch (NumberFormatException e) {
                        targetType = args[0];
                    }
                }
                if (args.length > 1) {
                    targetType = args[1];
                }
                if (args.length > 2) {
                    try {
                        attackCount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }

                final double finalRange = range;
                final String finalTargetType = targetType;
                final int finalAttackCount = attackCount;

                attacking.set(true);
                currentTask = startAttacking(player, finalRange, finalTargetType, finalAttackCount);
                currentTask.thenRun(() -> future.complete(null));

            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<Void> startAttacking(ClientPlayerEntity player, double range, String targetType, int count) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        final int[] remaining = {count};

        Runnable attackTask = new Runnable() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    if (!attacking.get() || remaining[0] <= 0) {
                        attacking.set(false);
                        future.complete(null);
                        return;
                    }

                    ClientPlayerEntity p = MinecraftClient.getInstance().player;
                    if (p == null) {
                        future.complete(null);
                        return;
                    }

                    LivingEntity target = findNearestTarget(p, range, targetType);
                    if (target != null) {
                        attackEntity(p, target);
                        remaining[0]--;
                        
                        if (remaining[0] > 0 && attacking.get()) {
                            scheduler.schedule(this, 500, TimeUnit.MILLISECONDS);
                        } else {
                            attacking.set(false);
                            future.complete(null);
                        }
                    } else {
                        attacking.set(false);
                        future.complete(null);
                    }
                });
            }
        };

        scheduler.schedule(attackTask, 0, TimeUnit.MILLISECONDS);
        return future;
    }

    private LivingEntity findNearestTarget(ClientPlayerEntity player, double range, String targetType) {
        Box searchBox = player.getBoundingBox().expand(range);
        
        List<LivingEntity> entities = player.getWorld().getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> {
                if (entity == player) return false;
                if (!entity.isAlive()) return false;
                
                switch (targetType.toLowerCase()) {
                    case "hostile":
                        return entity instanceof HostileEntity;
                    case "passive":
                        return entity instanceof AnimalEntity;
                    case "player":
                        return entity instanceof PlayerEntity;
                    case "all":
                        return true;
                    default:
                        String entityName = entity.getType().getTranslationKey().toLowerCase();
                        return entityName.contains(targetType.toLowerCase());
                }
            }
        );

        return entities.stream()
            .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
            .orElse(null);
    }

    private void attackEntity(ClientPlayerEntity player, LivingEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Поворачиваемся к цели
        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        player.setYaw(yaw);
        player.setPitch(pitch);
        
        // Атакуем
        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
    }

    public static void stopAttacking() {
        attacking.set(false);
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.complete(null);
        }
    }
}

package kasperstudios.kashub.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {
    private static AnimationManager instance;
    private final Map<String, Animation> activeAnimations = new ConcurrentHashMap<>();
    private final MinecraftClient client = MinecraftClient.getInstance();

    private AnimationManager() {}

    public static AnimationManager getInstance() {
        if (instance == null) {
            instance = new AnimationManager();
        }
        return instance;
    }

    public void tick() {
        if (client.player == null) return;

        Iterator<Map.Entry<String, Animation>> it = activeAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Animation> entry = it.next();
            Animation anim = entry.getValue();
            if (anim.tick()) {
                it.remove();
            }
        }
    }

    public void playAnimation(String name, int durationTicks) {
        activeAnimations.put(name, new Animation(name, durationTicks));
    }

    public void stopAnimation(String name) {
        activeAnimations.remove(name);
    }

    public void stopAll() {
        activeAnimations.clear();
    }

    public boolean isPlaying(String name) {
        return activeAnimations.containsKey(name);
    }

    public Collection<String> getActiveAnimations() {
        return activeAnimations.keySet();
    }

    private static class Animation {
        final String name;
        int remainingTicks;

        Animation(String name, int durationTicks) {
            this.name = name;
            this.remainingTicks = durationTicks;
        }

        boolean tick() {
            remainingTicks--;
            return remainingTicks <= 0;
        }
    }
}

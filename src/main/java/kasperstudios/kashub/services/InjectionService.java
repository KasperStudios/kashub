package kasperstudios.kashub.services;

import com.kasperstudios.khwrapper.KHWrapper;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InjectionService {
    private static final InjectionService INSTANCE = new InjectionService();
    private final Map<UUID, Long> pendingInjections = new HashMap<>();
    private final Map<UUID, Integer> pendingTiers = new HashMap<>();

    public static InjectionService getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        KHWrapper.get().onServerTick(server -> tick());
    }

    public void startInjection(ServerPlayer player, int tier) {
        // 1. Sound effects (Pneumatic hit)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            net.minecraft.sounds.SoundEvents.PISTON_EXTEND, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
            net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.8f);

        // 2. Visual Effects (Flash and Shake)
        KHWrapper.get().sendPacketToPlayer(player, new NetworkingPayloads.VisualEffectPayload(
            NetworkingPayloads.VisualEffectPayload.TYPE_FLASH, 1.0f, 15));
        KHWrapper.get().sendPacketToPlayer(player, new NetworkingPayloads.VisualEffectPayload(
            NetworkingPayloads.VisualEffectPayload.TYPE_SHAKE, 0.7f, 60));

        // 3. Status Effects (Nausea/Confusion - stronger for better implants)
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100 + (tier * 40), 0, false, false));

        // 4. System Message
        String message = tier == 2 ? "§b[ NEURAL CORE V2.0 ONLINE ]" : "§b[ SYSTEM LINK ESTABLISHED ]";
        player.sendSystemMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.BOLD));
        
        String hint = tier == 2 ? "Enhanced CC:T integration unlocked. Press 'K' for interface." : "Neural link established. Press 'K' to open interface.";
        KHWrapper.get().sendPacketToPlayer(player, new NetworkingPayloads.InjectionStatusPayload(true, hint));

        // Sync packet is no longer strictly needed for NBT tier, but good to keep for client notification
        KHWrapper.get().sendPacketToPlayer(player, new NetworkingPayloads.ImplantSyncPayload(tier));
    }

    public void tick() {
        kasperstudios.kashub.services.integration.CCTBridge.getInstance().tick();
    }
}

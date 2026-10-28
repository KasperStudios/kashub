package kasperstudios.kashub.network;

import com.kasperstudios.khwrapper.IPacketContext;
import com.kasperstudios.khwrapper.IPacketPayload;
import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.network.payload.NetworkingPayloads;
import kasperstudios.kashub.services.AccessPolicy;
import kasperstudios.kashub.services.integration.CCTBridge;
import kasperstudios.kashub.util.RemoteScriptManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side and common network packet handlers.
 */
public class NetworkingHandlers {

    public static void handleScriptMgmt(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.ScriptMgmtPayload payload = (NetworkingPayloads.ScriptMgmtPayload) packet;
        context.execute(() -> {
            try {
                RemoteScriptManager.getInstance().handleMessage(payload.action(), payload.data());
            } catch (Exception e) {
                Kashub.LOGGER.error("Failed to handle script management response", e);
            }
        });
    }

    public static void handleComputerLink(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.ComputerLinkPayload payload = (NetworkingPayloads.ComputerLinkPayload) packet;
        ServerPlayer player = context.getPlayer();
        if (player == null) return;

        context.execute(() -> {
            if (AccessPolicy.getInstance().canInteractWith(player, payload.pos())) {
                if (payload.connect()) {
                    CCTBridge.getInstance().establishLink(player, payload.computerId(), payload.pos());
                } else {
                    CCTBridge.getInstance().terminateLink(player);
                }
            } else {
                player.displayClientMessage(Component.literal("§cOut of range or no access."), false);
            }
        });
    }

    public static void handleCCScriptPush(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCScriptPushPayload payload = (NetworkingPayloads.CCScriptPushPayload) packet;
        ServerPlayer player = context.getPlayer();
        if (player == null) return;
        context.execute(() -> CCTBridge.getInstance().handleFilePush(player, payload.computerId(), payload.filename(), payload.content()));
    }

    public static void handleCCTerminalInput(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCTerminalInputPayload payload = (NetworkingPayloads.CCTerminalInputPayload) packet;
        ServerPlayer player = context.getPlayer();
        if (player == null) return;
        context.execute(() -> CCTBridge.getInstance().handleTerminalInput(player, payload.computerId(), payload.input()));
    }

    public static void handleCCFileReadRequest(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.CCFileReadRequestPayload payload = (NetworkingPayloads.CCFileReadRequestPayload) packet;
        ServerPlayer player = context.getPlayer();
        if (player == null) return;
        context.execute(() -> CCTBridge.getInstance().handleFileReadRequest(player, payload.computerId(), payload.filename()));
    }

    public static void handlePeripheralScanRequest(IPacketPayload packet, IPacketContext context) {
        NetworkingPayloads.PeripheralScanRequestPayload payload = (NetworkingPayloads.PeripheralScanRequestPayload) packet;
        ServerPlayer player = context.getPlayer();
        if (player == null) return;
        Kashub.LOGGER.info("NetworkingHandlers: Received C2S Scan Request for {} from {}", payload.pos(), player.getScoreboardName());
        context.execute(() -> CCTBridge.getInstance().handlePeripheralScanRequest(player, payload.pos()));
    }
}

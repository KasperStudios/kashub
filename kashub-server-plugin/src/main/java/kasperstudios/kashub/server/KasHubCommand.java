package kasperstudios.kashub.server;

import kasperstudios.kashub.server.dto.KasHubClientInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KasHubCommand implements CommandExecutor {
    private final KasHubServerPlugin plugin;

    public KasHubCommand(KasHubServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "KasHub Server Control " + ChatColor.GRAY + "v"
                    + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Usage: /kashub <reload | info <player>>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("kashub.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }

            plugin.getConfigManager().loadConfig();
            sender.sendMessage(ChatColor.GREEN + "[KasHub] Configuration reloaded.");

            // Resend config to all online players
            int count = 0;
            // Need access to HandshakeListener's send method or logic
            // Ideally we should have a public method in Plugin or Manager to send config to
            // all.
            // But HandshakeListener has the sendConfig method.
            // Let's iterate players.
            // Use reflection or just register the listener as a service/manager?
            // In KasHubServerPlugin I didn't expose HandshakeListener. I should.
            // Let's assume I fix KasHubServerPlugin to expose it or just use a new instance
            // to format packet? No, packet logic is there.
            // I'll update KasHubServerPlugin to expose HandshakeListener or add a helper
            // there.

            // For now, I will use a placeholder or assume I update main class.
            // Since I am writing this file now, I cannot reference a method that doesn't
            // exist.
            // I should update KasHubServerPlugin.java later to add getHandshakeListener().

            // Or better: The sending logic (VarInt + Bytes) should be in a utility or
            // ConfigManager itself?
            // "ConfigManager.sendConfig(Player)"?
            // HandshakeListener does `sendStringPayload`.
            // I'll duplicate `sendStringPayload` here for simplicity or make a Utils class.
            // Or just update KasHubServerPlugin to have `broadcastConfig`.

            // I'll stick to updating KasHubServerPlugin to expose `HandshakeListener`
            // getter.

            if (plugin.getHandshakeListener() != null) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    plugin.getHandshakeListener().sendConfig(p);
                    count++;
                }
            }

            sender.sendMessage(ChatColor.GREEN + "[KasHub] Sent updated config to " + count + " players.");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("kashub.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /kashub info <player>");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            if (plugin.getHandshakeListener() == null) {
                sender.sendMessage(ChatColor.RED + "Internal Error: HandshakeListener not initialized.");
                return true;
            }

            KasHubClientInfo info = plugin.getHandshakeListener().getClientInfo(target.getUniqueId());
            if (info == null) {
                sender.sendMessage(ChatColor.YELLOW + "Player " + target.getName()
                        + " does not have KasHub installed or hasn't completed handshake.");
            } else {
                sender.sendMessage(ChatColor.AQUA + "--- KasHub Client Info: " + target.getName() + " ---");
                sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + info.getVersion());
                sender.sendMessage(ChatColor.GRAY + "Last Seen: " + ChatColor.WHITE
                        + ((System.currentTimeMillis() - info.getLastSeen()) / 1000) + "s ago");
                sender.sendMessage(ChatColor.GRAY + "Capabilities: " + ChatColor.WHITE
                        + (info.getCapabilities() != null ? String.join(", ", info.getCapabilities()) : "None"));

                // Show current mode for them?
                // It depends on current config.
                kasperstudios.kashub.server.dto.ServerConfig cfg = plugin.getConfigManager().getServerConfig();
                sender.sendMessage(ChatColor.GRAY + "Effective Mode: " + ChatColor.GOLD + cfg.getMode());
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command.");
        return true;
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.BugReporter;
import kasperstudios.kashub.util.ScriptLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class ReportBugCommand implements Command {

    @Override
    public String getName() {
        return "reportBug";
    }

    @Override
    public String getDescription() {
        return "Report bugs to developers with latest logs";
    }

    @Override
    public String getParameters() {
        return "<description> - Brief description of the bug";
    }

    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public String getDetailedHelp() {
        return "Report bugs to developers with latest logs.\n\n" +
               "Usage:\n" +
               "  reportBug <description>\n\n" +
               "Description:\n" +
               "  Sends a bug report to developers including:\n" +
               "  - Your description of the bug\n" +
               "  - Latest 500 lines of logs\n" +
               "  - System information (OS, Java, Minecraft version)\n" +
               "  - Timestamp\n\n" +
               "Rate Limiting:\n" +
               "  - Maximum 1 report per 5 minutes per user\n" +
               "  - Prevents spam and abuse\n\n" +
               "Examples:\n" +
               "  reportBug Script crashes when using pathfind\n" +
               "  reportBug onDamage event not firing\n" +
               "  reportBug Game freezes after 10 minutes\n\n" +
               "Privacy:\n" +
               "  - Only logs and system info are sent\n" +
               "  - No personal data or world data\n" +
               "  - Username is included for follow-up\n\n" +
               "Notes:\n" +
               "  - Use clear, concise descriptions\n" +
               "  - Include steps to reproduce if possible\n" +
               "  - Check logs/latest.log for error details";
    }

    @Override
    public void execute(String[] args) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();

        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: reportBug <description>");
        }

        String description = String.join(" ", args);
        String username = client.player != null ? client.player.getName().getString() : "Unknown";

        ScriptLogger.getInstance().info("Sending bug report...");
        client.player.sendMessage(Text.literal("§7[Bug Report] Sending report..."), false);

        BugReporter.sendBugReport(description, username).thenAccept(success -> {
            client.execute(() -> {
                if (success) {
                    ScriptLogger.getInstance().info("Bug report sent successfully!");
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("§a[Bug Report] ✓ Report sent successfully! Thank you!"),
                            false
                        );
                    }
                } else {
                    ScriptLogger.getInstance().error("Failed to send bug report");
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("§c[Bug Report] ✗ Failed to send report. Check logs or try again later."),
                            false
                        );
                    }
                }
            });
        });
    }

    @Override
    public CompletableFuture<Void> executeAsync(String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            execute(args);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}

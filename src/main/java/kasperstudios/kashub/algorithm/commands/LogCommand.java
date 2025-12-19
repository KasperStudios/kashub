package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.util.ScriptLogger;

/**
 * Команда для логирования сообщений
 * Синтаксис: log [level] <message>
 */
public class LogCommand implements Command {

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public String getDescription() {
        return "Writes message to log";
    }

    @Override
    public String getParameters() {
        return "[level] <message> - level (debug/info/warn/error) and message";
    }

    @Override
    public String getCategory() {
        return "Output";
    }

    @Override
    public String getDetailedHelp() {
        return "Writes message to script log file.\n\n" +
               "Usage:\n" +
               "  log <message>           - Log with INFO level\n" +
               "  log <level> <message>   - Log with specified level\n\n" +
               "Log Levels:\n" +
               "  debug - Detailed debug information\n" +
               "  info  - General information (default)\n" +
               "  warn  - Warning messages\n" +
               "  error - Error messages\n\n" +
               "Examples:\n" +
               "  log Script started\n" +
               "  log info Player position: $player_x, $player_y, $player_z\n" +
               "  log warn Low health detected!\n" +
               "  log error Failed to find target\n" +
               "  log debug Variable value: $myVar\n\n" +
               "Notes:\n" +
               "  - Logs are saved to kashub/logs/ folder\n" +
               "  - Variables ($var) are expanded in messages\n" +
               "  - Use for debugging scripts";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Использование: log [level] <message>");
            return;
        }

        ScriptLogger logger = ScriptLogger.getInstance();
        ScriptLogger.LogLevel level = ScriptLogger.LogLevel.INFO;
        String message;

        // Проверяем, указан ли уровень логирования
        if (args.length > 1) {
            try {
                level = ScriptLogger.LogLevel.valueOf(args[0].toUpperCase());
                // Собираем сообщение из оставшихся аргументов
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]);
                    if (i < args.length - 1) sb.append(" ");
                }
                message = sb.toString();
            } catch (IllegalArgumentException e) {
                // Первый аргумент не уровень, используем всё как сообщение
                message = String.join(" ", args);
            }
        } else {
            message = args[0];
        }

        logger.log(level, message);
    }
}

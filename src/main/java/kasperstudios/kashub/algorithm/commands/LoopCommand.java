package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.ScriptInterpreter;

import java.util.concurrent.CompletableFuture;

/**
 * Команда для выполнения цикла
 * Синтаксис: loop count { commands }
 * Примечание: Эта команда обрабатывается парсером, здесь только для автокомплита
 */
public class LoopCommand implements Command {

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getDescription() {
        return "Executes commands specified number of times";
    }

    @Override
    public String getParameters() {
        return "<count> { <commands> } - number of repetitions and command block";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Использование: loop <count> { <commands> }");
            return;
        }

        try {
            int count = Integer.parseInt(args[0]);
            StringBuilder commands = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                commands.append(args[i]).append(" ");
            }
            
            String commandBlock = commands.toString().trim();
            // Убираем фигурные скобки если есть
            if (commandBlock.startsWith("{")) {
                commandBlock = commandBlock.substring(1);
            }
            if (commandBlock.endsWith("}")) {
                commandBlock = commandBlock.substring(0, commandBlock.length() - 1);
            }

            ScriptInterpreter interpreter = ScriptInterpreter.getInstance();
            for (int i = 0; i < count; i++) {
                interpreter.parseCommands(commandBlock);
            }
        } catch (NumberFormatException e) {
            System.out.println("Неверное количество повторений: " + args[0]);
        }
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

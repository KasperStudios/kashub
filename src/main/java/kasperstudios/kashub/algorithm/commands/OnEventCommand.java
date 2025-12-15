package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.events.EventManager;

/**
 * Команда для регистрации обработчика события
 * Синтаксис: onEvent <eventName> { <script> }
 */
public class OnEventCommand implements Command {

    @Override
    public String getName() {
        return "onEvent";
    }

    @Override
    public String getDescription() {
        return "Registers script to execute on event";
    }

    @Override
    public String getParameters() {
        return "<eventName> { <script> } - event name and script to execute";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Использование: onEvent <eventName> { <script> }");
            System.out.println("Доступные события: " + String.join(", ", EventManager.getInstance().getAvailableEvents()));
            return;
        }

        String eventName = args[0];
        
        // Собираем скрипт из оставшихся аргументов
        StringBuilder scriptBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            scriptBuilder.append(args[i]).append(" ");
        }
        
        String script = scriptBuilder.toString().trim();
        
        // Убираем фигурные скобки
        if (script.startsWith("{")) {
            script = script.substring(1);
        }
        if (script.endsWith("}")) {
            script = script.substring(0, script.length() - 1);
        }
        
        script = script.trim();

        if (!EventManager.getInstance().getAvailableEvents().contains(eventName)) {
            System.out.println("Неизвестное событие: " + eventName);
            System.out.println("Доступные события: " + String.join(", ", EventManager.getInstance().getAvailableEvents()));
            return;
        }

        EventManager.getInstance().registerEventScript(eventName, script);
        System.out.println("Зарегистрирован обработчик для события: " + eventName);
    }
}

package kasperstudios.kashub.algorithm.commands;

import kasperstudios.kashub.algorithm.Command;
import kasperstudios.kashub.algorithm.events.EventManager;

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
    public String getCategory() {
        return "Events";
    }

    @Override
    public String getDetailedHelp() {
        return "Registers script to execute on game events.\n\n" +
               "Usage:\n" +
               "  onEvent <eventName> { <script> }\n\n" +
               "✅ Implemented Events:\n" +
               "  onTick          - Every 20 ticks (1 second)\n" +
               "  onDamage        - Player takes damage\n" +
               "  onHeal          - Player heals\n" +
               "  onHunger        - Hunger level changes\n" +
               "  onDeath         - Player dies\n" +
               "  onChat          - Chat message received\n" +
               "  onBlockBreak    - Block broken\n" +
               "  onBlockPlace    - Block placed\n" +
               "  onAttack        - Entity attacked\n\n" +
               "⚠️ Work In Progress (WIP):\n" +
               "  onRespawn       - Player respawns (WIP)\n" +
               "  onJump          - Player jumps (WIP)\n" +
               "  onSneak         - Sneak toggled (WIP)\n" +
               "  onSprint        - Sprint toggled (WIP)\n" +
               "  onItemUse       - Item used (WIP)\n" +
               "  onInventoryChange - Inventory changed (WIP)\n\n" +
               "Event Variables:\n" +
               "  $event_*        - Event-specific data\n" +
               "  (varies by event type)\n\n" +
               "Examples:\n" +
               "  onEvent onDamage {\n" +
               "    log Took $event_damage damage!\n" +
               "    log Health: $event_health\n" +
               "  }\n\n" +
               "  onEvent onTick {\n" +
               "    log Tick: $event_tick\n" +
               "  }\n\n" +
               "Notes:\n" +
               "  - Events persist until script stops\n" +
               "  - WIP events will show warning when registered\n" +
               "  - Use sparingly to avoid lag";
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Использование: onEvent <eventName> { <script> }");
            System.out.println("Доступные события: " + String.join(", ", EventManager.getInstance().getAvailableEvents()));
            return;
        }

        String eventName = args[0];

        StringBuilder scriptBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            scriptBuilder.append(args[i]).append(" ");
        }

        String script = scriptBuilder.toString().trim();

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
            System.out.println("Реализованные: " + String.join(", ", EventManager.getInstance().getImplementedEvents()));
            return;
        }

        EventManager.getInstance().registerEventScript(eventName, script);

        if (EventManager.getInstance().isEventImplemented(eventName)) {
            System.out.println("✅ Зарегистрирован обработчик для события: " + eventName);
        } else {
            System.out.println("⚠️ Зарегистрирован обработчик для события: " + eventName + " (WIP - может не работать)");
        }
    }
}

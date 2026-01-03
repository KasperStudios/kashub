package kasperstudios.kashub.services.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;

class ScriptTaskTest {

    @Test
    void testIfExecutionOrder() {
        // Setup
        String code = "x = 1\n" +
                "if (true) {\n" +
                "    x = 2\n" +
                "}\n" +
                "x = 3";

        // Use unique task name to avoid conflicts if Singleton uses it
        ScriptTask task = new ScriptTask(1001, "test_order", code, new HashSet<>(), ScriptType.USER);
        task.parseAndQueue();

        // Run ticks
        // 1. x=1
        // 2. if -> queues x=2 at head
        // 3. x=2
        // 4. x=3

        int ticks = 0;
        while (ticks < 100 && (task.getQueuedCommands() > 0 || task.isProcessingCommand())) {
            task.tick();
            ticks++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        if (task.getLastError() != null) {
            System.err.println("Test Task Error: " + task.getLastError());
        }

        assertEquals("3", task.getVariables().get("x"),
                "Variable x should be 3 if execution order is correct (1 -> 2 -> 3)");
    }

    @Test
    void testNestedIfOrder() {
        String code = "x = 0\n" +
                "if (true) {\n" +
                "    x = 1\n" +
                "    if (true) {\n" +
                "        x = 2\n" +
                "    }\n" +
                "    x = 3\n" +
                "}\n" +
                "x = 4";

        // Order: 0 -> 1 -> 2 -> 3 -> 4.

        ScriptTask task = null;
        try {
            task = new ScriptTask(1002, "test_nested", code, new HashSet<>(), ScriptType.USER);
            task.parseAndQueue();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

        int ticks = 0;
        String history = "";

        while (ticks < 100 && (task.getQueuedCommands() > 0 || task.isProcessingCommand())) {
            task.tick();
            String val = task.getVariables().get("x");
            if (val != null && !val.equals(getLast(history))) {
                history += val + ",";
            }
            ticks++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
        assertEquals("4", task.getVariables().get("x"));
    }

    // Helper
    private String getLast(String s) {
        if (s.isEmpty())
            return "";
        String[] parts = s.split(",");
        return parts[parts.length - 1];
    }
}

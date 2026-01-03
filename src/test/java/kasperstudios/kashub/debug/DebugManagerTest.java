package kasperstudios.kashub.debug;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebugManagerTest {

    private DebugManager debugManager;

    @BeforeEach
    void setUp() {
        debugManager = DebugManager.getInstance();
        debugManager.clearAllBreakpoints();
        // Reset sessions if possible, though no public clearSessions method exists
    }

    @Test
    void testSetBreakpoints() {
        String scriptName = "test_script.kh";
        List<Integer> lines = Arrays.asList(5, 10, 15);

        debugManager.setBreakpoints(scriptName, lines);

        assertTrue(debugManager.hasBreakpoint(scriptName, 5), "Should have breakpoint at line 5");
        assertTrue(debugManager.hasBreakpoint(scriptName, 10), "Should have breakpoint at line 10");
        assertTrue(debugManager.hasBreakpoint(scriptName, 15), "Should have breakpoint at line 15");
        assertFalse(debugManager.hasBreakpoint(scriptName, 99), "Should NOT have breakpoint at line 99");

        List<Integer> storedBreakpoints = debugManager.getBreakpoints(scriptName);
        assertEquals(3, storedBreakpoints.size(), "Should store exactly 3 breakpoints");
    }

    @Test
    void testClearBreakpoints() {
        String scriptName = "test_script.kh";
        debugManager.setBreakpoints(scriptName, Arrays.asList(1, 2));
        assertTrue(debugManager.hasBreakpoint(scriptName, 1));

        debugManager.clearBreakpoints(scriptName);
        assertFalse(debugManager.hasBreakpoint(scriptName, 1), "Breakpoints should be cleared for file");
    }

    @Test
    void testBreakpointFileScoping() {
        String file1 = "file1.kh";
        String file2 = "file2.kh";

        debugManager.setBreakpoints(file1, Arrays.asList(10));
        debugManager.setBreakpoints(file2, Arrays.asList(20));

        assertTrue(debugManager.hasBreakpoint(file1, 10));
        assertFalse(debugManager.hasBreakpoint(file1, 20));

        assertTrue(debugManager.hasBreakpoint(file2, 20));
        assertFalse(debugManager.hasBreakpoint(file2, 10));
    }

    @Test
    void testShouldPause() {
        // This test is limited because shouldPause interacts with DebugSessions which
        // we can't easily mock purely here without more setup
        // But we can test the breakpoint triggering logic part if we treat the session
        // creation as a side effect

        String scriptName = "pause_test.kh";
        int scriptId = 999;
        int line = 42;

        debugManager.setBreakpoints(scriptName, Arrays.asList(line));

        // Initial check - should trigger pause (and set state to PAUSED) because
        // breakpoint exists
        boolean shouldPause = debugManager.shouldPause(scriptId, scriptName, line);

        assertTrue(shouldPause, "Should return true when hitting a breakpoint");
        assertTrue(debugManager.isPaused(scriptId), "Script ID should be in PAUSED state");
    }
}

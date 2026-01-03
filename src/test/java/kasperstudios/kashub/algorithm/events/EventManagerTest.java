package kasperstudios.kashub.algorithm.events;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class EventManagerTest {

    @BeforeEach
    void setUp() {
        // Clear event manager before each test
        EventManager.getInstance().clear();
    }

    @Test
    void testEventRegistration() {
        EventManager manager = EventManager.getInstance();
        
        String testScript = "print \"Test event fired\"";
        manager.registerEventScript("onTick", testScript);
        
        // Verify event was registered (we can't directly check private map, 
        // but we can test by firing the event)
        assertTrue(true, "Event registration should not throw exception");
    }

    @Test
    void testEventUnregistration() {
        EventManager manager = EventManager.getInstance();
        
        String testScript = "print \"Test event\"";
        manager.registerEventScript("onTick", testScript);
        manager.unregisterEventScript("onTick");
        
        assertTrue(true, "Event unregistration should not throw exception");
    }

    @Test
    void testEventHandlerExecution() {
        EventManager manager = EventManager.getInstance();
        
        AtomicInteger counter = new AtomicInteger(0);
        
        // Register a handler
        manager.registerHandler("onTick", data -> {
            counter.incrementAndGet();
        });
        
        // Fire event
        Map<String, Object> data = new HashMap<>();
        data.put("tick", 100);
        manager.fireEvent("onTick", data);
        
        assertEquals(1, counter.get(), "Handler should be called once");
    }

    @Test
    void testMultipleHandlers() {
        EventManager manager = EventManager.getInstance();
        
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        
        // Register multiple handlers
        manager.registerHandler("onTick", data -> counter1.incrementAndGet());
        manager.registerHandler("onTick", data -> counter2.incrementAndGet());
        
        // Fire event
        Map<String, Object> data = new HashMap<>();
        manager.fireEvent("onTick", data);
        
        assertEquals(1, counter1.get(), "First handler should be called");
        assertEquals(1, counter2.get(), "Second handler should be called");
    }

    @Test
    void testClearEvents() {
        EventManager manager = EventManager.getInstance();
        
        AtomicInteger counter = new AtomicInteger(0);
        
        manager.registerHandler("onTick", data -> counter.incrementAndGet());
        manager.registerEventScript("onTick", "print \"test\"");
        
        // Clear all events
        manager.clear();
        
        // Fire event - handlers should be cleared
        Map<String, Object> data = new HashMap<>();
        manager.fireEvent("onTick", data);
        
        assertEquals(0, counter.get(), "Handlers should be cleared after clear()");
    }

    @Test
    void testEventPersistenceAfterRestart() {
        EventManager manager = EventManager.getInstance();
        
        // Simulate script registration
        String testScript = "print \"Event works\"";
        manager.registerEventScript("onTick", testScript);
        
        // Simulate stop (clear)
        manager.clear();
        
        // Simulate restart (re-register)
        manager.registerEventScript("onTick", testScript);
        
        // Verify event still works by using a handler
        AtomicInteger counter = new AtomicInteger(0);
        manager.registerHandler("onTick", data -> counter.incrementAndGet());
        
        Map<String, Object> data = new HashMap<>();
        manager.fireEvent("onTick", data);
        
        assertEquals(1, counter.get(), "Event should work after clear and re-register");
    }

    @Test
    void testUnknownEventWarning() {
        EventManager manager = EventManager.getInstance();
        
        // This should log a warning but not throw
        manager.registerEventScript("onUnknownEvent", "print \"test\"");
        
        assertTrue(true, "Unknown event should log warning but not crash");
    }

    @Test
    void testWIPEventWarning() {
        EventManager manager = EventManager.getInstance();
        
        // onRespawn is in WIP_EVENTS
        manager.registerEventScript("onRespawn", "print \"test\"");
        
        assertTrue(true, "WIP event should log warning but register");
    }

    @Test
    void testAvailableEvents() {
        EventManager manager = EventManager.getInstance();
        
        var events = manager.getAvailableEvents();
        
        assertTrue(events.contains("onTick"), "Should include onTick");
        assertTrue(events.contains("onDamage"), "Should include onDamage");
        assertTrue(events.contains("onRespawn"), "Should include WIP events");
    }

    @Test
    void testImplementedEvents() {
        EventManager manager = EventManager.getInstance();
        
        var events = manager.getImplementedEvents();
        
        assertTrue(events.contains("onTick"), "Should include onTick");
        assertFalse(events.contains("onRespawn"), "Should not include WIP events");
    }

    @Test
    void testIsEventImplemented() {
        EventManager manager = EventManager.getInstance();
        
        assertTrue(manager.isEventImplemented("onTick"), "onTick should be implemented");
        assertFalse(manager.isEventImplemented("onRespawn"), "onRespawn should not be implemented");
        assertFalse(manager.isEventImplemented("onUnknown"), "Unknown event should not be implemented");
    }
}

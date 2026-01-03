package kasperstudios.kashub.api.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketProtocolTest {

    private KashubWebSocketServer server;
    private KashubWebSocketServer.WebSocketClient client;
    private ByteArrayOutputStream outputStream;
    private Gson gson;

    @BeforeEach
    void setUp() {
        server = new KashubWebSocketServer(25567);
        gson = new Gson();

        // Mock socket streams
        outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        Socket socket = new Socket();

        client = new KashubWebSocketServer.WebSocketClient(socket, inputStream, outputStream);
    }

    @Test
    void testAuthRequirement() {
        // Send set_breakpoints without auth
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "set_breakpoints");

        server.handleMessage(client, gson.toJson(msg));

        // Should have received error
        String response = getLastResponse();
        assertNotNull(response, "Should receive response");
        assertTrue(response.contains("Authentication required"), "Should deny access without auth");
        assertFalse(client.authenticated, "Client should not be authenticated");
    }

    // Helper to decode WebSocket frame from outputStream (simplified)
    private String getLastResponse() {
        try {
            byte[] bytes = outputStream.toByteArray();
            if (bytes.length == 0)
                return null;

            // Skip frame header (simplified for unit test verification)
            // Real WS frame parsing is complex, but here we just look for current output
            // which usually starts with 0x81 (text frame)

            // For simple unit testing where we control execution, we can just inspect the
            // raw bytes
            // wrote to the stream "payload"

            // The server writes: 0x81, [length], [payload]
            // We can skip the header bytes manually
            int i = 0;
            if (i < bytes.length && bytes[i] == (byte) 0x81) {
                i++; // Skip Opcode
                int len = bytes[i] & 0x7F; // unmasked length
                i++;
                if (len == 126)
                    i += 2;
                else if (len == 127)
                    i += 8;

                return new String(bytes, i, bytes.length - i, StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

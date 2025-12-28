package kasperstudios.kashub.api.server;

import kasperstudios.kashub.Kashub;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple WebSocket server for real-time communication with VSCode extension.
 * Broadcasts script output, errors, and state changes.
 */
public class KashubWebSocketServer {
    private final int port;
    private ServerSocket serverSocket;
    private final Set<WebSocketClient> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    
    public KashubWebSocketServer(int port) {
        this.port = port;
    }
    
    public void start() {
        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                Kashub.LOGGER.info("WebSocket server listening on port {}", port);
                
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        executor.submit(() -> handleConnection(socket));
                    } catch (SocketException e) {
                        if (running) {
                            Kashub.LOGGER.error("Socket error", e);
                        }
                    }
                }
            } catch (IOException e) {
                Kashub.LOGGER.error("Failed to start WebSocket server", e);
            }
        });
    }
    
    public void stop() {
        running = false;
        try {
            for (WebSocketClient client : clients) {
                client.close();
            }
            clients.clear();
            if (serverSocket != null) {
                serverSocket.close();
            }
            executor.shutdown();
        } catch (IOException e) {
            Kashub.LOGGER.error("Error stopping WebSocket server", e);
        }
    }
    
    private void handleConnection(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            
            // Read HTTP upgrade request
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            String wsKey = null;
            
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Sec-WebSocket-Key:")) {
                    wsKey = line.substring(19).trim();
                }
            }
            
            if (wsKey == null) {
                socket.close();
                return;
            }
            
            // Send WebSocket handshake response
            String acceptKey = generateAcceptKey(wsKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            
            WebSocketClient client = new WebSocketClient(socket, in, out);
            clients.add(client);
            Kashub.LOGGER.info("WebSocket client connected. Total clients: {}", clients.size());
            
            // Read messages
            while (running && !socket.isClosed()) {
                try {
                    String message = client.readMessage();
                    if (message == null) {
                        break;
                    }
                    handleMessage(client, message);
                } catch (IOException e) {
                    break;
                }
            }
            
            clients.remove(client);
            client.close();
            Kashub.LOGGER.info("WebSocket client disconnected. Total clients: {}", clients.size());
            
        } catch (Exception e) {
            Kashub.LOGGER.error("WebSocket connection error", e);
        }
    }
    
    private void handleMessage(WebSocketClient client, String message) {
        // Handle incoming messages from VSCode (if needed)
        Kashub.LOGGER.debug("Received WebSocket message: {}", message);
    }
    
    public void broadcast(String message) {
        for (WebSocketClient client : clients) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                Kashub.LOGGER.error("Error broadcasting to client", e);
                clients.remove(client);
            }
        }
    }
    
    public int getClientCount() {
        return clients.size();
    }
    
    private String generateAcceptKey(String key) {
        try {
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate WebSocket accept key", e);
        }
    }
    
    /**
     * Simple WebSocket client wrapper
     */
    private static class WebSocketClient {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        
        WebSocketClient(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }
        
        String readMessage() throws IOException {
            int firstByte = in.read();
            if (firstByte == -1) return null;
            
            boolean fin = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;
            
            if (opcode == 0x8) { // Close frame
                return null;
            }
            
            int secondByte = in.read();
            boolean masked = (secondByte & 0x80) != 0;
            int length = secondByte & 0x7F;
            
            if (length == 126) {
                length = (in.read() << 8) | in.read();
            } else if (length == 127) {
                // Skip 8 bytes for extended length (not commonly used)
                for (int i = 0; i < 8; i++) in.read();
                length = 0; // Simplified
            }
            
            byte[] mask = new byte[4];
            if (masked) {
                in.read(mask);
            }
            
            byte[] data = new byte[length];
            int read = 0;
            while (read < length) {
                int r = in.read(data, read, length - read);
                if (r == -1) break;
                read += r;
            }
            
            if (masked) {
                for (int i = 0; i < data.length; i++) {
                    data[i] ^= mask[i % 4];
                }
            }
            
            return new String(data, StandardCharsets.UTF_8);
        }
        
        synchronized void sendMessage(String message) throws IOException {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            
            out.write(0x81); // Text frame, FIN
            
            if (data.length < 126) {
                out.write(data.length);
            } else if (data.length < 65536) {
                out.write(126);
                out.write((data.length >> 8) & 0xFF);
                out.write(data.length & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((data.length >> (8 * i)) & 0xFF);
                }
            }
            
            out.write(data);
            out.flush();
        }
        
        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}

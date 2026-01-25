package kasperstudios.kashub.api.server;

import kasperstudios.kashub.Kashub;
import kasperstudios.kashub.core.TaskManager;
import kasperstudios.kashub.core.Task;
import kasperstudios.kashub.core.Value;
import kasperstudios.kashub.core.Context;
import kasperstudios.kashub.core.Interpreter;
import kasperstudios.kashub.core.Type;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import kasperstudios.kashub.debug.DebugManager;
import kasperstudios.kashub.debug.DebugEvent;

public class KashubWebSocketServer {
    private final int port;
    private ServerSocket serverSocket;
    private final Set<WebSocketClient> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    private final Gson gson = new Gson();
    private String authToken;

    private final java.util.function.Consumer<DebugEvent> debugListener = this::broadcastDebugEvent;

    public KashubWebSocketServer(int port) {
        this.port = port;
    }

    public void start() {
        executor.submit(() -> {
            try {

                this.authToken = UUID.randomUUID().toString();
                File tokenFile = new File(".kashub_token");
                try (FileWriter writer = new FileWriter(tokenFile)) {
                    writer.write(authToken);
                } catch (IOException e) {
                    Kashub.LOGGER.error("Failed to write .kashub_token", e);
                }
                Kashub.LOGGER.info("Debug server started. Token written to " + tokenFile.getAbsolutePath());

                serverSocket = new ServerSocket(port);
                running = true;

                DebugManager.getInstance().addDebugEventListener(debugListener);

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

            DebugManager.getInstance().removeDebugEventListener(debugListener);

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

    void handleMessage(WebSocketClient client, String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);

            if (!json.has("type"))
                return;

            String type = json.get("type").getAsString();

            client.authenticated = true;

            switch (type) {
                case "debug_action":
                    handleDebugAction(json);
                    break;
                case "set_breakpoints":
                    handleSetBreakpoints(json);
                    break;
                case "get_variables":
                    handleGetVariables(client, json);
                    break;
                case "set_variable":
                    handleSetVariable(client, json);
                    break;
                case "launch":
                    handleLaunch(client, json);
                    break;
                case "stackTrace":
                    handleStackTrace(client, json);
                    break;
                case "scopes":
                    handleScopes(client, json);
                    break;
            }

        } catch (JsonSyntaxException e) {
            Kashub.LOGGER.warn("Invalid JSON received via WebSocket: {}", message);
        } catch (Exception e) {
            Kashub.LOGGER.error("Error handling WebSocket message", e);
        }
    }

    private void handleSetVariable(WebSocketClient client, JsonObject json) {
        try {
            int scriptId = json.has("scriptId") ? json.get("scriptId").getAsInt() : -1;
            String name = json.get("name").getAsString();
            String value = json.get("value").getAsString();

            DebugManager.getInstance().setVariable(scriptId, name, value);

            JsonObject response = new JsonObject();
            response.addProperty("type", "setVariable_response");
            response.addProperty("success", true);
            response.addProperty("value", value);

            client.sendMessage(gson.toJson(response));
        } catch (Exception e) {
            Kashub.LOGGER.error("Error setting variable", e);
            JsonObject response = new JsonObject();
            response.addProperty("type", "setVariable_response");
            response.addProperty("success", false);
            response.addProperty("message", e.getMessage());
            try {
                client.sendMessage(gson.toJson(response));
            } catch (IOException ioException) {
            }
        }
    }

    private void handleDebugAction(JsonObject json) {
        String action = json.get("action").getAsString();
        int scriptId = json.has("scriptId") ? json.get("scriptId").getAsInt() : -1;

        System.err.println("DEBUG SERVER: Received action '" + action + "' for scriptId " + scriptId);

        DebugManager dm = DebugManager.getInstance();

        switch (action) {
            case "pause":

                break;
            case "resume":
                if (scriptId != -1)
                    dm.resume(scriptId);
                else
                    dm.resumeAll();
                break;
            case "step_over":
                if (scriptId != -1)
                    dm.stepOver(scriptId);
                break;
            case "step_into":
                if (scriptId != -1)
                    dm.stepInto(scriptId);
                break;
        }
    }

    private void handleSetBreakpoints(JsonObject json) {
        if (json.has("lines")) {
            com.google.gson.JsonArray linesArray = json.getAsJsonArray("lines");
            List<Integer> lines = new ArrayList<>();
            for (com.google.gson.JsonElement elem : linesArray) {
                lines.add(elem.getAsInt());
            }

            String scriptName = json.has("program") ? json.get("program").getAsString() : "debug_script.kh";

            DebugManager.getInstance().setBreakpoints(scriptName, lines);

            JsonObject response = new JsonObject();
            response.addProperty("type", "breakpoints_set");
            response.add("lines", linesArray);
            broadcast(gson.toJson(response));
        }
    }

    private void handleGetVariables(WebSocketClient client, JsonObject json) {
        int scriptId = json.has("scriptId") ? json.get("scriptId").getAsInt() : -1;
        if (scriptId != -1) {
            Map<String, String> vars = DebugManager.getInstance().getVariables(scriptId);

            JsonObject response = new JsonObject();
            response.addProperty("type", "variables_response");
            response.addProperty("scriptId", scriptId);
            JsonObject varObj = new JsonObject();
            vars.forEach(varObj::addProperty);
            response.add("variables", varObj);

            try {
                client.sendMessage(gson.toJson(response));
            } catch (IOException e) {

            }
        }
    }

    private void handleLaunch(WebSocketClient client, JsonObject json) {
        try {
            String program = json.has("program") ? json.get("program").getAsString() : "debug_script.kh";
            String code = json.has("code") ? json.get("code").getAsString() : null;

            if (code == null) {

                code = kasperstudios.kashub.util.ScriptManager.loadScript(program);
            }

            if (code != null) {

                kasperstudios.kashub.core.Task task = kasperstudios.kashub.core.TaskManager
                        .getInstance()
                        .startScript(program, code, null, kasperstudios.kashub.core.Type.USER);
                int taskId = task != null ? task.getId() : -1;

                JsonObject response = new JsonObject();
                response.addProperty("type", "launch_response");
                response.addProperty("success", true);
                response.addProperty("taskId", taskId);
                response.addProperty("program", program);
                client.sendMessage(gson.toJson(response));
            } else {
                JsonObject response = new JsonObject();
                response.addProperty("type", "launch_response");
                response.addProperty("success", false);
                response.addProperty("error", "Failed to load script");
                client.sendMessage(gson.toJson(response));
            }
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in handleLaunch", e);
        }
    }

    private void handleStackTrace(WebSocketClient client, JsonObject json) {
        try {
            int scriptId = json.has("scriptId") ? json.get("scriptId").getAsInt() : -1;

            JsonObject response = new JsonObject();
            response.addProperty("type", "stackTrace_response");

            if (scriptId != -1) {
                Task task = TaskManager
                        .getInstance()
                        .getTask(scriptId);

                if (task != null) {
                    com.google.gson.JsonArray frames = new com.google.gson.JsonArray();
                    JsonObject frame = new JsonObject();
                    frame.addProperty("id", 0);
                    frame.addProperty("name", task.getName());
                    frame.addProperty("line", task.getCurrentLine());
                    frame.addProperty("column", 0);
                    frames.add(frame);
                    response.add("stackFrames", frames);
                }
            }

            client.sendMessage(gson.toJson(response));
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in handleStackTrace", e);
        }
    }

    private void handleScopes(WebSocketClient client, JsonObject json) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("type", "scopes_response");

            com.google.gson.JsonArray scopes = new com.google.gson.JsonArray();

            JsonObject localScope = new JsonObject();
            localScope.addProperty("name", "Local");
            localScope.addProperty("variablesReference", 1);
            scopes.add(localScope);

            JsonObject globalScope = new JsonObject();
            globalScope.addProperty("name", "Global");
            globalScope.addProperty("variablesReference", 2);
            scopes.add(globalScope);

            response.add("scopes", scopes);
            client.sendMessage(gson.toJson(response));
        } catch (Exception e) {
            Kashub.LOGGER.error("Error in handleScopes", e);
        }
    }

    private void broadcastDebugEvent(DebugEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "debug_event");
        json.addProperty("event", event.getType().toString());
        json.addProperty("scriptId", event.getScriptId());
        json.addProperty("line", event.getLine());

        broadcast(gson.toJson(json));
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

    static class WebSocketClient {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        boolean authenticated = false;

        WebSocketClient(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        String readMessage() throws IOException {
            int firstByte = in.read();
            if (firstByte == -1)
                return null;

            boolean fin = (firstByte & 0x80) != 0;
            int opcode = firstByte & 0x0F;

            if (opcode == 0x8) {
                return null;
            }

            int secondByte = in.read();
            boolean masked = (secondByte & 0x80) != 0;
            int length = secondByte & 0x7F;

            if (length == 126) {
                length = (in.read() << 8) | in.read();
            } else if (length == 127) {

                for (int i = 0; i < 8; i++)
                    in.read();
                length = 0;
            }

            byte[] mask = new byte[4];
            if (masked) {
                in.read(mask);
            }

            byte[] data = new byte[length];
            int read = 0;
            while (read < length) {
                int r = in.read(data, read, length - read);
                if (r == -1)
                    break;
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

            out.write(0x81);

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
            } catch (IOException ignored) {
            }
        }
    }
}

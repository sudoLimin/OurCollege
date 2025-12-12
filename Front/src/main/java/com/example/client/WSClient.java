package com.example.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WSClient extends WebSocketClient {

    private static final Logger LOGGER = Logger.getLogger(WSClient.class.getName());

    private final Runnable defaultCallback;
    private final Map<String, Runnable> typeCallbacks = new HashMap<>();
    private final Map<String, Consumer<String>> typeCallbacksWithContent = new HashMap<>();

    private static final java.util.List<String> recentNotifications = new java.util.ArrayList<>();

    
    public WSClient(Runnable onUpdate) throws Exception {
        super(new URI("ws://localhost:8080/ws/notify"));
        this.defaultCallback = onUpdate;
    }

    
    public void onType(String type, Runnable callback) {
        typeCallbacks.put(type, callback);
    }

    
    public void onTypeWithContent(String type, Consumer<String> callback) {
        typeCallbacksWithContent.put(type, callback);
    }

    
    public static java.util.List<String> getRecentNotifications() {
        return new java.util.ArrayList<>(recentNotifications);
    }

    
    public static void clearRecentNotifications() {
        recentNotifications.clear();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.log(Level.INFO, "WS: connection opened");
        System.out.println("WS: connection opened");
    }

    @Override
    public void onMessage(String message) {
        LOGGER.log(Level.INFO, "WS: incoming message: {0}", message);
        System.out.println("WS: incoming message: " + message);

        String type = extractField(message, "type");
        String content = extractField(message, "content");

        if (content == null) {
            content = extractField(message, "message");
        }

        if (content != null && !content.isEmpty()) {
            recentNotifications.add(0, content);
            while (recentNotifications.size() > 50) {
                recentNotifications.remove(recentNotifications.size() - 1);
            }
        }

        if (type != null) {
            if (typeCallbacksWithContent.containsKey(type)) {
                LOGGER.log(Level.INFO, "WS: running callback with content for type: {0}", type);
                typeCallbacksWithContent.get(type).accept(content != null ? content : "");
            }
            else if (typeCallbacks.containsKey(type)) {
                LOGGER.log(Level.INFO, "WS: running callback for type: {0}", type);
                typeCallbacks.get(type).run();
            }
            else if (defaultCallback != null) {
                LOGGER.log(Level.INFO, "WS: running default callback for type: {0}", type);
                defaultCallback.run();
            }
        } else if (defaultCallback != null) {
            LOGGER.log(Level.INFO, "WS: running default callback (no type)");
            defaultCallback.run();
        }
    }

    
    private String extractField(String message, String fieldName) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        try {
            String pattern1 = "\"" + fieldName + "\":\"";
            if (message.contains(pattern1)) {
                String afterField = message.split(pattern1)[1];
                return afterField.split("\"")[0];
            }

            String pattern2 = "\"" + fieldName + "\": \"";
            if (message.contains(pattern2)) {
                String afterField = message.split(pattern2)[1];
                return afterField.split("\"")[0];
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not extract field: " + fieldName, e);
        }

        return null;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.log(Level.INFO, "WS: connection closed (code={0}, reason={1}, remote={2})",
                new Object[]{code, reason, remote});
        System.out.println("WS: connection closed (code=" + code + ", reason=" + reason + ", remote=" + remote + ")");
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.log(Level.SEVERE, "WS: error: {0}", ex.getMessage());
        System.err.println("WS: error: " + ex.getMessage());
    }
}

package com.example.client.controllers;

import com.example.client.WSClient;
import com.example.client.holders.UserHolder;
import com.example.client.holders.TaskHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationsController {

    private static final Logger LOGGER = Logger.getLogger(NotificationsController.class.getName());

    @FXML private ListView<String> notifList;

    private WSClient ws;

    @FXML
    public void initialize() {
        loadNotifications();
        try {
            ws = new WSClient(() -> Platform.runLater(this::loadNotifications));
            ws.onType("task_new", () -> Platform.runLater(this::loadNotifications));
            ws.onType("member_new", () -> Platform.runLater(this::loadNotifications));
            ws.onType("material_new", () -> Platform.runLater(this::loadNotifications));
            ws.connect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not connect WebSocket for notifications", e);
        }
    }

    private void loadNotifications() {
        if (UserHolder.userId == null) return;
        notifList.getItems().clear();

        HttpClient client = HttpClient.newHttpClient();

        // Load persisted notifications
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/notifications/" + UserHolder.userId))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body != null && body.trim().startsWith("[") && !body.trim().equals("[]")) {
                String[] parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"message\":")) {
                        String message = extractJsonValue(part, "message");
                        String readStatus = part.contains("\"read\":false") ? " [NEW]" : "";
                        String createdAt = extractJsonValue(part, "createdAt");
                        if (createdAt != null && createdAt.length() > 16) {
                            createdAt = " (" + createdAt.replace("T", " ").substring(0, 16) + ")";
                        } else if (createdAt != null) {
                            createdAt = " (" + createdAt.replace("T", " ") + ")";
                        } else {
                            createdAt = "";
                        }
                        notifList.getItems().add("📬 " + message + createdAt + readStatus);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading notifications", e);
        }

        // Load upcoming task deadlines
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/upcoming/" + UserHolder.userId))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body != null && body.trim().startsWith("[") && !body.trim().equals("[]")) {
                String[] parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"title\":")) {
                        String title = extractJsonValue(part, "title");
                        String deadline = extractJsonValue(part, "deadline");
                        if (deadline != null) {
                            deadline = deadline.replace("T", " ");
                            if (deadline.length() > 16) {
                                deadline = deadline.substring(0, 16);
                            }
                        } else {
                            deadline = "soon";
                        }
                        notifList.getItems().add("⏰ DEADLINE: " + title + " — " + deadline);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading upcoming deadlines", e);
        }

        if (notifList.getItems().isEmpty()) {
            notifList.getItems().add("No notifications or upcoming deadlines");
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;

            int valueStart = keyIndex + searchKey.length();
            // Skip whitespace
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= json.length()) return null;

            if (json.charAt(valueStart) == '"') {
                // String value
                int valueEnd = json.indexOf('"', valueStart + 1);
                if (valueEnd == -1) return null;
                return json.substring(valueStart + 1, valueEnd);
            } else if (json.charAt(valueStart) == 'n') {
                // null value
                return null;
            } else {
                // Number or boolean
                int valueEnd = valueStart;
                while (valueEnd < json.length() &&
                       (Character.isDigit(json.charAt(valueEnd)) ||
                        json.charAt(valueEnd) == '.' ||
                        json.charAt(valueEnd) == '-' ||
                        Character.isLetter(json.charAt(valueEnd)))) {
                    valueEnd++;
                }
                return json.substring(valueStart, valueEnd);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) notifList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/main.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Main Menu");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to main menu", e);
        }
    }
}

package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.WSClient;
import com.example.client.holders.TaskHolder;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatController {

    private static final Logger LOGGER = Logger.getLogger(ChatController.class.getName());

    @FXML private VBox messagesBox;
    @FXML private TextField inputField;
    @FXML private ScrollPane scrollPane;

    private WSClient ws;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "ChatController initialized for groupId: {0}", TaskHolder.groupId);

        loadMessages();

        try {
            ws = new WSClient(() -> Platform.runLater(this::loadMessages));
            ws.onType("chat_new", () -> Platform.runLater(this::loadMessages));
            ws.connect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not connect WebSocket for chat", e);
        }

        inputField.setOnAction(event -> sendMessage());
    }

    private void loadMessages() {
        if (TaskHolder.groupId == null) {
            LOGGER.log(Level.WARNING, "Cannot load messages: groupId is null");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/chat/" + TaskHolder.groupId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            LOGGER.log(Level.INFO, "Chat response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Chat response body: {0}", body);

            messagesBox.getChildren().clear();

            if (body.startsWith("[") && body.length() > 2) {
                String[] parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"content\":\"") || part.contains("\"message\":\"")) {
                        try {
                            String sender = "Unknown";
                            if (part.contains("\"senderName\":\"")) {
                                sender = part.split("\"senderName\":\"")[1].split("\"")[0];
                            } else if (part.contains("\"sender\":\"")) {
                                sender = part.split("\"sender\":\"")[1].split("\"")[0];
                            } else if (part.contains("\"userName\":\"")) {
                                sender = part.split("\"userName\":\"")[1].split("\"")[0];
                            }

                            String content = "";
                            if (part.contains("\"content\":\"")) {
                                content = part.split("\"content\":\"")[1].split("\"")[0];
                            } else if (part.contains("\"message\":\"")) {
                                content = part.split("\"message\":\"")[1].split("\"")[0];
                            } else if (part.contains("\"text\":\"")) {
                                content = part.split("\"text\":\"")[1].split("\"")[0];
                            }

                            String time = "";
                            if (part.contains("\"createdAt\":\"")) {
                                String timestamp = part.split("\"createdAt\":\"")[1].split("\"")[0];
                                if (timestamp.contains("T")) {
                                    time = timestamp.split("T")[1];
                                    if (time.contains(".")) {
                                        time = time.split("\\.")[0];
                                    }
                                }
                            } else if (part.contains("\"timestamp\":\"")) {
                                String timestamp = part.split("\"timestamp\":\"")[1].split("\"")[0];
                                if (timestamp.contains("T")) {
                                    time = timestamp.split("T")[1];
                                    if (time.contains(".")) {
                                        time = time.split("\\.")[0];
                                    }
                                }
                            }

                            addMessageToUI(sender, content, time);

                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error parsing message part", e);
                        }
                    }
                }
            }

            if (messagesBox.getChildren().isEmpty()) {
                Label noMessages = new Label("No messages yet. Start the conversation!");
                noMessages.setStyle("-fx-text-fill: #9E9E9E;");
                messagesBox.getChildren().add(noMessages);
            }

            Platform.runLater(() -> scrollPane.setVvalue(1.0));

            LOGGER.log(Level.INFO, "Loaded {0} messages", messagesBox.getChildren().size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading messages", e);
            Label errorLabel = new Label("Error loading messages.");
            errorLabel.setStyle("-fx-text-fill: #FF5252;");
            messagesBox.getChildren().add(errorLabel);
        }
    }

    private void addMessageToUI(String sender, String content, String time) {
        HBox messageRow = new HBox(5);

        boolean isMyMessage = sender.equals(UserHolder.userName);

        Label senderLabel = new Label(sender + ":");
        senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isMyMessage ? "#64B5F6;" : "#81C784;"));

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(250);
        contentLabel.setStyle("-fx-text-fill: #E0E0E0;");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9E9E9E;");

        messageRow.getChildren().addAll(senderLabel, contentLabel, timeLabel);

        if (isMyMessage) {
            messageRow.setAlignment(Pos.CENTER_RIGHT);
            messageRow.setStyle("-fx-background-color: #2D4A6D; -fx-padding: 5; -fx-background-radius: 5;");
        } else {
            messageRow.setAlignment(Pos.CENTER_LEFT);
            messageRow.setStyle("-fx-background-color: #3C3C3C; -fx-padding: 5; -fx-background-radius: 5;");
        }

        messagesBox.getChildren().add(messageRow);
    }

    @FXML
    public void sendMessage() {
        String message = inputField.getText();
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        if (TaskHolder.groupId == null) {
            LOGGER.log(Level.WARNING, "Cannot send message: groupId is null");
            ValidationUtils.showServerError("Cannot send message: no group selected.");
            return;
        }

        String sanitizedMessage = ValidationUtils.sanitize(message.trim());
        if (sanitizedMessage.isEmpty()) {
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();

            String userName = UserHolder.userName != null ? ValidationUtils.sanitize(UserHolder.userName) : "User";

            String json = "{ \"groupId\": " + TaskHolder.groupId +
                    ", \"userId\": " + UserHolder.userId +
                    ", \"senderId\": " + UserHolder.userId +
                    ", \"userName\": \"" + userName + "\"" +
                    ", \"senderName\": \"" + userName + "\"" +
                    ", \"content\": \"" + sanitizedMessage +
                    "\", \"message\": \"" + sanitizedMessage +
                    "\", \"text\": \"" + sanitizedMessage + "\" }";

            LOGGER.log(Level.INFO, "Sending message: {0}", json);

            String[] endpoints = {
                "http://localhost:8080/chat/send",
                "http://localhost:8080/chat",
                "http://localhost:8080/chat/group/" + TaskHolder.groupId,
                "http://localhost:8080/chat/" + TaskHolder.groupId + "/send",
                "http://localhost:8080/messages",
                "http://localhost:8080/messages/send"
            };

            boolean success = false;
            for (String endpoint : endpoints) {
                LOGGER.log(Level.INFO, "Trying endpoint: {0}", endpoint);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOGGER.log(Level.INFO, "Response from {0}: status={1}", new Object[]{endpoint, resp.statusCode()});

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    success = true;
                    inputField.clear();
                    loadMessages();
                    break;
                } else if (resp.statusCode() != 404) {
                    LOGGER.log(Level.WARNING, "Endpoint {0} returned error: {1}", new Object[]{endpoint, resp.body()});
                }
            }

            if (!success) {
                LOGGER.log(Level.SEVERE, "No working chat endpoint found. Backend needs to implement POST /chat/send or similar.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending message", e);
        }
    }

    @FXML
    public void back() {
        try {
            if (ws != null) {
                ws.close();
            }

            Stage stage = (Stage) messagesBox.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to groups", e);
        }
    }
}

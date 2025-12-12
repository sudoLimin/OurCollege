package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.WSClient;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MembersController {

    private static final Logger LOGGER = Logger.getLogger(MembersController.class.getName());

    @FXML private ListView<String> membersList;
    @FXML private TextField emailField;
    @FXML private Button addButton;
    @FXML private Button deleteButton;

    public static Long currentGroupId;

    private WSClient ws;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "MembersController initialized for groupId: {0}", currentGroupId);

        boolean isAdmin = UserHolder.isAdmin();
        if (!isAdmin) {
            if (addButton != null) addButton.setDisable(true);
            if (deleteButton != null) deleteButton.setDisable(true);
            if (emailField != null) emailField.setDisable(true);
        }

        membersList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item.contains("|")) {
                        setText(item.substring(item.indexOf("|") + 1));
                    } else {
                        setText(item);
                    }
                }
            }
        });

        loadMembers();

        try {
            ws = new WSClient(() -> Platform.runLater(this::loadMembers));
            ws.onType("member_new", () -> Platform.runLater(this::loadMembers));
            ws.connect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not connect WebSocket", e);
        }
    }


    private void loadMembers() {
        if (currentGroupId == null) {
            LOGGER.log(Level.WARNING, "Cannot load members: groupId is null");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/groups/" + currentGroupId + "/members"))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            LOGGER.log(Level.INFO, "Members response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Members response body: {0}", body);

            membersList.getItems().clear();

            if (body != null && body.trim().startsWith("[")) {
                String[] parts = body.split("\\{ ");
                parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"name\":")) {
                        String name = part.split("\"name\":\"")[1].split("\"")[0];
                        String email = "";
                        if (part.contains("\"email\":\"")) {
                            email = part.split("\"email\":\"")[1].split("\"")[0];
                        }
                        String idStr = null;
                        if (part.contains("\"id\":")) {
                            try {
                                idStr = part.split("\"id\":")[1].split(",")[0].trim();
                            } catch (Exception ignore) {
                            }
                        }
                        String internalKey = (idStr != null ? idStr + "|" : "");
                        String display = name + " (" + email + ")";
                        membersList.getItems().add(internalKey + display);
                    }
                }
            }

            LOGGER.log(Level.INFO, "Loaded {0} members", membersList.getItems().size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading members", e);
        }
    }

    @FXML
    public void addMember() {
        if (!UserHolder.isAdmin()) {
            ValidationUtils.showValidationError("Permission denied", "Only admin can add members.");
            return;
        }

        if (currentGroupId == null) {
            LOGGER.log(Level.WARNING, "Cannot add member: groupId is null");
            ValidationUtils.showServerError("No group selected.");
            return;
        }

        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            ValidationUtils.showValidationError("Invalid Email", "Email cannot be empty.");
            return;
        }

        email = email.trim();

        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.showValidationError("Invalid Email", "Email must contain '@' symbol.");
            return;
        }

        String sanitizedEmail = ValidationUtils.sanitizeForDisplay(email);

        try {
            LOGGER.log(Level.INFO, "Adding member with email: {0} to group: {1}", new Object[]{sanitizedEmail, currentGroupId});

            HttpClient client = HttpClient.newHttpClient();

            String url = "http://localhost:8080/groups/" + currentGroupId + "/add-member?email=" +
                    java.net.URLEncoder.encode(sanitizedEmail, java.nio.charset.StandardCharsets.UTF_8);

            LOGGER.log(Level.INFO, "POST URL: {0}", url);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            LOGGER.log(Level.INFO, "Add member response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Add member response body: {0}", resp.body());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                LOGGER.log(Level.INFO, "Member added successfully");
                ValidationUtils.showSuccess("Success", "Member added successfully!");
            } else {
                LOGGER.log(Level.WARNING, "Failed to add member. Status: {0}", resp.statusCode());
                ValidationUtils.showServerError("Failed to add member. User may not exist or is already a member.");
            }

            emailField.clear();

            loadMembers();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding member", e);
            ValidationUtils.showServerError("Error adding member: " + e.getMessage());
        }
    }

    @FXML
    public void deleteMember() {
        if (!UserHolder.isAdmin()) {
            ValidationUtils.showValidationError("Permission denied", "Only admin can delete members.");
            return;
        }

        String selected = membersList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ValidationUtils.showValidationError("No Selection", "Please select a member to delete.");
            return;
        }

        String idPart = selected.contains("|") ? selected.split("\\|", 2)[0] : null;
        if (idPart == null) {
            ValidationUtils.showValidationError("Cannot delete", "Member id not available.");
            return;
        }

        Long memberId;
        try {
            memberId = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            ValidationUtils.showValidationError("Cannot delete", "Invalid member id.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Member");
        alert.setHeaderText("Are you sure you want to remove this member?");
        SceneUtils.styleDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                HttpClient client = HttpClient.newHttpClient();

                String url = "http://localhost:8080/groups/" + currentGroupId + "/remove-member?userId=" + memberId;
                LOGGER.log(Level.INFO, "DELETE URL: {0}", url);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOGGER.log(Level.INFO, "Remove member response: {0}", resp.statusCode());
                LOGGER.log(Level.INFO, "Remove member response body: {0}", resp.body());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    ValidationUtils.showSuccess("Success", "Member removed.");
                } else {
                    LOGGER.log(Level.WARNING, "Failed to remove member. Response: {0}", resp.body());
                    ValidationUtils.showServerError("Failed to remove member. Status: " + resp.statusCode() + "\n" + resp.body());
                }

                loadMembers();

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error removing member", e);
                ValidationUtils.showServerError("Error removing member: " + e.getMessage());
            }
        }
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) membersList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to groups", e);
        }
    }
}

package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.TaskHolder;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroupsController {

    private static final Logger LOGGER = Logger.getLogger(GroupsController.class.getName());

    @FXML
    private ListView<String> groupsList;

    @FXML
    public void initialize() {
        loadGroups();
    }

    private Long getGroupIdByName(String groupName) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/groups"))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            String[] blocks = body.split("\\{");
            for (String b : blocks) {
                if (b.contains("\"name\":\"" + groupName + "\"")) {
                    String afterId = b.split("\"id\":")[1];
                    String idStr = afterId.split(",")[0];
                    return Long.parseLong(idStr);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting groupId", e);
        }
        return null;
    }

    @FXML
    public void groupClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String selected = groupsList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Long groupId = getGroupIdByName(selected);
            if (groupId == null) {
                LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
                return;
            }

            try {
                TaskHolder.groupId = groupId;

                Stage stage = (Stage) groupsList.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(Main.class.getResource("tasks.fxml"));
                stage.setScene(SceneUtils.createStyledScene(loader.load()));
                stage.setTitle("Tasks: " + selected);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading tasks.fxml", e);
            }
        }
    }

    @FXML
    public void openMembers() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        try {
            MembersController.currentGroupId = groupId;

            Stage stage = (Stage) groupsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("members.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Members: " + selected);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading members.fxml", e);
        }
    }

    @FXML
    public void openMaterials() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected for materials");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        try {
            TaskHolder.groupId = groupId;

            Stage stage = (Stage) groupsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("materials.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Materials: " + selected);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading materials.fxml", e);
        }
    }

    @FXML
    public void openChat() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected for chat");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        try {
            TaskHolder.groupId = groupId;

            Stage stage = (Stage) groupsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("chat.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Chat: " + selected);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading chat.fxml", e);
        }
    }

    @FXML
    public void openStatistics() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected for statistics");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        try {
            TaskHolder.groupId = groupId;

            Stage stage = (Stage) groupsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("statistics.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Statistics: " + selected);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading statistics.fxml", e);
        }
    }

    @FXML
    public void editGroup() {
        if (!UserHolder.isAdmin()) {
            ValidationUtils.showValidationError("Permission Denied", "Only admin can edit groups.");
            return;
        }

        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected for editing");
            ValidationUtils.showValidationError("No Selection", "Please select a group to edit.");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("Edit Group");
        dialog.setHeaderText("Enter new group name:");
        dialog.setContentText("Name:");
        SceneUtils.styleDialog(dialog);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!ValidationUtils.isValidName(newName)) {
                ValidationUtils.showValidationError("Invalid Name", "Group name cannot be empty.");
                return;
            }

            String sanitizedName = ValidationUtils.sanitize(newName);

            try {
                String json = "{ \"name\": \"" + sanitizedName + "\" }";

                LOGGER.log(Level.INFO, "Updating group {0} with name: {1}", new Object[]{groupId, sanitizedName});

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/groups/" + groupId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOGGER.log(Level.INFO, "Edit group response: {0}", resp.statusCode());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    loadGroups();
                } else {
                    ValidationUtils.showServerError("Failed to update group. Status: " + resp.statusCode());
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error editing group", e);
                ValidationUtils.showServerError("Error updating group: " + e.getMessage());
            }
        });
    }

    @FXML
    public void deleteGroup() {
        if (!UserHolder.isAdmin()) {
            ValidationUtils.showValidationError("Permission Denied", "Only admin can delete groups.");
            return;
        }

        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.log(Level.WARNING, "No group selected for deletion");
            return;
        }

        Long groupId = getGroupIdByName(selected);
        if (groupId == null) {
            LOGGER.log(Level.WARNING, "Could not find groupId for: {0}", selected);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Group");
        alert.setHeaderText("Are you sure you want to delete this group?");
        alert.setContentText("Group: " + selected);
        SceneUtils.styleDialog(alert);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                LOGGER.log(Level.INFO, "Deleting group: {0}", groupId);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/groups/" + groupId))
                        .DELETE()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                LOGGER.log(Level.INFO, "Delete group response: {0}", resp.statusCode());

                loadGroups();

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting group", e);
            }
        }
    }

    private void loadGroups() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/groups"))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            groupsList.getItems().clear();

            String[] parts = body.split("\"name\":\"");
            for (int i = 1; i < parts.length; i++) {
                String name = parts[i].split("\"")[0];
                groupsList.getItems().add(name);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading groups", e);
        }
    }

    @FXML
    public void addGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Group");
        dialog.setHeaderText("Enter group name:");
        SceneUtils.styleDialog(dialog);
        dialog.showAndWait().ifPresent(name -> {
            if (!ValidationUtils.isValidName(name)) {
                ValidationUtils.showValidationError("Invalid Name", "Group name cannot be empty.");
                return;
            }

            String sanitizedName = ValidationUtils.sanitize(name);

            try {
                String json = "{ \"name\": \"" + sanitizedName + "\", \"createdBy\": " + UserHolder.userId + " }";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/groups"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    loadGroups();
                } else {
                    ValidationUtils.showServerError("Failed to create group. Status: " + resp.statusCode());
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error adding group", e);
                ValidationUtils.showServerError("Error creating group: " + e.getMessage());
            }
        });
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) groupsList.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back", e);
        }
    }
}

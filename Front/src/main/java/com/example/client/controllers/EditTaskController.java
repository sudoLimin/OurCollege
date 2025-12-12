package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.TaskHolder;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditTaskController {

    private static final Logger logger = Logger.getLogger(EditTaskController.class.getName());

    @FXML private TextField titleField;
    @FXML private TextArea descField;
    @FXML private DatePicker deadlinePicker;

    private Long currentTaskId = null;

    private boolean isEditMode = false;

    private String originalStatus = "OPEN";
    private Long originalGroupId = null;

    @FXML
    private void initialize() {
        logger.log(Level.INFO, "EditTaskController initialized. Fields bound: titleField={0}, descField={1}",
            new Object[]{titleField != null, descField != null});
    }

    @FXML
    public void save() {
        try {
            String title = (titleField != null && titleField.getText() != null) ? titleField.getText().trim() : "";
            String description = (descField != null && descField.getText() != null) ? descField.getText().trim() : "";

            if (!ValidationUtils.isValidName(title)) {
                ValidationUtils.showValidationError("Invalid Title", "Task title cannot be empty.");
                return;
            }

            String sanitizedTitle = ValidationUtils.sanitize(title);
            String sanitizedDescription = ValidationUtils.sanitize(description);

            String deadlineValue = null;
            if (deadlinePicker != null && deadlinePicker.getValue() != null) {
                deadlineValue = deadlinePicker.getValue().toString(); // Format: 2025-12-09
            }

            logger.log(Level.INFO, "Saving task - Title: ''{0}'', Description: ''{1}'', Deadline: ''{2}'', isEditMode: {3}",
                new Object[]{sanitizedTitle, sanitizedDescription, deadlineValue, isEditMode});

            Long groupId = originalGroupId != null ? originalGroupId : TaskHolder.groupId;

            if (groupId == null && currentTaskId == null) {
                logger.log(Level.SEVERE, "Cannot save task: groupId is null and not in edit mode");
                ValidationUtils.showServerError("Cannot save task: no group selected.");
                goBackOrClose();
                return;
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req;
            HttpResponse<String> resp;

            if (currentTaskId == null) {
                String deadlineJson = deadlineValue != null ? "\"" + deadlineValue + "\"" : "null";
                Long createdBy = UserHolder.userId;
                String createdByJson = createdBy != null ? createdBy.toString() : "null";

                String json = "{ \"title\": \"" + sanitizedTitle +
                        "\", \"description\": \"" + sanitizedDescription +
                        "\", \"groupId\": " + groupId +
                        ", \"createdBy\": " + createdByJson +
                        ", \"status\": \"OPEN\"" +
                        ", \"deadline\": " + deadlineJson + " }";

                logger.log(Level.INFO, "Creating new task with JSON: {0}", json);

                String url = "http://localhost:8080/tasks/group/" + groupId;
                if (createdBy != null) {
                    url += "?createdBy=" + createdBy;
                }

                req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                logger.log(Level.INFO, "Create response status: {0}", resp.statusCode());
                logger.log(Level.INFO, "Create response body: {0}", resp.body());

                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    ValidationUtils.showServerError("Failed to create task. Status: " + resp.statusCode());
                }
            } else {
                String deadlineJson = deadlineValue != null ? "\"" + deadlineValue + "\"" : "null";
                String json = "{ \"title\": \"" + sanitizedTitle +
                        "\", \"description\": \"" + sanitizedDescription +
                        "\", \"groupId\": " + groupId +
                        ", \"status\": \"" + originalStatus + "\"" +
                        ", \"deadline\": " + deadlineJson + " }";

                logger.log(Level.INFO, "Updating task {0} with JSON: {1}", new Object[]{currentTaskId, json});

                req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/tasks/" + currentTaskId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                logger.log(Level.INFO, "Update response status: {0}", resp.statusCode());
                logger.log(Level.INFO, "Update response body: {0}", resp.body());

                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    ValidationUtils.showServerError("Failed to update task. Status: " + resp.statusCode());
                }

                if (deadlineValue != null) {
                    String deadlineUpdateJson = "{ \"deadline\": \"" + deadlineValue + "\" }";
                    logger.log(Level.INFO, "Updating deadline with: {0}", deadlineUpdateJson);

                    HttpRequest deadlineReq = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/tasks/" + currentTaskId + "/deadline"))
                            .header("Content-Type", "application/json")
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(deadlineUpdateJson))
                            .build();

                    HttpResponse<String> deadlineResp = client.send(deadlineReq, HttpResponse.BodyHandlers.ofString());
                    logger.log(Level.INFO, "Deadline update response status: {0}", deadlineResp.statusCode());
                    logger.log(Level.INFO, "Deadline update response body: {0}", deadlineResp.body());
                }
            }

            goBackOrClose();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving task", e);
            ValidationUtils.showServerError("Error saving task: " + e.getMessage());
            goBackOrClose();
        }
    }

    @FXML
    private void cancel() {
        logger.log(Level.INFO, "Cancel button pressed");
        goBackOrClose();
    }

    private void goBackOrClose() {
        Platform.runLater(() -> {
            try {
                if (titleField == null || titleField.getScene() == null || titleField.getScene().getWindow() == null) {
                    return;
                }

                Stage stage = (Stage) titleField.getScene().getWindow();

                if (isEditMode) {
                    logger.log(Level.INFO, "Edit mode: navigating back to tasks");
                    FXMLLoader loader = new FXMLLoader(Main.class.getResource("tasks.fxml"));
                    stage.setScene(SceneUtils.createStyledScene(loader.load()));
                    stage.setTitle("Tasks");
                } else {
                    logger.log(Level.INFO, "Add mode: closing dialog");
                    stage.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in goBackOrClose", e);
            }
        });
    }

    public void setTaskId(Long taskId) {
        this.currentTaskId = taskId;
        this.isEditMode = (taskId != null);
        logger.log(Level.INFO, "setTaskId called with: {0}, isEditMode: {1}", new Object[]{taskId, isEditMode});
    }

    @FXML
    public void load() {
        if (currentTaskId == null) {
            logger.log(Level.WARNING, "Cannot load task: taskId is null");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/" + currentTaskId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            logger.log(Level.INFO, "Loaded task: {0}", body);

            if (body.contains("\"title\":\"")) {
                String title = body.split("\"title\":\"")[1].split("\"")[0];
                titleField.setText(title);
                logger.log(Level.INFO, "Set title to: {0}", title);
            }

            if (body.contains("\"description\":\"")) {
                String desc = body.split("\"description\":\"")[1].split("\"")[0];
                descField.setText(desc);
                logger.log(Level.INFO, "Set description to: {0}", desc);
            }

            if (body.contains("\"status\":\"")) {
                originalStatus = body.split("\"status\":\"")[1].split("\"")[0];
                logger.log(Level.INFO, "Captured original status: {0}", originalStatus);
            }

            if (body.contains("\"groupId\":")) {
                String groupIdStr = body.split("\"groupId\":")[1].split("[,}]")[0].trim();
                if (!groupIdStr.equals("null")) {
                    originalGroupId = Long.parseLong(groupIdStr);
                    logger.log(Level.INFO, "Captured original groupId: {0}", originalGroupId);
                }
            }

            if (body.contains("\"deadline\":\"")) {
                String d = body.split("\"deadline\":\"")[1].split("\"")[0];
                if (!d.equals("null") && !d.isEmpty()) {
                    if (d.contains("T")) {
                        d = d.split("T")[0];
                    }
                    deadlinePicker.setValue(java.time.LocalDate.parse(d));
                    logger.log(Level.INFO, "Set deadline to: {0}", d);
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading task", e);
        }
    }
}

package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.TaskHolder;
import com.example.client.models.Task;
import com.example.client.utils.SceneUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskDetailsController {

    private static final Logger LOGGER = Logger.getLogger(TaskDetailsController.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FXML private Label titleLabel;
    @FXML private Label descLabel;
    @FXML private Label createdLabel;
    @FXML private Label statusLabel;
    @FXML private Label deadlineLabel;

    private Task task;

    @FXML
    public void initialize() {
        try {
            Long taskId = TaskHolder.openTaskId;
            if (taskId == null) {
                LOGGER.log(Level.SEVERE, "Task ID is null. Cannot load task details.");
                return;
            }

            LOGGER.log(Level.INFO, "Loading task details for taskId: {0}", taskId);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/info/" + taskId))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            LOGGER.log(Level.INFO, "Task details response status: {0}", response.statusCode());
            LOGGER.log(Level.INFO, "Task details response body: {0}", response.body());

            task = parseTask(response.body());

            if (task != null) {
                titleLabel.setText(task.getTitle() != null ? task.getTitle() : "");
                descLabel.setText(task.getDescription() != null ? task.getDescription() : "");
                createdLabel.setText("Created by: " + (task.getCreatedBy() != null ? task.getCreatedBy() : "Unknown"));
                statusLabel.setText("Status: " + (task.getStatus() != null ? task.getStatus() : "Unknown"));
                deadlineLabel.setText(task.getDeadline() == null || task.getDeadline().isEmpty() ? "" : "Deadline: " + task.getDeadline());
            } else {
                LOGGER.log(Level.WARNING, "Task is null after parsing");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing task details", e);
        }
    }

    @SuppressWarnings("unused")
    @FXML
    public void deleteTask() {
        if (task == null) {
            LOGGER.log(Level.WARNING, "Cannot delete task: Task is null");
            return;
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/" + task.getId()))
                    .DELETE()
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Task deleted successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting task", e);
        }
    }

    @SuppressWarnings("unused")
    @FXML
    public void markDone() {
        if (task == null) {
            LOGGER.log(Level.WARNING, "Cannot mark task as done: Task is null");
            return;
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            String json = "{ \"status\": \"DONE\" }";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/" + task.getId() + "/status"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Task marked as done");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error marking task as done", e);
        }
    }

    @FXML
    public void goBack() {
        try {
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("tasks.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error going back to task list", e);
        }
    }

    @FXML
    public void edit() {
        try {
            if (task == null) {
                LOGGER.log(Level.WARNING, "Cannot edit task: Task is null");
                return;
            }

            LOGGER.log(Level.INFO, "Opening edit view for task ID: {0}", task.getId());

            FXMLLoader loader = new FXMLLoader(Main.class.getResource("edit_task.fxml"));
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.setScene(SceneUtils.createStyledScene(loader.load()));

            EditTaskController editController = loader.getController();
            editController.setTaskId(task.getId());
            editController.load();

            stage.setTitle("Edit Task");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening edit task view", e);
        }
    }

    private Task parseTask(String json) {
        try {
            LOGGER.log(Level.INFO, "Parsing task JSON: {0}", json);
            Task parsed = objectMapper.readValue(json, Task.class);
            LOGGER.log(Level.INFO, "Parsed task - id: {0}, title: {1}", new Object[]{parsed.getId(), parsed.getTitle()});
            return parsed;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing task JSON: " + json, e);
            return null;
        }
    }
}

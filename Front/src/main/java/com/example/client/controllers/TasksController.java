package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.WSClient;
import com.example.client.holders.TaskHolder;
import com.example.client.utils.SceneUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TasksController {

    @FXML private VBox openColumn;
    @FXML private VBox inProgressColumn;
    @FXML private VBox doneColumn;

    private static final Logger LOGGER = Logger.getLogger(TasksController.class.getName());

    private WSClient ws;

    private void loadTasks() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            Long groupId = TaskHolder.groupId;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/group/" + groupId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String json = resp.body();

            clearColumns();

            JSONArray tasks = new JSONArray(json);
            for (int i = 0; i < tasks.length(); i++) {
                try {
                    JSONObject task = tasks.getJSONObject(i);

                    if (!task.has("id") || !task.has("title") || !task.has("status")) {
                        LOGGER.log(Level.WARNING, "Task JSON missing required fields: {0}", task);
                        continue;
                    }

                    Long id = task.getLong("id");
                    String title = task.getString("title");
                    String status = task.getString("status");
                    String deadline = task.optString("deadline", null);
                    if ("null".equals(deadline)) deadline = null;

                    addTaskToColumn(id, title, status, deadline);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing task: " + tasks.get(i), e);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading tasks", e);
        }
    }

    private void clearColumns() {
        if (openColumn.getChildren().size() > 1) {
            openColumn.getChildren().remove(1, openColumn.getChildren().size());
        }
        if (inProgressColumn.getChildren().size() > 1) {
            inProgressColumn.getChildren().remove(1, inProgressColumn.getChildren().size());
        }
        if (doneColumn.getChildren().size() > 1) {
            doneColumn.getChildren().remove(1, doneColumn.getChildren().size());
        }
    }

    private void addTaskToColumn(Long id, String title, String status, String deadline) {
        String labelText = title;
        if (deadline != null && !deadline.isEmpty()) {
            String dateOnly = deadline.contains("T") ? deadline.split("T")[0] : deadline;
            labelText = title + " (deadline: " + dateOnly + ")";
        }

        Label lbl = new Label(labelText);

        boolean isOverdue = false;
        if (deadline != null && !deadline.isEmpty() && !"DONE".equals(status)) {
            try {
                String dateOnly = deadline.contains("T") ? deadline.split("T")[0] : deadline;
                LocalDate deadlineDate = LocalDate.parse(dateOnly);
                if (deadlineDate.isBefore(LocalDate.now())) {
                    isOverdue = true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error parsing deadline: {0}", deadline);
            }
        }

        if (isOverdue) {
            lbl.setStyle("-fx-padding: 10; -fx-background-color: #4A2020; -fx-border-color: #FF5252; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: #E0E0E0;");
        } else {
            lbl.setStyle("-fx-padding: 10; -fx-background-color: #3C3C3C; -fx-border-color: #616161; -fx-background-radius: 5; -fx-border-radius: 5; -fx-text-fill: #E0E0E0;");
        }

        lbl.setOnMouseClicked(e -> {
            LOGGER.log(Level.INFO, "Mouse event detected on task: {0}", id);

            switch (e.getButton()) {
                case PRIMARY -> {
                    LOGGER.log(Level.INFO, "Left click detected on task: {0}. Attempting to open details.", id);
                    openDetails(id);
                }
                case SECONDARY -> {
                    LOGGER.log(Level.INFO, "Right click detected on task: {0}. Attempting to change status.", id);
                    changeStatus(id, status);
                }
                default -> LOGGER.log(Level.WARNING, "Unhandled mouse event on task: {0}", id);
            }
        });

        switch (status) {
            case "OPEN" -> openColumn.getChildren().add(lbl);
            case "IN_PROGRESS" -> inProgressColumn.getChildren().add(lbl);
            case "DONE" -> doneColumn.getChildren().add(lbl);
        }
    }

    private void changeStatus(Long id, String currentStatus) {

        String newStatus = switch (currentStatus) {
            case "OPEN" -> "IN_PROGRESS";
            case "IN_PROGRESS" -> "DONE";
            default -> "OPEN";
        };

        try {
            String json = "{ \"status\": \"" + newStatus + "\" }";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/" + id + "/status"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.send(req, HttpResponse.BodyHandlers.ofString());
            loadTasks();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error changing task status", e);
        }
    }

    private void openDetails(Long id) {
        try {
            TaskHolder.openTaskId = id;

            Stage stage = (Stage) openColumn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("task_details.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Task Details");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening details", e);
        }
    }

    @FXML
    private void addTask() {
        LOGGER.log(Level.INFO, "Add Task button clicked");
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("edit_task.fxml"));
            Stage stage = new Stage();
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Add New Task");
            stage.showAndWait();

            LOGGER.log(Level.INFO, "Edit dialog closed, refreshing tasks");
            loadTasks();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in addTask", e);
        }
    }

    @FXML
    private void back() {
        LOGGER.log(Level.INFO, "Back button clicked");
        try {
            Stage stage = (Stage) openColumn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back", e);
        }
    }

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "TasksController initialized with groupId: {0}", TaskHolder.groupId);
        loadTasks();

        try {
            ws = new WSClient(() -> Platform.runLater(this::loadTasks));
            ws.onType("task_new", () -> Platform.runLater(this::loadTasks));
            ws.connect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not connect WebSocket", e);
        }
    }
}

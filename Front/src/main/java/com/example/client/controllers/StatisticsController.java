package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.TaskHolder;
import com.example.client.utils.SceneUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatisticsController {

    private static final Logger LOGGER = Logger.getLogger(StatisticsController.class.getName());

    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label openTasksLabel;
    @FXML private Label completedTodayLabel;
    @FXML private Label completedWeekLabel;
    @FXML private Label completedMonthLabel;
    @FXML private BarChart<String, Number> statusChart;
    @FXML private VBox memberStatsBox;

    private int totalTasks = 0;
    private int tasksDone = 0;
    private int tasksInProgress = 0;
    private int tasksOpen = 0;
    private int tasksCompletedToday = 0;
    private int tasksCompletedThisWeek = 0;
    private int tasksCompletedThisMonth = 0;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "StatisticsController initialized for groupId: {0}", TaskHolder.groupId);
        loadStats();
        loadMemberStats();
    }

    private void loadStats() {
        if (TaskHolder.groupId == null) {
            LOGGER.log(Level.WARNING, "Cannot load stats: groupId is null");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/stats/group/" + TaskHolder.groupId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Stats response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Stats response body: {0}", resp.body());

            if (resp.statusCode() == 200) {
                parseStatsResponse(resp.body());
            } else {
                LOGGER.log(Level.INFO, "Stats endpoint not available, calculating from tasks...");
                calculateStatsFromTasks();
            }

            updateUI();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading stats", e);
            calculateStatsFromTasks();
            updateUI();
        }
    }

    private void parseStatsResponse(String body) {
        try {

            if (body.contains("\"totalTasks\":")) {
                String val = body.split("\"totalTasks\":")[1].split("[,}]")[0].trim();
                totalTasks = Integer.parseInt(val);
            }
            if (body.contains("\"tasksOpen\":")) {
                String val = body.split("\"tasksOpen\":")[1].split("[,}]")[0].trim();
                tasksOpen = Integer.parseInt(val);
            }
            if (body.contains("\"tasksInProgress\":")) {
                String val = body.split("\"tasksInProgress\":")[1].split("[,}]")[0].trim();
                tasksInProgress = Integer.parseInt(val);
            }
            if (body.contains("\"tasksDone\":")) {
                String val = body.split("\"tasksDone\":")[1].split("[,}]")[0].trim();
                tasksDone = Integer.parseInt(val);
            }
            if (body.contains("\"tasksCompletedToday\":")) {
                String val = body.split("\"tasksCompletedToday\":")[1].split("[,}]")[0].trim();
                tasksCompletedToday = Integer.parseInt(val);
            }
            if (body.contains("\"tasksCompletedThisWeek\":")) {
                String val = body.split("\"tasksCompletedThisWeek\":")[1].split("[,}]")[0].trim();
                tasksCompletedThisWeek = Integer.parseInt(val);
            }
            if (body.contains("\"tasksCompletedThisMonth\":")) {
                String val = body.split("\"tasksCompletedThisMonth\":")[1].split("[,}]")[0].trim();
                tasksCompletedThisMonth = Integer.parseInt(val);
            }

            LOGGER.log(Level.INFO, "Parsed stats: total={0}, open={1}, inProgress={2}, done={3}",
                    new Object[]{totalTasks, tasksOpen, tasksInProgress, tasksDone});

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing stats response", e);
        }
    }

    private void calculateStatsFromTasks() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/tasks/group/" + TaskHolder.groupId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            totalTasks = 0;
            tasksDone = 0;
            tasksInProgress = 0;
            tasksOpen = 0;

            if (body.startsWith("[") && body.length() > 2) {
                String[] parts = body.split("\\{");
                for (String part : parts) {
                    if (part.contains("\"status\":\"")) {
                        totalTasks++;
                        String status = part.split("\"status\":\"")[1].split("\"")[0];
                        switch (status.toUpperCase()) {
                            case "DONE" -> tasksDone++;
                            case "IN_PROGRESS" -> tasksInProgress++;
                            case "OPEN" -> tasksOpen++;
                        }
                    }
                }
            }

            LOGGER.log(Level.INFO, "Calculated stats: total={0}, done={1}, inProgress={2}, open={3}",
                    new Object[]{totalTasks, tasksDone, tasksInProgress, tasksOpen});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating stats from tasks", e);
        }
    }

    private void updateUI() {
        totalTasksLabel.setText(String.valueOf(totalTasks));
        completedTasksLabel.setText(String.valueOf(tasksDone));
        inProgressLabel.setText(String.valueOf(tasksInProgress));
        openTasksLabel.setText(String.valueOf(tasksOpen));

        if (completedTodayLabel != null) {
            completedTodayLabel.setText(String.valueOf(tasksCompletedToday));
        }
        if (completedWeekLabel != null) {
            completedWeekLabel.setText(String.valueOf(tasksCompletedThisWeek));
        }
        if (completedMonthLabel != null) {
            completedMonthLabel.setText(String.valueOf(tasksCompletedThisMonth));
        }

        statusChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tasks");
        series.getData().add(new XYChart.Data<>("Open", tasksOpen));
        series.getData().add(new XYChart.Data<>("In Progress", tasksInProgress));
        series.getData().add(new XYChart.Data<>("Done", tasksDone));
        statusChart.getData().add(series);
    }

    private void loadMemberStats() {
        if (TaskHolder.groupId == null) {
            return;
        }

        memberStatsBox.getChildren().clear();

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/stats/group/" + TaskHolder.groupId + "/members"))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(Level.INFO, "Member stats response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Member stats response body: {0}", resp.body());

            if (resp.statusCode() == 200 && resp.body().startsWith("[")) {
                parseMemberStats(resp.body());
            } else {
                LOGGER.log(Level.INFO, "Member stats endpoint not available, using fallback...");
                loadMemberStatsFallback();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading member stats", e);
            loadMemberStatsFallback();
        }
    }

    private void parseMemberStats(String body) {

        LOGGER.log(Level.INFO, "Parsing member stats from: {0}", body);

        try {
            String content = body.trim();
            if (content.startsWith("[")) {
                content = content.substring(1);
            }
            if (content.endsWith("]")) {
                content = content.substring(0, content.length() - 1);
            }

            String[] objects = content.split("\\}\\s*,\\s*\\{");
            boolean foundAny = false;

            for (String obj : objects) {
                String cleanObj = obj.replace("{", "").replace("}", "");
                LOGGER.log(Level.INFO, "Processing member object: {0}", cleanObj);

                if (cleanObj.contains("userName")) {
                    foundAny = true;

                    String userName = "Unknown";
                    if (cleanObj.contains("\"userName\":\"")) {
                        userName = cleanObj.split("\"userName\":\"")[1].split("\"")[0];
                    }

                    int tasksCreated = extractNumber(cleanObj, "tasksCreated");

                    int tasksCompleted = extractNumber(cleanObj, "tasksCompleted");

                    LOGGER.log(Level.INFO, "Parsed member: name={0}, created={1}, completed={2}",
                            new Object[]{userName, tasksCreated, tasksCompleted});

                    addMemberBar(userName, tasksCreated, tasksCompleted);
                }
            }

            if (!foundAny) {
                Label noMembers = new Label("No member statistics available.");
                noMembers.setStyle("-fx-text-fill: #9E9E9E;");
                memberStatsBox.getChildren().add(noMembers);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing member stats: " + e.getMessage(), e);
            loadMemberStatsFallback();
        }
    }

    private int extractNumber(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            if (json.contains(searchKey)) {
                int startIndex = json.indexOf(searchKey) + searchKey.length();
                StringBuilder numStr = new StringBuilder();
                for (int i = startIndex; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (Character.isDigit(c) || c == '-') {
                        numStr.append(c);
                    } else if (numStr.length() > 0) {
                        break;
                    }
                }
                if (numStr.length() > 0) {
                    return Integer.parseInt(numStr.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract number for key: " + key, e);
        }
        return 0;
    }

    private void loadMemberStatsFallback() {
        Label noStats = new Label("Member statistics not available.");
        noStats.setStyle("-fx-text-fill: #9E9E9E; -fx-font-style: italic;");
        memberStatsBox.getChildren().add(noStats);
    }

    private void addMemberBar(String name, int tasksCreated, int tasksCompleted) {
        HBox row = new HBox(10);
        row.setStyle("-fx-alignment: center-left; -fx-padding: 5;");

        Label nameLabel = new Label(name);
        nameLabel.setPrefWidth(100);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E0E0E0;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(20);

        double progress = tasksCreated > 0 ? (double) tasksCompleted / tasksCreated : 0;
        progressBar.setProgress(progress);

        if (progress >= 0.8) {
            progressBar.setStyle("-fx-accent: #81C784;");
        } else if (progress >= 0.5) {
            progressBar.setStyle("-fx-accent: #FFB74D;");
        } else {
            progressBar.setStyle("-fx-accent: #FF7043;");
        }

        Label statsLabel = new Label(tasksCompleted + "/" + tasksCreated + " completed");
        statsLabel.setStyle("-fx-text-fill: #9E9E9E;");

        row.getChildren().addAll(nameLabel, progressBar, statsLabel);
        memberStatsBox.getChildren().add(row);
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) totalTasksLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to groups", e);
        }
    }
}

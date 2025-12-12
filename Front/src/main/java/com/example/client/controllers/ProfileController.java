package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfileController {

    private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());

    @FXML private TextField nameField;
    @FXML private TextField emailField;

    @FXML
    public void initialize() {
        LOGGER.log(Level.INFO, "ProfileController initialized for userId: {0}", UserHolder.userId);
        loadProfile();
    }

    private void loadProfile() {
        if (UserHolder.userId == null) {
            LOGGER.log(Level.WARNING, "Cannot load profile: userId is null");
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/" + UserHolder.userId))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            LOGGER.log(Level.INFO, "Profile response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Profile response body: {0}", body);

            if (body.contains("\"name\":\"")) {
                String name = body.split("\"name\":\"")[1].split("\"")[0];
                nameField.setText(name);
            }
            if (body.contains("\"email\":\"")) {
                String email = body.split("\"email\":\"")[1].split("\"")[0];
                emailField.setText(email);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading profile", e);
        }
    }

    @FXML
    public void saveProfile() {
        if (UserHolder.userId == null) {
            LOGGER.log(Level.WARNING, "Cannot save profile: userId is null");
            return;
        }

        try {
            String name = nameField.getText() != null ? nameField.getText().trim() : "";
            String email = emailField.getText() != null ? emailField.getText().trim() : "";

            LOGGER.log(Level.INFO, "Saving profile - Name: {0}, Email: {1}", new Object[]{name, email});

            String json = "{ \"name\": \"" + name + "\", \"email\": \"" + email + "\" }";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/" + UserHolder.userId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            LOGGER.log(Level.INFO, "Save profile response status: {0}", resp.statusCode());
            LOGGER.log(Level.INFO, "Save profile response body: {0}", resp.body());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                UserHolder.userName = name;
                LOGGER.log(Level.INFO, "Profile saved successfully");
            } else {
                LOGGER.log(Level.WARNING, "Failed to save profile. Status: {0}", resp.statusCode());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving profile", e);
        }
    }

    @FXML
    public void back() {
        try {
            Stage stage = (Stage) nameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Main Menu");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to main", e);
        }
    }
}

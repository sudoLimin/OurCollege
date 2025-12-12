package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;


    public void register() {
        try {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText();

            if (!ValidationUtils.isValidName(name)) {
                ValidationUtils.showValidationError("Invalid Name", "Name cannot be empty.");
                return;
            }

            if (ValidationUtils.isAdminUser(name)) {
                ValidationUtils.showValidationError("Reserved Username", "The username 'admin' is reserved and cannot be registered.");
                return;
            }

            if (!ValidationUtils.isValidEmail(email)) {
                ValidationUtils.showValidationError("Invalid Email", "Email must contain '@' symbol.");
                return;
            }

            if (ValidationUtils.isAdminUser(email)) {
                ValidationUtils.showValidationError("Reserved Email", "The email 'admin' is reserved and cannot be used.");
                return;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                ValidationUtils.showValidationError("Invalid Password", "Password must be at least 6 characters.");
                return;
            }

            String sanitizedName = ValidationUtils.sanitize(name);
            String sanitizedEmail = ValidationUtils.sanitize(email);
            String sanitizedPassword = ValidationUtils.sanitize(password);

            String json = "{ \"name\": \"" + sanitizedName +
                    "\", \"email\": \"" + sanitizedEmail +
                    "\", \"password\": \"" + sanitizedPassword + "\" }";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("REGISTER RESPONSE STATUS: " + response.statusCode());
            System.out.println("REGISTER RESPONSE BODY: " + response.body());
            System.out.println("REGISTER RESPONSE HEADERS: " + response.headers().map());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                new Alert(Alert.AlertType.INFORMATION, "Registered! (status " + response.statusCode() + ")").show();
                return;
            }

            String body = response.body();
            String friendly = body;
            try {
                if (body != null && body.trim().startsWith("{")) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> map = mapper.readValue(body, Map.class);
                    if (map.containsKey("message")) friendly = map.get("message").toString();
                    else if (map.containsKey("error")) friendly = map.get("error").toString();
                }
            } catch (Exception ignore) {
            }

            if (response.statusCode() == 500) {
                new Alert(Alert.AlertType.ERROR,
                        "Registration failed with status 500 (Internal Server Error). Check backend logs for stacktrace.\n\nServer message: "
                                + friendly)
                        .show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Registration failed (status " + response.statusCode() + "): " + friendly)
                        .show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Registration failed! Check console for details.").show();
        }
    }

    public void back() {
        try {
            Stage stage = (Stage) nameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("login.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

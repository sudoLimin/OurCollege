package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import com.example.client.utils.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.http.*;
import java.net.URI;
import java.util.Map;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + "/.tsikt_credentials";

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    public void initialize() {
        loadSavedCredentials();
    }

    
    private void loadSavedCredentials() {
        try {
            File file = new File(CREDENTIALS_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String encodedEmail = reader.readLine();
                    String encodedPassword = reader.readLine();

                    if (encodedEmail != null && encodedPassword != null) {
                        String email = new String(Base64.getDecoder().decode(encodedEmail));
                        String password = new String(Base64.getDecoder().decode(encodedPassword));

                        emailField.setText(email);
                        passwordField.setText(password);
                        rememberMeCheckBox.setSelected(true);

                        LOGGER.log(Level.INFO, "Loaded saved credentials for: {0}", email);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load saved credentials", e);
        }
    }

    
    private void saveCredentials(String email, String password) {
        try {
            File file = new File(CREDENTIALS_FILE);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(Base64.getEncoder().encodeToString(email.getBytes()));
                writer.newLine();
                writer.write(Base64.getEncoder().encodeToString(password.getBytes()));
            }
            LOGGER.log(Level.INFO, "Saved credentials for: {0}", email);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not save credentials", e);
        }
    }

    
    private void clearSavedCredentials() {
        try {
            File file = new File(CREDENTIALS_FILE);
            if (file.exists()) {
                file.delete();
                LOGGER.log(Level.INFO, "Cleared saved credentials");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not clear saved credentials", e);
        }
    }

    public void login() {
        try {
            String email = emailField.getText() == null ? "" : emailField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText();

            if (!ValidationUtils.isValidEmail(email)) {
                ValidationUtils.showValidationError("Invalid Email", "Email must contain '@' symbol.");
                return;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                ValidationUtils.showValidationError("Invalid Password", "Password must be at least 6 characters.");
                return;
            }

            String sanitizedEmail = ValidationUtils.sanitize(email);
            String sanitizedPassword = ValidationUtils.sanitize(password);

            String json = "{ \"email\": \"" + sanitizedEmail +
                    "\", \"password\": \"" + sanitizedPassword + "\" }";

            System.out.println("LOGIN REQUEST BODY: " + json);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();

            System.out.println("LOGIN RESPONSE STATUS: " + resp.statusCode());
            System.out.println("LOGIN RESPONSE BODY: " + body);
            System.out.println("LOGIN RESPONSE HEADERS: " + resp.headers().map());

            if (resp.statusCode() != 200) {
                String normalized = (body == null ? "" : body.trim()).replaceAll("[_\\s-]+", " ").toLowerCase();

                if (body != null && body.trim().startsWith("{")) {
                    try {
                        ObjectMapper _m = new ObjectMapper();
                        Map<String, Object> err = _m.readValue(body, Map.class);
                        if (err.containsKey("message")) normalized = err.get("message").toString().replaceAll("[_\\s-]+", " ").toLowerCase();
                        else if (err.containsKey("error")) normalized = err.get("error").toString().replaceAll("[_\\s-]+", " ").toLowerCase();
                    } catch (Exception ignore) {
                    }
                }

                if (resp.statusCode() == 401 && normalized.contains("user") && normalized.contains("not")) {
                    new Alert(Alert.AlertType.ERROR,
                            "Login failed: user not found (401). The user does not exist on the backend — check that registration succeeded and backend user lookup.\n\nServer message: "
                                    + body).show();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Invalid login (status " + resp.statusCode() + "): " + body + "\n\nHeaders: " + resp.headers().map())
                            .show();
                }
                return;
            }

            if (body == null || body.equals("null") || body.isBlank()) {
                new Alert(Alert.AlertType.ERROR, "Invalid credentials! (empty response)").show();
                return;
            }

            if (!body.trim().startsWith("{")) {
                new Alert(Alert.AlertType.ERROR, "Login failed: " + body).show();
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(body, Map.class);

            Long id = Long.valueOf(map.get("id").toString());
            String name = map.get("name").toString();

            UserHolder.userId = id;
            UserHolder.userName = name;

            if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
                saveCredentials(email, password);
            } else {
                clearSavedCredentials();
            }

            Stage stage = (Stage) emailField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Main Menu");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Server error! Check console for details.").show();
        }
    }

    public void goToRegister() {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("register.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


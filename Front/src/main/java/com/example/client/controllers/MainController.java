package com.example.client.controllers;

import com.example.client.Main;
import com.example.client.holders.StageHolder;
import com.example.client.holders.UserHolder;
import com.example.client.utils.SceneUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    public void openGroups() {
        try {
            Stage stage = (Stage) StageHolder.stage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("groups.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Groups");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening groups", e);
        }
    }

    @FXML
    public void openProfile() {
        try {
            Stage stage = (Stage) StageHolder.stage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("profile.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Profile");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening profile", e);
        }
    }

    @FXML
    public void openNotifications() {
        try {
            Stage stage = (Stage) StageHolder.stage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("notifications.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Notifications");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening notifications", e);
        }
    }

    @FXML
    public void logout() {
        try {
            UserHolder.userId = null;
            UserHolder.userName = null;

            Stage stage = (Stage) StageHolder.stage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("login.fxml"));
            stage.setScene(SceneUtils.createStyledScene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during logout", e);
        }
    }
}

package com.example.client.utils;

import com.example.client.Main;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

public class SceneUtils {

    public static Scene applyTheme(Scene scene) {
        try {
            String css = Main.class.getResource("styles/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load dark theme stylesheet: " + e.getMessage());
        }
        return scene;
    }

    public static Scene createStyledScene(javafx.scene.Parent root) {
        Scene scene = new Scene(root, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        return applyTheme(scene);
    }

    public static void styleDialog(Dialog<?> dialog) {
        try {
            DialogPane dialogPane = dialog.getDialogPane();
            String css = Main.class.getResource("styles/dark-theme.css").toExternalForm();
            dialogPane.getStylesheets().add(css);

            dialogPane.setStyle("-fx-background-color: #2D2D2D;");

            if (dialogPane.getContent() != null) {
                dialogPane.getContent().setStyle("-fx-text-fill: #E0E0E0;");
            }
        } catch (Exception e) {
            System.err.println("Could not style dialog: " + e.getMessage());
        }
    }

    public static void styleAlert(Alert alert) {
        styleDialog(alert);
    }

    public static void styleStage(Stage stage) {
        try {
            if (stage.getScene() != null) {
                String css = Main.class.getResource("styles/dark-theme.css").toExternalForm();
                stage.getScene().getStylesheets().add(css);
            }
        } catch (Exception e) {
            System.err.println("Could not style stage: " + e.getMessage());
        }
    }
}

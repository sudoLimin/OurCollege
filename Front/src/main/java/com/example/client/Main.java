package com.example.client;

import com.example.client.holders.StageHolder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static final double WINDOW_WIDTH = 800;
    public static final double WINDOW_HEIGHT = 600;

    @Override
    public void start(Stage stage) throws Exception {
        StageHolder.stage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);

        String css = Main.class.getResource("styles/dark-theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setMinWidth(WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}

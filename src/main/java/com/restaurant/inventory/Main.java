package com.restaurant.inventory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application entry point: manages the primary Stage and switches between
 * the LoginView and MainView based on authentication state.
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("The Gourmet Kitchen - Restaurant Menu & Inventory Management");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);

        showLoginView();
        primaryStage.show();
    }

    public static void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1050, 680);
            scene.getStylesheets().add(Main.class.getResource("/css/styles.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1240, 780);
            scene.getStylesheets().add(Main.class.getResource("/css/styles.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

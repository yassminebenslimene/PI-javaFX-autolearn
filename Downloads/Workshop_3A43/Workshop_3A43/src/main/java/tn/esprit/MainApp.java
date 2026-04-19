package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("AutoLearn");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        showLogin();
        primaryStage.show();
    }

    public static void showRegister() throws Exception {
        load("/views/auth/register.fxml");
        primaryStage.setTitle("AutoLearn ÔÇö Inscription");
    }

    public static void showLogin() throws Exception {
        load("/views/auth/login.fxml");
        primaryStage.setTitle("AutoLearn ÔÇö Connexion");
    }

    public static void showResetPassword() throws Exception {
        load("/views/auth/reset_password.fxml");
        primaryStage.setTitle("AutoLearn ÔÇö R├®initialisation");
    }

    public static void showBackoffice() throws Exception {
        load("/views/backoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn ÔÇö Backoffice");
    }

    public static void showFrontoffice() throws Exception {
        load("/views/frontoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn ÔÇö Espace ├ëtudiant");
    }

    public static void showProfile() throws Exception {
        load("/views/profile.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn ÔÇö Mon Profil");
    }

    private static void load(String fxml) throws Exception {
        // Use screen size so the scene always fills the window
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        java.net.URL resource = MainApp.class.getResource(fxml);
        if (resource == null) {
            // fallback: try without leading slash
            resource = MainApp.class.getResource(fxml.startsWith("/") ? fxml.substring(1) : fxml);
        }
        if (resource == null) throw new Exception("FXML not found: " + fxml);
        FXMLLoader loader = new FXMLLoader(resource);
        primaryStage.setScene(new Scene(loader.load(), screen.getWidth(), screen.getHeight()));
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
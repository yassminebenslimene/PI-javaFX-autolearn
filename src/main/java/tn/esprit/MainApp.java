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
        primaryStage.setTitle("AutoLearn — Inscription");
    }

    public static void showLogin() throws Exception {
        load("/views/auth/login.fxml");
        primaryStage.setTitle("AutoLearn — Connexion");
    }

    public static void showResetPassword() throws Exception {
        load("/views/auth/reset_password.fxml");
        primaryStage.setTitle("AutoLearn — Réinitialisation");
    }

    public static void showBackoffice() throws Exception {
        load("/views/backoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Backoffice");
    }

    public static void showFrontoffice() throws Exception {
        load("/views/frontoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Espace Étudiant");
    }

    public static void showProfile() throws Exception {
        load("/views/profile.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Mon Profil");
    }

    private static void load(String fxml) throws Exception {
        // Use screen size so the scene always fills the window
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));
        primaryStage.setScene(new Scene(loader.load(), screen.getWidth(), screen.getHeight()));
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}

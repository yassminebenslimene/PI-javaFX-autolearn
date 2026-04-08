package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("AutoLearn");
        showRegister();
        stage.show();
    }

    public static void showRegister() throws Exception {
        primaryStage.setResizable(false);
        load("/views/auth/register.fxml", 960, 680);
        primaryStage.setTitle("AutoLearn — Inscription");
    }

    public static void showLogin() throws Exception {
        primaryStage.setResizable(false);
        load("/views/auth/login.fxml", 900, 580);
        primaryStage.setTitle("AutoLearn — Connexion");
    }

    public static void showResetPassword() throws Exception {
        primaryStage.setResizable(false);
        load("/views/auth/reset_password.fxml", 860, 520);
        primaryStage.setTitle("AutoLearn — Réinitialisation");
    }

    public static void showBackoffice() throws Exception {
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        load("/views/backoffice/layout.fxml", 1280, 760);
        primaryStage.setTitle("AutoLearn — Backoffice");
    }

    public static void showFrontoffice() throws Exception {
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        load("/views/frontoffice/layout.fxml", 1100, 680);
        primaryStage.setTitle("AutoLearn — Espace Étudiant");
    }

    public static void showProfile() throws Exception {
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        load("/views/profile.fxml", 1100, 680);
        primaryStage.setTitle("AutoLearn — Mon Profil");
    }

    private static void load(String fxml, int w, int h) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));
        primaryStage.setScene(new Scene(loader.load(), w, h));
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}

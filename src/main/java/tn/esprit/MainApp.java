package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tn.esprit.controllers.evenement.front.*;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Participation;

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

    public static void showEvenementsFront() throws Exception {
        load("/views/frontoffice/evenements.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Événements");
    }

    public static void showJoinEvent(Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/join_event.fxml");
        setScene(loader);
        JoinEventController ctrl = loader.getController();
        ctrl.setEvenement(ev);
        primaryStage.setTitle("AutoLearn — Rejoindre l'événement");
    }

    public static void showCreateTeam(Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/create_team.fxml");
        setScene(loader);
        CreateTeamController ctrl = loader.getController();
        ctrl.setEvenement(ev);
        primaryStage.setTitle("AutoLearn — Créer une équipe");
    }

    public static void showTeamDetails(Equipe eq, Evenement ev, boolean showSuccess) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/team_details.fxml");
        setScene(loader);
        TeamDetailsController ctrl = loader.getController();
        ctrl.setData(eq, ev, showSuccess);
        primaryStage.setTitle("AutoLearn — Détails équipe");
    }

    public static void showEditTeam(Equipe eq, Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/edit_team.fxml");
        setScene(loader);
        EditTeamController ctrl = loader.getController();
        ctrl.setData(eq, ev);
        primaryStage.setTitle("AutoLearn — Modifier l'équipe");
    }

    public static void showMesParticipations(String successMsg) throws Exception {
        if (successMsg != null) MesParticipationsController.setPendingSuccess(successMsg);
        load("/views/frontoffice/mes_participations.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Mes Participations");
    }

    public static void showParticipationDetails(Participation p, Equipe eq, Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/participation_details.fxml");
        setScene(loader);
        ParticipationDetailsController ctrl = loader.getController();
        ctrl.setData(p, eq, ev);
        primaryStage.setTitle("AutoLearn — Détails participation");
    }

    public static void showEditParticipation(Participation p) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/edit_participation.fxml");
        setScene(loader);
        EditParticipationController ctrl = loader.getController();
        ctrl.setParticipation(p);
        primaryStage.setTitle("AutoLearn — Modifier participation");
    }

    private static FXMLLoader getLoader(String fxml) throws Exception {
        java.net.URL resource = MainApp.class.getResource(fxml);
        if (resource == null) throw new Exception("FXML not found: " + fxml);
        FXMLLoader loader = new FXMLLoader(resource);
        loader.load();
        return loader;
    }

    private static void setScene(FXMLLoader loader) {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        primaryStage.setScene(new Scene(loader.getRoot(), screen.getWidth(), screen.getHeight()));
        primaryStage.setMaximized(true);
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

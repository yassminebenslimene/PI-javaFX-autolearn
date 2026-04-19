package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.MainApp;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.session.SessionManager;

public class BackofficeController {

    @FXML private StackPane contentArea;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelCurrentRole;
    @FXML private Label labelAvatarSidebar;
    @FXML private Label labelPageTitle;

    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnActivites;
    @FXML private Button btnQuiz;
    @FXML private Button btnCours;
    @FXML private Button btnEvenements;
    @FXML private Button btnExercices;
    @FXML private Button btnChallenges;
    @FXML private Button btnCommunaute;
    @FXML private Button btnProfile;

    private static final String ACTIVE_STYLE =
        "-fx-background-color:rgba(122,106,216,0.25); -fx-text-fill:#a5b4fc;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-font-weight:600; -fx-cursor:hand; -fx-border-width:0;";

    private static final String INACTIVE_STYLE =
        "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-weight:normal;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";

    @FXML
    public void initialize() {
        MainApp.setBackofficeController(this);
        if (SessionManager.getCurrentUser() != null) {
            String name = SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom();
            if (labelCurrentUser != null) labelCurrentUser.setText(name);
            if (labelCurrentRole != null) labelCurrentRole.setText(SessionManager.getCurrentUser().getRole());
            if (labelAvatarSidebar != null) {
                String initials = SessionManager.getCurrentUser().getPrenom().substring(0,1).toUpperCase()
                                + SessionManager.getCurrentUser().getNom().substring(0,1).toUpperCase();
                labelAvatarSidebar.setText(initials);
            }
        }
        javafx.application.Platform.runLater(this::navigateToUsers);
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnDashboard, btnUsers, btnActivites, btnQuiz, btnCours, btnEvenements,
                                      btnExercices, btnChallenges, btnCommunaute, btnProfile}) {
            if (b != null) b.setStyle(b == active ? ACTIVE_STYLE : INACTIVE_STYLE);
        }
    }

    /** Log admin navigation action */
    private void logNav(String section) {
        var admin = SessionManager.getCurrentUser();
        if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.view_" + section,
            java.util.Map.of("section", section));
    }

    @FXML public void navigateToDashboard() {
        setActive(btnDashboard);
        if (labelPageTitle != null) labelPageTitle.setText("Dashboard");
        logNav("dashboard");
        loadView("/views/backoffice/user/index.fxml");
    }

    @FXML public void navigateToUsers() {
        setActive(btnUsers);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Utilisateurs");
        logNav("users");
        loadView("/views/backoffice/user/index.fxml");
    }

    @FXML public void navigateToActivites() {
        setActive(btnActivites);
        if (labelPageTitle != null) labelPageTitle.setText("Suivi des Activites");
        loadView("/views/backoffice/activites/index.fxml");
    }

    @FXML public void navigateToQuiz() {
        setActive(btnQuiz);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Quiz");
        logNav("quiz");
        loadView("/views/backoffice/quiz/index.fxml");
    }

    @FXML public void navigateToCours() {
        setActive(btnCours);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Cours");
        logNav("cours");
        loadView("/views/backoffice/cours/index.fxml");
    }

    @FXML public void navigateToEvenements() {
        setActive(btnEvenements);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Événements");
        logNav("evenements");
        loadView("/views/backoffice/evenement/index.fxml");
    }

    @FXML public void navigateToChapitres() {
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Chapitres");
        logNav("chapitres");
        loadView("/views/backoffice/chapitre/index.fxml");
    }

    @FXML public void navigateToExercices() {
        setActive(btnExercices);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Exercices");
        logNav("exercices");
        loadView("/views/backoffice/exercice/exercices.fxml");
    }

    @FXML public void navigateToChallenges() {
        setActive(btnChallenges);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion des Challenges");
        logNav("challenges");
        loadView("/views/backoffice/challenge/challenges.fxml");
    }

    @FXML public void navigateToCommunaute() {
        setActive(btnCommunaute);
        if (labelPageTitle != null) labelPageTitle.setText("Gestion de la Communauté");
        logNav("communaute");
        loadView("/views/backoffice/communaute/index.fxml");
    }

    @FXML public void navigateToProfile() {
        setActive(btnProfile);
        if (labelPageTitle != null) labelPageTitle.setText("Mon Profil");
        loadView("/views/backoffice/profile.fxml");
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Impossible de charger : " + path + " — " + e.getMessage());
        }
    }
}

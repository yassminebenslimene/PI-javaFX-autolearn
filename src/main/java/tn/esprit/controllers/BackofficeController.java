package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.MainApp;
import tn.esprit.session.SessionManager;
public class BackofficeController {

    @FXML private StackPane contentArea;
    @FXML private Label     labelCurrentUser;
    @FXML private Label     labelCurrentRole;
    @FXML private Label     labelAvatarSidebar;
    @FXML private Label     labelPageTitle;

    // Sidebar nav buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnEvenements;
    @FXML private Button btnExercices;  // ← NOUVEAU BOUTON
    @FXML private Button btnProfile;

    private static final String ACTIVE_STYLE =
            "-fx-background-color:rgba(5,150,105,0.2); -fx-text-fill:#34d399; -fx-font-weight:bold;" +
                    "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
                    "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-weight:normal;" +
                    "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
                    "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() != null) {
            String name = SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom();
            labelCurrentUser.setText(name);
            labelCurrentRole.setText(SessionManager.getCurrentUser().getRole());
            String initials = SessionManager.getCurrentUser().getPrenom().substring(0,1).toUpperCase()
                    + SessionManager.getCurrentUser().getNom().substring(0,1).toUpperCase();
            labelAvatarSidebar.setText(initials);
        }
        navigateToUsers();
    }

    @FXML public void navigateToDashboard() {
        setActive(btnDashboard);
        labelPageTitle.setText("Dashboard");
        loadView("/views/backoffice/dashboard.fxml");
    }

    @FXML public void navigateToUsers() {
        setActive(btnUsers);
        labelPageTitle.setText("Gestion des Utilisateurs");
        loadView("/views/backoffice/user/index.fxml");
    }

    // ✅ NOUVELLE MÉTHODE POUR EXERCICES
    @FXML public void navigateToExercices() {
        setActive(btnExercices);
        labelPageTitle.setText("Gestion des Exercices");
        loadView("/views/backoffice/exercice/exercices.fxml");
    }

    @FXML public void navigateToProfile() {
        setActive(btnProfile);
        labelPageTitle.setText("Mon Profil");
        loadView("/views/backoffice/profile.fxml");
    }

    @FXML
    public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void setActive(Button active) {
        Button[] buttons = {btnDashboard, btnUsers, btnExercices, btnProfile};
        for (Button b : buttons) {
            if (b != null) {
                b.setStyle(b == active ? ACTIVE_STYLE : INACTIVE_STYLE);
            }
        }
    }

    public void loadView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load: " + path + " — " + e.getMessage());
        }
    }
    @FXML private Button btnChallenges;

    @FXML
    public void navigateToChallenges() {
        setActive(btnChallenges);
        labelPageTitle.setText("Gestion des Challenges");
        loadView("/views/backoffice/challenge/challenges.fxml");
    }
    @FXML
    private void navigateToEvenements() {
        setActive(btnEvenements);
        labelPageTitle.setText("Gestion des Evenements");
        loadView("/views/backoffice/evenement/index.fxml");
    }
}
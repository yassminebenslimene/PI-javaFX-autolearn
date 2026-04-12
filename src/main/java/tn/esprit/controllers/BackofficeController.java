package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.MainApp;
import tn.esprit.session.SessionManager;

import java.io.IOException;

public class BackofficeController {

    @FXML private StackPane contentArea;
    @FXML private Label labelPageTitle;

    // Déclaration des boutons de la sidebar
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnEvenements;
    @FXML private Button btnQuiz;
    @FXML private Button btnCours;
    @FXML private Button btnChapitres;
    @FXML private Button btnExercices;
    @FXML private Button btnChallenges;
    @FXML private Button btnProfile;

    @FXML
    public void initialize() {
        MainApp.setBackofficeController(this);
        // Utiliser Platform.runLater pour s'assurer que tous les composants sont initialisés
        javafx.application.Platform.runLater(() -> {
            navigateToUsers();
        });
    }

    private void setActive(Button activeButton) {
        // Vérifier que tous les boutons sont non null avant de les modifier
        String defaultStyle = "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7);" +
                "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
                "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";

        String activeStyle = "-fx-background-color:rgba(122,106,216,0.25); -fx-text-fill:#a5b4fc;" +
                "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
                "-fx-font-size:13; -fx-font-weight:600; -fx-cursor:hand; -fx-border-width:0;";

        // Réinitialiser tous les boutons s'ils existent
        if (btnDashboard != null) btnDashboard.setStyle(defaultStyle);
        if (btnUsers != null) btnUsers.setStyle(defaultStyle);
        if (btnEvenements != null) btnEvenements.setStyle(defaultStyle);
        if (btnQuiz != null) btnQuiz.setStyle(defaultStyle);
        if (btnCours != null) btnCours.setStyle(defaultStyle);
        if (btnChapitres != null) btnChapitres.setStyle(defaultStyle);
        if (btnExercices != null) btnExercices.setStyle(defaultStyle);
        if (btnChallenges != null) btnChallenges.setStyle(defaultStyle);
        if (btnProfile != null) btnProfile.setStyle(defaultStyle);

        // Appliquer le style actif au bouton sélectionné
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    @FXML
    public void navigateToDashboard() {
        setActive(btnDashboard);
        labelPageTitle.setText("Dashboard");
        loadView("/views/backoffice/dashboard.fxml");
    }

    @FXML
    public void navigateToUsers() {
        setActive(btnUsers);
        labelPageTitle.setText("Gestion des Utilisateurs");
        loadView("/views/backoffice/user/index.fxml");
    }

    @FXML
    public void navigateToEvenements() {
        setActive(btnEvenements);
        labelPageTitle.setText("Gestion des Événements");
        loadView("/views/backoffice/evenement/index.fxml");
    }

    @FXML
    public void navigateToQuiz() {
        setActive(btnQuiz);
        labelPageTitle.setText("Gestion des Quiz");
        loadView("/views/backoffice/quiz/index.fxml");
    }

    @FXML
    public void navigateToCours() {
        setActive(btnCours);
        labelPageTitle.setText("Gestion des Cours");
        loadView("/views/backoffice/cours/index.fxml");
    }

    @FXML
    public void navigateToChapitres() {
        setActive(btnChapitres);
        labelPageTitle.setText("Gestion des Chapitres");
        loadView("/views/backoffice/chapitre/index.fxml");
    }

    @FXML
    public void navigateToExercices() {
        setActive(btnExercices);
        labelPageTitle.setText("Gestion des Exercices");
        loadView("/views/backoffice/exercice/exercices.fxml");
    }

    @FXML
    public void navigateToChallenges() {
        setActive(btnChallenges);
        labelPageTitle.setText("Gestion des Challenges");
        loadView("/views/backoffice/challenge/challenges.fxml");
    }

    @FXML
    public void navigateToProfile() {
        setActive(btnProfile);
        labelPageTitle.setText("Mon Profil");
        loadView("/views/backoffice/profile.fxml");
    }

    @FXML
    public void onLogout() {
        SessionManager.logout();
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
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
}
package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

import java.io.IOException;

public class FrontofficeController {

    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelNiveauStat;
    @FXML private Label welcomeLabel;
    @FXML private Button btnHome;  // ← AJOUTER CETTE LIGNE

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null) return;

        var u = SessionManager.getCurrentUser();
        String name = u.getPrenom() + " " + u.getNom();
        if (labelCurrentUser != null) labelCurrentUser.setText(name);
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");

        // Avatar initials
        String initials = u.getPrenom().substring(0,1).toUpperCase()
                + u.getNom().substring(0,1).toUpperCase();
        if (labelAvatarNav != null) labelAvatarNav.setText(initials);

        if (u instanceof Etudiant e && e.getNiveau() != null) {
            labelNiveauUser.setText("Niveau : " + e.getNiveau());
            if (labelNiveauStat != null) labelNiveauStat.setText(e.getNiveau());
        } else {
            labelNiveauUser.setText("");
            if (labelNiveauStat != null) labelNiveauStat.setText("—");
        }
    }

    @FXML private void onHome() { /* already on home */ }

    @FXML
    private void onEvenements() {
        try { MainApp.showEvenementsFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onProfile() {
        try {
            MainApp.showProfile();
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur profil");
            alert.setHeaderText(e.getClass().getSimpleName() + ": " + e.getMessage());
            alert.setContentText(cause.getClass().getSimpleName() + ": " + cause.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void onChallenges() {
        System.out.println("Bouton Challenges cliqué !");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onViewCourses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/cours/index.fxml"));
            javafx.scene.Parent root = loader.load();

            // Remplacer le contenu du centre
            if (btnHome != null) {
                BorderPane parent = (BorderPane) btnHome.getScene().getRoot();
                parent.setCenter(root);
            } else {
                // Alternative: utiliser MainApp
                MainApp.getPrimaryStage().getScene().setRoot(root);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page des cours");
        }
    }

    // Ajouter la méthode showAlert
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
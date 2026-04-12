package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

import java.io.IOException;

public class FrontofficeController {

    // These labels live in frontoffice/layout.fxml (not in the navbar component)
    @FXML private Label welcomeLabel;
    @FXML private Label labelNiveauStat;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;

    // Injected automatically by JavaFX because fx:id="navbar" → navbarController
    // Si vous n'avez pas de composant navbar, commentez ou supprimez cette ligne
    // @FXML private NavbarController navbarController;

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        // Afficher les informations utilisateur
        if (labelCurrentUser != null) {
            String name = u.getPrenom() + " " + u.getNom();
            labelCurrentUser.setText(name);
        }

        if (welcomeLabel != null)
            welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");

        // Avatar initials
        if (labelAvatarNav != null) {
            String initials = u.getPrenom().substring(0,1).toUpperCase()
                    + u.getNom().substring(0,1).toUpperCase();
            labelAvatarNav.setText(initials);
        }

        if (u instanceof Etudiant e && e.getNiveau() != null) {
            if (labelNiveauUser != null) labelNiveauUser.setText("Niveau : " + e.getNiveau());
            if (labelNiveauStat != null) labelNiveauStat.setText(e.getNiveau());
        }

        // Highlight "Accueil" in the shared navbar (commenté si pas de navbar)
        // if (navbarController != null)
        //     navbarController.setActive("Accueil");
    }

    @FXML
    public void onHome() {
        // Déjà sur la page d'accueil, ne rien faire ou recharger
        System.out.println("Page d'accueil");
    }

    @FXML
    public void onEvenements() {
        try {
            MainApp.showEvenementsFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onProfile() {
        try {
            MainApp.showProfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void onViewCourses() {
        System.out.println("Bouton Voir les cours cliqué !");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/cours/index.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
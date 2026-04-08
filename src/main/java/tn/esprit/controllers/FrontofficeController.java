package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

public class FrontofficeController {

    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelNiveauStat;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null) return;

        var u = SessionManager.getCurrentUser();
        String name = u.getPrenom() + " " + u.getNom();
        labelCurrentUser.setText(name);
        welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");

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
    private void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }
}

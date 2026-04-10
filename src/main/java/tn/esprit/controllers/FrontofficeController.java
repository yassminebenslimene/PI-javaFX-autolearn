package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

public class FrontofficeController {

    // These labels live in frontoffice/layout.fxml (not in the navbar component)
    @FXML private Label welcomeLabel;
    @FXML private Label labelNiveauStat;

    // Injected automatically by JavaFX because fx:id="navbar" → navbarController
    @FXML private NavbarController navbarController;

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        if (welcomeLabel != null)
            welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");

        if (u instanceof Etudiant e && e.getNiveau() != null) {
            if (labelNiveauStat != null) labelNiveauStat.setText(e.getNiveau());
        }

        // Highlight "Accueil" in the shared navbar
        if (navbarController != null)
            navbarController.setActive("Accueil");
    }
}

package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FrontChapitreDetailController {

    @FXML private Label labelBadge;
    @FXML private Label labelCours;
    @FXML private Label labelTitre;
    @FXML private Label labelContenu;
    @FXML private Label labelRessource;
    @FXML private VBox  boxRessource;
    @FXML private Button btnSuivant;

    private Cours cours;
    private List<Chapitre> chapitres;
    private int currentIndex;

    private Runnable onRetourCallback;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    /** Appelé par FrontChapitreController pour injecter le contexte */
    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours = cours;
        this.onRetourCallback = onRetour;
        this.chapitres = serviceChapitre.consulterParCoursId(cours.getId());
        this.currentIndex = chapitres.indexOf(chapitre);
        // fallback si indexOf retourne -1 (objet différent mais même id)
        if (currentIndex < 0) {
            for (int i = 0; i < chapitres.size(); i++) {
                if (chapitres.get(i).getId() == chapitre.getId()) {
                    currentIndex = i;
                    break;
                }
            }
        }
        afficher(chapitre);
    }

    private void afficher(Chapitre chapitre) {
        labelBadge.setText("📌  Chapitre " + chapitre.getOrdre());
        labelCours.setText("🎓  " + cours.getTitre());
        labelTitre.setText(chapitre.getTitre());
        labelContenu.setText(chapitre.getContenu() == null ? "" : chapitre.getContenu());

        // Ressource
        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            labelRessource.setText(res);
            boxRessource.setVisible(true);
            boxRessource.setManaged(true);
        } else {
            boxRessource.setVisible(false);
            boxRessource.setManaged(false);
        }

        // Bouton suivant
        boolean hasSuivant = currentIndex < chapitres.size() - 1;
        btnSuivant.setVisible(hasSuivant);
        btnSuivant.setManaged(hasSuivant);
    }

    @FXML
    private void onRetour() {
        if (onRetourCallback != null) onRetourCallback.run();
    }

    @FXML
    private void onSuivant() {
        if (currentIndex < chapitres.size() - 1) {
            currentIndex++;
            afficher(chapitres.get(currentIndex));
        }
    }
}

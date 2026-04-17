package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;

import java.util.List;

/**
 * FrontChapitreDetailController — affiche le contenu complet d'un chapitre.
 * Vue : frontoffice/chapitre/detail.fxml
 */
public class FrontChapitreDetailController {

    @FXML private Label  labelBadge;
    @FXML private Label  labelCours;
    @FXML private Label  labelTitre;
    @FXML private Label  labelContenu;
    @FXML private Label  labelRessource;
    @FXML private VBox   boxRessource;
    @FXML private Button btnSuivant;
    @FXML private Button btnQuiz;   // bouton "Passer le quiz" (onAction="#onQuiz" dans le FXML)

    private Cours          cours;
    private List<Chapitre> chapitres;
    private int            currentIndex;

    private Runnable onRetourCallback;
    private Runnable onQuizCallback;   // injecté par FrontofficeController

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    /** Injecte le contexte : cours parent, chapitre à afficher, callback retour. */
    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours            = cours;
        this.onRetourCallback = onRetour;
        this.chapitres        = serviceChapitre.consulterParCoursId(cours.getId());

        this.currentIndex = chapitres.indexOf(chapitre);
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

    /** Injecté par FrontofficeController pour brancher le bouton quiz. */
    public void setOnQuizCallback(Runnable callback) {
        this.onQuizCallback = callback;
    }

    private void afficher(Chapitre chapitre) {
        labelBadge.setText("📌  Chapitre " + chapitre.getOrdre());
        labelCours.setText("🎓  " + cours.getTitre());
        labelTitre.setText(chapitre.getTitre());
        labelContenu.setText(chapitre.getContenu() == null ? "" : chapitre.getContenu());

        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            labelRessource.setText(res);
            boxRessource.setVisible(true);
            boxRessource.setManaged(true);
        } else {
            boxRessource.setVisible(false);
            boxRessource.setManaged(false);
        }

        boolean hasSuivant = currentIndex < chapitres.size() - 1;
        btnSuivant.setVisible(hasSuivant);
        btnSuivant.setManaged(hasSuivant);
    }

    @FXML
    private void onRetour() {
        if (onRetourCallback != null) onRetourCallback.run();
    }

    @FXML
    private void onQuiz() {
        if (onQuizCallback != null) onQuizCallback.run();
    }

    @FXML
    private void onSuivant() {
        if (currentIndex < chapitres.size() - 1) {
            currentIndex++;
            afficher(chapitres.get(currentIndex));
        }
    }
}

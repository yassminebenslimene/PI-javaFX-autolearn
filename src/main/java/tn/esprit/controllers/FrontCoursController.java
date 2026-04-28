package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * FrontCoursController — affiche la liste des cours côté étudiant (frontoffice).
 * Vue : frontoffice/cours/index.fxml
 *
 * Chaque cours est affiché sous forme de carte (card) avec :
 * - titre, matière, niveau, durée
 * - description
 * - badge avec le nombre de chapitres
 * - bouton "Voir chapitres" qui déclenche la navigation vers les chapitres
 *
 * La navigation est gérée via un callback (onVoirChapitres) injecté par FrontofficeController.
 * Ce pattern évite le couplage direct entre les controllers.
 */
public class FrontCoursController {

    // ── Composants FXML ───────────────────────────────────────────────────────
    @FXML private VBox  cardsContainer;   // conteneur où les cartes cours sont ajoutées dynamiquement
    @FXML private Label labelTotalCours;  // affiche le nombre total de cours
    @FXML private Label labelEmpty;       // message affiché si aucun cours n'existe

    // ── Services ──────────────────────────────────────────────────────────────
    private final ServiceCours    serviceCours    = new ServiceCours();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // ── Callback de navigation ────────────────────────────────────────────────
    // Injecté par FrontofficeController pour naviguer vers les chapitres d'un cours
    private Consumer<Cours> onVoirChapitres;

    /** Injecte le callback appelé quand l'étudiant clique sur "Voir chapitres". */
    public void setOnVoirChapitres(Consumer<Cours> onVoirChapitres) {
        this.onVoirChapitres = onVoirChapitres;
    }

    // ── CHARGEMENT DES DONNÉES ────────────────────────────────────────────────
    /**
     * Charge tous les cours depuis la BDD et crée une carte visuelle pour chacun.
     * Appelé par FrontofficeController après avoir injecté le callback.
     */
    public void loadData() {
        List<Cours> coursList = serviceCours.consulter();

        // Compter les chapitres par cours pour afficher le badge
        Map<Integer, Integer> countByCours = new HashMap<>();
        serviceChapitre.consulter().forEach(ch -> countByCours.merge(ch.getCoursId(), 1, Integer::sum));

        labelTotalCours.setText(String.valueOf(coursList.size()));
        cardsContainer.getChildren().clear(); // vider les cartes précédentes

        if (coursList.isEmpty()) {
            labelEmpty.setVisible(true);
            labelEmpty.setManaged(true);
            return;
        }
        labelEmpty.setVisible(false);
        labelEmpty.setManaged(false);

        // Créer une carte pour chaque cours
        for (Cours cours : coursList) {
            int nbChapitres = countByCours.getOrDefault(cours.getId(), 0);

            // Titre du cours
            Label titre = new Label(cours.getTitre());
            titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:#0f172a;");

            // Métadonnées : matière, niveau, durée
            Label meta = new Label("Matiere: " + cours.getMatiere() + " | Niveau: " + cours.getNiveau() + " | Duree: " + cours.getDuree() + "h");
            meta.setStyle("-fx-font-size:12; -fx-text-fill:#475569; -fx-font-weight:600;");

            // Description complète du cours
            Label description = new Label(cours.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-font-size:13; -fx-text-fill:#334155; -fx-line-spacing:2;");

            // Badge bleu avec le nombre de chapitres
            Label badge = new Label(nbChapitres + " chapitres");
            badge.setStyle("-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8; -fx-font-size:11; -fx-font-weight:700; -fx-padding:4 10 4 10; -fx-background-radius:999;");

            // Bouton "Voir chapitres" → déclenche le callback de navigation
            Button btnVoir = new Button("Voir chapitres");
            btnVoir.setStyle("-fx-background-color:linear-gradient(to right,#0ea5e9,#2563eb); -fx-text-fill:white; -fx-font-weight:700; -fx-padding:8 16 8 16; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
            btnVoir.setOnAction(e -> {
                if (onVoirChapitres != null) onVoirChapitres.accept(cours);
            });

            HBox footer = new HBox(12, badge, btnVoir);
            footer.setAlignment(Pos.CENTER_LEFT);

            // Assembler la carte
            VBox card = new VBox(10, titre, meta, description, footer);
            card.setPadding(new Insets(18));
            card.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#e2e8f0; -fx-border-radius:14; -fx-effect:dropshadow(gaussian,rgba(15,23,42,0.08),10,0,0,3);");
            VBox.setVgrow(card, Priority.NEVER);

            cardsContainer.getChildren().add(card);
        }
    }
}

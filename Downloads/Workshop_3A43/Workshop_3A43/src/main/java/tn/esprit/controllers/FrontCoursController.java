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
 * FrontCoursController ÔÇö affiche la liste des cours c├┤t├® ├®tudiant (frontoffice).
 * Vue : frontoffice/cours/index.fxml
 *
 * Chaque cours est affich├® sous forme de carte (card) avec :
 * - titre, mati├¿re, niveau, dur├®e
 * - description
 * - badge avec le nombre de chapitres
 * - bouton "Voir chapitres" qui d├®clenche la navigation vers les chapitres
 *
 * La navigation est g├®r├®e via un callback (onVoirChapitres) inject├® par FrontofficeController.
 * Ce pattern ├®vite le couplage direct entre les controllers.
 */
public class FrontCoursController {

    // ÔöÇÔöÇ Composants FXML ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML private VBox  cardsContainer;   // conteneur o├╣ les cartes cours sont ajout├®es dynamiquement
    @FXML private Label labelTotalCours;  // affiche le nombre total de cours
    @FXML private Label labelEmpty;       // message affich├® si aucun cours n'existe

    // ÔöÇÔöÇ Services ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    private final ServiceCours    serviceCours    = new ServiceCours();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // ÔöÇÔöÇ Callback de navigation ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Inject├® par FrontofficeController pour naviguer vers les chapitres d'un cours
    private Consumer<Cours> onVoirChapitres;

    /** Injecte le callback appel├® quand l'├®tudiant clique sur "Voir chapitres". */
    public void setOnVoirChapitres(Consumer<Cours> onVoirChapitres) {
        this.onVoirChapitres = onVoirChapitres;
    }

    // ÔöÇÔöÇ CHARGEMENT DES DONN├ëES ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /**
     * Charge tous les cours depuis la BDD et cr├®e une carte visuelle pour chacun.
     * Appel├® par FrontofficeController apr├¿s avoir inject├® le callback.
     */
    public void loadData() {
        List<Cours> coursList = serviceCours.consulter();

        // Compter les chapitres par cours pour afficher le badge
        Map<Integer, Integer> countByCours = new HashMap<>();
        serviceChapitre.consulter().forEach(ch -> countByCours.merge(ch.getCoursId(), 1, Integer::sum));

        labelTotalCours.setText(String.valueOf(coursList.size()));
        cardsContainer.getChildren().clear(); // vider les cartes pr├®c├®dentes

        if (coursList.isEmpty()) {
            labelEmpty.setVisible(true);
            labelEmpty.setManaged(true);
            return;
        }
        labelEmpty.setVisible(false);
        labelEmpty.setManaged(false);

        // Cr├®er une carte pour chaque cours
        for (Cours cours : coursList) {
            int nbChapitres = countByCours.getOrDefault(cours.getId(), 0);

            // Titre du cours
            Label titre = new Label(cours.getTitre());
            titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:#0f172a;");

            // M├®tadonn├®es : mati├¿re, niveau, dur├®e
            Label meta = new Label("Matiere: " + cours.getMatiere() + " | Niveau: " + cours.getNiveau() + " | Duree: " + cours.getDuree() + "h");
            meta.setStyle("-fx-font-size:12; -fx-text-fill:#475569; -fx-font-weight:600;");

            // Description compl├¿te du cours
            Label description = new Label(cours.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-font-size:13; -fx-text-fill:#334155; -fx-line-spacing:2;");

            // Badge bleu avec le nombre de chapitres
            Label badge = new Label(nbChapitres + " chapitres");
            badge.setStyle("-fx-background-color:#dbeafe; -fx-text-fill:#1d4ed8; -fx-font-size:11; -fx-font-weight:700; -fx-padding:4 10 4 10; -fx-background-radius:999;");

            // Bouton "Voir chapitres" ÔåÆ d├®clenche le callback de navigation
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
package tn.esprit.controllers.evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.EquipeService;
import tn.esprit.services.ParticipationService;

import java.net.URL;
import java.time.format.DateTimeFormatter;

public class EvenementShowController {

    @FXML private Label labelTitre;
    @FXML private Label labelType;
    @FXML private Label labelDescription;
    @FXML private Label labelLieu;
    @FXML private Label labelDateDebut;
    @FXML private Label labelDateFin;
    @FXML private Label labelStatut;
    @FXML private Label labelAnnule;
    @FXML private Label labelNbMax;
    @FXML private Label labelNbEquipes;
    @FXML private Label labelNbParticipations;

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private final ParticipationService participationService = new ParticipationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Evenement evenement;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        populate();
    }

    private void populate() {
        labelTitre.setText(evenement.getTitre());
        labelType.setText(evenement.getType());
        labelDescription.setText(evenement.getDescription());
        labelLieu.setText(evenement.getLieu());
        labelDateDebut.setText(evenement.getDateDebut() != null ? evenement.getDateDebut().format(FMT) : "—");
        labelDateFin.setText(evenement.getDateFin() != null ? evenement.getDateFin().format(FMT) : "—");

        String statut = evenement.computeStatus();
        labelStatut.setText("● " + capitalize(statut));
        labelStatut.setStyle(getStatutBadgeStyle(statut));

        labelAnnule.setText(evenement.isIsCanceled() ? "Oui" : "Non");
        labelNbMax.setText(String.valueOf(evenement.getNbMax()));

        // Compter équipes et participations depuis la BD
        int nbEquipes = equipeService.countByEvenement(evenement.getId());
        int nbParticipations = participationService.countByEvenement(evenement.getId());
        labelNbEquipes.setText(String.valueOf(nbEquipes));
        labelNbParticipations.setText(String.valueOf(nbParticipations));
    }

    @FXML
    private void onModifier() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/form.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            EvenementFormController ctrl = loader.getController();
            ctrl.setEvenement(evenement);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/index.fxml");
            Parent view = FXMLLoader.load(resource);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getStatutBadgeStyle(String statut) {
        return switch (statut) {
            case "Plannifié" -> "-fx-text-fill:#60a5fa; -fx-background-color:rgba(96,165,250,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "En cours"  -> "-fx-text-fill:#34d399; -fx-background-color:rgba(52,211,153,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "Passé"     -> "-fx-text-fill:#4ade80; -fx-background-color:rgba(74,222,128,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "Annulé"    -> "-fx-text-fill:#fbbf24; -fx-background-color:rgba(251,191,36,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            default          -> "-fx-text-fill:white; -fx-font-size:12;";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase().replace("_", " ");
    }

    private StackPane getContentArea() {
        return (StackPane) labelTitre.getScene().lookup("#contentArea");
    }
}

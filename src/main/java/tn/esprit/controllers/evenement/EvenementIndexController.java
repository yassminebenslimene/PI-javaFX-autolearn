package tn.esprit.controllers.evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class EvenementIndexController implements Initializable {

    @FXML private VBox tableRows;

    private final EvenementService service = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadTable();
    }

    private void loadTable() {
        tableRows.getChildren().clear();
        List<Evenement> list = service.getAll();
        for (int i = 0; i < list.size(); i++) {
            Evenement e = list.get(i);
            HBox row = buildRow(e, i % 2 == 0);
            tableRows.getChildren().add(row);
        }
    }

    private HBox buildRow(Evenement e, boolean even) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setStyle(even
                ? "-fx-background-color:rgba(255,255,255,0.02); -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;"
                : "-fx-background-color:transparent; -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;");

        // Titre
        Label lTitre = new Label(e.getTitre());
        lTitre.setPrefWidth(180);
        lTitre.setStyle("-fx-text-fill:white; -fx-font-size:13;");

        // Type
        Label lType = new Label(e.getType());
        lType.setPrefWidth(100);
        lType.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        // Date début
        Label lDebut = new Label(e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "—");
        lDebut.setPrefWidth(120);
        lDebut.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        // Date fin
        Label lFin = new Label(e.getDateFin() != null ? e.getDateFin().format(FMT) : "—");
        lFin.setPrefWidth(120);
        lFin.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        // Statut badge
        String statut = e.computeStatus();
        Label lStatut = new Label("● " + statut);
        lStatut.setPrefWidth(100);
        lStatut.setStyle(getStatutStyle(statut));

        // Places max
        Label lNbMax = new Label(String.valueOf(e.getNbMax()));
        lNbMax.setPrefWidth(80);
        lNbMax.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        // Actions
        HBox actions = new HBox(6);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button btnVoir = new Button("👁 Voir");
        btnVoir.setMinWidth(70);
        btnVoir.setStyle("-fx-background-color:#7c3aed; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnVoir.setOnAction(ev -> onVoir(e));

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setMinWidth(85);
        btnModifier.setStyle("-fx-background-color:#db2777; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnModifier.setOnAction(ev -> onModifier(e));

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setMinWidth(95);
        btnSupprimer.setStyle("-fx-background-color:#e11d48; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnSupprimer.setOnAction(ev -> onSupprimer(e));

        actions.getChildren().addAll(btnVoir, btnModifier, btnSupprimer);

        // Bouton Annuler si pas encore annulé
        if (!e.isIsCanceled() && !"Passé".equals(statut)) {
            Button btnAnnuler = new Button("✖ Annuler");
            btnAnnuler.setMinWidth(85);
            btnAnnuler.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
            btnAnnuler.setOnAction(ev -> onAnnuler(e));
            actions.getChildren().add(btnAnnuler);
        }

        row.getChildren().addAll(lTitre, lType, lDebut, lFin, lStatut, lNbMax, actions);
        return row;
    }

    private String getStatutStyle(String statut) {
        return switch (statut) {
            case "Plannifié" -> "-fx-text-fill:#60a5fa; -fx-font-size:12; -fx-font-weight:bold;";
            case "En cours"  -> "-fx-text-fill:#34d399; -fx-font-size:12; -fx-font-weight:bold;";
            case "Passé"     -> "-fx-text-fill:#4ade80; -fx-font-size:12; -fx-font-weight:bold;";
            case "Annulé"    -> "-fx-text-fill:#fbbf24; -fx-font-size:12; -fx-font-weight:bold;";
            default          -> "-fx-text-fill:rgba(255,255,255,0.6); -fx-font-size:12;";
        };
    }


    @FXML
    private void onAjouter() {
        loadView("/views/backoffice/evenement/form.fxml", null);
    }

    private void onVoir(Evenement e) {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/show.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent view = loader.load();
            EvenementShowController ctrl = loader.getController();
            ctrl.setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onModifier(Evenement e) {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/form.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent view = loader.load();
            EvenementFormController ctrl = loader.getController();
            ctrl.setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onSupprimer(Evenement e) {
        service.supprimer(e.getId());
        loadTable();
    }

    private void onAnnuler(Evenement e) {
        e.setIsCanceled(true);
        e.setWorkflowStatus("annule");
        e.setStatus("Annulé");
        service.modifier(e);
        loadTable();
    }

    private void loadView(String fxml, Evenement e) {
        try {
            URL resource = getClass().getResource(fxml);
            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent view = loader.load();
            if (e != null) {
                EvenementFormController ctrl = loader.getController();
                ctrl.setEvenement(e);
            }
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private javafx.scene.layout.StackPane getContentArea() {
        return (javafx.scene.layout.StackPane) tableRows.getScene().lookup("#contentArea");
    }
}

package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class SelectEventController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private VBox eventsContainer;

    private final EvenementService evenementService = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
                loadEvents();
    }

    private void loadEvents() {
        eventsContainer.getChildren().clear();
        List<Evenement> evenements = evenementService.getAll().stream()
                .filter(ev -> !ev.isIsCanceled() && !"Annule".equals(ev.getStatus()))
                .toList();

        if (evenements.isEmpty()) {
            Label empty = new Label("Aucun evenement disponible.");
            empty.setStyle("-fx-font-size:14; -fx-text-fill:#888;");
            eventsContainer.getChildren().add(empty);
            return;
        }

        for (Evenement ev : evenements) {
            eventsContainer.getChildren().add(buildEventRow(ev));
        }
    }

    private HBox buildEventRow(Evenement ev) {
        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 20, 16, 20));
        row.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                + "-fx-border-color:#eeeeee; -fx-border-radius:12; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Type badge
        Label typeBadge = new Label(ev.getType() != null ? ev.getType().toUpperCase() : "");
        typeBadge.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:10;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 10 3 10;");

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        HBox meta = new HBox(16);
        if (ev.getDateDebut() != null) {
            Label date = new Label("&#128197; " + ev.getDateDebut().format(FMT));
            date.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
            meta.getChildren().add(date);
        }
        if (ev.getLieu() != null) {
            Label lieu = new Label("&#128205; " + ev.getLieu());
            lieu.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
            meta.getChildren().add(lieu);
        }
        info.getChildren().addAll(titre, meta);

        // Select button
        Button selectBtn = new Button("Selectionner");
        selectBtn.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12;"
                + "-fx-font-weight:700; -fx-padding:9 24 9 24; -fx-background-radius:10;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        selectBtn.setOnAction(e -> {
            try { MainApp.showCreateTeam(ev); } catch (Exception ex) { ex.printStackTrace(); }
        });

        row.getChildren().addAll(typeBadge, info, selectBtn);
        return row;
    }

    @FXML private void onBack() { FrontNavHelper.goMesEquipes(null); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}

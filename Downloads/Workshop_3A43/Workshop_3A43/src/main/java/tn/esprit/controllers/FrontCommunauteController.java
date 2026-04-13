package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ServiceCommunaute;
import tn.esprit.session.SessionManager;

import java.util.List;

public class FrontCommunauteController {

    @FXML private FlowPane cardsPane;
    @FXML private Label emptyLabel;
    @FXML private TextField searchField;

    private final ServiceCommunaute service = new ServiceCommunaute();
    private List<Communaute> allCommunautes;
    private Runnable onRetour;

    public void setOnRetour(Runnable r) { this.onRetour = r; }

    @FXML
    public void initialize() {
        allCommunautes = service.getList();
        searchField.textProperty().addListener((obs, o, n) -> afficher(n));
        afficher("");
    }

    private void afficher(String query) {
        cardsPane.getChildren().clear();
        List<Communaute> filtered = allCommunautes.stream()
            .filter(c -> query == null || query.isBlank()
                      || c.getNom().toLowerCase().contains(query.toLowerCase()))
            .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        for (Communaute c : filtered) {
            cardsPane.getChildren().add(buildCard(c));
        }
    }

    private VBox buildCard(Communaute c) {
        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        boolean hasAccess = c.getOwnerId() == currentUserId
                || c.getMemberIds().contains(currentUserId);

        VBox card = new VBox(10);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14; " +
                      "-fx-border-color:#eeeeee; -fx-border-radius:14; " +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3); -fx-padding:22;");

        // Icône + badge cadenas si accès restreint
        HBox iconRow = new HBox(8);
        iconRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size:28; -fx-background-color:rgba(122,106,216,0.1); " +
                      "-fx-background-radius:12; -fx-padding:10 12 10 12;");
        iconRow.getChildren().add(icon);
        if (!hasAccess) {
            Label lock = new Label("🔒");
            lock.setStyle("-fx-font-size:14; -fx-text-fill:#e94560;");
            iconRow.getChildren().add(lock);
        }

        Label nom = new Label(c.getNom());
        nom.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");

        Label desc = new Label(c.getDescription() != null ? c.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:12; -fx-text-fill:#666;");

        javafx.scene.control.Button btn;
        if (hasAccess) {
            btn = new javafx.scene.control.Button("Voir la communauté →");
            btn.setStyle("-fx-background-color:linear-gradient(to right,#7a6ad8,#4e3b9c); " +
                         "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700; " +
                         "-fx-padding:9 20 9 20; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
            btn.setOnAction(e -> ouvrirDetail(c));
        } else {
            btn = new javafx.scene.control.Button("🔒  Accès restreint");
            btn.setStyle("-fx-background-color:#f5f5f5; -fx-text-fill:#aaa; -fx-font-size:12; " +
                         "-fx-padding:9 20 9 20; -fx-background-radius:10; -fx-border-width:0;");
            btn.setDisable(true);
        }

        card.getChildren().addAll(iconRow, nom, desc, btn);
        return card;
    }

    private void ouvrirDetail(Communaute c) {
        try {
            // Recharger depuis la DB pour avoir l'ID et les membres à jour
            Communaute fresh = service.getById(c.getId());
            if (fresh == null) fresh = c;

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/communaute/detail.fxml"));
            Parent view = loader.load();
            FrontCommunauteDetailController ctrl = loader.getController();
            ctrl.setCommunaute(fresh, () -> setCenter(buildSelf()));
            setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onCreer() {
        // Seul un utilisateur connecté peut créer
        if (SessionManager.getCurrentUser() == null) return;
        // Ouvrir un dialog simple
        javafx.scene.control.Dialog<Communaute> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Nouvelle Communauté");
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));
        TextField fNom = new TextField(); fNom.setPromptText("Nom *");
        javafx.scene.control.TextArea fDesc = new javafx.scene.control.TextArea();
        fDesc.setPromptText("Description"); fDesc.setPrefRowCount(3);
        content.getChildren().addAll(new Label("Nom :"), fNom, new Label("Description :"), fDesc);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK && !fNom.getText().isBlank()) {
                return new Communaute(fNom.getText().trim(), fDesc.getText().trim(),
                                      SessionManager.getCurrentUser().getId());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(c -> {
            service.ajouter(c);
            allCommunautes = service.getList();
            afficher(searchField.getText());
        });
    }

    private void setCenter(Parent view) {
        if (cardsPane.getScene() == null) return;
        BorderPane root = (BorderPane) cardsPane.getScene().getRoot();
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(view);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
        root.setCenter(sp);
    }

    private Parent buildSelf() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/communaute/index.fxml"));
            return loader.load();
        } catch (Exception e) { e.printStackTrace(); return new VBox(); }
    }
}

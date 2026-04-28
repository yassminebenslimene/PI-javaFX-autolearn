package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ServiceCommunaute;
import tn.esprit.session.SessionManager;
import tn.esprit.controllers.FrontCoursController;

import java.util.List;

public class FrontCommunauteController {

    @FXML private FlowPane cardsPane;
    @FXML private Label emptyLabel;
    @FXML private TextField searchField;

    private final ServiceCommunaute service = new ServiceCommunaute();
    private List<Communaute> allCommunautes;
    private Runnable onRetour;
    private java.util.function.Consumer<Communaute> onOuvrirDetail;

    public void setOnRetour(Runnable r) { this.onRetour = r; }
    public void setOnOuvrirDetail(java.util.function.Consumer<Communaute> cb) { this.onOuvrirDetail = cb; }

    /** Pre-fills the search field with a course name so relevant communities show up */
    public void preselectCours(String coursNom) {
        if (searchField != null && coursNom != null) {
            searchField.setText(coursNom);
            afficher(coursNom);
        }
    }

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
        List<Integer> memberIds = c.getMemberIds() != null ? c.getMemberIds() : java.util.Collections.emptyList();
        boolean hasAccess = c.getOwnerId() == currentUserId
                || memberIds.contains(currentUserId);

        VBox card = new VBox(16);
        card.setPrefWidth(300);
        card.setUserData(c);
        card.setStyle("-fx-background-color:white; -fx-background-radius:24; " +
                      "-fx-border-color:#ede9fe; -fx-border-radius:24; -fx-border-width:1.5; " +
                      "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.1),22,0,0,7); -fx-padding:28;");

        // ── Card header: icon + 3-dot menu (owner only) ──────────────────
        HBox iconRow = new HBox(10);
        iconRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size:22; -fx-background-color:#f5f3ff; " +
                      "-fx-background-radius:14; -fx-padding:13;");
        iconRow.getChildren().add(icon);
        if (!hasAccess) {
            Label lock = new Label("🔒");
            lock.setStyle("-fx-font-size:14; -fx-text-fill:#e94560;");
            iconRow.getChildren().add(lock);
        }

        // 3-dot menu for owner
        boolean isOwner = c.getOwnerId() == currentUserId;
        if (isOwner) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            javafx.scene.control.Button btnDots = new javafx.scene.control.Button("⋯");
            btnDots.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                             "-fx-font-size:16; -fx-font-weight:900;" +
                             "-fx-padding:4 12; -fx-background-radius:20;" +
                             "-fx-cursor:hand; -fx-border-width:0;");
            btnDots.setOnMouseEntered(e -> btnDots.setStyle(
                    "-fx-background-color:#ede9fe; -fx-text-fill:#6d28d9;" +
                    "-fx-font-size:16; -fx-font-weight:900;" +
                    "-fx-padding:4 12; -fx-background-radius:20;" +
                    "-fx-cursor:hand; -fx-border-width:0;"));
            btnDots.setOnMouseExited(e -> btnDots.setStyle(
                    "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                    "-fx-font-size:16; -fx-font-weight:900;" +
                    "-fx-padding:4 12; -fx-background-radius:20;" +
                    "-fx-cursor:hand; -fx-border-width:0;"));

            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                          "-fx-border-color:#ede9fe; -fx-border-radius:14; -fx-border-width:1.5;" +
                          "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.15),16,0,0,4);" +
                          "-fx-padding:6 0 6 0;");

            MenuItem itemEdit = new MenuItem("  ✏  Modifier");
            itemEdit.setStyle("-fx-font-size:13; -fx-padding:10 20 10 20;");
            itemEdit.setOnAction(e -> onModifier(c));

            MenuItem itemDel = new MenuItem("  🗑  Supprimer");
            itemDel.setStyle("-fx-font-size:13; -fx-padding:10 20 10 20; -fx-text-fill:#e94560;");
            itemDel.setOnAction(e -> onSupprimer(c));

            menu.getItems().addAll(itemEdit, new SeparatorMenuItem(), itemDel);
            btnDots.setOnAction(e -> menu.show(btnDots, javafx.geometry.Side.BOTTOM, 0, 4));

            iconRow.getChildren().addAll(spacer, btnDots);
        }

        Label nom = new Label(c.getNom());
        nom.setStyle("-fx-font-size:18; -fx-font-weight:900; -fx-text-fill:#1e1b4b;");

        Label desc = new Label(c.getDescription() != null ? c.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:12; -fx-text-fill:#6b7280; -fx-line-spacing:4;");

        javafx.scene.control.Button btn;
        if (hasAccess) {
            btn = new javafx.scene.control.Button("Rejoindre →");
            btn.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); " +
                         "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:900; " +
                         "-fx-padding:13 30; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0; " +
                         "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.4),14,0,0,5);");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:linear-gradient(to right,#6d28d9,#4338ca); " +
                                                    "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:900; " +
                                                    "-fx-padding:13 30; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0; " +
                                                    "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.55),18,0,0,7);"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); " +
                                                   "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:900; " +
                                                   "-fx-padding:13 30; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0; " +
                                                   "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.4),14,0,0,5);"));
            btn.setOnAction(e -> {
                if (onOuvrirDetail != null) onOuvrirDetail.accept(c);
                else ouvrirDetail(c);
            });
        } else {
            btn = new javafx.scene.control.Button("🔒  Accès restreint");
            btn.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#c4b5fd; -fx-font-size:12; " +
                         "-fx-padding:11 24; -fx-background-radius:30; -fx-border-width:0;");
            btn.setDisable(true);
        }

        card.getChildren().addAll(iconRow, nom, desc, btn);

        return card;
    }

    private void ouvrirDetail(Communaute c) {
        try {
            Communaute fresh = service.getById(c.getId());
            if (fresh == null) fresh = c;
            var resource = getClass().getResource("/views/frontoffice/communaute/detail.fxml");
            if (resource == null) {
                showUiError("Impossible d'ouvrir la communauté", "Le fichier de vue detail.fxml est introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent view = loader.load();
            FrontCommunauteDetailController ctrl = loader.getController();
            ctrl.setCommunaute(fresh, () -> {
                try {
                    // Go back to layout with community list
                    FrontofficeController.setPendingSection("communaute");
                    tn.esprit.MainApp.showFrontoffice();
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            // ── Resource navigation callbacks ──────────────────────────────
            ctrl.setOnNavigateToCours(coursId -> {
                try {
                    FrontofficeController.setPendingSection("cours");
                    tn.esprit.MainApp.showFrontoffice();
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            ctrl.setOnNavigateToQuiz(quizId -> {
                try {
                    FrontofficeController.setPendingSection("cours");
                    tn.esprit.MainApp.showFrontoffice();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            
            // Load detail inside layout's center (extract center from BorderPane)
            if (view instanceof BorderPane bp && bp.getCenter() != null) {
                setCenter((Parent) bp.getCenter());
            } else {
                setCenter(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String details = cause.getMessage() != null ? ("\n\nDétail: " + cause.getMessage()) : "";
            showUiError("Impossible d'ouvrir la communauté", "Une erreur est survenue pendant le chargement de la page détail." + details);
        }
    }

    private void showUiError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.showAndWait();
    }

    @FXML
    public void onCreer() {
        if (SessionManager.getCurrentUser() == null) return;

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
        dialog.setResizable(false);

        // ── Root ──────────────────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#ffffff; -fx-background-radius:20;"
            + "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.25),40,0,0,12);");
        root.setPrefWidth(460);

        // ── Header ────────────────────────────────────────────────────────────
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);"
            + "-fx-background-radius:20 20 0 0; -fx-padding:24 28 20 28;");

        javafx.scene.layout.HBox titleRow = new javafx.scene.layout.HBox(10);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label iconLbl = new Label("👥");
        iconLbl.setStyle("-fx-font-size:22;");
        Label titleLbl = new Label("Nouvelle Communauté");
        titleLbl.setStyle("-fx-font-size:18; -fx-font-weight:800; -fx-text-fill:white;");
        titleRow.getChildren().addAll(iconLbl, titleLbl);

        Label subLbl = new Label("Créez un espace de partage pour votre communauté");
        subLbl.setStyle("-fx-font-size:12; -fx-text-fill:rgba(255,255,255,0.75);");
        header.getChildren().addAll(titleRow, subLbl);

        // ── Body ──────────────────────────────────────────────────────────────
        VBox body = new VBox(18);
        body.setStyle("-fx-padding:28 28 8 28;");

        // Champ Nom
        VBox nomBox = new VBox(6);
        Label nomLbl = new Label("Nom de la communauté *");
        nomLbl.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#374151;");
        TextField fNom = new TextField();
        fNom.setPromptText("Ex: Python Avancé, IA & Machine Learning...");
        fNom.setStyle("-fx-background-color:#f9fafb; -fx-border-color:#e5e7eb; -fx-border-radius:10;"
            + "-fx-background-radius:10; -fx-padding:11 14; -fx-font-size:13; -fx-text-fill:#111827;"
            + "-fx-border-width:1.5;");
        fNom.focusedProperty().addListener((o, ov, nv) -> fNom.setStyle(
            "-fx-background-color:" + (nv ? "white" : "#f9fafb")
            + "; -fx-border-color:" + (nv ? "#7c3aed" : "#e5e7eb")
            + "; -fx-border-radius:10; -fx-background-radius:10;"
            + "-fx-padding:11 14; -fx-font-size:13; -fx-text-fill:#111827; -fx-border-width:1.5;"));
        nomBox.getChildren().addAll(nomLbl, fNom);

        // Champ Description
        VBox descBox = new VBox(6);
        Label descLbl = new Label("Description *");
        descLbl.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#374151;");
        javafx.scene.control.TextArea fDesc = new javafx.scene.control.TextArea();
        fDesc.setPromptText("Décrivez l'objectif et le contenu de votre communauté (min 15 caractères)...");
        fDesc.setPrefRowCount(4);
        fDesc.setWrapText(true);
        fDesc.setStyle("-fx-background-color:#f9fafb; -fx-border-color:#e5e7eb; -fx-border-radius:10;"
            + "-fx-background-radius:10; -fx-padding:11 14; -fx-font-size:13; -fx-text-fill:#111827;"
            + "-fx-border-width:1.5;");
        fDesc.focusedProperty().addListener((o, ov, nv) -> fDesc.setStyle(
            "-fx-background-color:" + (nv ? "white" : "#f9fafb")
            + "; -fx-border-color:" + (nv ? "#7c3aed" : "#e5e7eb")
            + "; -fx-border-radius:10; -fx-background-radius:10;"
            + "-fx-padding:11 14; -fx-font-size:13; -fx-text-fill:#111827; -fx-border-width:1.5;"));

        // Compteur caractères
        Label charCount = new Label("0 / 500");
        charCount.setStyle("-fx-font-size:11; -fx-text-fill:#9ca3af;");
        fDesc.textProperty().addListener((o, ov, nv) -> {
            int len = nv.length();
            charCount.setText(len + " / 500");
            charCount.setStyle("-fx-font-size:11; -fx-text-fill:" + (len > 500 ? "#e94560" : "#9ca3af") + ";");
        });
        javafx.scene.layout.HBox descFooter = new javafx.scene.layout.HBox();
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        descFooter.getChildren().addAll(spacer, charCount);
        descBox.getChildren().addAll(descLbl, fDesc, descFooter);

        // Message d'erreur
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill:#e94560; -fx-font-size:12; -fx-font-weight:600;"
            + "-fx-background-color:#fff1f2; -fx-background-radius:8; -fx-padding:8 12;");
        errLabel.setVisible(false); errLabel.setManaged(false);
        errLabel.setWrapText(true);

        body.getChildren().addAll(nomBox, descBox, errLabel);

        // ── Footer / Boutons ──────────────────────────────────────────────────
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(12);
        footer.setStyle("-fx-padding:16 28 24 28;");
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280; -fx-font-size:13;"
            + "-fx-font-weight:700; -fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle("-fx-background-color:#e5e7eb; -fx-text-fill:#374151;"
            + "-fx-font-size:13; -fx-font-weight:700; -fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"));
        btnAnnuler.setOnMouseExited(e -> btnAnnuler.setStyle("-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;"
            + "-fx-font-size:13; -fx-font-weight:700; -fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"));
        btnAnnuler.setOnAction(e -> dialog.close());

        Button btnCreer = new Button("✨  Créer la communauté");
        btnCreer.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5);"
            + "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;"
            + "-fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
            + "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.4),12,0,0,4);");
        btnCreer.setOnMouseEntered(e -> btnCreer.setStyle("-fx-background-color:linear-gradient(to right,#6d28d9,#4338ca);"
            + "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;"
            + "-fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
            + "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.55),16,0,0,6);"));
        btnCreer.setOnMouseExited(e -> btnCreer.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5);"
            + "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;"
            + "-fx-padding:11 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
            + "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.4),12,0,0,4);"));

        btnCreer.setOnAction(e -> {
            String nom  = fNom.getText().trim();
            String desc = fDesc.getText().trim();
            if (nom.length() < 3 || nom.length() > 80) {
                errLabel.setText("⚠  Le nom doit contenir entre 3 et 80 caractères.");
                errLabel.setVisible(true); errLabel.setManaged(true);
            } else if (desc.length() < 15) {
                errLabel.setText("⚠  La description doit contenir au moins 15 caractères.");
                errLabel.setVisible(true); errLabel.setManaged(true);
            } else if (desc.length() > 500) {
                errLabel.setText("⚠  La description ne peut pas dépasser 500 caractères.");
                errLabel.setVisible(true); errLabel.setManaged(true);
            } else {
                Communaute c = new Communaute(nom, desc, SessionManager.getCurrentUser().getId());
                service.ajouter(c);
                allCommunautes = service.getList();
                afficher(searchField.getText());
                dialog.close();
            }
        });

        footer.getChildren().addAll(btnAnnuler, btnCreer);
        root.getChildren().addAll(header, body, footer);

        // ── Drag to move ──────────────────────────────────────────────────────
        final double[] drag = {0, 0};
        header.setOnMousePressed(e -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        header.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - drag[0]);
            dialog.setY(e.getScreenY() - drag[1]);
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
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

    private void onModifier(Communaute c) {
        // Find the card in the cardsPane
        VBox card = cardsPane.getChildren().stream()
            .filter(n -> n instanceof VBox && n.getUserData() == c)
            .map(n -> (VBox) n)
            .findFirst().orElse(null);
        if (card == null) return;

        // Avoid double edit panels
        card.getChildren().removeIf(n -> "edit-panel".equals(n.getId()));

        // ── Inline edit panel ─────────────────────────────────────────────
        VBox editPanel = new VBox(14);
        editPanel.setId("edit-panel");
        editPanel.setStyle(
            "-fx-background-color:#faf8ff; -fx-background-radius:16;" +
            "-fx-border-color:#ddd6fe; -fx-border-radius:16; -fx-border-width:1.5;" +
            "-fx-padding:18;");

        Label editTitle = new Label("✏  Modifier la communauté");
        editTitle.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#4f46e5;");

        // Nom field
        Label lblNom = new Label("Nom");
        lblNom.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#7c3aed;");
        TextField fNom = new TextField(c.getNom());
        fNom.setStyle(
            "-fx-background-color:white; -fx-background-radius:10;" +
            "-fx-border-color:#ddd6fe; -fx-border-radius:10; -fx-border-width:1.5;" +
            "-fx-padding:10 14; -fx-font-size:13; -fx-text-fill:#1e1b4b;");

        // Description field
        Label lblDesc = new Label("Description");
        lblDesc.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#7c3aed;");
        javafx.scene.control.TextArea fDesc = new javafx.scene.control.TextArea(c.getDescription());
        fDesc.setPrefRowCount(3);
        fDesc.setWrapText(true);
        fDesc.setStyle(
            "-fx-background-color:white; -fx-background-radius:10;" +
            "-fx-border-color:#ddd6fe; -fx-border-radius:10; -fx-border-width:1.5;" +
            "-fx-padding:10 14; -fx-font-size:12; -fx-text-fill:#1e1b4b;");

        // Error label
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill:#e94560; -fx-font-size:11;");
        errLabel.setWrapText(true);

        // Buttons row
        javafx.scene.control.Button btnSave = new javafx.scene.control.Button("✔  Enregistrer");
        btnSave.setStyle(
            "-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5);" +
            "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:800;" +
            "-fx-padding:9 22; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
        btnSave.setOnMouseEntered(e -> btnSave.setOpacity(0.88));
        btnSave.setOnMouseExited(e -> btnSave.setOpacity(1.0));

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Annuler");
        btnCancel.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#9ca3af;" +
            "-fx-font-size:12; -fx-font-weight:600;" +
            "-fx-padding:9 18; -fx-background-radius:20; -fx-cursor:hand;" +
            "-fx-border-width:1; -fx-border-color:#e5e7eb; -fx-border-radius:20;");
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(
            "-fx-background-color:#f9fafb; -fx-text-fill:#6b7280;" +
            "-fx-font-size:12; -fx-font-weight:600;" +
            "-fx-padding:9 18; -fx-background-radius:20; -fx-cursor:hand;" +
            "-fx-border-width:1; -fx-border-color:#d1d5db; -fx-border-radius:20;"));
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#9ca3af;" +
            "-fx-font-size:12; -fx-font-weight:600;" +
            "-fx-padding:9 18; -fx-background-radius:20; -fx-cursor:hand;" +
            "-fx-border-width:1; -fx-border-color:#e5e7eb; -fx-border-radius:20;"));

        HBox btnRow = new HBox(10, btnSave, btnCancel);
        btnRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        editPanel.getChildren().addAll(editTitle, lblNom, fNom, lblDesc, fDesc, errLabel, btnRow);

        // Animate in
        editPanel.setOpacity(0);
        card.getChildren().add(editPanel);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), editPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // ── Actions ───────────────────────────────────────────────────────
        btnCancel.setOnAction(e -> {
            javafx.animation.FadeTransition out = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), editPanel);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ev -> card.getChildren().remove(editPanel));
            out.play();
        });

        btnSave.setOnAction(e -> {
            String nom  = fNom.getText().trim();
            String desc = fDesc.getText().trim();
            if (nom.length() < 3 || nom.length() > 80) {
                errLabel.setText("Le nom doit contenir entre 3 et 80 caractères.");
                fNom.setStyle("-fx-background-color:#fff1f2; -fx-background-radius:10;" +
                    "-fx-border-color:#fca5a5; -fx-border-radius:10; -fx-border-width:1.5;" +
                    "-fx-padding:10 14; -fx-font-size:13;");
                return;
            }
            if (desc.length() < 15) {
                errLabel.setText("La description doit contenir au moins 15 caractères.");
                fDesc.setStyle("-fx-background-color:#fff1f2; -fx-background-radius:10;" +
                    "-fx-border-color:#fca5a5; -fx-border-radius:10; -fx-border-width:1.5;" +
                    "-fx-padding:10 14; -fx-font-size:12;");
                return;
            }
            c.setNom(nom);
            c.setDescription(desc);
            service.modifier(c);
            allCommunautes = service.getList();
            afficher(searchField.getText());
        });
    }

    private void onSupprimer(Communaute c) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION,
            "Supprimer la communauté \"" + c.getNom() + "\" ?",
            javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.YES) {
                service.supprimer(c);
                allCommunautes = service.getList();
                afficher(searchField.getText());
            }
        });
    }

    private Parent buildSelf() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/communaute/index.fxml"));
            return loader.load();
        } catch (Exception e) { e.printStackTrace(); return new VBox(); }
    }
}

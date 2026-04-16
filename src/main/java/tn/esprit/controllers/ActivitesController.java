package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.services.ApiService;
import tn.esprit.services.AuditService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ActivitesController {

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statCreations;
    @FXML private Label statModifs;
    @FXML private Label statSupprs;

    // Filters
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterAction;
    @FXML private ComboBox<String> filterUser;

    // Geo bar
    @FXML private Label labelGeoInfo;
    @FXML private Label labelGeoIp;

    // List
    @FXML private VBox auditListContainer;

    private final AuditService auditService = new AuditService();
    private List<AuditService.AuditEntry> allEntries;

    @FXML
    public void initialize() {
        // Load geo info for current admin (async)
        CompletableFuture.supplyAsync(ApiService::getMyGeoInfo).thenAccept(geo -> {
            Platform.runLater(() -> {
                if (geo != null) {
                    labelGeoInfo.setText(geo.city() + ", " + geo.country() + "  |  " + geo.isp());
                    labelGeoIp.setText("IP: " + geo.ip());
                } else {
                    labelGeoInfo.setText("Localisation indisponible");
                }
            });
        });

        // Load audit data (async to avoid blocking UI)
        CompletableFuture.supplyAsync(() -> auditService.getAllAuditEntries(500))
            .thenAccept(entries -> Platform.runLater(() -> {
                allEntries = entries;
                populateFilters(entries);
                updateStats(entries);
                renderRows(entries);
            }));
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void populateFilters(List<AuditService.AuditEntry> entries) {
        // Entity types
        List<String> types = entries.stream()
            .map(AuditService.AuditEntry::entityType)
            .distinct().sorted().collect(Collectors.toList());
        types.add(0, "Tous les types");
        filterType.setItems(FXCollections.observableArrayList(types));
        filterType.setValue("Tous les types");
        filterType.setOnAction(e -> applyFilters());

        // Actions
        filterAction.setItems(FXCollections.observableArrayList(
            "Toutes les actions", "Création (INS)", "Modification (UPD)", "Suppression (DEL)"));
        filterAction.setValue("Toutes les actions");
        filterAction.setOnAction(e -> applyFilters());

        // Users
        List<String> users = entries.stream()
            .map(AuditService.AuditEntry::username)
            .distinct().sorted().collect(Collectors.toList());
        users.add(0, "Tous les utilisateurs");
        filterUser.setItems(FXCollections.observableArrayList(users));
        filterUser.setValue("Tous les utilisateurs");
        filterUser.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        if (allEntries == null) return;
        List<AuditService.AuditEntry> filtered = allEntries.stream()
            .filter(e -> {
                String type = filterType.getValue();
                if (type != null && !type.startsWith("Tous") && !type.equals(e.entityType()))
                    return false;
                String action = filterAction.getValue();
                if (action != null && !action.startsWith("Toutes")) {
                    String code = action.contains("INS") ? "INS" : action.contains("UPD") ? "UPD" : "DEL";
                    if (!code.equals(e.revType())) return false;
                }
                String user = filterUser.getValue();
                if (user != null && !user.startsWith("Tous") && !user.equals(e.username()))
                    return false;
                return true;
            })
            .collect(Collectors.toList());
        updateStats(filtered);
        renderRows(filtered);
    }

    @FXML
    private void onReset() {
        filterType.setValue("Tous les types");
        filterAction.setValue("Toutes les actions");
        filterUser.setValue("Tous les utilisateurs");
        if (allEntries != null) {
            updateStats(allEntries);
            renderRows(allEntries);
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void updateStats(List<AuditService.AuditEntry> entries) {
        statTotal.setText(String.valueOf(entries.size()));
        statCreations.setText(String.valueOf(entries.stream().filter(e -> "INS".equals(e.revType())).count()));
        statModifs.setText(String.valueOf(entries.stream().filter(e -> "UPD".equals(e.revType())).count()));
        statSupprs.setText(String.valueOf(entries.stream().filter(e -> "DEL".equals(e.revType())).count()));
    }

    // ── Render rows ───────────────────────────────────────────────────────────

    private void renderRows(List<AuditService.AuditEntry> entries) {
        auditListContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label empty = new Label("Aucune activite trouvee.");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:14; -fx-padding:40 0 0 0;");
            auditListContainer.getChildren().add(empty);
            return;
        }

        // Header row
        HBox header = buildHeaderRow();
        auditListContainer.getChildren().add(header);

        // Data rows
        for (int i = 0; i < entries.size(); i++) {
            auditListContainer.getChildren().add(buildRow(entries.get(i), i % 2 == 0));
        }
    }

    private HBox buildHeaderRow() {
        HBox row = new HBox(0);
        row.setStyle("-fx-padding:8 0 8 0; -fx-border-color:transparent transparent rgba(255,255,255,0.1) transparent; -fx-border-width:0 0 1 0;");
        row.getChildren().addAll(
            headerCell("Rev #",       60),
            headerCell("Date",        130),
            headerCell("Utilisateur", 160),
            headerCell("Entite",      110),
            headerCell("Libelle",     220),
            headerCell("Action",      110)
        );
        return row;
    }

    private Label headerCell(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11; -fx-font-weight:700; -fx-padding:0 8 0 8;");
        return l;
    }

    private HBox buildRow(AuditService.AuditEntry e, boolean even) {
        String bg = even
            ? "-fx-background-color:rgba(255,255,255,0.02);"
            : "-fx-background-color:transparent;";

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(bg + "-fx-padding:10 0 10 0; -fx-cursor:hand;" +
                     "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent;" +
                     "-fx-border-width:0 0 1 0;");

        // Rev #
        Label revLbl = new Label("#" + e.revId());
        revLbl.setPrefWidth(60);
        revLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:11; -fx-padding:0 8 0 8;");

        // Date
        Label dateLbl = new Label(e.timestamp());
        dateLbl.setPrefWidth(130);
        dateLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12; -fx-padding:0 8 0 8;");

        // Username
        Label userLbl = new Label(e.username());
        userLbl.setPrefWidth(160);
        userLbl.setStyle("-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:600; -fx-padding:0 8 0 8;");

        // Entity type with icon
        Label typeLbl = new Label(e.entityIcon() + " " + e.entityType());
        typeLbl.setPrefWidth(110);
        typeLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.75); -fx-font-size:12; -fx-padding:0 8 0 8;");

        // Label/title
        Label labelLbl = new Label(e.entityLabel());
        labelLbl.setPrefWidth(220);
        labelLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:11; -fx-padding:0 8 0 8;");

        // Action badge
        String badgeColor = switch (e.revType()) {
            case "INS" -> "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;";
            case "UPD" -> "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;";
            case "DEL" -> "-fx-background-color:rgba(239,68,68,0.25); -fx-text-fill:#f87171;";
            default    -> "-fx-background-color:rgba(255,255,255,0.1); -fx-text-fill:white;";
        };
        Label actionLbl = new Label(e.actionIcon() + " " + e.actionLabel());
        actionLbl.setStyle(badgeColor +
            "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:6;" +
            "-fx-padding:3 10 3 10;");
        HBox actionBox = new HBox(actionLbl);
        actionBox.setPrefWidth(110);
        actionBox.setPadding(new javafx.geometry.Insets(0, 8, 0, 8));
        actionBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(revLbl, dateLbl, userLbl, typeLbl, labelLbl, actionBox);

        // Hover effect
        row.setOnMouseEntered(ev -> row.setStyle(
            "-fx-background-color:rgba(122,106,216,0.1); -fx-padding:10 0 10 0; -fx-cursor:hand;" +
            "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent;" +
            "-fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(ev -> row.setStyle(
            bg + "-fx-padding:10 0 10 0; -fx-cursor:hand;" +
            "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent;" +
            "-fx-border-width:0 0 1 0;"));

        return row;
    }
}

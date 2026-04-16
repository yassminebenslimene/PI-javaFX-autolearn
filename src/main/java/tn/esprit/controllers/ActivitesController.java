package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.services.ActivityApiClient;
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
    private List<ActivityApiClient.ActivityEntry> allApiEntries;

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

        // Load from Symfony ActivityAPI (JavaFX-side actions: login, suspend, etc.)
        ActivityApiClient.fetchRecentActivities(200).thenAccept(apiEntries -> {
            // Also load Doctrine audit entries (Symfony-side content changes)
            CompletableFuture.supplyAsync(() -> auditService.getAllAuditEntries(300))
                .thenAccept(auditEntries -> Platform.runLater(() -> {
                    allApiEntries = apiEntries;
                    allEntries    = auditEntries;
                    populateFilters(apiEntries, auditEntries);
                    updateStats(apiEntries, auditEntries);
                    renderRows(apiEntries, auditEntries);
                }));
        });
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void populateFilters(List<ActivityApiClient.ActivityEntry> apiEntries,
                                  List<AuditService.AuditEntry> auditEntries) {
        // Collect all action types
        List<String> types = new java.util.ArrayList<>();
        types.add("Toutes les activites");
        types.add("--- Actions JavaFX ---");
        apiEntries.stream().map(ActivityApiClient.ActivityEntry::action)
            .distinct().sorted().forEach(types::add);
        types.add("--- Contenu Symfony ---");
        auditEntries.stream().map(AuditService.AuditEntry::entityType)
            .distinct().sorted().forEach(types::add);
        filterType.setItems(FXCollections.observableArrayList(types));
        filterType.setValue("Toutes les activites");
        filterType.setOnAction(e -> applyFilters());

        filterAction.setItems(FXCollections.observableArrayList(
            "Toutes les actions", "Connexion", "Suspension", "Creation", "Modification", "Suppression"));
        filterAction.setValue("Toutes les actions");
        filterAction.setOnAction(e -> applyFilters());

        // Users from API entries
        List<String> users = new java.util.ArrayList<>();
        users.add("Tous les utilisateurs");
        apiEntries.stream().map(ActivityApiClient.ActivityEntry::userName)
            .distinct().sorted().forEach(users::add);
        filterUser.setItems(FXCollections.observableArrayList(users));
        filterUser.setValue("Tous les utilisateurs");
        filterUser.setOnAction(e -> applyFilters());
    }

    private void applyFilters() {
        if (allApiEntries == null) return;
        applyFiltersInternal(allApiEntries, allEntries);
    }

    private void applyFiltersInternal(List<ActivityApiClient.ActivityEntry> apiEntries,
                                       List<AuditService.AuditEntry> auditEntries) {
        String typeFilter   = filterType.getValue();
        String actionFilter = filterAction.getValue();
        String userFilter   = filterUser.getValue();

        List<ActivityApiClient.ActivityEntry> filteredApi = apiEntries.stream()
            .filter(e -> {
                if (typeFilter != null && !typeFilter.startsWith("Toutes") && !typeFilter.startsWith("---")) {
                    if (!e.action().equals(typeFilter)) return false;
                }
                if (actionFilter != null && !actionFilter.startsWith("Toutes")) {
                    boolean match = switch (actionFilter) {
                        case "Connexion"    -> e.action().contains("login");
                        case "Suspension"   -> e.action().contains("suspend");
                        case "Creation"     -> e.action().contains("creat");
                        case "Modification" -> e.action().contains("updat");
                        default -> true;
                    };
                    if (!match) return false;
                }
                if (userFilter != null && !userFilter.startsWith("Tous")) {
                    if (!e.userName().equals(userFilter)) return false;
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());

        List<AuditService.AuditEntry> filteredAudit = auditEntries.stream()
            .filter(e -> {
                if (typeFilter != null && !typeFilter.startsWith("Toutes") && !typeFilter.startsWith("---")) {
                    if (!e.entityType().equals(typeFilter)) return false;
                }
                if (actionFilter != null && !actionFilter.startsWith("Toutes")) {
                    boolean match = switch (actionFilter) {
                        case "Creation"     -> "INS".equals(e.revType());
                        case "Modification" -> "UPD".equals(e.revType());
                        case "Suppression"  -> "DEL".equals(e.revType());
                        default -> true;
                    };
                    if (!match) return false;
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());

        updateStats(filteredApi, filteredAudit);
        renderRows(filteredApi, filteredAudit);
    }

    @FXML
    private void onReset() {
        filterType.setValue("Toutes les activites");
        filterAction.setValue("Toutes les actions");
        filterUser.setValue("Tous les utilisateurs");
        if (allApiEntries != null) applyFiltersInternal(allApiEntries, allEntries);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void updateStats(List<ActivityApiClient.ActivityEntry> apiEntries,
                              List<AuditService.AuditEntry> auditEntries) {
        int total = apiEntries.size() + auditEntries.size();
        statTotal.setText(String.valueOf(total));
        long creations = apiEntries.stream().filter(e -> e.action().contains("creat")).count()
                       + auditEntries.stream().filter(e -> "INS".equals(e.revType())).count();
        long modifs    = apiEntries.stream().filter(e -> e.action().contains("updat") || e.action().contains("login")).count()
                       + auditEntries.stream().filter(e -> "UPD".equals(e.revType())).count();
        long supprs    = apiEntries.stream().filter(e -> e.action().contains("suspend") || e.action().contains("delet")).count()
                       + auditEntries.stream().filter(e -> "DEL".equals(e.revType())).count();
        statCreations.setText(String.valueOf(creations));
        statModifs.setText(String.valueOf(modifs));
        statSupprs.setText(String.valueOf(supprs));
    }

    // ── Render rows ───────────────────────────────────────────────────────────

    private void renderRows(List<ActivityApiClient.ActivityEntry> apiEntries,
                             List<AuditService.AuditEntry> auditEntries) {
        auditListContainer.getChildren().clear();

        if (apiEntries.isEmpty() && auditEntries.isEmpty()) {
            Label empty = new Label("Aucune activite trouvee.");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:14; -fx-padding:40 0 0 0;");
            auditListContainer.getChildren().add(empty);
            return;
        }

        auditListContainer.getChildren().add(buildHeaderRow());

        // API entries first (most recent JavaFX actions)
        if (!apiEntries.isEmpty()) {
            Label section = new Label("  Actions depuis l'application JavaFX");
            section.setStyle("-fx-text-fill:rgba(165,180,252,0.7); -fx-font-size:11; -fx-font-weight:700;" +
                             "-fx-padding:12 0 4 0;");
            auditListContainer.getChildren().add(section);
            for (int i = 0; i < apiEntries.size(); i++) {
                auditListContainer.getChildren().add(buildApiRow(apiEntries.get(i), i % 2 == 0));
            }
        }

        // Audit entries (Symfony content changes)
        if (!auditEntries.isEmpty()) {
            Label section = new Label("  Modifications de contenu (Symfony)");
            section.setStyle("-fx-text-fill:rgba(52,211,153,0.7); -fx-font-size:11; -fx-font-weight:700;" +
                             "-fx-padding:16 0 4 0;");
            auditListContainer.getChildren().add(section);
            for (int i = 0; i < auditEntries.size(); i++) {
                auditListContainer.getChildren().add(buildRow(auditEntries.get(i), i % 2 == 0));
            }
        }
    }

    private HBox buildApiRow(ActivityApiClient.ActivityEntry e, boolean even) {
        String bg = even ? "-fx-background-color:rgba(255,255,255,0.02);" : "-fx-background-color:transparent;";
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(bg + "-fx-padding:10 0 10 0; -fx-cursor:hand;" +
                     "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent;" +
                     "-fx-border-width:0 0 1 0;");

        Label revLbl = new Label("#" + e.id());
        revLbl.setPrefWidth(60);
        revLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:11; -fx-padding:0 8 0 8;");

        Label dateLbl = new Label(e.createdAt());
        dateLbl.setPrefWidth(130);
        dateLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12; -fx-padding:0 8 0 8;");

        Label userLbl = new Label(e.userName());
        userLbl.setPrefWidth(160);
        userLbl.setStyle("-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:600; -fx-padding:0 8 0 8;");

        Label typeLbl = new Label("👤 " + e.userRole());
        typeLbl.setPrefWidth(110);
        typeLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.75); -fx-font-size:12; -fx-padding:0 8 0 8;");

        String loc = e.location() != null && !e.location().equals("—") ? e.location() : e.ipAddress();
        Label locLbl = new Label(loc != null ? loc : "—");
        locLbl.setPrefWidth(220);
        locLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.45); -fx-font-size:11; -fx-padding:0 8 0 8;");

        String badgeColor = e.action().contains("login")     ? "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;" :
                            e.action().contains("suspend")   ? "-fx-background-color:rgba(239,68,68,0.25); -fx-text-fill:#f87171;" :
                            e.action().contains("reactivat") ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                            e.action().contains("creat")     ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                                                               "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;";
        Label actionLbl = new Label(e.actionIcon() + " " + e.actionLabel());
        actionLbl.setStyle(badgeColor + "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:3 10 3 10;");
        HBox actionBox = new HBox(actionLbl);
        actionBox.setPrefWidth(110);
        actionBox.setPadding(new javafx.geometry.Insets(0, 8, 0, 8));
        actionBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(revLbl, dateLbl, userLbl, typeLbl, locLbl, actionBox);

        row.setOnMouseEntered(ev -> row.setStyle(
            "-fx-background-color:rgba(99,102,241,0.1); -fx-padding:10 0 10 0; -fx-cursor:hand;" +
            "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(ev -> row.setStyle(
            bg + "-fx-padding:10 0 10 0; -fx-cursor:hand;" +
            "-fx-border-color:transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width:0 0 1 0;"));
        return row;
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

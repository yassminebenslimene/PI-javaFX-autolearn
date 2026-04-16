package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ApiService;
import tn.esprit.session.SessionManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ActivitesController {

    // Tabs
    @FXML private Button btnTabStudents;
    @FXML private Button btnTabAdmin;

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statLogins;
    @FXML private Label statViews;
    @FXML private Label statSuspensions;

    // Filters
    @FXML private ComboBox<String> filterAction;
    @FXML private ComboBox<String> filterUser;

    // Geo
    @FXML private Label labelGeoInfo;
    @FXML private Label labelGeoIp;

    // List
    @FXML private VBox activityListContainer;

    // State
    private enum Tab { STUDENTS, ADMIN }
    private Tab currentTab = Tab.STUDENTS;

    // All data loaded from API
    private List<ActivityApiClient.ActivityEntry> allStudentEntries = List.of();
    private List<ActivityApiClient.ActivityEntry> allAdminEntries   = List.of();

    // Current admin info
    private int    currentAdminId   = 0;
    private String currentAdminName = "";

    private static final String TAB_ACTIVE =
        "-fx-background-color:rgba(122,106,216,0.2); -fx-text-fill:#a5b4fc;" +
        "-fx-font-size:13; -fx-font-weight:700; -fx-cursor:hand; -fx-border-width:0;" +
        "-fx-padding:12 20 12 20; -fx-background-radius:0;" +
        "-fx-border-color:transparent transparent #7a6ad8 transparent; -fx-border-width:0 0 2 0;";
    private static final String TAB_INACTIVE =
        "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.5);" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;" +
        "-fx-padding:12 20 12 20; -fx-background-radius:0;";

    @FXML
    public void initialize() {
        // Get current admin info
        var admin = SessionManager.getCurrentUser();
        if (admin != null) {
            currentAdminId   = admin.getId();
            currentAdminName = admin.getPrenom() + " " + admin.getNom();
        }

        // Load geo info
        CompletableFuture.supplyAsync(ApiService::getMyGeoInfo).thenAccept(geo ->
            Platform.runLater(() -> {
                if (geo != null) {
                    labelGeoInfo.setText(geo.city() + ", " + geo.country() + "  |  " + geo.isp());
                    labelGeoIp.setText("IP: " + geo.ip());
                } else {
                    labelGeoInfo.setText("Localisation indisponible");
                }
            })
        );

        // Load data
        loadData();
    }

    private void loadData() {
        showLoading();

        // Try Symfony API first, fall back to direct DB read if API unavailable
        ActivityApiClient.fetchRecentActivities(500).thenAccept(apiResult -> {
            List<ActivityApiClient.ActivityEntry> all;

            if (apiResult.isEmpty()) {
                System.out.println("[Activites] API returned 0 — falling back to direct DB");
                all = ActivityApiClient.fetchFromDbDirect(500);
            } else {
                all = apiResult;
            }

            List<ActivityApiClient.ActivityEntry> students = all.stream()
                .filter(e -> !"ADMIN".equalsIgnoreCase(e.userRole()))
                .collect(Collectors.toList());

            List<ActivityApiClient.ActivityEntry> adminHistory = all.stream()
                .filter(e -> e.userId() == currentAdminId)
                .collect(Collectors.toList());

            System.out.println("[Activites] Total=" + all.size()
                + " Students=" + students.size() + " Admin=" + adminHistory.size());

            Platform.runLater(() -> {
                allStudentEntries = students;
                allAdminEntries   = adminHistory;
                populateFilters();
                renderCurrentTab();
            });
        });
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    @FXML private void onTabStudents() {
        currentTab = Tab.STUDENTS;
        btnTabStudents.setStyle(TAB_ACTIVE);
        btnTabAdmin.setStyle(TAB_INACTIVE);
        populateFilters();
        renderCurrentTab();
    }

    @FXML private void onTabAdmin() {
        currentTab = Tab.ADMIN;
        btnTabAdmin.setStyle(TAB_ACTIVE);
        btnTabStudents.setStyle(TAB_INACTIVE);
        populateFilters();
        renderCurrentTab();
    }

    @FXML private void onRefresh() { loadData(); }

    // ── Filters ───────────────────────────────────────────────────────────────

    private void populateFilters() {
        List<ActivityApiClient.ActivityEntry> source = currentTab == Tab.STUDENTS
            ? allStudentEntries : allAdminEntries;

        // Action filter
        List<String> actions = new java.util.ArrayList<>();
        actions.add("Toutes les actions");
        source.stream().map(ActivityApiClient.ActivityEntry::action)
            .distinct().sorted()
            .map(a -> a.replace("user.", "").replace(".", " "))
            .forEach(actions::add);
        filterAction.setItems(FXCollections.observableArrayList(actions));
        if (filterAction.getValue() == null) filterAction.setValue("Toutes les actions");
        filterAction.setOnAction(e -> renderCurrentTab());

        // User filter (only for students tab)
        if (currentTab == Tab.STUDENTS) {
            List<String> users = new java.util.ArrayList<>();
            users.add("Tous les etudiants");
            source.stream().map(ActivityApiClient.ActivityEntry::userName)
                .distinct().sorted().forEach(users::add);
            filterUser.setItems(FXCollections.observableArrayList(users));
            if (filterUser.getValue() == null) filterUser.setValue("Tous les etudiants");
            filterUser.setDisable(false);
        } else {
            filterUser.setItems(FXCollections.observableArrayList("Moi (" + currentAdminName + ")"));
            filterUser.setValue("Moi (" + currentAdminName + ")");
            filterUser.setDisable(true);
        }
        filterUser.setOnAction(e -> renderCurrentTab());
    }

    @FXML private void onReset() {
        filterAction.setValue("Toutes les actions");
        filterUser.setValue(currentTab == Tab.STUDENTS ? "Tous les etudiants" : "Moi (" + currentAdminName + ")");
        renderCurrentTab();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void renderCurrentTab() {
        List<ActivityApiClient.ActivityEntry> source = currentTab == Tab.STUDENTS
            ? allStudentEntries : allAdminEntries;

        // Apply filters
        String actionFilter = filterAction.getValue();
        String userFilter   = filterUser.getValue();

        List<ActivityApiClient.ActivityEntry> filtered = source.stream()
            .filter(e -> {
                if (actionFilter != null && !actionFilter.startsWith("Toutes")) {
                    String label = e.action().replace("user.", "").replace(".", " ");
                    if (!label.equalsIgnoreCase(actionFilter)) return false;
                }
                if (currentTab == Tab.STUDENTS && userFilter != null && !userFilter.startsWith("Tous")) {
                    if (!e.userName().equals(userFilter)) return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        updateStats(filtered);
        renderRows(filtered);
    }

    private void updateStats(List<ActivityApiClient.ActivityEntry> entries) {
        statTotal.setText(String.valueOf(entries.size()));
        statLogins.setText(String.valueOf(
            entries.stream().filter(e -> e.action().contains("login")).count()));
        statViews.setText(String.valueOf(
            entries.stream().filter(e -> e.action().contains("view") || e.action().contains("cours")
                || e.action().contains("challenge") || e.action().contains("event")).count()));
        statSuspensions.setText(String.valueOf(
            entries.stream().filter(e -> e.action().contains("suspend")).count()));
    }

    private void renderRows(List<ActivityApiClient.ActivityEntry> entries) {
        activityListContainer.getChildren().clear();

        if (entries.isEmpty()) {
            Label empty = new Label(currentTab == Tab.STUDENTS
                ? "Aucune activite etudiant enregistree."
                : "Aucune activite dans votre historique.");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:14; -fx-padding:40 0 0 0;");
            activityListContainer.getChildren().add(empty);
            return;
        }

        // Header
        activityListContainer.getChildren().add(buildHeader());

        // Rows
        for (int i = 0; i < entries.size(); i++) {
            activityListContainer.getChildren().add(buildRow(entries.get(i), i % 2 == 0));
        }
    }

    private HBox buildHeader() {
        HBox h = new HBox(0);
        h.setStyle("-fx-padding:8 0 8 0; -fx-border-color:transparent transparent rgba(255,255,255,0.1) transparent; -fx-border-width:0 0 1 0;");
        h.getChildren().addAll(
            hCell("#",            50),
            hCell("Date",         140),
            hCell("Utilisateur",  170),
            hCell("Role",          90),
            hCell("Localisation", 200),
            hCell("Action",       140)
        );
        return h;
    }

    private Label hCell(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:11; -fx-font-weight:700; -fx-padding:0 8 0 8;");
        return l;
    }

    private HBox buildRow(ActivityApiClient.ActivityEntry e, boolean even) {
        String bg = even ? "-fx-background-color:rgba(255,255,255,0.025);" : "-fx-background-color:transparent;";
        String border = "-fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;";

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(bg + "-fx-padding:11 0 11 0; -fx-cursor:hand;" + border);

        // ID
        Label idLbl = new Label("#" + e.id());
        idLbl.setPrefWidth(50);
        idLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.25); -fx-font-size:10; -fx-padding:0 8 0 8;");

        // Date
        Label dateLbl = new Label(e.createdAt());
        dateLbl.setPrefWidth(140);
        dateLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-padding:0 8 0 8;");

        // User
        Label userLbl = new Label(e.userName());
        userLbl.setPrefWidth(170);
        userLbl.setStyle("-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:600; -fx-padding:0 8 0 8;");

        // Role badge
        String roleColor = "ADMIN".equalsIgnoreCase(e.userRole())
            ? "-fx-background-color:rgba(239,68,68,0.2); -fx-text-fill:#f87171;"
            : "-fx-background-color:rgba(99,102,241,0.2); -fx-text-fill:#a5b4fc;";
        Label roleLbl = new Label(e.userRole() != null ? e.userRole() : "—");
        roleLbl.setStyle(roleColor + "-fx-font-size:10; -fx-font-weight:700; -fx-background-radius:5; -fx-padding:2 7 2 7;");
        HBox roleBox = new HBox(roleLbl);
        roleBox.setPrefWidth(90);
        roleBox.setPadding(new Insets(0, 8, 0, 8));
        roleBox.setAlignment(Pos.CENTER_LEFT);

        // Location
        String loc = (e.location() != null && !e.location().equals("—") && !e.location().isBlank())
            ? e.location() : (e.ipAddress() != null ? e.ipAddress() : "—");
        Label locLbl = new Label("📍 " + loc);
        locLbl.setPrefWidth(200);
        locLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11; -fx-padding:0 8 0 8;");

        // Action badge
        String action = e.action();
        String badgeStyle = action.contains("login")      ? "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;" :
                            action.contains("logout")     ? "-fx-background-color:rgba(255,255,255,0.1); -fx-text-fill:rgba(245,245,244,0.6);" :
                            action.contains("suspend")    ? "-fx-background-color:rgba(239,68,68,0.25); -fx-text-fill:#f87171;" :
                            action.contains("reactivat")  ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                            action.contains("creat")      ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                            action.contains("view")       ? "-fx-background-color:rgba(251,191,36,0.2); -fx-text-fill:#fbbf24;" :
                            action.contains("cours")      ? "-fx-background-color:rgba(251,191,36,0.2); -fx-text-fill:#fbbf24;" :
                            action.contains("challenge")  ? "-fx-background-color:rgba(251,191,36,0.2); -fx-text-fill:#fbbf24;" :
                                                            "-fx-background-color:rgba(255,255,255,0.1); -fx-text-fill:rgba(245,245,244,0.6);";
        Label actionLbl = new Label(e.actionIcon() + "  " + e.actionLabel());
        actionLbl.setStyle(badgeStyle + "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:3 10 3 10;");
        HBox actionBox = new HBox(actionLbl);
        actionBox.setPrefWidth(140);
        actionBox.setPadding(new Insets(0, 8, 0, 8));
        actionBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(idLbl, dateLbl, userLbl, roleBox, locLbl, actionBox);

        // Hover
        String hoverBg = currentTab == Tab.STUDENTS
            ? "-fx-background-color:rgba(99,102,241,0.08);"
            : "-fx-background-color:rgba(122,106,216,0.08);";
        row.setOnMouseEntered(ev -> row.setStyle(hoverBg + "-fx-padding:11 0 11 0; -fx-cursor:hand;" + border));
        row.setOnMouseExited(ev  -> row.setStyle(bg + "-fx-padding:11 0 11 0; -fx-cursor:hand;" + border));

        return row;
    }

    private void showLoading() {
        activityListContainer.getChildren().clear();
        Label loading = new Label("Chargement des activites...");
        loading.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:13; -fx-padding:40 0 0 0;");
        activityListContainer.getChildren().add(loading);
    }
}

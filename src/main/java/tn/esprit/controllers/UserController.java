package tn.esprit.controllers;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Admin;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserAiInsightService;
import tn.esprit.services.UserService;
import tn.esprit.session.JwtManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class UserController {

    // ── Index ─────────────────────────────────────────────────────────────────
    @FXML private TableView<User>            tableUsers;
    @FXML private TableColumn<User, User>    colUser;
    @FXML private TableColumn<User, String>  colNiveau;
    @FXML private TableColumn<User, String>  colStatut;
    @FXML private TableColumn<User, String>  colCreated;
    @FXML private TableColumn<User, Void>    colRisk;
    @FXML private TableColumn<User, Void>    colActions;
    @FXML private TextField                  searchField;
    @FXML private Label                      labelTotalUsers;
    @FXML private Label                      labelTotalAdmins;
    @FXML private Label                      labelTotalEtudiants;
    @FXML private Label                      labelActiveWeek;
    @FXML private Label                      labelNewWeek;
    @FXML private Label                      labelSuspended;
    @FXML private HBox                       adminToolbarNew;

    // ── Feature 1: Heatmap ────────────────────────────────────────────────────
    @FXML private Canvas heatmapCanvas;
    @FXML private HBox   heatmapLabels;

    // ── Feature 2: AI Risk ────────────────────────────────────────────────────
    @FXML private VBox   riskSummaryBox;
    @FXML private Button btnViewAllRisk;

    // ── Form ──────────────────────────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private Label         formSubtitle;
    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private Label         labelPasswordHint;
    @FXML private ComboBox<String> comboRole;
    @FXML private Label         labelNiveau;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private Label         errorNom;
    @FXML private Label         errorPrenom;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;
    @FXML private Label         errorRole;
    @FXML private Label         errorNiveau;

    private final UserService service    = new UserService();
    private User              editingUser = null;
    private boolean           isEditMode  = false;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    // Risk scores cache: userId -> RiskResult
    private final Map<Integer, UserAiInsightService.RiskResult> riskCache = new HashMap<>();
    
    // Shared risk data for risk analysis page
    private static List<UserAiInsightService.RiskResult> sharedRiskData = null;
    
    public static void setSharedRiskData(List<UserAiInsightService.RiskResult> data) {
        sharedRiskData = data;
    }
    
    public static List<UserAiInsightService.RiskResult> getSharedRiskData() {
        return sharedRiskData;
    }

    @FXML
    public void initialize() {
        if (tableUsers != null) initTable();
        if (comboRole  != null) initForm();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TABLE
    // ─────────────────────────────────────────────────────────────────────────
    private void initTable() {
        tableUsers.setStyle(
            "-fx-background-color:#0a0f0d; -fx-border-width:0;" +
            "-fx-table-cell-border-color:rgba(255,255,255,0.06);"
        );
        tableUsers.skinProperty().addListener((obs, o, skin) ->
            javafx.application.Platform.runLater(this::applyTableDarkTheme));
        tableUsers.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(this::applyTableDarkTheme);
        });

        tableUsers.setRowFactory(tv -> {
            javafx.scene.control.TableRow<User> row = new javafx.scene.control.TableRow<>();
            row.setStyle("-fx-background-color:#0a0f0d;");
            row.selectedProperty().addListener((obs, was, is) ->
                row.setStyle(is ? "-fx-background-color:rgba(5,150,105,0.18);" : "-fx-background-color:#0a0f0d;"));
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isSelected())
                    row.setStyle(is ? "-fx-background-color:rgba(255,255,255,0.04);" : "-fx-background-color:#0a0f0d;");
            });
            return row;
        });

        // ── User column ──
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;");
                if (empty || user == null) { setGraphic(null); return; }
                String initials = user.getPrenom().substring(0,1).toUpperCase() + user.getNom().substring(0,1).toUpperCase();
                Label avatar = new Label(initials);
                avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
                                "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;" +
                                "-fx-background-radius:50%; -fx-min-width:38; -fx-min-height:38;" +
                                "-fx-max-width:38; -fx-max-height:38; -fx-alignment:CENTER;");
                Label name  = new Label(user.getPrenom() + " " + user.getNom());
                name.setStyle("-fx-text-fill:white; -fx-font-weight:600; -fx-font-size:13;");
                Label email = new Label(user.getEmail());
                email.setStyle("-fx-text-fill:rgba(245,245,244,0.45); -fx-font-size:11;");
                VBox info = new VBox(2, name, email);
                HBox cell = new HBox(10, avatar, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setStyle("-fx-padding:3 0 3 4;");
                setGraphic(cell);
            }
        });

        // ── Level badge ──
        colNiveau.setCellValueFactory(data -> {
            User u = data.getValue();
            return new SimpleStringProperty((u instanceof Etudiant e && e.getNiveau() != null) ? e.getNiveau() : "—");
        });
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null || "—".equals(item)) { setText("—"); setGraphic(null); return; }
                Label badge = new Label(item);
                String base = "-fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20; -fx-text-fill:white;";
                switch (item) {
                    case "DEBUTANT"      -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#34d399,#059669);");
                    case "INTERMEDIAIRE" -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#fbbf24,#f59e0b); -fx-text-fill:#1a1a1a;");
                    case "AVANCE"        -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#f87171,#dc2626);");
                    default              -> badge.setStyle(base + "-fx-background-color:rgba(255,255,255,0.15);");
                }
                HBox wrap = new HBox(badge); wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // ── Status badge ──
        colStatut.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isIsSuspended() ? "Suspendu" : "Actif"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.setStyle("Suspendu".equals(item)
                    ? "-fx-background-color:linear-gradient(to right,#dc2626,#b91c1c); -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20;"
                    : "-fx-background-color:linear-gradient(to right,#34d399,#059669); -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20;");
                HBox wrap = new HBox(badge); wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // ── Joined date ──
        colCreated.setCellValueFactory(data -> {
            User u = data.getValue();
            return new SimpleStringProperty(u.getCreatedAt() != null ? SDF.format(u.getCreatedAt()) : "—");
        });
        colCreated.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12;");
                setText(empty || item == null ? null : item);
            }
        });

        // ── AI Risk column ──
        if (colRisk != null) {
            colRisk.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                    if (empty) { setGraphic(null); return; }
                    User u = getTableView().getItems().get(getIndex());
                    if (u == null) { setGraphic(null); return; }
                    UserAiInsightService.RiskResult risk = riskCache.get(u.getId());
                    if (risk == null) {
                        Label loading = new Label("...");
                        loading.setStyle("-fx-text-fill:rgba(245,245,244,0.3); -fx-font-size:11;");
                        setGraphic(loading); return;
                    }
                    // Animated ring gauge
                    Canvas ring = buildRiskRing(risk.riskScore(), risk.riskLevel());
                    setGraphic(ring);
                }
            });
        }

        // ── Actions ──
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView     = new Button("View");
            private final Button btnEdit     = new Button("Edit");
            private final Button btnSuspend  = new Button("Suspend");
            private final Button btnActivity = new Button("📊");
            private final HBox   box         = new HBox(5, btnView, btnEdit, btnSuspend, btnActivity);
            {
                String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                btnView.setStyle(base + "-fx-background-color:rgba(14,165,233,0.25); -fx-text-fill:#38bdf8;");
                btnEdit.setStyle(base + "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;");
                btnSuspend.setStyle(base + "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;");
                btnActivity.setStyle(base + "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;");
                box.setAlignment(Pos.CENTER);
                btnView.setOnAction(e -> openDetailWindow(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> { onEditUser(getTableView().getItems().get(getIndex())); loadTable(); });
                btnSuspend.setOnAction(e -> { onSuspendUser(getTableView().getItems().get(getIndex())); loadTable(); });
                btnActivity.setOnAction(e -> openActivityWindow(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0a0f0d; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;");
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                if (u != null) {
                    String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                    if (u.isIsSuspended()) {
                        btnSuspend.setStyle(base + "-fx-background-color:rgba(52,211,153,0.25); -fx-text-fill:#34d399;");
                        btnSuspend.setText("Réactiver");
                    } else {
                        btnSuspend.setStyle(base + "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;");
                        btnSuspend.setText("Suspend");
                    }
                }
                setGraphic(box);
            }
        });

        if (adminToolbarNew != null) {
            adminToolbarNew.setVisible(JwtManager.isAdmin());
            adminToolbarNew.setManaged(JwtManager.isAdmin());
        }

        try { loadTable(); } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
        }
    }

    private void applyTableDarkTheme() {
        // Header row
        javafx.scene.Node header = tableUsers.lookup("TableHeaderRow");
        if (header != null)
            header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableUsers.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableUsers.lookupAll(".column-header .label").forEach(n ->
            ((javafx.scene.control.Label) n).setStyle(
                "-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableUsers.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710;"));
        // Scrollbars
        tableUsers.lookupAll(".scroll-bar").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
        tableUsers.lookupAll(".scroll-bar .track").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"));
        tableUsers.lookupAll(".scroll-bar .thumb").forEach(n ->
            n.setStyle("-fx-background-color:rgba(52,211,153,0.22); -fx-background-radius:4;"));
        tableUsers.lookupAll(".increment-button, .decrement-button").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-pref-height:0; -fx-pref-width:0;"));
        tableUsers.lookupAll(".corner").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
    }

    private void loadTable() {
        List<User> all = service.afficher();
        List<User> displayed = all.stream().filter(u -> u instanceof Etudiant).toList();
        tableUsers.setItems(FXCollections.observableArrayList(displayed));

        // ── Feature 1: Animated stat counters ──
        UserAiInsightService.DashboardStats stats = UserAiInsightService.getDashboardStats();
        animateCount(labelTotalUsers,    0, stats.totalUsers());
        animateCount(labelTotalEtudiants,0, stats.totalEtudiants());
        animateCount(labelTotalAdmins,   0, stats.totalAdmins());
        if (labelActiveWeek != null) labelActiveWeek.setText(stats.activeThisWeek() + " actifs cette semaine");
        if (labelNewWeek    != null) labelNewWeek.setText(stats.newThisWeek() + " nouveaux cette semaine");
        if (labelSuspended  != null) labelSuspended.setText(stats.suspended() + " suspendus");

        // ── Feature 3: Heatmap ──
        if (heatmapCanvas != null) {
            javafx.concurrent.Task<UserAiInsightService.HeatmapData> heatTask = new javafx.concurrent.Task<>() {
                @Override protected UserAiInsightService.HeatmapData call() {
                    return UserAiInsightService.getLoginHeatmap();
                }
            };
            heatTask.setOnSucceeded(e -> drawHeatmap(heatTask.getValue()));
            new Thread(heatTask, "heatmap-loader").start();
        }

        // ── Feature 2: AI Risk (async — doesn't block UI) ──
        if (riskSummaryBox != null) {
            UserAiInsightService.computeRiskScores(displayed).thenAccept(results -> {
                javafx.application.Platform.runLater(() -> {
                    riskCache.clear();
                    results.forEach(r -> riskCache.put(r.userId(), r));
                    tableUsers.refresh();
                    updateRiskSummary(results);
                });
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 1: Animated counter
    // ─────────────────────────────────────────────────────────────────────────
    private void animateCount(Label label, int from, int to) {
        if (label == null) return;
        Timeline tl = new Timeline();
        int steps = 30;
        for (int i = 0; i <= steps; i++) {
            final int val = from + (int) Math.round((to - from) * (double) i / steps);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 30L), e -> label.setText(String.valueOf(val))));
        }
        tl.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 3: Heatmap drawing
    // ─────────────────────────────────────────────────────────────────────────
    private void drawHeatmap(UserAiInsightService.HeatmapData data) {
        if (heatmapCanvas == null) return;
        double w = heatmapCanvas.getWidth();
        double h = heatmapCanvas.getHeight();
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int[] hourly = data.hourly();
        int   maxVal = data.maxValue();
        double barW  = (w - 10) / 24.0;
        double padX  = 5;

        // Animate bars growing from 0
        final double[] animProgress = {0.0};
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                animProgress[0] = Math.min(1.0, animProgress[0] + 0.04);
                gc.clearRect(0, 0, w, h);

                for (int hour = 0; hour < 24; hour++) {
                    double ratio    = maxVal > 0 ? (double) hourly[hour] / maxVal : 0;
                    double barH     = (h - 16) * ratio * animProgress[0];
                    double x        = padX + hour * barW;
                    double y        = h - 14 - barH;

                    // Color: green for peak hours, blue for off-peak
                    double hue = 160 - ratio * 100; // 160=green, 60=yellow, 0=red
                    Color barColor = Color.hsb(hue, 0.8, 0.9, 0.85);

                    // Gradient fill
                    LinearGradient grad = new LinearGradient(0, y, 0, h - 14,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, barColor),
                        new Stop(1, barColor.deriveColor(0, 1, 0.5, 0.4)));
                    gc.setFill(grad);

                    double rounding = Math.min(4, barW * 0.4);
                    gc.fillRoundRect(x + 1, y, barW - 2, barH, rounding, rounding);

                    // Glow on peak bar
                    if (hourly[hour] == maxVal && animProgress[0] > 0.9) {
                        gc.setFill(Color.rgb(52, 211, 153, 0.15));
                        gc.fillRoundRect(x, h - 14 - (h - 16) * ratio - 4, barW, (h - 16) * ratio + 4, rounding, rounding);
                    }

                    // Hour label every 3 hours
                    if (hour % 3 == 0) {
                        gc.setFill(Color.rgb(245, 245, 244, 0.35));
                        gc.setFont(javafx.scene.text.Font.font(9));
                        gc.fillText(String.format("%02d", hour), x + barW / 2 - 6, h - 2);
                    }
                }

                if (animProgress[0] >= 1.0) stop();
            }
        };
        timer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 2: AI Risk ring gauge + summary panel
    // ─────────────────────────────────────────────────────────────────────────
    private Canvas buildRiskRing(int score, String level) {
        Canvas c = new Canvas(52, 52);
        GraphicsContext gc = c.getGraphicsContext2D();

        Color trackColor = Color.rgb(255, 255, 255, 0.08);
        Color arcColor = switch (level) {
            case "CRITIQUE" -> Color.rgb(239, 68, 68, 0.9);
            case "ELEVE"    -> Color.rgb(251, 146, 60, 0.9);
            case "MOYEN"    -> Color.rgb(251, 191, 36, 0.9);
            default         -> Color.rgb(52, 211, 153, 0.9);
        };

        // Animate arc from 0 to score
        final double[] progress = {0.0};
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                progress[0] = Math.min(score, progress[0] + 2.5);
                gc.clearRect(0, 0, 52, 52);

                // Track
                gc.setStroke(trackColor);
                gc.setLineWidth(5);
                gc.strokeArc(6, 6, 40, 40, 0, 360, javafx.scene.shape.ArcType.OPEN);

                // Arc
                gc.setStroke(arcColor);
                gc.setLineWidth(5);
                double angle = (progress[0] / 100.0) * 360;
                gc.strokeArc(6, 6, 40, 40, 90, -angle, javafx.scene.shape.ArcType.OPEN);

                // Score text
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("System Bold", 11));
                String txt = (int) progress[0] + "%";
                gc.fillText(txt, 26 - txt.length() * 3.2, 30);

                if (progress[0] >= score) stop();
            }
        };
        timer.start();
        return c;
    }

    private void updateRiskSummary(List<UserAiInsightService.RiskResult> results) {
        if (riskSummaryBox == null) return;
        riskSummaryBox.getChildren().clear();

        long critique = results.stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long eleve    = results.stream().filter(r -> "ELEVE".equals(r.riskLevel())).count();
        long moyen    = results.stream().filter(r -> "MOYEN".equals(r.riskLevel())).count();
        long faible   = results.stream().filter(r -> "FAIBLE".equals(r.riskLevel())).count();

        riskSummaryBox.getChildren().addAll(
            riskChip("🔴 Critique",    critique, "#ef4444", "rgba(239,68,68,0.12)"),
            riskChip("🟠 Élevé",       eleve,    "#fb923c", "rgba(251,146,60,0.12)"),
            riskChip("🟡 Moyen",       moyen,    "#fbbf24", "rgba(251,191,36,0.12)"),
            riskChip("🟢 Faible",      faible,   "#34d399", "rgba(52,211,153,0.12)")
        );

        // Show top 2 at-risk students with AI explanation
        results.stream().filter(r -> r.riskScore() >= 50).limit(2).forEach(r -> {
            VBox card = new VBox(3);
            card.setStyle("-fx-background-color:rgba(239,68,68,0.07); -fx-background-radius:8;" +
                          "-fx-border-color:rgba(239,68,68,0.2); -fx-border-radius:8; -fx-padding:8 10 8 10;");
            Label name = new Label("⚠ " + r.userName());
            name.setStyle("-fx-text-fill:#f87171; -fx-font-size:11; -fx-font-weight:700;");
            Label expl = new Label(r.aiExplanation());
            expl.setStyle("-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:10;");
            expl.setWrapText(true);
            card.getChildren().addAll(name, expl);

            FadeTransition ft = new FadeTransition(Duration.millis(600), card);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            riskSummaryBox.getChildren().add(card);
        });
    }

    private HBox riskChip(String label, long count, String color, String bg) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:6; -fx-padding:5 10 5 10;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:" + color + "; -fx-font-size:11; -fx-font-weight:600;");
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13; -fx-font-weight:900;");
        box.getChildren().addAll(lbl, sp, cnt);
        return box;
    }

    @FXML private void onViewAllRisk() {
        if (riskCache.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "IA en cours", "L'analyse IA est encore en cours. Réessayez dans quelques secondes.");
            return;
        }
        openFullRiskWindow();
    }

    private void openFullRiskWindow() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("🤖 Analyse IA — Risque d'abandon");
        stage.setResizable(true);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#0a0f0d;");

        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:#0d1a14; -fx-padding:18 24 18 24;" +
                        "-fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🤖  Analyse IA — Risque d'abandon étudiant");
        title.setStyle("-fx-text-fill:white; -fx-font-size:16; -fx-font-weight:700;");
        Label sub = new Label("Powered by Groq AI (Llama 4 Scout)");
        sub.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11;");
        VBox titleBox = new VBox(3, title, sub);
        header.getChildren().add(titleBox);
        root.getChildren().add(header);

        // Table of all students
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
        VBox list = new VBox(6);
        list.setStyle("-fx-padding:16 24 24 24; -fx-background-color:#0a0f0d;");

        List<UserAiInsightService.RiskResult> sorted = riskCache.values().stream()
            .sorted((a, b) -> b.riskScore() - a.riskScore())
            .toList();

        for (UserAiInsightService.RiskResult r : sorted) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:rgba(255,255,255,0.03); -fx-background-radius:10;" +
                         "-fx-border-color:rgba(255,255,255,0.07); -fx-border-radius:10; -fx-padding:10 14 10 14;");

            // Ring
            Canvas ring = buildRiskRing(r.riskScore(), r.riskLevel());

            // Info
            Label name = new Label(r.userName());
            name.setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;");
            Label email = new Label(r.userEmail());
            email.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11;");
            Label expl = new Label(r.aiExplanation());
            expl.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:11;");
            expl.setWrapText(true);
            VBox info = new VBox(2, name, email, expl);
            javafx.scene.layout.HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            // Level badge
            String levelColor = switch (r.riskLevel()) {
                case "CRITIQUE" -> "#ef4444"; case "ELEVE" -> "#fb923c";
                case "MOYEN"    -> "#fbbf24"; default       -> "#34d399";
            };
            Label lvl = new Label(r.riskLevel());
            lvl.setStyle("-fx-text-fill:" + levelColor + "; -fx-font-size:11; -fx-font-weight:700;" +
                         "-fx-background-color:" + levelColor.replace("#", "rgba(") + ",0.15); " +
                         "-fx-background-radius:6; -fx-padding:4 10 4 10;");

            // Days info
            Label days = new Label(r.daysSinceLogin() + "j inactif");
            days.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:10;");

            row.getChildren().addAll(ring, info, days, lvl);

            FadeTransition ft = new FadeTransition(Duration.millis(400), row);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(sorted.indexOf(r) * 50L));
            ft.play();

            list.getChildren().add(row);
        }

        scroll.setContent(list);
        javafx.scene.layout.VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(scroll);

        stage.setScene(new Scene(root, 720, 560));
        stage.show();
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase().trim();
        List<User> base = service.afficher().stream().filter(u -> u instanceof Etudiant).toList();
        tableUsers.setItems(FXCollections.observableArrayList(
            base.stream().filter(u ->
                u.getNom().toLowerCase().contains(q) ||
                u.getPrenom().toLowerCase().contains(q) ||
                u.getEmail().toLowerCase().contains(q) ||
                u.getRole().toLowerCase().contains(q)
            ).toList()
        ));
    }

    @FXML private void onClearSearch() { searchField.clear(); loadTable(); }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD actions
    // ─────────────────────────────────────────────────────────────────────────
    @FXML private void onNewUser() {
        if (!JwtManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        openFormWindow(null);
    }

    private void onEditUser(User sel) {
        if (!JwtManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        openFormWindow(sel);
    }

    private void onSuspendUser(User sel) {
        if (!JwtManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        if (!(sel instanceof tn.esprit.entities.Etudiant)) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Seul un étudiant peut être suspendu."); return;
        }
        openSuspendWindow(sel);
    }

    private void openDetailWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/show.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails — " + user.getPrenom() + " " + user.getNom());
            stage.setResizable(false);
            double maxH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.88;
            stage.setScene(new Scene(loader.load(), 500, Math.min(580, maxH)));
            ShowUserController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openActivityWindow(User user) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📊 Activités — " + user.getPrenom() + " " + user.getNom());
        stage.setResizable(true);

        // ── Header ──
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setStyle("-fx-background-color:#0d1a14; -fx-padding:18 24 18 24; " +
                        "-fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        String initials = user.getPrenom().substring(0,1).toUpperCase() + user.getNom().substring(0,1).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
                        "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:15;" +
                        "-fx-background-radius:50%; -fx-min-width:44; -fx-min-height:44;" +
                        "-fx-max-width:44; -fx-max-height:44; -fx-alignment:CENTER;");
        Label nameLabel = new Label(user.getPrenom() + " " + user.getNom());
        nameLabel.setStyle("-fx-text-fill:white; -fx-font-size:16; -fx-font-weight:700;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:12;");
        javafx.scene.layout.VBox userInfo = new javafx.scene.layout.VBox(3, nameLabel, emailLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Export CSV button
        Button btnExport = new Button("💾 Exporter CSV");
        btnExport.setStyle("-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" +
                           "-fx-font-size:12; -fx-font-weight:600; -fx-padding:8 16 8 16;" +
                           "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");

        header.getChildren().addAll(avatar, userInfo, spacer, btnExport);

        // ── Stats bar ──
        javafx.scene.layout.HBox statsBar = new javafx.scene.layout.HBox(12);
        statsBar.setStyle("-fx-background-color:#0d1a14; -fx-padding:12 24 12 24;");
        statsBar.setAlignment(Pos.CENTER_LEFT);
        Label statTotalLbl    = statChip("---", "Total",        "#34d399", "rgba(5,150,105,0.15)");
        Label statLoginLbl    = statChip("---", "Connexions",   "#a5b4fc", "rgba(99,102,241,0.15)");
        Label statViewLbl     = statChip("---", "Consultations","#38bdf8", "rgba(14,165,233,0.15)");
        Label statActionLbl   = statChip("---", "Actions",      "#fbbf24", "rgba(251,191,36,0.15)");
        statsBar.getChildren().addAll(statTotalLbl, statLoginLbl, statViewLbl, statActionLbl);

        // ── Activity list ──
        VBox listContainer = new VBox(0);
        listContainer.setStyle("-fx-background-color:#0a0f0d; -fx-padding:0 24 20 24;");

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");

        // ── Root layout ──
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0, header, statsBar, scroll);
        root.setStyle("-fx-background-color:#0a0f0d;");
        javafx.scene.layout.VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        stage.setScene(new Scene(root, 860, 600));

        // ── Load activities async ──
        Label loading = new Label("Chargement des activités...");
        loading.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:13; -fx-padding:40 0 0 20;");
        listContainer.getChildren().add(loading);

        ActivityApiClient.fetchUserActivities(user.getId()).thenAccept(entries -> {
            javafx.application.Platform.runLater(() -> {
                listContainer.getChildren().clear();

                // Update stats
                long logins  = entries.stream().filter(e -> e.action().contains("login")).count();
                long views   = entries.stream().filter(e -> e.action().contains("view")).count();
                long actions = entries.stream().filter(e -> e.action().startsWith("user.") && !e.action().contains("view") && !e.action().contains("login")).count();
                updateStatChip(statTotalLbl,  String.valueOf(entries.size()));
                updateStatChip(statLoginLbl,  String.valueOf(logins));
                updateStatChip(statViewLbl,   String.valueOf(views));
                updateStatChip(statActionLbl, String.valueOf(actions));

                // Export CSV handler
                btnExport.setOnAction(ev -> exportUserActivityCsv(user, entries));

                if (entries.isEmpty()) {
                    Label empty = new Label("Aucune activité enregistrée pour cet utilisateur.");
                    empty.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:13; -fx-padding:40 0 0 0;");
                    listContainer.getChildren().add(empty);
                    return;
                }

                // Header row
                HBox hdr = new HBox(0);
                hdr.setStyle("-fx-padding:8 0 8 0; -fx-border-color:transparent transparent rgba(255,255,255,0.1) transparent; -fx-border-width:0 0 1 0;");
                hdr.getChildren().addAll(
                    actHCell("#",            50),
                    actHCell("Date",         160),
                    actHCell("IP / Lieu",    200),
                    actHCell("Action",       220),
                    actHCell("Succès",        80)
                );
                listContainer.getChildren().add(hdr);

                // Data rows
                for (int i = 0; i < entries.size(); i++) {
                    ActivityApiClient.ActivityEntry e = entries.get(i);
                    boolean even = i % 2 == 0;
                    String bg     = even ? "-fx-background-color:rgba(255,255,255,0.025);" : "-fx-background-color:transparent;";
                    String border = "-fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;";

                    HBox row = new HBox(0);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle(bg + "-fx-padding:10 0 10 0;" + border);

                    Label idL = new Label("#" + e.id());
                    idL.setPrefWidth(50); idL.setMinWidth(50);
                    idL.setStyle("-fx-text-fill:rgba(245,245,244,0.25); -fx-font-size:10; -fx-padding:0 8 0 8;");

                    Label dateL = new Label(e.createdAt() != null ? e.createdAt() : "—");
                    dateL.setPrefWidth(160); dateL.setMinWidth(160);
                    dateL.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12; -fx-padding:0 8 0 8;");

                    String loc = (e.location() != null && !e.location().isBlank() && !e.location().equals("—"))
                        ? "📍 " + e.location()
                        : (e.ipAddress() != null ? "🔌 " + e.ipAddress() : "—");
                    Label locL = new Label(loc);
                    locL.setPrefWidth(200); locL.setMinWidth(200);
                    locL.setWrapText(true);
                    locL.setStyle("-fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:11; -fx-padding:0 8 0 8;");

                    String act = e.action();
                    String badgeStyle =
                        act.contains("login")     ? "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;" :
                        act.contains("logout")    ? "-fx-background-color:rgba(255,255,255,0.1); -fx-text-fill:rgba(245,245,244,0.6);" :
                        act.contains("suspend")   ? "-fx-background-color:rgba(239,68,68,0.25); -fx-text-fill:#f87171;" :
                        act.contains("reactivat") ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                        act.contains("creat")     ? "-fx-background-color:rgba(5,150,105,0.25); -fx-text-fill:#34d399;" :
                        act.contains("delet")     ? "-fx-background-color:rgba(239,68,68,0.2); -fx-text-fill:#f87171;" :
                        act.contains("updat")     ? "-fx-background-color:rgba(251,191,36,0.2); -fx-text-fill:#fbbf24;" :
                        act.contains("view")      ? "-fx-background-color:rgba(14,165,233,0.2); -fx-text-fill:#38bdf8;" :
                                                    "-fx-background-color:rgba(255,255,255,0.1); -fx-text-fill:rgba(245,245,244,0.6);";
                    Label actL = new Label(e.actionIcon() + "  " + e.actionLabel());
                    actL.setStyle(badgeStyle + "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:4 12 4 12;");
                    HBox actBox = new HBox(actL);
                    actBox.setPrefWidth(220); actBox.setMinWidth(220);
                    actBox.setPadding(new Insets(0, 8, 0, 8));
                    actBox.setAlignment(Pos.CENTER_LEFT);

                    Label succL = new Label(e.success() ? "✅" : "❌");
                    succL.setPrefWidth(80); succL.setMinWidth(80);
                    succL.setStyle("-fx-font-size:13; -fx-padding:0 8 0 8; -fx-alignment:CENTER;");

                    row.getChildren().addAll(idL, dateL, locL, actBox, succL);

                    String hoverBg = "-fx-background-color:rgba(122,106,216,0.08);";
                    row.setOnMouseEntered(ev -> row.setStyle(hoverBg + "-fx-padding:10 0 10 0;" + border));
                    row.setOnMouseExited(ev  -> row.setStyle(bg + "-fx-padding:10 0 10 0;" + border));

                    listContainer.getChildren().add(row);
                }
            });
        });

        stage.show();
    }

    /** Creates a stat chip label for the activity modal */
    private Label statChip(String value, String label, String color, String bg) {
        Label l = new Label(value + "  " + label);
        l.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + color + ";" +
                   "-fx-font-size:12; -fx-font-weight:700; -fx-background-radius:8;" +
                   "-fx-padding:6 14 6 14; -fx-border-color:" + color.replace(")", ",0.3)").replace("rgb", "rgba") + ";" +
                   "-fx-border-radius:8; -fx-border-width:1;");
        return l;
    }

    /** Updates the value part of a stat chip */
    private void updateStatChip(Label chip, String newValue) {
        String text = chip.getText();
        int idx = text.indexOf("  ");
        if (idx >= 0) chip.setText(newValue + text.substring(idx));
    }

    /** Exports user activity to a CSV file */
    private void exportUserActivityCsv(User user, List<ActivityApiClient.ActivityEntry> entries) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exporter les activités");
        fc.setInitialFileName("activites_" + user.getPrenom() + "_" + user.getNom() + ".csv");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File file = fc.showSaveDialog(null);
        if (file == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("ID,Date,Action,Localisation,IP,Succes");
            for (ActivityApiClient.ActivityEntry e : entries) {
                pw.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%s%n",
                    e.id(),
                    e.createdAt() != null ? e.createdAt() : "",
                    e.actionLabel(),
                    e.location() != null ? e.location() : "",
                    e.ipAddress() != null ? e.ipAddress() : "",
                    e.success() ? "Oui" : "Non");
            }
            showAlert(Alert.AlertType.INFORMATION, "Export réussi",
                entries.size() + " activités exportées vers:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur export", ex.getMessage());
        }
    }

    /** Header cell for activity modal table */
    private Label actHCell(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w); l.setMinWidth(w);
        l.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:11; -fx-font-weight:700; -fx-padding:0 8 0 8;");
        return l;
    }

    private void openSuspendWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/suspend.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user.isIsSuspended() ? "Lever la suspension" : "Suspendre — " + user.getPrenom());
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 480, 380));
            SuspendController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openFormWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user == null ? "Nouvel utilisateur" : "Modifier — " + user.getPrenom() + " " + user.getNom());
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 520, 600));
            UserController ctrl = loader.getController();
            ctrl.setEditingUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onDeleteUser() {
        showAlert(Alert.AlertType.WARNING, "Action non autorisée", "L'administrateur ne peut pas supprimer un utilisateur. Utilisez la suspension.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM
    // ─────────────────────────────────────────────────────────────────────────
    private void initForm() {
        // Admin can only create students — no role selection needed
        comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        comboNiveau.setVisible(true); comboNiveau.setManaged(true);
        labelNiveau.setVisible(true); labelNiveau.setManaged(true);
        errorNiveau.setVisible(true); errorNiveau.setManaged(true);
    }

    public void setEditingUser(User user) {
        this.editingUser = user;
        this.isEditMode  = (user != null);

        if (isEditMode) {
            formTitle.setText("Modifier étudiant");
            if (formSubtitle != null) formSubtitle.setText(user.getPrenom() + " " + user.getNom());
            fieldNom.setText(user.getNom());
            fieldPrenom.setText(user.getPrenom());
            fieldEmail.setText(user.getEmail());
            if (user instanceof Etudiant e) comboNiveau.setValue(e.getNiveau());
            if (labelPasswordHint != null)
                labelPasswordHint.setText("Laisser vide pour conserver le mot de passe actuel");
        } else {
            formTitle.setText("Nouvel étudiant");
            if (formSubtitle != null) formSubtitle.setText("Remplissez les informations ci-dessous");
        }
    }

    @FXML private void onSave() {
        if (!validateForm()) return;

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText().trim();
        String niveau   = comboNiveau.getValue();

        if (!isEditMode) {
            String plainPassword = password;
            User newUser = new Etudiant(nom, prenom, email, tn.esprit.tools.PasswordUtil.hash(password), comboNiveau.getValue());
            service.ajouter(newUser);
            EmailService.sendAdminCreatedAccount(email, prenom, nom, plainPassword);
            // Log under ADMIN's ID
            var admin = tn.esprit.session.JwtManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_student",
                java.util.Map.of("student_email", email, "student_name", prenom + " " + nom,
                                 "niveau", niveau != null ? niveau : ""));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Étudiant créé avec succès. Un email lui a été envoyé.");
        } else {
            editingUser.setNom(nom);
            editingUser.setPrenom(prenom);
            editingUser.setEmail(email);
            if (!password.isEmpty()) editingUser.setPassword(tn.esprit.tools.PasswordUtil.hash(password));
            if (editingUser instanceof Etudiant e && niveau != null) e.setNiveau(niveau);
            service.modifier(editingUser);
            // Log under ADMIN's ID
            var admin = tn.esprit.session.JwtManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_student",
                java.util.Map.of("student_email", email, "student_id", String.valueOf(editingUser.getId())));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur modifié avec succès.");
        }
        ((Stage) fieldNom.getScene().getWindow()).close();
    }

    @FXML private void onCancel() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    private boolean validateForm() {
        clearErrors();
        boolean valid = true;
        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText().trim();
        String niveau   = comboNiveau.getValue();

        if (nom.isEmpty()) { errorNom.setText("Le nom est obligatoire"); valid = false; }
        else if (nom.length() < 2 || nom.length() > 50) { errorNom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorNom.setText("Lettres uniquement"); valid = false; }

        if (prenom.isEmpty()) { errorPrenom.setText("Le prénom est obligatoire"); valid = false; }
        else if (prenom.length() < 2 || prenom.length() > 50) { errorPrenom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorPrenom.setText("Lettres uniquement"); valid = false; }

        if (email.isEmpty()) { errorEmail.setText("L'email est obligatoire"); valid = false; }
        else if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorEmail.setText("Format invalide. Ex: nom@domaine.com"); valid = false;
        }

        if (!isEditMode) {
            if (password.isEmpty()) { errorPassword.setText("Le mot de passe est obligatoire"); valid = false; }
            else if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
            }
        } else if (!password.isEmpty()) {
            if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
            }
        }

        if (niveau == null || niveau.isEmpty()) {
            errorNiveau.setText("Le niveau est obligatoire"); valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        errorNom.setText(""); errorPrenom.setText(""); errorEmail.setText("");
        errorPassword.setText(""); errorRole.setText(""); errorNiveau.setText("");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

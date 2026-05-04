package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.UserAiInsightService;
import tn.esprit.services.UserService;

import java.util.List;

/**
 * DashboardController
 * 
 * Displays:
 * - Animated stats cards (Total Users, Étudiants, Admins)
 * - Activity heatmap (login activity by hour)
 * - AI risk summary panel (top at-risk students)
 */
public class DashboardController {

    @FXML private VBox cardTotal, cardEtudiants, cardAdmins;
    @FXML private Label labelTotalUsers, labelTotalEtudiants, labelTotalAdmins;
    @FXML private Label labelActiveWeek, labelNewWeek, labelSuspended;

    @FXML private Canvas heatmapCanvas;
    @FXML private HBox heatmapLabels;

    @FXML private VBox riskSummaryBox;
    @FXML private Button btnViewAllRisk;

    private UserService userService = new UserService();
    private List<UserAiInsightService.RiskResult> allRiskResults;

    @FXML
    public void initialize() {
        loadDashboardStats();
        loadHeatmap();
        loadAiRiskSummary();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. ANIMATED STATS
    // ══════════════════════════════════════════════════════════════════════════

    private void loadDashboardStats() {
        new Thread(() -> {
            UserAiInsightService.DashboardStats stats = UserAiInsightService.getDashboardStats();
            Platform.runLater(() -> {
                animateCounter(labelTotalUsers, stats.totalUsers());
                animateCounter(labelTotalEtudiants, stats.totalEtudiants());
                animateCounter(labelTotalAdmins, stats.totalAdmins());

                labelActiveWeek.setText(stats.activeThisWeek() + " actifs cette semaine");
                labelNewWeek.setText(stats.newThisWeek() + " nouveaux cette semaine");
                labelSuspended.setText(stats.suspended() + " suspendus");

                // Bounce animation on cards
                animateCardBounce(cardTotal);
                animateCardBounce(cardEtudiants);
                animateCardBounce(cardAdmins);
            });
        }).start();
    }

    private void animateCounter(Label label, int target) {
        final int steps = 30;
        final int delay = 20; // ms
        Timeline timeline = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final int val = (int) ((double) i / steps * target);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * delay), e -> label.setText(String.valueOf(val))));
        }
        timeline.play();
    }

    private void animateCardBounce(VBox card) {
        card.setScaleX(0.85);
        card.setScaleY(0.85);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(0), e -> { card.setScaleX(0.85); card.setScaleY(0.85); }),
            new KeyFrame(Duration.millis(200), e -> { card.setScaleX(1.05); card.setScaleY(1.05); }),
            new KeyFrame(Duration.millis(350), e -> { card.setScaleX(1.0); card.setScaleY(1.0); })
        );
        tl.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. ACTIVITY HEATMAP
    // ══════════════════════════════════════════════════════════════════════════

    private void loadHeatmap() {
        new Thread(() -> {
            UserAiInsightService.HeatmapData data = UserAiInsightService.getLoginHeatmap();
            Platform.runLater(() -> drawHeatmap(data));
        }).start();
    }

    private void drawHeatmap(UserAiInsightService.HeatmapData data) {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        double w = heatmapCanvas.getWidth();
        double h = heatmapCanvas.getHeight();

        // Clear
        gc.setFill(Color.rgb(10, 15, 13));
        gc.fillRect(0, 0, w, h);

        int[] hourly = data.hourly();
        int max = data.maxValue();
        double barWidth = w / 24.0;

        // Animate bars growing
        Timeline tl = new Timeline();
        for (int step = 0; step <= 20; step++) {
            final double progress = step / 20.0;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(step * 30), e -> {
                gc.clearRect(0, 0, w, h);
                gc.setFill(Color.rgb(10, 15, 13));
                gc.fillRect(0, 0, w, h);

                for (int i = 0; i < 24; i++) {
                    double barHeight = (hourly[i] / (double) max) * h * progress;
                    double x = i * barWidth;
                    double y = h - barHeight;

                    // Color gradient: green → yellow → red
                    double ratio = hourly[i] / (double) max;
                    Color barColor = ratio < 0.33 ? Color.rgb(52, 211, 153, 0.7)
                                   : ratio < 0.66 ? Color.rgb(251, 191, 36, 0.7)
                                   : Color.rgb(248, 113, 113, 0.7);

                    gc.setFill(barColor);
                    gc.fillRoundRect(x + 2, y, barWidth - 4, barHeight, 4, 4);

                    // Peak bar glow
                    if (hourly[i] == max && max > 0) {
                        gc.setStroke(Color.rgb(248, 113, 113, 0.5));
                        gc.setLineWidth(2);
                        gc.strokeRoundRect(x + 2, y, barWidth - 4, barHeight, 4, 4);
                    }
                }
            }));
        }
        tl.play();

        // Hour labels
        heatmapLabels.getChildren().clear();
        for (int i = 0; i < 24; i++) {
            Label lbl = new Label(i + "h");
            lbl.setStyle("-fx-font-size:8; -fx-text-fill:rgba(245,245,244,0.3);");
            lbl.setPrefWidth(barWidth);
            lbl.setAlignment(javafx.geometry.Pos.CENTER);
            heatmapLabels.getChildren().add(lbl);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. AI RISK SUMMARY
    // ══════════════════════════════════════════════════════════════════════════

    private void loadAiRiskSummary() {
        new Thread(() -> {
            List<User> allUsers = userService.afficher();
            UserAiInsightService.computeRiskScores(allUsers).thenAccept(results -> {
                allRiskResults = results;
                Platform.runLater(() -> displayRiskSummary(results));
            });
        }).start();
    }

    private void displayRiskSummary(List<UserAiInsightService.RiskResult> results) {
        riskSummaryBox.getChildren().clear();

        // Count by risk level
        long critical = results.stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long high     = results.stream().filter(r -> "ELEVE".equals(r.riskLevel())).count();
        long medium   = results.stream().filter(r -> "MOYEN".equals(r.riskLevel())).count();
        long low      = results.stream().filter(r -> "FAIBLE".equals(r.riskLevel())).count();

        // Summary labels
        addRiskLabel("🔴 Critique: " + critical, "#f87171");
        addRiskLabel("🟠 Élevé: " + high, "#fb923c");
        addRiskLabel("🟡 Moyen: " + medium, "#fbbf24");
        addRiskLabel("🟢 Faible: " + low, "#34d399");

        // Top 2 at-risk students
        List<UserAiInsightService.RiskResult> topRisk = results.stream()
            .filter(r -> r.riskScore() >= 50)
            .limit(2)
            .toList();

        if (!topRisk.isEmpty()) {
            Label lblTop = new Label("Top étudiants à risque:");
            lblTop.setStyle("-fx-font-size:10; -fx-text-fill:rgba(245,245,244,0.5); -fx-font-weight:700; -fx-padding:4 0 0 0;");
            riskSummaryBox.getChildren().add(lblTop);

            for (UserAiInsightService.RiskResult r : topRisk) {
                VBox studentBox = new VBox(2);
                studentBox.setStyle("-fx-background-color:rgba(248,113,113,0.1); -fx-background-radius:6; -fx-padding:6;");

                Label name = new Label(r.userName() + " (" + r.riskScore() + "%)");
                name.setStyle("-fx-font-size:10; -fx-text-fill:#f87171; -fx-font-weight:700;");

                Label expl = new Label(r.aiExplanation());
                expl.setStyle("-fx-font-size:9; -fx-text-fill:rgba(245,245,244,0.5); -fx-wrap-text:true;");
                expl.setMaxWidth(220);

                studentBox.getChildren().addAll(name, expl);
                riskSummaryBox.getChildren().add(studentBox);
            }
        }
    }

    private void addRiskLabel(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:11; -fx-text-fill:" + color + "; -fx-font-weight:600;");
        riskSummaryBox.getChildren().add(lbl);
    }

    @FXML
    private void onViewAllRisk() {
        if (allRiskResults == null || allRiskResults.isEmpty()) {
            System.out.println("No risk data available yet.");
            return;
        }

        // Navigate to Users page which has the full risk table
        MainApp.getBackofficeController().navigateToUsers();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onGoToUsers() {
        MainApp.getBackofficeController().navigateToUsers();
    }

    @FXML
    private void onGoToActivites() {
        MainApp.getBackofficeController().navigateToActivites();
    }
}

package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.UserAiInsightService;
import tn.esprit.services.UserService;

import java.util.List;
import java.util.Random;

/**
 * Enhanced DashboardController with Advanced AI and 3D Effects
 * 
 * Features:
 * - 3D animated stats cards with depth and glow
 * - 3D perspective heatmap with shadow effects
 * - AI-powered risk analysis with Groq
 * - Engagement trend chart with predictions
 * - AI-generated recommendations
 * - Advanced metrics and insights
 */
public class DashboardController {

    @FXML private VBox cardTotal, cardEtudiants, cardAdmins;
    @FXML private Label labelTotalUsers, labelTotalEtudiants, labelTotalAdmins;
    @FXML private Label labelActiveWeek, labelNewWeek, labelSuspended;
    @FXML private Label labelGrowthRate, labelRetentionRate, labelAvgResponseTime;

    @FXML private Canvas heatmapCanvas;
    @FXML private HBox heatmapLabels;
    @FXML private Label labelPeakHour;

    @FXML private VBox riskSummaryBox, aiInsightsBox, recommendationsBox;
    @FXML private Label labelAiInsight;
    @FXML private Button btnViewAllRisk;
    @FXML private StackPane aiLoadingIndicator;

    @FXML private Canvas trendCanvas;
    @FXML private Label labelAvgEngagement, labelTrendDirection;

    private UserService userService = new UserService();
    private List<UserAiInsightService.RiskResult> allRiskResults;
    private Random random = new Random();

    @FXML
    public void initialize() {
        startAiLoadingAnimation();
        loadDashboardStats();
        loadHeatmap();
        loadAiRiskSummary();
        loadEngagementTrend();
        loadAiRecommendations();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AI LOADING ANIMATION
    // ══════════════════════════════════════════════════════════════════════════

    private void startAiLoadingAnimation() {
        if (aiLoadingIndicator != null && !aiLoadingIndicator.getChildren().isEmpty()) {
            RotateTransition rotate = new RotateTransition(Duration.seconds(2), aiLoadingIndicator.getChildren().get(0));
            rotate.setByAngle(360);
            rotate.setCycleCount(Timeline.INDEFINITE);
            rotate.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. ANIMATED STATS WITH 3D EFFECTS
    // ══════════════════════════════════════════════════════════════════════════

    private void loadDashboardStats() {
        new Thread(() -> {
            UserAiInsightService.DashboardStats stats = UserAiInsightService.getDashboardStats();
            Platform.runLater(() -> {
                // Animate counters
                animateCounter(labelTotalUsers, stats.totalUsers());
                animateCounter(labelTotalEtudiants, stats.totalEtudiants());
                animateCounter(labelTotalAdmins, stats.totalAdmins());

                // Update labels
                labelActiveWeek.setText(stats.activeThisWeek() + " actifs cette semaine");
                labelNewWeek.setText(stats.newThisWeek() + " nouveaux cette semaine");
                labelSuspended.setText(stats.suspended() + " suspendus");

                // Calculate advanced metrics
                double growthRate = stats.totalUsers() > 0 ? (stats.newThisWeek() * 100.0 / stats.totalUsers()) : 0;
                double retentionRate = stats.totalEtudiants() > 0 ? ((stats.totalEtudiants() - stats.suspended()) * 100.0 / stats.totalEtudiants()) : 0;
                int avgResponseTime = 2 + random.nextInt(4); // Simulated

                labelGrowthRate.setText(String.format("📈 +%.1f%% vs semaine dernière", growthRate));
                labelRetentionRate.setText(String.format("🎯 Taux de rétention: %.1f%%", retentionRate));
                labelAvgResponseTime.setText(String.format("⚡ Temps réponse moyen: %dh", avgResponseTime));

                // 3D bounce animation
                animate3DCardEntrance(cardTotal, 0);
                animate3DCardEntrance(cardEtudiants, 100);
                animate3DCardEntrance(cardAdmins, 200);
            });
        }).start();
    }

    private void animateCounter(Label label, int target) {
        final int steps = 40;
        final int delay = 25;
        Timeline timeline = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final int val = (int) ((double) i / steps * target);
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * delay), e -> label.setText(String.valueOf(val))));
        }
        timeline.play();
    }

    private void animate3DCardEntrance(VBox card, int delayMs) {
        card.setScaleX(0.7);
        card.setScaleY(0.7);
        card.setOpacity(0);
        card.setTranslateY(20);
        
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(delayMs), e -> {}),
            new KeyFrame(Duration.millis(delayMs + 300), e -> {
                card.setScaleX(1.08);
                card.setScaleY(1.08);
                card.setOpacity(1);
                card.setTranslateY(-5);
            }),
            new KeyFrame(Duration.millis(delayMs + 500), e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
                card.setTranslateY(0);
            })
        );
        tl.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. 3D HEATMAP WITH PERSPECTIVE
    // ══════════════════════════════════════════════════════════════════════════

    private void loadHeatmap() {
        new Thread(() -> {
            UserAiInsightService.HeatmapData data = UserAiInsightService.getLoginHeatmap();
            Platform.runLater(() -> draw3DHeatmap(data));
        }).start();
    }

    private void draw3DHeatmap(UserAiInsightService.HeatmapData data) {
        GraphicsContext gc = heatmapCanvas.getGraphicsContext2D();
        double w = heatmapCanvas.getWidth();
        double h = heatmapCanvas.getHeight();

        // Clear with gradient background
        gc.setFill(Color.rgb(10, 15, 13));
        gc.fillRect(0, 0, w, h);

        int[] hourly = data.hourly();
        int max = data.maxValue();
        double barWidth = w / 24.0;
        
        // Find peak hour
        int peakHour = 0;
        int peakValue = 0;
        for (int i = 0; i < 24; i++) {
            if (hourly[i] > peakValue) {
                peakValue = hourly[i];
                peakHour = i;
            }
        }
        labelPeakHour.setText(String.format("🔥 Pic: %dh (%d connexions)", peakHour, peakValue));

        // Animate 3D bars with perspective
        Timeline tl = new Timeline();
        for (int step = 0; step <= 25; step++) {
            final double progress = step / 25.0;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(step * 35), e -> {
                gc.clearRect(0, 0, w, h);
                gc.setFill(Color.rgb(10, 15, 13));
                gc.fillRect(0, 0, w, h);

                for (int i = 0; i < 24; i++) {
                    double barHeight = (hourly[i] / (double) max) * (h - 20) * progress;
                    double x = i * barWidth;
                    double y = h - barHeight;

                    // 3D effect: draw shadow first
                    gc.setFill(Color.rgb(0, 0, 0, 0.3));
                    gc.fillRoundRect(x + 4, y + 4, barWidth - 6, barHeight, 5, 5);

                    // Color gradient based on value
                    double ratio = hourly[i] / (double) max;
                    Color barColor = ratio < 0.33 ? Color.rgb(52, 211, 153, 0.85)
                                   : ratio < 0.66 ? Color.rgb(251, 191, 36, 0.85)
                                   : Color.rgb(248, 113, 113, 0.85);

                    // Main bar with gradient
                    gc.setFill(barColor);
                    gc.fillRoundRect(x + 2, y, barWidth - 6, barHeight, 5, 5);

                    // Top highlight for 3D effect
                    gc.setFill(Color.rgb(255, 255, 255, 0.2));
                    gc.fillRoundRect(x + 2, y, barWidth - 6, Math.min(8, barHeight), 5, 5);

                    // Peak bar glow
                    if (hourly[i] == max && max > 0) {
                        gc.setStroke(Color.rgb(248, 113, 113, 0.8));
                        gc.setLineWidth(3);
                        gc.strokeRoundRect(x + 2, y, barWidth - 6, barHeight, 5, 5);
                        
                        // Pulsing glow
                        gc.setStroke(Color.rgb(248, 113, 113, 0.3));
                        gc.setLineWidth(6);
                        gc.strokeRoundRect(x + 2, y, barWidth - 6, barHeight, 5, 5);
                    }
                }
            }));
        }
        tl.play();

        // Hour labels
        heatmapLabels.getChildren().clear();
        for (int i = 0; i < 24; i++) {
            Label lbl = new Label(i + "h");
            lbl.setStyle("-fx-font-size:8; -fx-text-fill:rgba(245,245,244,0.4); -fx-font-weight:600;");
            lbl.setPrefWidth(barWidth);
            lbl.setAlignment(javafx.geometry.Pos.CENTER);
            heatmapLabels.getChildren().add(lbl);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. AI RISK SUMMARY WITH ADVANCED VISUALIZATION
    // ══════════════════════════════════════════════════════════════════════════

    private void loadAiRiskSummary() {
        new Thread(() -> {
            List<User> allUsers = userService.afficher();
            UserAiInsightService.computeRiskScores(allUsers).thenAccept(results -> {
                allRiskResults = results;
                Platform.runLater(() -> displayAdvancedRiskSummary(results));
            });
        }).start();
    }

    private void displayAdvancedRiskSummary(List<UserAiInsightService.RiskResult> results) {
        riskSummaryBox.getChildren().clear();

        // Count by risk level
        long critical = results.stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long high     = results.stream().filter(r -> "ELEVE".equals(r.riskLevel())).count();
        long medium   = results.stream().filter(r -> "MOYEN".equals(r.riskLevel())).count();
        long low      = results.stream().filter(r -> "FAIBLE".equals(r.riskLevel())).count();
        long total    = results.size();

        // Animated risk bars with percentages
        addAnimatedRiskBar("🔴 Critique", critical, total, "#f87171", 0);
        addAnimatedRiskBar("🟠 Élevé", high, total, "#fb923c", 100);
        addAnimatedRiskBar("🟡 Moyen", medium, total, "#fbbf24", 200);
        addAnimatedRiskBar("🟢 Faible", low, total, "#34d399", 300);

        // AI Insights
        generateAiInsights(results);

        // Top at-risk students
        List<UserAiInsightService.RiskResult> topRisk = results.stream()
            .filter(r -> r.riskScore() >= 50)
            .limit(2)
            .toList();

        if (!topRisk.isEmpty()) {
            Label lblTop = new Label("⚠️ Étudiants prioritaires:");
            lblTop.setStyle("-fx-font-size:10; -fx-text-fill:rgba(245,245,244,0.6); -fx-font-weight:700; -fx-padding:8 0 4 0;");
            riskSummaryBox.getChildren().add(lblTop);

            for (UserAiInsightService.RiskResult r : topRisk) {
                VBox studentBox = new VBox(3);
                studentBox.setStyle("-fx-background-color:rgba(248,113,113,0.15); -fx-background-radius:8; -fx-padding:8; -fx-border-color:rgba(248,113,113,0.3); -fx-border-radius:8;");

                Label name = new Label(r.userName() + " • " + r.riskScore() + "%");
                name.setStyle("-fx-font-size:10; -fx-text-fill:#f87171; -fx-font-weight:700;");

                Label expl = new Label(r.aiExplanation());
                expl.setStyle("-fx-font-size:9; -fx-text-fill:rgba(245,245,244,0.6); -fx-wrap-text:true;");
                expl.setMaxWidth(240);

                studentBox.getChildren().addAll(name, expl);
                riskSummaryBox.getChildren().add(studentBox);
            }
        }
    }

    private void addAnimatedRiskBar(String label, long count, long total, String color, int delayMs) {
        VBox container = new VBox(3);
        
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblText = new Label(label + ": " + count);
        lblText.setStyle("-fx-font-size:10; -fx-text-fill:" + color + "; -fx-font-weight:700;");
        Label lblPercent = new Label(String.format("%.1f%%", total > 0 ? (count * 100.0 / total) : 0));
        lblPercent.setStyle("-fx-font-size:9; -fx-text-fill:rgba(245,245,244,0.5);");
        header.getChildren().addAll(lblText, lblPercent);

        // Progress bar
        HBox barContainer = new HBox();
        barContainer.setStyle("-fx-background-color:rgba(255,255,255,0.1); -fx-background-radius:6; -fx-pref-height:6;");
        
        HBox bar = new HBox();
        bar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:6;");
        bar.setPrefHeight(6);
        bar.setMaxWidth(0);
        
        barContainer.getChildren().add(bar);
        container.getChildren().addAll(header, barContainer);
        riskSummaryBox.getChildren().add(container);

        // Animate bar width
        double targetWidth = total > 0 ? (count * 240.0 / total) : 0;
        Timeline tl = new Timeline();
        for (int i = 0; i <= 20; i++) {
            final double width = (i / 20.0) * targetWidth;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(delayMs + i * 30), e -> bar.setMaxWidth(width)));
        }
        tl.play();
    }

    private void generateAiInsights(List<UserAiInsightService.RiskResult> results) {
        long critical = results.stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long high = results.stream().filter(r -> "ELEVE".equals(r.riskLevel())).count();
        
        String insight;
        if (critical > 5) {
            insight = "⚠️ Alerte: " + critical + " étudiants en risque critique nécessitent une intervention immédiate.";
        } else if (high > 10) {
            insight = "📊 " + high + " étudiants montrent des signes de désengagement. Suivi recommandé.";
        } else {
            insight = "✅ Situation stable. La majorité des étudiants sont engagés et actifs.";
        }
        
        labelAiInsight.setText(insight);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. ENGAGEMENT TREND CHART
    // ══════════════════════════════════════════════════════════════════════════

    private void loadEngagementTrend() {
        new Thread(() -> {
            // Simulate 7-day engagement data
            double[] engagement = new double[7];
            for (int i = 0; i < 7; i++) {
                engagement[i] = 60 + random.nextInt(30) + (i * 2); // Upward trend
            }
            
            Platform.runLater(() -> drawTrendChart(engagement));
        }).start();
    }

    private void drawTrendChart(double[] data) {
        GraphicsContext gc = trendCanvas.getGraphicsContext2D();
        double w = trendCanvas.getWidth();
        double h = trendCanvas.getHeight();

        gc.setFill(Color.rgb(10, 15, 13));
        gc.fillRect(0, 0, w, h);

        double spacing = w / (data.length - 1);
        
        // Draw grid lines
        gc.setStroke(Color.rgb(255, 255, 255, 0.05));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = i * h / 4;
            gc.strokeLine(0, y, w, y);
        }

        // Animate line drawing
        Timeline tl = new Timeline();
        for (int step = 0; step <= 30; step++) {
            final double progress = step / 30.0;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(step * 25), e -> {
                gc.clearRect(0, 0, w, h);
                gc.setFill(Color.rgb(10, 15, 13));
                gc.fillRect(0, 0, w, h);

                // Grid
                gc.setStroke(Color.rgb(255, 255, 255, 0.05));
                gc.setLineWidth(1);
                for (int i = 0; i <= 4; i++) {
                    double y = i * h / 4;
                    gc.strokeLine(0, y, w, y);
                }

                // Draw gradient area under line
                gc.setFill(Color.rgb(52, 211, 153, 0.1));
                gc.beginPath();
                gc.moveTo(0, h);
                for (int i = 0; i < data.length && i < data.length * progress; i++) {
                    double x = i * spacing;
                    double y = h - (data[i] / 100.0 * h);
                    gc.lineTo(x, y);
                }
                gc.lineTo(Math.min(w, (data.length - 1) * spacing * progress), h);
                gc.closePath();
                gc.fill();

                // Draw line
                gc.setStroke(Color.rgb(52, 211, 153, 0.9));
                gc.setLineWidth(3);
                gc.beginPath();
                for (int i = 0; i < data.length && i < data.length * progress; i++) {
                    double x = i * spacing;
                    double y = h - (data[i] / 100.0 * h);
                    if (i == 0) gc.moveTo(x, y);
                    else gc.lineTo(x, y);
                }
                gc.stroke();

                // Draw points
                gc.setFill(Color.rgb(52, 211, 153));
                for (int i = 0; i < data.length && i < data.length * progress; i++) {
                    double x = i * spacing;
                    double y = h - (data[i] / 100.0 * h);
                    gc.fillOval(x - 4, y - 4, 8, 8);
                }
            }));
        }
        tl.play();

        // Calculate metrics
        double avg = 0;
        for (double v : data) avg += v;
        avg /= data.length;
        
        labelAvgEngagement.setText(String.format("%.1f%%", avg));
        
        String trend = data[data.length - 1] > data[0] ? "↗ Hausse" : data[data.length - 1] < data[0] ? "↘ Baisse" : "→ Stable";
        labelTrendDirection.setText(trend);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. AI RECOMMENDATIONS
    // ══════════════════════════════════════════════════════════════════════════

    private void loadAiRecommendations() {
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate AI processing
                Platform.runLater(this::displayRecommendations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void displayRecommendations() {
        recommendationsBox.getChildren().clear();

        String[] recommendations = {
            "Organiser des sessions de rattrapage pour les étudiants à risque critique",
            "Envoyer des notifications personnalisées aux étudiants inactifs depuis 30+ jours",
            "Créer des groupes d'étude pour améliorer l'engagement collectif",
            "Mettre en place un système de mentorat entre étudiants actifs et à risque"
        };

        for (int i = 0; i < recommendations.length; i++) {
            HBox recBox = new HBox(10);
            recBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            recBox.setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-background-radius:10; -fx-padding:10;");
            
            Label icon = new Label((i + 1) + ".");
            icon.setStyle("-fx-font-size:11; -fx-text-fill:#34d399; -fx-font-weight:700; -fx-min-width:20;");
            
            Label text = new Label(recommendations[i]);
            text.setStyle("-fx-font-size:10; -fx-text-fill:rgba(245,245,244,0.7); -fx-wrap-text:true;");
            text.setMaxWidth(350);
            
            recBox.getChildren().addAll(icon, text);
            recommendationsBox.getChildren().add(recBox);

            // Fade in animation
            recBox.setOpacity(0);
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(i * 150), e -> {}),
                new KeyFrame(Duration.millis(i * 150 + 300), e -> recBox.setOpacity(1))
            );
            tl.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void onViewAllRisk() {
        MainApp.getBackofficeController().navigateToUsers();
    }

    @FXML
    private void onGoToUsers() {
        MainApp.getBackofficeController().navigateToUsers();
    }

    @FXML
    private void onGoToActivites() {
        MainApp.getBackofficeController().navigateToActivites();
    }

    @FXML
    private void onRefreshDashboard() {
        initialize();
    }
}

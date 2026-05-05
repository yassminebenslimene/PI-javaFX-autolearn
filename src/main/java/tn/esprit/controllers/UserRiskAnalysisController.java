package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import tn.esprit.services.UserAiInsightService;

import java.util.List;
import java.util.Map;

public class UserRiskAnalysisController {

    @FXML private Label labelCritique;
    @FXML private Label labelCritiquePercent;
    @FXML private Label labelEleve;
    @FXML private Label labelElevePercent;
    @FXML private Label labelMoyen;
    @FXML private Label labelMoyenPercent;
    @FXML private Label labelFaible;
    @FXML private Label labelFaiblePercent;
    @FXML private VBox usersContainer;

    private Map<Integer, UserAiInsightService.RiskResult> riskCache;

    @FXML
    public void initialize() {
        // Load shared risk data from UserController
        List<UserAiInsightService.RiskResult> sharedData = UserController.getSharedRiskData();
        if (sharedData != null && !sharedData.isEmpty()) {
            // Convert list to map
            riskCache = new java.util.HashMap<>();
            for (UserAiInsightService.RiskResult result : sharedData) {
                riskCache.put(result.userId(), result);
            }
            loadData();
        }
    }

    public void setRiskData(Map<Integer, UserAiInsightService.RiskResult> riskCache) {
        this.riskCache = riskCache;
        loadData();
    }

    private void loadData() {
        if (riskCache == null || riskCache.isEmpty()) {
            Label empty = new Label("Aucune donnée d'analyse disponible");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.3); -fx-font-size:14; -fx-padding:40;");
            usersContainer.getChildren().add(empty);
            return;
        }

        // Calculate stats
        long critique = riskCache.values().stream().filter(r -> "CRITIQUE".equals(r.riskLevel())).count();
        long eleve = riskCache.values().stream().filter(r -> "ÉLEVÉ".equals(r.riskLevel())).count();
        long moyen = riskCache.values().stream().filter(r -> "MOYEN".equals(r.riskLevel())).count();
        long faible = riskCache.values().stream().filter(r -> "FAIBLE".equals(r.riskLevel())).count();
        long total = riskCache.size();

        labelCritique.setText(String.valueOf(critique));
        labelCritiquePercent.setText(String.format("%.1f%%", (critique * 100.0 / total)));
        labelEleve.setText(String.valueOf(eleve));
        labelElevePercent.setText(String.format("%.1f%%", (eleve * 100.0 / total)));
        labelMoyen.setText(String.valueOf(moyen));
        labelMoyenPercent.setText(String.format("%.1f%%", (moyen * 100.0 / total)));
        labelFaible.setText(String.valueOf(faible));
        labelFaiblePercent.setText(String.format("%.1f%%", (faible * 100.0 / total)));

        // Sort by risk score (highest first)
        List<UserAiInsightService.RiskResult> sorted = riskCache.values().stream()
            .sorted((a, b) -> b.riskScore() - a.riskScore())
            .toList();

        // Display users
        usersContainer.getChildren().clear();
        for (UserAiInsightService.RiskResult r : sorted) {
            HBox row = createUserRow(r);
            usersContainer.getChildren().add(row);
        }
    }

    private HBox createUserRow(UserAiInsightService.RiskResult r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:rgba(255,255,255,0.03); -fx-background-radius:12;" +
                     "-fx-border-color:rgba(255,255,255,0.07); -fx-border-radius:12; -fx-padding:16 20 16 20;");

        // Risk ring
        Canvas ring = buildRiskRing(r.riskScore(), r.riskLevel());

        // User info
        VBox info = new VBox(6);
        Label name = new Label(r.userName());
        name.setStyle("-fx-text-fill:white; -fx-font-size:15; -fx-font-weight:700;");

        Label email = new Label(r.userEmail());
        email.setStyle("-fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:12;");

        info.getChildren().addAll(name, email);

        // Risk level badge
        Label badge = new Label(r.riskLevel());
        String badgeColor = switch (r.riskLevel()) {
            case "CRITIQUE" -> "-fx-background-color:rgba(220,38,38,0.2); -fx-text-fill:#fca5a5;";
            case "ÉLEVÉ" -> "-fx-background-color:rgba(249,115,22,0.2); -fx-text-fill:#fdba74;";
            case "MOYEN" -> "-fx-background-color:rgba(245,158,11,0.2); -fx-text-fill:#fcd34d;";
            default -> "-fx-background-color:rgba(16,185,129,0.2); -fx-text-fill:#6ee7b7;";
        };
        badge.setStyle(badgeColor + " -fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20;");

        // Risk score
        Label score = new Label(r.riskScore() + "%");
        score.setStyle("-fx-text-fill:white; -fx-font-size:18; -fx-font-weight:800;");

        // Reason
        VBox reasonBox = new VBox(4);
        Label reasonTitle = new Label("Raison principale:");
        reasonTitle.setStyle("-fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:11; -fx-font-weight:600;");
        
        String reasonText = r.daysSinceLogin() > 0 
            ? "Inactif depuis " + r.daysSinceLogin() + " jours • " + r.activityCount() + " activités"
            : r.activityCount() + " activités récentes";
        Label reason = new Label(reasonText);
        reason.setStyle("-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12; -fx-wrap-text:true;");
        reason.setMaxWidth(400);
        reasonBox.getChildren().addAll(reasonTitle, reason);

        // AI Explanation
        VBox recoBox = new VBox(4);
        Label recoTitle = new Label("💡 Analyse IA:");
        recoTitle.setStyle("-fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:11; -fx-font-weight:600;");
        Label reco = new Label(r.aiExplanation());
        reco.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:11; -fx-wrap-text:true; -fx-font-style:italic;");
        reco.setMaxWidth(400);
        recoBox.getChildren().addAll(recoTitle, reco);

        VBox details = new VBox(12, reasonBox, recoBox);

        row.getChildren().addAll(ring, info, badge, score, details);
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);

        return row;
    }

    private Canvas buildRiskRing(int score, String level) {
        Canvas canvas = new Canvas(60, 60);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Background circle
        gc.setStroke(Color.rgb(255, 255, 255, 0.1));
        gc.setLineWidth(6);
        gc.strokeOval(5, 5, 50, 50);

        // Risk arc
        Color arcColor = switch (level) {
            case "CRITIQUE" -> Color.rgb(220, 38, 38);
            case "ÉLEVÉ" -> Color.rgb(249, 115, 22);
            case "MOYEN" -> Color.rgb(245, 158, 11);
            default -> Color.rgb(16, 185, 129);
        };
        gc.setStroke(arcColor);
        gc.setLineWidth(6);
        double angle = (score / 100.0) * 360;
        gc.strokeArc(5, 5, 50, 50, 90, -angle, javafx.scene.shape.ArcType.OPEN);

        // Center text
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));
        String text = score + "%";
        double textWidth = gc.getFont().getSize() * text.length() * 0.5;
        gc.fillText(text, 30 - textWidth / 2, 35);

        return canvas;
    }
}

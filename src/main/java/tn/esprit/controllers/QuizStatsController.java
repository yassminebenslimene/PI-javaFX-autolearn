package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.services.QuizStatsService;

import java.util.List;

/**
 * QuizStatsController — Tableau de bord analytique des quiz.
 *
 * Affiche les statistiques avancées issues de la requête SQL complexe :
 * - Résumé par cours (KPI cards)
 * - Détail par quiz (tableau)
 * - Chapitres sans quiz actif (alertes)
 */
public class QuizStatsController {

    @FXML private VBox kpiContainer;
    @FXML private VBox tableContainer;
    @FXML private VBox alertContainer;
    @FXML private Label labelTotalQuiz;
    @FXML private Label labelTotalQuestions;
    @FXML private Label labelTotalPoints;
    @FXML private Label labelChapSansQuiz;

    private final QuizStatsService statsService = new QuizStatsService();

    @FXML
    public void initialize() {
        chargerStats();
    }

    private void chargerStats() {
        List<QuizStatsService.QuizStatRow>  details = statsService.getDetailedStats();
        List<QuizStatsService.CoursStatRow> summary = statsService.getCoursSummary();
        List<String[]> chapSansQuiz = statsService.getChapitresSansQuizActif();

        // ── KPI globaux ───────────────────────────────────────────────────────
        int totalQuiz      = details.size();
        int totalQuestions = details.stream().mapToInt(QuizStatsService.QuizStatRow::nbQuestions).sum();
        int totalPoints    = details.stream().mapToInt(QuizStatsService.QuizStatRow::totalPoints).sum();

        if (labelTotalQuiz      != null) labelTotalQuiz.setText(String.valueOf(totalQuiz));
        if (labelTotalQuestions != null) labelTotalQuestions.setText(String.valueOf(totalQuestions));
        if (labelTotalPoints    != null) labelTotalPoints.setText(String.valueOf(totalPoints));
        if (labelChapSansQuiz   != null) labelChapSansQuiz.setText(String.valueOf(chapSansQuiz.size()));

        // ── Cards résumé par cours ────────────────────────────────────────────
        if (kpiContainer != null) {
            kpiContainer.getChildren().clear();
            HBox cardsRow = new HBox(16);
            cardsRow.setAlignment(Pos.CENTER_LEFT);
            for (QuizStatsService.CoursStatRow row : summary) {
                cardsRow.getChildren().add(buildCoursCard(row));
            }
            kpiContainer.getChildren().add(cardsRow);
        }

        // ── Tableau détaillé ──────────────────────────────────────────────────
        if (tableContainer != null) {
            tableContainer.getChildren().clear();
            tableContainer.getChildren().add(buildTable(details));
        }

        // ── Alertes chapitres sans quiz ───────────────────────────────────────
        if (alertContainer != null) {
            alertContainer.getChildren().clear();
            if (chapSansQuiz.isEmpty()) {
                Label ok = new Label("✅  Tous les chapitres ont au moins un quiz actif.");
                ok.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:700;");
                alertContainer.getChildren().add(ok);
            } else {
                Label titre = new Label("⚠  Chapitres sans quiz actif (" + chapSansQuiz.size() + ")");
                titre.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13; -fx-font-weight:800;");
                alertContainer.getChildren().add(titre);
                for (String[] row : chapSansQuiz) {
                    HBox line = new HBox(8);
                    line.setAlignment(Pos.CENTER_LEFT);
                    Label dot = new Label("•");
                    dot.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:14;");
                    Label txt = new Label(row[0] + "  →  Chapitre " + row[2] + " : " + row[1]
                        + (row[3].equals("0") ? "" : "  (" + row[3] + " quiz inactif(s))"));
                    txt.setStyle("-fx-text-fill:rgba(245,245,244,0.75); -fx-font-size:12;");
                    line.getChildren().addAll(dot, txt);
                    alertContainer.getChildren().add(line);
                }
            }
        }
    }

    // ── Builders UI ──────────────────────────────────────────────────────────

    private VBox buildCoursCard(QuizStatsService.CoursStatRow row) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18, 22, 18, 22));
        card.setMinWidth(200);
        card.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-background-radius:14;" +
            "-fx-border-color:#30363d; -fx-border-radius:14; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),12,0,0,4);");

        Label titre = new Label(row.coursTitre());
        titre.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13; -fx-font-weight:800;");
        titre.setWrapText(true); titre.setMaxWidth(180);

        Label niveau = new Label(row.coursNiveau());
        niveau.setStyle("-fx-background-color:rgba(124,58,237,0.2); -fx-text-fill:#a78bfa;" +
            "-fx-background-radius:999; -fx-padding:2 10 2 10; -fx-font-size:11; -fx-font-weight:700;");

        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            buildMiniStat(String.valueOf(row.nbQuizActifs()), "actifs", "#34d399"),
            buildMiniStat(String.valueOf(row.nbQuizTotal()), "total", "#8b949e"),
            buildMiniStat(String.valueOf(row.nbQuestionsTotal()), "questions", "#60a5fa")
        );

        Label pts = new Label("🏆 " + row.totalPointsMax() + " pts max");
        pts.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:11; -fx-font-weight:700;");

        card.getChildren().addAll(titre, niveau, stats, pts);
        return card;
    }

    private VBox buildMiniStat(String value, String label, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:" + color + "; -fx-font-size:18; -fx-font-weight:900;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:#484f58; -fx-font-size:10; -fx-font-weight:700;");
        box.getChildren().addAll(v, l);
        return box;
    }

    private VBox buildTable(List<QuizStatsService.QuizStatRow> rows) {
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color:#161b22; -fx-background-radius:14;" +
            "-fx-border-color:#30363d; -fx-border-radius:14; -fx-border-width:1;");

        // En-tête
        HBox header = buildTableRow(
            "Cours", "Chapitre", "Quiz", "État",
            "Questions", "Points", "Options", "Taux correct",
            true
        );
        table.getChildren().add(header);

        // Lignes
        for (int i = 0; i < rows.size(); i++) {
            QuizStatsService.QuizStatRow r = rows.get(i);
            String etatColor = switch (r.quizEtat()) {
                case "actif"    -> "#34d399";
                case "brouillon"-> "#fbbf24";
                case "archive"  -> "#6b7280";
                default         -> "#f87171";
            };
            HBox row = buildTableRow(
                r.coursTitre(),
                "Ch." + r.chapitreOrdre() + " " + r.chapitreTitre(),
                r.quizTitre(),
                r.quizEtat(),
                String.valueOf(r.nbQuestions()),
                String.valueOf(r.totalPoints()),
                String.valueOf(r.nbOptions()),
                r.tauxCouverture() + "%",
                false
            );
            // Couleur alternée
            if (i % 2 == 0) row.setStyle("-fx-background-color:rgba(255,255,255,0.02);");
            // Colorier la colonne état
            if (row.getChildren().size() > 3) {
                Label etatLbl = (Label) row.getChildren().get(3);
                etatLbl.setStyle("-fx-text-fill:" + etatColor + "; -fx-font-size:11;" +
                    "-fx-font-weight:700; -fx-background-color:" + etatColor + "22;" +
                    "-fx-background-radius:999; -fx-padding:2 8 2 8;");
            }
            table.getChildren().add(row);
        }

        if (rows.isEmpty()) {
            Label empty = new Label("Aucune donnée disponible.");
            empty.setStyle("-fx-text-fill:#484f58; -fx-font-size:13; -fx-padding:20;");
            table.getChildren().add(empty);
        }

        return table;
    }

    private HBox buildTableRow(String col1, String col2, String col3, String col4,
                                String col5, String col6, String col7, String col8,
                                boolean isHeader) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        String baseStyle = isHeader
            ? "-fx-text-fill:#8b949e; -fx-font-size:11; -fx-font-weight:700; -fx-padding:10 12 10 12;"
            : "-fx-text-fill:#e6edf3; -fx-font-size:12; -fx-padding:10 12 10 12;";

        double[] widths = {160, 140, 160, 80, 80, 70, 70, 90};
        String[] cols   = {col1, col2, col3, col4, col5, col6, col7, col8};

        for (int i = 0; i < cols.length; i++) {
            Label lbl = new Label(cols[i]);
            lbl.setStyle(baseStyle);
            lbl.setPrefWidth(widths[i]);
            lbl.setMinWidth(widths[i]);
            lbl.setMaxWidth(widths[i]);
            lbl.setWrapText(false);
            row.getChildren().add(lbl);
        }

        if (isHeader) {
            row.setStyle("-fx-background-color:#0d1117;" +
                "-fx-border-color:transparent transparent #30363d transparent; -fx-border-width:0 0 1 0;");
        }
        return row;
    }
}

package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import tn.esprit.services.QuizStatsService;

import java.util.List;

public class QuizStatsController {

    @FXML private VBox  kpiContainer;
    @FXML private VBox  alertContainer;
    @FXML private VBox  auditContainer;
    @FXML private Label labelTotalQuiz;
    @FXML private Label labelTotalQuestions;
    @FXML private Label labelTotalPoints;
    @FXML private Label labelChapSansQuiz;
    @FXML private javafx.scene.control.Button btnRetour;

    private final QuizStatsService statsService = new QuizStatsService();

    @FXML
    public void initialize() {
        // Effet hover sur le bouton retour
        if (btnRetour != null) {
            String normalStyle =
                "-fx-background-color:rgba(255,255,255,0.07);" +
                "-fx-border-color:rgba(255,255,255,0.15); -fx-border-width:1;" +
                "-fx-border-radius:10; -fx-background-radius:10;" +
                "-fx-text-fill:rgba(230,237,243,0.85); -fx-font-size:12;" +
                "-fx-font-weight:700; -fx-padding:8 16 8 16; -fx-cursor:hand;";
            String hoverStyle =
                "-fx-background-color:rgba(124,58,237,0.2);" +
                "-fx-border-color:#7c3aed66; -fx-border-width:1;" +
                "-fx-border-radius:10; -fx-background-radius:10;" +
                "-fx-text-fill:#a78bfa; -fx-font-size:12;" +
                "-fx-font-weight:700; -fx-padding:8 16 8 16; -fx-cursor:hand;";
            btnRetour.setOnMouseEntered(e -> btnRetour.setStyle(hoverStyle));
            btnRetour.setOnMouseExited(e  -> btnRetour.setStyle(normalStyle));
        }
        chargerStats();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void retourQuiz() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/index.fxml"));
            javafx.scene.Parent view = loader.load();
            // Remonter jusqu'au StackPane contentArea du layout backoffice
            javafx.scene.layout.StackPane contentArea =
                (javafx.scene.layout.StackPane) kpiContainer.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Chargement principal ──────────────────────────────────────────────────

    private void chargerStats() {
        List<QuizStatsService.QuizStatRow>  details      = statsService.getDetailedStats();
        List<QuizStatsService.CoursStatRow> summary      = statsService.getCoursSummary();
        List<String[]>                      chapSansQuiz = statsService.getChapitresSansQuizActif();
        List<QuizStatsService.AuditRow>     audit        = statsService.getAuditIntelligent();

        // ── KPI globaux ──
        int totalQuiz      = details.size();
        int totalQuestions = details.stream().mapToInt(QuizStatsService.QuizStatRow::nbQuestions).sum();
        int totalPoints    = details.stream().mapToInt(QuizStatsService.QuizStatRow::totalPoints).sum();

        if (labelTotalQuiz      != null) labelTotalQuiz.setText(String.valueOf(totalQuiz));
        if (labelTotalQuestions != null) labelTotalQuestions.setText(String.valueOf(totalQuestions));
        if (labelTotalPoints    != null) labelTotalPoints.setText(String.valueOf(totalPoints));
        if (labelChapSansQuiz   != null) labelChapSansQuiz.setText(String.valueOf(chapSansQuiz.size()));

        // ── Cards résumé par cours ──
        if (kpiContainer != null) {
            kpiContainer.getChildren().clear();
            HBox cardsRow = new HBox(16);
            cardsRow.setAlignment(Pos.TOP_LEFT);
            cardsRow.setStyle("-fx-padding:0;");
            for (QuizStatsService.CoursStatRow row : summary) {
                cardsRow.getChildren().add(buildCoursCard(row));
            }
            kpiContainer.getChildren().add(cardsRow);
        }

        // ── Alertes chapitres sans quiz ──
        if (alertContainer != null) {
            alertContainer.getChildren().clear();
            if (chapSansQuiz.isEmpty()) {
                HBox okBox = new HBox(10);
                okBox.setAlignment(Pos.CENTER_LEFT);
                okBox.setPadding(new Insets(10, 16, 10, 16));
                okBox.setStyle("-fx-background-color:rgba(52,211,153,0.08);" +
                    "-fx-background-radius:10; -fx-border-color:#34d39933;" +
                    "-fx-border-radius:10; -fx-border-width:1;");
                Label ok = new Label("✅  Tous les chapitres ont au moins un quiz actif.");
                ok.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:700;");
                okBox.getChildren().add(ok);
                alertContainer.getChildren().add(okBox);
            } else {
                // Barre de progression couverture
                int totalChap = chapSansQuiz.size();
                HBox progressHeader = new HBox(12);
                progressHeader.setAlignment(Pos.CENTER_LEFT);
                Label titre = new Label("⚠  Chapitres sans quiz actif (" + totalChap + ")");
                titre.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13; -fx-font-weight:800;");
                progressHeader.getChildren().add(titre);
                alertContainer.getChildren().add(progressHeader);

                for (String[] row : chapSansQuiz) {
                    HBox line = new HBox(10);
                    line.setAlignment(Pos.CENTER_LEFT);
                    line.setPadding(new Insets(7, 14, 7, 14));
                    line.setStyle("-fx-background-color:rgba(251,191,36,0.06);" +
                        "-fx-background-radius:8; -fx-border-color:#fbbf2422;" +
                        "-fx-border-radius:8; -fx-border-width:1;");

                    Label dot = new Label("▸");
                    dot.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13;");

                    Label cours = new Label(row[0]);
                    cours.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12; -fx-font-weight:700;");
                    cours.setMinWidth(200);

                    Label arrow = new Label("→");
                    arrow.setStyle("-fx-text-fill:#484f58; -fx-font-size:12;");

                    Label chap = new Label("Chapitre " + row[2] + " : " + row[1]);
                    chap.setStyle("-fx-text-fill:rgba(230,237,243,0.75); -fx-font-size:12;");

                    if (!row[3].equals("0")) {
                        Label badge = new Label(row[3] + " quiz inactif(s)");
                        badge.setStyle("-fx-background-color:rgba(251,191,36,0.15);" +
                            "-fx-text-fill:#fbbf24; -fx-font-size:10; -fx-font-weight:700;" +
                            "-fx-background-radius:999; -fx-padding:2 8 2 8;");
                        line.getChildren().addAll(dot, cours, arrow, chap, badge);
                    } else {
                        line.getChildren().addAll(dot, cours, arrow, chap);
                    }
                    alertContainer.getChildren().add(line);
                }
            }
        }

        // ── Audit intelligent ──
        if (auditContainer != null) {
            auditContainer.getChildren().clear();
            auditContainer.getChildren().add(buildAuditTable(audit));
        }
    }

    // ── Builders UI ───────────────────────────────────────────────────────────

    /**
     * Tableau d'audit avec barres de progression pour le taux de réussite.
     */
    private VBox buildAuditTable(List<QuizStatsService.AuditRow> rows) {
        VBox table = new VBox(0);
        table.setStyle(
            "-fx-background-color:#0d1117;" +
            "-fx-background-radius:16;" +
            "-fx-border-color:#21262d;" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),20,0,0,6);");

        // ── En-tête ──
        HBox header = new HBox(0);
        header.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-background-radius:16 16 0 0;" +
            "-fx-border-color:transparent transparent #21262d transparent;" +
            "-fx-border-width:0 0 1 0;" +
            "-fx-padding:0;");

        String[][] cols = {
            {"Cours",            "155"}, {"Chapitre",         "145"},
            {"Quiz",             "165"}, {"État",              "85"},
            {"Questions",         "95"}, {"Taux réussite",    "160"},
            {"Diagnostic",       "130"}, {"Action recommandée","230"}
        };
        for (String[] col : cols) {
            Label l = new Label(col[0]);
            l.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11; -fx-font-weight:700;" +
                "-fx-padding:12 12 12 14;");
            l.setPrefWidth(Double.parseDouble(col[1]));
            header.getChildren().add(l);
        }
        table.getChildren().add(header);

        // ── Lignes ──
        for (int i = 0; i < rows.size(); i++) {
            QuizStatsService.AuditRow r = rows.get(i);

            // Couleurs selon diagnostic
            String[] diagStyle = switch (r.diagnostic()) {
                case "QUIZ_VIDE"      -> new String[]{"#ef4444", "🔴", "rgba(239,68,68,0.07)"};
                case "SANS_OPTIONS"   -> new String[]{"#dc2626", "🔴", "rgba(220,38,38,0.07)"};
                case "TROP_FACILE"    -> new String[]{"#f59e0b", "🟡", "rgba(245,158,11,0.07)"};
                case "TROP_DIFFICILE" -> new String[]{"#ef4444", "🔴", "rgba(239,68,68,0.07)"};
                default               -> new String[]{"#34d399", "🟢", "rgba(52,211,153,0.04)"};
            };

            String etatColor = switch (r.quizEtat() != null ? r.quizEtat() : "") {
                case "actif"     -> "#34d399";
                case "brouillon" -> "#fbbf24";
                default          -> "#6b7280";
            };
            String etatBg = switch (r.quizEtat() != null ? r.quizEtat() : "") {
                case "actif"     -> "rgba(52,211,153,0.12)";
                case "brouillon" -> "rgba(251,191,36,0.12)";
                default          -> "rgba(107,114,128,0.12)";
            };

            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(46);

            // Alternance + couleur diagnostic
            String rowBg = (i % 2 == 0)
                ? diagStyle[2]
                : "rgba(255,255,255,0.01)";
            row.setStyle("-fx-background-color:" + rowBg + ";");

            // Cellule : Cours
            row.getChildren().add(buildCell(r.coursTitre(), 155, "#c9d1d9", false));
            // Cellule : Chapitre
            row.getChildren().add(buildCell(r.chapitreTitre(), 145, "#c9d1d9", false));
            // Cellule : Quiz
            row.getChildren().add(buildCell(r.quizTitre(), 165, "#e6edf3", true));

            // Cellule : État (badge coloré)
            HBox etatCell = new HBox();
            etatCell.setAlignment(Pos.CENTER_LEFT);
            etatCell.setPadding(new Insets(0, 12, 0, 14));
            etatCell.setPrefWidth(85);
            etatCell.setMinWidth(85);
            Label etatLbl = new Label(r.quizEtat() != null ? r.quizEtat() : "—");
            etatLbl.setStyle(
                "-fx-background-color:" + etatBg + ";" +
                "-fx-text-fill:" + etatColor + ";" +
                "-fx-font-size:10; -fx-font-weight:800;" +
                "-fx-background-radius:999; -fx-padding:3 9 3 9;");
            etatCell.getChildren().add(etatLbl);
            row.getChildren().add(etatCell);

            // Cellule : Questions (badge bleu)
            HBox qCell = new HBox();
            qCell.setAlignment(Pos.CENTER_LEFT);
            qCell.setPadding(new Insets(0, 12, 0, 14));
            qCell.setPrefWidth(95);
            qCell.setMinWidth(95);
            Label qLbl = new Label(String.valueOf(r.nbQuestions()));
            qLbl.setStyle(
                "-fx-background-color:rgba(96,165,250,0.12);" +
                "-fx-text-fill:#60a5fa;" +
                "-fx-font-size:12; -fx-font-weight:800;" +
                "-fx-background-radius:999; -fx-padding:3 10 3 10;");
            qCell.getChildren().add(qLbl);
            row.getChildren().add(qCell);

            // Cellule : Taux réussite avec ProgressBar
            HBox tauxCell = new HBox(8);
            tauxCell.setAlignment(Pos.CENTER_LEFT);
            tauxCell.setPadding(new Insets(0, 8, 0, 14));
            tauxCell.setPrefWidth(160);
            tauxCell.setMinWidth(160);

            double taux = r.tauxReussite();
            ProgressBar pb = new ProgressBar(taux / 100.0);
            pb.setPrefWidth(80);
            pb.setPrefHeight(8);
            String pbColor = taux >= 60 ? "#34d399" : taux >= 30 ? "#f59e0b" : "#ef4444";
            pb.setStyle(
                "-fx-accent:" + pbColor + ";" +
                "-fx-background-color:rgba(255,255,255,0.08);" +
                "-fx-background-radius:4; -fx-border-radius:4;");

            Label tauxLbl = new Label(String.format("%.0f%%", taux));
            tauxLbl.setStyle("-fx-text-fill:" + pbColor + "; -fx-font-size:11; -fx-font-weight:800;");
            tauxCell.getChildren().addAll(pb, tauxLbl);
            row.getChildren().add(tauxCell);

            // Cellule : Diagnostic (badge)
            HBox diagCell = new HBox();
            diagCell.setAlignment(Pos.CENTER_LEFT);
            diagCell.setPadding(new Insets(0, 12, 0, 14));
            diagCell.setPrefWidth(130);
            diagCell.setMinWidth(130);
            Label diagLbl = new Label(diagStyle[1] + " " + r.diagnostic());
            diagLbl.setStyle(
                "-fx-background-color:rgba(255,255,255,0.05);" +
                "-fx-text-fill:" + diagStyle[0] + ";" +
                "-fx-font-size:10; -fx-font-weight:700;" +
                "-fx-background-radius:6; -fx-padding:3 8 3 8;");
            diagLbl.setWrapText(false);
            diagCell.getChildren().add(diagLbl);
            row.getChildren().add(diagCell);

            // Cellule : Action recommandée
            row.getChildren().add(buildCell(r.action(), 230, "#8b949e", false));

            table.getChildren().add(row);
        }

        if (rows.isEmpty()) {
            Label empty = new Label("Aucun quiz à auditer.");
            empty.setStyle("-fx-text-fill:#484f58; -fx-font-size:13; -fx-padding:24;");
            table.getChildren().add(empty);
        }

        // Pied de tableau : légende
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(12, 20, 12, 20));
        legend.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-background-radius:0 0 16 16;" +
            "-fx-border-color:#21262d transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;");
        for (String[] leg : new String[][]{
            {"🔴", "QUIZ_VIDE / TROP_DIFFICILE", "#ef4444"},
            {"🟡", "TROP_FACILE",                "#f59e0b"},
            {"🟢", "NORMAL",                     "#34d399"}}) {
            HBox item = new HBox(5);
            item.setAlignment(Pos.CENTER_LEFT);
            Label icon = new Label(leg[0]);
            icon.setStyle("-fx-font-size:11;");
            Label txt = new Label(leg[1]);
            txt.setStyle("-fx-text-fill:" + leg[2] + "; -fx-font-size:10; -fx-font-weight:700;");
            item.getChildren().addAll(icon, txt);
            legend.getChildren().add(item);
        }
        table.getChildren().add(legend);

        return table;
    }

    /**
     * Card cours améliorée avec barre de progression quiz actifs/total.
     */
    private VBox buildCoursCard(QuizStatsService.CoursStatRow row) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMinWidth(220);
        card.setMaxWidth(280);

        // Couleur accent selon niveau
        String accentColor = switch (row.coursNiveau() != null ? row.coursNiveau().toLowerCase() : "") {
            case "avancé", "avance"     -> "#ef4444";
            case "intermédiaire", "intermediaire" -> "#f59e0b";
            default                     -> "#7c3aed";
        };
        String accentBg = switch (row.coursNiveau() != null ? row.coursNiveau().toLowerCase() : "") {
            case "avancé", "avance"     -> "rgba(239,68,68,0.08)";
            case "intermédiaire", "intermediaire" -> "rgba(245,158,11,0.08)";
            default                     -> "rgba(124,58,237,0.08)";
        };

        card.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-background-radius:16;" +
            "-fx-border-color:" + accentColor + "33;" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),16,0,0,5);");

        // Titre
        Label titre = new Label(row.coursTitre());
        titre.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13; -fx-font-weight:800;");
        titre.setWrapText(true);
        titre.setMaxWidth(230);

        // Badge niveau
        Label niveau = new Label(row.coursNiveau() != null ? row.coursNiveau() : "—");
        niveau.setStyle(
            "-fx-background-color:" + accentBg + ";" +
            "-fx-text-fill:" + accentColor + ";" +
            "-fx-background-radius:999; -fx-padding:3 12 3 12;" +
            "-fx-font-size:11; -fx-font-weight:700;");

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#21262d;");

        // Stats mini
        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            buildMiniStat(String.valueOf(row.nbQuizActifs()),     "actifs",    "#34d399"),
            buildMiniStat(String.valueOf(row.nbQuizTotal()),      "total",     "#8b949e"),
            buildMiniStat(String.valueOf(row.nbQuestionsTotal()), "questions", "#60a5fa")
        );

        // Barre de progression quiz actifs / total
        VBox progressSection = new VBox(5);
        int total  = row.nbQuizTotal();
        int actifs = row.nbQuizActifs();
        double ratio = total > 0 ? (double) actifs / total : 0.0;

        HBox progressHeader = new HBox();
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        Label progressLbl = new Label("Couverture active");
        progressLbl.setStyle("-fx-text-fill:#6e7681; -fx-font-size:10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ratioLbl = new Label(String.format("%.0f%%", ratio * 100));
        ratioLbl.setStyle("-fx-text-fill:#34d399; -fx-font-size:10; -fx-font-weight:800;");
        progressHeader.getChildren().addAll(progressLbl, spacer, ratioLbl);

        ProgressBar pb = new ProgressBar(ratio);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        String pbColor = ratio >= 0.7 ? "#34d399" : ratio >= 0.4 ? "#f59e0b" : "#ef4444";
        pb.setStyle(
            "-fx-accent:" + pbColor + ";" +
            "-fx-background-color:rgba(255,255,255,0.07);" +
            "-fx-background-radius:3; -fx-border-radius:3;");
        HBox.setHgrow(pb, Priority.ALWAYS);

        progressSection.getChildren().addAll(progressHeader, pb);

        // Points max
        HBox ptsBox = new HBox(6);
        ptsBox.setAlignment(Pos.CENTER_LEFT);
        Label ptsIcon = new Label("🏆");
        ptsIcon.setStyle("-fx-font-size:12;");
        Label pts = new Label(row.totalPointsMax() + " pts max");
        pts.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:12; -fx-font-weight:700;");
        ptsBox.getChildren().addAll(ptsIcon, pts);

        card.getChildren().addAll(titre, niveau, sep, stats, progressSection, ptsBox);
        return card;
    }

    private VBox buildMiniStat(String value, String label, String color) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:" + color + "; -fx-font-size:20; -fx-font-weight:900;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:#484f58; -fx-font-size:10; -fx-font-weight:700;");
        box.getChildren().addAll(v, l);
        return box;
    }

    /**
     * Cellule générique pour le tableau d'audit.
     */
    private HBox buildCell(String text, double width, String color, boolean bold) {
        HBox cell = new HBox();
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(0, 8, 0, 14));
        cell.setPrefWidth(width);
        cell.setMinWidth(width);
        cell.setMaxWidth(width);
        Label l = new Label(text != null ? text : "—");
        l.setStyle("-fx-text-fill:" + color + "; -fx-font-size:11;" +
            (bold ? " -fx-font-weight:700;" : ""));
        l.setWrapText(false);
        l.setMaxWidth(width - 22);
        cell.getChildren().add(l);
        return cell;
    }

    // ── Méthodes conservées (compatibilité) ───────────────────────────────────

    private HBox buildRecoCard(QuizStatsService.RecommandationRow r) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 16, 10, 16));

        String[] colors = switch (r.priorite()) {
            case "HAUTE"   -> new String[]{"rgba(220,38,38,0.1)",  "#dc2626", "🔴"};
            case "MOYENNE" -> new String[]{"rgba(217,119,6,0.1)",  "#d97706", "🟡"};
            default        -> new String[]{"rgba(5,150,105,0.1)",  "#34d399", "🟢"};
        };
        card.setStyle("-fx-background-color:" + colors[0] + "; -fx-background-radius:10;" +
            "-fx-border-color:" + colors[1] + "44; -fx-border-radius:10; -fx-border-width:1;");

        Label icon = new Label(colors[2]);
        icon.setStyle("-fx-font-size:16;");

        VBox text = new VBox(3);
        Label titre = new Label(r.coursTitre() + "  →  " + r.chapitreTitre()
            + ("—".equals(r.quizTitre()) ? "" : "  →  " + r.quizTitre()));
        titre.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12; -fx-font-weight:700;");
        Label msg = new Label(r.message());
        msg.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11;");
        text.getChildren().addAll(titre, msg);

        Label badge = new Label(r.priorite());
        badge.setStyle("-fx-background-color:" + colors[1] + "22; -fx-text-fill:" + colors[1] + ";" +
            "-fx-font-size:10; -fx-font-weight:800; -fx-background-radius:999; -fx-padding:2 8 2 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(icon, text, spacer, badge);
        return card;
    }
}

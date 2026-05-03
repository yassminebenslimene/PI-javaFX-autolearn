package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
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

        // ── KPI globaux avec animation ──
        int totalQuiz      = details.size();
        int totalQuestions = details.stream().mapToInt(QuizStatsService.QuizStatRow::nbQuestions).sum();
        int totalPoints    = details.stream().mapToInt(QuizStatsService.QuizStatRow::totalPoints).sum();

        animateCount(labelTotalQuiz,      totalQuiz);
        animateCount(labelTotalQuestions, totalQuestions);
        animateCount(labelTotalPoints,    totalPoints);
        animateCount(labelChapSansQuiz,   chapSansQuiz.size());

        // ── Cards résumé par cours ──
        if (kpiContainer != null) {
            kpiContainer.getChildren().clear();
            kpiContainer.getChildren().add(buildGlobalStatsBar(audit));
            HBox cardsRow = new HBox(16);
            cardsRow.setAlignment(Pos.TOP_LEFT);
            cardsRow.setPadding(new Insets(4, 0, 0, 0));
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
                okBox.setPadding(new Insets(12, 18, 12, 18));
                okBox.setStyle("-fx-background-color:rgba(52,211,153,0.08);" +
                    "-fx-background-radius:10; -fx-border-color:#34d39933;" +
                    "-fx-border-radius:10; -fx-border-width:1;");
                Label ok = new Label("✅  Tous les chapitres ont au moins un quiz actif — couverture complète !");
                ok.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:700;");
                okBox.getChildren().add(ok);
                alertContainer.getChildren().add(okBox);
            } else {
                Label titre = new Label("⚠  Chapitres sans quiz actif (" + chapSansQuiz.size() + ")");
                titre.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13; -fx-font-weight:800;");
                alertContainer.getChildren().add(titre);
                for (String[] row : chapSansQuiz) {
                    HBox line = new HBox(10);
                    line.setAlignment(Pos.CENTER_LEFT);
                    line.setPadding(new Insets(8, 16, 8, 16));
                    line.setStyle("-fx-background-color:rgba(251,191,36,0.05);" +
                        "-fx-background-radius:8; -fx-border-color:#fbbf2420;" +
                        "-fx-border-radius:8; -fx-border-width:1;");
                    Label dot   = new Label("▸");
                    dot.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13;");
                    Label cours = new Label(row[0]);
                    cours.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:12; -fx-font-weight:700;");
                    cours.setMinWidth(200);
                    Label arrow = new Label("→");
                    arrow.setStyle("-fx-text-fill:#484f58; -fx-font-size:12;");
                    Label chap  = new Label("Chapitre " + row[2] + " : " + row[1]);
                    chap.setStyle("-fx-text-fill:rgba(230,237,243,0.75); -fx-font-size:12;");
                    line.getChildren().addAll(dot, cours, arrow, chap);
                    if (!row[3].equals("0")) {
                        Label badge = new Label(row[3] + " quiz inactif(s)");
                        badge.setStyle("-fx-background-color:rgba(251,191,36,0.15);" +
                            "-fx-text-fill:#fbbf24; -fx-font-size:10; -fx-font-weight:700;" +
                            "-fx-background-radius:999; -fx-padding:2 8 2 8;");
                        line.getChildren().add(badge);
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

    // ── Animation compteur KPI ────────────────────────────────────────────────

    private void animateCount(Label label, int target) {
        if (label == null) return;
        if (target == 0) { label.setText("0"); return; }
        int[] current = {0};
        int steps = 25;
        double inc = Math.max(1, target / (double) steps);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(22), e -> {
            current[0] = Math.min(target, (int)(current[0] + inc));
            label.setText(String.valueOf(current[0]));
        }));
        tl.setCycleCount(steps);
        tl.play();
    }

    // ── Barre de statistiques globales ────────────────────────────────────────

    private HBox buildGlobalStatsBar(List<QuizStatsService.AuditRow> audit) {
        long nbNormal    = audit.stream().filter(r -> "NORMAL".equals(r.diagnostic())).count();
        long nbVides     = audit.stream().filter(r -> "QUIZ_VIDE".equals(r.diagnostic()) || "SANS_OPTIONS".equals(r.diagnostic())).count();
        long nbFacile    = audit.stream().filter(r -> "TROP_FACILE".equals(r.diagnostic())).count();
        long nbDifficile = audit.stream().filter(r -> "TROP_DIFFICILE".equals(r.diagnostic())).count();

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle("-fx-background-color:#161b22; -fx-background-radius:12;" +
            "-fx-border-color:#21262d; -fx-border-radius:12; -fx-border-width:1;");

        Label title = new Label("Vue d'ensemble :");
        title.setStyle("-fx-text-fill:#6e7681; -fx-font-size:11; -fx-font-weight:700;");
        bar.getChildren().add(title);

        for (Object[] stat : new Object[][]{
            {nbNormal,    "Équilibrés",  "#34d399", "rgba(52,211,153,0.12)"},
            {nbFacile,    "Trop faciles","#f59e0b", "rgba(245,158,11,0.12)"},
            {nbDifficile, "Trop durs",   "#ef4444", "rgba(239,68,68,0.12)"},
            {nbVides,     "Vides",       "#dc2626", "rgba(220,38,38,0.12)"}
        }) {
            HBox chip = new HBox(6);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setPadding(new Insets(5, 12, 5, 12));
            chip.setStyle("-fx-background-color:" + stat[3] + "; -fx-background-radius:999;" +
                "-fx-border-color:" + stat[2] + "33; -fx-border-radius:999; -fx-border-width:1;");
            Label val = new Label(String.valueOf(stat[0]));
            val.setStyle("-fx-text-fill:" + stat[2] + "; -fx-font-size:14; -fx-font-weight:900;");
            Label lbl = new Label((String) stat[1]);
            lbl.setStyle("-fx-text-fill:" + stat[2] + "; -fx-font-size:11;");
            chip.getChildren().addAll(val, lbl);
            bar.getChildren().add(chip);
        }

        if (!audit.isEmpty()) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bar.getChildren().add(spacer);
            double pct = nbNormal * 100.0 / audit.size();
            VBox pbBox = new VBox(3);
            pbBox.setAlignment(Pos.CENTER_RIGHT);
            Label pctLbl = new Label(String.format("%.0f%% équilibrés", pct));
            pctLbl.setStyle("-fx-text-fill:#6e7681; -fx-font-size:10;");
            ProgressBar globalPb = new ProgressBar(pct / 100.0);
            globalPb.setPrefWidth(130);
            globalPb.setPrefHeight(7);
            String pbCol = pct >= 70 ? "#34d399" : pct >= 40 ? "#f59e0b" : "#ef4444";
            globalPb.setStyle("-fx-accent:" + pbCol + "; -fx-background-color:rgba(255,255,255,0.07); -fx-background-radius:3;");
            pbBox.getChildren().addAll(pctLbl, globalPb);
            bar.getChildren().add(pbBox);
        }
        return bar;
    }

    // ── Builders UI ───────────────────────────────────────────────────────────

    /**
     * Tableau d'audit avec barres de progression, hover, tooltips et légende.
     */
    private VBox buildAuditTable(List<QuizStatsService.AuditRow> rows) {
        VBox wrapper = new VBox(0);

        // Compteur
        HBox countBar = new HBox(8);
        countBar.setAlignment(Pos.CENTER_LEFT);
        countBar.setPadding(new Insets(0, 0, 10, 0));
        Label countLbl = new Label(rows.size() + " quiz analysés");
        countLbl.setStyle("-fx-text-fill:#6e7681; -fx-font-size:11;");
        countBar.getChildren().add(countLbl);
        wrapper.getChildren().add(countBar);

        VBox table = new VBox(0);
        table.setStyle(
            "-fx-background-color:#0d1117;" +
            "-fx-background-radius:16;" +
            "-fx-border-color:#21262d;" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),24,0,0,8);");

        // En-tête
        HBox header = new HBox(0);
        header.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-background-radius:16 16 0 0;" +
            "-fx-border-color:transparent transparent #21262d transparent;" +
            "-fx-border-width:0 0 1 0;");
        String[][] cols = {
            {"Cours","130"},{"Chapitre","120"},{"Quiz","145"},
            {"État","75"},{"Questions","80"},{"Taux réussite","145"},
            {"Diagnostic","120"},{"Action recommandée","185"}
        };
        for (String[] col : cols) {
            Label l = new Label(col[0]);
            l.setStyle("-fx-text-fill:#8b949e; -fx-font-size:11; -fx-font-weight:700; -fx-padding:13 12 13 14;");
            l.setPrefWidth(Double.parseDouble(col[1]));
            header.getChildren().add(l);
        }
        table.getChildren().add(header);

        // Lignes
        for (int i = 0; i < rows.size(); i++) {
            QuizStatsService.AuditRow r = rows.get(i);

            String[] ds = switch (r.diagnostic()) {
                case "QUIZ_VIDE"      -> new String[]{"#ef4444","🔴","rgba(239,68,68,0.06)"};
                case "SANS_OPTIONS"   -> new String[]{"#dc2626","🔴","rgba(220,38,38,0.06)"};
                case "TROP_FACILE"    -> new String[]{"#f59e0b","🟡","rgba(245,158,11,0.06)"};
                case "TROP_DIFFICILE" -> new String[]{"#ef4444","🔴","rgba(239,68,68,0.06)"};
                default               -> new String[]{"#34d399","🟢","rgba(52,211,153,0.03)"};
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

            final String normalBg = i % 2 == 0 ? ds[2] : "rgba(255,255,255,0.008)";
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMinHeight(50);
            row.setStyle("-fx-background-color:" + normalBg + ";");
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:rgba(124,58,237,0.08); -fx-cursor:hand;"));
            row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:" + normalBg + ";"));

            row.getChildren().add(buildCell(r.coursTitre(),    130, "#c9d1d9", false));
            row.getChildren().add(buildCell(r.chapitreTitre(), 120, "#8b949e", false));
            row.getChildren().add(buildCell(r.quizTitre(),     145, "#e6edf3", true));

            // État badge
            HBox etatCell = new HBox();
            etatCell.setAlignment(Pos.CENTER_LEFT);
            etatCell.setPadding(new Insets(0, 8, 0, 14));
            etatCell.setPrefWidth(75); etatCell.setMinWidth(75);
            Label etatLbl = new Label(r.quizEtat() != null ? r.quizEtat() : "—");
            etatLbl.setStyle("-fx-background-color:" + etatBg + "; -fx-text-fill:" + etatColor + ";" +
                "-fx-font-size:10; -fx-font-weight:800; -fx-background-radius:999; -fx-padding:3 9 3 9;");
            etatCell.getChildren().add(etatLbl);
            row.getChildren().add(etatCell);

            // Questions badge
            HBox qCell = new HBox();
            qCell.setAlignment(Pos.CENTER_LEFT);
            qCell.setPadding(new Insets(0, 8, 0, 14));
            qCell.setPrefWidth(80); qCell.setMinWidth(80);
            Label qLbl = new Label(String.valueOf(r.nbQuestions()));
            qLbl.setStyle("-fx-background-color:rgba(96,165,250,0.12); -fx-text-fill:#60a5fa;" +
                "-fx-font-size:12; -fx-font-weight:800; -fx-background-radius:999; -fx-padding:3 10 3 10;");
            qCell.getChildren().add(qLbl);
            row.getChildren().add(qCell);

            // Taux réussite avec ProgressBar + tooltip
            double taux = r.tauxReussite();
            String pbColor = taux >= 60 ? "#34d399" : taux >= 30 ? "#f59e0b" : "#ef4444";
            HBox tauxCell = new HBox(8);
            tauxCell.setAlignment(Pos.CENTER_LEFT);
            tauxCell.setPadding(new Insets(0, 8, 0, 14));
            tauxCell.setPrefWidth(145); tauxCell.setMinWidth(145);
            ProgressBar pb = new ProgressBar(taux / 100.0);
            pb.setPrefWidth(70); pb.setPrefHeight(8);
            pb.setStyle("-fx-accent:" + pbColor + "; -fx-background-color:rgba(255,255,255,0.08); -fx-background-radius:4;");
            Tooltip.install(pb, new Tooltip(String.format("%.1f%% d'options correctes", taux)));
            Label tauxLbl = new Label(String.format("%.0f%%", taux));
            tauxLbl.setStyle("-fx-text-fill:" + pbColor + "; -fx-font-size:11; -fx-font-weight:800;");
            tauxCell.getChildren().addAll(pb, tauxLbl);
            row.getChildren().add(tauxCell);

            // Diagnostic badge
            HBox diagCell = new HBox();
            diagCell.setAlignment(Pos.CENTER_LEFT);
            diagCell.setPadding(new Insets(0, 8, 0, 14));
            diagCell.setPrefWidth(120); diagCell.setMinWidth(120);
            Label diagLbl = new Label(ds[1] + " " + r.diagnostic());
            diagLbl.setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-text-fill:" + ds[0] + ";" +
                "-fx-font-size:10; -fx-font-weight:700; -fx-background-radius:6; -fx-padding:3 8 3 8;");
            diagLbl.setWrapText(false);
            diagCell.getChildren().add(diagLbl);
            row.getChildren().add(diagCell);

            row.getChildren().add(buildCell(r.action(), 185, "#8b949e", false));
            table.getChildren().add(row);
        }

        if (rows.isEmpty()) {
            Label empty = new Label("Aucun quiz à auditer.");
            empty.setStyle("-fx-text-fill:#484f58; -fx-font-size:13; -fx-padding:28;");
            table.getChildren().add(empty);
        }

        // Pied : légende
        HBox legend = new HBox(24);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(12, 20, 12, 20));
        legend.setStyle("-fx-background-color:#161b22; -fx-background-radius:0 0 16 16;" +
            "-fx-border-color:#21262d transparent transparent transparent; -fx-border-width:1 0 0 0;");
        Label legendTitle = new Label("Légende :");
        legendTitle.setStyle("-fx-text-fill:#484f58; -fx-font-size:10; -fx-font-weight:700;");
        legend.getChildren().add(legendTitle);
        for (String[] leg : new String[][]{
            {"🔴","QUIZ_VIDE / TROP_DIFFICILE","#ef4444"},
            {"🟡","TROP_FACILE","#f59e0b"},
            {"🟢","NORMAL — bien équilibré","#34d399"}}) {
            HBox item = new HBox(5);
            item.setAlignment(Pos.CENTER_LEFT);
            Label ic = new Label(leg[0]);
            ic.setStyle("-fx-font-size:11;");
            Label tx = new Label(leg[1]);
            tx.setStyle("-fx-text-fill:" + leg[2] + "; -fx-font-size:10; -fx-font-weight:700;");
            item.getChildren().addAll(ic, tx);
            legend.getChildren().add(item);
        }
        table.getChildren().add(legend);
        wrapper.getChildren().add(table);

        // Espace en bas pour éviter que la légende soit cachée par la barre des tâches
        Region bottomSpacer = new Region();
        bottomSpacer.setPrefHeight(32);
        wrapper.getChildren().add(bottomSpacer);

        return wrapper;
    }

    /**
     * Card cours avec hover, barre de progression et tooltip.
     */
    private VBox buildCoursCard(QuizStatsService.CoursStatRow row) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setMinWidth(220);
        card.setMaxWidth(280);

        String accentColor = switch (row.coursNiveau() != null ? row.coursNiveau().toLowerCase() : "") {
            case "avancé","avance"               -> "#ef4444";
            case "intermédiaire","intermediaire" -> "#f59e0b";
            default                              -> "#7c3aed";
        };
        String accentBg = switch (row.coursNiveau() != null ? row.coursNiveau().toLowerCase() : "") {
            case "avancé","avance"               -> "rgba(239,68,68,0.08)";
            case "intermédiaire","intermediaire" -> "rgba(245,158,11,0.08)";
            default                              -> "rgba(124,58,237,0.08)";
        };

        String baseStyle =
            "-fx-background-color:#161b22; -fx-background-radius:16;" +
            "-fx-border-color:" + accentColor + "33;" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),16,0,0,5);";
        String hoverStyle =
            "-fx-background-color:#1c2128; -fx-background-radius:16;" +
            "-fx-border-color:" + accentColor + "66;" +
            "-fx-border-radius:16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),22,0,0,7); -fx-cursor:hand;";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        Label titre = new Label(row.coursTitre());
        titre.setStyle("-fx-text-fill:#e6edf3; -fx-font-size:13; -fx-font-weight:800;");
        titre.setWrapText(true);
        titre.setMaxWidth(230);

        Label niveau = new Label(row.coursNiveau() != null ? row.coursNiveau() : "—");
        niveau.setStyle("-fx-background-color:" + accentBg + "; -fx-text-fill:" + accentColor + ";" +
            "-fx-background-radius:999; -fx-padding:3 12 3 12; -fx-font-size:11; -fx-font-weight:700;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#21262d;");

        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            buildMiniStat(String.valueOf(row.nbQuizActifs()),     "actifs",    "#34d399"),
            buildMiniStat(String.valueOf(row.nbQuizTotal()),      "total",     "#8b949e"),
            buildMiniStat(String.valueOf(row.nbQuestionsTotal()), "questions", "#60a5fa")
        );

        int total  = row.nbQuizTotal();
        int actifs = row.nbQuizActifs();
        double ratio = total > 0 ? (double) actifs / total : 0.0;
        String pbColor = ratio >= 0.7 ? "#34d399" : ratio >= 0.4 ? "#f59e0b" : "#ef4444";

        HBox phdr = new HBox();
        phdr.setAlignment(Pos.CENTER_LEFT);
        Label pLbl = new Label("Couverture active");
        pLbl.setStyle("-fx-text-fill:#6e7681; -fx-font-size:10;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label rLbl = new Label(String.format("%.0f%%", ratio * 100));
        rLbl.setStyle("-fx-text-fill:" + pbColor + "; -fx-font-size:10; -fx-font-weight:800;");
        phdr.getChildren().addAll(pLbl, sp, rLbl);

        ProgressBar pb = new ProgressBar(ratio);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent:" + pbColor + "; -fx-background-color:rgba(255,255,255,0.07); -fx-background-radius:3;");
        Tooltip.install(pb, new Tooltip(actifs + " actifs sur " + total + " quiz"));

        VBox progressSection = new VBox(5);
        progressSection.getChildren().addAll(phdr, pb);

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
        if (text != null && text.length() > 22) Tooltip.install(l, new Tooltip(text));
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

package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.SessionManager;

import java.util.*;

/**
 * TodoController — Kanban 3 colonnes.
 *
 * Logique métier :
 *   - Cours TO DO      : 0 chapitre complété (aucun quiz réussi)
 *   - Cours IN PROGRESS: 1 à N-1 chapitres complétés
 *   - Cours DONE       : tous les chapitres complétés
 *
 * Dans chaque carte cours :
 *   - Chapitres complétés  → affichés avec ✓ vert
 *   - Chapitres non faits  → affichés avec ○ gris
 */
public class TodoController {

    @FXML private Label labelProgGlobale;
    @FXML private Label labelDoneCount;
    @FXML private Label labelInProgressCount;
    @FXML private Label labelTodoCount;
    @FXML private Label badgeTodo;
    @FXML private Label badgeInProgress;
    @FXML private Label badgeDone;
    @FXML private VBox  todoContainer;
    @FXML private VBox  inProgressContainer;
    @FXML private VBox  doneContainer;

    private final ServiceCours          serviceCours    = new ServiceCours();
    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService = new CourseProgressService();

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null) return;
        Thread t = new Thread(this::loadData);
        t.setDaemon(true);
        t.start();
    }

    private void loadData() {
        int userId = SessionManager.getCurrentUser().getId();
        List<Cours> allCours = serviceCours.consulter();

        List<VBox> todoCards       = new ArrayList<>();
        List<VBox> inProgressCards = new ArrayList<>();
        List<VBox> doneCards       = new ArrayList<>();
        int todoCount = 0, inProgressCount = 0, doneCount = 0, totalProgress = 0;

        for (Cours cours : allCours) {
            List<Chapitre> chapitres = serviceChapitre.consulterParCoursId(cours.getId());
            Set<Integer> completedIds = new HashSet<>(
                progressService.getCompletedChapitreIds(userId, cours.getId()));

            int total     = chapitres.size();
            int completed = (int) chapitres.stream().filter(ch -> completedIds.contains(ch.getId())).count();
            int progress  = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
            totalProgress += progress;

            VBox card = buildCoursCard(cours, chapitres, completedIds, progress, completed, total);

            if (completed == 0)          { todoCards.add(card);       todoCount++; }
            else if (completed >= total && total > 0) { doneCards.add(card); doneCount++; }
            else                         { inProgressCards.add(card); inProgressCount++; }
        }

        final int fTodo = todoCount, fInProgress = inProgressCount, fDone = doneCount;
        final int fGlobal = allCours.isEmpty() ? 0 : totalProgress / allCours.size();
        final List<VBox> fTodo2 = todoCards, fInProg2 = inProgressCards, fDone2 = doneCards;

        javafx.application.Platform.runLater(() -> {
            todoContainer.getChildren().clear();
            inProgressContainer.getChildren().clear();
            doneContainer.getChildren().clear();

            fTodo2.forEach(c -> todoContainer.getChildren().add(c));
            fInProg2.forEach(c -> inProgressContainer.getChildren().add(c));
            fDone2.forEach(c -> doneContainer.getChildren().add(c));

            if (labelProgGlobale    != null) labelProgGlobale.setText(fGlobal + "%");
            if (labelDoneCount      != null) labelDoneCount.setText(String.valueOf(fDone));
            if (labelInProgressCount!= null) labelInProgressCount.setText(String.valueOf(fInProgress));
            if (labelTodoCount      != null) labelTodoCount.setText(String.valueOf(fTodo));
            if (badgeTodo           != null) badgeTodo.setText(String.valueOf(fTodo));
            if (badgeInProgress     != null) badgeInProgress.setText(String.valueOf(fInProgress));
            if (badgeDone           != null) badgeDone.setText(String.valueOf(fDone));

            if (fTodo == 0)       addEmpty(todoContainer,       "Aucun cours à commencer");
            if (fInProgress == 0) addEmpty(inProgressContainer, "Aucun cours en cours");
            if (fDone == 0)       addEmpty(doneContainer,       "Aucun cours terminé");
        });
    }

    private VBox buildCoursCard(Cours cours, List<Chapitre> chapitres,
                                Set<Integer> completedIds, int progress,
                                int completed, int total) {
        // Couleurs selon statut
        String accent, bgHeader, statusText, progressColor;
        if (completed >= total && total > 0) {
            accent = "#059669"; bgHeader = "#ecfdf5";
            statusText = "✓  Terminé — 100%"; progressColor = "#059669";
        } else if (completed > 0) {
            accent = "#f59e0b"; bgHeader = "#fffbeb";
            statusText = "◑  En cours — " + completed + "/" + total + " chapitres";
            progressColor = "#f59e0b";
        } else {
            accent = "#94a3b8"; bgHeader = "#f8fafc";
            statusText = "○  À faire — " + total + " chapitres";
            progressColor = "#94a3b8";
        }

        // ── Header de la carte ──────────────────────────────────────────────
        VBox header = new VBox(6);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle("-fx-background-color:" + bgHeader + "; -fx-background-radius:14 14 0 0;");

        // Titre + badge niveau
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(cours.getTitre());
        titleLabel.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        String niveauColor = switch (cours.getNiveau() == null ? "" : cours.getNiveau().toUpperCase()) {
            case "AVANCE", "AVANCÉ" -> "#e94560";
            case "INTERMEDIAIRE", "INTERMÉDIAIRE" -> "#f59e0b";
            default -> "#059669";
        };
        Label niveauBadge = new Label(cours.getNiveau() != null ? cours.getNiveau() : "");
        niveauBadge.setStyle("-fx-font-size:9; -fx-font-weight:700; -fx-text-fill:" + niveauColor
            + "; -fx-background-color:white; -fx-background-radius:20; -fx-padding:2 8 2 8;");
        titleRow.getChildren().addAll(titleLabel, niveauBadge);

        // Statut
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:" + accent + ";");

        // Barre de progression
        HBox barBg = new HBox();
        barBg.setPrefHeight(6);
        barBg.setStyle("-fx-background-color:#e2e8f0; -fx-background-radius:3;");
        Region fill = new Region();
        fill.setPrefHeight(6);
        fill.setPrefWidth(Math.min(progress, 100) / 100.0 * 240);
        fill.setStyle("-fx-background-color:" + progressColor + "; -fx-background-radius:3;");
        barBg.getChildren().add(fill);

        header.getChildren().addAll(titleRow, statusLabel, barBg);

        // ── Liste des chapitres ─────────────────────────────────────────────
        VBox chapBox = new VBox(0);
        chapBox.setPadding(new Insets(8, 14, 12, 14));

        for (Chapitre ch : chapitres) {
            boolean done = completedIds.contains(ch.getId());
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 6, 5, 6));
            row.setStyle("-fx-background-radius:8;");

            // Icône statut
            Label icon = new Label(done ? "✓" : "○");
            icon.setStyle("-fx-font-size:11; -fx-min-width:16; -fx-text-fill:"
                + (done ? "#059669" : "#94a3b8") + ";");

            // Titre chapitre
            Label chapTitle = new Label("Chapitre " + ch.getOrdre() + " — " + ch.getTitre());
            chapTitle.setStyle("-fx-font-size:11; -fx-text-fill:" + (done ? "#059669" : "#64748b") + ";"
                + (done ? "-fx-font-weight:600;" : ""));
            chapTitle.setWrapText(true);
            HBox.setHgrow(chapTitle, Priority.ALWAYS);

            // Badge statut chapitre
            Label chapBadge = new Label(done ? "Fait" : "À faire");
            chapBadge.setStyle("-fx-font-size:9; -fx-font-weight:700; -fx-padding:2 6 2 6;"
                + "-fx-background-radius:20; -fx-text-fill:" + (done ? "#059669" : "#94a3b8")
                + "; -fx-background-color:" + (done ? "rgba(5,150,105,0.1)" : "rgba(148,163,184,0.1)") + ";");

            row.getChildren().addAll(icon, chapTitle, chapBadge);

            // Hover sur la ligne
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-radius:8;"));

            chapBox.getChildren().add(row);

            // Séparateur léger entre chapitres
            if (chapitres.indexOf(ch) < chapitres.size() - 1) {
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color:#f1f5f9;");
                chapBox.getChildren().add(sep);
            }
        }

        if (chapitres.isEmpty()) {
            Label noChap = new Label("Aucun chapitre");
            noChap.setStyle("-fx-font-size:11; -fx-text-fill:#cbd5e1; -fx-padding:4 0 0 0;");
            chapBox.getChildren().add(noChap);
        }

        // ── Assembler la carte ──────────────────────────────────────────────
        VBox card = new VBox(0, header, chapBox);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
            + "-fx-border-color:#e2e8f0; -fx-border-radius:14;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
            + "-fx-border-color:" + accent + "; -fx-border-radius:14;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.13),14,0,0,5);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
            + "-fx-border-color:#e2e8f0; -fx-border-radius:14;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);"));

        return card;
    }

    private void addEmpty(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#cbd5e1; -fx-font-size:12; -fx-padding:12 0 0 4;");
        container.getChildren().add(lbl);
    }
}

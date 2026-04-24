package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.SessionManager;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * TodoController — page "Ma Liste d'Apprentissage" (Kanban 3 colonnes).
 *
 * Logique de statut d'un cours :
 *   - TO DO      : progression = 0%  (aucun chapitre commencé)
 *   - IN PROGRESS: progression 1-99% (au moins un chapitre complété)
 *   - DONE       : progression = 100% (tous les chapitres complétés)
 *
 * Chaque carte cours affiche ses chapitres avec ✓ (complété) ou ○ (à faire).
 */
public class TodoController {

    @FXML private Label labelProgGlobale;
    @FXML private Label labelDoneCount;
    @FXML private Label labelInProgressCount;
    @FXML private Label labelTodoCount;
    @FXML private Label badgeTodo;
    @FXML private Label badgeInProgress;
    @FXML private Label badgeDone;
    @FXML private VBox todoContainer;
    @FXML private VBox inProgressContainer;
    @FXML private VBox doneContainer;

    private final ServiceCours          serviceCours    = new ServiceCours();
    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService = new CourseProgressService();

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null) return;
        loadData();
    }

    private void loadData() {
        int userId = SessionManager.getCurrentUser().getId();
        List<Cours> allCours = serviceCours.consulter();

        todoContainer.getChildren().clear();
        inProgressContainer.getChildren().clear();
        doneContainer.getChildren().clear();

        int todoCount = 0, inProgressCount = 0, doneCount = 0;
        int totalProgress = 0;

        for (Cours cours : allCours) {
            int progress = progressService.getCourseProgress(userId, cours.getId());
            totalProgress += progress;

            VBox card = buildCoursCard(cours, progress, userId);

            if (progress == 0) {
                todoContainer.getChildren().add(card);
                todoCount++;
            } else if (progress >= 100) {
                doneContainer.getChildren().add(card);
                doneCount++;
            } else {
                inProgressContainer.getChildren().add(card);
                inProgressCount++;
            }
        }

        // Stats globales
        int globalProgress = allCours.isEmpty() ? 0 : totalProgress / allCours.size();
        if (labelProgGlobale    != null) labelProgGlobale.setText(globalProgress + "%");
        if (labelDoneCount      != null) labelDoneCount.setText(String.valueOf(doneCount));
        if (labelInProgressCount!= null) labelInProgressCount.setText(String.valueOf(inProgressCount));
        if (labelTodoCount      != null) labelTodoCount.setText(String.valueOf(todoCount));
        if (badgeTodo           != null) badgeTodo.setText(String.valueOf(todoCount));
        if (badgeInProgress     != null) badgeInProgress.setText(String.valueOf(inProgressCount));
        if (badgeDone           != null) badgeDone.setText(String.valueOf(doneCount));

        // Message vide si colonne vide
        if (todoCount == 0)       addEmptyMessage(todoContainer, "Aucun cours à commencer");
        if (inProgressCount == 0) addEmptyMessage(inProgressContainer, "Aucun cours en cours");
        if (doneCount == 0)       addEmptyMessage(doneContainer, "Aucun cours terminé");
    }

    /**
     * Construit une carte cours avec :
     * - Header coloré selon le statut
     * - Barre de progression
     * - Liste des chapitres avec ✓ ou ○
     */
    private VBox buildCoursCard(Cours cours, int progress, int userId) {
        // Couleur selon statut
        String accentColor, bgColor, textColor;
        String statusLabel;
        if (progress >= 100) {
            accentColor = "#059669"; bgColor = "#ecfdf5"; textColor = "#065f46";
            statusLabel = "✓  Terminé";
        } else if (progress > 0) {
            accentColor = "#f59e0b"; bgColor = "#fffbeb"; textColor = "#92400e";
            statusLabel = "◑  En cours — " + progress + "%";
        } else {
            accentColor = "#94a3b8"; bgColor = "#f8fafc"; textColor = "#475569";
            statusLabel = "○  À faire";
        }

        // Header de la carte
        VBox header = new VBox(4);
        header.setPadding(new Insets(12, 14, 10, 14));
        header.setStyle("-fx-background-color:" + bgColor + "; -fx-background-radius:12 12 0 0;");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(cours.getTitre());
        titleLabel.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        titleLabel.setWrapText(true);
        Label statusBadge = new Label(statusLabel);
        statusBadge.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:" + accentColor + ";");
        titleRow.getChildren().addAll(titleLabel);
        header.getChildren().addAll(titleRow, statusBadge);

        // Barre de progression
        HBox progressBar = new HBox();
        progressBar.setPrefHeight(5);
        progressBar.setStyle("-fx-background-color:#e2e8f0; -fx-background-radius:3;");
        Region fill = new Region();
        fill.setPrefHeight(5);
        double ratio = Math.min(progress, 100) / 100.0;
        fill.setPrefWidth(ratio * 220);
        fill.setStyle("-fx-background-color:" + accentColor + "; -fx-background-radius:3;");
        progressBar.getChildren().add(fill);
        header.getChildren().add(progressBar);

        // Chapitres
        VBox chapitresBox = new VBox(6);
        chapitresBox.setPadding(new Insets(10, 14, 12, 14));

        List<Chapitre> chapitres = serviceChapitre.consulterParCoursId(cours.getId());
        Set<Integer> completedIds = new HashSet<>(
            progressService.getCompletedChapitreIds(userId, cours.getId()));

        for (Chapitre ch : chapitres) {
            boolean done = completedIds.contains(ch.getId());
            HBox chapRow = new HBox(8);
            chapRow.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label(done ? "✓" : "○");
            icon.setStyle("-fx-font-size:12; -fx-text-fill:" + (done ? "#059669" : "#94a3b8") + "; -fx-min-width:16;");

            Label chapTitle = new Label(ch.getTitre());
            chapTitle.setStyle("-fx-font-size:11; -fx-text-fill:" + (done ? "#059669" : "#64748b") + ";"
                + (done ? "-fx-strikethrough:false;" : ""));
            chapTitle.setWrapText(true);

            chapRow.getChildren().addAll(icon, chapTitle);
            chapitresBox.getChildren().add(chapRow);
        }

        if (chapitres.isEmpty()) {
            Label noChap = new Label("Aucun chapitre");
            noChap.setStyle("-fx-font-size:11; -fx-text-fill:#cbd5e1;");
            chapitresBox.getChildren().add(noChap);
        }

        // Assembler la carte
        VBox card = new VBox(0, header, chapitresBox);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;"
            + "-fx-border-color:#e2e8f0; -fx-border-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:white; -fx-background-radius:12;"
            + "-fx-border-color:" + accentColor + "; -fx-border-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),12,0,0,4);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:white; -fx-background-radius:12;"
            + "-fx-border-color:#e2e8f0; -fx-border-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"));

        return card;
    }

    private void addEmptyMessage(VBox container, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#cbd5e1; -fx-font-size:12; -fx-padding:12 0 0 4;");
        container.getChildren().add(lbl);
    }
}

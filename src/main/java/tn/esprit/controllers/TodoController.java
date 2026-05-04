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
 * ═══════════════════════════════════════════════════════════════
 * CONTROLLER : MA LISTE D'APPRENTISSAGE (TODO / KANBAN)
 * ═══════════════════════════════════════════════════════════════
 * Affiche la progression de l'étudiant sous forme de Kanban 3 colonnes :
 *   - À Faire    : cours avec 0 chapitre complété
 *   - En Cours   : cours avec 1 à N-1 chapitres complétés
 *   - Terminé    : cours avec tous les chapitres complétés
 *
 * FONCTIONNALITÉS :
 *   - Dashboard gamification (points, streak, badges)
 *   - Barre de progression globale
 *   - Recommandations personnalisées (prochain chapitre à faire)
 *   - Bouton "Retour aux Cours"
 *
 * DÉPENDANCES :
 *   - CourseProgressService : récupère la progression depuis la BDD
 *   - ServiceCours / ServiceChapitre : récupère les données des cours
 *   - SessionManager : récupère l'utilisateur connecté
 * ═══════════════════════════════════════════════════════════════
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
    @FXML private VBox  recoContainer;
    // Dashboard gamification
    @FXML private Label  labelPoints;
    @FXML private Label  labelStreak;
    @FXML private Label  labelChapCompletes;
    @FXML private Label  labelCoursTermines;
    @FXML private HBox   badgesContainer;
    @FXML private Label  labelProgPct;
    @FXML private Region globalProgressFill;
    @FXML private Label  labelNextBadge;

    private final ServiceCours          serviceCours    = new ServiceCours();
    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService = new CourseProgressService();

    @FXML
    public void initialize() {
        // Vérifier que l'utilisateur est connecté avant de charger les données
        if (SessionManager.getCurrentUser() == null) return;
        // Charger les données dans un thread séparé pour ne pas bloquer l'UI
        Thread t = new Thread(this::loadData);
        t.setDaemon(true); // Thread daemon : s'arrête quand l'app se ferme
        t.start();
    }

    private void loadData() {
        int userId = SessionManager.getCurrentUser().getId();
        List<Cours> allCours = serviceCours.consulter(); // Récupère tous les cours

        // Listes temporaires pour chaque colonne Kanban
        List<VBox> todoCards       = new ArrayList<>();
        List<VBox> inProgressCards = new ArrayList<>();
        List<VBox> doneCards       = new ArrayList<>();
        int todoCount = 0, inProgressCount = 0, doneCount = 0, totalProgress = 0;

        // Pour chaque cours, calculer la progression et classer dans la bonne colonne
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

            // Recommandations
            buildRecommandations(userId, allCours);
            // Dashboard gamification
            buildDashboard(userId, allCours, fGlobal);
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

    private void buildRecommandations(int userId, List<Cours> allCours) {
        if (recoContainer == null) return;
        recoContainer.getChildren().clear();

        List<HBox> recos = new ArrayList<>();

        for (Cours cours : allCours) {
            List<Chapitre> chapitres = serviceChapitre.consulterParCoursId(cours.getId());
            Set<Integer> completedIds = new HashSet<>(
                progressService.getCompletedChapitreIds(userId, cours.getId()));
            int total = chapitres.size();
            int completed = (int) chapitres.stream().filter(ch -> completedIds.contains(ch.getId())).count();

            if (total == 0) continue;

            if (completed > 0 && completed < total) {
                // Cours en cours → suggérer le prochain chapitre
                Chapitre next = progressService.getNextChapitre(userId, cours.getId());
                if (next != null) {
                    recos.add(buildRecoCard(
                        "▶",  "#7a6ad8", "rgba(122,106,216,0.1)",
                        "Continue " + cours.getTitre(),
                        "Prochain : Chapitre " + next.getOrdre() + " — " + next.getTitre()
                            + "  (" + completed + "/" + total + " complétés)"
                    ));
                }
            } else if (completed == 0) {
                // Cours non commencé → suggérer de commencer
                Chapitre first = chapitres.isEmpty() ? null : chapitres.get(0);
                if (first != null) {
                    recos.add(buildRecoCard(
                        "🚀", "#059669", "rgba(5,150,105,0.1)",
                        "Commence " + cours.getTitre(),
                        "Commence par : Chapitre 1 — " + first.getTitre()
                    ));
                }
            }
        }

        if (recos.isEmpty()) {
            Label lbl = new Label("🎉  Félicitations ! Tous vos cours sont terminés.");
            lbl.setStyle("-fx-text-fill:#059669; -fx-font-size:13; -fx-font-weight:700;");
            recoContainer.getChildren().add(lbl);
        } else {
            // Afficher max 3 recommandations
            recos.stream().limit(3).forEach(r -> recoContainer.getChildren().add(r));
        }
    }

    private Runnable onRetourCallback;

    public void setOnRetour(Runnable callback) {
        this.onRetourCallback = callback;
    }

    @FXML
    private void onRetourCours() {
        if (onRetourCallback != null) {
            // Utiliser le callback si disponible (reste dans le frontoffice)
            onRetourCallback.run();
        } else {
            // Fallback: comportement actuel
            try {
                tn.esprit.MainApp.showCoursPage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HBox buildRecoCard(String icon, String accent, String bg, String titre, String detail) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12;"
            + "-fx-border-color:" + accent + "; -fx-border-radius:12; -fx-border-width:1;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:20; -fx-min-width:28;");

        VBox text = new VBox(3);
        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        Label detailLabel = new Label(detail);
        detailLabel.setStyle("-fx-font-size:11; -fx-text-fill:#64748b;");
        detailLabel.setWrapText(true);
        text.getChildren().addAll(titreLabel, detailLabel);
        HBox.setHgrow(text, Priority.ALWAYS);

        card.getChildren().addAll(iconLbl, text);
        return card;
    }

    private void buildDashboard(int userId, List<Cours> allCours, int globalProgress) {
        int points  = progressService.getTotalPoints(userId, allCours);
        int streak  = progressService.getStreak(userId);
        int chapDone = progressService.getTotalCompletedChapitres(userId);
        long coursDone = allCours.stream()
            .filter(c -> progressService.getCourseProgress(userId, c.getId()) >= 100).count();
        List<String[]> badges = progressService.getBadges(userId, allCours);

        if (labelPoints       != null) labelPoints.setText(String.valueOf(points));
        if (labelStreak       != null) labelStreak.setText(String.valueOf(streak));
        if (labelChapCompletes!= null) labelChapCompletes.setText(String.valueOf(chapDone));
        if (labelCoursTermines!= null) labelCoursTermines.setText(String.valueOf(coursDone));

        // Barre de progression globale
        if (globalProgressFill != null) {
            double ratio = Math.min(globalProgress, 100) / 100.0;
            globalProgressFill.setPrefWidth(ratio * 900); // largeur max approximative
        }
        if (labelProgPct != null) labelProgPct.setText(globalProgress + "%");

        // Prochain badge
        if (labelNextBadge != null) {
            if (chapDone < 1)       labelNextBadge.setText("💡 Complète 1 chapitre pour débloquer ⭐ Premier pas");
            else if (chapDone < 5)  labelNextBadge.setText("💡 " + (5 - chapDone) + " chapitres restants pour débloquer 📚 Lecteur");
            else if (chapDone < 10) labelNextBadge.setText("💡 " + (10 - chapDone) + " chapitres restants pour débloquer 🎓 Étudiant");
            else if (streak < 3)    labelNextBadge.setText("💡 Étudie " + (3 - streak) + " jours de plus pour débloquer 🔥 En feu");
            else if (streak < 7)    labelNextBadge.setText("💡 Étudie " + (7 - streak) + " jours de plus pour débloquer 💎 Invincible");
            else                    labelNextBadge.setText("🎉 Tous les badges débloqués !");
        }

        // Badges
        if (badgesContainer != null) {
            badgesContainer.getChildren().clear();
            if (badges.isEmpty()) {
                Label none = new Label("Aucun badge encore — commence à étudier !");
                none.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12;");
                badgesContainer.getChildren().add(none);
            } else {
                for (String[] b : badges) {
                    VBox badgeCard = new VBox(4);
                    badgeCard.setAlignment(Pos.CENTER);
                    badgeCard.setPadding(new Insets(10, 14, 10, 14));
                    badgeCard.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:12;"
                        + "-fx-border-color:#c4b5fd; -fx-border-radius:12;");
                    Label iconLbl = new Label(b[0]);
                    iconLbl.setStyle("-fx-font-size:24;");
                    Label nameLbl = new Label(b[1]);
                    nameLbl.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#4e3b9c;");
                    Label descLbl = new Label(b[2]);
                    descLbl.setStyle("-fx-font-size:9; -fx-text-fill:#94a3b8;");
                    badgeCard.getChildren().addAll(iconLbl, nameLbl, descLbl);
                    badgesContainer.getChildren().add(badgeCard);
                }
            }
        }
    }
}

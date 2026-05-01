package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;
import tn.esprit.session.JwtManager;

import java.util.List;

/**
 * QuizController — gère la liste des quiz en backoffice avec hiérarchie inline :
 * Quiz → Questions → Options (comme Symfony)
 */
public class QuizController {

    @FXML private VBox mainContainer;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceOption serviceOption = new ServiceOption();

    // Champ de recherche
    @FXML private TextField searchField;

    // Quiz/question actuellement développé (expanded)
    private Integer expandedQuizId = null;
    private Integer expandedQuestionId = null;
    private String searchTerm = "";

    // ── Pagination ────────────────────────────────────────────────────────────
    private int currentPage = 1;
    private int itemsPerPage = 5; // Nombre de quiz par page
    private int totalItems = 0;
    private int totalPages = 0;

    // Chargement initial
    @FXML
    public void initialize() {
        chargerTout();
    }

    // Recharge la liste avec filtre et pagination
    private void chargerTout() {
        mainContainer.getChildren().clear();
        List<Quiz> allQuizzes = serviceQuiz.afficher();

        // Filter by search term
        if (searchTerm != null && !searchTerm.isBlank()) {
            String term = searchTerm.toLowerCase();
            allQuizzes = allQuizzes.stream()
                .filter(q -> q.getTitre().toLowerCase().contains(term)
                          || q.getDescription().toLowerCase().contains(term))
                .toList();
        }

        totalItems = allQuizzes.size();
        totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        
        // Ajuster la page courante si nécessaire
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        if (allQuizzes.isEmpty()) {
            Label empty = new Label(searchTerm.isBlank()
                ? "Aucun quiz. Créez-en un avec « + Nouveau Quiz »."
                : "Aucun quiz trouvé pour « " + searchTerm + " ».");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:13; -fx-padding:24;");
            mainContainer.getChildren().add(empty);
            ajouterPagination(); // Ajouter la pagination même si vide
            return;
        }

        // Calculer les indices pour la pagination
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allQuizzes.size());
        
        // Afficher seulement les quiz de la page courante
        List<Quiz> pageQuizzes = allQuizzes.subList(startIndex, endIndex);
        
        for (int i = 0; i < pageQuizzes.size(); i++) {
            // L'index global pour la numérotation
            int globalIndex = startIndex + i + 1;
            mainContainer.getChildren().add(buildQuizBlock(pageQuizzes.get(i), globalIndex));
        }
        
        // Ajouter la pagination en bas
        ajouterPagination();
    }

    // Filtre la liste par le texte saisi dans le champ de recherche
    @FXML
    public void onSearch() {
        searchTerm = searchField.getText() == null ? "" : searchField.getText().trim();
        expandedQuizId = null;
        expandedQuestionId = null;
        currentPage = 1; // Retour à la première page lors d'une recherche
        chargerTout();
    }

    // Efface la recherche et recharge tout
    @FXML
    public void onClearSearch() {
        searchField.clear();
        searchTerm = "";
        expandedQuizId = null;
        expandedQuestionId = null;
        currentPage = 1; // Retour à la première page
        chargerTout();
    }

    // ── Pagination style web ──────────────────────────────────────────────────

    // Ajoute les boutons de pagination en bas de la liste
    private void ajouterPagination() {
        if (totalPages <= 1) return; // Pas de pagination si une seule page ou moins

        VBox paginationContainer = new VBox(16);
        paginationContainer.setAlignment(Pos.CENTER);
        paginationContainer.setStyle("-fx-padding:24 0 16 0;");

        // Informations sur la pagination
        Label infoLabel = new Label(String.format("Page %d sur %d • %d quiz au total", 
            currentPage, totalPages, totalItems));
        infoLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12;");

        // Conteneur des boutons de pagination
        HBox paginationBox = new HBox(8);
        paginationBox.setAlignment(Pos.CENTER);

        // Bouton Précédent
        Button btnPrev = createPaginationButton("← Précédent", currentPage > 1);
        btnPrev.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                chargerTout();
            }
        });

        paginationBox.getChildren().add(btnPrev);

        // Boutons de pages
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        // Première page si on est loin du début
        if (startPage > 1) {
            Button btn1 = createPageButton(1, false);
            btn1.setOnAction(e -> navigateToPage(1));
            paginationBox.getChildren().add(btn1);
            
            if (startPage > 2) {
                Label dots = new Label("...");
                dots.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:14; -fx-padding:8 4;");
                paginationBox.getChildren().add(dots);
            }
        }

        // Pages autour de la page courante
        for (int i = startPage; i <= endPage; i++) {
            final int pageNum = i; // Variable finale pour la lambda
            Button pageBtn = createPageButton(pageNum, pageNum == currentPage);
            pageBtn.setOnAction(e -> navigateToPage(pageNum));
            paginationBox.getChildren().add(pageBtn);
        }

        // Dernière page si on est loin de la fin
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                Label dots = new Label("...");
                dots.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:14; -fx-padding:8 4;");
                paginationBox.getChildren().add(dots);
            }
            
            Button btnLast = createPageButton(totalPages, false);
            btnLast.setOnAction(e -> navigateToPage(totalPages));
            paginationBox.getChildren().add(btnLast);
        }

        // Bouton Suivant
        Button btnNext = createPaginationButton("Suivant →", currentPage < totalPages);
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                chargerTout();
            }
        });

        paginationBox.getChildren().add(btnNext);

        paginationContainer.getChildren().addAll(infoLabel, paginationBox);
        mainContainer.getChildren().add(paginationContainer);
    }

    // Crée un bouton Précédent/Suivant (grisé si désactivé)
    private Button createPaginationButton(String text, boolean enabled) {
        Button btn = new Button(text);
        if (enabled) {
            btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.08);" +
                "-fx-border-color:rgba(255,255,255,0.2); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:rgba(245,245,244,0.85); -fx-font-size:12;" +
                "-fx-padding:8 16; -fx-cursor:hand;"
            );
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.12);" +
                "-fx-border-color:rgba(255,255,255,0.3); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:#f5f5f4; -fx-font-size:12;" +
                "-fx-padding:8 16; -fx-cursor:hand;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.08);" +
                "-fx-border-color:rgba(255,255,255,0.2); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:rgba(245,245,244,0.85); -fx-font-size:12;" +
                "-fx-padding:8 16; -fx-cursor:hand;"
            ));
        } else {
            btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.03);" +
                "-fx-border-color:rgba(255,255,255,0.1); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:rgba(245,245,244,0.3); -fx-font-size:12;" +
                "-fx-padding:8 16; -fx-cursor:default;"
            );
            btn.setDisable(true);
        }
        return btn;
    }

    // Crée un bouton de numéro de page (violet si page courante)
    private Button createPageButton(int pageNumber, boolean isCurrent) {
        Button btn = new Button(String.valueOf(pageNumber));
        if (isCurrent) {
            btn.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
                "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12;" +
                "-fx-padding:8 12; -fx-background-radius:8; -fx-border-width:0;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.4),8,0,0,2);"
            );
        } else {
            btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                "-fx-border-color:rgba(255,255,255,0.15); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:rgba(245,245,244,0.8); -fx-font-size:12;" +
                "-fx-padding:8 12; -fx-cursor:hand;"
            );
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.1);" +
                "-fx-border-color:rgba(255,255,255,0.25); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:#f5f5f4; -fx-font-size:12;" +
                "-fx-padding:8 12; -fx-cursor:hand;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.06);" +
                "-fx-border-color:rgba(255,255,255,0.15); -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-text-fill:rgba(245,245,244,0.8); -fx-font-size:12;" +
                "-fx-padding:8 12; -fx-cursor:hand;"
            ));
        }
        return btn;
    }

    // Navigue vers une page et ferme les éléments expandés
    private void navigateToPage(int page) {
        currentPage = page;
        expandedQuizId = null; // Fermer les quiz expandés lors du changement de page
        expandedQuestionId = null;
        chargerTout();
    }

    // ── Blocs visuels ─────────────────────────────────────────────────────────

    // Construit la carte quiz + ses questions si expandé
    private Node buildQuizBlock(Quiz quiz, int index) {
        VBox block = new VBox(0);
        block.setStyle("-fx-padding:0 0 2 0;");

        // Quiz card row
        block.getChildren().add(buildQuizCard(quiz, index));

        // If this quiz is expanded → show questions inline below
        if (expandedQuizId != null && expandedQuizId == quiz.getId()) {
            block.getChildren().add(buildQuestionsSection(quiz));
        }

        return block;
    }

    // Construit la ligne de carte d'un quiz avec ses boutons d'action
    private Node buildQuizCard(Quiz quiz, int index) {
        // Outer card
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color:rgba(255,255,255,0.04);" +
            "-fx-border-color:rgba(255,255,255,0.07) transparent rgba(255,255,255,0.07) transparent;" +
            "-fx-border-width:1 0 1 0; -fx-padding:16 20 16 20;"
        );

        // Left: number + title + badge + description
        VBox left = new VBox(4);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label num = new Label("#" + index);
        num.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:11; -fx-font-weight:bold;");

        Label titre = new Label(quiz.getTitre());
        titre.setStyle("-fx-text-fill:#f5f5f4; -fx-font-size:14; -fx-font-weight:bold;");

        Label badge = new Label("+ " + capitalize(quiz.getEtat()));
        String etat = quiz.getEtat().toLowerCase();
        String badgeStyle = switch (etat) {
            case "actif"     -> "-fx-background-color:rgba(16,185,129,0.15); -fx-text-fill:#22c55e;";
            case "inactif"   -> "-fx-background-color:rgba(245,158,11,0.15); -fx-text-fill:#eab308;";
            case "brouillon" -> "-fx-background-color:rgba(59,130,246,0.15); -fx-text-fill:#0ea5e9;";
            default          -> "-fx-background-color:rgba(71,85,105,0.3); -fx-text-fill:rgba(245,245,244,0.45);";
        };
        badge.setStyle(badgeStyle +
            "-fx-background-radius:20px; -fx-padding:3 10; -fx-font-size:11px; -fx-font-weight:bold;");

        titleRow.getChildren().addAll(num, titre, badge);

        Label desc = new Label(truncate(quiz.getDescription(), 100));
        desc.setStyle("-fx-text-fill:rgba(245,245,244,0.45); -fx-font-size:12; -fx-wrap-text:true;");

        left.getChildren().addAll(titleRow, desc);

        // Right: action buttons (2 rows like Symfony)
        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(220);

        // Row 1: Sélectionner + Voir
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_RIGHT);

        boolean isExpanded = expandedQuizId != null && expandedQuizId == quiz.getId();

        // Sélectionner — vert si actif, gris sinon
        Button btnSelect = new Button(isExpanded ? "✓ Sélectionné" : "✓ Sélectionner");
        btnSelect.setStyle(isExpanded
            ? "-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
              "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;" +
              "-fx-padding:6 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            : "-fx-background-color:rgba(255,255,255,0.08);" +
              "-fx-border-color:rgba(255,255,255,0.2); -fx-border-width:1;" +
              "-fx-border-radius:8; -fx-background-radius:8;" +
              "-fx-text-fill:rgba(245,245,244,0.85); -fx-font-size:12px;" +
              "-fx-padding:6 12; -fx-cursor:hand;");
        btnSelect.setOnAction(e -> {
            if (isExpanded) {
                expandedQuizId = null;
                expandedQuestionId = null;
            } else {
                expandedQuizId = quiz.getId();
                expandedQuestionId = null;
            }
            chargerTout();
        });

        // Voir — bleu
        Button btnVoir = new Button("⊙ Voir");
        btnVoir.setStyle(
            "-fx-background-color:rgba(14,165,233,0.15);" +
            "-fx-border-color:rgba(14,165,233,0.4); -fx-border-width:1;" +
            "-fx-border-radius:8; -fx-background-radius:8;" +
            "-fx-text-fill:#38bdf8; -fx-font-size:12px;" +
            "-fx-padding:6 12; -fx-cursor:hand;");
        btnVoir.setOnAction(e -> voirQuiz(quiz));

        row1.getChildren().addAll(btnSelect, btnVoir);

        HBox row2 = new HBox(4);
        row2.setAlignment(Pos.CENTER_RIGHT);

        // Modifier — or/amber
        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#e8c9a0,#d4a574);" +
            "-fx-text-fill:#0f1a14; -fx-font-weight:bold; -fx-font-size:12px;" +
            "-fx-padding:6 14; -fx-background-radius:8; -fx-cursor:hand;" +
            "-fx-border-width:0; -fx-min-width:100;");
        btnEdit.setOnAction(e -> ouvrirFormModification(quiz));

        // Supprimer — rouge
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#f87171,#dc2626);" +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;" +
            "-fx-padding:6 14; -fx-background-radius:8; -fx-cursor:hand;" +
            "-fx-border-width:0; -fx-min-width:100;");
        btnDel.setOnAction(e -> supprimerQuiz(quiz));

        row2.getChildren().addAll(btnEdit, btnDel);
        right.getChildren().addAll(row1, row2);

        card.getChildren().addAll(left, right);
        return card;
    }

    // Construit la section questions inline sous un quiz expandé
    private Node buildQuestionsSection(Quiz quiz) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:rgba(0,0,0,0.15); -fx-padding:0 0 0 32;");

        // "+ Nouvelle Question" button row
        HBox newQRow = new HBox();
        newQRow.setStyle("-fx-padding:10 20 10 20;");
        Button btnNewQ = new Button("+ Nouvelle Question");
        btnNewQ.setStyle(
            "-fx-background-color:rgba(52,211,153,0.15); -fx-text-fill:#34d399;" +
            "-fx-border-color:rgba(52,211,153,0.3); -fx-border-width:1; -fx-border-radius:8;" +
            "-fx-background-radius:8; -fx-font-size:12; -fx-font-weight:bold;" +
            "-fx-padding:6 14; -fx-cursor:hand;"
        );
        btnNewQ.setOnAction(e -> ouvrirFormQuestionDialog(null, quiz.getId()));
        newQRow.getChildren().add(btnNewQ);
        section.getChildren().add(newQRow);

        // Question cards
        List<Question> questions = serviceQuestion.findByQuizId(quiz.getId());
        if (questions.isEmpty()) {
            Label empty = new Label("Aucune question pour ce quiz.");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:12; -fx-padding:8 20;");
            section.getChildren().add(empty);
        } else {
            for (int i = 0; i < questions.size(); i++) {
                section.getChildren().add(buildQuestionBlock(questions.get(i), i + 1, quiz.getId()));
            }
        }

        return section;
    }

    private Node buildQuestionBlock(Question q, int index, int quizId) {
        VBox block = new VBox(0);

        // Question card row
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color:rgba(255,255,255,0.03);" +
            "-fx-border-color:rgba(255,255,255,0.05) transparent rgba(255,255,255,0.05) transparent;" +
            "-fx-border-width:1 0 1 0; -fx-padding:12 20 12 20;"
        );

        // Left: number + text + points
        VBox left = new VBox(3);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label num = new Label("#" + index);
        num.setStyle("-fx-background-color:rgba(99,102,241,0.2); -fx-text-fill:#818cf8;" +
                     "-fx-background-radius:4; -fx-padding:1 6; -fx-font-size:10; -fx-font-weight:bold;");

        Label texte = new Label(truncate(q.getTexteQuestion(), 70));
        texte.setStyle("-fx-text-fill:#f5f5f4; -fx-font-size:13; -fx-font-weight:bold;");

        titleRow.getChildren().addAll(num, texte);

        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("🔲 Type: Standard");
        typeLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11;");
        Label ptsLabel = new Label("★ " + q.getPoint() + " points");
        ptsLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11;");
        metaRow.getChildren().addAll(typeLabel, ptsLabel);

        left.getChildren().addAll(titleRow, metaRow);

        // Right: Sélectionner + Voir + Modifier + Supprimer
        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(200);

        boolean isExpanded = expandedQuestionId != null && expandedQuestionId == q.getId();

        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_RIGHT);

        // Sélectionner — vert si actif, gris sinon
        Button btnSelect = new Button(isExpanded ? "✓ Sélectionné" : "✓ Sélectionner");
        btnSelect.setStyle(isExpanded
            ? "-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
              "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:11px;" +
              "-fx-padding:5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            : "-fx-background-color:rgba(255,255,255,0.08);" +
              "-fx-border-color:rgba(255,255,255,0.2); -fx-border-width:1;" +
              "-fx-border-radius:8; -fx-background-radius:8;" +
              "-fx-text-fill:rgba(245,245,244,0.85); -fx-font-size:11px;" +
              "-fx-padding:5 10; -fx-cursor:hand;");
        btnSelect.setOnAction(e -> {
            expandedQuestionId = isExpanded ? null : q.getId();
            chargerTout();
        });

        // Voir — bleu
        Button btnVoir = new Button("⊙ Voir");
        btnVoir.setStyle(
            "-fx-background-color:rgba(14,165,233,0.15);" +
            "-fx-border-color:rgba(14,165,233,0.4); -fx-border-width:1;" +
            "-fx-border-radius:8; -fx-background-radius:8;" +
            "-fx-text-fill:#38bdf8; -fx-font-size:11px;" +
            "-fx-padding:5 10; -fx-cursor:hand;");
        btnVoir.setOnAction(e -> voirQuestion(q));

        row1.getChildren().addAll(btnSelect, btnVoir);

        HBox row2 = new HBox(4);
        row2.setAlignment(Pos.CENTER_RIGHT);

        // Modifier — or/amber
        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#e8c9a0,#d4a574);" +
            "-fx-text-fill:#0f1a14; -fx-font-weight:bold; -fx-font-size:11px;" +
            "-fx-padding:5 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnEdit.setOnAction(e -> ouvrirFormQuestionDialog(q, quizId));

        // Supprimer — rouge
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#f87171,#dc2626);" +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:11px;" +
            "-fx-padding:5 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnDel.setOnAction(e -> supprimerQuestion(q));

        row2.getChildren().addAll(btnEdit, btnDel);
        right.getChildren().addAll(row1, row2);

        card.getChildren().addAll(left, right);
        block.getChildren().add(card);

        // If this question is expanded → show options inline below
        if (isExpanded) {
            block.getChildren().add(buildOptionsSection(q));
        }

        return block;
    }

    // Construit la section options inline sous une question expandée
    private Node buildOptionsSection(Question q) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:rgba(0,0,0,0.2); -fx-padding:0 0 0 32;");

        // "+ Nouvelle Option" button
        HBox newOptRow = new HBox();
        newOptRow.setStyle("-fx-padding:8 20 8 20;");
        Button btnNewOpt = new Button("+ Nouvelle Option");
        btnNewOpt.setStyle(
            "-fx-background-color:rgba(139,92,246,0.15); -fx-text-fill:#a78bfa;" +
            "-fx-border-color:rgba(139,92,246,0.3); -fx-border-width:1; -fx-border-radius:8;" +
            "-fx-background-radius:8; -fx-font-size:12; -fx-font-weight:bold;" +
            "-fx-padding:5 12; -fx-cursor:hand;"
        );
        btnNewOpt.setOnAction(e -> ouvrirFormOptionDialog(null, q.getId()));
        newOptRow.getChildren().add(btnNewOpt);
        section.getChildren().add(newOptRow);

        List<Option> options = serviceOption.findByQuestionId(q.getId());
        if (options.isEmpty()) {
            Label empty = new Label("Aucune option pour cette question.");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.35); -fx-font-size:12; -fx-padding:6 20;");
            section.getChildren().add(empty);
        } else {
            for (int i = 0; i < options.size(); i++) {
                section.getChildren().add(buildOptionRow(options.get(i), i + 1, q.getId()));
            }
        }

        return section;
    }

    private Node buildOptionRow(Option opt, int index, int questionId) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-background-color:rgba(255,255,255,0.02);" +
            "-fx-border-color:rgba(255,255,255,0.04) transparent rgba(255,255,255,0.04) transparent;" +
            "-fx-border-width:1 0 1 0; -fx-padding:10 20 10 20;"
        );

        Label num = new Label("#" + index);
        num.setStyle("-fx-text-fill:rgba(245,245,244,0.3); -fx-font-size:10; -fx-font-weight:bold; -fx-min-width:20;");

        Label texte = new Label(opt.getTexteOption());
        texte.setStyle("-fx-text-fill:rgba(245,245,244,0.8); -fx-font-size:12; -fx-wrap-text:true;");
        HBox.setHgrow(texte, Priority.ALWAYS);

        // Badge correcte/incorrecte
        Label badge = new Label(opt.isEstCorrecte() ? "✓ Correcte" : "✗ Incorrecte");
        badge.setStyle(opt.isEstCorrecte()
            ? "-fx-background-color:rgba(16,185,129,0.15); -fx-text-fill:#22c55e;" +
              "-fx-background-radius:12; -fx-padding:2 8; -fx-font-size:11; -fx-font-weight:bold;"
            : "-fx-background-color:rgba(239,68,68,0.15); -fx-text-fill:#f87171;" +
              "-fx-background-radius:12; -fx-padding:2 8; -fx-font-size:11; -fx-font-weight:bold;"
        );

        // Modifier — or/amber
        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#e8c9a0,#d4a574);" +
            "-fx-text-fill:#0f1a14; -fx-font-weight:bold; -fx-font-size:11px;" +
            "-fx-padding:4 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnEdit.setOnAction(e -> ouvrirFormOptionDialog(opt, questionId));

        // Supprimer — rouge
        Button btnDel = new Button("🗑");
        btnDel.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#f87171,#dc2626);" +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:11px;" +
            "-fx-padding:4 8; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnDel.setOnAction(e -> supprimerOption(opt));

        row.getChildren().addAll(num, texte, badge, btnEdit, btnDel);
        return row;
    }

    // ── CRUD Quiz ─────────────────────────────────────────────────────────────

    // Ouvre le formulaire de création d'un nouveau quiz
    @FXML
    public void ouvrirFormAjout() {
        naviguerVersFormulaire(null);
    }

    // Ouvre la fenêtre de génération de quiz par IA (Groq)
    @FXML
    public void ouvrirGenerateurIA() {
        tn.esprit.services.ServiceChapitre serviceChapitre = new tn.esprit.services.ServiceChapitre();
        java.util.List<tn.esprit.entities.Chapitre> chapitres = serviceChapitre.consulter();

        if (chapitres.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                "Aucun chapitre disponible. Créez d'abord un chapitre avec du contenu.")
                .showAndWait();
            return;
        }

        // ── Fenêtre custom sombre ──────────────────────────────────────────────
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.setResizable(false);

        // ── Conteneur racine ──────────────────────────────────────────────────
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color:#0f1a14; -fx-background-radius:16;");
        root.setPrefWidth(460);

        // ── En-tête ───────────────────────────────────────────────────────────
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setSpacing(12);
        header.setPadding(new javafx.geometry.Insets(20, 24, 16, 24));
        header.setStyle(
            "-fx-background-color:rgba(255,255,255,0.04);" +
            "-fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent;" +
            "-fx-border-width:0 0 1 0;"
        );

        // Icône IA
        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("🤖");
        iconLbl.setStyle(
            "-fx-font-size:22; -fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
            "-fx-background-radius:10; -fx-padding:8 10 8 10;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),10,0,0,3);"
        );

        javafx.scene.layout.VBox titleBox = new javafx.scene.layout.VBox(3);
        javafx.scene.control.Label titleLbl = new javafx.scene.control.Label("Générer un Quiz avec IA");
        titleLbl.setStyle("-fx-text-fill:#f5f5f4; -fx-font-size:16; -fx-font-weight:bold;");
        javafx.scene.control.Label subtitleLbl = new javafx.scene.control.Label("Génération automatique via Groq (Llama 4 Scout)");
        subtitleLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:11;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Bouton fermer ×
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("✕");
        btnClose.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.4);" +
            "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:4 8;"
        );
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
            "-fx-background-color:rgba(239,68,68,0.2); -fx-text-fill:#fca5a5;" +
            "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:4 8; -fx-background-radius:6;"
        ));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.4);" +
            "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:4 8;"
        ));
        btnClose.setOnAction(e -> stage.close());

        header.getChildren().addAll(iconLbl, titleBox, spacer, btnClose);

        // ── Corps du formulaire dans un ScrollPane ────────────────────────────
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(6);
        body.setPadding(new javafx.geometry.Insets(24, 28, 8, 28));

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(body);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxHeight(420);
        scrollPane.setStyle(
            "-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;"
        );

        String labelStyle = "-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12; -fx-font-weight:bold; -fx-padding:0 0 4 0;";
        String fieldStyle =
            "-fx-background-color:rgba(255,255,255,0.05);" +
            "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8;" +
            "-fx-background-radius:8; -fx-border-width:1;" +
            "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
            "-fx-padding:9 13; -fx-font-size:13;";
        String comboStyle =
            "-fx-background-color:#1a2e1f;" +
            "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8;" +
            "-fx-background-radius:8; -fx-border-width:1; -fx-padding:3;" +
            "-fx-text-fill:#f5f5f4;";

        // Chapitre
        javafx.scene.control.Label lblChapitre = new javafx.scene.control.Label("Chapitre source");
        lblChapitre.setStyle(labelStyle);
        javafx.scene.control.ComboBox<tn.esprit.entities.Chapitre> comboChapitre = new javafx.scene.control.ComboBox<>();
        comboChapitre.getItems().addAll(chapitres);
        comboChapitre.setPromptText("Sélectionnez un chapitre");
        comboChapitre.setMaxWidth(Double.MAX_VALUE);
        comboChapitre.setStyle(comboStyle);
        comboChapitre.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(tn.esprit.entities.Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4; -fx-background-color:#1a2e1f; -fx-padding:8 14;");
            }
        });
        comboChapitre.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(tn.esprit.entities.Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez un chapitre" : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });

        // Nombre de questions
        javafx.scene.control.Label lblNb = new javafx.scene.control.Label("Nombre de questions (1-10)");
        lblNb.setStyle(labelStyle);
        javafx.scene.control.Spinner<Integer> spinnerNb = new javafx.scene.control.Spinner<>(1, 10, 5);
        spinnerNb.setEditable(true);
        spinnerNb.setMaxWidth(Double.MAX_VALUE);
        spinnerNb.setStyle("-fx-background-color:#1a2e1f;");
        // Style interne du spinner
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node editor = spinnerNb.getEditor();
            if (editor != null) editor.setStyle(fieldStyle);
        });

        // Difficulté
        javafx.scene.control.Label lblDiff = new javafx.scene.control.Label("Niveau de difficulté");
        lblDiff.setStyle(labelStyle);
        javafx.scene.control.ComboBox<String> comboDiff = new javafx.scene.control.ComboBox<>();
        comboDiff.getItems().addAll("facile", "moyen", "difficile");
        comboDiff.setValue("moyen");
        comboDiff.setMaxWidth(Double.MAX_VALUE);
        comboDiff.setStyle(comboStyle);
        comboDiff.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#f5f5f4; -fx-background-color:#1a2e1f; -fx-padding:8 14;");
            }
        });
        comboDiff.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "moyen" : item);
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });

        // Status
        javafx.scene.control.Label statusLabel = new javafx.scene.control.Label("Sélectionnez un chapitre, une difficulté et un état pour activer la génération");
        statusLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:12;");
        statusLabel.setWrapText(true);
        statusLabel.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

        // ── Séparateur ────────────────────────────────────────────────────────
        javafx.scene.layout.Region sep = new javafx.scene.layout.Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.08);");

        // ── État ──────────────────────────────────────────────────────────────
        javafx.scene.control.Label lblEtat = new javafx.scene.control.Label("État");
        lblEtat.setStyle(labelStyle);
        javafx.scene.control.ComboBox<String> comboEtat = new javafx.scene.control.ComboBox<>();
        comboEtat.getItems().addAll("actif", "inactif", "brouillon", "archive");
        comboEtat.setValue("brouillon");
        comboEtat.setMaxWidth(Double.MAX_VALUE);
        comboEtat.setStyle(comboStyle);
        comboEtat.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#f5f5f4; -fx-background-color:#1a2e1f; -fx-padding:8 14;");
            }
        });
        comboEtat.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "brouillon" : item);
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });

        // ── Durée maximale ────────────────────────────────────────────────────
        javafx.scene.control.Label lblDuree = new javafx.scene.control.Label("Durée maximale (minutes)");
        lblDuree.setStyle(labelStyle);
        javafx.scene.control.TextField fieldDuree = new javafx.scene.control.TextField();
        fieldDuree.setPromptText("ex: 30  —  laisser vide pour illimité");
        fieldDuree.setStyle(fieldStyle);
        fieldDuree.setMaxWidth(Double.MAX_VALUE);

        // ── Seuil de réussite ─────────────────────────────────────────────────
        javafx.scene.control.Label lblSeuil = new javafx.scene.control.Label("Seuil de réussite (%)");
        lblSeuil.setStyle(labelStyle);
        javafx.scene.control.TextField fieldSeuil = new javafx.scene.control.TextField();
        fieldSeuil.setPromptText("ex: 50  —  défaut 50%");
        fieldSeuil.setStyle(fieldStyle);
        fieldSeuil.setMaxWidth(Double.MAX_VALUE);

        // ── Tentatives ────────────────────────────────────────────────────────
        javafx.scene.control.Label lblTentatives = new javafx.scene.control.Label("Nombre maximum de tentatives");
        lblTentatives.setStyle(labelStyle);
        javafx.scene.control.TextField fieldTentatives = new javafx.scene.control.TextField();
        fieldTentatives.setPromptText("ex: 3  —  laisser vide pour illimité");
        fieldTentatives.setStyle(fieldStyle);
        fieldTentatives.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(
            lblChapitre, comboChapitre,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            lblNb, spinnerNb,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            lblDiff, comboDiff,
            new javafx.scene.layout.Region() {{ setPrefHeight(12); }},
            sep,
            new javafx.scene.layout.Region() {{ setPrefHeight(12); }},
            lblEtat, comboEtat,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            lblDuree, fieldDuree,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            lblSeuil, fieldSeuil,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            lblTentatives, fieldTentatives,
            new javafx.scene.layout.Region() {{ setPrefHeight(8); }},
            statusLabel
        );

        // ── Pied de page avec boutons ─────────────────────────────────────────
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(12);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(20, 28, 24, 28));
        footer.setStyle(
            "-fx-border-color:rgba(255,255,255,0.08) transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;"
        );

        javafx.scene.control.Button btnAnnuler = new javafx.scene.control.Button("Annuler");
        btnAnnuler.setStyle(
            "-fx-background-color:rgba(255,255,255,0.06); -fx-text-fill:rgba(245,245,244,0.7);" +
            "-fx-background-radius:8; -fx-padding:10 20; -fx-cursor:hand; -fx-border-width:0;"
        );
        btnAnnuler.setOnAction(e -> stage.close());

        javafx.scene.control.Button btnGenerer = new javafx.scene.control.Button("🚀  Générer");
        btnGenerer.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
            "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;" +
            "-fx-padding:10 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.4),10,0,0,3);"
        );
        btnGenerer.setDisable(true);

        // ── Validation en temps réel pour activer/désactiver le bouton ────────
        Runnable validateForm = () -> {
            boolean isValid = comboChapitre.getValue() != null && 
                             comboDiff.getValue() != null && 
                             comboEtat.getValue() != null;
            btnGenerer.setDisable(!isValid);
            
            if (isValid) {
                statusLabel.setText("✅ Prêt pour la génération IA");
                statusLabel.setStyle("-fx-text-fill:#22c55e; -fx-font-size:12; -fx-font-weight:bold;");
            } else {
                statusLabel.setText("Sélectionnez un chapitre, une difficulté et un état pour activer la génération");
                statusLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:12;");
            }
        };

        // Listeners pour validation en temps réel
        comboChapitre.valueProperty().addListener((o, ov, nv) -> validateForm.run());
        comboDiff.valueProperty().addListener((o, ov, nv) -> validateForm.run());
        comboEtat.valueProperty().addListener((o, ov, nv) -> validateForm.run());

        btnGenerer.setOnAction(e -> {
            // ── Validation complète avant génération ──────────────────────────────
            statusLabel.setText("");
            statusLabel.setStyle("");

            // Réinitialiser les styles des champs
            comboChapitre.setStyle(comboStyle);
            comboEtat.setStyle(comboStyle);
            comboDiff.setStyle(comboStyle);
            fieldDuree.setStyle(fieldStyle);
            fieldSeuil.setStyle(fieldStyle);
            fieldTentatives.setStyle(fieldStyle);

            // ── Validation du nombre de questions ──
            int nb = spinnerNb.getValue();
            if (nb < 1 || nb > 10) {
                statusLabel.setText("⚠ Le nombre de questions doit être entre 1 et 10 (actuellement " + nb + ")");
                statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                return;
            }

            // ── Validation du chapitre (OBLIGATOIRE) ──
            tn.esprit.entities.Chapitre chapitre = comboChapitre.getValue();
            if (chapitre == null) {
                comboChapitre.setStyle(comboStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                statusLabel.setText("🔒 OBLIGATOIRE : Sélectionnez un chapitre source pour la génération IA");
                statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                return;
            }

            // ── Validation de la difficulté (doit être dans la liste) ──
            String diff = comboDiff.getValue();
            if (diff == null || !java.util.List.of("facile","moyen","difficile").contains(diff)) {
                comboDiff.setStyle(comboStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                statusLabel.setText("⚠ Veuillez sélectionner un niveau de difficulté : Facile, Moyen, Difficile");
                statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                return;
            }

            // ── Validation de l'état (doit être dans la liste) ──
            String etat = comboEtat.getValue();
            if (etat == null || !java.util.List.of("actif","inactif","brouillon","archive").contains(etat)) {
                comboEtat.setStyle(comboStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                statusLabel.setText("⚠ Veuillez sélectionner un état parmi : Actif, Inactif, Brouillon, Archive");
                statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                return;
            }

            // ── Validation de la durée (optionnelle, entier positif max 600) ──
            String dureeStr = fieldDuree.getText() == null ? "" : fieldDuree.getText().trim();
            Integer duree = null;
            if (!dureeStr.isEmpty()) {
                try {
                    duree = Integer.parseInt(dureeStr);
                    if (duree <= 0) {
                        fieldDuree.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                        statusLabel.setText("⚠ La durée doit être un nombre entier positif (ex: 30)");
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                        return;
                    } else if (duree > 600) {
                        fieldDuree.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                        statusLabel.setText("⚠ La durée maximale est 600 minutes (10 heures)");
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    fieldDuree.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                    statusLabel.setText("⚠ La durée doit être un nombre entier (ex: 30), pas \"" + dureeStr + "\"");
                    statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                    return;
                }
            }

            // ── Validation du seuil de réussite (optionnel, entre 0 et 100) ──
            String seuilStr = fieldSeuil.getText() == null ? "" : fieldSeuil.getText().trim();
            Integer seuil = null;
            if (!seuilStr.isEmpty()) {
                try {
                    seuil = Integer.parseInt(seuilStr);
                    if (seuil < 0 || seuil > 100) {
                        fieldSeuil.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                        statusLabel.setText("⚠ Le seuil doit être un pourcentage entre 0 et 100");
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    fieldSeuil.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                    statusLabel.setText("⚠ Le seuil doit être un entier entre 0 et 100 (ex: 50), pas \"" + seuilStr + "\"");
                    statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                    return;
                }
            }

            // ── Validation du nombre de tentatives (optionnel, entier positif max 100) ──
            String tentStr = fieldTentatives.getText() == null ? "" : fieldTentatives.getText().trim();
            Integer tentatives = null;
            if (!tentStr.isEmpty()) {
                try {
                    tentatives = Integer.parseInt(tentStr);
                    if (tentatives <= 0) {
                        fieldTentatives.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                        statusLabel.setText("⚠ Le nombre de tentatives doit être un entier positif (ex: 3)");
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                        return;
                    } else if (tentatives > 100) {
                        fieldTentatives.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                        statusLabel.setText("⚠ Le nombre de tentatives ne peut pas dépasser 100");
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    fieldTentatives.setStyle(fieldStyle.replace("rgba(255,255,255,0.1)", "rgba(239,68,68,0.6)"));
                    statusLabel.setText("⚠ Le nombre de tentatives doit être un entier (ex: 3), pas \"" + tentStr + "\"");
                    statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12; -fx-font-weight:bold;");
                    return;
                }
            }

            // ── Créer des copies finales pour les lambdas ─────────────────────────
            final Integer dureeFinal = duree;
            final Integer seuilFinal = seuil;
            final Integer tentativesFinal = tentatives;

            // ── Génération IA ─────────────────────────────────────────────────────
            btnGenerer.setDisable(true);
            btnAnnuler.setDisable(true);
            statusLabel.setText("⏳ L'IA génère " + nb + " questions... (5-15 secondes)");
            statusLabel.setStyle("-fx-text-fill:#a5b4fc; -fx-font-size:12;");

            tn.esprit.services.GroqQuizGeneratorService groqService = new tn.esprit.services.GroqQuizGeneratorService();
            groqService.genererQuizAsync(chapitre, nb, diff, null, chapitre.getId())
                .thenAccept(quiz -> javafx.application.Platform.runLater(() -> {
                    stage.close();
                    if (quiz != null) {
                        // Appliquer les paramètres personnalisés validés
                        quiz.setEtat(comboEtat.getValue());
                        if (dureeFinal != null) quiz.setDureeMaxMinutes(dureeFinal);
                        if (seuilFinal != null) quiz.setSeuilReussite(seuilFinal);
                        if (tentativesFinal != null) quiz.setMaxTentatives(tentativesFinal);
                        serviceQuiz.modifier(quiz);
                        
                        // Construire le message de succès avec les paramètres appliqués
                        StringBuilder successMsg = new StringBuilder();
                        successMsg.append("Quiz \"").append(quiz.getTitre()).append("\" créé avec ").append(nb).append(" questions !\n\n");
                        successMsg.append("État : ").append(quiz.getEtat()).append("\n");
                        if (dureeFinal != null) {
                            successMsg.append("Durée : ").append(dureeFinal).append(" minutes\n");
                        }
                        if (seuilFinal != null) {
                            successMsg.append("Seuil de réussite : ").append(seuilFinal).append("%\n");
                        }
                        if (tentativesFinal != null) {
                            successMsg.append("Tentatives max : ").append(tentativesFinal).append("\n");
                        }
                        successMsg.append("\n— Révisez et activez-le.");
                        
                        showAlert(true, successMsg.toString(), "");
                        chargerTout();
                    }
                }))
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        btnGenerer.setDisable(false);
                        btnAnnuler.setDisable(false);
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        statusLabel.setText("❌ Erreur : " + msg);
                        statusLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12;");
                    });
                    return null;
                });
        });

        footer.getChildren().addAll(btnAnnuler, btnGenerer);

        // ── Assemblage ────────────────────────────────────────────────────────
        root.getChildren().addAll(header, scrollPane, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // Ouvre le formulaire de modification d'un quiz existant
    private void ouvrirFormModification(Quiz quiz) {
        naviguerVersFormulaire(quiz);
    }

    // Navigue vers le formulaire quiz (création ou modification)
    private void naviguerVersFormulaire(Quiz quiz) {
        try {
            StackPane contentArea =
                (StackPane) mainContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/quiz_form.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            QuizFormController ctrl = loader.getController();
            if (quiz != null) ctrl.initEdit(quiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Demande confirmation puis supprime le quiz et ses questions/options
    private void supprimerQuiz(Quiz quiz) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le quiz « " + quiz.getTitre() + " » ?\nToutes ses questions seront supprimées.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = serviceQuiz.supprimer(quiz);
                if (ok) {
                    var admin = JwtManager.getCurrentUser();
                    if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.deleted_quiz",
                        java.util.Map.of("titre", quiz.getTitre() != null ? quiz.getTitre() : ""));
                }
                showAlert(ok, "Quiz supprimé avec succès !", "Échec de la suppression du quiz.");
                if (ok) {
                    if (expandedQuizId != null && expandedQuizId == quiz.getId()) {
                        expandedQuizId = null;
                        expandedQuestionId = null;
                    }
                    chargerTout();
                }
            }
        });
    }

    // Ouvre la page de détail d'un quiz
    private void voirQuiz(Quiz quiz) {
        try {
            StackPane contentArea =
                (StackPane) mainContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/show.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            QuizShowController ctrl = loader.getController();
            ctrl.init(quiz, this::chargerTout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── CRUD Question ─────────────────────────────────────────────────────────

    // Ouvre le formulaire question (création ou modification)
    private void ouvrirFormQuestionDialog(Question question, int quizId) {
        try {
            StackPane contentArea =
                (StackPane) mainContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/question_form.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            QuestionController ctrl = loader.getController();
            if (question == null) ctrl.initNouvelle(quizId);
            else ctrl.initModifier(question);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Demande confirmation puis supprime la question et ses options
    private void supprimerQuestion(Question q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = serviceQuestion.supprimer(q);
                showAlert(ok, "Question supprimée avec succès !", "Échec de la suppression de la question.");
                if (ok) {
                    if (expandedQuestionId != null && expandedQuestionId == q.getId())
                        expandedQuestionId = null;
                    chargerTout();
                }
            }
        });
    }

    // Affiche les détails d'une question dans une alerte
    private void voirQuestion(Question q) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Détails de la Question");
        info.setHeaderText(null);
        info.setContentText(
            "Question : " + q.getTexteQuestion() + "\n" +
            "Points : " + q.getPoint() + "\n" +
            "Options : " + serviceOption.findByQuestionId(q.getId()).size()
        );
        info.showAndWait();
    }

    // ── CRUD Option ───────────────────────────────────────────────────────────

    // Ouvre le formulaire option (création ou modification)
    private void ouvrirFormOptionDialog(Option option, int questionId) {
        try {
            StackPane contentArea =
                (StackPane) mainContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/option_form.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            OptionController ctrl = loader.getController();
            if (option == null) ctrl.initNouvelle(questionId);
            else ctrl.initModifier(option);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Demande confirmation puis supprime l'option
    private void supprimerOption(Option opt) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette option ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = serviceOption.supprimer(opt);
                showAlert(ok, "Option supprimée avec succès !", "Échec de la suppression de l'option.");
                if (ok) chargerTout();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Affiche une alerte de succès ou d'échec
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "✅ Succès" : "❌ Échec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait();
    }

    // Affiche une notification de succès stylée (fond sombre, bordure verte)
    private void afficherNotificationSucces(String titre, String message) {
        Stage notif = new Stage();
        notif.initModality(Modality.APPLICATION_MODAL);
        notif.initStyle(javafx.stage.StageStyle.UNDECORATED);
        notif.setResizable(false);

        // Conteneur racine avec fond sombre et bordure verte
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle(
            "-fx-background-color:#0f1a14;" +
            "-fx-border-color:#34d399;" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:12;" +
            "-fx-background-radius:12;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),24,0,0,8);"
        );
        root.setPrefWidth(420);

        // En-tête vert
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new javafx.geometry.Insets(18, 22, 14, 22));
        header.setStyle(
            "-fx-background-color:rgba(52,211,153,0.10);" +
            "-fx-border-color:transparent transparent rgba(52,211,153,0.2) transparent;" +
            "-fx-border-width:0 0 1 0;" +
            "-fx-background-radius:12 12 0 0;"
        );

        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("✅");
        iconLbl.setStyle("-fx-font-size:22;");

        javafx.scene.control.Label titleLbl = new javafx.scene.control.Label(titre);
        titleLbl.setStyle("-fx-text-fill:#34d399; -fx-font-size:15; -fx-font-weight:bold;");

        header.getChildren().addAll(iconLbl, titleLbl);

        // Corps
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(0);
        body.setPadding(new javafx.geometry.Insets(18, 22, 8, 22));

        javafx.scene.control.Label msgLbl = new javafx.scene.control.Label(message);
        msgLbl.setStyle("-fx-text-fill:rgba(245,245,244,0.8); -fx-font-size:13;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(376);

        body.getChildren().add(msgLbl);

        // Pied
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(14, 22, 18, 22));

        javafx.scene.control.Button btnOk = new javafx.scene.control.Button("OK");
        btnOk.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13;" +
            "-fx-padding:9 32; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(5,150,105,0.4),10,0,0,3);"
        );
        btnOk.setOnAction(e -> notif.close());

        footer.getChildren().add(btnOk);
        root.getChildren().addAll(header, body, footer);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        notif.setScene(scene);
        notif.showAndWait();
    }

    // Tronque un texte long avec "..." si dépasse max caractères
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // Met la première lettre en majuscule
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

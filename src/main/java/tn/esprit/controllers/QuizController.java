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
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

/**
 * Reproduces the Symfony backoffice inline hierarchy:
 *   Quiz card
 *     └── [Sélectionner] → "+ Nouvelle Question" + question cards inline below
 *           └── [Sélectionner] → "+ Nouvelle Option" + option rows inline below
 */
public class QuizController {

    @FXML private VBox mainContainer;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceOption serviceOption = new ServiceOption();

    // Track which quiz/question is currently expanded
    private Integer expandedQuizId = null;
    private Integer expandedQuestionId = null;

    @FXML
    public void initialize() {
        chargerTout();
    }

    // ── Full rebuild ──────────────────────────────────────────────────────────

    private void chargerTout() {
        mainContainer.getChildren().clear();
        List<Quiz> quizzes = serviceQuiz.afficher();

        if (quizzes.isEmpty()) {
            Label empty = new Label("Aucun quiz. Créez-en un avec « + Nouveau Quiz ».");
            empty.setStyle("-fx-text-fill:rgba(245,245,244,0.4); -fx-font-size:13; -fx-padding:24;");
            mainContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < quizzes.size(); i++) {
            Quiz quiz = quizzes.get(i);
            mainContainer.getChildren().add(buildQuizBlock(quiz, i + 1));
        }
    }

    // ── Quiz block (card + optional questions inline) ─────────────────────────

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
        badge.getStyleClass().add("badge-" + quiz.getEtat().toLowerCase());

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

        Button btnSelect = new Button(isExpanded ? "✓ Sélectionné" : "✓ Sélectionner");
        btnSelect.getStyleClass().add(isExpanded ? "btn-add" : "btn-select");
        btnSelect.setStyle(isExpanded
            ? "-fx-font-size:12px; -fx-padding:6 12;"
            : "-fx-font-size:12px; -fx-padding:6 12;");
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

        Button btnVoir = new Button("⊙ Voir");
        btnVoir.getStyleClass().add("btn-view");
        btnVoir.setStyle("-fx-font-size:12px; -fx-padding:6 12;");
        btnVoir.setOnAction(e -> voirQuiz(quiz));

        row1.getChildren().addAll(btnSelect, btnVoir);

        // Row 2: Modifier + Supprimer (full width like Symfony)
        HBox row2 = new HBox(4);
        row2.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.getStyleClass().add("btn-edit");
        btnEdit.setStyle("-fx-font-size:12px; -fx-padding:6 14; -fx-min-width:100;");
        btnEdit.setOnAction(e -> ouvrirFormModification(quiz));

        Button btnDel = new Button("🗑 Supprimer");
        btnDel.getStyleClass().add("btn-delete");
        btnDel.setStyle("-fx-font-size:12px; -fx-padding:6 14; -fx-min-width:100;");
        btnDel.setOnAction(e -> supprimerQuiz(quiz));

        row2.getChildren().addAll(btnEdit, btnDel);
        right.getChildren().addAll(row1, row2);

        card.getChildren().addAll(left, right);
        return card;
    }

    // ── Questions section (inline below quiz) ─────────────────────────────────

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

        Button btnSelect = new Button(isExpanded ? "✓ Sélectionné" : "✓ Sélectionner");
        btnSelect.getStyleClass().add(isExpanded ? "btn-add" : "btn-select");
        btnSelect.setStyle("-fx-font-size:11px; -fx-padding:5 10;");
        btnSelect.setOnAction(e -> {
            expandedQuestionId = isExpanded ? null : q.getId();
            chargerTout();
        });

        Button btnVoir = new Button("⊙ Voir");
        btnVoir.getStyleClass().add("btn-view");
        btnVoir.setStyle("-fx-font-size:11px; -fx-padding:5 10;");
        btnVoir.setOnAction(e -> voirQuestion(q));

        row1.getChildren().addAll(btnSelect, btnVoir);

        HBox row2 = new HBox(4);
        row2.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.getStyleClass().add("btn-edit");
        btnEdit.setStyle("-fx-font-size:11px; -fx-padding:5 12;");
        btnEdit.setOnAction(e -> ouvrirFormQuestionDialog(q, quizId));

        Button btnDel = new Button("🗑 Supprimer");
        btnDel.getStyleClass().add("btn-delete");
        btnDel.setStyle("-fx-font-size:11px; -fx-padding:5 12;");
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

    // ── Options section (inline below question) ───────────────────────────────

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

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.getStyleClass().add("btn-edit");
        btnEdit.setStyle("-fx-font-size:11px; -fx-padding:4 10;");
        btnEdit.setOnAction(e -> ouvrirFormOptionDialog(opt, questionId));

        Button btnDel = new Button("🗑");
        btnDel.getStyleClass().add("btn-delete");
        btnDel.setStyle("-fx-font-size:11px; -fx-padding:4 8;");
        btnDel.setOnAction(e -> supprimerOption(opt));

        row.getChildren().addAll(num, texte, badge, btnEdit, btnDel);
        return row;
    }

    // ── Quiz CRUD ─────────────────────────────────────────────────────────────

    @FXML
    public void ouvrirFormAjout() {
        naviguerVersFormulaire(null);
    }

    private void ouvrirFormModification(Quiz quiz) {
        naviguerVersFormulaire(quiz);
    }

    /** Navigate to the full-page form (like Symfony /quiz/new or /quiz/{id}/edit) */
    private void naviguerVersFormulaire(Quiz quiz) {
        try {
            StackPane contentArea =
                (StackPane) mainContainer.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/form_page.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            QuizFormPageController ctrl = loader.getController();
            if (quiz != null) ctrl.initEdit(quiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerQuiz(Quiz quiz) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le quiz « " + quiz.getTitre() + " » ?\nToutes ses questions seront supprimées.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuiz.supprimer(quiz);
                if (expandedQuizId != null && expandedQuizId == quiz.getId()) {
                    expandedQuizId = null;
                    expandedQuestionId = null;
                }
                chargerTout();
            }
        });
    }

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

    // ── Question CRUD ─────────────────────────────────────────────────────────

    private void ouvrirFormQuestionDialog(Question question, int quizId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/question_form.fxml"));
            VBox formNode = loader.load();
            QuestionFormController ctrl = loader.getController();
            ctrl.init(question, quizId, this::chargerTout);

            Stage dialog = new Stage();
            dialog.initOwner(mainContainer.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setTitle(question == null ? "Nouvelle Question" : "Modifier Question");
            javafx.scene.Scene scene = new javafx.scene.Scene(formNode);
            scene.setFill(javafx.scene.paint.Color.web("#0a0f0d"));
            dialog.setScene(scene);
            ctrl.setStage(dialog);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerQuestion(Question q) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuestion.supprimer(q);
                if (expandedQuestionId != null && expandedQuestionId == q.getId()) {
                    expandedQuestionId = null;
                }
                chargerTout();
            }
        });
    }

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

    // ── Option CRUD ───────────────────────────────────────────────────────────

    private void ouvrirFormOptionDialog(Option option, int questionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/option_form.fxml"));
            VBox formNode = loader.load();
            OptionFormController ctrl = loader.getController();
            ctrl.init(option, questionId, this::chargerTout);

            Stage dialog = new Stage();
            dialog.initOwner(mainContainer.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setTitle(option == null ? "Nouvelle Option" : "Modifier Option");
            javafx.scene.Scene scene = new javafx.scene.Scene(formNode);
            scene.setFill(javafx.scene.paint.Color.web("#0a0f0d"));
            dialog.setScene(scene);
            ctrl.setStage(dialog);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerOption(Option opt) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette option ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceOption.supprimer(opt);
                chargerTout();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

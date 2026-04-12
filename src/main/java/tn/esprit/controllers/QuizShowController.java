package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

/**
 * Controller de la page de détails d'un quiz (show.fxml).
 * Affiche les informations d'un quiz sélectionné.
 * Permet aussi de modifier ou supprimer le quiz depuis cette page.
 */
public class QuizShowController {

    // ── Composants FXML (liés aux éléments de show.fxml) ─────────────────────
    @FXML private Label labelTitre;      // grand titre en haut (nom du quiz)
    @FXML private Label labelSubtitle;   // sous-titre "Quiz #id"
    @FXML private Label labelTitreVal;   // valeur du titre dans le corps
    @FXML private Label labelDescVal;    // valeur de la description
    @FXML private Label labelEtatBadge;  // badge coloré de l'état (actif, inactif...)

    // Service pour les opérations BDD sur les quiz
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    // Le quiz actuellement affiché
    private Quiz quiz;

    // Callback appelé quand on revient en arrière (pour rafraîchir la liste)
    private Runnable onBack;

    // ── Initialisation : appelée depuis QuizController avec le quiz à afficher ─
    public void init(Quiz quiz, Runnable onBack) {
        this.quiz = quiz;
        this.onBack = onBack;

        // Remplir les labels avec les données du quiz
        labelTitre.setText(quiz.getTitre());
        labelSubtitle.setText("Quiz #" + quiz.getId());
        labelTitreVal.setText(quiz.getTitre());
        labelDescVal.setText(quiz.getDescription());

        // Afficher le badge d'état avec la couleur correspondante
        labelEtatBadge.setText("● " + capitalize(quiz.getEtat()));
        String etat = quiz.getEtat().toLowerCase();
        String badgeStyle = switch (etat) {
            case "actif"     -> "-fx-background-color:rgba(16,185,129,0.15); -fx-text-fill:#22c55e;";
            case "inactif"   -> "-fx-background-color:rgba(245,158,11,0.15); -fx-text-fill:#eab308;";
            case "brouillon" -> "-fx-background-color:rgba(59,130,246,0.15); -fx-text-fill:#0ea5e9;";
            default          -> "-fx-background-color:rgba(71,85,105,0.3); -fx-text-fill:rgba(245,245,244,0.45);";
        };
        labelEtatBadge.setStyle(badgeStyle +
            "-fx-background-radius:20px; -fx-padding:3 10; -fx-font-size:12px; -fx-font-weight:bold;");
    }

    // ── Retour : revenir à la liste des quiz ──────────────────────────────────
    @FXML
    public void retour() {
        navigateToQuizList();
    }

    // ── Modifier : ouvrir le formulaire de modification pour ce quiz ──────────
    @FXML
    public void modifier() {
        try {
            StackPane contentArea = (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            // Charger le formulaire de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/quiz_form.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            // Passer le quiz au formulaire pour pré-remplir les champs
            QuizFormController ctrl = loader.getController();
            ctrl.initEdit(quiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Supprimer : demander confirmation puis supprimer le quiz ─────────────
    @FXML
    public void supprimer() {
        // Afficher une boîte de dialogue de confirmation avant de supprimer
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le quiz « " + quiz.getTitre() + " » ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuiz.supprimer(quiz); // supprimer en BDD
                navigateToQuizList();        // retourner à la liste
            }
        });
    }

    // ── Navigation vers la liste des quiz ────────────────────────────────────
    private void navigateToQuizList() {
        try {
            StackPane contentArea = (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Met la première lettre en majuscule (ex: "actif" → "Actif")
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

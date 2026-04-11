package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

public class QuizShowController {

    @FXML private Label labelTitre;
    @FXML private Label labelSubtitle;
    @FXML private Label labelTitreVal;
    @FXML private Label labelDescVal;
    @FXML private Label labelEtatBadge;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quiz;
    private Runnable onBack;

    public void init(Quiz quiz, Runnable onBack) {
        this.quiz = quiz;
        this.onBack = onBack;

        labelTitre.setText(quiz.getTitre());
        labelSubtitle.setText("Quiz #" + quiz.getId());
        labelTitreVal.setText(quiz.getTitre());
        labelDescVal.setText(quiz.getDescription());

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

    @FXML
    public void retour() {
        navigateToQuizList();
    }

    @FXML
    public void modifier() {
        try {
            StackPane contentArea =
                (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/backoffice/quiz/form_page.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            QuizFormPageController ctrl = loader.getController();
            ctrl.initEdit(quiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void supprimer() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le quiz « " + quiz.getTitre() + " » ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuiz.supprimer(quiz);
                navigateToQuizList();
            }
        });
    }

    private void navigateToQuizList() {
        try {
            StackPane contentArea =
                (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

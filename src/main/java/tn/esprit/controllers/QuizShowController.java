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
        labelEtatBadge.getStyleClass().add("badge-" + quiz.getEtat().toLowerCase());
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

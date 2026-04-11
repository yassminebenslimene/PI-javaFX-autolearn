package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Question;
import tn.esprit.services.ServiceQuestion;

public class QuestionFormController {

    @FXML private Label formTitle;
    @FXML private TextArea texteField;
    @FXML private TextField pointField;
    @FXML private Label messageLabel;

    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private Question questionAModifier = null;
    private int quizId;
    private Runnable onSuccess;
    private Stage stage;

    public void init(Question question, int quizId, Runnable onSuccess) {
        this.quizId = quizId;
        this.onSuccess = onSuccess;
        if (question != null) {
            this.questionAModifier = question;
            formTitle.setText("Modifier la Question");
            texteField.setText(question.getTexteQuestion());
            pointField.setText(String.valueOf(question.getPoint()));
        } else {
            formTitle.setText("Nouvelle Question");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void sauvegarder() {
        String texte = texteField.getText();
        String pointStr = pointField.getText();

        String erreur = valider(texte, pointStr);
        if (erreur != null) {
            messageLabel.setText(erreur);
            messageLabel.getStyleClass().removeAll("msg-success");
            messageLabel.getStyleClass().add("msg-error");
            return;
        }

        int point = Integer.parseInt(pointStr.trim());

        if (questionAModifier == null) {
            serviceQuestion.ajouter(new Question(texte.trim(), point, null, quizId));
        } else {
            questionAModifier.setTexteQuestion(texte.trim());
            questionAModifier.setPoint(point);
            serviceQuestion.modifier(questionAModifier);
        }

        if (onSuccess != null) onSuccess.run();
        if (stage != null) stage.close();
    }

    @FXML
    public void annuler() {
        if (stage != null) stage.close();
    }

    private String valider(String texte, String pointStr) {
        if (texte == null || texte.trim().isEmpty())
            return "Le texte de la question est obligatoire.";
        if (texte.trim().length() < 10 || texte.trim().length() > 1000)
            return "La question doit contenir entre 10 et 1000 caractères.";
        if (pointStr == null || pointStr.trim().isEmpty())
            return "Le nombre de points est obligatoire.";
        try {
            int p = Integer.parseInt(pointStr.trim());
            if (p < 1 || p > 100) return "Les points doivent être entre 1 et 100.";
        } catch (NumberFormatException e) {
            return "Le nombre de points doit être un entier.";
        }
        return null;
    }
}

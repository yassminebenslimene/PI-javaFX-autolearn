package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.Exercice;

public class ExerciceFormController {

    @FXML private TextField txtId;
    @FXML private TextField txtQuestion;
    @FXML private TextField txtReponse;
    @FXML private TextField txtPoints;
    @FXML private Label errorQuestion;
    @FXML private Label errorReponse;
    @FXML private Label errorPoints;
    @FXML private Label dialogTitle;

    private Exercice exercice;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        // Validation des points (chiffres uniquement)
        txtPoints.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtPoints.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
        if (exercice != null) {
            isEditMode = true;
            dialogTitle.setText("Edit Exercise");
            txtId.setText(String.valueOf(exercice.getId()));
            txtQuestion.setText(exercice.getQuestion());
            txtReponse.setText(exercice.getReponse());
            txtPoints.setText(String.valueOf(exercice.getPoints()));
        } else {
            isEditMode = false;
            dialogTitle.setText("Add Exercise");
            this.exercice = new Exercice();
        }
    }

    public Exercice getExercice() {
        return exercice;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public boolean validateFields() {
        boolean isValid = true;

        // Reset errors
        errorQuestion.setText("");
        errorReponse.setText("");
        errorPoints.setText("");

        String question = txtQuestion.getText().trim();
        if (question.isEmpty()) {
            errorQuestion.setText("La question ne peut pas être vide");
            isValid = false;
        }

        String reponse = txtReponse.getText().trim();
        if (reponse.isEmpty()) {
            errorReponse.setText("La réponse ne peut pas être vide");
            isValid = false;
        }

        String points = txtPoints.getText().trim();
        if (points.isEmpty()) {
            errorPoints.setText("Les points ne peuvent pas être vides");
            isValid = false;
        } else {
            try {
                int pointsValue = Integer.parseInt(points);
                if (pointsValue <= 0) {
                    errorPoints.setText("Les points doivent être un nombre positif");
                    isValid = false;
                } else {
                    exercice.setPoints(pointsValue);
                }
            } catch (NumberFormatException e) {
                errorPoints.setText("Les points doivent être un nombre valide");
                isValid = false;
            }
        }

        if (isValid) {
            exercice.setQuestion(question);
            exercice.setReponse(reponse);
        }

        return isValid;
    }
}

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
    private javafx.stage.Stage dialogStage;

    // Style par défaut pour les champs
    private final String DEFAULT_STYLE = "-fx-background-color:rgba(255,255,255,0.08); " +
            "-fx-border-color:rgba(255,255,255,0.15); " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:10 14 10 14; -fx-font-size:13; " +
            "-fx-text-fill:white;";

    // Style d'erreur pour les champs
    private final String ERROR_STYLE = "-fx-background-color:rgba(239,68,68,0.08); " +
            "-fx-border-color:#ef4444; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:10 14 10 14; -fx-font-size:13; " +
            "-fx-text-fill:white;";

    @FXML
    public void initialize() {
        // Validation des points (chiffres uniquement)
        txtPoints.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtPoints.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // Effacer les erreurs quand l'utilisateur commence à taper
        txtQuestion.textProperty().addListener((obs, oldVal, newVal) -> {
            errorQuestion.setText("");
            txtQuestion.setStyle(DEFAULT_STYLE);
        });

        txtReponse.textProperty().addListener((obs, oldVal, newVal) -> {
            errorReponse.setText("");
            txtReponse.setStyle(DEFAULT_STYLE);
        });

        txtPoints.textProperty().addListener((obs, oldVal, newVal) -> {
            errorPoints.setText("");
            txtPoints.setStyle(DEFAULT_STYLE);
        });
    }

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
        if (exercice != null) {
            isEditMode = true;
            dialogTitle.setText("Modifier l'exercice");
            txtId.setText(String.valueOf(exercice.getId()));
            txtQuestion.setText(exercice.getQuestion());
            txtReponse.setText(exercice.getReponse());
            txtPoints.setText(String.valueOf(exercice.getPoints()));
        } else {
            isEditMode = false;
            dialogTitle.setText("Ajouter un exercice");
            this.exercice = new Exercice();
        }
    }

    public void setDialogStage(javafx.stage.Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public boolean validateFields() {
        boolean isValid = true;

        // Reset errors et styles
        errorQuestion.setText("");
        errorReponse.setText("");
        errorPoints.setText("");

        txtQuestion.setStyle(DEFAULT_STYLE);
        txtReponse.setStyle(DEFAULT_STYLE);
        txtPoints.setStyle(DEFAULT_STYLE);

        // Validation Question
        String question = txtQuestion.getText().trim();
        if (question.isEmpty()) {
            errorQuestion.setText("⚠ La question ne peut pas être vide");
            txtQuestion.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (question.length() < 3) {
            errorQuestion.setText("⚠ La question doit contenir au moins 3 caractères");
            txtQuestion.setStyle(ERROR_STYLE);
            isValid = false;
        } else {
            exercice.setQuestion(question);
        }

        // Validation Réponse
        String reponse = txtReponse.getText().trim();
        if (reponse.isEmpty()) {
            errorReponse.setText("⚠ La réponse ne peut pas être vide");
            txtReponse.setStyle(ERROR_STYLE);
            isValid = false;
        } else {
            exercice.setReponse(reponse);
        }

        // Validation Points
        String points = txtPoints.getText().trim();
        if (points.isEmpty()) {
            errorPoints.setText("⚠ Les points ne peuvent pas être vides");
            txtPoints.setStyle(ERROR_STYLE);
            isValid = false;
        } else {
            try {
                int pointsValue = Integer.parseInt(points);
                if (pointsValue <= 0) {
                    errorPoints.setText("⚠ Les points doivent être supérieurs à 0");
                    txtPoints.setStyle(ERROR_STYLE);
                    isValid = false;
                } else if (pointsValue > 100) {
                    errorPoints.setText("⚠ Les points ne peuvent pas dépasser 100");
                    txtPoints.setStyle(ERROR_STYLE);
                    isValid = false;
                } else {
                    exercice.setPoints(pointsValue);
                }
            } catch (NumberFormatException e) {
                errorPoints.setText("⚠ Les points doivent être un nombre valide");
                txtPoints.setStyle(ERROR_STYLE);
                isValid = false;
            }
        }

        return isValid;
    }
}
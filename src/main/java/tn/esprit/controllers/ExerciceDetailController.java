package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.entities.Exercice;

public class ExerciceDetailController {

    @FXML private Label lblQuestion;
    @FXML private Label lblReponse;
    @FXML private Label lblPoints;

    public void setExercice(Exercice exercice) {
        lblQuestion.setText(exercice.getQuestion());
        lblReponse.setText(exercice.getReponse());
        lblPoints.setText(String.valueOf(exercice.getPoints()));
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) lblQuestion.getScene().getWindow();
        stage.close();
    }
}
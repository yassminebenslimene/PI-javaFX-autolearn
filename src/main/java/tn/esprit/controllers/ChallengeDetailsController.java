package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import tn.esprit.entities.Challenge;

public class ChallengeDetailsController {

    @FXML private Label lblTitre;
    @FXML private Label lblDescription;
    @FXML private Label lblNiveau;
    @FXML private Label lblDuree;
    @FXML private Label lblDateDebut;
    @FXML private Label lblDateFin;

    public void setChallenge(Challenge challenge) {
        lblTitre.setText(challenge.getTitre());
        lblDescription.setText(challenge.getDescription());
        lblNiveau.setText(challenge.getNiveau());
        lblDuree.setText(challenge.getDuree() + " minutes");
        lblDateDebut.setText(challenge.getDateDebut().toString());
        lblDateFin.setText(challenge.getDateFin().toString());
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) lblTitre.getScene().getWindow();
        stage.close();
    }
}
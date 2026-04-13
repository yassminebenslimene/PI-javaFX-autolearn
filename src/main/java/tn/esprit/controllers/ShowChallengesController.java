package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.services.ChallengeService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class ShowChallengesController {

    @FXML private FlowPane challengesContainer;
    @FXML private Label messageLabel;

    private ChallengeService challengeService;
    private List<Challenge> allChallenges;

    @FXML
    public void initialize() {
        challengeService = new ChallengeService();
        loadChallenges();
    }

    private void loadChallenges() {
        allChallenges = challengeService.getAll();
        displayChallenges();
    }

    private void displayChallenges() {
        challengesContainer.getChildren().clear();

        LocalDate today = LocalDate.now();

        for (Challenge c : allChallenges) {
            // Vérifier si le challenge est encore valide (date fin non dépassée)
            boolean isExpired = c.getDateFin().isBefore(today);

            VBox card = createChallengeCard(c, isExpired);
            challengesContainer.getChildren().add(card);
        }
    }

    private VBox createChallengeCard(Challenge challenge, boolean isExpired) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color:white; -fx-background-radius:15; " +
                "-fx-border-color:#eeeeee; -fx-border-radius:15; " +
                "-fx-padding:20; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,0);");
        card.setPrefWidth(350);

        Label titleLabel = new Label(challenge.getTitre());
        titleLabel.setStyle("-fx-font-size:18; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");

        Label descriptionLabel = new Label(challenge.getDescription());
        descriptionLabel.setStyle("-fx-font-size:13; -fx-text-fill:#777; -fx-wrap-text:true;");
        descriptionLabel.setMaxWidth(310);

        Label levelLabel = new Label("⭐ " + challenge.getNiveau());
        levelLabel.setStyle("-fx-background-color:#f0f0f0; -fx-text-fill:#777; " +
                "-fx-padding:4 12 4 12; -fx-background-radius:20; -fx-font-size:12;");

        Label dateLabel = new Label("📅 " + challenge.getDateDebut() + " → " + challenge.getDateFin());
        dateLabel.setStyle("-fx-font-size:12; -fx-text-fill:#999;");

        javafx.scene.control.Button actionButton = new javafx.scene.control.Button();

        if (isExpired) {
            actionButton.setText("🔒 Challenge expiré");
            actionButton.setStyle("-fx-background-color:#ccc; -fx-text-fill:#666; " +
                    "-fx-padding:10 20 10 20; -fx-background-radius:25; -fx-font-weight:700;");
            actionButton.setDisable(true);
        } else {
            actionButton.setText("🚀 Voir le challenge");
            actionButton.setStyle("-fx-background-color:linear-gradient(to right,#7a6ad8,#4e3b9c); " +
                    "-fx-text-fill:white; -fx-padding:10 20 10 20; -fx-background-radius:25; " +
                    "-fx-font-weight:700; -fx-cursor:hand;");
            actionButton.setOnAction(e -> openChallengeDetail(challenge));
        }

        card.getChildren().addAll(titleLabel, descriptionLabel, levelLabel, dateLabel, actionButton);
        return card;
    }

    private void openChallengeDetail(Challenge challenge) {
        try {
            // NE PAS définir le contrôleur ici car il est déjà dans le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/challenge.fxml"));
            javafx.scene.Parent root = loader.load();

            // Récupérer le contrôleur après le chargement
            ChallengeDetailController controller = loader.getController();
            controller.setChallenge(challenge);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onHome() {
        try {
            MainApp.showFrontoffice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
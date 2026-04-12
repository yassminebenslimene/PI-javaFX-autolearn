package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.services.VoteService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ResultChallengeController {

    @FXML private Label challengeTitle;
    @FXML private Label scoreLabel;
    @FXML private Label percentageLabel;
    @FXML private Label dateDebutLabel;
    @FXML private Label dateFinLabel;
    @FXML private Label niveauLabel;
    @FXML private HBox starsContainer;
    @FXML private Label ratingMessage;

    private Challenge challenge;
    private int score;
    private int totalPoints;
    private VoteService voteService;
    private int userRating = 0;

    // Flag pour savoir si les données sont prêtes
    private boolean isReady = false;

    @FXML
    public void initialize() {
        // Cette méthode est appelée APRÈS que tous les @FXML soient injectés
        System.out.println("ResultChallengeController initialisé");

        // Si le challenge a déjà été défini avant initialize(), on affiche
        if (challenge != null) {
            displayInfo();
            loadUserRating();
        } else {
            isReady = true;
        }
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        voteService = new VoteService();

        // Si le contrôleur est déjà initialisé, on affiche immédiatement
        if (challengeTitle != null) {
            displayInfo();
            loadUserRating();
        }
        // Sinon, on attend initialize()
    }

    public void setScore(int score, int totalPoints) {
        this.score = score;
        this.totalPoints = totalPoints;

        // Si déjà initialisé, mettre à jour l'affichage
        if (scoreLabel != null && challengeTitle != null) {
            updateScoreDisplay();
        }
    }

    private void updateScoreDisplay() {
        int percentage = totalPoints > 0 ? (score * 100 / totalPoints) : 0;
        scoreLabel.setText(score + "/" + totalPoints);
        percentageLabel.setText(percentage + "%");
    }

    private void displayInfo() {
        // Vérification de sécurité
        if (challengeTitle == null) {
            System.out.println("Attention: challengeTitle est null, affichage différé");
            return;
        }

        challengeTitle.setText(challenge.getTitre());

        int percentage = totalPoints > 0 ? (score * 100 / totalPoints) : 0;
        if (scoreLabel != null) {
            scoreLabel.setText(score + "/" + totalPoints);
            percentageLabel.setText(percentage + "%");
        }

        dateDebutLabel.setText(challenge.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateFinLabel.setText(challenge.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        niveauLabel.setText(challenge.getNiveau());
    }

    private void loadUserRating() {
        if (ratingMessage == null || starsContainer == null) {
            System.out.println("Composants rating non initialisés");
            return;
        }

        Integer rating = voteService.getUserRatingForChallenge(
                SessionManager.getCurrentUser().getId(), challenge.getId());
        if (rating != null) {
            userRating = rating;
            displayStars(rating);
            ratingMessage.setText("Vous avez déjà noté ce challenge " + rating + "/5");
            ratingMessage.setStyle("-fx-text-fill:#28a745; -fx-font-size:12;");
        } else {
            createClickableStars();
        }
    }

    private void createClickableStars() {
        if (starsContainer == null) return;

        starsContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            final int starValue = i;
            Label star = new Label("☆");
            star.setStyle("-fx-font-size:32; -fx-text-fill:#ddd; -fx-cursor:hand;");
            star.setOnMouseClicked(e -> submitRating(starValue));
            starsContainer.getChildren().add(star);
        }
    }

    private void displayStars(int rating) {
        if (starsContainer == null) return;

        starsContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= rating ? "★" : "☆");
            star.setStyle("-fx-font-size:32; -fx-text-fill:" + (i <= rating ? "#f1c40f" : "#ddd") + ";");
            starsContainer.getChildren().add(star);
        }
    }

    private void submitRating(int rating) {
        boolean success = voteService.saveOrUpdateVote(
                SessionManager.getCurrentUser().getId(), challenge.getId(), rating);

        if (success) {
            userRating = rating;
            displayStars(rating);
            ratingMessage.setText("Merci pour votre évaluation ! (" + rating + "/5)");
            ratingMessage.setStyle("-fx-text-fill:#28a745; -fx-font-size:12;");
        } else {
            ratingMessage.setText("Erreur lors de l'enregistrement de votre note");
            ratingMessage.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:12;");
        }
    }

    @FXML
    public void onBackToChallenges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onQuit() {
        onBackToChallenges();
    }
}
package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.services.VoteService;
import tn.esprit.services.QuoteService;
import tn.esprit.services.GiphyService;
import tn.esprit.session.SessionManager;

import java.io.IOException;

public class ResultChallengeController {

    @FXML private Label challengeTitle;
    @FXML private Label scoreLabel;
    @FXML private Label percentageLabel;
    @FXML private Label niveauLabel;
    @FXML private HBox starsContainer;
    @FXML private Label ratingMessage;
    @FXML private TextArea aiAnalysisArea;
    @FXML private Label congratsQuoteLabel;
    @FXML private ImageView reactionGif;

    private QuoteService quoteService;
    private GiphyService giphyService;
    private Challenge challenge;
    private int score;
    private int totalPoints;
    private VoteService voteService;
    private int userRating = 0;
    private boolean isReady = false;

    @FXML
    public void initialize() {
        System.out.println("ResultChallengeController initialisé");
        quoteService = new QuoteService();
        giphyService = new GiphyService();

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
        if (challengeTitle != null) {
            displayInfo();
            loadUserRating();
        }
    }

    public void setScore(int score, int totalPoints) {
        this.score = score;
        this.totalPoints = totalPoints;
        if (scoreLabel != null && challengeTitle != null) {
            updateScoreDisplay();
            displayCongratulationQuote();
            loadReactionGif();
        }
    }

    public void setAIAnalysis(String analysis) {
        if (aiAnalysisArea != null && analysis != null && !analysis.isEmpty()) {
            aiAnalysisArea.setText(analysis);
            aiAnalysisArea.setVisible(true);
            aiAnalysisArea.setManaged(true);
            System.out.println("Analyse IA chargée, longueur: " + analysis.length());
        } else {
            System.out.println("Analyse IA non disponible ou vide");
        }
    }

    private void updateScoreDisplay() {
        int percentage = totalPoints > 0 ? (score * 100 / totalPoints) : 0;
        scoreLabel.setText(score + "/" + totalPoints);
        percentageLabel.setText(percentage + "%");
    }

    private void displayInfo() {
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
        niveauLabel.setText(challenge.getNiveau());
    }

    private void displayCongratulationQuote() {
        if (congratsQuoteLabel != null && quoteService != null) {
            String quote = quoteService.getCongratulationQuote(score, totalPoints);
            congratsQuoteLabel.setText(quote);
            congratsQuoteLabel.setVisible(true);
            congratsQuoteLabel.setManaged(true);
            System.out.println("Citation affichée: " + quote);
        }
    }

    private void loadReactionGif() {
        if (reactionGif == null) {
            System.err.println("reactionGif est null");
            return;
        }

        int percentage = (score * 100) / totalPoints;
        String gifPath;

        if (percentage >= 80) {
            gifPath = "/images/Well Done Success.gif";
            System.out.println("🎉 Score excellent (" + percentage + "%) - GIF: Well Done Applause");
        } else if (percentage >= 50) {
            gifPath = "/images/Well Done Applause.gif";
            System.out.println("👍 Bon score (" + percentage + "%) - GIF: Well Done Success");
        } else {
            gifPath = "/images/Stay Strong Never Give Up.gif";
            System.out.println("💪 Score à améliorer (" + percentage + "%) - GIF: Stay Strong Never Give Up");
        }

        try {
            // Charger le GIF depuis les ressources
            java.io.InputStream inputStream = getClass().getResourceAsStream(gifPath);
            if (inputStream != null) {
                Image gifImage = new Image(inputStream);
                reactionGif.setImage(gifImage);
                reactionGif.setVisible(true);
                reactionGif.setManaged(true);
                reactionGif.setFitWidth(250);
                reactionGif.setFitHeight(180);
                reactionGif.setPreserveRatio(true);
                System.out.println("✅ GIF chargé avec succès: " + gifPath);
            } else {
                System.err.println("❌ Fichier non trouvé: " + gifPath);
                System.err.println("   Vérifiez que le chemin est correct et que le fichier existe");
                reactionGif.setVisible(false);
                showTextReaction(percentage);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement GIF: " + e.getMessage());
            reactionGif.setVisible(false);
            showTextReaction(percentage);
        }
    }

    private void showTextReaction(int percentage) {
        VBox parent = (VBox) reactionGif.getParent();
        if (parent != null) {
            // Supprimer l'ancien message s'il existe
            parent.getChildren().removeIf(node -> node instanceof Label && node.getStyle().contains("-fx-font-size:20"));

            Label fallbackLabel = new Label();
            fallbackLabel.setWrapText(true);
            fallbackLabel.setAlignment(javafx.geometry.Pos.CENTER);

            if (percentage >= 80) {
                fallbackLabel.setText("🎉🏆🌟 FÉLICITATIONS ! 🌟🏆🎉");
                fallbackLabel.setStyle("-fx-font-size:18; -fx-text-fill:#f1c40f; -fx-font-weight:bold; -fx-padding:15;");
            } else if (percentage >= 50) {
                fallbackLabel.setText("👍💪 BON TRAVAIL ! 💪👍");
                fallbackLabel.setStyle("-fx-font-size:18; -fx-text-fill:#34d399; -fx-font-weight:bold; -fx-padding:15;");
            } else {
                fallbackLabel.setText("📚💪 CONTINUE À T'ENTRAÎNER ! 💪📚");
                fallbackLabel.setStyle("-fx-font-size:18; -fx-text-fill:#f97316; -fx-font-weight:bold; -fx-padding:15;");
            }

            parent.getChildren().add(1, fallbackLabel);
            System.out.println("✅ Message de secours affiché");
        }
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
package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
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
import tn.esprit.services.UserChallengeService;
import tn.esprit.session.JwtManager;

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
    @FXML private VBox containerCorrectionIA;
    @FXML private VBox containerResumePedago;
    @FXML private Label labelResumeGeneral;
    @FXML private VBox containerPointsForts;
    @FXML private VBox listPointsForts;
    @FXML private VBox containerPointsAmeliorer;
    @FXML private VBox listPointsAmeliorer;
    @FXML private Label labelEncouragement;
    @FXML private VBox containerExplications;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private MenuButton menuUser;

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

        // Afficher les infos utilisateur dans la navbar
        var u = JwtManager.getCurrentUser();
        if (u != null) {
            String name = u.getPrenom() + " " + u.getNom();
            if (labelCurrentUser != null) labelCurrentUser.setText(name);

            String initials = u.getPrenom().substring(0,1).toUpperCase()
                    + u.getNom().substring(0,1).toUpperCase();
            if (labelAvatarNav != null) labelAvatarNav.setText(initials);
            
            // Afficher le niveau
            if (labelNiveauUser != null) {
                tn.esprit.services.ServiceQuiz quizService = new tn.esprit.services.ServiceQuiz();
                String niveau = quizService.getTitreNiveau(u.getId());
                labelNiveauUser.setText("Niveau : " + niveau);
            }
        }

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
        if (analysis == null || analysis.isEmpty()) {
            System.out.println("Analyse IA non disponible ou vide");
            return;
        }

        try {
            // Parse the analysis text and create beautiful cards
            parseAndDisplayCorrection(analysis);
            
            // Show the correction container
            if (containerCorrectionIA != null) {
                containerCorrectionIA.setVisible(true);
                containerCorrectionIA.setManaged(true);
            }
            
            System.out.println("Analyse IA chargée et affichée en cartes");
        } catch (Exception e) {
            // Fallback to TextArea if parsing fails
            System.err.println("Erreur parsing IA: " + e.getMessage());
            if (aiAnalysisArea != null) {
                aiAnalysisArea.setText(analysis);
                aiAnalysisArea.setVisible(true);
                aiAnalysisArea.setManaged(true);
            }
        }
    }

    private void parseAndDisplayCorrection(String analysis) {
        // Parse the text analysis and extract information
        String[] sections = analysis.split("═{70,}");
        
        int questionNum = 1;
        
        // Parse each exercise section
        String[] lines = analysis.split("\n");
        boolean inExercise = false;
        String currentQuestion = "";
        String currentAnswer = "";
        String currentScore = "";
        String currentFeedback = "";
        String currentPointsForts = "";
        String currentPointsManques = "";
        String currentConseil = "";
        boolean isCorrect = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.contains("EXERCICE N°")) {
                // Save previous exercise if exists
                if (inExercise && !currentQuestion.isEmpty()) {
                    addQuestionCard(questionNum - 1, currentQuestion, currentAnswer, currentScore, 
                                  currentFeedback, currentPointsForts, currentPointsManques, 
                                  currentConseil, isCorrect);
                }
                
                // Start new exercise
                inExercise = true;
                questionNum++;
                currentQuestion = "";
                currentAnswer = "";
                currentScore = "";
                currentFeedback = "";
                currentPointsForts = "";
                currentPointsManques = "";
                currentConseil = "";
                isCorrect = false;
            } else if (line.startsWith("│ ❓")) {
                currentQuestion = line.substring(line.indexOf("❓") + 1).trim();
            } else if (line.contains("VOTRE RÉPONSE :")) {
                // Next line will be the answer
            } else if (line.startsWith("│") && !line.contains("SCORE") && !line.contains("FEEDBACK") 
                      && !line.contains("POINTS FORTS") && !line.contains("À AMÉLIORER") 
                      && !line.contains("CONSEIL") && currentAnswer.isEmpty() && !currentQuestion.isEmpty()) {
                currentAnswer = line.substring(1).trim();
            } else if (line.contains("SCORE :")) {
                currentScore = line.substring(line.indexOf("SCORE :") + 7).trim();
                // Check if correct (percentage >= 80%)
                if (line.contains("%")) {
                    try {
                        String pct = line.substring(line.indexOf("(") + 1, line.indexOf("%"));
                        isCorrect = Integer.parseInt(pct.trim()) >= 80;
                    } catch (Exception ignored) {}
                }
            } else if (line.contains("FEEDBACK IA :")) {
                // Next line will be feedback
            } else if (line.startsWith("│") && currentFeedback.isEmpty() && !currentScore.isEmpty()) {
                String content = line.substring(1).trim();
                if (!content.contains("POINTS FORTS") && !content.contains("À AMÉLIORER") && !content.contains("CONSEIL")) {
                    currentFeedback = content;
                }
            } else if (line.contains("POINTS FORTS :")) {
                currentPointsForts = line.substring(line.indexOf("POINTS FORTS :") + 14).trim();
            } else if (line.contains("À AMÉLIORER :")) {
                currentPointsManques = line.substring(line.indexOf("À AMÉLIORER :") + 13).trim();
            } else if (line.contains("CONSEIL :")) {
                currentConseil = line.substring(line.indexOf("CONSEIL :") + 9).trim();
            } else if (line.contains("BILAN PÉDAGOGIQUE GLOBAL")) {
                // Save last exercise
                if (inExercise && !currentQuestion.isEmpty()) {
                    addQuestionCard(questionNum - 1, currentQuestion, currentAnswer, currentScore,
                                  currentFeedback, currentPointsForts, currentPointsManques,
                                  currentConseil, isCorrect);
                }
                break; // Start parsing global summary
            }
        }
        
        // Save last exercise if not saved
        if (inExercise && !currentQuestion.isEmpty()) {
            addQuestionCard(questionNum - 1, currentQuestion, currentAnswer, currentScore,
                          currentFeedback, currentPointsForts, currentPointsManques,
                          currentConseil, isCorrect);
        }
        
        // Parse global summary
        parseGlobalSummary(analysis);
    }

    private void addQuestionCard(int questionNum, String question, String answer, String score,
                                 String feedback, String pointsForts, String pointsManques,
                                 String conseil, boolean isCorrect) {
        if (containerExplications == null) return;
        
        // Create question card
        VBox questionCard = new VBox(12);
        questionCard.setStyle("-fx-background-color:white; -fx-background-radius:16; " +
                            "-fx-border-color:#e2e8f0; -fx-border-radius:16; -fx-border-width:1; " +
                            "-fx-padding:20 24 20 24;");
        
        // Header with question number and status
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label questionLabel = new Label("Question " + questionNum);
        questionLabel.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#7c3aed;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusLabel = new Label(isCorrect ? "✓ Correct" : "✗ Incorrect");
        statusLabel.setStyle(isCorrect ? 
            "-fx-background-color:#d1fae5; -fx-text-fill:#059669; -fx-font-size:13; " +
            "-fx-font-weight:700; -fx-padding:6 14 6 14; -fx-background-radius:12;" :
            "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626; -fx-font-size:13; " +
            "-fx-font-weight:700; -fx-padding:6 14 6 14; -fx-background-radius:12;");
        
        header.getChildren().addAll(questionLabel, spacer, statusLabel);
        
        // Question text
        Label questionText = new Label(question);
        questionText.setStyle("-fx-font-size:16; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        questionText.setWrapText(true);
        
        // Answer box
        VBox answerBox = new VBox(8);
        answerBox.setStyle(isCorrect ?
            "-fx-background-color:#d1fae5; -fx-background-radius:12; -fx-padding:14 18 14 18; " +
            "-fx-border-color:#6ee7b7; -fx-border-radius:12; -fx-border-width:1;" :
            "-fx-background-color:#fef3c7; -fx-background-radius:12; -fx-padding:14 18 14 18; " +
            "-fx-border-color:#fcd34d; -fx-border-radius:12; -fx-border-width:1;");
        
        HBox answerHeader = new HBox(8);
        answerHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label answerIcon = new Label(isCorrect ? "✓" : "📝");
        answerIcon.setStyle("-fx-font-size:18;");
        Label answerTitle = new Label(isCorrect ? "Bonne réponse" : "Votre réponse");
        answerTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#0f172a;");
        answerHeader.getChildren().addAll(answerIcon, answerTitle);
        
        Label answerText = new Label(answer.isEmpty() ? "(non répondue)" : answer);
        answerText.setStyle("-fx-font-size:14; -fx-text-fill:#1e293b;");
        answerText.setWrapText(true);
        
        answerBox.getChildren().addAll(answerHeader, answerText);
        
        // Explanation box
        VBox explanationBox = new VBox(10);
        explanationBox.setStyle("-fx-background-color:#f8f7ff; -fx-background-radius:12; " +
                              "-fx-padding:16 20 16 20;");
        
        Label explanationTitle = new Label("💬  Explication de votre professeur IA");
        explanationTitle.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#7c3aed;");
        
        Label feedbackText = new Label(feedback);
        feedbackText.setStyle("-fx-font-size:14; -fx-text-fill:#1e293b;");
        feedbackText.setWrapText(true);
        
        explanationBox.getChildren().addAll(explanationTitle, feedbackText);
        
        // Points forts (if any)
        if (!pointsForts.isEmpty()) {
            VBox fortsBox = new VBox(6);
            fortsBox.setStyle("-fx-background-color:#d1fae5; -fx-background-radius:10; -fx-padding:12 16 12 16;");
            Label fortsTitle = new Label("✓  Pourquoi c'est correct :");
            fortsTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#059669;");
            Label fortsText = new Label(pointsForts);
            fortsText.setStyle("-fx-font-size:13; -fx-text-fill:#065f46;");
            fortsText.setWrapText(true);
            fortsBox.getChildren().addAll(fortsTitle, fortsText);
            explanationBox.getChildren().add(fortsBox);
        }
        
        // Recommendation (if any)
        if (!conseil.isEmpty()) {
            VBox conseilBox = new VBox(6);
            conseilBox.setStyle("-fx-background-color:#fef3c7; -fx-background-radius:10; -fx-padding:12 16 12 16;");
            Label conseilTitle = new Label("🔸  Recommandation :");
            conseilTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#d97706;");
            Label conseilText = new Label(conseil);
            conseilText.setStyle("-fx-font-size:13; -fx-text-fill:#92400e; -fx-font-style:italic;");
            conseilText.setWrapText(true);
            conseilBox.getChildren().addAll(conseilTitle, conseilText);
            explanationBox.getChildren().add(conseilBox);
        }
        
        questionCard.getChildren().addAll(header, questionText, answerBox, explanationBox);
        containerExplications.getChildren().add(questionCard);
    }

    private void parseGlobalSummary(String analysis) {
        // Extract global summary information
        String[] lines = analysis.split("\n");
        boolean inSummary = false;
        StringBuilder messageGeneral = new StringBuilder();
        java.util.List<String> pointsForts = new java.util.ArrayList<>();
        java.util.List<String> pointsAmeliorer = new java.util.ArrayList<>();
        String encouragement = "";
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.contains("BILAN PÉDAGOGIQUE GLOBAL")) {
                inSummary = true;
                continue;
            }
            
            if (!inSummary) continue;
            
            if (line.startsWith("🎯")) {
                messageGeneral.append(line.substring(2).trim());
            } else if (line.contains("POINTS FORTS")) {
                // Next lines are points forts
            } else if (line.startsWith("•") && pointsAmeliorer.isEmpty()) {
                pointsForts.add(line.substring(1).trim());
            } else if (line.contains("À AMÉLIORER")) {
                // Next lines are points to improve
            } else if (line.startsWith("•") && !pointsForts.isEmpty()) {
                pointsAmeliorer.add(line.substring(1).trim());
            } else if (line.startsWith("🚀")) {
                encouragement = line.substring(2).trim();
            }
        }
        
        // Display global summary
        if (containerResumePedago != null && !messageGeneral.toString().isEmpty()) {
            if (labelResumeGeneral != null) {
                labelResumeGeneral.setText(messageGeneral.toString());
            }
            
            if (!pointsForts.isEmpty() && listPointsForts != null && containerPointsForts != null) {
                for (String point : pointsForts) {
                    Label pointLabel = new Label("• " + point);
                    pointLabel.setStyle("-fx-font-size:13; -fx-text-fill:#065f46;");
                    pointLabel.setWrapText(true);
                    listPointsForts.getChildren().add(pointLabel);
                }
                containerPointsForts.setVisible(true);
                containerPointsForts.setManaged(true);
            }
            
            if (!pointsAmeliorer.isEmpty() && listPointsAmeliorer != null && containerPointsAmeliorer != null) {
                for (String point : pointsAmeliorer) {
                    Label pointLabel = new Label("• " + point);
                    pointLabel.setStyle("-fx-font-size:13; -fx-text-fill:#92400e;");
                    pointLabel.setWrapText(true);
                    listPointsAmeliorer.getChildren().add(pointLabel);
                }
                containerPointsAmeliorer.setVisible(true);
                containerPointsAmeliorer.setManaged(true);
            }
            
            if (!encouragement.isEmpty() && labelEncouragement != null) {
                labelEncouragement.setText(encouragement);
            }
            
            containerResumePedago.setVisible(true);
            containerResumePedago.setManaged(true);
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
                JwtManager.getCurrentUser().getId(), challenge.getId());
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
            star.setStyle("-fx-font-size:36; -fx-text-fill:#ddd; -fx-cursor:hand;");
            star.setOnMouseEntered(e -> star.setStyle("-fx-font-size:36; -fx-text-fill:#f1c40f; -fx-cursor:hand;"));
            star.setOnMouseExited(e -> star.setStyle("-fx-font-size:36; -fx-text-fill:#ddd; -fx-cursor:hand;"));
            star.setOnMouseClicked(e -> submitRating(starValue));
            starsContainer.getChildren().add(star);
        }
    }

    private void displayStars(int rating) {
        if (starsContainer == null) return;
        starsContainer.getChildren().clear();
        for (int i = 1; i <= 5; i++) {
            Label star = new Label(i <= rating ? "★" : "☆");
            star.setStyle("-fx-font-size:36; -fx-text-fill:" + (i <= rating ? "#f1c40f" : "#ddd") + ";");
            starsContainer.getChildren().add(star);
        }
    }

    private void submitRating(int rating) {
        boolean success = voteService.saveOrUpdateVote(
                JwtManager.getCurrentUser().getId(), challenge.getId(), rating);
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
    public void onRetryChallenge() {
        if (challenge == null) {
            onBackToChallenges();
            return;
        }
        
        try {
            // Delete previous attempt
            int userId = JwtManager.getCurrentUser().getId();
            UserChallengeService userChallengeService = new UserChallengeService();
            tn.esprit.entities.UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(userId, challenge.getId());
            if (userChallenge != null) {
                userChallengeService.delete(userChallenge.getId());
            }

            // Start fresh challenge
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/playchallenge.fxml"));
            javafx.scene.Parent root = loader.load();

            tn.esprit.controllers.PlayChallengeController controller = loader.getController();
            controller.setChallenge(challenge);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onQuit() {
        onBackToChallenges();
    }

    // ========== MÉTHODES DE LA NAVBAR ==========

    @FXML
    public void onHome() {
        try {
            MainApp.showFrontoffice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onCours() {
        try {
            MainApp.showCoursPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onLeaderboard() {
        try {
            MainApp.showLeaderboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onEvenements() {
        try {
            MainApp.showEvenementsFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onCommunaute() {
        try {
            MainApp.showCommunauteFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onMessagerie() {
        try {
            MainApp.showFrontoffice();
            javafx.application.Platform.runLater(() ->
                tn.esprit.controllers.FrontofficeController.navigateToSection("messagerie"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onProfile() {
        try {
            MainApp.showProfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onMesParticipations() {
        try {
            MainApp.showMesParticipations(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onMesEquipes() {
        try {
            MainApp.showMesEquipes(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onLogout() {
        JwtManager.logout();
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.UserChallenge;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.UserChallengeService;
import tn.esprit.services.VoteService;
import tn.esprit.services.ServiceQuiz;
import tn.esprit.session.JwtManager;

import java.io.IOException;
import java.util.List;

public class ShowChallengesController {

    @FXML private FlowPane challengesContainer;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private MenuButton menuUser;

    private ChallengeService challengeService;
    private UserChallengeService userChallengeService;
    private VoteService voteService;
    private List<Challenge> allChallenges;

    @FXML
    public void initialize() {
        challengeService = new ChallengeService();
        userChallengeService = new UserChallengeService();
        voteService = new VoteService();

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
                ServiceQuiz quizService = new ServiceQuiz();
                String niveau = quizService.getTitreNiveau(u.getId());
                labelNiveauUser.setText("Niveau : " + niveau);
            }
        }

        loadChallenges();
    }

    private void loadChallenges() {
        allChallenges = challengeService.getAll();
        displayChallenges();
    }

    private void displayChallenges() {
        challengesContainer.getChildren().clear();

        int currentUserId = JwtManager.getCurrentUser() != null ?
                JwtManager.getCurrentUser().getId() : -1;

        // Color palette for cards (matching event style)
        String[][] palette = {
            {"#7a6ad8", "#ede9ff"},
            {"#10b981", "#dcfce7"},
            {"#f59e0b", "#fef3c7"},
            {"#6366f1", "#e0e7ff"},
            {"#ec4899", "#fce7f3"},
            {"#0ea5e9", "#e0f2fe"},
        };

        for (int i = 0; i < allChallenges.size(); i++) {
            Challenge c = allChallenges.get(i);
            String[] colors = palette[i % palette.length];
            
            // Check if user has completed this challenge
            UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(currentUserId, c.getId());
            
            double averageRating = voteService.getAverageRatingForChallenge(c.getId());
            Integer userRating = voteService.getUserRatingForChallenge(currentUserId, c.getId());

            VBox card = createChallengeCard(c, userChallenge, averageRating, userRating, colors[0], colors[1]);
            challengesContainer.getChildren().add(card);
        }
    }

    private VBox createChallengeCard(Challenge challenge, UserChallenge userChallenge, 
                                     double averageRating, Integer userRating,
                                     String primaryColor, String lightColor) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white; -fx-background-radius:24; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),25,0,0,8); " +
                "-fx-padding:0;");
        card.setPrefWidth(400);
        card.setMaxWidth(400);

        // Header with gradient background
        VBox header = new VBox(14);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + primaryColor + "," + lightColor + "); " +
                "-fx-background-radius:24 24 0 0; -fx-padding:28 28 24 28;");
        
        // Status badge
        Label statusBadge = new Label();
        if (userChallenge != null && userChallenge.isCompleted()) {
            statusBadge.setText("✓ Terminé");
            statusBadge.setStyle("-fx-background-color:rgba(255,255,255,0.95); -fx-text-fill:#059669; " +
                    "-fx-font-size:12; -fx-font-weight:800; -fx-padding:6 16 6 16; " +
                    "-fx-background-radius:20;");
        } else if (userChallenge != null && userChallenge.getCurrentIndex() > 0) {
            statusBadge.setText("⏸ En cours");
            statusBadge.setStyle("-fx-background-color:rgba(255,255,255,0.95); -fx-text-fill:#d97706; " +
                    "-fx-font-size:12; -fx-font-weight:800; -fx-padding:6 16 6 16; " +
                    "-fx-background-radius:20;");
        } else {
            statusBadge.setText("🆕 Nouveau");
            statusBadge.setStyle("-fx-background-color:rgba(255,255,255,0.95); -fx-text-fill:#6366f1; " +
                    "-fx-font-size:12; -fx-font-weight:800; -fx-padding:6 16 6 16; " +
                    "-fx-background-radius:20;");
        }
        
        Label titleLabel = new Label(challenge.getTitre());
        titleLabel.setStyle("-fx-font-size:22; -fx-font-weight:900; -fx-text-fill:white; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),5,0,0,2);");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(340);
        
        header.getChildren().addAll(statusBadge, titleLabel);

        // Body
        VBox body = new VBox(18);
        body.setStyle("-fx-padding:24 28 28 28;");

        Label descriptionLabel = new Label(challenge.getDescription());
        descriptionLabel.setStyle("-fx-font-size:14; -fx-text-fill:#64748b; -fx-wrap-text:true; -fx-line-spacing:2;");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(340);
        descriptionLabel.setMaxHeight(65);

        // Info row
        HBox infoRow = new HBox(14);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        Label levelLabel = new Label("⭐ " + challenge.getNiveau());
        levelLabel.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#475569; " +
                "-fx-padding:8 16 8 16; -fx-background-radius:14; -fx-font-size:13; -fx-font-weight:800;");
        
        Label durationLabel = new Label("⏱ " + challenge.getDuree() + " min");
        durationLabel.setStyle("-fx-background-color:#f1f5f9; -fx-text-fill:#475569; " +
                "-fx-padding:8 16 8 16; -fx-background-radius:14; -fx-font-size:13; -fx-font-weight:800;");
        
        infoRow.getChildren().addAll(levelLabel, durationLabel);

        // Rating display
        HBox ratingBox = createRatingDisplay(averageRating, userRating);

        // Separator
        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
        separator.setStyle("-fx-background-color:#e2e8f0; -fx-padding:8 0 8 0;");

        // Action buttons
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER);
        
        if (userChallenge != null && userChallenge.isCompleted()) {
            // Show results and retry buttons
            Button viewResultsBtn = new Button("📊 Voir résultats");
            viewResultsBtn.setStyle("-fx-background-color:" + primaryColor + "; -fx-text-fill:white; " +
                    "-fx-padding:14 28 14 28; -fx-background-radius:30; " +
                    "-fx-font-weight:800; -fx-font-size:13; -fx-cursor:hand; -fx-border-width:0; " +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);");
            viewResultsBtn.setOnAction(e -> viewChallengeResults(challenge));
            
            Button retryBtn = new Button("↺ Refaire");
            retryBtn.setStyle("-fx-background-color:white; -fx-text-fill:" + primaryColor + "; " +
                    "-fx-padding:14 28 14 28; -fx-background-radius:30; " +
                    "-fx-font-weight:800; -fx-font-size:13; -fx-cursor:hand; " +
                    "-fx-border-color:" + primaryColor + "; -fx-border-width:2.5; -fx-border-radius:30;");
            retryBtn.setOnAction(e -> retryChallenge(challenge));
            
            actionButtons.getChildren().addAll(viewResultsBtn, retryBtn);
        } else if (userChallenge != null && userChallenge.getCurrentIndex() > 0) {
            // Continue button
            Button continueBtn = new Button("▶ Continuer");
            continueBtn.setStyle("-fx-background-color:" + primaryColor + "; -fx-text-fill:white; " +
                    "-fx-padding:14 36 14 36; -fx-background-radius:30; " +
                    "-fx-font-weight:800; -fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; " +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);");
            continueBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(continueBtn, javafx.scene.layout.Priority.ALWAYS);
            continueBtn.setOnAction(e -> startChallenge(challenge));
            
            actionButtons.getChildren().add(continueBtn);
        } else {
            // Start button
            Button startBtn = new Button("🚀 Commencer");
            startBtn.setStyle("-fx-background-color:" + primaryColor + "; -fx-text-fill:white; " +
                    "-fx-padding:14 36 14 36; -fx-background-radius:30; " +
                    "-fx-font-weight:800; -fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; " +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),10,0,0,3);");
            startBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(startBtn, javafx.scene.layout.Priority.ALWAYS);
            startBtn.setOnAction(e -> startChallenge(challenge));
            
            actionButtons.getChildren().add(startBtn);
        }

        body.getChildren().addAll(descriptionLabel, infoRow, ratingBox, separator, actionButtons);
        card.getChildren().addAll(header, body);
        
        return card;
    }

    private HBox createRatingDisplay(double averageRating, Integer userRating) {
        HBox ratingBox = new HBox(8);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        // Étoiles pour la note moyenne
        HBox starsBox = new HBox(2);
        starsBox.setAlignment(Pos.CENTER_LEFT);

        int fullStars = (int) Math.floor(averageRating);
        boolean hasHalfStar = (averageRating - fullStars) >= 0.5;

        for (int i = 1; i <= 5; i++) {
            Label star = new Label();
            if (i <= fullStars) {
                star.setText("★");
                star.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:16;");
            } else if (i == fullStars + 1 && hasHalfStar) {
                star.setText("★");
                star.setStyle("-fx-text-fill:#fcd34d; -fx-font-size:16;");
            } else {
                star.setText("☆");
                star.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:16;");
            }
            starsBox.getChildren().add(star);
        }

        // Label avec la note moyenne
        Label ratingLabel = new Label(String.format("%.1f", averageRating));
        ratingLabel.setStyle("-fx-font-size:13; -fx-text-fill:#64748b; -fx-font-weight:700;");

        // Si l'utilisateur a voté
        if (userRating != null && userRating > 0) {
            Label userRatingLabel = new Label("(Votre note: " + userRating + "/5)");
            userRatingLabel.setStyle("-fx-font-size:11; -fx-text-fill:#10b981; -fx-font-weight:600;");
            ratingBox.getChildren().addAll(starsBox, ratingLabel, userRatingLabel);
        } else {
            Label votesLabel = new Label("(Votes: " + (int)(averageRating * 10) + ")");
            votesLabel.setStyle("-fx-font-size:11; -fx-text-fill:#94a3b8;");
            ratingBox.getChildren().addAll(starsBox, ratingLabel, votesLabel);
        }

        return ratingBox;
    }

    private void startChallenge(Challenge challenge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/playchallenge.fxml"));
            javafx.scene.Parent root = loader.load();

            PlayChallengeController controller = loader.getController();
            controller.setChallenge(challenge);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void viewChallengeResults(Challenge challenge) {
        try {
            int userId = JwtManager.getCurrentUser().getId();
            UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(userId, challenge.getId());
            
            if (userChallenge == null || !userChallenge.isCompleted()) {
                System.err.println("Challenge not completed yet");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();

            ResultChallengeController controller = loader.getController();
            controller.setChallenge(challenge);
            controller.setScore(userChallenge.getScore(), userChallenge.getTotalPoints());
            // Note: AI analysis would need to be regenerated or stored

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retryChallenge(Challenge challenge) {
        try {
            // Delete previous attempt
            int userId = JwtManager.getCurrentUser().getId();
            UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(userId, challenge.getId());
            if (userChallenge != null) {
                userChallengeService.delete(userChallenge.getId());
            }

            // Start fresh
            startChallenge(challenge);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            MainApp.showFrontoffice();
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
            MainApp.showFrontoffice();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onMesEquipes() {
        try {
            MainApp.showFrontoffice();
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

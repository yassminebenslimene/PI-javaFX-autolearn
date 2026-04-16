package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.VoteService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class ShowChallengesController {

    @FXML private FlowPane challengesContainer;
    @FXML private Label messageLabel;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label welcomeLabel;

    private ChallengeService challengeService;
    private VoteService voteService;
    private List<Challenge> allChallenges;

    @FXML
    public void initialize() {
        challengeService = new ChallengeService();
        voteService = new VoteService();

        // Afficher les infos utilisateur dans la navbar
        var u = SessionManager.getCurrentUser();
        if (u != null) {
            String name = u.getPrenom() + " " + u.getNom();
            if (labelCurrentUser != null) labelCurrentUser.setText(name);

            String initials = u.getPrenom().substring(0,1).toUpperCase()
                    + u.getNom().substring(0,1).toUpperCase();
            if (labelAvatarNav != null) labelAvatarNav.setText(initials);
        }

        loadChallenges();
    }

    private void loadChallenges() {
        allChallenges = challengeService.getAll();
        displayChallenges();
    }

    private void displayChallenges() {
        challengesContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        int currentUserId = SessionManager.getCurrentUser() != null ?
                SessionManager.getCurrentUser().getId() : -1;

        for (Challenge c : allChallenges) {
            boolean isExpired = c.getDateFin().isBefore(today);
            // Récupérer la note moyenne du challenge
            double averageRating = voteService.getAverageRatingForChallenge(c.getId());
            // Récupérer la note de l'utilisateur connecté (s'il a voté)
            Integer userRating = voteService.getUserRatingForChallenge(currentUserId, c.getId());

            VBox card = createChallengeCard(c, isExpired, averageRating, userRating);
            challengesContainer.getChildren().add(card);
        }
    }

    private VBox createChallengeCard(Challenge challenge, boolean isExpired,
                                     double averageRating, Integer userRating) {
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

        // === SECTION NOTE (RATING) ===
        HBox ratingBox = createRatingDisplay(averageRating, userRating);

        // === BOUTON ACTION ===
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

        card.getChildren().addAll(titleLabel, descriptionLabel, levelLabel, dateLabel, ratingBox, actionButton);
        return card;
    }

    private HBox createRatingDisplay(double averageRating, Integer userRating) {
        HBox ratingBox = new HBox(10);
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
                star.setStyle("-fx-text-fill:#f1c40f; -fx-font-size:14;");
            } else if (i == fullStars + 1 && hasHalfStar) {
                star.setText("½");
                star.setStyle("-fx-text-fill:#f1c40f; -fx-font-size:14;");
            } else {
                star.setText("☆");
                star.setStyle("-fx-text-fill:#ddd; -fx-font-size:14;");
            }
            starsBox.getChildren().add(star);
        }

        // Label avec la note moyenne
        Label ratingLabel = new Label(String.format("%.1f/5", averageRating));
        ratingLabel.setStyle("-fx-font-size:12; -fx-text-fill:#777; -fx-font-weight:600;");

        // Si l'utilisateur a voté, afficher sa note
        if (userRating != null && userRating > 0) {
            Label userRatingLabel = new Label(" (Votre note: " + userRating + "/5)");
            userRatingLabel.setStyle("-fx-font-size:11; -fx-text-fill:#28a745; -fx-font-weight:600;");
            ratingBox.getChildren().addAll(starsBox, ratingLabel, userRatingLabel);
        } else {
            ratingBox.getChildren().addAll(starsBox, ratingLabel);
        }

        return ratingBox;
    }

    private void openChallengeDetail(Challenge challenge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/challenge.fxml"));
            javafx.scene.Parent root = loader.load();

            ChallengeDetailController controller = loader.getController();
            controller.setChallenge(challenge);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
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
    public void onEvenements() {
        try {
            MainApp.showEvenementsFront();
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
    public void onLogout() {
        SessionManager.logout();
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
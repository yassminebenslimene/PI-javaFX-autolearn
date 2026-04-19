package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.VoteService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowChallengesController {

    @FXML private FlowPane challengesContainer;
    @FXML private Label messageLabel;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label welcomeLabel;

    // Composants du calendrier
    @FXML private GridPane calendarGrid;
    @FXML private Label monthYearLabel;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;

    private ChallengeService challengeService;
    private VoteService voteService;
    private List<Challenge> allChallenges;
    private LocalDate currentDate = LocalDate.now();
    private Map<LocalDate, List<Challenge>> challengesByDate = new HashMap<>();

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
        updateCalendar();
    }

    private void loadChallenges() {
        allChallenges = challengeService.getAll();
        groupChallengesByDate();
        displayChallenges();
    }

    private void groupChallengesByDate() {
        challengesByDate.clear();
        for (Challenge c : allChallenges) {
            // Ajouter le challenge pour chaque jour entre date début et date fin
            LocalDate start = c.getDateDebut();
            LocalDate end = c.getDateFin();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                challengesByDate.computeIfAbsent(current, k -> new ArrayList<>()).add(c);
                current = current.plusDays(1);
            }
        }
    }

    private void displayChallenges() {
        challengesContainer.getChildren().clear();

        LocalDate today = LocalDate.now();
        int currentUserId = SessionManager.getCurrentUser() != null ?
                SessionManager.getCurrentUser().getId() : -1;

        for (Challenge c : allChallenges) {
            boolean isExpired = c.getDateFin().isBefore(today);
            double averageRating = voteService.getAverageRatingForChallenge(c.getId());
            Integer userRating = voteService.getUserRatingForChallenge(currentUserId, c.getId());

            VBox card = createChallengeCard(c, isExpired, averageRating, userRating);
            challengesContainer.getChildren().add(card);
        }
    }

    private String getChallengeStatus(Challenge challenge) {
        LocalDate today = LocalDate.now();
        LocalDate start = challenge.getDateDebut();
        LocalDate end = challenge.getDateFin();

        if (end.isBefore(today)) {
            return "expired";
        } else if (start.isAfter(today)) {
            long daysUntilStart = ChronoUnit.DAYS.between(today, start);
            if (daysUntilStart <= 3) {
                return "expiring_soon";
            }
            return "upcoming";
        } else {
            return "active";
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "expired": return "#e94560";      // Rouge - expiré
            case "active": return "#7a6ad8";       // Bleu - en cours
            case "upcoming": return "#fbbf24";     // Jaune - bientôt
            case "expiring_soon": return "#f59e0b"; // Orange - expire bientôt
            default: return "#cccccc";
        }
    }

    private void updateCalendar() {
        if (calendarGrid == null) return;

        YearMonth yearMonth = YearMonth.from(currentDate);
        monthYearLabel.setText(yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        // Nettoyer la grille (garder les en-têtes des jours lignes 0)
        calendarGrid.getChildren().removeIf(node ->
                GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

        LocalDate firstOfMonth = yearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() - 1;

        int row = 1;
        int col = dayOfWeek;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(8);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPrefWidth(100);
        cell.setPrefHeight(120);
        cell.setStyle("-fx-background-color:#f9f9ff; -fx-border-color:#eeeeee; -fx-border-radius:8; -fx-background-radius:8; -fx-padding:10;");

        // Numéro du jour
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:#1e1e1e;");

        cell.getChildren().add(dayLabel);

        // Ajouter les challenges de ce jour
        List<Challenge> dayChallenges = challengesByDate.get(date);
        if (dayChallenges != null && !dayChallenges.isEmpty()) {
            VBox challengesBox = new VBox(5);
            challengesBox.setAlignment(Pos.TOP_CENTER);

            for (Challenge c : dayChallenges) {
                String status = getChallengeStatus(c);
                String color = getStatusColor(status);

                // Cadre pour chaque challenge
                VBox challengeBox = new VBox(2);
                challengeBox.setStyle("-fx-background-color:" + color + "20; -fx-border-color:" + color + "; " +
                        "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:5; -fx-border-width:1;");
                challengeBox.setPrefWidth(85);
                challengeBox.setAlignment(Pos.CENTER);

                // Titre du challenge (tronqué si trop long)
                String title = c.getTitre();
                if (title.length() > 15) {
                    title = title.substring(0, 12) + "...";
                }
                Label titleLabel = new Label(title);
                titleLabel.setStyle("-fx-font-size:10; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
                titleLabel.setAlignment(Pos.CENTER);
                titleLabel.setWrapText(true);

                // Dates
                Label dateRangeLabel = new Label(formatDateRange(c.getDateDebut(), c.getDateFin()));
                dateRangeLabel.setStyle("-fx-font-size:8; -fx-text-fill:#666;");
                dateRangeLabel.setAlignment(Pos.CENTER);

                challengeBox.getChildren().addAll(titleLabel, dateRangeLabel);

                // Tooltip pour voir le titre complet
                Tooltip tooltip = new Tooltip(c.getTitre() + "\n📅 " + formatDateRange(c.getDateDebut(), c.getDateFin()));
                Tooltip.install(challengeBox, tooltip);

                // Click pour ouvrir le challenge
                challengeBox.setOnMouseClicked(e -> openChallengeDetail(c));
                challengeBox.setCursor(javafx.scene.Cursor.HAND);

                challengesBox.getChildren().add(challengeBox);
            }
            cell.getChildren().add(challengesBox);
        }

        // Style pour le jour actuel
        if (date.equals(LocalDate.now())) {
            cell.setStyle("-fx-background-color:#7a6ad8; -fx-border-color:#7a6ad8; -fx-background-radius:8; -fx-border-radius:8; -fx-padding:10;");
            dayLabel.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:white;");
        }

        return cell;
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        return start.format(formatter) + " → " + end.format(formatter);
    }

    @FXML
    private void previousMonth() {
        currentDate = currentDate.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void nextMonth() {
        currentDate = currentDate.plusMonths(1);
        updateCalendar();
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

        HBox ratingBox = createRatingDisplay(averageRating, userRating);

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

        Label ratingLabel = new Label(String.format("%.1f/5", averageRating));
        ratingLabel.setStyle("-fx-font-size:12; -fx-text-fill:#777; -fx-font-weight:600;");

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
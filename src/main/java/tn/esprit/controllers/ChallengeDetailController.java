package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.UserChallenge;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.UserChallengeService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ChallengeDetailController {

    @FXML private Label challengeTitle;
    @FXML private Label dateDebutLabel;
    @FXML private Label dateFinLabel;
    @FXML private Label niveauLabel;
    @FXML private Label dureeLabel;
    @FXML private VBox resultContainer;
    @FXML private Label scoreLabel;
    @FXML private Label completedAtLabel;
    @FXML private Button startButton;

    private Challenge challenge;
    private ChallengeService challengeService;
    private UserChallengeService userChallengeService;

    @FXML
    public void initialize() {
        System.out.println("ChallengeDetailController initialisé");
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        this.challengeService = new ChallengeService();
        this.userChallengeService = new UserChallengeService();

        javafx.application.Platform.runLater(() -> {
            if (challengeTitle == null) {
                System.err.println("ERREUR: challengeTitle est null - Vérifiez l'ID dans le FXML");
                return;
            }
            displayChallengeInfo();
            checkIfCompleted();
        });
    }

    private void displayChallengeInfo() {
        challengeTitle.setText(challenge.getTitre());

        if (dateDebutLabel != null) {
            dateDebutLabel.setText(challenge.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (dateFinLabel != null) {
            dateFinLabel.setText(challenge.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (niveauLabel != null) {
            niveauLabel.setText(challenge.getNiveau());
        }
        if (dureeLabel != null) {
            dureeLabel.setText(challenge.getDuree() + " minutes");
        }
    }

    private void checkIfCompleted() {
        if (SessionManager.getCurrentUser() == null) return;

        UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(
                SessionManager.getCurrentUser().getId(), challenge.getId());

        if (userChallenge != null && userChallenge.isCompleted()) {
            if (resultContainer != null) {
                resultContainer.setVisible(true);
                resultContainer.setManaged(true);
            }
            if (scoreLabel != null) {
                scoreLabel.setText(userChallenge.getScore() + "/" + userChallenge.getTotalPoints());
            }
            if (completedAtLabel != null && userChallenge.getCompletedAt() != null) {
                completedAtLabel.setText("Terminé le " + userChallenge.getCompletedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            if (startButton != null) {
                startButton.setText("📊 Voir mon résultat");
                startButton.setOnAction(e -> openResult());
            }
        } else if (challenge.getDateFin().isBefore(LocalDate.now())) {
            if (startButton != null) {
                startButton.setText("🔒 Challenge expiré");
                startButton.setDisable(true);
            }
        } else {
            if (startButton != null) {
                startButton.setText("🚀 Commencer le challenge");
                startButton.setOnAction(e -> startChallenge());
            }
        }
    }

    private void startChallenge() {
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

    private void openResult() {
        try {
            // Charger le FXML - NE PAS créer manuellement le contrôleur
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();

            // Récupérer le contrôleur créé automatiquement par FXMLLoader
            ResultChallengeController controller = loader.getController();

            // Passer les données au contrôleur
            controller.setChallenge(challenge);

            // Récupérer et passer le score
            UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(
                    SessionManager.getCurrentUser().getId(), challenge.getId());

            if (userChallenge != null) {
                controller.setScore(userChallenge.getScore(), userChallenge.getTotalPoints());
            }

            // Changer la vue
            MainApp.getPrimaryStage().getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de resultchallenge.fxml: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
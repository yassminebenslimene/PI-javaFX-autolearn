package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;
import tn.esprit.entities.Quiz;
import tn.esprit.entities.UserChallenge;
import tn.esprit.services.ExerciceService;
import tn.esprit.services.ServiceQuiz;
import tn.esprit.services.UserChallengeService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayChallengeController {

    @FXML private Label timerLabel;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label questionLabel;
    @FXML private TextField answerField;
    @FXML private Label pointsLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;

    private Challenge challenge;
    private List<Exercice> exercices;
    private List<Quiz> quizzes;
    private List<Object> allQuestions; // Pour mélanger exercices et quiz
    private Map<Integer, String> answers;
    private Map<Integer, Integer> quizAnswers; // Pour stocker les réponses des quiz (score)
    private int currentIndex = 0;
    private int score = 0;
    private Timeline timer;
    private int remainingSeconds;
    private UserChallengeService userChallengeService;
    private ExerciceService exerciceService;
    private ServiceQuiz quizService;

    @FXML
    public void initialize() {
        System.out.println("PlayChallengeController initialisé");
        quizService = new ServiceQuiz();
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        this.exerciceService = new ExerciceService();
        this.userChallengeService = new UserChallengeService();

        // Récupérer les exercices du challenge
        exercices = new ArrayList<>();
        for (Integer exerciceId : challenge.getExerciceIds()) {
            Exercice e = exerciceService.getById(exerciceId);
            if (e != null) {
                exercices.add(e);
            }
        }

        // Récupérer les quiz du challenge
        quizzes = new ArrayList<>();
        for (Integer quizId : challenge.getQuizIds()) {
            Quiz q = quizService.findById(quizId);
            if (q != null) {
                quizzes.add(q);
            }
        }

        // Combiner exercices et quiz dans une seule liste
        allQuestions = new ArrayList<>();
        allQuestions.addAll(exercices);
        allQuestions.addAll(quizzes);

        answers = new HashMap<>();
        quizAnswers = new HashMap<>();

        // Vérifier si déjà commencé
        UserChallenge existing = userChallengeService.findByUserAndChallenge(
                SessionManager.getCurrentUser().getId(), challenge.getId());
        if (existing != null && !existing.isCompleted()) {
            answers = existing.getAnswersMap();
            currentIndex = existing.getCurrentIndex();
        }

        remainingSeconds = challenge.getDuree() * 60;

        // Attendre que le FXML soit chargé
        Platform.runLater(() -> {
            startTimer();
            displayChallenge();
        });
    }

    private void startTimer() {
        if (timerLabel == null) {
            System.err.println("ERREUR: timerLabel est null");
            return;
        }

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimer() {
        if (remainingSeconds <= 0) {
            timer.stop();
            Platform.runLater(() -> timeOut());
        } else {
            remainingSeconds--;
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    private void timeOut() {
        saveCurrentAnswer();
        calculateFinalScore();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();
            ResultChallengeController controller = loader.getController();
            controller.setChallenge(challenge);
            controller.setScore(score, getTotalPoints());

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayChallenge() {
        if (questionLabel == null) {
            System.err.println("ERREUR: questionLabel est null");
            return;
        }

        if (allQuestions.isEmpty()) {
            questionLabel.setText("Aucun contenu disponible pour ce challenge.");
            answerField.setDisable(true);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }

        updateProgress();
        displayCurrentQuestion();
    }

    private void updateProgress() {
        progressLabel.setText((currentIndex + 1) + "/" + allQuestions.size());
        progressBar.setProgress((double)(currentIndex + 1) / allQuestions.size());

        prevButton.setDisable(currentIndex == 0);

        if (currentIndex == allQuestions.size() - 1) {
            nextButton.setVisible(false);
            nextButton.setManaged(false);
            finishButton.setVisible(true);
            finishButton.setManaged(true);
        } else {
            nextButton.setVisible(true);
            nextButton.setManaged(true);
            finishButton.setVisible(false);
            finishButton.setManaged(false);
        }
    }

    private void displayCurrentQuestion() {
        Object current = allQuestions.get(currentIndex);

        if (current instanceof Exercice) {
            Exercice e = (Exercice) current;
            questionLabel.setText(e.getQuestion());
            pointsLabel.setText("Points: " + e.getPoints());
            answerField.setPromptText("Votre réponse...");
            answerField.setDisable(false);

            String savedAnswer = answers.get(e.getId());
            answerField.setText(savedAnswer != null ? savedAnswer : "");

        } else if (current instanceof Quiz) {
            Quiz q = (Quiz) current;
            questionLabel.setText(q.getTitre() + "\n\n" + q.getDescription());
            pointsLabel.setText("Points: 50");
            answerField.setPromptText("Entrez votre score (0-100) ou laissez vide...");
            answerField.setDisable(false);

            Integer savedScore = quizAnswers.get(q.getId());
            answerField.setText(savedScore != null ? String.valueOf(savedScore) : "");
        }
    }

    private void saveCurrentAnswer() {
        if (currentIndex < allQuestions.size()) {
            Object current = allQuestions.get(currentIndex);
            String answer = answerField.getText();

            if (current instanceof Exercice) {
                answers.put(((Exercice) current).getId(), answer);
            } else if (current instanceof Quiz) {
                try {
                    int quizScore = Integer.parseInt(answer.trim());
                    if (quizScore >= 0 && quizScore <= 100) {
                        quizAnswers.put(((Quiz) current).getId(), quizScore);
                    }
                } catch (NumberFormatException e) {
                    // Si ce n'est pas un nombre, on ne sauvegarde pas
                }
            }

            UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(
                    SessionManager.getCurrentUser().getId(), challenge.getId());
            if (userChallenge == null) {
                userChallenge = new UserChallenge();
                userChallenge.setUserId(SessionManager.getCurrentUser().getId());
                userChallenge.setChallengeId(challenge.getId());
            }
            userChallenge.setAnswersMap(answers);
            userChallenge.setCurrentIndex(currentIndex);
            userChallengeService.save(userChallenge);
        }
    }

    @FXML
    public void onNext() {
        saveCurrentAnswer();
        if (currentIndex < allQuestions.size() - 1) {
            currentIndex++;
            displayCurrentQuestion();
            updateProgress();
        }
    }

    @FXML
    public void onPrev() {
        saveCurrentAnswer();
        if (currentIndex > 0) {
            currentIndex--;
            displayCurrentQuestion();
            updateProgress();
        }
    }

    @FXML
    public void onFinish() {
        saveCurrentAnswer();
        calculateFinalScore();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();
            ResultChallengeController controller = loader.getController();
            controller.setChallenge(challenge);
            controller.setScore(score, getTotalPoints());

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculateFinalScore() {
        score = 0;

        // Calculer le score des exercices
        for (Exercice e : exercices) {
            String userAnswer = answers.get(e.getId());
            if (userAnswer != null && userAnswer.trim().equalsIgnoreCase(e.getReponse().trim())) {
                score += e.getPoints();
            }
        }

        // Calculer le score des quiz
        for (Quiz q : quizzes) {
            Integer quizScore = quizAnswers.get(q.getId());
            if (quizScore != null && quizScore >= 50) { // Seuil de réussite à 50%
                score += 50; // Points pour avoir réussi le quiz
            }
        }
    }

    private int getTotalPoints() {
        int total = 0;
        for (Exercice e : exercices) {
            total += e.getPoints();
        }
        total += quizzes.size() * 50; // 50 points par quiz
        return total;
    }

    @FXML
    public void onQuit() {
        if (timer != null) {
            timer.stop();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
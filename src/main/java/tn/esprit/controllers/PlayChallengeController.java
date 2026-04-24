package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.*;
import tn.esprit.services.*;
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
    @FXML private Label typeBadge;
    @FXML private Label quoteLabel;
    private QuoteService quoteService;
    private Challenge challenge;
    private List<Exercice> exercices;
    private List<Quiz> quizzes;
    private List<Object> allQuestions;
    private Map<Integer, String> exerciceAnswers;
    private Map<Integer, Integer> quizScores;
    private int currentIndex = 0;
    private int score = 0;
    private Timeline timer;
    private int remainingSeconds;
    private UserChallengeService userChallengeService;
    private ExerciceService exerciceService;
    private ServiceQuiz quizService;
    private EmailService emailService;
    private String aiAnalysis;

    @FXML
    public void initialize() {
        System.out.println("PlayChallengeController initialisé");
        quizService = new ServiceQuiz();
        emailService = new EmailService();
        quoteService = new QuoteService();
        exerciceAnswers = new HashMap<>();
        quizScores = new HashMap<>();
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        this.exerciceService = new ExerciceService();
        this.userChallengeService = new UserChallengeService();

        exercices = new ArrayList<>();
        if (challenge.getExerciceIds() != null) {
            for (Integer exerciceId : challenge.getExerciceIds()) {
                Exercice e = exerciceService.getById(exerciceId);
                if (e != null) exercices.add(e);
            }
        }

        quizzes = new ArrayList<>();
        if (challenge.getQuizIds() != null) {
            for (Integer quizId : challenge.getQuizIds()) {
                Quiz q = quizService.findById(quizId);
                if (q != null) quizzes.add(q);
            }
        }

        allQuestions = new ArrayList<>();
        allQuestions.addAll(exercices);
        allQuestions.addAll(quizzes);

        UserChallenge existing = userChallengeService.findByUserAndChallenge(
                SessionManager.getCurrentUser().getId(), challenge.getId());
        if (existing != null && !existing.isCompleted()) {
            currentIndex = existing.getCurrentIndex();
        }

        remainingSeconds = challenge.getDuree() * 60;

        Platform.runLater(() -> {
            startTimer();
            displayChallenge();
            displayMotivationalQuote();
        });
    }
    private void displayMotivationalQuote() {
        if (quoteLabel != null) {
            String quote = quoteService.getMotivationalQuote();
            quoteLabel.setText(quote);
        }
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
            Platform.runLater(() -> finishChallenge());
        } else {
            remainingSeconds--;
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    private void displayChallenge() {
        if (questionLabel == null) return;

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
            typeBadge.setText("📝 EXERCICE");
            typeBadge.setStyle("-fx-background-color:rgba(59,130,246,0.15); -fx-text-fill:#3b82f6; -fx-font-weight:700;");

            Exercice e = (Exercice) current;
            questionLabel.setText(e.getQuestion());
            pointsLabel.setText("Points: " + e.getPoints());

            answerField.setVisible(true);
            answerField.setManaged(true);
            answerField.setPromptText("Votre réponse...");

            String savedAnswer = exerciceAnswers.get(e.getId());
            answerField.setText(savedAnswer != null ? savedAnswer : "");

        } else if (current instanceof Quiz) {
            typeBadge.setText("📋 QUIZ");
            typeBadge.setStyle("-fx-background-color:rgba(168,85,247,0.15); -fx-text-fill:#a855f7; -fx-font-weight:700;");

            Quiz q = (Quiz) current;
            questionLabel.setText("📋 QUIZ: " + q.getTitre() + "\n\n" + q.getDescription());
            pointsLabel.setText("Points: 50");

            answerField.setPromptText("Entrez votre score (0-100)");
            answerField.setVisible(true);
            answerField.setManaged(true);

            Integer savedScore = quizScores.get(q.getId());
            answerField.setText(savedScore != null ? String.valueOf(savedScore) : "");
        }
    }

    private void saveCurrentAnswer() {
        if (currentIndex >= allQuestions.size()) return;

        Object current = allQuestions.get(currentIndex);

        if (current instanceof Exercice) {
            String answer = answerField.getText();
            exerciceAnswers.put(((Exercice) current).getId(), answer);
        } else if (current instanceof Quiz) {
            try {
                int scoreValue = Integer.parseInt(answerField.getText().trim());
                if (scoreValue >= 0 && scoreValue <= 100) {
                    quizScores.put(((Quiz) current).getId(), scoreValue);
                }
            } catch (NumberFormatException e) {
                // Ignorer
            }
        }

        UserChallenge userChallenge = userChallengeService.findByUserAndChallenge(
                SessionManager.getCurrentUser().getId(), challenge.getId());
        if (userChallenge == null) {
            userChallenge = new UserChallenge();
            userChallenge.setUserId(SessionManager.getCurrentUser().getId());
            userChallenge.setChallengeId(challenge.getId());
        }
        userChallenge.setCurrentIndex(currentIndex);
        userChallengeService.save(userChallenge);
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
        calculateScore();
        generateAIAnalysis();
        sendResultEmail();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();
            ResultChallengeController controller = loader.getController();
            controller.setChallenge(challenge);
            controller.setScore(score, getTotalPoints());
            controller.setAIAnalysis(aiAnalysis);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculateScore() {
        score = 0;
        // Score basé sur la présence d'une réponse (pas sur la correction)
        for (Exercice e : exercices) {
            String userAnswer = exerciceAnswers.get(e.getId());
            if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                score += e.getPoints();
            }
        }
        for (Quiz q : quizzes) {
            Integer quizScore = quizScores.get(q.getId());
            if (quizScore != null && quizScore >= 50) {
                score += 50;
            }
        }
    }

    private void generateAIAnalysis() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("║                    🤖 CORRECTION DÉTAILLÉE PAR L'IA                  ║\n");
        analysis.append("╚══════════════════════════════════════════════════════════════════════╝\n\n");

        int questionNumber = 1;
        int correctCount = 0;
        int totalQuestions = exercices.size();

        for (Exercice e : exercices) {
            String userAnswer = exerciceAnswers.get(e.getId());
            String correctAnswer = e.getReponse();
            boolean isCorrect = userAnswer != null && userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());

            if (isCorrect) correctCount++;

            analysis.append("┌─────────────────────────────────────────────────────────────────┐\n");
            analysis.append("│ 📌 QUESTION N°").append(questionNumber).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ ❓ ").append(e.getQuestion()).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 📝 VOTRE RÉPONSE :\n");
            analysis.append("│ ").append(userAnswer != null && !userAnswer.isEmpty() ? userAnswer : "(non répondue)").append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ ✅ RÉPONSE CORRECTE :\n");
            analysis.append("│ ").append(correctAnswer).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");

            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                analysis.append("│ ❌ RÉSULTAT : Aucune réponse fournie\n");
                analysis.append("│\n");
                analysis.append("│ 💡 ANALYSE DU PROFESSEUR :\n");
                analysis.append("│   Vous n'avez pas répondu à cette question. Prenez le temps\n");
                analysis.append("│   de lire attentivement chaque question et d'y répondre.\n");
            } else if (isCorrect) {
                analysis.append("│ ✅ RÉSULTAT : CORRECT !\n");
                analysis.append("│ 🎉 Bravo ! Votre réponse est juste.\n");
                analysis.append("│\n");
                analysis.append("│ 📚 EXPLICATION :\n");
                analysis.append("│   ").append(getPositiveFeedback(e.getQuestion())).append("\n");
            } else {
                analysis.append("│ ❌ RÉSULTAT : INCORRECT\n");
                analysis.append("│\n");
                analysis.append("│ 🔍 ERREURS DÉTECTÉES :\n");
                List<String> errors = detectSpecificErrors(userAnswer, correctAnswer, e.getQuestion());
                for (String error : errors) {
                    analysis.append("│   • ").append(error).append("\n");
                }
                analysis.append("│\n");
                analysis.append("│ 💡 EXPLICATION DU PROFESSEUR :\n");
                analysis.append("│   ").append(getDetailedExplanation(e.getQuestion(), correctAnswer)).append("\n");
                analysis.append("│\n");
                analysis.append("│ 📚 RECOMMANDATIONS :\n");
                List<String> recommendations = getRecommendationsForQuestion(e.getQuestion());
                for (String rec : recommendations) {
                    analysis.append("│   • ").append(rec).append("\n");
                }
            }
            analysis.append("└─────────────────────────────────────────────────────────────────┘\n\n");
            questionNumber++;
        }

        // Résumé final
        int percentage = (correctCount * 100) / totalQuestions;

        analysis.append("\n╔══════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("║                         📊 RÉSUMÉ FINAL                               ║\n");
        analysis.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        analysis.append("\n");
        analysis.append("   ┌─────────────────────────────────────────────────────────────────┐\n");
        analysis.append("   │                                                                 │\n");
        analysis.append("   │   📝 Questions totales : ").append(String.format("%-30d", totalQuestions)).append("│\n");
        analysis.append("   │   ✅ Réponses correctes : ").append(String.format("%-30d", correctCount)).append("│\n");
        analysis.append("   │   ❌ Réponses incorrectes : ").append(String.format("%-28d", (totalQuestions - correctCount))).append("│\n");
        analysis.append("   │   📊 Pourcentage de réussite : ").append(String.format("%-25d", percentage)).append("%│\n");
        analysis.append("   │                                                                 │\n");

        if (percentage >= 80) {
            analysis.append("   │   🎉 FÉLICITATIONS ! Excellent travail !                       │\n");
        } else if (percentage >= 60) {
            analysis.append("   │   👍 BON TRAVAIL ! Continuez comme ça !                         │\n");
        } else if (percentage >= 40) {
            analysis.append("   │   📚 BON COURAGE ! Révisez les points faibles et réessayez !   │\n");
        } else {
            analysis.append("   │   💪 CONTINUEZ À VOUS ENTRAÎNER ! La pratique est la clé !     │\n");
        }
        analysis.append("   │                                                                 │\n");
        analysis.append("   └─────────────────────────────────────────────────────────────────┘\n");

        this.aiAnalysis = analysis.toString();
    }

    private List<String> detectSpecificErrors(String userAnswer, String correctAnswer, String question) {
        List<String> errors = new ArrayList<>();
        String lowerUser = userAnswer.toLowerCase();
        String lowerCorrect = correctAnswer.toLowerCase();

        if (!lowerUser.contains(lowerCorrect) && !lowerCorrect.contains(lowerUser)) {
            errors.add("La réponse est différente de la réponse attendue");
        }

        if (userAnswer.length() < 10) {
            errors.add("Réponse trop courte, manque de développement");
        }

        if (question.toLowerCase().contains("explique") && !userAnswer.contains("car") && !userAnswer.contains("parce que")) {
            errors.add("Manque de justification ou d'explication");
        }

        return errors;
    }

    private String getDetailedExplanation(String question, String correctAnswer) {
        String lowerQ = question.toLowerCase();

        if (lowerQ.contains("java") || lowerQ.contains("programmation")) {
            return "En programmation Java, la bonne réponse met en évidence les concepts clés. " +
                    "La réponse correcte était : \"" + correctAnswer + "\". " +
                    "Révisez les fondamentaux de la POO (classes, objets, héritage, polymorphisme).";
        } else if (lowerQ.contains("sql") || lowerQ.contains("base de données")) {
            return "En SQL, la syntaxe et la structure des requêtes sont essentielles. " +
                    "La réponse attendue : \"" + correctAnswer + "\". " +
                    "Entraînez-vous sur les requêtes SELECT, JOIN et GROUP BY.";
        } else if (lowerQ.contains("html") || lowerQ.contains("css")) {
            return "En développement web, la structure HTML/CSS est fondamentale. " +
                    "La bonne réponse : \"" + correctAnswer + "\". " +
                    "Révisez les balises HTML et les sélecteurs CSS.";
        } else {
            return "La réponse correcte était : \"" + correctAnswer + "\". " +
                    "Concentrez-vous sur les concepts clés de cette question.";
        }
    }

    private String getPositiveFeedback(String question) {
        String lowerQ = question.toLowerCase();

        if (lowerQ.contains("java")) {
            return "Excellente compréhension des concepts Java ! Vous maîtrisez bien la POO.";
        } else if (lowerQ.contains("sql")) {
            return "Très bonne maîtrise des requêtes SQL ! Continuez ainsi.";
        } else if (lowerQ.contains("html") || lowerQ.contains("css")) {
            return "Bonnes connaissances en développement web !";
        } else {
            return "Très bonne réponse ! Vous avez bien compris le concept.";
        }
    }

    private List<String> getRecommendationsForQuestion(String question) {
        List<String> recommendations = new ArrayList<>();
        String lowerQ = question.toLowerCase();

        if (lowerQ.contains("java")) {
            recommendations.add("Révisez les chapitres sur la programmation orientée objet");
            recommendations.add("Pratiquez avec des exercices Java supplémentaires");
        } else if (lowerQ.contains("sql")) {
            recommendations.add("Entraînez-vous sur des plateformes comme LeetCode ou HackerRank");
            recommendations.add("Révisez les différents types de jointures");
        } else if (lowerQ.contains("html") || lowerQ.contains("css")) {
            recommendations.add("Créez des mini-projets pour mettre en pratique");
            recommendations.add("Révisez les balises HTML et les propriétés CSS");
        } else {
            recommendations.add("Relisez le cours correspondant à cette question");
            recommendations.add("Faites des exercices similaires pour vous entraîner");
        }

        return recommendations;
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private void sendResultEmail() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                emailService.sendChallengeResult(currentUser, challenge.getTitre(),
                        score, getTotalPoints(), LocalDateTime.now());
                System.out.println("✅ Email envoyé à : " + currentUser.getEmail());
            } else {
                System.err.println("❌ Impossible d'envoyer l'email : utilisateur non connecté ou email manquant");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getTotalPoints() {
        int total = 0;
        for (Exercice e : exercices) total += e.getPoints();
        total += quizzes.size() * 50;
        return total;
    }

    private void finishChallenge() {
        calculateScore();
        generateAIAnalysis();
        sendResultEmail();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/resultchallenge.fxml"));
            javafx.scene.Parent root = loader.load();
            ResultChallengeController controller = loader.getController();
            controller.setChallenge(challenge);
            controller.setScore(score, getTotalPoints());
            controller.setAIAnalysis(aiAnalysis);

            MainApp.getPrimaryStage().getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onQuit() {
        if (timer != null) timer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
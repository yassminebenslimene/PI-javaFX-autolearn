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
import tn.esprit.session.JwtManager;

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
    @FXML private Button translateBtn;
    @FXML private ComboBox<String> langCombo;
    @FXML private Label translatedLabel;

    private TranslateService translateService;
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
        translateService = new TranslateService();
        if (langCombo != null) {
            langCombo.getItems().addAll(TranslateService.LANGUAGE_NAMES);
            langCombo.setValue("English");
        }
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
                JwtManager.getCurrentUser().getId(), challenge.getId());
        if (existing != null && !existing.isCompleted()) {
            currentIndex = existing.getCurrentIndex();
            // Restaurer les réponses sauvegardées
            if (existing.getAnswersMap() != null) {
                exerciceAnswers.putAll(existing.getAnswersMap());
            }
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
        if (translatedLabel != null) {
            translatedLabel.setVisible(false);
            translatedLabel.setManaged(false);
        }
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
            if (translateBtn != null) translateBtn.setDisable(false);
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
            if (translateBtn != null) translateBtn.setDisable(false);
        }
    }
    @FXML
    private void onTranslate() {
        Object current = allQuestions.get(currentIndex);
        String textToTranslate = "";

        if (current instanceof Exercice) {
            textToTranslate = ((Exercice) current).getQuestion();
        } else if (current instanceof Quiz) {
            textToTranslate = ((Quiz) current).getTitre() + "\n" + ((Quiz) current).getDescription();
        }

        if (textToTranslate.isEmpty()) return;

        // Obtenir le code de langue sélectionné
        String selectedLangName = langCombo.getValue();
        String langCode = getLangCode(selectedLangName);

        // Désactiver le bouton pendant la traduction
        translateBtn.setDisable(true);
        translateBtn.setText("🌐 Traduction...");

        // Traduire dans un thread séparé
        final String finalTextToTranslate = textToTranslate;
        final String finalLangCode = langCode;

        new Thread(() -> {
            // Utiliser l'instance translateService (non statique)
            String translatedText = translateService.translate(finalTextToTranslate, finalLangCode);

            javafx.application.Platform.runLater(() -> {
                if (translatedLabel != null) {
                    String langName = translateService.getLangName(finalLangCode);
                    translatedLabel.setText("📖 Traduction (" + langName + ") :\n" + translatedText);
                    translatedLabel.setVisible(true);
                    translatedLabel.setManaged(true);
                }
                translateBtn.setDisable(false);
                translateBtn.setText("🌐 Traduire");
            });
        }).start();
    }
    // Méthode helper pour obtenir le code langue
    private String getLangCode(String langName) {
        for (int i = 0; i < TranslateService.LANGUAGE_NAMES.length; i++) {
            if (TranslateService.LANGUAGE_NAMES[i].equals(langName)) {
                return TranslateService.SUPPORTED_LANGUAGES[i];
            }
        }
        return "en";
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
                JwtManager.getCurrentUser().getId(), challenge.getId());
        if (userChallenge == null) {
            userChallenge = new UserChallenge();
            userChallenge.setUserId(JwtManager.getCurrentUser().getId());
            userChallenge.setChallengeId(challenge.getId());
        }
        userChallenge.setCurrentIndex(currentIndex);
        userChallenge.setAnswersMap(exerciceAnswers);
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
        // Score basé uniquement sur la participation (avoir répondu)
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
        analysis.append("║                    🤖 ANALYSE IA DE VOS RÉPONSES                      ║\n");
        analysis.append("╚══════════════════════════════════════════════════════════════════════╝\n\n");

        int questionNumber = 1;

        for (Exercice e : exercices) {
            String userAnswer = exerciceAnswers.get(e.getId());
            String theme = detectTheme(e.getQuestion());

            analysis.append("┌─────────────────────────────────────────────────────────────────┐\n");
            analysis.append("│ 📌 QUESTION N°").append(questionNumber).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ ❓ ").append(e.getQuestion()).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 📝 VOTRE RÉPONSE :\n");
            analysis.append("│ ").append(userAnswer != null && !userAnswer.isEmpty() ? userAnswer : "(non répondue)").append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");

            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                analysis.append("│ ⚠️ ANALYSE : Aucune réponse fournie\n");
                analysis.append("│\n");
                analysis.append("│ 💡 RECOMMANDATION : Prenez le temps de répondre à chaque question.\n");
            } else {
                // Analyse de la qualité de la réponse
                analysis.append("│ 🧠 ANALYSE DE VOTRE RÉPONSE :\n");
                analysis.append("│\n");

                // Longueur
                if (userAnswer.length() < 20) {
                    analysis.append("│   ⚠️ Réponse trop courte. Développez davantage.\n");
                } else if (userAnswer.length() > 100) {
                    analysis.append("│   ✅ Réponse bien développée.\n");
                } else {
                    analysis.append("│   📘 Longueur de réponse correcte.\n");
                }

                // Structure
                if (userAnswer.contains(".") && userAnswer.split("\\.").length >= 2) {
                    analysis.append("│   ✅ Bonne structure avec plusieurs phrases.\n");
                } else {
                    analysis.append("│   ⚠️ Structure à améliorer. Utilisez des phrases complètes.\n");
                }

                // Vocabulaire technique
                if (containsTechnicalTerms(userAnswer, theme)) {
                    analysis.append("│   ✅ Bon usage du vocabulaire technique.\n");
                } else {
                    analysis.append("│   ⚠️ Utilisez davantage de termes techniques.\n");
                }

                analysis.append("│\n");
                analysis.append("│ 💡 RECOMMANDATIONS :\n");
                analysis.append("│   • ").append(getRecommendationByTheme(theme)).append("\n");
            }
            analysis.append("└─────────────────────────────────────────────────────────────────┘\n\n");
            questionNumber++;
        }

        // Analyse des quiz
        for (Quiz q : quizzes) {
            Integer quizScore = quizScores.get(q.getId());
            String theme = detectTheme(q.getTitre());

            analysis.append("┌─────────────────────────────────────────────────────────────────┐\n");
            analysis.append("│ 📋 QUIZ : ").append(q.getTitre()).append("\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 🎯 VOTRE AUTO-ÉVALUATION : ").append(quizScore != null ? quizScore : 0).append("/100\n");
            analysis.append("├─────────────────────────────────────────────────────────────────┤\n");

            if (quizScore == null || quizScore < 50) {
                analysis.append("│ 📊 ANALYSE : Auto-évaluation basse\n");
                analysis.append("│\n");
                analysis.append("│ 💡 CONSEILS : Révisez les concepts de ").append(theme).append("\n");
            } else {
                analysis.append("│ 📊 ANALYSE : Bonne confiance en vos connaissances\n");
                analysis.append("│\n");
                analysis.append("│ 🚀 POUR ALLER PLUS LOIN : Testez-vous sur des exercices avancés\n");
            }
            analysis.append("└─────────────────────────────────────────────────────────────────┘\n\n");
        }

        this.aiAnalysis = analysis.toString();
    }

    private String detectTheme(String text) {
        String lowerText = text.toLowerCase();
        if (lowerText.contains("java") || lowerText.contains("poo") || lowerText.contains("objet")) {
            return "Java";
        } else if (lowerText.contains("sql") || lowerText.contains("base de données")) {
            return "SQL";
        } else if (lowerText.contains("html") || lowerText.contains("css") || lowerText.contains("javascript")) {
            return "Web";
        } else {
            return "Général";
        }
    }

    private boolean containsTechnicalTerms(String answer, String theme) {
        String lowerAnswer = answer.toLowerCase();
        if (theme.equals("Java")) {
            return lowerAnswer.contains("classe") || lowerAnswer.contains("objet") || lowerAnswer.contains("méthode");
        } else if (theme.equals("SQL")) {
            return lowerAnswer.contains("select") || lowerAnswer.contains("from") || lowerAnswer.contains("join");
        } else if (theme.equals("Web")) {
            return lowerAnswer.contains("html") || lowerAnswer.contains("css") || lowerAnswer.contains("javascript");
        }
        return false;
    }

    private String getRecommendationByTheme(String theme) {
        switch (theme) {
            case "Java": return "Révisez les concepts de programmation orientée objet (classes, objets, héritage)";
            case "SQL": return "Entraînez-vous sur les requêtes SQL (SELECT, JOIN, GROUP BY)";
            case "Web": return "Pratiquez HTML/CSS et JavaScript avec des mini-projets";
            default: return "Structurez vos réponses et utilisez des exemples concrets";
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private void sendResultEmail() {
        try {
            User currentUser = JwtManager.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                System.out.println("✅ Challenge termine par : " + currentUser.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur sendResultEmail : " + e.getMessage());
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
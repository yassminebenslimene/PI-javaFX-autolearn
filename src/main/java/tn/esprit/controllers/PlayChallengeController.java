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
        analysis.append("╔════════════════════════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("║                         🤖 ANALYSE IA DE VOS RÉPONSES                                  ║\n");
        analysis.append("╚════════════════════════════════════════════════════════════════════════════════════════╝\n\n");

        int questionNumber = 1;

        for (Exercice e : exercices) {
            String userAnswer = exerciceAnswers.get(e.getId());
            String theme = detectTheme(e.getQuestion());

            analysis.append("┌────────────────────────────────────────────────────────────────────────────────────┐\n");
            analysis.append("│ 📌 QUESTION N°").append(questionNumber).append(" - ").append(theme).append("\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ ❓ ").append(e.getQuestion()).append("\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 📝 VOTRE RÉPONSE :\n");
            analysis.append("│ ").append(userAnswer != null && !userAnswer.isEmpty() ? userAnswer : "(non répondue)").append("\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");

            if (userAnswer == null || userAnswer.trim().isEmpty()) {
                analysis.append("│ ⚠️ ANALYSE : Aucune réponse fournie\n");
                analysis.append("│\n");
                analysis.append("│ 💡 RECOMMANDATION : N'hésitez pas à exprimer votre compréhension du sujet.\n");
                analysis.append("│    Même une réponse partielle est meilleure qu'une absence de réponse.\n");
            } else {
                // Analyse IA professionnelle
                analysis.append("│ 🧠 ANALYSE DE VOTRE RÉPONSE :\n");
                analysis.append("│\n");

                // Analyse de la clarté
                if (isAnswerClear(userAnswer)) {
                    analysis.append("│   ✅ Clarté : Votre réponse est claire et facile à comprendre.\n");
                } else {
                    analysis.append("│   ⚠️ Clarté : Votre réponse manque de clarté. Essayez d'organiser vos idées.\n");
                }

                // Analyse de la pertinence
                if (isAnswerRelevantToQuestion(userAnswer, e.getQuestion())) {
                    analysis.append("│   ✅ Pertinence : Votre réponse est pertinente par rapport à la question.\n");
                } else {
                    analysis.append("│   ⚠️ Pertinence : Votre réponse semble hors sujet. Relisez attentivement la question.\n");
                }

                // Analyse de la structure
                if (hasGoodStructure(userAnswer)) {
                    analysis.append("│   ✅ Structure : Bonne organisation avec des phrases bien construites.\n");
                } else {
                    analysis.append("│   ⚠️ Structure : Votre réponse manque de structure. Utilisez des phrases complètes.\n");
                }

                // Analyse de la profondeur
                int depth = getAnswerDepth(userAnswer);
                if (depth >= 3) {
                    analysis.append("│   ✅ Profondeur : Réponse détaillée et complète.\n");
                } else if (depth >= 2) {
                    analysis.append("│   📘 Profondeur : Bonne réponse mais pourrait être plus développée.\n");
                } else {
                    analysis.append("│   ⚠️ Profondeur : Réponse trop superficielle. Développez davantage.\n");
                }

                // Analyse du vocabulaire technique
                int technicalTerms = countTechnicalTermsInAnswer(userAnswer, theme);
                if (technicalTerms > 2) {
                    analysis.append("│   ✅ Vocabulaire technique : Bonne maîtrise des termes spécifiques.\n");
                } else if (technicalTerms > 0) {
                    analysis.append("│   📘 Vocabulaire technique : Quelques termes techniques utilisés, à enrichir.\n");
                } else {
                    analysis.append("│   ⚠️ Vocabulaire technique : Utilisez davantage de termes techniques.\n");
                }

                // Suggestions d'amélioration
                analysis.append("│\n");
                analysis.append("│ 💡 SUGGESTIONS D'AMÉLIORATION :\n");
                List<String> suggestions = getImprovementSuggestions(userAnswer, theme);
                for (String suggestion : suggestions) {
                    analysis.append("│   • ").append(suggestion).append("\n");
                }
            }
            analysis.append("└────────────────────────────────────────────────────────────────────────────────────┘\n\n");
            questionNumber++;
        }

        // Analyse des quiz
        for (Quiz q : quizzes) {
            Integer quizScore = quizScores.get(q.getId());
            String theme = detectTheme(q.getTitre());

            analysis.append("┌────────────────────────────────────────────────────────────────────────────────────┐\n");
            analysis.append("│ 📋 QUIZ : ").append(q.getTitre()).append("\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 📝 DESCRIPTION :\n");
            analysis.append("│ ").append(q.getDescription()).append("\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");
            analysis.append("│ 🎯 VOTRE SCORE : ").append(quizScore != null ? quizScore : 0).append("/100\n");
            analysis.append("├────────────────────────────────────────────────────────────────────────────────────┤\n");

            if (quizScore == null || quizScore < 50) {
                analysis.append("│ ⚠️ ANALYSE : Score insuffisant (seuil requis: 50%)\n");
                analysis.append("│\n");
                analysis.append("│ 💡 RECOMMANDATIONS POUR PROGRESSER :\n");
                analysis.append("│   • Révisez les concepts fondamentaux de ").append(theme).append("\n");
                analysis.append("│   • Pratiquez avec des exercices similaires\n");
                analysis.append("│   • Consultez des ressources supplémentaires sur le sujet\n");
            } else if (quizScore >= 80) {
                analysis.append("│ 🎉 ANALYSE : Excellent score ! Vous maîtrisez bien ce thème.\n");
                analysis.append("│\n");
                analysis.append("│ 🚀 POUR ALLER PLUS LOIN :\n");
                analysis.append("│   • Explorez des concepts avancés de ").append(theme).append("\n");
                analysis.append("│   • Partagez vos connaissances avec d'autres apprenants\n");
            } else {
                analysis.append("│ 📘 ANALYSE : Bon score ! Quelques points à améliorer.\n");
                analysis.append("│\n");
                analysis.append("│ 💡 POUR VOUS AMÉLIORER :\n");
                analysis.append("│   • Identifiez vos points faibles dans ce thème\n");
                analysis.append("│   • Refaites les questions où vous avez hésité\n");
            }
            analysis.append("└────────────────────────────────────────────────────────────────────────────────────┘\n\n");
        }

        // Résumé final
        int totalPoints = getTotalPoints();
        int percentage = totalPoints > 0 ? (score * 100 / totalPoints) : 0;

        analysis.append("\n╔════════════════════════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("║                                     📊 RÉSUMÉ FINAL                                     ║\n");
        analysis.append("╚════════════════════════════════════════════════════════════════════════════════════════╝\n");
        analysis.append("\n");
        analysis.append("   ╔══════════════════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("   ║                                                                                  ║\n");
        analysis.append("   ║      🎯 Participation : ").append(String.format("%-4d", score)).append("/").append(totalPoints).append(" points                     ║\n");
        analysis.append("   ║      📈 Taux de complétion : ").append(String.format("%-4d", percentage)).append("%                                   ║\n");
        analysis.append("   ║                                                                                  ║\n");

        if (percentage >= 80) {
            analysis.append("   ║      🎉 FÉLICITATIONS ! Excellent travail ! Vous avez bien participé.         ║\n");
        } else if (percentage >= 60) {
            analysis.append("   ║      👍 BON TRAVAIL ! Continuez vos efforts pour vous améliorer.              ║\n");
        } else if (percentage >= 40) {
            analysis.append("   ║      📚 ENCOURAGEMENT ! Plus de pratique vous aidera à progresser.            ║\n");
        } else {
            analysis.append("   ║      💪 CONTINUEZ ! La pratique régulière est la clé du succès.               ║\n");
        }

        analysis.append("   ║                                                                                  ║\n");
        analysis.append("   ╚══════════════════════════════════════════════════════════════════════════════════╝\n");

        analysis.append("\n╔════════════════════════════════════════════════════════════════════════════════════════╗\n");
        analysis.append("║                              📅 PLAN D'ACTION PERSONNALISÉ                             ║\n");
        analysis.append("╚════════════════════════════════════════════════════════════════════════════════════════╝\n");
        analysis.append("\n");
        analysis.append("   ┌────────────────────────────────────────────────────────────────────────────────────┐\n");
        analysis.append("   │ 1. 🔄 Refaites les exercices pour renforcer votre compréhension                    │\n");
        analysis.append("   │ 2. 📖 Consultez les ressources pédagogiques recommandées                           │\n");
        analysis.append("   │ 3. 💻 Pratiquez avec des cas concrets et des exemples réels                       │\n");
        analysis.append("   │ 4. 🎯 Fixez-vous des objectifs d'apprentissage hebdomadaires                       │\n");
        analysis.append("   │ 5. 👥 Échangez avec d'autres apprenants pour partager vos connaissances            │\n");
        analysis.append("   └────────────────────────────────────────────────────────────────────────────────────┘\n");

        this.aiAnalysis = analysis.toString();
    }

    // ========== MÉTHODES D'ANALYSE IA SANS RÉPONSE CORRECTE ==========

    private String detectTheme(String text) {
        String lowerText = text.toLowerCase();
        if (lowerText.contains("java") || lowerText.contains("poo") || lowerText.contains("objet")) {
            return "Programmation Java";
        } else if (lowerText.contains("sql") || lowerText.contains("base de données") || lowerText.contains("mysql")) {
            return "Base de données SQL";
        } else if (lowerText.contains("html") || lowerText.contains("css") || lowerText.contains("javascript")) {
            return "Développement Web";
        } else if (lowerText.contains("spring") || lowerText.contains("boot")) {
            return "Framework Spring";
        } else {
            return "Informatique générale";
        }
    }

    private boolean isAnswerClear(String answer) {
        String[] sentences = answer.split("[.!?]");
        return sentences.length >= 1 && answer.length() > 20;
    }

    private boolean isAnswerRelevantToQuestion(String answer, String question) {
        String lowerAnswer = answer.toLowerCase();
        String lowerQuestion = question.toLowerCase();
        String[] keyWords = lowerQuestion.split("\\s+");
        int matches = 0;
        for (String word : keyWords) {
            if (word.length() > 3 && lowerAnswer.contains(word)) {
                matches++;
            }
        }
        return matches >= 1;
    }

    private boolean hasGoodStructure(String answer) {
        return answer.contains(".") && answer.split("\\.").length >= 2 && answer.length() > 30;
    }

    private int getAnswerDepth(String answer) {
        int depth = 1;
        if (answer.length() > 50) depth++;
        if (answer.split("\\.").length >= 3) depth++;
        if (answer.contains("par exemple") || answer.contains("exemple")) depth++;
        if (answer.contains("car") || answer.contains("donc") || answer.contains("ainsi")) depth++;
        return Math.min(depth, 4);
    }

    private int countTechnicalTermsInAnswer(String answer, String theme) {
        String lowerAnswer = answer.toLowerCase();
        int count = 0;
        String[] javaTerms = {"classe", "objet", "méthode", "héritage", "polymorphisme", "encapsulation", "interface", "variable", "fonction"};
        String[] sqlTerms = {"select", "from", "where", "join", "table", "colonne", "clé", "requête", "base"};
        String[] webTerms = {"html", "css", "javascript", "dom", "balise", "sélecteur", "responsive", "api", "frontend"};

        String[] termsToCheck;
        if (theme.contains("Java")) termsToCheck = javaTerms;
        else if (theme.contains("SQL")) termsToCheck = sqlTerms;
        else if (theme.contains("Web")) termsToCheck = webTerms;
        else termsToCheck = javaTerms;

        for (String term : termsToCheck) {
            if (lowerAnswer.contains(term)) count++;
        }
        return count;
    }

    private List<String> getImprovementSuggestions(String answer, String theme) {
        List<String> suggestions = new ArrayList<>();

        if (answer.length() < 30) {
            suggestions.add("Développez davantage votre réponse avec plus de détails");
        }

        if (!answer.contains("par exemple") && !answer.contains("exemple")) {
            suggestions.add("Illustrez votre réponse avec des exemples concrets");
        }

        if (countTechnicalTermsInAnswer(answer, theme) < 2) {
            suggestions.add("Utilisez le vocabulaire technique spécifique au domaine");
        }

        if (!answer.contains("car") && !answer.contains("donc") && !answer.contains("parce que")) {
            suggestions.add("Justifiez vos arguments avec des explications logiques");
        }

        suggestions.add("Relisez votre réponse pour corriger les éventuelles fautes");

        return suggestions;
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
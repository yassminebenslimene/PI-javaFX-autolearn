package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller du quiz frontoffice — gère les 3 écrans :
 * 1. Intro (infos du quiz + bouton commencer)
 * 2. Questions (une par une avec options cliquables)
 * 3. Résultats (score final)
 */
public class FrontQuizController {

    // ── FXML intro ────────────────────────────────────────────────────────────
    @FXML private Label labelTitreQuiz;
    @FXML private Label labelDescQuiz;
    @FXML private Label labelNbQuestions;
    @FXML private Label labelTotalPoints;
    @FXML private Label labelDuree;

    // ── FXML question ─────────────────────────────────────────────────────────
    @FXML private Label labelTitreHeader;
    @FXML private Label labelTimer;
    @FXML private Label labelProgress;
    @FXML private Label labelQuestion;
    @FXML private Label labelPoints;
    @FXML private VBox  optionsContainer;
    @FXML private Label labelRepondues;
    @FXML private Button btnSoumettre;

    // ── FXML résultat ─────────────────────────────────────────────────────────
    @FXML private Label labelTitreResultat;
    @FXML private Label labelPointsObtenus;
    @FXML private Label labelPourcentage;
    @FXML private Label labelPointsTotal;
    @FXML private Label labelMessage;
    @FXML private Label labelTentative;
    @FXML private Label labelMaxTentatives;
    @FXML private Label labelMeilleurScore;
    @FXML private Label labelPeutRecommencer;

    // ── État interne ──────────────────────────────────────────────────────────
    private Quiz quiz;
    private Chapitre chapitre;
    private List<Question> questions;
    private List<Option> optionsQuestionCourante;
    private int indexQuestion = 0;
    private final Map<Integer, Integer> reponsesChoisies = new HashMap<>();
    private int totalPoints = 0;
    private Timeline timerTimeline;
    private int secondesRestantes;
    private Runnable onRetourCallback;

    // Cache des options par question — chargées une seule fois pendant le quiz
    private final Map<Integer, List<Option>> optionsParQuestion = new HashMap<>();

    // Référence à n'importe quel nœud de la scène courante pour setCenter
    private javafx.scene.Node sceneRef;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceOption serviceOption = new ServiceOption();

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Appelé par FrontChapitreController pour injecter le chapitre et le callback retour.
     * Cherche le quiz lié au chapitre (par titre ou premier quiz actif).
     */
    public void setSceneRef(javafx.scene.Node ref) {
        this.sceneRef = ref;
    }

    public void setChapitre(Chapitre chapitre, Runnable onRetour) {        this.chapitre = chapitre;
        this.onRetourCallback = onRetour;
        List<Quiz> quizDuChapitre = serviceQuiz.findByChapitreId(chapitre.getId());
        Quiz trouve = quizDuChapitre.isEmpty() ? null : quizDuChapitre.get(0);
        if (trouve != null) {
            setQuiz(trouve);
        } else {
            if (labelTitreQuiz != null) labelTitreQuiz.setText("Aucun quiz disponible pour ce chapitre");
        }
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        this.questions = serviceQuestion.findByQuizId(quiz.getId());
        this.totalPoints = questions.stream().mapToInt(Question::getPoint).sum();
        // Capturer la référence de scène depuis le label intro
        javafx.application.Platform.runLater(() -> {
            if (labelTitreQuiz != null) sceneRef = labelTitreQuiz;
        });
        afficherIntro();
    }

    // ── Écran 1 : Intro ───────────────────────────────────────────────────────

    private void afficherIntro() {
        if (labelTitreQuiz == null) return;
        labelTitreQuiz.setText("Quiz - " + quiz.getTitre());
        labelDescQuiz.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        labelNbQuestions.setText(String.valueOf(questions.size()));
        labelTotalPoints.setText(String.valueOf(totalPoints));
        labelDuree.setText(quiz.getDureeMaxMinutes() != null ? String.valueOf(quiz.getDureeMaxMinutes()) : "—");
    }

    @FXML
    private void onCommencer() {
        indexQuestion = 0;
        reponsesChoisies.clear();
        naviguerVersChargement();
    }

    // ── Écran de chargement (2s) avant les questions ──────────────────────────
    private void naviguerVersChargement() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/views/frontoffice/quiz/loading.fxml"));
            javafx.scene.Parent view = loader.load();
            FrontQuizLoadingController loadingCtrl = loader.getController();
            // Capturer sceneRef avant de changer de vue
            if (sceneRef == null) sceneRef = labelTitreQuiz;
            setCenter(view);
            // Démarrer l'animation et passer aux questions après 2s
            loadingCtrl.start("Quiz - " + quiz.getTitre(), this::naviguerVersQuestion);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback : aller directement aux questions
            naviguerVersQuestion();
        }
    }

    // ── Écran 2 : Questions ───────────────────────────────────────────────────

    private void naviguerVersQuestion() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/quiz/question.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.indexQuestion = this.indexQuestion;
            ctrl.reponsesChoisies.putAll(this.reponsesChoisies);
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelTitreQuiz;
            ctrl.afficherQuestion();
            setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherQuestion() {
        if (questions == null || questions.isEmpty() || labelQuestion == null) return;

        Question q = questions.get(indexQuestion);

        labelTitreHeader.setText("Quiz - " + quiz.getTitre());
        labelProgress.setText("Question " + (indexQuestion + 1) + " / " + questions.size());
        labelQuestion.setText(q.getTexteQuestion());
        labelPoints.setText("⭐ " + q.getPoint() + " points");
        mettreAJourRepondues();
        demarrerTimer();
        afficherOptions(q); // charge et met en cache les options
    }

    private void afficherOptions(Question q) {
        optionsContainer.getChildren().clear();
        Integer dejaChoisi = reponsesChoisies.get(q.getId());

        // Charger et mettre en cache les options (évite N+1 requêtes BDD)
        List<Option> opts = optionsParQuestion.computeIfAbsent(
            q.getId(), id -> serviceOption.findByQuestionId(id));
        optionsQuestionCourante = opts;

        for (Option opt : optionsQuestionCourante) {
            Button btn = new Button(opt.getTexteOption());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setWrapText(true);
            btn.setPadding(new Insets(14, 20, 14, 20));

            boolean selectionne = dejaChoisi != null && dejaChoisi == opt.getId();
            appliquerStyleOption(btn, selectionne);

            btn.setOnAction(e -> {
                reponsesChoisies.put(q.getId(), opt.getId());
                // Rafraîchir les styles de toutes les options
                afficherOptions(q);
                mettreAJourRepondues();
                // Passer auto à la question suivante après 400ms
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(400));
                pause.setOnFinished(ev -> {
                    if (indexQuestion < questions.size() - 1) {
                        indexQuestion++;
                        afficherQuestion();
                    }
                });
                pause.play();
            });

            optionsContainer.getChildren().add(btn);
        }
    }

    private void appliquerStyleOption(Button btn, boolean selectionne) {
        if (selectionne) {
            btn.setStyle(
                "-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);" +
                "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:700;" +
                "-fx-background-radius:12; -fx-cursor:hand; -fx-border-width:0;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),10,0,0,3);"
            );
        } else {
            btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.15);" +
                "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:600;" +
                "-fx-background-radius:12; -fx-cursor:hand; -fx-border-width:2;" +
                "-fx-border-color:rgba(255,255,255,0.3); -fx-border-radius:12;"
            );
        }
    }

    private void mettreAJourRepondues() {
        if (labelRepondues == null) return;
        labelRepondues.setText(reponsesChoisies.size() + " / " + questions.size() + " questions répondues");
    }

    private void demarrerTimer() {
        if (timerTimeline != null) timerTimeline.stop();
        if (quiz.getDureeMaxMinutes() == null) {
            if (labelTimer != null) labelTimer.setText("⏱  ∞");
            return;
        }
        secondesRestantes = quiz.getDureeMaxMinutes() * 60;
        mettreAJourAffichageTimer();
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesRestantes--;
            mettreAJourAffichageTimer();
            if (secondesRestantes <= 0) {
                timerTimeline.stop();
                onSoumettre();
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void mettreAJourAffichageTimer() {
        if (labelTimer == null) return;
        int min = secondesRestantes / 60;
        int sec = secondesRestantes % 60;
        labelTimer.setText(String.format("⏱  %d:%02d", min, sec));
    }

    @FXML
    private void onSoumettre() {
        if (timerTimeline != null) timerTimeline.stop();
        naviguerVersResultat();
    }

    // ── Écran 3 : Résultats ───────────────────────────────────────────────────

    private void naviguerVersResultat() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/quiz/resultat.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.reponsesChoisies.putAll(this.reponsesChoisies);
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelQuestion;
            ctrl.optionsParQuestion.putAll(this.optionsParQuestion); // ✅ propager le cache
            ctrl.afficherResultat();
            setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherResultat() {
        if (labelTitreResultat == null) return;

        int pointsObtenus = 0;

        // ── Calcul du score (identique à QuizManagementService.php Symfony) ──
        for (Question q : questions) {
            Integer optionChoisieId = reponsesChoisies.get(q.getId());
            if (optionChoisieId == null) continue; // pas répondu → 0 point

            // ✅ PERF : utiliser le cache — pas de requête BDD supplémentaire
            List<Option> opts = optionsParQuestion.getOrDefault(
                q.getId(), serviceOption.findByQuestionId(q.getId()));

            for (Option o : opts) {
                if (o.isEstCorrecte()) {
                    // ✅ BUG 1 corrigé : .intValue() pour comparer int primitif == Integer objet
                    if (o.getId() == optionChoisieId.intValue()) {
                        pointsObtenus += q.getPoint();
                    }
                    break; // une seule bonne réponse par question
                }
            }
        }

        // ✅ BUG 2 corrigé : double pour éviter la troncature de la division entière
        double pct = totalPoints > 0
            ? Math.round((pointsObtenus * 100.0 / totalPoints) * 100.0) / 100.0
            : 0.0;
        int seuil = quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50;

        labelTitreResultat.setText("Quiz - " + quiz.getTitre());
        labelPointsObtenus.setText(String.valueOf(pointsObtenus));
        labelPourcentage.setText(String.format("%.0f%%", pct));
        labelPointsTotal.setText(String.valueOf(totalPoints));

        // Message selon le score
        if (pct >= seuil) {
            labelMessage.setText("🎉  Félicitations ! Vous avez réussi le quiz !");
            labelMessage.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#059669;" +
                "-fx-background-color:#f0fdf4; -fx-background-radius:10; -fx-padding:10 20 10 20;");
        } else if (pct >= (double) seuil / 2) {
            labelMessage.setText("📈  Peut mieux faire — continuez vos efforts !");
            labelMessage.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#d97706;" +
                "-fx-background-color:#fffbeb; -fx-background-radius:10; -fx-padding:10 20 10 20;");
        } else {
            labelMessage.setText("😔  Score insuffisant — révisez et réessayez !");
            labelMessage.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#dc2626;" +
                "-fx-background-color:#fef2f2; -fx-background-radius:10; -fx-padding:10 20 10 20;");
        }

        // ── Statistiques ──
        if (labelTentative != null) labelTentative.setText("1");
        if (labelMaxTentatives != null) {
            String max = quiz.getMaxTentatives() != null ? String.valueOf(quiz.getMaxTentatives()) : "∞";
            labelMaxTentatives.setText("TENTATIVE / " + max);
        }
        if (labelMeilleurScore != null) labelMeilleurScore.setText(String.format("%.0f%%", pct));
        if (labelPeutRecommencer != null) {
            boolean peutRecommencer = quiz.getMaxTentatives() == null || quiz.getMaxTentatives() > 1;
            labelPeutRecommencer.setText(peutRecommencer ? "OUI" : "NON");
            if (!peutRecommencer) {
                labelPeutRecommencer.setStyle("-fx-font-size:22; -fx-font-weight:900; -fx-text-fill:#ef4444;");
            }
        }
    }

    @FXML
    private void onAccueil() {
        if (timerTimeline != null) timerTimeline.stop();
        // Retourner à la page d'accueil via le callback retour (qui remonte jusqu'au FrontofficeController)
        // On utilise onRetourCallback qui pointe vers la liste des chapitres,
        // puis on laisse l'utilisateur naviguer — ou on peut appeler onHome directement
        if (sceneRef != null && sceneRef.getScene() != null) {
            // Chercher le FrontofficeController via la scène et appeler onHome
            javafx.scene.Node navbar = sceneRef.getScene().getRoot().lookup("#btnHome");
            if (navbar instanceof javafx.scene.control.Button btn) {
                btn.fire();
                return;
            }
        }
        // Fallback : retour aux chapitres
        if (onRetourCallback != null) onRetourCallback.run();
    }

    @FXML
    private void onRefaire() {
        indexQuestion = 0;
        reponsesChoisies.clear();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/quiz/intro.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelTitreResultat;
            ctrl.setQuiz(this.quiz);
            setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        if (timerTimeline != null) timerTimeline.stop();
        if (onRetourCallback != null) onRetourCallback.run();
    }

    // ── Utilitaire navigation ─────────────────────────────────────────────────

    private void setCenter(Parent view) {
        javafx.scene.Node ref = sceneRef != null ? sceneRef
                : labelTitreQuiz != null ? labelTitreQuiz
                : labelQuestion != null ? labelQuestion
                : labelTitreResultat;
        if (ref == null || ref.getScene() == null) return;
        BorderPane root = (BorderPane) ref.getScene().getRoot();
        // Mettre la vue directement sans ScrollPane pour garder le fond violet plein écran
        root.setCenter(view);
    }
}

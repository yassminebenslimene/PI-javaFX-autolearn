package tn.esprit.controllers;

/**
 * ============================================================
 * FrontQuizController — Contrôleur principal du quiz (front-office)
 * ============================================================
 *
 * Ce contrôleur gère l'intégralité du flux d'un quiz côté étudiant.
 * Il est partagé par les quatre vues FXML du quiz :
 *
 *   1. intro.fxml    — Écran d'introduction : titre, stats, bouton "Commencer"
 *   2. loading.fxml  — Écran de chargement animé (transition entre intro et questions)
 *   3. question.fxml — Écran de question : affichage des options, timer, progression
 *   4. resultat.fxml — Écran de résultats : score, pourcentage, statistiques, actions
 *
 * Flux de navigation :
 *   intro → (clic "Commencer") → loading → (pause 2s) → question(s) → (soumission) → résultat
 *   résultat → (clic "Refaire") → intro
 *   résultat / intro → (clic "Retour") → liste des chapitres
 *
 * Calcul du score :
 *   Pour chaque question, on compare l'option choisie par l'étudiant
 *   avec l'option marquée "estCorrecte = true". Si elles correspondent,
 *   on ajoute les points de la question au total obtenu.
 *   Le pourcentage = (pointsObtenus / totalPoints) × 100.
 *   Le seuil de réussite est défini dans l'entité Quiz (seuilReussite, défaut 50 %).
 */

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.EmailService;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontQuizController {

    // ══════════════════════════════════════════════════════════════════════════
    // CHAMPS FXML — loading.fxml
    // Ces champs sont injectés automatiquement par JavaFX lors du chargement
    // de l'écran de chargement animé (loading.fxml).
    // ══════════════════════════════════════════════════════════════════════════

    /** Conteneur racine de l'écran de chargement (utilisé pour l'animation de fondu sortant). */
    @FXML private StackPane rootPane;

    /** Conteneur des 4 carrés colorés qui tournent en animation de chargement. */
    @FXML private StackPane logoContainer;

    /** Carré bleu (coin supérieur gauche du logo animé). */
    @FXML private Rectangle squareBlue;

    /** Carré vert (coin supérieur droit du logo animé). */
    @FXML private Rectangle squareGreen;

    /** Carré rouge (coin inférieur gauche du logo animé). */
    @FXML private Rectangle squareRed;

    /** Carré orange (coin inférieur droit du logo animé). */
    @FXML private Rectangle squareOrange;

    /** Label "Chargement du Quiz..." qui clignote pendant l'animation. */
    @FXML private Label loadingLabel;

    /** Label affichant le nom du quiz en cours de chargement. */
    @FXML private Label quizNameLabel;

    // ══════════════════════════════════════════════════════════════════════════
    // CHAMPS FXML — intro.fxml
    // Ces champs sont injectés lors du chargement de l'écran d'introduction.
    // ══════════════════════════════════════════════════════════════════════════

    /** Titre du quiz affiché dans la carte centrale de l'intro. */
    @FXML private Label labelTitreQuiz;

    /** Description du quiz (sous-titre en violet). */
    @FXML private Label labelDescQuiz;

    /** Nombre total de questions du quiz (affiché dans la section stats). */
    @FXML private Label labelNbQuestions;

    /** Nombre total de points du quiz (somme des points de toutes les questions). */
    @FXML private Label labelTotalPoints;

    /** Durée maximale du quiz en minutes (ou "—" si illimitée). */
    @FXML private Label labelDuree;

    // ── Champ image — injecté depuis question.fxml ────────────────────────────
    /** Image du quiz affichée dans la carte de question (visible si image définie). */
    @FXML private javafx.scene.image.ImageView quizImageView;

    // ══════════════════════════════════════════════════════════════════════════
    // CHAMPS FXML — question.fxml
    // Ces champs sont injectés lors du chargement de l'écran de question.
    // ══════════════════════════════════════════════════════════════════════════

    /** Titre du quiz affiché dans l'en-tête de l'écran de question. */
    @FXML private Label  labelTitreHeader;

    /** Affichage du timer (ex : "⏱  2:30") dans l'en-tête, fond dégradé orange. */
    @FXML private Label  labelTimer;

    /** Bouton activer/désactiver le son. */
    @FXML private Button btnSound;

    /** Indicateur de progression (ex : "Question 3 / 10"). */
    @FXML private Label  labelProgress;

    /** Barre de progression animée */
    @FXML private javafx.scene.control.ProgressBar progressBar;

    /** Pourcentage affiché à droite de la barre */
    @FXML private Label  labelPourcentageProgress;

    /** Conteneur des points de navigation (un cercle par question) */
    @FXML private HBox   questionDots;

    /** Texte de la question courante, affiché dans la carte blanche centrale. */
    @FXML private Label  labelQuestion;

    /** Points attribués à la question courante (ex : "⭐ 10 points"). */
    @FXML private Label  labelPoints;

    /** Conteneur vertical dans lequel les boutons d'options sont générés dynamiquement. */
    @FXML private VBox   optionsContainer;

    /** Grille 2x2 pour les options style Symfony */
    @FXML private javafx.scene.layout.GridPane optionsGrid;

    /** Boutons navigation précédent/suivant */
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    /** Compteur de questions répondues (ex : "3 / 10 questions répondues"). */
    @FXML private Label  labelRepondues;

    /** Bouton de soumission du quiz, visible en bas à droite de l'écran de question. */
    @FXML private Button btnSoumettre;

    // ══════════════════════════════════════════════════════════════════════════
    // CHAMPS FXML — resultat.fxml
    // Ces champs sont injectés lors du chargement de l'écran de résultats.
    // ══════════════════════════════════════════════════════════════════════════

    /** Titre du quiz affiché dans la carte de résultats (en violet). */
    @FXML private Label labelTitreResultat;

    /** Points obtenus par l'étudiant (grand chiffre violet). */
    @FXML private Label labelPointsObtenus;

    /** Pourcentage de réussite (grand chiffre orange, ex : "75%"). */
    @FXML private Label labelPourcentage;

    /** Total des points possibles pour ce quiz (grand chiffre noir). */
    @FXML private Label labelPointsTotal;

    /**
     * Message de résultat contextuel :
     *   - Vert  : "Félicitations !" si pct >= seuil
     *   - Orange: "Peut mieux faire" si pct >= seuil/2
     *   - Rouge : "Score insuffisant" sinon
     */
    @FXML private Label labelMessage;

    /** Numéro de la tentative actuelle (toujours "1" dans cette version). */
    @FXML private Label labelTentative;

    /** Affiche le nombre maximum de tentatives autorisées (ex : "TENTATIVE / 3" ou "TENTATIVE / ∞"). */
    @FXML private Label labelMaxTentatives;

    /** Meilleur score enregistré pour ce quiz (dans cette version = score actuel). */
    @FXML private Label labelMeilleurScore;

    /** Indique si l'étudiant peut recommencer le quiz ("OUI" en bleu ou "NON" en rouge). */
    @FXML private Label labelPeutRecommencer;

    // ══════════════════════════════════════════════════════════════════════════
    // CHAMPS FXML — resultat_pro.fxml (VERSION PROFESSIONNELLE)
    // Nouveaux champs pour XP, badges et corrections détaillées
    // ══════════════════════════════════════════════════════════════════════════

    /** XP gagné lors de cette tentative (ex : "+850 XP"). */
    @FXML private Label labelXPGagne;

    /** Niveau actuel de l'étudiant (ex : "Niveau 3"). */
    @FXML private Label labelNiveau;

    /** Titre du niveau (ex : "INTERMÉDIAIRE"). */
    @FXML private Label labelTitreNiveau;

    /** Icône du niveau (emoji qui change selon le niveau). */
    @FXML private Label labelIconeNiveau;

    /** XP total accumulé par l'étudiant (ex : "3,450 XP"). */
    @FXML private Label labelXPTotal;

    /** Conteneur de la section badges (visible uniquement si badges débloqués). */
    @FXML private javafx.scene.layout.VBox containerBadges;

    /** FlowPane pour afficher les badges en grille. */
    @FXML private javafx.scene.layout.FlowPane flowPaneBadges;

    // ══════════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE DU CONTRÔLEUR
    // Ces champs maintiennent l'état du quiz tout au long de la session.
    // ══════════════════════════════════════════════════════════════════════════

    /** Le quiz en cours de passage (entité Quiz chargée depuis la base de données). */
    private Quiz quiz;

    /** Le chapitre auquel appartient ce quiz (utilisé pour le retour). */
    private Chapitre chapitre;

    /** Liste ordonnée de toutes les questions du quiz. */
    private List<Question> questions;

    /** Liste des options de la question actuellement affichée (mise à jour à chaque question). */
    private List<Option> optionsQuestionCourante;

    /** Index (0-based) de la question actuellement affichée. */
    private int indexQuestion = 0;

    /**
     * Map des réponses choisies par l'étudiant.
     * Clé   : ID de la question
     * Valeur: ID de l'option sélectionnée
     * Permet de conserver les réponses lors de la navigation entre questions.
     */
    private final Map<Integer, Integer> reponsesChoisies = new HashMap<>();

    /** Somme des points de toutes les questions (calculée une seule fois à l'initialisation). */
    private int totalPoints = 0;

    /** Timeline JavaFX qui décrémente le timer chaque seconde. Stoppée à la soumission. */
    private Timeline timerTimeline;

    /** Nombre de secondes restantes pour le quiz (initialisé depuis dureeMaxMinutes × 60). */
    private int secondesRestantes;

    /**
     * Callback exécuté lors du retour aux chapitres.
     * Fourni par le contrôleur parent (FrontChapitreDetailController) via setChapitre().
     */
    private Runnable onRetourCallback;

    /**
     * Cache des options par question pour éviter des requêtes répétées à la base de données.
     * Clé   : ID de la question
     * Valeur: liste des options de cette question
     */
    private final Map<Integer, List<Option>> optionsParQuestion = new HashMap<>();

    /**
     * Référence à un nœud JavaFX de la scène courante.
     * Utilisée par setCenter() pour remonter jusqu'au BorderPane racine
     * et y injecter la nouvelle vue.
     */
    private javafx.scene.Node sceneRef;

    // ══════════════════════════════════════════════════════════════════════════
    // SERVICES (accès à la base de données)
    // ══════════════════════════════════════════════════════════════════════════

    /** Service pour récupérer les quiz depuis la base de données. */
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    /** Service pour récupérer les questions d'un quiz. */
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    /** Service pour récupérer les options d'une question. */
    private final ServiceOption serviceOption = new ServiceOption();

    // ══════════════════════════════════════════════════════════════════════════
    // API PUBLIQUE — méthodes appelées depuis l'extérieur du contrôleur
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Fournit une référence à un nœud de la scène courante.
     * Nécessaire pour que setCenter() puisse remonter jusqu'au BorderPane racine.
     *
     * @param ref n'importe quel nœud déjà attaché à la scène principale
     */
    public void setSceneRef(javafx.scene.Node ref) { this.sceneRef = ref; }

    /**
     * Point d'entrée principal : initialise le contrôleur à partir d'un chapitre.
     * Recherche le quiz associé au chapitre via ServiceQuiz.findByChapitreId().
     * Si un quiz est trouvé, appelle setQuiz() pour lancer le flux.
     * Sinon, affiche un message d'erreur dans le label de titre.
     *
     * @param chapitre   le chapitre dont on veut passer le quiz
     * @param onRetour   callback à exécuter quand l'étudiant clique "Retour aux chapitres"
     */
    public void setChapitre(Chapitre chapitre, Runnable onRetour) {
        this.chapitre = chapitre;
        this.onRetourCallback = onRetour;
        List<Quiz> quizDuChapitre = serviceQuiz.findByChapitreId(chapitre.getId());
        // Filtrer uniquement les quiz actifs
        List<Quiz> quizActifs = quizDuChapitre.stream()
            .filter(q -> "actif".equals(q.getEtat()))
            .collect(java.util.stream.Collectors.toList());

        if (quizActifs.isEmpty()) {
            if (labelTitreQuiz != null) labelTitreQuiz.setText("Aucun quiz disponible pour ce chapitre");
        } else if (quizActifs.size() == 1) {
            // Un seul quiz → lancer directement
            setQuiz(quizActifs.get(0));
        } else {
            // Plusieurs quiz → afficher la sélection après que la scène soit prête
            javafx.application.Platform.runLater(() -> afficherSelectionQuiz(quizActifs));
        }
    }

    /**
     * Affiche une page de sélection quand le chapitre a plusieurs quiz actifs.
     * L'étudiant choisit lequel passer.
     */
    private void afficherSelectionQuiz(List<Quiz> quizActifs) {
        try {
            // ── Fond violet dégradé (même style que intro.fxml) ──────────────
            javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
            root.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#a78bfa);");

            // Décorations cercles
            for (double[] c : new double[][]{
                {200,-340,-200,0.06},{170,360,-190,0.05},{150,-380,230,0.07},
                {100,-100,280,0.05},{110,200,260,0.06},{75,180,-230,0.08},
                {65,-280,60,0.09},{60,280,100,0.07}}) {
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(c[0]);
                circle.setFill(javafx.scene.paint.Color.rgb(255,255,255,c[3]));
                circle.setTranslateX(c[1]); circle.setTranslateY(c[2]);
                circle.setMouseTransparent(true);
                root.getChildren().add(circle);
            }

            // ── Bouton retour en haut à gauche ────────────────────────────────
            javafx.scene.control.Button btnRetour = new javafx.scene.control.Button("← Retour aux chapitres");
            btnRetour.setStyle("-fx-background-color:rgba(255,255,255,0.14); -fx-text-fill:white;" +
                "-fx-font-size:13; -fx-font-weight:800; -fx-padding:10 18; -fx-background-radius:999;" +
                "-fx-cursor:hand; -fx-border-width:1; -fx-border-color:rgba(255,255,255,0.22); -fx-border-radius:999;" +
                "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.16),10,0,0,3);");
            btnRetour.setOnAction(e -> { if (onRetourCallback != null) onRetourCallback.run(); });
            javafx.scene.layout.HBox topBar = new javafx.scene.layout.HBox(btnRetour);
            topBar.setPadding(new Insets(18, 0, 0, 18));
            javafx.scene.layout.StackPane.setAlignment(topBar, javafx.geometry.Pos.TOP_LEFT);
            root.getChildren().add(topBar);

            // ── Contenu central : cartes côte à côte ─────────────────────────
            javafx.scene.layout.VBox centerBox = new javafx.scene.layout.VBox(24);
            centerBox.setAlignment(javafx.geometry.Pos.CENTER);
            centerBox.setMaxWidth(900);

            // Cartes en HBox (côte à côte comme Symfony)
            javafx.scene.layout.HBox cardsRow = new javafx.scene.layout.HBox(24);
            cardsRow.setAlignment(javafx.geometry.Pos.CENTER);

            String[] colors = {"#e53935", "#1e88e5", "#43a047", "#fb8c00", "#8e24aa"};

            for (int i = 0; i < quizActifs.size(); i++) {
                Quiz q = quizActifs.get(i);
                List<Question> qs = serviceQuestion.findByQuizId(q.getId());
                int totalPts = qs.stream().mapToInt(Question::getPoint).sum();
                String color = colors[i % colors.length];

                // ── Carte blanche (style Symfony) ─────────────────────────────
                javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(0);
                card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                card.setPrefWidth(360);
                card.setMaxWidth(360);
                card.setStyle("-fx-background-color:rgba(255,255,255,0.98); -fx-background-radius:30;" +
                    "-fx-border-color:rgba(226,232,240,0.95); -fx-border-width:1; -fx-border-radius:30;" +
                    "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.10),22,0,0,10);");

                // Bande colorée en haut
                javafx.scene.layout.HBox topStripe = new javafx.scene.layout.HBox();
                topStripe.setPrefHeight(8);
                topStripe.setStyle("-fx-background-color:linear-gradient(to right," + color + ", derive(" + color + ", -12%));" +
                    "-fx-background-radius:30 30 0 0;");
                card.getChildren().add(topStripe);

                // Corps de la carte
                javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(14);
                body.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                body.setPadding(new Insets(18, 24, 24, 24));

                javafx.scene.control.Label meta = new javafx.scene.control.Label("Quiz interactif");
                meta.setStyle("-fx-background-color:rgba(124,58,237,0.12); -fx-text-fill:#6d28d9;" +
                    "-fx-font-size:11; -fx-font-weight:800; -fx-background-radius:999; -fx-padding:6 12 6 12;");

                // Icône colorée
                javafx.scene.layout.StackPane iconCircle = new javafx.scene.layout.StackPane();
                iconCircle.setPrefSize(84, 84);
                iconCircle.setMaxSize(84, 84);
                javafx.scene.shape.Rectangle iconBg = new javafx.scene.shape.Rectangle(84, 84);
                iconBg.setArcWidth(28); iconBg.setArcHeight(28);
                iconBg.setStyle("-fx-fill:linear-gradient(to bottom right," + color + ", derive(" + color + ", -24%));" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.16),16,0,0,4);");
                javafx.scene.control.Label iconLbl = new javafx.scene.control.Label("▶");
                iconLbl.setStyle("-fx-font-size:24; -fx-text-fill:white; -fx-font-weight:900;");
                iconCircle.getChildren().addAll(iconBg, iconLbl);

                // Titre
                javafx.scene.control.Label qTitre = new javafx.scene.control.Label(q.getTitre());
                qTitre.setStyle("-fx-font-size:18; -fx-font-weight:900; -fx-text-fill:#0f172a; -fx-text-alignment:CENTER; -fx-line-spacing:2;");
                qTitre.setWrapText(true);
                qTitre.setMaxWidth(290);
                qTitre.setAlignment(javafx.geometry.Pos.CENTER);

                // Description
                String descText = q.getDescription() != null
                    ? (q.getDescription().length() > 55 ? q.getDescription().substring(0, 55) + "..." : q.getDescription())
                    : "";
                javafx.scene.control.Label qDesc = new javafx.scene.control.Label(descText);
                qDesc.setStyle("-fx-font-size:12.5; -fx-text-fill:#475569; -fx-text-alignment:CENTER; -fx-line-spacing:2;");
                qDesc.setWrapText(true);
                qDesc.setMaxWidth(290);
                qDesc.setAlignment(javafx.geometry.Pos.CENTER);

                // Stats (Questions / Points / Minutes)
                javafx.scene.layout.HBox statsBox = new javafx.scene.layout.HBox(10);
                statsBox.setAlignment(javafx.geometry.Pos.CENTER);
                statsBox.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:22;" +
                    "-fx-border-color:#e2e8f0; -fx-border-radius:22; -fx-padding:14 12 14 12;");
                statsBox.setMaxWidth(Double.MAX_VALUE);

                javafx.scene.layout.VBox statQ = makeStat("❓", String.valueOf(qs.size()), "QUESTIONS", "#ef4444");
                javafx.scene.layout.VBox statP = makeStat("⭐", String.valueOf(totalPts), "POINTS", "#f59e0b");
                javafx.scene.layout.VBox statD = makeStat("⏱",
                    q.getDureeMaxMinutes() != null ? String.valueOf(q.getDureeMaxMinutes()) : "—",
                    "MINUTES", "#3b82f6");

                // Séparateurs
                javafx.scene.layout.Region sep1 = new javafx.scene.layout.Region();
                sep1.setPrefWidth(1); sep1.setPrefHeight(40);
                sep1.setStyle("-fx-background-color:#e2e8f0;");
                javafx.scene.layout.Region sep2 = new javafx.scene.layout.Region();
                sep2.setPrefWidth(1); sep2.setPrefHeight(40);
                sep2.setStyle("-fx-background-color:#e2e8f0;");

                statQ.setPrefWidth(94); statP.setPrefWidth(94); statD.setPrefWidth(94);
                statsBox.getChildren().addAll(statQ, sep1, statP, sep2, statD);

                // Bouton Commencer
                javafx.scene.control.Button btnCommencer = new javafx.scene.control.Button("▶   Commencer le quiz");
                btnCommencer.setMaxWidth(Double.MAX_VALUE);
                btnCommencer.setStyle("-fx-background-color:linear-gradient(to right,#22c55e,#16a34a);" +
                    "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:900;" +
                    "-fx-padding:14 18 14 18; -fx-background-radius:999; -fx-cursor:hand; -fx-border-width:0;" +
                    "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.25),18,0,0,6);");
                btnCommencer.setOnMouseEntered(ev -> btnCommencer.setStyle("-fx-background-color:linear-gradient(to right,#16a34a,#15803d);" +
                    "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:900;" +
                    "-fx-padding:14 18 14 18; -fx-background-radius:999; -fx-cursor:hand; -fx-border-width:0;" +
                    "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.32),20,0,0,8);"));
                btnCommencer.setOnMouseExited(ev -> btnCommencer.setStyle("-fx-background-color:linear-gradient(to right,#22c55e,#16a34a);" +
                    "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:900;" +
                    "-fx-padding:14 18 14 18; -fx-background-radius:999; -fx-cursor:hand; -fx-border-width:0;" +
                    "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.25),18,0,0,6);"));

                final Quiz quizChoisi = q;
                btnCommencer.setOnAction(e -> {
                    try {
                        // Initialiser l'état du quiz
                        this.quiz = quizChoisi;
                        this.questions = serviceQuestion.findByQuizIdAleatoire(quizChoisi.getId(), 0);
                        this.totalPoints = questions.stream().mapToInt(Question::getPoint).sum();
                        this.indexQuestion = 0;
                        this.reponsesChoisies.clear();
                        playStart();

                        // Garder une référence stable, pas le bouton temporaire du card
                        if (sceneRef == null && labelTitreQuiz != null) {
                            sceneRef = labelTitreQuiz;
                        } else if (sceneRef == null && btnCommencer.getScene() != null) {
                            sceneRef = btnCommencer.getScene().getRoot();
                        }

                        System.out.println("[Quiz] sceneRef=" + sceneRef + " scene=" +
                            (sceneRef != null ? sceneRef.getScene() : "null"));

                        FXMLLoader loadingLoader = new FXMLLoader(
                            getClass().getResource("/views/frontoffice/quiz/loading.fxml"));
                        Parent loadingView = loadingLoader.load();
                        FrontQuizController loadingCtrl = loadingLoader.getController();
                        setCenter(loadingView);
                        loadingCtrl.startLoading("Quiz - " + quizChoisi.getTitre(), this::naviguerVersQuestion);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                body.getChildren().addAll(meta, iconCircle, qTitre, qDesc, statsBox, btnCommencer);
                card.getChildren().add(body);

                // Hover sur la carte
                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:rgba(255,255,255,1); -fx-background-radius:30;" +
                    "-fx-border-color:" + color + "22; -fx-border-width:1; -fx-border-radius:30;" +
                    "-fx-effect:dropshadow(gaussian," + color + "44,28,0,0,12); -fx-cursor:hand;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color:rgba(255,255,255,0.98); -fx-background-radius:30;" +
                    "-fx-border-color:rgba(226,232,240,0.95); -fx-border-width:1; -fx-border-radius:30;" +
                    "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.10),22,0,0,10);"));

                cardsRow.getChildren().add(card);
            }

            centerBox.getChildren().add(cardsRow);
            root.getChildren().add(centerBox);
            javafx.scene.layout.StackPane.setAlignment(centerBox, javafx.geometry.Pos.CENTER);

            // Afficher
            if (labelTitreQuiz != null && labelTitreQuiz.getScene() != null) {
                sceneRef = labelTitreQuiz;
                setCenter(root);
            } else {
                javafx.application.Platform.runLater(() -> {
                    if (labelTitreQuiz != null && labelTitreQuiz.getScene() != null) {
                        sceneRef = labelTitreQuiz;
                        setCenter(root);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            setQuiz(quizActifs.get(quizActifs.size() - 1));
        }
    }

    /** Crée une colonne de statistique (icône + valeur + label) */
    private javafx.scene.layout.VBox makeStat(String icon, String value, String label, String color) {
        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label(icon);
        iconLbl.setStyle("-fx-font-size:20; -fx-background-color:" + color + ";" +
            "-fx-background-radius:50%; -fx-padding:5 7 5 7;");
        javafx.scene.control.Label valLbl = new javafx.scene.control.Label(value);
        valLbl.setStyle("-fx-font-size:20; -fx-font-weight:900; -fx-text-fill:#0f172a;");
        javafx.scene.control.Label lblLbl = new javafx.scene.control.Label(label);
        lblLbl.setStyle("-fx-font-size:9; -fx-text-fill:#94a3b8; -fx-font-weight:700;");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4, iconLbl, valLbl, lblLbl);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    /**
     * Initialise le quiz : charge les questions, calcule le total de points,
     * puis affiche l'écran d'introduction (intro.fxml).
     *
     * @param quiz le quiz à passer
     */
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        // Chargement des questions en ordre ALÉATOIRE
        // Chaque passage du quiz sera différent — les options sont aussi mélangées
        this.questions = serviceQuestion.findByQuizIdAleatoire(quiz.getId(), 0);
        // Calcul du total des points (somme des points de chaque question)
        this.totalPoints = questions.stream().mapToInt(Question::getPoint).sum();
        javafx.application.Platform.runLater(() -> { 
            if (labelTitreQuiz != null) sceneRef = labelTitreQuiz;
        });
        afficherIntro();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ÉCRAN 1 : INTRODUCTION (intro.fxml)
    // Affiche le titre, la description et les statistiques du quiz.
    // L'étudiant peut lancer le quiz ou retourner aux chapitres.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Remplit les labels de l'écran d'introduction avec les données du quiz.
     * Appelée après setQuiz() ou après un "Refaire".
     * Ne fait rien si labelTitreQuiz est null (vue non chargée).
     */
    private void afficherIntro() {
        if (labelTitreQuiz == null) return;
        labelTitreQuiz.setText("Quiz - " + quiz.getTitre());
        labelDescQuiz.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        labelNbQuestions.setText(String.valueOf(questions.size()));
        labelTotalPoints.setText(String.valueOf(totalPoints));
        labelDuree.setText(quiz.getDureeMaxMinutes() != null ? String.valueOf(quiz.getDureeMaxMinutes()) : "—");
    }

    /**
     * Action du bouton "▶ Commencer le quiz" (intro.fxml).
     * Réinitialise l'index de question et les réponses, puis charge l'écran
     * de chargement animé (loading.fxml). En cas d'erreur de chargement FXML,
     * navigue directement vers les questions.
     * 
     * ✅ FIX BUG 2 : Vérification si l'étudiant peut passer le quiz
     */
    @FXML
    private void onCommencer() {
        // ✅ VÉRIFICATION : L'étudiant peut-il passer ce quiz ?
        int etudiantId = tn.esprit.session.SessionManager.getCurrentUser().getId();
        java.util.Map<String, Object> check = serviceQuiz.canStudentTakeQuiz(etudiantId, quiz);
        boolean canTake = (boolean) check.get("canTake");
        
        if (!canTake) {
            // Afficher les erreurs et bloquer l'accès
            @SuppressWarnings("unchecked")
            java.util.List<String> errors = (java.util.List<String>) check.get("errors");
            
            // Créer une alerte d'erreur
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Accès refusé");
            alert.setHeaderText("Vous ne pouvez pas passer ce quiz");
            alert.setContentText(String.join("\n", errors));
            alert.showAndWait();
            
            System.err.println("❌ Accès refusé au quiz : " + String.join(", ", errors));
            return; // Bloquer l'accès
        }
        
        indexQuestion = 0;
        reponsesChoisies.clear();
        playStart(); // Son de démarrage du quiz
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/loading.fxml"));
            Parent view = loader.load();
            FrontQuizController loadingCtrl = loader.getController();
            if (sceneRef == null) sceneRef = labelTitreQuiz;
            setCenter(view);
            loadingCtrl.startLoading("Quiz - " + quiz.getTitre(), this::naviguerVersQuestion);
        } catch (Exception e) {
            e.printStackTrace();
            naviguerVersQuestion();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ÉCRAN DE CHARGEMENT (loading.fxml)
    // Affiche une animation de 2 secondes avant de passer aux questions.
    // Animations :
    //   - Rotation continue du conteneur de 4 carrés (logoContainer)
    //   - Pulsation (scale) de chaque carré avec un délai décalé (effet cascade)
    //   - Clignotement (fade) du label "Chargement du Quiz..."
    //   - Fondu sortant (fadeOut) du panneau entier avant de lancer onFinished
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Lance l'animation de chargement et programme la transition vers l'écran suivant.
     * Appelée par le contrôleur de l'intro après avoir chargé loading.fxml.
     *
     * @param titre      nom du quiz à afficher sous le logo animé
     * @param onFinished callback exécuté après le fondu sortant (navigue vers les questions)
     */
    public void startLoading(String titre, Runnable onFinished) {
        // Affiche le nom du quiz sous le logo
        if (quizNameLabel != null) quizNameLabel.setText(titre);

        if (logoContainer != null) {
            // Animation 1 : rotation continue du logo (360° en 1,5 s, en boucle)
            RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), logoContainer);
            rotate.setByAngle(360);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setInterpolator(Interpolator.EASE_BOTH);
            rotate.play();

            // Animation 2 : pulsation de chaque carré avec délai décalé (effet cascade)
            // Délais : bleu=0s, vert=0.2s, rouge=0.4s, orange=0.6s
            animerCarre(squareBlue, 0.0); animerCarre(squareGreen, 0.2);
            animerCarre(squareRed,  0.4); animerCarre(squareOrange, 0.6);
        }

        if (loadingLabel != null) {
            // Animation 3 : clignotement du texte "Chargement du Quiz..." (opacité 1.0 → 0.4)
            FadeTransition fade = new FadeTransition(Duration.seconds(1.2), loadingLabel);
            fade.setFromValue(1.0); fade.setToValue(0.4);
            fade.setCycleCount(Animation.INDEFINITE); fade.setAutoReverse(true);
            fade.play();
        }

        // Pause de 2 secondes, puis fondu sortant de 400 ms avant de lancer le callback
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            if (rootPane != null) {
                // Animation 4 : fondu sortant du panneau entier (opacité 1.0 → 0.0 en 400 ms)
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), rootPane);
                fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> onFinished.run());
                fadeOut.play();
            } else { onFinished.run(); }
        });
        pause.play();
    }

    /**
     * Applique une animation de pulsation (scale 1.0 → 1.15) à un carré du logo.
     * L'animation est en boucle infinie avec auto-reverse (effet de respiration).
     *
     * @param square le Rectangle JavaFX à animer
     * @param delay  délai en secondes avant le début de l'animation (effet cascade)
     */
    private void animerCarre(Rectangle square, double delay) {
        if (square == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.75), square);
        scale.setFromX(1.0); scale.setToX(1.15);
        scale.setFromY(1.0); scale.setToY(1.15);
        scale.setCycleCount(Animation.INDEFINITE); scale.setAutoReverse(true);
        scale.setDelay(Duration.seconds(delay));
        scale.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ÉCRAN 2 : QUESTIONS (question.fxml)
    // Affiche les questions une par une avec leurs options.
    // L'étudiant clique sur une option → réponse enregistrée → question suivante.
    // Un timer décompte le temps restant si le quiz a une durée maximale.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Charge question.fxml dans un nouveau contrôleur et y transfère tout l'état
     * (quiz, questions, réponses, index, callbacks). Puis affiche la première question.
     * Appelée depuis startLoading() via le callback onFinished.
     */
    private void naviguerVersQuestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/question.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            // Transfert de l'état complet vers le nouveau contrôleur de question
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.indexQuestion = this.indexQuestion;
            ctrl.reponsesChoisies.putAll(this.reponsesChoisies);
            ctrl.optionsParQuestion.putAll(this.optionsParQuestion);
            ctrl.onRetourCallback = this.onRetourCallback;
            setCenter(view);
            ctrl.sceneRef = view;
            ctrl.afficherQuestion();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Affiche la question à l'index courant (indexQuestion).
     * Met à jour : en-tête, progression, texte de la question, points,
     * compteur de réponses, timer et liste des options.
     */
    // ══════════════════════════════════════════════════════════════════════════
    // DÉCORATIONS — Cercles translucides sur le fond de la page question
    // ══════════════════════════════════════════════════════════════════════════

    /** Indique si les décorations ont déjà été ajoutées (évite les doublons). */
    private boolean decorationsAjoutees = false;

    /**
     * Ajoute des cercles et formes translucides sur le BorderPane de la page question.
     * Appelée une seule fois lors du premier affichage d'une question.
     */
    private void ajouterDecorationsQuestionPage() {
        if (decorationsAjoutees || labelQuestion == null) return;
        try {
            // Remonter jusqu'au BorderPane de question.fxml
            javafx.scene.Node node = labelQuestion;
            while (node != null && !(node instanceof javafx.scene.layout.BorderPane)) {
                node = node.getParent();
            }
            if (!(node instanceof javafx.scene.layout.BorderPane bp)) return;

            // Créer un StackPane de décorations en arrière-plan
            javafx.scene.layout.StackPane decoPane = new javafx.scene.layout.StackPane();
            decoPane.setMouseTransparent(true);
            decoPane.setStyle("-fx-background-color:transparent;");

            double[][] cercles = {
                // Grands cercles pleins
                {200, -380, -200, 0.06}, {170, 380,  -190, 0.05},
                {150, -370,  230, 0.07}, {180, 370,   220, 0.04},
                {130,    0, -260, 0.06}, {100, -100,  280, 0.05},
                {110,  200,  260, 0.06},
                // Cercles moyens
                {75,  180, -230, 0.08}, {65, -280,   60, 0.09},
                {60,  280,  100, 0.07}, {55, -180, -100, 0.08},
                {50,  150,  230, 0.07}, {45, -350,  -50, 0.09},
                {40,  350,  -50, 0.08}, {35,    0,  200, 0.10},
                {30, -150, -200, 0.09}, {28,  320, -220, 0.08},
                // Points lumineux
                {7,  -200, -240, 0.28}, {5,   250,  -90, 0.22},
                {6,  -150,  180, 0.25}, {8,   200,  230, 0.20},
                {5,   100, -260, 0.28}, {6,  -320,  150, 0.22},
                {4,   380,   40, 0.35}, {5,   -80,  270, 0.25},
                {4,   320, -260, 0.30}, {3,    60, -290, 0.40},
                {4,   -60,  290, 0.30}, {5,   240,  180, 0.25},
                {3,  -240, -180, 0.35}, {4,   160, -160, 0.28},
                {3,   340,  100, 0.38}, {4,  -340, -100, 0.30},
            };

            for (double[] c : cercles) {
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(c[0]);
                circle.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, c[3]));
                circle.setTranslateX(c[1]);
                circle.setTranslateY(c[2]);
                circle.setMouseTransparent(true);
                decoPane.getChildren().add(circle);
            }

            // Rectangles inclinés
            javafx.scene.shape.Rectangle r1 = new javafx.scene.shape.Rectangle(80, 80);
            r1.setArcWidth(20); r1.setArcHeight(20);
            r1.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.05));
            r1.setRotate(30); r1.setTranslateX(-300); r1.setTranslateY(-100);
            r1.setMouseTransparent(true);

            javafx.scene.shape.Rectangle r2 = new javafx.scene.shape.Rectangle(60, 60);
            r2.setArcWidth(14); r2.setArcHeight(14);
            r2.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, 0.06));
            r2.setRotate(45); r2.setTranslateX(310); r2.setTranslateY(150);
            r2.setMouseTransparent(true);

            // Rectangles inclinés
            double[][] rects = {
                {90, 22, 30, -300, -100, 0.05}, {70, 16, 45,  310,  150, 0.06},
                {55, 12, 20,  200, -180, 0.06}, {45, 10, 60, -200,  220, 0.08},
                {35,  8, 15,  100,  260, 0.09}, {30,  7, 75, -350,  200, 0.07},
            };
            for (double[] r : rects) {
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(r[0], r[0]);
                rect.setArcWidth(r[1]); rect.setArcHeight(r[1]);
                rect.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, r[5]));
                rect.setRotate(r[2]); rect.setTranslateX(r[3]); rect.setTranslateY(r[4]);
                rect.setMouseTransparent(true);
                decoPane.getChildren().add(rect);
            }

            // Anneaux (cercles vides avec contour)
            double[][] anneaux = {
                {120, -250, 180, 0.10, 2.0}, {90, 260, -150, 0.12, 2.0},
                {70, -100, -220, 0.09, 1.5}, {55, 320,  150, 0.14, 1.5},
                {40,   80,  270, 0.12, 1.0}, {35, -300, -240, 0.10, 1.0},
            };
            for (double[] a : anneaux) {
                javafx.scene.shape.Circle anneau = new javafx.scene.shape.Circle(a[0]);
                anneau.setFill(javafx.scene.paint.Color.TRANSPARENT);
                anneau.setStroke(javafx.scene.paint.Color.rgb(255, 255, 255, a[3]));
                anneau.setStrokeWidth(a[4]);
                anneau.setTranslateX(a[1]); anneau.setTranslateY(a[2]);
                anneau.setMouseTransparent(true);
                decoPane.getChildren().add(anneau);
            }

            // Ellipses
            double[][] ellipses = {
                {220, 65, 0, 290, 0.04}, {150, 50, -150, -270, 0.05},
                {110, 38, 300, 260, 0.06}, {80, 30, -320, 100, 0.07},
            };
            for (double[] e : ellipses) {
                javafx.scene.shape.Ellipse ellipse = new javafx.scene.shape.Ellipse(e[0], e[1]);
                ellipse.setFill(javafx.scene.paint.Color.rgb(255, 255, 255, e[4]));
                ellipse.setTranslateX(e[2]); ellipse.setTranslateY(e[3]);
                ellipse.setMouseTransparent(true);
                decoPane.getChildren().add(ellipse);
            }

            decoPane.getChildren(); // flush

            // Injecter le decoPane comme fond du BorderPane
            bp.getChildren().add(0, decoPane);
            decoPane.prefWidthProperty().bind(bp.widthProperty());
            decoPane.prefHeightProperty().bind(bp.heightProperty());

            decorationsAjoutees = true;
        } catch (Exception e) {
            System.err.println("Décorations non ajoutées : " + e.getMessage());
        }
    }

    private void afficherQuestion() {
        if (questions == null || questions.isEmpty() || labelQuestion == null) {
            System.err.println("[afficherQuestion] vue question non initialisée ou liste vide : questions="
                + (questions != null ? questions.size() : "null")
                + ", labelQuestion=" + labelQuestion);
            return;
        }

        // ── Décorations de fond (cercles translucides) ──
        // Ajoutées une seule fois sur le BorderPane racine
        ajouterDecorationsQuestionPage();

        Question q = questions.get(indexQuestion);
        labelTitreHeader.setText("Quiz - " + quiz.getTitre());
        labelProgress.setText("Question " + (indexQuestion + 1) + " / " + questions.size());
        labelQuestion.setText(q.getTexteQuestion());
        labelPoints.setText("⭐ " + q.getPoint() + " points");

        // ── Afficher l'image du quiz dans la carte question ───────────────────
        if (quizImageView != null) {
            if (quiz.getImageName() != null && !quiz.getImageName().isBlank()) {
                try {
                    java.nio.file.Path imgPath = java.nio.file.Paths.get(
                        "src/main/resources/images/quiz", quiz.getImageName());
                    if (java.nio.file.Files.exists(imgPath)) {
                        quizImageView.setImage(new javafx.scene.image.Image(imgPath.toUri().toString()));
                        quizImageView.setVisible(true);
                        quizImageView.setManaged(true);
                    }
                } catch (Exception ex) {
                    quizImageView.setVisible(false);
                    quizImageView.setManaged(false);
                }
            } else {
                quizImageView.setVisible(false);
                quizImageView.setManaged(false);
            }
        }

        // ── Barre de progression animée ──
        double progress = (double)(indexQuestion + 1) / questions.size();
        if (progressBar != null) {
            // Animation fluide de la barre
            javafx.animation.Timeline anim = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300),
                    new javafx.animation.KeyValue(progressBar.progressProperty(), progress,
                        javafx.animation.Interpolator.EASE_OUT))
            );
            anim.play();
        }
        if (labelPourcentageProgress != null)
            labelPourcentageProgress.setText((int)(progress * 100) + "%");

        // ── Points de navigation (un cercle par question) ──
        if (questionDots != null) {
            questionDots.getChildren().clear();
            for (int i = 0; i < questions.size(); i++) {
                Label dot = new Label("●");
                boolean isAnswered = reponsesChoisies.containsKey(questions.get(i).getId());
                boolean isCurrent  = i == indexQuestion;
                if (isCurrent) {
                    dot.setStyle("-fx-font-size:14; -fx-text-fill:white;");
                } else if (isAnswered) {
                    dot.setStyle("-fx-font-size:10; -fx-text-fill:#22c55e;");
                } else {
                    dot.setStyle("-fx-font-size:10; -fx-text-fill:rgba(255,255,255,0.3);");
                }
                final int idx = i;
                dot.setOnMouseClicked(e -> {
                    // Navigation directe vers une question en cliquant sur son point
                    indexQuestion = idx;
                    afficherQuestion();
                });
                dot.setStyle(dot.getStyle() + "-fx-cursor:hand;");
                questionDots.getChildren().add(dot);
            }
        }

        // Initialiser le bouton soumettre
        if (btnSoumettre != null && reponsesChoisies.size() < questions.size()) {
            btnSoumettre.setDisable(true);
        }
        mettreAJourRepondues();
        demarrerTimer();
        afficherOptions(q);
    }

    /**
     * Génère dynamiquement les boutons d'options pour la question donnée.
     * L'option déjà sélectionnée est mise en surbrillance (fond violet dégradé).
     * Au clic sur une option :
     *   1. La réponse est enregistrée dans reponsesChoisies
     *   2. Les boutons sont redessinés (mise à jour visuelle)
     *   3. Après 400 ms, on passe automatiquement à la question suivante
     *      (si ce n'est pas la dernière question)
     *
     * @param q la question dont on affiche les options
     */
    private void afficherOptions(Question q) {
        List<Option> opts = optionsParQuestion.computeIfAbsent(q.getId(), id -> {
            // Charger les options et les mélanger aléatoirement
            List<Option> loaded = serviceOption.findByQuestionId(id);
            java.util.Collections.shuffle(loaded);
            return loaded;
        });
        optionsQuestionCourante = opts;
        Integer dejaChoisi = reponsesChoisies.get(q.getId());

        // Couleurs exactes Kahoot/AutoLearn selon COULEURS_QUIZ_JAVAFX.md
        String[][] palette = {
            {"linear-gradient(to bottom right,#e74c3c,#c0392b)", "▲"},  // Rouge
            {"linear-gradient(to bottom right,#3498db,#2980b9)", "◆"},  // Bleu
            {"linear-gradient(to bottom right,#f39c12,#e67e22)", "●"},  // Orange
            {"linear-gradient(to bottom right,#2ecc71,#27ae60)", "■"},  // Vert
        };

        // Utiliser la grille 2x2 si disponible
        if (optionsGrid != null) {
            optionsGrid.getChildren().clear();
            optionsGrid.getColumnConstraints().clear();
            optionsGrid.getRowConstraints().clear();

            // 2 colonnes égales
            for (int c = 0; c < 2; c++) {
                javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
                cc.setPercentWidth(50);
                cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                optionsGrid.getColumnConstraints().add(cc);
            }

            for (int i = 0; i < opts.size(); i++) {
                Option opt = opts.get(i);
                String[] p = palette[i % palette.length];
                String color = p[0];
                String icon  = p[1];

                boolean sel = dejaChoisi != null && dejaChoisi == opt.getId();

                // Icône en haut à gauche
                Label iconLbl = new Label(icon);
                iconLbl.setStyle("-fx-font-size:18; -fx-text-fill:rgba(255,255,255,0.7);");

                Label textLbl = new Label(opt.getTexteOption());
                textLbl.setWrapText(true);
                textLbl.setAlignment(javafx.geometry.Pos.CENTER);
                textLbl.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:white; -fx-text-alignment:CENTER;");
                textLbl.setMaxWidth(Double.MAX_VALUE);
                textLbl.setPrefWidth(0); // force le wrap dans la grille

                VBox iconTop = new VBox(4, iconLbl);
                iconTop.setAlignment(javafx.geometry.Pos.TOP_LEFT);

                VBox content = new VBox(8, iconTop, textLbl);
                content.setAlignment(javafx.geometry.Pos.CENTER);
                content.setPadding(new Insets(16, 20, 16, 20));
                content.setMaxWidth(Double.MAX_VALUE);

                String bgColor = sel ? "derive(" + color + ", -20%)" : color;
                String border  = sel ? "-fx-border-color:white; -fx-border-width:3; -fx-border-radius:14;" : "";
                content.setStyle(
                    "-fx-background-color:" + bgColor + ";" +
                    "-fx-background-radius:15; -fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),10,0,0,5);" +
                    border
                );
                content.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                content.setMinHeight(90);

                final int idx = i;
                content.setOnMouseClicked(e -> {
                    playClick();
                    reponsesChoisies.put(q.getId(), opt.getId());
                    afficherOptions(q);
                    mettreAJourRepondues();
                    // Passer à la question suivante après 400ms
                    PauseTransition pause = new PauseTransition(Duration.millis(400));
                    pause.setOnFinished(ev -> {
                        if (indexQuestion < questions.size() - 1) {
                            indexQuestion++;
                            afficherQuestion();
                        }
                    });
                    pause.play();
                });

                // Hover effect
                content.setOnMouseEntered(e -> content.setStyle(
                    "-fx-background-color:derive(" + color + ", 15%);" +
                    "-fx-background-radius:14; -fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),12,0,0,4);" + border
                ));
                content.setOnMouseExited(e -> content.setStyle(
                    "-fx-background-color:" + bgColor + ";" +
                    "-fx-background-radius:14; -fx-cursor:hand;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.25),8,0,0,3);" + border
                ));

                int row = i / 2;
                int col = i % 2;
                optionsGrid.add(content, col, row);
                GridPane.setFillWidth(content, true);
                GridPane.setFillHeight(content, true);
                GridPane.setHgrow(content, Priority.ALWAYS);
                GridPane.setVgrow(content, Priority.ALWAYS);
            }
        } else if (optionsContainer != null) {
            // Fallback liste verticale
            optionsContainer.getChildren().clear();
            for (Option opt : opts) {
                Button btn = new Button(opt.getTexteOption());
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setWrapText(true);
                btn.setPadding(new Insets(14, 20, 14, 20));
                boolean sel = dejaChoisi != null && dejaChoisi == opt.getId();
                btn.setStyle(sel
                    ? "-fx-background-color:linear-gradient(to right,#9333ea,#7c3aed);-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:700;-fx-background-radius:12;-fx-cursor:hand;-fx-border-width:0;"
                    : "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:600;-fx-background-radius:12;-fx-cursor:hand;-fx-border-width:2;-fx-border-color:rgba(255,255,255,0.3);-fx-border-radius:12;"
                );
                btn.setOnAction(e -> {
                    playClick();
                    reponsesChoisies.put(q.getId(), opt.getId());
                    afficherOptions(q);
                    mettreAJourRepondues();
                    PauseTransition p = new PauseTransition(Duration.millis(400));
                    p.setOnFinished(ev -> { if (indexQuestion < questions.size() - 1) { indexQuestion++; afficherQuestion(); } });
                    p.play();
                });
                optionsContainer.getChildren().add(btn);
            }
        }

        // Boutons nav
        if (btnPrev != null) btnPrev.setDisable(indexQuestion == 0);
        if (btnNext != null) btnNext.setDisable(indexQuestion >= questions.size() - 1);
    }

    @FXML
    private void onPrev() {
        if (indexQuestion > 0) { indexQuestion--; afficherQuestion(); }
    }

    @FXML
    private void onNext() {
        if (indexQuestion < questions.size() - 1) { indexQuestion++; afficherQuestion(); }
    }

    /**
     * Met à jour le label de progression des réponses.
     * Ex : "3 / 10 questions répondues"
     */
    private void mettreAJourRepondues() {
        int repondues = reponsesChoisies.size();
        int total = questions.size();

        if (labelRepondues != null)
            labelRepondues.setText(repondues + " / " + total + " questions répondues");

        // Mettre à jour la barre de progression
        if (progressBar != null) {
            double prog = total > 0 ? (double) repondues / total : 0;
            javafx.animation.Timeline anim = new javafx.animation.Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(progressBar.progressProperty(), prog, Interpolator.EASE_OUT))
            );
            anim.play();
        }

        // Activer le bouton soumettre quand toutes les questions sont répondues
        if (btnSoumettre != null) {
            boolean toutesRepondues = repondues >= total;
            btnSoumettre.setDisable(!toutesRepondues);
            btnSoumettre.setStyle(toutesRepondues
                ? "-fx-background-color:linear-gradient(to bottom right,#2ecc71,#27ae60);" +
                  "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;" +
                  "-fx-padding:10 24 10 24; -fx-background-radius:24;" +
                  "-fx-cursor:hand; -fx-border-width:0;" +
                  "-fx-effect:dropshadow(gaussian,rgba(46,204,113,0.4),15,0,0,5);"
                : "-fx-background-color:linear-gradient(to bottom right,#95a5a6,#7f8c8d);" +
                  "-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:13; -fx-font-weight:700;" +
                  "-fx-padding:10 24 10 24; -fx-background-radius:24;" +
                  "-fx-cursor:default; -fx-border-width:0;"
            );
        }
    }

    /**
     * Démarre (ou redémarre) le timer pour la question courante.
     * Si le quiz n'a pas de durée maximale, affiche "⏱  ∞" et ne démarre pas de timer.
     * Sinon, décrémente secondesRestantes chaque seconde.
     * Quand le temps atteint 0, soumet automatiquement le quiz (onSoumettre()).
     * Le timer est partagé pour tout le quiz (pas réinitialisé à chaque question).
     */
    private void demarrerTimer() {
        if (timerTimeline != null) timerTimeline.stop();
        if (quiz.getDureeMaxMinutes() == null) { if (labelTimer != null) labelTimer.setText("⏱  ∞"); return; }
        secondesRestantes = quiz.getDureeMaxMinutes() * 60;
        mettreAJourAffichageTimer();
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesRestantes--;
            mettreAJourAffichageTimer();
            // Soumission automatique quand le temps est écoulé
            if (secondesRestantes <= 0) { timerTimeline.stop(); onSoumettre(); }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    /**
     * Formate et affiche le temps restant dans le label du timer.
     * Format : "⏱  M:SS" (ex : "⏱  2:05")
     */
    private void mettreAJourAffichageTimer() {
        if (labelTimer != null)
            labelTimer.setText(String.format("⏱  %d:%02d", secondesRestantes / 60, secondesRestantes % 60));
    }

    /**
     * Action du bouton "✔ Soumettre le Quiz" (question.fxml).
     * Arrête le timer et navigue vers l'écran de résultats.
     */
    @FXML
    private void onSoumettre() {
        if (timerTimeline != null) timerTimeline.stop();
        playFinish(); // Son de fin de quiz
        naviguerVersResultat();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ÉCRAN 3 : RÉSULTATS (resultat.fxml)
    // Calcule et affiche le score final, le pourcentage de réussite,
    // un message contextuel et les statistiques de la session.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Charge resultat.fxml dans un nouveau contrôleur et y transfère l'état
     * (quiz, questions, réponses, options en cache, callbacks).
     * Puis appelle afficherResultat() pour calculer et afficher le score.
     */
    private void naviguerVersResultat() {
        try {
            java.net.URL vueResultat = getClass().getResource("/views/frontoffice/quiz/resultat.fxml");
            if (vueResultat == null) {
                throw new IllegalStateException("Aucune vue de résultat quiz n'a été trouvée");
            }

            FXMLLoader loader = new FXMLLoader(vueResultat);
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            // Transfert de l'état complet vers le contrôleur de résultats
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.reponsesChoisies.putAll(this.reponsesChoisies);
            ctrl.optionsParQuestion.putAll(this.optionsParQuestion);
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelQuestion;
            ctrl.afficherResultat();
            setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Calcule le score et remplit tous les labels de l'écran de résultats.
     *
     * Algorithme de calcul du score :
     *   Pour chaque question du quiz :
     *     1. Récupère l'ID de l'option choisie par l'étudiant (reponsesChoisies)
     *     2. Récupère la liste des options (depuis le cache optionsParQuestion ou la BDD)
     *     3. Trouve l'option marquée "estCorrecte = true"
     *     4. Si l'option correcte correspond à l'option choisie → ajoute les points de la question
     *   Pourcentage = (pointsObtenus / totalPoints) × 100, arrondi à 2 décimales
     *
     * Messages contextuels selon le pourcentage vs seuil de réussite :
     *   - pct >= seuil          → "Félicitations !" (vert)
     *   - pct >= seuil / 2      → "Peut mieux faire" (orange)
     *   - pct < seuil / 2       → "Score insuffisant" (rouge)
     *   
     * ✅ FIX BUG 1 : Enregistrement de la tentative après soumission
     * ✅ FIX BUG 3 : Utilisation des vraies statistiques
     * ✅ NOUVEAU : Système de badges, XP et statistiques avancées
     */
    private void afficherResultat() {
        if (labelTitreResultat == null) return;
        
        int pointsObtenus = 0;
        java.util.Map<Integer, Boolean> detailsReponses = new java.util.HashMap<>();
        
        for (Question q : questions) {
            Integer choisi = reponsesChoisies.get(q.getId());
            boolean correct = false;
            
            if (choisi != null) {
                List<Option> opts = optionsParQuestion.getOrDefault(q.getId(), serviceOption.findByQuestionId(q.getId()));
                for (Option o : opts) {
                    if (o.isEstCorrecte()) {
                        if (o.getId() == choisi.intValue()) {
                            pointsObtenus += q.getPoint();
                            correct = true;
                        }
                        break;
                    }
                }
            }
            
            detailsReponses.put(q.getId(), correct);
        }
        
        // Calcul du pourcentage, arrondi à 2 décimales
        double pct = totalPoints > 0 ? Math.round((pointsObtenus * 100.0 / totalPoints) * 100.0) / 100.0 : 0.0;
        int seuil = quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50;

        // Calculer la durée (si timer était actif)
        int dureeSecondes = 0;
        if (quiz.getDureeMaxMinutes() != null) {
            dureeSecondes = (quiz.getDureeMaxMinutes() * 60) - secondesRestantes;
        }

        // ✅ FIX BUG 1 : Enregistrer la tentative terminée avec détails complets
        int etudiantId = tn.esprit.session.SessionManager.getCurrentUser().getId();
        serviceQuiz.enregistrerTentative(etudiantId, quiz.getId(), pointsObtenus, totalPoints, pct, dureeSecondes, detailsReponses);
        
        // ✅ FIX BUG 3 : Récupérer les vraies statistiques
        java.util.Map<String, Object> statistiques = serviceQuiz.getStatistiquesEtudiant(etudiantId, quiz);
        int nombreTentatives = (int) statistiques.get("nombreTentatives");
        Integer maxTentatives = (Integer) statistiques.get("maxTentatives");
        boolean peutRecommencer = (boolean) statistiques.get("peutRecommencer");
        double meilleurScore = (double) statistiques.get("meilleurScore");
        
        // Récupérer les nouveaux badges et XP
        @SuppressWarnings("unchecked")
        java.util.Set<String> badges = (java.util.Set<String>) statistiques.get("badges");
        
        // Récupérer XP gagné depuis les derniers résultats
        java.util.Map<String, Object> derniers = serviceQuiz.getDerniersResultats(etudiantId, quiz.getId());
        int xpGagne = derniers != null ? (int) derniers.getOrDefault("xpGagne", 0) : 0;
        int xpTotal = (int) statistiques.get("xp");
        int niveau = (int) statistiques.get("niveau");
        String titreNiveau = (String) statistiques.get("titreNiveau");

        // Remplissage des labels principaux
        labelTitreResultat.setText("Quiz - " + quiz.getTitre());
        labelPointsObtenus.setText(String.valueOf(pointsObtenus));
        labelPourcentage.setText(String.format("%.0f%%", pct));
        labelPointsTotal.setText(String.valueOf(totalPoints));

        // ── NOUVEAUX LABELS XP ET NIVEAU ──
        if (labelXPGagne != null) {
            labelXPGagne.setText("+" + xpGagne + " XP");
        }
        if (labelXPTotal != null) {
            labelXPTotal.setText(String.format("%,d XP", xpTotal));
        }
        if (labelNiveau != null) {
            labelNiveau.setText("Niveau " + niveau);
        }
        if (labelTitreNiveau != null) {
            labelTitreNiveau.setText(titreNiveau.toUpperCase().replace("🌱 ", "")
                .replace("🎯 ", "").replace("💎 ", "").replace("⭐ ", "").replace("🏆 ", ""));
        }
        if (labelIconeNiveau != null) {
            // Extraire l'emoji du titre
            String icone = titreNiveau.split(" ")[0];
            labelIconeNiveau.setText(icone);
        }

        // ── AFFICHAGE DES BADGES ──
        if (containerBadges != null && flowPaneBadges != null && !badges.isEmpty()) {
            containerBadges.setVisible(true);
            containerBadges.setManaged(true);
            flowPaneBadges.getChildren().clear();
            
            for (String badge : badges) {
                // Créer une carte pour chaque badge
                javafx.scene.layout.VBox badgeCard = new javafx.scene.layout.VBox();
                badgeCard.setAlignment(javafx.geometry.Pos.CENTER);
                badgeCard.setSpacing(6);
                badgeCard.setStyle(
                    "-fx-background-color:linear-gradient(to bottom right,#fef3c7,#fde68a);" +
                    "-fx-background-radius:12; -fx-padding:16; -fx-min-width:120; -fx-min-height:100;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,2);"
                );
                
                // Extraire l'emoji et le nom du badge
                String[] parts = badge.split(" ", 2);
                String emoji = parts.length > 0 ? parts[0] : "🏅";
                String nom = parts.length > 1 ? parts[1] : badge;
                
                Label emojiLabel = new Label(emoji);
                emojiLabel.setStyle("-fx-font-size:36;");
                
                Label nomLabel = new Label(nom);
                nomLabel.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#92400e; -fx-text-alignment:center;");
                nomLabel.setWrapText(true);
                nomLabel.setMaxWidth(110);
                
                badgeCard.getChildren().addAll(emojiLabel, nomLabel);
                flowPaneBadges.getChildren().add(badgeCard);
            }
        }

        // ── PROGRESSION : marquer le chapitre comme complété si quiz réussi ──
        if (pct >= seuil && chapitre != null) {
            try {
                tn.esprit.services.CourseProgressService progressService =
                    new tn.esprit.services.CourseProgressService();
                int userId = tn.esprit.session.SessionManager.getCurrentUser().getId();
                int coursId = chapitre.getCoursId();
                System.out.println("DEBUG progression: userId=" + userId
                    + " chapitreId=" + chapitre.getId()
                    + " coursId=" + coursId
                    + " score=" + (int)pct + "%");
                progressService.markChapterCompleted(userId, chapitre.getId(), coursId, (int) pct);
                System.out.println("✅ Chapitre " + chapitre.getId() + " marqué complété — score: " + (int)pct + "%");
            } catch (Exception ex) {
                System.err.println("Erreur progression: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Message et couleur selon le résultat
        String messageTexte = "";
        String messageStyle = "";
        
        if (pct >= seuil) {
            messageTexte = "🎉  Félicitations ! Vous avez réussi le quiz !";
            messageStyle = "-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#059669;-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-padding:10 20 10 20;";
        } else if (pct >= (double) seuil / 2) {
            messageTexte = "📈  Peut mieux faire — continuez vos efforts !";
            messageStyle = "-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#d97706;-fx-background-color:#fffbeb;-fx-background-radius:10;-fx-padding:10 20 10 20;";
        } else {
            messageTexte = "😔  Score insuffisant — révisez et réessayez !";
            messageStyle = "-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#dc2626;-fx-background-color:#fef2f2;-fx-background-radius:10;-fx-padding:10 20 10 20;";
        }

        if (titreNiveau != null && !titreNiveau.isBlank()) {
            String niveauTexte = titreNiveau.replace("🌱 ", "")
                .replace("🎯 ", "").replace("💎 ", "").replace("⭐ ", "").replace("🏆 ", "");
            messageTexte += "  |  Niveau " + niveau + "  " + niveauTexte;
        }
        
        labelMessage.setText(messageTexte);
        labelMessage.setStyle(messageStyle);

        // ── NOTIFICATIONS EMAIL ──────────────────────────────────────────────
        // Envoi asynchrone (ne bloque pas l'UI) uniquement si l'étudiant a échoué
        try {
            tn.esprit.entities.User user = tn.esprit.session.SessionManager.getCurrentUser();
            if (user != null && user.getEmail() != null && pct < seuil) {
                String email  = user.getEmail();
                String prenom = user.getPrenom();
                int scorePct  = (int) pct;

                if (pct < (double) seuil / 2) {
                    // Score très bas → rappel de révision du chapitre
                    String chapitreTitre = chapitre != null ? chapitre.getTitre() : "ce chapitre";
                    EmailService.sendRevisionReminder(email, prenom, quiz.getTitre(), chapitreTitre, scorePct);
                } else {
                    // Score insuffisant mais pas catastrophique → rappel de refaire le quiz
                    EmailService.sendQuizRetryReminder(email, prenom, quiz.getTitre(),
                        scorePct, seuil, nombreTentatives, maxTentatives);
                }
            }
        } catch (Exception ex) {
            System.err.println("[Notification] Erreur envoi email : " + ex.getMessage());
        }

        // ✅ Statistiques réelles (pas hardcodées)
        if (labelTentative != null) labelTentative.setText(String.valueOf(nombreTentatives));
        if (labelMaxTentatives != null) {
            String maxText = maxTentatives != null ? String.valueOf(maxTentatives) : "∞";
            labelMaxTentatives.setText("TENTATIVE / " + maxText);
        }
        if (labelMeilleurScore != null) labelMeilleurScore.setText(String.format("%.0f%%", meilleurScore));
        if (labelPeutRecommencer != null) {
            labelPeutRecommencer.setText(peutRecommencer ? "OUI" : "NON");
            if (!peutRecommencer) {
                labelPeutRecommencer.setStyle("-fx-font-size:22;-fx-font-weight:900;-fx-text-fill:#ef4444;");
            } else {
                labelPeutRecommencer.setStyle("-fx-font-size:22;-fx-font-weight:900;-fx-text-fill:#3b82f6;");
            }
        }
        
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS — boutons de navigation (résultat.fxml et intro.fxml)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Action du bouton "🏠 Accueil" (resultat.fxml).
     * Arrête le timer, puis tente de déclencher le bouton "btnHome" de la barre
     * de navigation principale via lookup sur la scène. Si introuvable, exécute
     * le callback de retour aux chapitres.
     */
    @FXML
    private void onAccueil() {
        if (timerTimeline != null) timerTimeline.stop();
        if (sceneRef != null && sceneRef.getScene() != null) {
            // Recherche du bouton "Accueil" dans la barre de navigation du layout principal
            javafx.scene.Node btn = sceneRef.getScene().getRoot().lookup("#btnHome");
            if (btn instanceof Button b) { b.fire(); return; }
        }
        if (onRetourCallback != null) onRetourCallback.run();
    }

    /**
     * Action du bouton "↺ Refaire le quiz" (resultat.fxml).
     * Réinitialise l'index et les réponses, recharge intro.fxml avec le même quiz.
     * Transfère le callback de retour et la référence de scène au nouveau contrôleur.
     */
    @FXML
    private void onRefaire() {
        indexQuestion = 0;
        reponsesChoisies.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/intro.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();

            // Transfert complet de l'état
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.onRetourCallback = this.onRetourCallback;
            // Transmettre la référence de scène stable (labelCurrentUser ou sceneRef)
            ctrl.sceneRef = this.sceneRef;

            // Injecter la vue dans le layout principal
            setCenter(view);

            // Remplir les labels de l'intro (la vue est maintenant dans la scène)
            javafx.application.Platform.runLater(ctrl::afficherIntro);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du rechargement du quiz : " + e.getMessage());
        }
    }

    /**
     * Action du bouton "← Retour aux chapitres" (intro.fxml et resultat.fxml).
     * Arrête le timer et exécute le callback de retour fourni par le contrôleur parent.
     */
    @FXML
    private void onRetour() {
        if (timerTimeline != null) timerTimeline.stop();
        if (onRetourCallback != null) onRetourCallback.run();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRE — navigation entre vues
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Injecte une nouvelle vue dans le centre du BorderPane racine de la scène.
     * Remonte la hiérarchie de nœuds depuis sceneRef (ou les labels disponibles)
     * jusqu'à la racine de la scène, puis appelle setCenter() sur le BorderPane.
     *
     * Ordre de priorité pour trouver la référence de scène :
     *   1. sceneRef (fourni explicitement)
     *   2. labelTitreQuiz (intro.fxml)
     *   3. labelQuestion (question.fxml)
     *   4. labelTitreResultat (resultat.fxml)
     *
     * @param view la nouvelle vue Parent à afficher au centre du layout
     */
    private void setCenter(Parent view) {
        // Priorité 1 : utiliser la scène directement si disponible (chemin le plus fiable)
        javafx.scene.Scene scene = null;
        if (sceneRef != null && sceneRef.getScene() != null) {
            scene = sceneRef.getScene();
        } else {
            // Chercher via les labels disponibles
            for (javafx.scene.Node n : new javafx.scene.Node[]{
                    labelTitreQuiz, labelQuestion, labelTitreResultat}) {
                if (n != null && n.getScene() != null) { scene = n.getScene(); break; }
            }
        }

        BorderPane root = null;
        if (scene != null && scene.getRoot() instanceof BorderPane bp) {
            root = bp;
        } else {
            // Fallback : remonter l'arbre jusqu'au BorderPane le plus haut
            javafx.scene.Node ref = sceneRef != null ? sceneRef
                : labelTitreQuiz != null ? labelTitreQuiz
                : labelQuestion != null ? labelQuestion
                : labelTitreResultat;
            if (ref != null) {
                javafx.scene.Parent current = ref instanceof javafx.scene.Parent p ? p : ref.getParent();
                while (current != null) {
                    if (current instanceof BorderPane bp2) root = bp2;
                    current = current.getParent();
                }
            }
        }

        if (root == null) {
            System.err.println("[setCenter] impossible de localiser le BorderPane racine");
            return;
        }

        // Les vues quiz prennent toute la hauteur disponible (pas de ScrollPane wrapper)
        if (view instanceof javafx.scene.layout.Region region) {
            region.prefHeightProperty().unbind();
            region.prefWidthProperty().unbind();
            region.setMaxHeight(Double.MAX_VALUE);
            region.setMaxWidth(Double.MAX_VALUE);
        }
        root.setCenter(view);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SONS — Sons instantanés via Clip pré-chargé (fusionné depuis SoundPlayer)
    // ══════════════════════════════════════════════════════════════════════════

    /** Toggle son ON/OFF — appelé par le bouton 🔊/🔇 dans question.fxml */
    @FXML
    private void onToggleSound() {
        soundEnabled = !soundEnabled;
        if (btnSound != null) {
            btnSound.setText(soundEnabled ? "🔊" : "🔇");
            btnSound.setStyle(soundEnabled
                ? "-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white;" +
                  "-fx-font-size:16; -fx-background-radius:50%;" +
                  "-fx-min-width:38; -fx-min-height:38; -fx-cursor:hand; -fx-border-width:0;"
                : "-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:rgba(255,255,255,0.4);" +
                  "-fx-font-size:16; -fx-background-radius:50%;" +
                  "-fx-min-width:38; -fx-min-height:38; -fx-cursor:hand; -fx-border-width:0;"
            );
        }
    }

    private static boolean soundEnabled = true;
    private static final javax.sound.sampled.Clip clipClick  = buildClip(880,  60,  0.25f);
    private static final javax.sound.sampled.Clip clipStart  = buildClip(784,  200, 0.40f);
    private static final javax.sound.sampled.Clip clipFinish = buildClip(1047, 300, 0.55f);

    private static void playClick()  { playClip(clipClick); }
    private static void playStart()  { playClip(clipStart); }
    private static void playFinish() { playClip(clipFinish); }

    private static void playClip(javax.sound.sampled.Clip clip) {
        if (!soundEnabled || clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    private static javax.sound.sampled.Clip buildClip(int freqHz, int durationMs, float volume) {
        try {
            float sampleRate = 44100f;
            int samples = (int)(sampleRate * durationMs / 1000);
            byte[] buf = new byte[samples * 2];
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * freqHz / sampleRate;
                double fadeIn  = sampleRate * 0.005;
                double fadeOut = sampleRate * 0.015;
                double env = Math.min(1.0, Math.min(i / fadeIn, (samples - i) / fadeOut));
                short val = (short)(Math.sin(angle) * env * volume * Short.MAX_VALUE);
                buf[i * 2]     = (byte)(val & 0xFF);
                buf[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
            }
            javax.sound.sampled.AudioFormat fmt =
                new javax.sound.sampled.AudioFormat(sampleRate, 16, 1, true, false);
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(fmt, buf, 0, buf.length);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }
}

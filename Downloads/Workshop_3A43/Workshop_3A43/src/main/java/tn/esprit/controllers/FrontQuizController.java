package tn.esprit.controllers;

/**
 * ============================================================
 * FrontQuizController ÔÇö Contr├┤leur principal du quiz (front-office)
 * ============================================================
 *
 * Ce contr├┤leur g├¿re l'int├®gralit├® du flux d'un quiz c├┤t├® ├®tudiant.
 * Il est partag├® par les quatre vues FXML du quiz :
 *
 *   1. intro.fxml    ÔÇö ├ëcran d'introduction : titre, stats, bouton "Commencer"
 *   2. loading.fxml  ÔÇö ├ëcran de chargement anim├® (transition entre intro et questions)
 *   3. question.fxml ÔÇö ├ëcran de question : affichage des options, timer, progression
 *   4. resultat.fxml ÔÇö ├ëcran de r├®sultats : score, pourcentage, statistiques, actions
 *
 * Flux de navigation :
 *   intro ÔåÆ (clic "Commencer") ÔåÆ loading ÔåÆ (pause 2s) ÔåÆ question(s) ÔåÆ (soumission) ÔåÆ r├®sultat
 *   r├®sultat ÔåÆ (clic "Refaire") ÔåÆ intro
 *   r├®sultat / intro ÔåÆ (clic "Retour") ÔåÆ liste des chapitres
 *
 * Calcul du score :
 *   Pour chaque question, on compare l'option choisie par l'├®tudiant
 *   avec l'option marqu├®e "estCorrecte = true". Si elles correspondent,
 *   on ajoute les points de la question au total obtenu.
 *   Le pourcentage = (pointsObtenus / totalPoints) ├ù 100.
 *   Le seuil de r├®ussite est d├®fini dans l'entit├® Quiz (seuilReussite, d├®faut 50 %).
 */

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
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

public class FrontQuizController {

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // CHAMPS FXML ÔÇö loading.fxml
    // Ces champs sont inject├®s automatiquement par JavaFX lors du chargement
    // de l'├®cran de chargement anim├® (loading.fxml).
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Conteneur racine de l'├®cran de chargement (utilis├® pour l'animation de fondu sortant). */
    @FXML private StackPane rootPane;

    /** Conteneur des 4 carr├®s color├®s qui tournent en animation de chargement. */
    @FXML private StackPane logoContainer;

    /** Carr├® bleu (coin sup├®rieur gauche du logo anim├®). */
    @FXML private Rectangle squareBlue;

    /** Carr├® vert (coin sup├®rieur droit du logo anim├®). */
    @FXML private Rectangle squareGreen;

    /** Carr├® rouge (coin inf├®rieur gauche du logo anim├®). */
    @FXML private Rectangle squareRed;

    /** Carr├® orange (coin inf├®rieur droit du logo anim├®). */
    @FXML private Rectangle squareOrange;

    /** Label "Chargement du Quiz..." qui clignote pendant l'animation. */
    @FXML private Label loadingLabel;

    /** Label affichant le nom du quiz en cours de chargement. */
    @FXML private Label quizNameLabel;

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // CHAMPS FXML ÔÇö intro.fxml
    // Ces champs sont inject├®s lors du chargement de l'├®cran d'introduction.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Titre du quiz affich├® dans la carte centrale de l'intro. */
    @FXML private Label labelTitreQuiz;

    /** Description du quiz (sous-titre en violet). */
    @FXML private Label labelDescQuiz;

    /** Nombre total de questions du quiz (affich├® dans la section stats). */
    @FXML private Label labelNbQuestions;

    /** Nombre total de points du quiz (somme des points de toutes les questions). */
    @FXML private Label labelTotalPoints;

    /** Dur├®e maximale du quiz en minutes (ou "ÔÇö" si illimit├®e). */
    @FXML private Label labelDuree;

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // CHAMPS FXML ÔÇö question.fxml
    // Ces champs sont inject├®s lors du chargement de l'├®cran de question.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Titre du quiz affich├® dans l'en-t├¬te de l'├®cran de question. */
    @FXML private Label  labelTitreHeader;

    /** Affichage du timer (ex : "ÔÅ▒  2:30") dans l'en-t├¬te, fond d├®grad├® orange. */
    @FXML private Label  labelTimer;

    /** Indicateur de progression (ex : "Question 3 / 10"). */
    @FXML private Label  labelProgress;

    /** Texte de la question courante, affich├® dans la carte blanche centrale. */
    @FXML private Label  labelQuestion;

    /** Points attribu├®s ├á la question courante (ex : "Ô¡É 10 points"). */
    @FXML private Label  labelPoints;

    /** Conteneur vertical dans lequel les boutons d'options sont g├®n├®r├®s dynamiquement. */
    @FXML private VBox   optionsContainer;

    /** Compteur de questions r├®pondues (ex : "3 / 10 questions r├®pondues"). */
    @FXML private Label  labelRepondues;

    /** Bouton de soumission du quiz, visible en bas ├á droite de l'├®cran de question. */
    @FXML private Button btnSoumettre;

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // CHAMPS FXML ÔÇö resultat.fxml
    // Ces champs sont inject├®s lors du chargement de l'├®cran de r├®sultats.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Titre du quiz affich├® dans la carte de r├®sultats (en violet). */
    @FXML private Label labelTitreResultat;

    /** Points obtenus par l'├®tudiant (grand chiffre violet). */
    @FXML private Label labelPointsObtenus;

    /** Pourcentage de r├®ussite (grand chiffre orange, ex : "75%"). */
    @FXML private Label labelPourcentage;

    /** Total des points possibles pour ce quiz (grand chiffre noir). */
    @FXML private Label labelPointsTotal;

    /**
     * Message de r├®sultat contextuel :
     *   - Vert  : "F├®licitations !" si pct >= seuil
     *   - Orange: "Peut mieux faire" si pct >= seuil/2
     *   - Rouge : "Score insuffisant" sinon
     */
    @FXML private Label labelMessage;

    /** Num├®ro de la tentative actuelle (toujours "1" dans cette version). */
    @FXML private Label labelTentative;

    /** Affiche le nombre maximum de tentatives autoris├®es (ex : "TENTATIVE / 3" ou "TENTATIVE / Ôê×"). */
    @FXML private Label labelMaxTentatives;

    /** Meilleur score enregistr├® pour ce quiz (dans cette version = score actuel). */
    @FXML private Label labelMeilleurScore;

    /** Indique si l'├®tudiant peut recommencer le quiz ("OUI" en bleu ou "NON" en rouge). */
    @FXML private Label labelPeutRecommencer;

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ├ëTAT INTERNE DU CONTR├öLEUR
    // Ces champs maintiennent l'├®tat du quiz tout au long de la session.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Le quiz en cours de passage (entit├® Quiz charg├®e depuis la base de donn├®es). */
    private Quiz quiz;

    /** Le chapitre auquel appartient ce quiz (utilis├® pour le retour). */
    private Chapitre chapitre;

    /** Liste ordonn├®e de toutes les questions du quiz. */
    private List<Question> questions;

    /** Liste des options de la question actuellement affich├®e (mise ├á jour ├á chaque question). */
    private List<Option> optionsQuestionCourante;

    /** Index (0-based) de la question actuellement affich├®e. */
    private int indexQuestion = 0;

    /**
     * Map des r├®ponses choisies par l'├®tudiant.
     * Cl├®   : ID de la question
     * Valeur: ID de l'option s├®lectionn├®e
     * Permet de conserver les r├®ponses lors de la navigation entre questions.
     */
    private final Map<Integer, Integer> reponsesChoisies = new HashMap<>();

    /** Somme des points de toutes les questions (calcul├®e une seule fois ├á l'initialisation). */
    private int totalPoints = 0;

    /** Timeline JavaFX qui d├®cr├®mente le timer chaque seconde. Stopp├®e ├á la soumission. */
    private Timeline timerTimeline;

    /** Nombre de secondes restantes pour le quiz (initialis├® depuis dureeMaxMinutes ├ù 60). */
    private int secondesRestantes;

    /**
     * Callback ex├®cut├® lors du retour aux chapitres.
     * Fourni par le contr├┤leur parent (FrontChapitreDetailController) via setChapitre().
     */
    private Runnable onRetourCallback;

    /**
     * Cache des options par question pour ├®viter des requ├¬tes r├®p├®t├®es ├á la base de donn├®es.
     * Cl├®   : ID de la question
     * Valeur: liste des options de cette question
     */
    private final Map<Integer, List<Option>> optionsParQuestion = new HashMap<>();

    /**
     * R├®f├®rence ├á un n┼ôud JavaFX de la sc├¿ne courante.
     * Utilis├®e par setCenter() pour remonter jusqu'au BorderPane racine
     * et y injecter la nouvelle vue.
     */
    private javafx.scene.Node sceneRef;

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // SERVICES (acc├¿s ├á la base de donn├®es)
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /** Service pour r├®cup├®rer les quiz depuis la base de donn├®es. */
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();

    /** Service pour r├®cup├®rer les questions d'un quiz. */
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    /** Service pour r├®cup├®rer les options d'une question. */
    private final ServiceOption serviceOption = new ServiceOption();

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // API PUBLIQUE ÔÇö m├®thodes appel├®es depuis l'ext├®rieur du contr├┤leur
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Fournit une r├®f├®rence ├á un n┼ôud de la sc├¿ne courante.
     * N├®cessaire pour que setCenter() puisse remonter jusqu'au BorderPane racine.
     *
     * @param ref n'importe quel n┼ôud d├®j├á attach├® ├á la sc├¿ne principale
     */
    public void setSceneRef(javafx.scene.Node ref) { this.sceneRef = ref; }

    /**
     * Point d'entr├®e principal : initialise le contr├┤leur ├á partir d'un chapitre.
     * Recherche le quiz associ├® au chapitre via ServiceQuiz.findByChapitreId().
     * Si un quiz est trouv├®, appelle setQuiz() pour lancer le flux.
     * Sinon, affiche un message d'erreur dans le label de titre.
     *
     * @param chapitre   le chapitre dont on veut passer le quiz
     * @param onRetour   callback ├á ex├®cuter quand l'├®tudiant clique "Retour aux chapitres"
     */
    public void setChapitre(Chapitre chapitre, Runnable onRetour) {
        this.chapitre = chapitre;
        this.onRetourCallback = onRetour;
        List<Quiz> quizDuChapitre = serviceQuiz.findByChapitreId(chapitre.getId());
        Quiz trouve = quizDuChapitre.isEmpty() ? null : quizDuChapitre.get(0);
        if (trouve != null) setQuiz(trouve);
        else if (labelTitreQuiz != null) labelTitreQuiz.setText("Aucun quiz disponible pour ce chapitre");
    }

    /**
     * Initialise le quiz : charge les questions, calcule le total de points,
     * puis affiche l'├®cran d'introduction (intro.fxml).
     *
     * @param quiz le quiz ├á passer
     */
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        // Chargement de toutes les questions du quiz depuis la base de donn├®es
        this.questions = serviceQuestion.findByQuizId(quiz.getId());
        // Calcul du total des points (somme des points de chaque question)
        this.totalPoints = questions.stream().mapToInt(Question::getPoint).sum();
        javafx.application.Platform.runLater(() -> { if (labelTitreQuiz != null) sceneRef = labelTitreQuiz; });
        afficherIntro();
    }

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ├ëCRAN 1 : INTRODUCTION (intro.fxml)
    // Affiche le titre, la description et les statistiques du quiz.
    // L'├®tudiant peut lancer le quiz ou retourner aux chapitres.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Remplit les labels de l'├®cran d'introduction avec les donn├®es du quiz.
     * Appel├®e apr├¿s setQuiz() ou apr├¿s un "Refaire".
     * Ne fait rien si labelTitreQuiz est null (vue non charg├®e).
     */
    private void afficherIntro() {
        if (labelTitreQuiz == null) return;
        labelTitreQuiz.setText("Quiz - " + quiz.getTitre());
        labelDescQuiz.setText(quiz.getDescription() != null ? quiz.getDescription() : "");
        labelNbQuestions.setText(String.valueOf(questions.size()));
        labelTotalPoints.setText(String.valueOf(totalPoints));
        // Affiche la dur├®e en minutes, ou "ÔÇö" si le quiz est sans limite de temps
        labelDuree.setText(quiz.getDureeMaxMinutes() != null ? String.valueOf(quiz.getDureeMaxMinutes()) : "ÔÇö");
    }

    /**
     * Action du bouton "ÔûÂ Commencer le quiz" (intro.fxml).
     * R├®initialise l'index de question et les r├®ponses, puis charge l'├®cran
     * de chargement anim├® (loading.fxml). En cas d'erreur de chargement FXML,
     * navigue directement vers les questions.
     */
    @FXML
    private void onCommencer() {
        indexQuestion = 0;
        reponsesChoisies.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/loading.fxml"));
            Parent view = loader.load();
            FrontQuizController loadingCtrl = loader.getController();
            if (sceneRef == null) sceneRef = labelTitreQuiz;
            setCenter(view);
            // Lance l'animation de chargement ; ├á la fin, navigue vers les questions
            loadingCtrl.startLoading("Quiz - " + quiz.getTitre(), this::naviguerVersQuestion);
        } catch (Exception e) {
            e.printStackTrace();
            naviguerVersQuestion();
        }
    }

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ├ëCRAN DE CHARGEMENT (loading.fxml)
    // Affiche une animation de 2 secondes avant de passer aux questions.
    // Animations :
    //   - Rotation continue du conteneur de 4 carr├®s (logoContainer)
    //   - Pulsation (scale) de chaque carr├® avec un d├®lai d├®cal├® (effet cascade)
    //   - Clignotement (fade) du label "Chargement du Quiz..."
    //   - Fondu sortant (fadeOut) du panneau entier avant de lancer onFinished
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Lance l'animation de chargement et programme la transition vers l'├®cran suivant.
     * Appel├®e par le contr├┤leur de l'intro apr├¿s avoir charg├® loading.fxml.
     *
     * @param titre      nom du quiz ├á afficher sous le logo anim├®
     * @param onFinished callback ex├®cut├® apr├¿s le fondu sortant (navigue vers les questions)
     */
    public void startLoading(String titre, Runnable onFinished) {
        // Affiche le nom du quiz sous le logo
        if (quizNameLabel != null) quizNameLabel.setText(titre);

        if (logoContainer != null) {
            // Animation 1 : rotation continue du logo (360┬░ en 1,5 s, en boucle)
            RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), logoContainer);
            rotate.setByAngle(360);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setInterpolator(Interpolator.EASE_BOTH);
            rotate.play();

            // Animation 2 : pulsation de chaque carr├® avec d├®lai d├®cal├® (effet cascade)
            // D├®lais : bleu=0s, vert=0.2s, rouge=0.4s, orange=0.6s
            animerCarre(squareBlue, 0.0); animerCarre(squareGreen, 0.2);
            animerCarre(squareRed,  0.4); animerCarre(squareOrange, 0.6);
        }

        if (loadingLabel != null) {
            // Animation 3 : clignotement du texte "Chargement du Quiz..." (opacit├® 1.0 ÔåÆ 0.4)
            FadeTransition fade = new FadeTransition(Duration.seconds(1.2), loadingLabel);
            fade.setFromValue(1.0); fade.setToValue(0.4);
            fade.setCycleCount(Animation.INDEFINITE); fade.setAutoReverse(true);
            fade.play();
        }

        // Pause de 2 secondes, puis fondu sortant de 400 ms avant de lancer le callback
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            if (rootPane != null) {
                // Animation 4 : fondu sortant du panneau entier (opacit├® 1.0 ÔåÆ 0.0 en 400 ms)
                FadeTransition fadeOut = new FadeTransition(Duration.millis(400), rootPane);
                fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(ev -> onFinished.run());
                fadeOut.play();
            } else { onFinished.run(); }
        });
        pause.play();
    }

    /**
     * Applique une animation de pulsation (scale 1.0 ÔåÆ 1.15) ├á un carr├® du logo.
     * L'animation est en boucle infinie avec auto-reverse (effet de respiration).
     *
     * @param square le Rectangle JavaFX ├á animer
     * @param delay  d├®lai en secondes avant le d├®but de l'animation (effet cascade)
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

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ├ëCRAN 2 : QUESTIONS (question.fxml)
    // Affiche les questions une par une avec leurs options.
    // L'├®tudiant clique sur une option ÔåÆ r├®ponse enregistr├®e ÔåÆ question suivante.
    // Un timer d├®compte le temps restant si le quiz a une dur├®e maximale.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Charge question.fxml dans un nouveau contr├┤leur et y transf├¿re tout l'├®tat
     * (quiz, questions, r├®ponses, index, callbacks). Puis affiche la premi├¿re question.
     * Appel├®e depuis startLoading() via le callback onFinished.
     */
    private void naviguerVersQuestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/question.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            // Transfert de l'├®tat complet vers le nouveau contr├┤leur de question
            ctrl.quiz = this.quiz;
            ctrl.chapitre = this.chapitre;
            ctrl.questions = this.questions;
            ctrl.totalPoints = this.totalPoints;
            ctrl.indexQuestion = this.indexQuestion;
            ctrl.reponsesChoisies.putAll(this.reponsesChoisies);
            ctrl.optionsParQuestion.putAll(this.optionsParQuestion);
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelTitreQuiz;
            ctrl.afficherQuestion();
            setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Affiche la question ├á l'index courant (indexQuestion).
     * Met ├á jour : en-t├¬te, progression, texte de la question, points,
     * compteur de r├®ponses, timer et liste des options.
     */
    private void afficherQuestion() {
        if (questions == null || questions.isEmpty() || labelQuestion == null) return;
        Question q = questions.get(indexQuestion);
        labelTitreHeader.setText("Quiz - " + quiz.getTitre());
        // Ex : "Question 3 / 10"
        labelProgress.setText("Question " + (indexQuestion + 1) + " / " + questions.size());
        labelQuestion.setText(q.getTexteQuestion());
        labelPoints.setText("Ô¡É " + q.getPoint() + " points");
        mettreAJourRepondues();
        demarrerTimer();
        afficherOptions(q);
    }

    /**
     * G├®n├¿re dynamiquement les boutons d'options pour la question donn├®e.
     * L'option d├®j├á s├®lectionn├®e est mise en surbrillance (fond violet d├®grad├®).
     * Au clic sur une option :
     *   1. La r├®ponse est enregistr├®e dans reponsesChoisies
     *   2. Les boutons sont redessin├®s (mise ├á jour visuelle)
     *   3. Apr├¿s 400 ms, on passe automatiquement ├á la question suivante
     *      (si ce n'est pas la derni├¿re question)
     *
     * @param q la question dont on affiche les options
     */
    private void afficherOptions(Question q) {
        optionsContainer.getChildren().clear();
        Integer dejaChoisi = reponsesChoisies.get(q.getId());
        // Utilise le cache pour ├®viter des requ├¬tes r├®p├®t├®es ├á la base de donn├®es
        List<Option> opts = optionsParQuestion.computeIfAbsent(q.getId(), id -> serviceOption.findByQuestionId(id));
        optionsQuestionCourante = opts;

        for (Option opt : opts) {
            Button btn = new Button(opt.getTexteOption());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setWrapText(true);
            btn.setPadding(new Insets(14, 20, 14, 20));
            // Style s├®lectionn├® (violet) vs non s├®lectionn├® (semi-transparent blanc)
            boolean sel = dejaChoisi != null && dejaChoisi == opt.getId();
            btn.setStyle(sel
                ? "-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:700;-fx-background-radius:12;-fx-cursor:hand;-fx-border-width:0;-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),10,0,0,3);"
                : "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:600;-fx-background-radius:12;-fx-cursor:hand;-fx-border-width:2;-fx-border-color:rgba(255,255,255,0.3);-fx-border-radius:12;"
            );
            btn.setOnAction(e -> {
                // Enregistre la r├®ponse choisie pour cette question
                reponsesChoisies.put(q.getId(), opt.getId());
                // Redessine les options pour refl├®ter la s├®lection
                afficherOptions(q);
                mettreAJourRepondues();
                // Pause de 400 ms pour laisser l'├®tudiant voir sa s├®lection, puis question suivante
                PauseTransition p = new PauseTransition(Duration.millis(400));
                p.setOnFinished(ev -> { if (indexQuestion < questions.size() - 1) { indexQuestion++; afficherQuestion(); } });
                p.play();
            });
            optionsContainer.getChildren().add(btn);
        }
    }

    /**
     * Met ├á jour le label de progression des r├®ponses.
     * Ex : "3 / 10 questions r├®pondues"
     */
    private void mettreAJourRepondues() {
        if (labelRepondues != null)
            labelRepondues.setText(reponsesChoisies.size() + " / " + questions.size() + " questions r├®pondues");
    }

    /**
     * D├®marre (ou red├®marre) le timer pour la question courante.
     * Si le quiz n'a pas de dur├®e maximale, affiche "ÔÅ▒  Ôê×" et ne d├®marre pas de timer.
     * Sinon, d├®cr├®mente secondesRestantes chaque seconde.
     * Quand le temps atteint 0, soumet automatiquement le quiz (onSoumettre()).
     * Le timer est partag├® pour tout le quiz (pas r├®initialis├® ├á chaque question).
     */
    private void demarrerTimer() {
        if (timerTimeline != null) timerTimeline.stop();
        if (quiz.getDureeMaxMinutes() == null) { if (labelTimer != null) labelTimer.setText("ÔÅ▒  Ôê×"); return; }
        secondesRestantes = quiz.getDureeMaxMinutes() * 60;
        mettreAJourAffichageTimer();
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondesRestantes--;
            mettreAJourAffichageTimer();
            // Soumission automatique quand le temps est ├®coul├®
            if (secondesRestantes <= 0) { timerTimeline.stop(); onSoumettre(); }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    /**
     * Formate et affiche le temps restant dans le label du timer.
     * Format : "ÔÅ▒  M:SS" (ex : "ÔÅ▒  2:05")
     */
    private void mettreAJourAffichageTimer() {
        if (labelTimer != null)
            labelTimer.setText(String.format("ÔÅ▒  %d:%02d", secondesRestantes / 60, secondesRestantes % 60));
    }

    /**
     * Action du bouton "Ô£ö Soumettre le Quiz" (question.fxml).
     * Arr├¬te le timer et navigue vers l'├®cran de r├®sultats.
     */
    @FXML
    private void onSoumettre() {
        if (timerTimeline != null) timerTimeline.stop();
        naviguerVersResultat();
    }

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ├ëCRAN 3 : R├ëSULTATS (resultat.fxml)
    // Calcule et affiche le score final, le pourcentage de r├®ussite,
    // un message contextuel et les statistiques de la session.
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Charge resultat.fxml dans un nouveau contr├┤leur et y transf├¿re l'├®tat
     * (quiz, questions, r├®ponses, options en cache, callbacks).
     * Puis appelle afficherResultat() pour calculer et afficher le score.
     */
    private void naviguerVersResultat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/resultat.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            // Transfert de l'├®tat complet vers le contr├┤leur de r├®sultats
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
     * Calcule le score et remplit tous les labels de l'├®cran de r├®sultats.
     *
     * Algorithme de calcul du score :
     *   Pour chaque question du quiz :
     *     1. R├®cup├¿re l'ID de l'option choisie par l'├®tudiant (reponsesChoisies)
     *     2. R├®cup├¿re la liste des options (depuis le cache optionsParQuestion ou la BDD)
     *     3. Trouve l'option marqu├®e "estCorrecte = true"
     *     4. Si l'option correcte correspond ├á l'option choisie ÔåÆ ajoute les points de la question
     *   Pourcentage = (pointsObtenus / totalPoints) ├ù 100, arrondi ├á 2 d├®cimales
     *
     * Messages contextuels selon le pourcentage vs seuil de r├®ussite :
     *   - pct >= seuil          ÔåÆ "F├®licitations !" (vert)
     *   - pct >= seuil / 2      ÔåÆ "Peut mieux faire" (orange)
     *   - pct < seuil / 2       ÔåÆ "Score insuffisant" (rouge)
     */
    private void afficherResultat() {
        if (labelTitreResultat == null) return;
        int pointsObtenus = 0;
        for (Question q : questions) {
            Integer choisi = reponsesChoisies.get(q.getId());
            if (choisi == null) continue; // Question non r├®pondue ÔåÆ 0 point
            List<Option> opts = optionsParQuestion.getOrDefault(q.getId(), serviceOption.findByQuestionId(q.getId()));
            for (Option o : opts) {
                if (o.isEstCorrecte()) {
                    // Comparaison par valeur (int) pour ├®viter les pi├¿ges de l'auto-unboxing Integer
                    if (o.getId() == choisi.intValue()) pointsObtenus += q.getPoint();
                    break; // Une seule option correcte par question
                }
            }
        }
        // Calcul du pourcentage, arrondi ├á 2 d├®cimales
        double pct = totalPoints > 0 ? Math.round((pointsObtenus * 100.0 / totalPoints) * 100.0) / 100.0 : 0.0;
        // Seuil de r├®ussite d├®fini dans le quiz, 50 % par d├®faut
        int seuil = quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50;

        // Remplissage des labels principaux
        labelTitreResultat.setText("Quiz - " + quiz.getTitre());
        labelPointsObtenus.setText(String.valueOf(pointsObtenus));
        labelPourcentage.setText(String.format("%.0f%%", pct));
        labelPointsTotal.setText(String.valueOf(totalPoints));

        // Message et couleur selon le r├®sultat
        if (pct >= seuil) {
            labelMessage.setText("­ƒÄë  F├®licitations ! Vous avez r├®ussi le quiz !");
            labelMessage.setStyle("-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#059669;-fx-background-color:#f0fdf4;-fx-background-radius:10;-fx-padding:10 20 10 20;");
        } else if (pct >= (double) seuil / 2) {
            labelMessage.setText("­ƒôê  Peut mieux faire ÔÇö continuez vos efforts !");
            labelMessage.setStyle("-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#d97706;-fx-background-color:#fffbeb;-fx-background-radius:10;-fx-padding:10 20 10 20;");
        } else {
            labelMessage.setText("­ƒÿö  Score insuffisant ÔÇö r├®visez et r├®essayez !");
            labelMessage.setStyle("-fx-font-size:14;-fx-font-weight:700;-fx-text-fill:#dc2626;-fx-background-color:#fef2f2;-fx-background-radius:10;-fx-padding:10 20 10 20;");
        }

        // Statistiques de la session (tentative, meilleur score, possibilit├® de recommencer)
        if (labelTentative != null) labelTentative.setText("1"); // Toujours 1 dans cette version
        if (labelMaxTentatives != null) labelMaxTentatives.setText("TENTATIVE / " + (quiz.getMaxTentatives() != null ? quiz.getMaxTentatives() : "Ôê×"));
        if (labelMeilleurScore != null) labelMeilleurScore.setText(String.format("%.0f%%", pct));
        if (labelPeutRecommencer != null) {
            // Peut recommencer si maxTentatives est null (illimit├®) ou > 1
            boolean peut = quiz.getMaxTentatives() == null || quiz.getMaxTentatives() > 1;
            labelPeutRecommencer.setText(peut ? "OUI" : "NON");
            if (!peut) labelPeutRecommencer.setStyle("-fx-font-size:22;-fx-font-weight:900;-fx-text-fill:#ef4444;");
        }
    }

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // ACTIONS ÔÇö boutons de navigation (r├®sultat.fxml et intro.fxml)
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Action du bouton "­ƒÅá Accueil" (resultat.fxml).
     * Arr├¬te le timer, puis tente de d├®clencher le bouton "btnHome" de la barre
     * de navigation principale via lookup sur la sc├¿ne. Si introuvable, ex├®cute
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
     * Action du bouton "Ôå║ Refaire le quiz" (resultat.fxml).
     * R├®initialise l'index et les r├®ponses, recharge intro.fxml avec le m├¬me quiz.
     * Transf├¿re le callback de retour et la r├®f├®rence de sc├¿ne au nouveau contr├┤leur.
     */
    @FXML
    private void onRefaire() {
        indexQuestion = 0;
        reponsesChoisies.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/intro.fxml"));
            Parent view = loader.load();
            FrontQuizController ctrl = loader.getController();
            ctrl.onRetourCallback = this.onRetourCallback;
            ctrl.sceneRef = this.sceneRef != null ? this.sceneRef : this.labelTitreResultat;
            ctrl.setQuiz(this.quiz);
            setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Action du bouton "ÔåÉ Retour aux chapitres" (intro.fxml et resultat.fxml).
     * Arr├¬te le timer et ex├®cute le callback de retour fourni par le contr├┤leur parent.
     */
    @FXML
    private void onRetour() {
        if (timerTimeline != null) timerTimeline.stop();
        if (onRetourCallback != null) onRetourCallback.run();
    }

    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ
    // UTILITAIRE ÔÇö navigation entre vues
    // ÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉ

    /**
     * Injecte une nouvelle vue dans le centre du BorderPane racine de la sc├¿ne.
     * Remonte la hi├®rarchie de n┼ôuds depuis sceneRef (ou les labels disponibles)
     * jusqu'├á la racine de la sc├¿ne, puis appelle setCenter() sur le BorderPane.
     *
     * Ordre de priorit├® pour trouver la r├®f├®rence de sc├¿ne :
     *   1. sceneRef (fourni explicitement)
     *   2. labelTitreQuiz (intro.fxml)
     *   3. labelQuestion (question.fxml)
     *   4. labelTitreResultat (resultat.fxml)
     *
     * @param view la nouvelle vue Parent ├á afficher au centre du layout
     */
    private void setCenter(Parent view) {
        javafx.scene.Node ref = sceneRef != null ? sceneRef
            : labelTitreQuiz != null ? labelTitreQuiz
            : labelQuestion != null ? labelQuestion
            : labelTitreResultat;
        if (ref == null || ref.getScene() == null) return;
        ((BorderPane) ref.getScene().getRoot()).setCenter(view);
    }
}
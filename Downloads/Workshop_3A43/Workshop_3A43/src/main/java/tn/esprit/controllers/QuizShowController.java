package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceQuiz;

/**
 * Controller de la page de d├®tails d'un quiz (show.fxml).
 * Affiche les informations d'un quiz s├®lectionn├®.
 * Permet aussi de modifier ou supprimer le quiz depuis cette page.
 */
public class QuizShowController {

    // ÔöÇÔöÇ Composants FXML (li├®s aux ├®l├®ments de show.fxml) ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML private Label labelTitre;
    @FXML private Label labelSubtitle;
    @FXML private Label labelTitreVal;
    @FXML private Label labelDescVal;
    @FXML private Label labelEtatBadge;
    @FXML private Label labelChapitreTitre;
    @FXML private Label labelChapitreDetail;
    @FXML private VBox  boxChapitre;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // Le quiz actuellement affich├®
    private Quiz quiz;

    // Callback appel├® quand on revient en arri├¿re (pour rafra├«chir la liste)
    private Runnable onBack;

    // ÔöÇÔöÇ Initialisation : appel├®e depuis QuizController avec le quiz ├á afficher ÔöÇ
    public void init(Quiz quiz, Runnable onBack) {
        this.quiz = quiz;
        this.onBack = onBack;

        labelTitre.setText(quiz.getTitre());
        labelSubtitle.setText("Quiz #" + quiz.getId());
        labelTitreVal.setText(quiz.getTitre());
        labelDescVal.setText(quiz.getDescription());

        // Chapitre li├®
        if (quiz.getChapitreId() != null) {
            Chapitre ch = serviceChapitre.consulterParId(quiz.getChapitreId());
            if (ch != null) {
                labelChapitreTitre.setText(ch.getTitre());
                labelChapitreDetail.setText("Chapitre #" + ch.getId() + " ÔÇô Ordre : " + ch.getOrdre());
                boxChapitre.setVisible(true);
                boxChapitre.setManaged(true);
            }
        } else {
            boxChapitre.setVisible(false);
            boxChapitre.setManaged(false);
        }

        // Afficher le badge d'├®tat avec la couleur correspondante
        labelEtatBadge.setText("ÔùÅ " + capitalize(quiz.getEtat()));
        String etat = quiz.getEtat().toLowerCase();
        String badgeStyle = switch (etat) {
            case "actif"     -> "-fx-background-color:rgba(16,185,129,0.15); -fx-text-fill:#22c55e;";
            case "inactif"   -> "-fx-background-color:rgba(245,158,11,0.15); -fx-text-fill:#eab308;";
            case "brouillon" -> "-fx-background-color:rgba(59,130,246,0.15); -fx-text-fill:#0ea5e9;";
            default          -> "-fx-background-color:rgba(71,85,105,0.3); -fx-text-fill:rgba(245,245,244,0.45);";
        };
        labelEtatBadge.setStyle(badgeStyle +
            "-fx-background-radius:20px; -fx-padding:3 10; -fx-font-size:12px; -fx-font-weight:bold;");
    }

    // ÔöÇÔöÇ Retour : revenir ├á la liste des quiz ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void retour() {
        navigateToQuizList();
    }

    // ÔöÇÔöÇ Modifier : ouvrir le formulaire de modification pour ce quiz ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void modifier() {
        try {
            StackPane contentArea = (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea == null) return;
            // Charger le formulaire de modification
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/quiz_form.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
            // Passer le quiz au formulaire pour pr├®-remplir les champs
            QuizFormController ctrl = loader.getController();
            ctrl.initEdit(quiz);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ÔöÇÔöÇ Supprimer : demander confirmation puis supprimer le quiz ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void supprimer() {
        // Afficher une bo├«te de dialogue de confirmation avant de supprimer
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le quiz ┬½ " + quiz.getTitre() + " ┬╗ ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuiz.supprimer(quiz); // supprimer en BDD
                navigateToQuizList();        // retourner ├á la liste
            }
        });
    }

    // ÔöÇÔöÇ Navigation vers la liste des quiz ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    private void navigateToQuizList() {
        try {
            StackPane contentArea = (StackPane) labelTitre.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Met la premi├¿re lettre en majuscule (ex: "actif" ÔåÆ "Actif")
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
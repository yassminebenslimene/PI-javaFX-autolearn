package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Question;
import tn.esprit.services.ServiceQuestion;

/**
 * Controller du formulaire Question (question_form.fxml).
 * Gère la création d'une nouvelle question et la modification d'une question existante.
 * Si questionAModifier == null → mode création, sinon → mode modification.
 */
public class QuestionController {

    // ── Composants FXML (liés aux éléments de question_form.fxml) ────────────
    @FXML private Label pageTitle;     // titre en haut de la page
    @FXML private Label cardTitle;     // titre de la carte
    @FXML private Label cardSubtitle;  // sous-titre de la carte
    @FXML private TextArea texteField; // zone de texte pour la question
    @FXML private TextField pointField; // champ pour le nombre de points
    @FXML private Label messageLabel;  // affiche les messages d'erreur

    // Style normal d'un champ de saisie
    private static final String FIELD_NORMAL =
        "-fx-background-color:rgba(255,255,255,0.05);" +
        "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    // Style d'un champ en erreur (bordure rouge)
    private static final String FIELD_ERROR =
        "-fx-background-color:rgba(239,68,68,0.08);" +
        "-fx-border-color:rgba(239,68,68,0.6); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1.5px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    // Service pour les opérations BDD sur les questions
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    // La question à modifier (null si on est en mode création)
    private Question questionAModifier = null;

    // L'id du quiz auquel appartient cette question
    private int quizId;

    // ── Initialisation : appelée automatiquement au chargement du FXML ───────
    @FXML
    public void initialize() {
        // Effacer les erreurs dès que l'utilisateur commence à taper
        texteField.textProperty().addListener((o, ov, nv) -> resetField(texteField));
        pointField.textProperty().addListener((o, ov, nv) -> resetField(pointField));
    }

    // ── Mode création : définir le quiz parent ────────────────────────────────
    // Appelé depuis QuizController quand on clique "+ Nouvelle Question"
    public void initNouvelle(int quizId) {
        this.quizId = quizId; // on mémorise l'id du quiz pour l'insertion en BDD
    }

    // ── Mode modification : pré-remplir le formulaire ─────────────────────────
    // Appelé depuis QuizController quand on clique "Modifier" sur une question
    public void initModifier(Question question) {
        this.questionAModifier = question;
        this.quizId = question.getQuizId();
        // Changer les textes pour indiquer le mode modification
        pageTitle.setText("Modifier la Question");
        cardTitle.setText("Modifier la Question");
        cardSubtitle.setText("Mettez à jour la question");
        // Pré-remplir les champs
        texteField.setText(question.getTexteQuestion());
        pointField.setText(String.valueOf(question.getPoint()));
    }

    // ── Sauvegarder : appelé quand on clique sur le bouton Enregistrer ────────
    @FXML
    public void sauvegarder() {
        resetAll(); // effacer les erreurs précédentes
        boolean valid = true;

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();
        String pointStr = pointField.getText() == null ? "" : pointField.getText().trim();

        // ── Validation du texte de la question ──
        if (texte.isEmpty()) {
            markError(texteField, "⚠ Le texte de la question est obligatoire.");
            valid = false;
        } else if (texte.length() < 10) {
            markError(texteField,
                "⚠ Question trop courte — minimum 10 caractères (actuellement " + texte.length() + ").\n" +
                "   Formulez une question complète et compréhensible.");
            valid = false;
        } else if (texte.length() > 1000) {
            markError(texteField,
                "⚠ Question trop longue — maximum 1000 caractères (actuellement " + texte.length() + ").\n" +
                "   Simplifiez l'énoncé de la question.");
            valid = false;
        } else if (!texte.contains(" ")) {
            // Une question doit contenir au moins 2 mots
            markError(texteField,
                "⚠ La question doit contenir au moins deux mots.\n" +
                "   Exemple : « Qu'est-ce que la programmation orientée objet ? »");
            valid = false;
        }

        // ── Validation des points ──
        if (valid) {
            if (pointStr.isEmpty()) {
                markError(pointField, "⚠ Le nombre de points est obligatoire.");
                valid = false;
            } else {
                try {
                    int point = Integer.parseInt(pointStr);
                    if (point < 1) {
                        markError(pointField, "⚠ Les points doivent être au minimum 1 (valeur saisie : " + point + ").");
                        valid = false;
                    } else if (point > 100) {
                        markError(pointField, "⚠ Les points ne peuvent pas dépasser 100 (valeur saisie : " + point + ").");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    // L'utilisateur a saisi du texte au lieu d'un nombre
                    markError(pointField,
                        "⚠ « " + pointStr + " » n'est pas un nombre valide.\n" +
                        "   Entrez un entier entre 1 et 100 (ex: 5, 10, 20).");
                    valid = false;
                }
            }
        }

        // Si une validation a échoué, on arrête ici
        if (!valid) return;

        // ── Sauvegarde en BDD ──
        int point = Integer.parseInt(pointStr);
        boolean ok;
        if (questionAModifier == null) {
            // Mode création : insérer une nouvelle question liée au quiz
            ok = serviceQuestion.ajouter(new Question(texte, point, null, quizId));
            showAlert(ok, "Question ajoutée avec succès !", "Échec de l'ajout de la question.");
        } else {
            // Mode modification : mettre à jour la question existante
            questionAModifier.setTexteQuestion(texte);
            questionAModifier.setPoint(point);
            ok = serviceQuestion.modifier(questionAModifier);
            showAlert(ok, "Question modifiée avec succès !", "Échec de la modification de la question.");
        }
        // Si succès → retourner à la liste des quiz
        if (ok) retour();
    }

    // ── Retour : revenir à la liste sans sauvegarder ─────────────────────────
    @FXML
    public void retour() {
        try {
            StackPane contentArea = (StackPane) texteField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Affiche une alerte de succès ou d'échec
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "✅ Succès" : "❌ Échec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    // Marque un champ en erreur et affiche le message
    private void markError(Control field, String msg) {
        field.setStyle(FIELD_ERROR);
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1; -fx-wrap-text:true;");
    }

    // Remet un champ à son style normal
    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    // Remet tous les champs à leur style normal
    private void resetAll() {
        texteField.setStyle(FIELD_NORMAL);
        pointField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }
}

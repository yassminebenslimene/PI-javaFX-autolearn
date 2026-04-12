package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Option;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;

/**
 * Controller du formulaire Option (option_form.fxml).
 * Gère la création d'une nouvelle option et la modification d'une option existante.
 * Si optionAModifier == null → mode création, sinon → mode modification.
 */
public class OptionController {

    // ── Composants FXML (liés aux éléments de option_form.fxml) ──────────────
    @FXML private Label pageTitle;          // titre en haut de la page
    @FXML private Label cardTitle;          // titre de la carte
    @FXML private TextField texteField;     // champ texte pour l'option de réponse
    @FXML private CheckBox estCorrecteCheck; // case à cocher : bonne réponse ou non
    @FXML private Label messageLabel;       // affiche les messages d'erreur

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

    // Service pour les opérations BDD sur les options
    private final ServiceOption serviceOption = new ServiceOption();

    // Service question (utilisé pour vérifier les doublons)
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();

    // L'option à modifier (null si on est en mode création)
    private Option optionAModifier = null;

    // L'id de la question à laquelle appartient cette option
    private int questionId;

    // ── Initialisation : appelée automatiquement au chargement du FXML ───────
    @FXML
    public void initialize() {
        // Effacer les erreurs dès que l'utilisateur commence à taper
        texteField.textProperty().addListener((o, ov, nv) -> {
            texteField.setStyle(FIELD_NORMAL);
            messageLabel.setText("");
            messageLabel.setStyle("");
        });
    }

    // ── Mode création : définir la question parente ───────────────────────────
    // Appelé depuis QuizController quand on clique "+ Nouvelle Option"
    public void initNouvelle(int questionId) {
        this.questionId = questionId; // on mémorise l'id de la question pour l'insertion
    }

    // ── Mode modification : pré-remplir le formulaire ─────────────────────────
    // Appelé depuis QuizController quand on clique "Modifier" sur une option
    public void initModifier(Option option) {
        this.optionAModifier = option;
        this.questionId = option.getQuestionId();
        // Changer les textes pour indiquer le mode modification
        pageTitle.setText("Modifier l'Option");
        cardTitle.setText("Modifier l'Option");
        // Pré-remplir les champs avec les valeurs actuelles
        texteField.setText(option.getTexteOption());
        estCorrecteCheck.setSelected(option.isEstCorrecte()); // cocher si bonne réponse
    }

    // ── Sauvegarder : appelé quand on clique sur le bouton Enregistrer ────────
    @FXML
    public void sauvegarder() {
        // Réinitialiser le style et le message d'erreur
        texteField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();

        // ── Validation du texte de l'option ──
        if (texte.isEmpty()) {
            markError("⚠ Le texte de l'option est obligatoire.\n" +
                      "   Entrez le libellé de la réponse proposée.");
            return;
        }
        if (texte.length() < 2) {
            markError("⚠ L'option est trop courte — minimum 2 caractères (actuellement " + texte.length() + ").");
            return;
        }
        if (texte.length() > 255) {
            markError("⚠ L'option est trop longue — maximum 255 caractères (actuellement " + texte.length() + ").\n" +
                      "   Résumez la réponse en moins de 255 caractères.");
            return;
        }

        // ── Vérification des doublons (uniquement en mode création) ──
        // On vérifie qu'il n'existe pas déjà une option identique pour cette question
        if (optionAModifier == null) {
            boolean doublon = serviceOption.findByQuestionId(questionId).stream()
                .anyMatch(o -> o.getTexteOption().equalsIgnoreCase(texte));
            if (doublon) {
                markError("⚠ Cette option existe déjà pour cette question.\n" +
                          "   Chaque option doit être unique.");
                return;
            }
        }

        // ── Sauvegarde en BDD ──
        boolean ok;
        if (optionAModifier == null) {
            // Mode création : créer une nouvelle option liée à la question
            ok = serviceOption.ajouter(new Option(texte, estCorrecteCheck.isSelected(), questionId));
            showAlert(ok, "Option ajoutée avec succès !", "Échec de l'ajout de l'option.");
        } else {
            // Mode modification : mettre à jour l'option existante
            optionAModifier.setTexteOption(texte);
            optionAModifier.setEstCorrecte(estCorrecteCheck.isSelected());
            ok = serviceOption.modifier(optionAModifier);
            showAlert(ok, "Option modifiée avec succès !", "Échec de la modification de l'option.");
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

    // Marque le champ texte en erreur et affiche le message
    private void markError(String msg) {
        texteField.setStyle(FIELD_ERROR);
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1; -fx-wrap-text:true;");
    }
}

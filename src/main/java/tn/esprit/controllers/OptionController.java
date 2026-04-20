package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;

/**
 * Controller du formulaire Option (option_form.fxml).
 * Gère la création d'une nouvelle option et la modification d'une option existante.
 * Si optionAModifier == null → mode création, sinon → mode modification.
 */
public class OptionController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private TextField texteField;
    @FXML private CheckBox estCorrecteCheck;
    @FXML private ComboBox<Question> questionCombo;
    @FXML private Label questionErrorLabel;
    @FXML private Label messageLabel;

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

    private final ServiceOption serviceOption = new ServiceOption();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private Option optionAModifier = null;
    private int questionId;

    @FXML
    public void initialize() {
        // Remplir la ComboBox questions
        questionCombo.getItems().addAll(serviceQuestion.afficher());
        questionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTexteQuestion());
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });
        questionCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisissez la question à laquelle appartient cette option" : item.getTexteQuestion());
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });
        questionCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                questionCombo.setStyle("");
                if (questionErrorLabel != null) { questionErrorLabel.setVisible(false); questionErrorLabel.setManaged(false); }
            }
        });
        texteField.textProperty().addListener((o, ov, nv) -> {
            texteField.setStyle(FIELD_NORMAL);
            messageLabel.setText("");
            messageLabel.setStyle("");
        });
    }

    public void initNouvelle(int questionId) {
        this.questionId = questionId;
        questionCombo.getItems().stream().filter(q -> q.getId() == questionId).findFirst().ifPresent(questionCombo::setValue);
    }

    public void initModifier(Option option) {
        this.optionAModifier = option;
        this.questionId = option.getQuestionId();
        pageTitle.setText("Modifier l'Option");
        cardTitle.setText("Modifier l'Option");
        texteField.setText(option.getTexteOption());
        estCorrecteCheck.setSelected(option.isEstCorrecte());
        // Pré-sélectionner la question
        questionCombo.getItems().stream().filter(q -> q.getId() == option.getQuestionId()).findFirst().ifPresent(questionCombo::setValue);
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
        if (optionAModifier == null) {
            boolean doublon = serviceOption.findByQuestionId(questionId).stream()
                .anyMatch(o -> o.getTexteOption().equalsIgnoreCase(texte));
            if (doublon) {
                markError("⚠ Cette option existe déjà pour cette question.\n" +
                          "   Chaque option doit être unique.");
                return;
            }
        }

        // ── Validation de la question associée ──
        Question questionSelectionnee = questionCombo.getValue();
        if (questionSelectionnee == null) {
            questionCombo.setStyle(FIELD_ERROR);
            if (questionErrorLabel != null) { questionErrorLabel.setVisible(true); questionErrorLabel.setManaged(true); }
            markError("⚠ Veuillez choisir la question à laquelle appartient cette option.");
            return;
        }
        questionId = questionSelectionnee.getId();

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

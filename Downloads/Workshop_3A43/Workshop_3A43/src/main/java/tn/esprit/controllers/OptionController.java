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
 * G├¿re la cr├®ation d'une nouvelle option et la modification d'une option existante.
 * Si optionAModifier == null ÔåÆ mode cr├®ation, sinon ÔåÆ mode modification.
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
            }
        });
        questionCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisissez la question ├á laquelle appartient cette option" : item.getTexteQuestion());
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
        // Pr├®-s├®lectionner la question
        questionCombo.getItems().stream().filter(q -> q.getId() == option.getQuestionId()).findFirst().ifPresent(questionCombo::setValue);
    }

    // ÔöÇÔöÇ Sauvegarder : appel├® quand on clique sur le bouton Enregistrer ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void sauvegarder() {
        // R├®initialiser le style et le message d'erreur
        texteField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();

        // ÔöÇÔöÇ Validation du texte de l'option ÔöÇÔöÇ
        if (texte.isEmpty()) {
            markError("ÔÜá Le texte de l'option est obligatoire.\n" +
                      "   Entrez le libell├® de la r├®ponse propos├®e.");
            return;
        }
        if (texte.length() < 2) {
            markError("ÔÜá L'option est trop courte ÔÇö minimum 2 caract├¿res (actuellement " + texte.length() + ").");
            return;
        }
        if (texte.length() > 255) {
            markError("ÔÜá L'option est trop longue ÔÇö maximum 255 caract├¿res (actuellement " + texte.length() + ").\n" +
                      "   R├®sumez la r├®ponse en moins de 255 caract├¿res.");
            return;
        }

        // ÔöÇÔöÇ V├®rification des doublons (uniquement en mode cr├®ation) ÔöÇÔöÇ
        if (optionAModifier == null) {
            boolean doublon = serviceOption.findByQuestionId(questionId).stream()
                .anyMatch(o -> o.getTexteOption().equalsIgnoreCase(texte));
            if (doublon) {
                markError("ÔÜá Cette option existe d├®j├á pour cette question.\n" +
                          "   Chaque option doit ├¬tre unique.");
                return;
            }
        }

        // ÔöÇÔöÇ Validation de la question associ├®e ÔöÇÔöÇ
        Question questionSelectionnee = questionCombo.getValue();
        if (questionSelectionnee == null) {
            questionCombo.setStyle(FIELD_ERROR);
            if (questionErrorLabel != null) { questionErrorLabel.setVisible(true); questionErrorLabel.setManaged(true); }
            markError("ÔÜá Veuillez choisir la question ├á laquelle appartient cette option.");
            return;
        }
        questionId = questionSelectionnee.getId();

        // ÔöÇÔöÇ Sauvegarde en BDD ÔöÇÔöÇ
        boolean ok;
        if (optionAModifier == null) {
            // Mode cr├®ation : cr├®er une nouvelle option li├®e ├á la question
            ok = serviceOption.ajouter(new Option(texte, estCorrecteCheck.isSelected(), questionId));
            showAlert(ok, "Option ajout├®e avec succ├¿s !", "├ëchec de l'ajout de l'option.");
        } else {
            // Mode modification : mettre ├á jour l'option existante
            optionAModifier.setTexteOption(texte);
            optionAModifier.setEstCorrecte(estCorrecteCheck.isSelected());
            ok = serviceOption.modifier(optionAModifier);
            showAlert(ok, "Option modifi├®e avec succ├¿s !", "├ëchec de la modification de l'option.");
        }
        // Si succ├¿s ÔåÆ retourner ├á la liste des quiz
        if (ok) retour();
    }

    // ÔöÇÔöÇ Retour : revenir ├á la liste sans sauvegarder ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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

    // Affiche une alerte de succ├¿s ou d'├®chec
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "Ô£à Succ├¿s" : "ÔØî ├ëchec");
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
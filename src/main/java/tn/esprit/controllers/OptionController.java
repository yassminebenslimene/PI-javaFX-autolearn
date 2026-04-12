package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Option;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;

public class OptionController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private TextField texteField;
    @FXML private CheckBox estCorrecteCheck;
    @FXML private Label messageLabel;

    private static final String FIELD_NORMAL =
        "-fx-background-color:rgba(255,255,255,0.05);" +
        "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

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
        texteField.textProperty().addListener((o, ov, nv) -> {
            texteField.setStyle(FIELD_NORMAL);
            messageLabel.setText("");
            messageLabel.setStyle("");
        });
    }

    public void initNouvelle(int questionId) {
        this.questionId = questionId;
    }

    public void initModifier(Option option) {
        this.optionAModifier = option;
        this.questionId = option.getQuestionId();
        pageTitle.setText("Modifier l'Option");
        cardTitle.setText("Modifier l'Option");
        texteField.setText(option.getTexteOption());
        estCorrecteCheck.setSelected(option.isEstCorrecte());
    }

    @FXML
    public void sauvegarder() {
        texteField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();

        // ── Texte de l'option ──
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

        // ── Vérifier doublons dans la même question ──
        if (optionAModifier == null) {
            boolean doublon = serviceOption.findByQuestionId(questionId).stream()
                .anyMatch(o -> o.getTexteOption().equalsIgnoreCase(texte));
            if (doublon) {
                markError("⚠ Cette option existe déjà pour cette question.\n" +
                          "   Chaque option doit être unique.");
                return;
            }
        }

        // ── Sauvegarde ──
        boolean ok;
        if (optionAModifier == null) {
            ok = serviceOption.ajouter(new Option(texte, estCorrecteCheck.isSelected(), questionId));
            showAlert(ok, "Option ajoutée avec succès !", "Échec de l'ajout de l'option.");
        } else {
            optionAModifier.setTexteOption(texte);
            optionAModifier.setEstCorrecte(estCorrecteCheck.isSelected());
            ok = serviceOption.modifier(optionAModifier);
            showAlert(ok, "Option modifiée avec succès !", "Échec de la modification de l'option.");
        }
        if (ok) retour();
    }

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

    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "✅ Succès" : "❌ Échec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait();
    }

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

package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Option;
import tn.esprit.services.ServiceOption;

public class OptionFormController {

    @FXML private Label formTitle;
    @FXML private TextField texteField;
    @FXML private CheckBox estCorrecteCheck;
    @FXML private Label messageLabel;

    private final ServiceOption serviceOption = new ServiceOption();
    private Option optionAModifier = null;
    private int questionId;
    private Runnable onSuccess;
    private Stage stage;

    public void init(Option option, int questionId, Runnable onSuccess) {
        this.questionId = questionId;
        this.onSuccess = onSuccess;
        if (option != null) {
            this.optionAModifier = option;
            formTitle.setText("Modifier l'Option");
            texteField.setText(option.getTexteOption());
            estCorrecteCheck.setSelected(option.isEstCorrecte());
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void sauvegarder() {
        String texte = texteField.getText();
        if (texte == null || texte.trim().isEmpty()) {
            messageLabel.setText("Le texte de l'option est obligatoire.");
            messageLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;");
            return;
        }
        if (texte.trim().length() > 255) {
            messageLabel.setText("L'option ne peut pas dépasser 255 caractères.");
            messageLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;");
            return;
        }

        if (optionAModifier == null) {
            serviceOption.ajouter(new Option(texte.trim(), estCorrecteCheck.isSelected(), questionId));
        } else {
            optionAModifier.setTexteOption(texte.trim());
            optionAModifier.setEstCorrecte(estCorrecteCheck.isSelected());
            serviceOption.modifier(optionAModifier);
        }

        if (onSuccess != null) onSuccess.run();
        if (stage != null) stage.close();
    }

    @FXML
    public void annuler() {
        if (stage != null) stage.close();
    }
}

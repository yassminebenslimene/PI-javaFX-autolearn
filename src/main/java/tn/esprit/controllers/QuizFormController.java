package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

public class QuizFormController {

    @FXML private Label formTitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private Label messageLabel;
    @FXML private Button btnSauvegarder;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quizAModifier = null;
    private Runnable onSuccess;
    private Stage stage;

    @FXML
    public void initialize() {
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));
    }

    public void init(Quiz quiz, Runnable onSuccess) {
        this.onSuccess = onSuccess;
        if (quiz != null) {
            this.quizAModifier = quiz;
            formTitle.setText("Modifier le Quiz");
            btnSauvegarder.setText("Mettre à jour");
            titreField.setText(quiz.getTitre());
            descriptionField.setText(quiz.getDescription());
            etatCombo.setValue(quiz.getEtat());
        } else {
            formTitle.setText("Nouveau Quiz");
            btnSauvegarder.setText("Créer le Quiz");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void sauvegarder() {
        String titre = titreField.getText();
        String description = descriptionField.getText();
        String etat = etatCombo.getValue();

        String erreur = valider(titre, description, etat);
        if (erreur != null) {
            messageLabel.setText(erreur);
            messageLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;");
            return;
        }

        if (quizAModifier == null) {
            serviceQuiz.ajouter(new Quiz(titre.trim(), description.trim(), etat,
                null, null, null, null, null, null));
        } else {
            quizAModifier.setTitre(titre.trim());
            quizAModifier.setDescription(description.trim());
            quizAModifier.setEtat(etat);
            serviceQuiz.modifier(quizAModifier);
        }

        if (onSuccess != null) onSuccess.run();
        if (stage != null) stage.close();
    }

    @FXML
    public void annuler() {
        if (stage != null) stage.close();
    }

    private String valider(String titre, String description, String etat) {
        if (titre == null || titre.trim().isEmpty())
            return "Le titre est obligatoire.";
        if (titre.trim().length() < 3 || titre.trim().length() > 255)
            return "Le titre doit contenir entre 3 et 255 caractères.";
        if (description == null || description.trim().isEmpty())
            return "La description est obligatoire.";
        if (description.trim().length() < 10 || description.trim().length() > 2000)
            return "La description doit contenir entre 10 et 2000 caractères.";
        if (etat == null || !List.of("actif", "inactif", "brouillon", "archive").contains(etat))
            return "Sélectionnez un état valide.";
        return null;
    }
}

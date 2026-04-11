package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

public class QuizFormPageController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TextField dureeField;
    @FXML private TextField seuilField;
    @FXML private TextField tentativesField;
    @FXML private Label messageLabel;
    @FXML private Button btnSauvegarder;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quizAModifier = null;

    @FXML
    public void initialize() {
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));
    }

    /** Call this to pre-fill for edit mode */
    public void initEdit(Quiz quiz) {
        this.quizAModifier = quiz;
        pageTitle.setText("Modifier le Quiz");
        cardTitle.setText("Modifier le Quiz");
        cardSubtitle.setText("Mettez à jour les informations");
        btnSauvegarder.setText("✓ Mettre à jour");

        titreField.setText(quiz.getTitre());
        descriptionField.setText(quiz.getDescription());
        etatCombo.setValue(quiz.getEtat());
        if (quiz.getDureeMaxMinutes() != null)
            dureeField.setText(String.valueOf(quiz.getDureeMaxMinutes()));
        if (quiz.getSeuilReussite() != null)
            seuilField.setText(String.valueOf(quiz.getSeuilReussite()));
        if (quiz.getMaxTentatives() != null)
            tentativesField.setText(String.valueOf(quiz.getMaxTentatives()));
    }

    @FXML
    public void sauvegarder() {
        String titre = titreField.getText();
        String description = descriptionField.getText();
        String etat = etatCombo.getValue();

        String erreur = valider(titre, description, etat);
        if (erreur != null) {
            messageLabel.setText(erreur);
            messageLabel.getStyleClass().removeAll("msg-success");
            messageLabel.getStyleClass().add("msg-error");
            return;
        }

        Integer duree = parseOptionalInt(dureeField.getText());
        Integer seuil = parseOptionalInt(seuilField.getText());
        Integer tentatives = parseOptionalInt(tentativesField.getText());

        if (quizAModifier == null) {
            serviceQuiz.ajouter(new Quiz(
                titre.trim(), description.trim(), etat,
                duree, seuil, tentatives, null, null, null
            ));
        } else {
            quizAModifier.setTitre(titre.trim());
            quizAModifier.setDescription(description.trim());
            quizAModifier.setEtat(etat);
            quizAModifier.setDureeMaxMinutes(duree);
            quizAModifier.setSeuilReussite(seuil);
            quizAModifier.setMaxTentatives(tentatives);
            serviceQuiz.modifier(quizAModifier);
        }

        navigateToList();
    }

    @FXML
    public void retour() {
        navigateToList();
    }

    private void navigateToList() {
        try {
            StackPane contentArea =
                (StackPane) titreField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private Integer parseOptionalInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}

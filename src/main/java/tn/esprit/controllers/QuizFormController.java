package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

public class QuizFormController {

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

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quizAModifier = null;

    @FXML
    public void initialize() {
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));
        // Real-time: clear error on typing
        titreField.textProperty().addListener((o, ov, nv) -> resetField(titreField));
        descriptionField.textProperty().addListener((o, ov, nv) -> resetField(descriptionField));
        dureeField.textProperty().addListener((o, ov, nv) -> resetField(dureeField));
        seuilField.textProperty().addListener((o, ov, nv) -> resetField(seuilField));
        tentativesField.textProperty().addListener((o, ov, nv) -> resetField(tentativesField));
    }

    public void initEdit(Quiz quiz) {
        this.quizAModifier = quiz;
        pageTitle.setText("Modifier le Quiz");
        cardTitle.setText("Modifier le Quiz");
        cardSubtitle.setText("Mettez à jour les informations");
        btnSauvegarder.setText("✓ Mettre à jour");
        titreField.setText(quiz.getTitre());
        descriptionField.setText(quiz.getDescription());
        etatCombo.setValue(quiz.getEtat());
        if (quiz.getDureeMaxMinutes() != null) dureeField.setText(String.valueOf(quiz.getDureeMaxMinutes()));
        if (quiz.getSeuilReussite() != null)   seuilField.setText(String.valueOf(quiz.getSeuilReussite()));
        if (quiz.getMaxTentatives() != null)   tentativesField.setText(String.valueOf(quiz.getMaxTentatives()));
    }

    @FXML
    public void sauvegarder() {
        resetAll();
        boolean valid = true;

        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        String etat = etatCombo.getValue();
        String dureeStr = dureeField.getText() == null ? "" : dureeField.getText().trim();
        String seuilStr = seuilField.getText() == null ? "" : seuilField.getText().trim();
        String tentStr  = tentativesField.getText() == null ? "" : tentativesField.getText().trim();

        // ── Titre ──
        if (titre.isEmpty()) {
            markError(titreField, "⚠ Le titre du quiz est obligatoire.");
            valid = false;
        } else if (titre.length() < 3) {
            markError(titreField, "⚠ Le titre est trop court — minimum 3 caractères (actuellement " + titre.length() + ").");
            valid = false;
        } else if (titre.length() > 255) {
            markError(titreField, "⚠ Le titre est trop long — maximum 255 caractères (actuellement " + titre.length() + ").");
            valid = false;
        }

        // ── Description ──
        if (valid) {
            if (description.isEmpty()) {
                markError(descriptionField, "⚠ La description est obligatoire.");
                valid = false;
            } else if (description.length() < 10) {
                markError(descriptionField, "⚠ Description trop courte — minimum 10 caractères (actuellement " + description.length() + ").");
                valid = false;
            } else if (description.length() > 2000) {
                markError(descriptionField, "⚠ Description trop longue — maximum 2000 caractères (actuellement " + description.length() + ").");
                valid = false;
            }
        }

        // ── État ──
        if (valid && (etat == null || !List.of("actif","inactif","brouillon","archive").contains(etat))) {
            showError("⚠ Veuillez sélectionner un état parmi : Actif, Inactif, Brouillon, Archive.");
            valid = false;
        }

        // ── Durée (optionnelle mais doit être un entier positif si renseignée) ──
        Integer duree = null;
        if (valid && !dureeStr.isEmpty()) {
            try {
                duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    markError(dureeField, "⚠ La durée doit être un nombre entier positif (ex: 30).");
                    valid = false;
                } else if (duree > 600) {
                    markError(dureeField, "⚠ La durée maximale est 600 minutes (10 heures).");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(dureeField, "⚠ La durée doit être un nombre entier (ex: 30), pas \"" + dureeStr + "\".");
                valid = false;
            }
        }

        // ── Seuil de réussite (optionnel, entre 0 et 100) ──
        Integer seuil = null;
        if (valid && !seuilStr.isEmpty()) {
            try {
                seuil = Integer.parseInt(seuilStr);
                if (seuil < 0 || seuil > 100) {
                    markError(seuilField, "⚠ Le seuil doit être un pourcentage entre 0 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(seuilField, "⚠ Le seuil doit être un entier entre 0 et 100 (ex: 50), pas \"" + seuilStr + "\".");
                valid = false;
            }
        }

        // ── Tentatives (optionnelles, entier positif) ──
        Integer tentatives = null;
        if (valid && !tentStr.isEmpty()) {
            try {
                tentatives = Integer.parseInt(tentStr);
                if (tentatives <= 0) {
                    markError(tentativesField, "⚠ Le nombre de tentatives doit être un entier positif (ex: 3).");
                    valid = false;
                } else if (tentatives > 100) {
                    markError(tentativesField, "⚠ Le nombre de tentatives ne peut pas dépasser 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(tentativesField, "⚠ Le nombre de tentatives doit être un entier (ex: 3), pas \"" + tentStr + "\".");
                valid = false;
            }
        }

        if (!valid) return;

        // ── Sauvegarde ──
        if (quizAModifier == null) {
            serviceQuiz.ajouter(new Quiz(titre, description, etat, duree, seuil, tentatives, null, null, null));
        } else {
            quizAModifier.setTitre(titre);
            quizAModifier.setDescription(description);
            quizAModifier.setEtat(etat);
            quizAModifier.setDureeMaxMinutes(duree);
            quizAModifier.setSeuilReussite(seuil);
            quizAModifier.setMaxTentatives(tentatives);
            serviceQuiz.modifier(quizAModifier);
        }
        navigateToList();
    }

    @FXML
    public void retour() { navigateToList(); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void markError(Control field, String msg) {
        field.setStyle(FIELD_ERROR);
        showError(msg);
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1;");
    }

    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    private void resetAll() {
        titreField.setStyle(FIELD_NORMAL);
        descriptionField.setStyle(FIELD_NORMAL);
        dureeField.setStyle(FIELD_NORMAL);
        seuilField.setStyle(FIELD_NORMAL);
        tentativesField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    private void navigateToList() {
        try {
            StackPane contentArea = (StackPane) titreField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

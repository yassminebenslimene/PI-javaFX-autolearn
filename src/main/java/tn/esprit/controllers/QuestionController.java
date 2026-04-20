package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

/**
 * Controller du formulaire Question (question_form.fxml).
 * Gère la création d'une nouvelle question et la modification d'une question existante.
 * Si questionAModifier == null → mode création, sinon → mode modification.
 */
public class QuestionController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;
    @FXML private TextArea texteField;
    @FXML private TextField pointField;
    @FXML private ComboBox<Quiz> quizCombo;
    @FXML private Label quizErrorLabel;
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

    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Question questionAModifier = null;
    private int quizId;

    @FXML
    public void initialize() {
        // Remplir la ComboBox quiz
        quizCombo.getItems().addAll(serviceQuiz.afficher());
        quizCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Quiz item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });
        quizCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Quiz item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisissez le quiz auquel appartient cette question" : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });
        quizCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                quizCombo.setStyle(FIELD_NORMAL);
                if (quizErrorLabel != null) { quizErrorLabel.setVisible(false); quizErrorLabel.setManaged(false); }
            }
        });

        // Forcer le fond sombre sur le TextArea
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node content = texteField.lookup(".content");
            if (content != null) content.setStyle("-fx-background-color:#1a2e1f;");
        });

        texteField.textProperty().addListener((o, ov, nv) -> resetField(texteField));
        pointField.textProperty().addListener((o, ov, nv) -> resetField(pointField));
    }

    public void initNouvelle(int quizId) {
        this.quizId = quizId;
        // Pré-sélectionner le quiz dans la ComboBox
        quizCombo.getItems().stream().filter(q -> q.getId() == quizId).findFirst().ifPresent(quizCombo::setValue);
    }

    public void initModifier(Question question) {
        this.questionAModifier = question;
        this.quizId = question.getQuizId();
        pageTitle.setText("Modifier la Question");
        cardTitle.setText("Modifier la Question");
        cardSubtitle.setText("Mettez à jour les informations");
        texteField.setText(question.getTexteQuestion());
        pointField.setText(String.valueOf(question.getPoint()));
        // Pré-sélectionner le quiz
        quizCombo.getItems().stream().filter(q -> q.getId() == question.getQuizId()).findFirst().ifPresent(quizCombo::setValue);
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

        // ── Validation du quiz associé ──
        Quiz quizSelectionne = quizCombo.getValue();
        if (quizSelectionne == null) {
            quizCombo.setStyle(FIELD_ERROR);
            if (quizErrorLabel != null) { quizErrorLabel.setVisible(true); quizErrorLabel.setManaged(true); }
            showError("⚠ Veuillez choisir le quiz auquel appartient cette question.");
            return;
        }
        quizId = quizSelectionne.getId();

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
